package netlab.processing.disjointpaths;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.Connections;
import netlab.submission.request.Failures;
import netlab.submission.request.NumFailsAllowed;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PartialBhandariService {

    private BhandariService bhandariService;

    private BellmanFordService bellmanFordService;

    @Autowired
    public PartialBhandariService(BhandariService bhandariService, BellmanFordService bellmanFordService){
        this.bhandariService = bhandariService;
        this.bellmanFordService = bellmanFordService;
    }


    public Request solve(Request request, ProblemClass problemClass, Objective objective, Topology topology, String requestSetId) {
        Map<SourceDestPair, Map<String, Path>> paths = new HashMap<>();
        Set<SourceDestPair> pairs = request.getPairs();
        Failures failCollection = request.getFailures();
        NumFailsAllowed nfaCollection = request.getNumFailsAllowed();
        Connections connCollection = request.getConnections();
        long startTime = System.nanoTime();
        switch(problemClass){
            /*case Flex:
                paths = pathsForFlex(pairs, failCollection.getFailureSet(),
                        nfaCollection.getTotalNumFailsAllowed(), connCollection.getNumConnections(),
                        failCollection.getFailureGroups(), topology);*/
            case Combined:
                paths = pathsForCombined(pairs, failCollection, nfaCollection, connCollection, topology);
            //TODO: Implement Flow
            //TODO: Implement Endpoint
            //TODO: Implement FlowSharedF
            //TODO: Implement EndpointSharedF
        }
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        log.info("Solution took: " + duration + " seconds");
        request.setChosenPaths(paths);
        request.setRunningTimeSeconds(duration);
        request.setIsFeasible(paths.values().stream().noneMatch(Map::isEmpty));
        return request;
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
     * @param nfaCollection
     * @param connCollection
     * @param topo
     * @return
     */
    private Map<SourceDestPair,Map<String,Path>> pathsForCombined(Set<SourceDestPair> pairs, Failures failCollection,
                                                                  NumFailsAllowed nfaCollection,
                                                                  Connections connCollection, Topology topo) {

        // Get relevant parameters from input
        Integer numConnections = connCollection.getNumConnections();
        Map<SourceDestPair, Integer> pairMinConnMap = connCollection.getPairMinConnectionsMap();
        Map<SourceDestPair, Integer> pairMaxConnMap = connCollection.getPairMaxConnectionsMap();
        Map<Node, Integer> srcMinConnMap = connCollection.getSrcMinConnectionsMap();
        Map<Node, Integer> srcMaxConnMap = connCollection.getSrcMaxConnectionsMap();
        Map<Node, Integer> dstMinConnMap = connCollection.getSrcMinConnectionsMap();
        Map<Node, Integer> dstMaxConnMap = connCollection.getSrcMaxConnectionsMap();
        Integer reachMinS = connCollection.getReachMinS();
        Integer reachMaxS = connCollection.getReachMaxS();
        Integer reachMinD = connCollection.getReachMinD();
        Integer reachMaxD = connCollection.getReachMaxD();
        Set<Failure> failureSet = failCollection.getFailureSet();
        List<List<Failure>> failureGroups = failCollection.getFailureGroups();
        Integer totalNumFailsAllowed = nfaCollection.getTotalNumFailsAllowed();

        // Build a map of paths and failures associated with those paths.
        Map<SourceDestPair, Map<String, Path>> pathMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> new HashMap<>()));

        if(!checkIfInputIsValid(srcMinConnMap, dstMinConnMap, pairMinConnMap, reachMaxS, reachMaxD, numConnections)){
            return pathMap;
        }

        // Used to determine path viability and if you have sufficient survivable paths.
        Map<List<Failure>, List<Path>> failureToPathMap = failureGroups.stream().collect(Collectors.toMap(f -> f, f -> new ArrayList<>()));
        Map<Path, List<List<Failure>>> pathToFailureGroupMap = new HashMap<>();

        List<SourceDestPair> sortedPairs = sortPairsByPathCost(pairs, topo);


        //Modify the topology (if necessary) by removing nodes and replacing with incoming/outgoing nodes
        boolean nodesCanFail = failureSet.stream().anyMatch(f -> f.getNode() != null);

        int totalChosenPaths = 0;
        boolean sufficientPathsEstablished = false;

        Map<SourceDestPair, Integer> pairNumEstablished = sortedPairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
        Map<Node, Integer> srcNumEstablished = srcMinConnMap.keySet().stream().collect(Collectors.toMap(s -> s, s -> 0));
        Map<Node, Integer> dstNumEstablished = dstMinConnMap.keySet().stream().collect(Collectors.toMap(d -> d, d -> 0));

        //TODO: Update number of paths established per pair, src, and dest.
        //TODO: Evaluate if you have enough total, src, dst, pair paths. (Include min/max reachability for src/dst).

        for(SourceDestPair pair : sortedPairs){
            List<Path> paths = findPaths(topo, pair, numConnections, totalChosenPaths, pairMinConnMap, pairMaxConnMap, srcMinConnMap,
                    srcMaxConnMap, dstMinConnMap, srcNumEstablished, dstNumEstablished, dstMaxConnMap,
                    totalNumFailsAllowed, nodesCanFail, failureSet);
            // For each new path, figure out if adding it will get you any closer to goal
            // Will not get you closer if it will be disconnected by X failures shared by an existing path
            int id = 0;
            for(Path newPath : paths){
                for(List<Failure> failureGroup : failureGroups){
                    // If this failure is in this path
                    if(isFailureGroupInPath(failureGroup, newPath)){
                        // If this failure already could disconnect a chosen path, don't keep this new path
                        failureToPathMap.get(failureGroup).add(newPath);
                        pathToFailureGroupMap.putIfAbsent(newPath, new ArrayList<>());
                        pathToFailureGroupMap.get(newPath).add(failureGroup);
                    }
                }
                pathMap.get(pair).put(String.valueOf(id), newPath);
                id++;
                totalChosenPaths++;
                sufficientPathsEstablished = determineSufficientPaths(failureToPathMap, numConnections, totalChosenPaths);
                if(sufficientPathsEstablished){
                    break;
                }
            }
            if(sufficientPathsEstablished){
                break;
            }
        }

        if(!sufficientPathsEstablished){
            totalChosenPaths = augmentPathMap(pathMap, failureToPathMap, pathToFailureGroupMap, numConnections, totalChosenPaths);
        }

        return determineSufficientPaths(failureToPathMap, numConnections, totalChosenPaths) ?
                pathMap : pairs.stream().collect(Collectors.toMap(p -> p, p -> new HashMap<>()));

    }



    private boolean checkIfInputIsValid(Map<Node, Integer> srcMinConnMap, Map<Node, Integer> dstMinConnMap,
                                        Map<SourceDestPair, Integer> pairMinConnMap,
                                        Integer reachMaxS, Integer reachMinD, Integer numC) {

        long numMinSrcs = srcMinConnMap.values().stream().filter(c -> c > 0).count();
        long numMinDsts = dstMinConnMap.values().stream().filter(c -> c > 0).count();
        long numMinPairs = pairMinConnMap.values().stream().filter(c -> c > 0).count();
        if(numMinSrcs > reachMaxS){
            log.warn("Number of srcs that require at least one connection greater than max srcs that can transmit traffic.");
            return false;
        }
        if(numMinDsts < reachMinD){
            log.warn("Number of dsts that require at least one connection greater than max dsts that can receive traffic.");
            return false;
        }
        if(numMinSrcs == 0 && numMinDsts == 0 && numMinPairs == 0 && numC == 0){
            log.warn("No minimum connection requirements established. No paths to find.");
            return false;
        }
        return true;
    }


    /**
     * Duplicate existing paths to attempt to increase the number of viable paths to satisfy the demanded number of connections.
     */
    private int augmentPathMap(Map<SourceDestPair, Map<String, Path>> pathMap,
                                Map<List<Failure>, List<Path>> failureToPathMap,
                                Map<Path, List<List<Failure>>> pathToFailureGroupMap, Integer numConnections,
                                Integer totalChosenPaths) {

        // Get all paths that are disconnected by any failure group with disconnections = Max(disconnections)
        Set<Path> pathsAcrossMaxFailureGroups = findAllPathsForMaximumFailureGroups(failureToPathMap);

        // For each of those paths, duplicate it NumConnections-1 times, and add it to the appropriate maps
        for(Path pathToCopy : pathsAcrossMaxFailureGroups){
            List<Node> pathNodes = pathToCopy.getNodes();
            Node src = pathNodes.get(0);
            Node dst = pathNodes.get(pathNodes.size()-1);
            SourceDestPair pair = SourceDestPair.builder().src(src).dst(dst).build();
            Map<String, Path> pairPathMap = pathMap.get(pair);

            int newId = pairPathMap.size();
            int numCopiesToMake = numConnections - 1;

            // Get the relevant failure groups
            List<List<Failure>> relevantFailureGroups = pathToFailureGroupMap.get(pathToCopy);
            // Add the path copy to the pairPathMap and failureToPathMap
            for(int i = 0; i < numCopiesToMake; i++){
                pairPathMap.put(String.valueOf(newId), pathToCopy);
                newId++;
                for(List<Failure> failureGroup : relevantFailureGroups){
                    failureToPathMap.get(failureGroup).add(pathToCopy);
                }
                totalChosenPaths++;
            }
        }
        return totalChosenPaths;
    }

    private Set<Path> findAllPathsForMaximumFailureGroups(Map<List<Failure>, List<Path>> failureToPathMap) {
        int mostRemovedPaths = getMaximumRemovedPaths(failureToPathMap);
        Set<Path> sharedPaths = new HashSet<>();
        for(List<Path> paths : failureToPathMap.values()){
            if(paths.size() == mostRemovedPaths){
                sharedPaths.addAll(paths);
            }
        }
        return sharedPaths;
    }

    private boolean determineSufficientPaths(Map<List<Failure>, List<Path>> failureToPathMap, Integer numConnections,
                                             int totalChosenPaths) {
        // Get "most damaging" failure group -> determine maximum number of paths removed by any key in failureToPathMap
        int mostRemovedPaths = getMaximumRemovedPaths(failureToPathMap);
        return totalChosenPaths - mostRemovedPaths >= numConnections;
    }

    private int getMaximumRemovedPaths(Map<List<Failure>, List<Path>> failureToPathMap){
        Optional<List<Path>> maxRemovedPathSet = failureToPathMap.values().stream().max(Comparator.comparingInt(List::size));
        return maxRemovedPathSet.isPresent() ? maxRemovedPathSet.get().size() : 0;
    }

    private boolean isFailureGroupInPath(List<Failure> failureGroup, Path newPath) {
        for(Failure failure : failureGroup){
            if(failure.getLink() != null){
                // Check for both this link and its reverse
                Link link = failure.getLink();
                String reverseId = link.getTarget().getId() + "-" + link.getOrigin().getId();
                if(newPath.getLinkIds().contains(link.getId()) || newPath.getLinkIds().contains(reverseId)){
                    return true;
                }
            }
            else{
                if(newPath.getNodeIds().contains(failure.getNode().getId())){
                    return true;
                }
            }
        }
        return false;
    }

    private List<Path> findPaths(Topology topo, SourceDestPair pair, Integer numC, Integer numCPaths,
                                 Map<SourceDestPair, Integer> pairMinConnMap, Map<SourceDestPair, Integer> pairMaxConnMap,
                                 Map<Node, Integer> srcMinConnMap, Map<Node, Integer> srcMaxConnMap,
                                 Map<Node, Integer> dstMinConnMap, Map<Node, Integer> srcNumEstablished,
                                 Map<Node, Integer> dstNumEstablished, Map<Node, Integer> dstMaxConnMap,
                                 Integer nfa, boolean nodesCanFail, Set<Failure> failureSet) {

        Node src = pair.getSrc();
        Node dst = pair.getDst();

        Integer minPairC = pairMinConnMap.get(pair);
        Integer maxPairC = pairMinConnMap.get(pair);
        Integer minSrcC = srcMinConnMap.get(src);
        Integer maxSrcC = srcMaxConnMap.get(src);
        Integer minDstC = dstMinConnMap.get(dst);
        Integer maxDstC = dstMaxConnMap.get(dst);
        Integer numSPaths = srcNumEstablished.get(src);
        Integer numDPaths = dstNumEstablished.get(dst);
        List<List<Link>> pathLinks = bhandariService.computeDisjointPaths(topo, src, dst, numC, numCPaths, minPairC, maxPairC,
                minSrcC, maxSrcC, minDstC, maxDstC, numSPaths, numDPaths, nfa, nodesCanFail, failureSet, false);
        return convertToPaths(pathLinks);
    }

    private List<Path> findPaths(Topology topology, Node src, Node dst, Integer numConnections,
                                 Integer totalNumFailsAllowed, boolean nodesCanFail, Set<Failure> failureSet){
        List<List<Link>> pathLinks = bhandariService.computeDisjointPaths(topology, src, dst,
                numConnections, totalNumFailsAllowed, nodesCanFail, failureSet, false);
        return convertToPaths(pathLinks);
    }


    private List<Path> sortPathsByWeight(List<Path> paths) {
        return paths.stream().sorted(Comparator.comparing(Path::getTotalWeight)).collect(Collectors.toList());
    }

    private List<Path> convertToPaths(List<List<Link>> pathLinks){
        return pathLinks.stream().map(Path::new).collect(Collectors.toList());
    }

    private List<SourceDestPair> sortPairsByPathCost(Set<SourceDestPair> pairs, Topology topology) {
        Map<SourceDestPair, Long> leastCostMap = new HashMap<>();
        for(SourceDestPair pair : pairs){
            List<Link> leastCostPath = bellmanFordService.shortestPath(topology, pair.getSrc(), pair.getDst());
            Long weight = leastCostPath.stream().map(Link::getWeight).reduce(0L, (li1, li2) -> li1 + li2);
            leastCostMap.put(pair, weight);
        }
        return pairs.stream().sorted(Comparator.comparing((leastCostMap::get))).collect(Collectors.toList());
    }


        /*
    private Map<SourceDestPair,Map<String,Path>> pathsForFlex(Set<SourceDestPair> pairs, Set<Failure> failureSet,
                                                              Integer totalNumFailsAllowed, Integer numConnections,
                                                              List<List<Failure>> failureGroups, Topology topo) {

        Map<SourceDestPair, Map<String, Path>> pathMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> new HashMap<>()));
        Map<List<Failure>, List<Path>> failureToPathMap = failureGroups.stream().collect(Collectors.toMap(f -> f, f -> new ArrayList<>()));
        Map<Path, List<List<Failure>>> pathToFailureGroupMap = new HashMap<>();
        List<SourceDestPair> sortedPairs = sortPairsByPathCost(pairs, topo);
        //Modify the topology (if necessary) by removing nodes and replacing with incoming/outgoing nodes
        boolean nodesCanFail = failureSet.stream().anyMatch(f -> f.getNode() != null);

        int totalChosenPaths = 0;
        boolean sufficientPathsEstablished = false;
        for(SourceDestPair pair : sortedPairs){
            List<Path> paths = findPaths(topo, pair.getSrc(), pair.getDst(),
                    numConnections, totalNumFailsAllowed, nodesCanFail, failureSet);
            // For each new path, figure out if adding it will get you any closer to goal
            // Will not get you closer if it will be disconnected by X failures shared by an existing path
            int id = 0;
            for(Path newPath : paths){
                for(List<Failure> failureGroup : failureGroups){
                    // If this failure is in this path
                    if(isFailureGroupInPath(failureGroup, newPath)){
                        // If this failure already could disconnect a chosen path, don't keep this new path
                        failureToPathMap.get(failureGroup).add(newPath);
                        pathToFailureGroupMap.putIfAbsent(newPath, new ArrayList<>());
                        pathToFailureGroupMap.get(newPath).add(failureGroup);
                    }
                }
                pathMap.get(pair).put(String.valueOf(id), newPath);
                id++;
                totalChosenPaths++;
                sufficientPathsEstablished = determineSufficientPaths(failureToPathMap, numConnections, totalChosenPaths);
                if(sufficientPathsEstablished){
                    break;
                }
            }
            if(sufficientPathsEstablished){
                break;
            }
        }
        if(!sufficientPathsEstablished){
            totalChosenPaths = augmentPathMap(pathMap, failureToPathMap, pathToFailureGroupMap, numConnections, totalChosenPaths);
        }
        return determineSufficientPaths(failureToPathMap, numConnections, totalChosenPaths) ?
                pathMap : pairs.stream().collect(Collectors.toMap(p -> p, p -> new HashMap<>()));

    }*/

}
