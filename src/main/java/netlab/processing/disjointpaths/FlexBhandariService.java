package netlab.processing.disjointpaths;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.PathMappingService;
import netlab.processing.shortestPaths.BellmanFordService;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.*;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FlexBhandariService {

    private BhandariService bhandariService;

    private BellmanFordService bellmanFordService;

    private TopologyAdjustmentService topologyAdjustmentService;

    private PathMappingService pathMappingService;

    @Autowired
    public FlexBhandariService(BhandariService bhandariService, BellmanFordService bellmanFordService,
                               TopologyAdjustmentService topologyAdjustmentService, PathMappingService pathMappingService){
        this.bhandariService = bhandariService;
        this.bellmanFordService = bellmanFordService;
        this.topologyAdjustmentService = topologyAdjustmentService;
        this.pathMappingService = pathMappingService;
    }


    public Details solve(Request request, Topology topology) {
        Details details = request.getDetails();

        // Get sorted pairs
        List<SourceDestPair> pairs = topologyAdjustmentService.sortPairsByPathCost(details.getPairs(), topology);

        Failures failCollection = details.getFailures();
        NumFailureEvents nfaCollection = details.getNumFailureEvents();
        Connections connCollection = details.getConnections();
        long startTime = System.nanoTime();
        Map<SourceDestPair, Map<String, Path>> paths = findPaths(pairs, failCollection, nfaCollection, connCollection,
                request.getFailureClass(), request.getTrafficCombinationType(), topology);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        log.info("Solution took: " + duration + " seconds");
        details.setChosenPaths(paths);
        details.setRunningTimeSeconds(duration);
        details.setIsFeasible(paths.values().stream().noneMatch(Map::isEmpty));
        return details;
    }

    /**
     * Need to reach minimum number of total connections
     * Max/Min conns per source
     * Max/Min conns per dest
     * Max/Min conns per pair
     * Max/Min number of sources that transmit traffic
     * Max/Min number of destinations that receive traffic
     * @param pairs
     * @param failCollection
     * @param nfeCollection
     * @param connCollection
     * @param failureClass
     *@param topo  @return
     */
    private Map<SourceDestPair,Map<String,Path>> findPaths(Collection<SourceDestPair> pairs, Failures failCollection,
                                                           NumFailureEvents nfeCollection, Connections connCollection,
                                                           FailureClass failureClass, TrafficCombinationType trafficType,
                                                           Topology topo) {

        // Get relevant parameters from input
        Integer numConnections = connCollection.getNumConnections();
        Map<SourceDestPair, Integer> pairMinConnMap = connCollection.getPairMinConnectionsMap();
        Map<Node, Integer> srcMinConnMap = connCollection.getSrcMinConnectionsMap();
        Map<Node, Integer> dstMinConnMap = connCollection.getDstMinConnectionsMap();
        Integer reachMinS = connCollection.getUseMinS();
        Integer reachMaxS = connCollection.getUseMaxS();
        Integer reachMinD = connCollection.getUseMinD();
        Integer reachMaxD = connCollection.getUseMaxD();
        Set<Failure> failureSet = failCollection.getFailureSet();
        List<List<Failure>> failureGroups = failCollection.getFailureGroups();
        Integer nfe = nfeCollection.getTotalNumFailureEvents();

        boolean nodesCanFail = failureClass == FailureClass.Both || failureClass == FailureClass.Node;

        if(!checkIfInputIsValid(srcMinConnMap, dstMinConnMap, pairMinConnMap, reachMinS, reachMinD, reachMaxS, reachMaxD, numConnections)){
            return pairs.stream().collect(Collectors.toMap(p -> p, p -> new HashMap<>()));
        }

        Map<Node, Set<Path>> usedSources = new HashMap<>();
        Map<Node, Set<Path>> usedDestinations = new HashMap<>();
        Map<SourceDestPair, List<Path>> pathsPerPair = new HashMap<>();
        for(SourceDestPair pair : pairs){
            Node src = pair.getSrc();
            Node dst = pair.getDst();
            // Modify the topology if combining traffic
            Topology modifiedTopo = topologyAdjustmentService.adjustWeightsUsingTrafficCombination(topo, trafficType, src, dst,
                    usedSources, usedDestinations);
            int minForSrc = srcMinConnMap.get(src);
            int minForDst = dstMinConnMap.get(dst);
            int minForPair = pairMinConnMap.get(pair);
            int numC = Math.max(1, Math.max(minForSrc, Math.max(minForDst, minForPair)));
            List<List<Link>> pathLinks = bhandariService.computeDisjointPaths(modifiedTopo, src, dst, numC, nfe, nodesCanFail,
                    failureSet, false);
            List<Path> paths = pathLinks.stream()
                    .map(li -> pathMappingService.convertToPath(li, topo.getLinkIdMap()))
                    .collect(Collectors.toList());
            pathsPerPair.put(pair, paths);

            usedSources.putIfAbsent(src, new HashSet<>());
            usedSources.get(src).addAll(paths);

            usedDestinations.putIfAbsent(dst, new HashSet<>());
            usedDestinations.get(dst).addAll(paths);
        }
        // We now have the path lists for each pair, and the path sets per src and dest
        // Evaluate if minimum requirements were met for each src and dest, and sufficient srcs and dests are connected
        // Sort the pairs by their total path cost, then only keep the pairs that help you reach the goal
        Map<SourceDestPair, Long> totalWeightMap = pathsPerPair.keySet().stream()
                .collect(Collectors.toMap(
                        p -> p,
                        p -> pathsPerPair.get(p).stream().mapToLong(Path::getTotalWeight).sum())
                );
        Map<SourceDestPair, Integer> pathNumMap = pathsPerPair.keySet().stream()
                .collect(Collectors.toMap(
                        p -> p,
                        p -> pathsPerPair.get(p).size())
                );
        List<SourceDestPair> sortedPairs = pathsPerPair.keySet()
                .stream()
                .filter(p -> !pathsPerPair.get(p).isEmpty())
                //.sorted(Comparator.comparing(pathNumMap::get).reversed().thenComparing(totalWeightMap::get))
                .sorted(Comparator.comparing(totalWeightMap::get))
                .collect(Collectors.toList());

        return filterPathsPerPair(pathsPerPair, sortedPairs, pairMinConnMap, reachMinS, reachMinD, reachMaxS, reachMaxD,
                srcMinConnMap, dstMinConnMap, failureGroups);
    }

    private Map<SourceDestPair,Map<String,Path>> filterPathsPerPair(Map<SourceDestPair, List<Path>> pathsPerPair,
                                                                    List<SourceDestPair> sortedPairs,
                                                                    Map<SourceDestPair, Integer> pairMinConnMap,
                                                                    Integer reachMinS, Integer reachMinD,
                                                                    Integer reachMaxS, Integer reachMaxD,
                                                                    Map<Node, Integer> srcMinConnMap,
                                                                    Map<Node, Integer> dstMinConnMap,
                                                                    List<List<Failure>> failureGroups) {

        // Build a map of paths and failures associated with those paths.
        Map<SourceDestPair, Map<String, Path>> chosenPathsMap = new HashMap<>();
        Map<Node, Set<Path>> pathsPerSrc = new HashMap<>();
        Map<Node, Set<Path>> pathsPerDest = new HashMap<>();
        Set<Node> connectedSources = new HashSet<>();
        Set<Node> connectedDests = new HashSet<>();
        boolean allPairsNeeded = pairMinConnMap.values().stream().allMatch(min -> min >= 1);
        for(SourceDestPair pair : sortedPairs){
            Node src = pair.getSrc();
            Node dst = pair.getDst();
            List<Path> pairPaths = pathsPerPair.get(pair);
            // If not every pair is needed, and this pair is not needed, and there are sufficient sources/dests, continue
            if(!allPairsNeeded && pairMinConnMap.get(pair) == 0 && !checkIfPathsAreNeeded(connectedSources, connectedDests,
                    reachMinS, reachMinD, reachMaxS, reachMaxD, src, dst)){
                continue;
            }
            chosenPathsMap.put(pair, new HashMap<>());
            int pathId = 1;
            // Add the paths to the node path sets and store the paths
            for(Path path : pairPaths){
                pathsPerSrc.putIfAbsent(src, new HashSet<>());
                pathsPerSrc.get(src).add(path);

                pathsPerDest.putIfAbsent(dst, new HashSet<>());
                pathsPerDest.get(dst).add(path);

                chosenPathsMap.get(pair).put(String.valueOf(pathId), path);

                pathId++;
            }
            // Check if the src and dst now have enough to be considered connected
            if(!connectedSources.contains(src) && pathsPerSrc.get(src).size() >= srcMinConnMap.get(src)){
                boolean enough = checkForSufficientPaths(srcMinConnMap.get(src), pathsPerSrc.get(src), failureGroups);
                if(enough){
                    connectedSources.add(src);
                }
            }
            if(!connectedDests.contains(dst) && pathsPerDest.get(dst).size() >= dstMinConnMap.get(dst)){
                boolean enough = checkForSufficientPaths(dstMinConnMap.get(dst), pathsPerDest.get(dst), failureGroups);
                if(enough){
                    connectedDests.add(dst);
                }
            }
        }
        return chosenPathsMap;
    }

    // Only add the new src/dst if they will contribute to the minS/D goals
    private boolean checkIfPathsAreNeeded(Set<Node> connectedSources, Set<Node> connectedDests, Integer reachMinS,
                                          Integer reachMinD, Integer reachMaxS, Integer reachMaxD, Node src, Node dst) {
        Set<Node> addedSrc = new HashSet<>(connectedSources);
        addedSrc.add(src);
        Set<Node> addedDst = new HashSet<>(connectedDests);
        addedDst.add(dst);
        // If you still need sources or destinations
        if(connectedSources.size() < reachMinS || connectedDests.size() < reachMinD){
            if(addedSrc.size() > connectedSources.size() || addedDst.size() > connectedDests.size()){
                return true;
            }
        }
        return false;
    }

    private int getNumDisconn(Collection<Path> paths, List<List<Failure>> failureGroups){
        int greatestDisconn = 0;
        for(List<Failure> group : failureGroups){
            int disconnPaths = 0;
            for(Path path : paths) {
                List<Node> reachablenodes = pathMappingService.getReachableNodes(path, group);
                if(!reachablenodes.contains(path.getNodes().get(path.getNodes().size()-1))){
                    disconnPaths++;
                }
            }
            greatestDisconn = Math.max(greatestDisconn, disconnPaths);
        }
        return greatestDisconn;
    }

    private boolean checkForSufficientPaths(Integer minC, Set<Path> paths, List<List<Failure>> failureGroups) {
        return minC <= paths.size() - getNumDisconn(paths, failureGroups);
    }


    private boolean checkIfInputIsValid(Map<Node, Integer> srcMinConnMap, Map<Node, Integer> dstMinConnMap,
                                        Map<SourceDestPair, Integer> pairMinConnMap,
                                        Integer reachMinS, Integer reachMinD, Integer reachMaxS, Integer reachMaxD, Integer numC) {

        long numMinSrcs = srcMinConnMap.values().stream().filter(c -> c > 0).count();
        long numMinDsts = dstMinConnMap.values().stream().filter(c -> c > 0).count();
        long numMinPairs = pairMinConnMap.values().stream().filter(c -> c > 0).count();
        if(numMinSrcs > reachMaxS){
            log.warn("Number of srcs that require at least one connection greater than max srcs that can transmit traffic.");
            return false;
        }
        if(numMinDsts > reachMaxD){
            log.warn("Number of dsts that require at least one connection greater than max dsts that can receive traffic.");
            return false;
        }
        if(numMinSrcs == 0 && numMinDsts == 0 && numMinPairs == 0 && numC == 0 && reachMinS == 0 && reachMinD == 0){
            log.warn("No minimum connection requirements established. No paths to find.");
            return false;
        }
        return true;
    }


}
