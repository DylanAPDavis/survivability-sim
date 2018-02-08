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

import javax.xml.transform.Source;
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

        Failures failCollection = details.getFailures();
        NumFailureEvents nfaCollection = details.getNumFailureEvents();
        Connections connCollection = details.getConnections();
        long startTime = System.nanoTime();
        //Topology riskTopology = topologyAdjustmentService.adjustWeightsWithFailureProbs(topology, failCollection.getFailureSet());
        Map<SourceDestPair, Map<String, Path>> paths = findPaths(details.getPairs(), failCollection, nfaCollection, connCollection,
                request.getFailureClass(), request.getTrafficCombinationType(), topology);
        //topologyAdjustmentService.readjustLinkWeights(paths, topology);
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
    private Map<SourceDestPair,Map<String,Path>> findPaths(Set<SourceDestPair> pairs, Failures failCollection,
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
            //int minForSrc = srcMinConnMap.get(src);
            //int minForDst = dstMinConnMap.get(dst);
            int minForPair = pairMinConnMap.get(pair);
            //int numC = Math.max(1, Math.max(minForSrc, Math.max(minForDst, minForPair)));
            int numC = Math.max(1, minForPair);
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

        return filterPathsPerPair(pathsPerPair, pairs, usedSources.keySet(), usedDestinations.keySet(),
                pairMinConnMap, reachMinS, reachMinD, reachMaxS, reachMaxD,
                srcMinConnMap, dstMinConnMap, failureGroups, failureSet);
    }


    private Map<SourceDestPair,Map<String,Path>> filterPathsPerPair(Map<SourceDestPair, List<Path>> pathsPerPair,
                                                                    Set<SourceDestPair> pairs, Set<Node> sources,
                                                                    Set<Node> destinations,
                                                                    Map<SourceDestPair, Integer> pairMinConnMap,
                                                                    Integer reachMinS, Integer reachMinD,
                                                                    Integer reachMaxS, Integer reachMaxD,
                                                                    Map<Node, Integer> srcMinConnMap,
                                                                    Map<Node, Integer> dstMinConnMap,
                                                                    List<List<Failure>> failureGroups,
                                                                    Set<Failure> failures) {

        // Figure out the number of disconnected paths, and whether the source/dests can fail
        Set<String> failureIds = failures.stream()
                .map(f -> f.getNode() != null ? f.getNode().getId() : f.getLink().getId())
                .collect(Collectors.toSet());
        Map<SourceDestPair, Integer> intactPathCount = new HashMap<>();
        Map<SourceDestPair, Double> totalWeights = new HashMap<>();
        Set<SourceDestPair> srcDstDontFail = new HashSet<>();
        Set<SourceDestPair> srcFails = new HashSet<>();
        Set<SourceDestPair> dstFails = new HashSet<>();
        Set<SourceDestPair> srcDstFail = new HashSet<>();
        for(SourceDestPair pair : pairs){
            Node src = pair.getSrc();
            Node dst = pair.getDst();
            List<Path> paths = pathsPerPair.get(pair);

            Double totalWeight = paths.stream().mapToDouble(Path::getTotalWeight).sum();
            totalWeights.put(pair, totalWeight);

            Integer numDisconn = getNumDisconn(src, dst, paths, failureGroups);
            Integer numIntact = paths.size() - numDisconn;
            intactPathCount.put(pair, numIntact);

            if(failureIds.contains(src.getId()) || failureIds.contains(dst.getId())){
                // Just dest
                if(!failureIds.contains(src.getId())){
                    dstFails.add(pair);
                }
                // Just src
                else if (!failureIds.contains(dst.getId())){
                    srcFails.add(pair);
                }
                // Both
                else {
                    srcDstFail.add(pair);
                }
            } else{
                srcDstDontFail.add(pair);
            }
        }

        List<SourceDestPair> sortedPairs = sortPairs(intactPathCount, totalWeights, srcDstDontFail, srcFails, dstFails,
                srcDstFail, sources.size(), destinations.size());
        sortedPairs = interleavePairs(sortedPairs, sources, destinations);

        // Build a map of paths and failures associated with those paths.
        Map<Node, Integer> intactPathsPerSrc = sources.stream().collect(Collectors.toMap(s -> s, s -> 0));
        Map<Node, Integer> intactPathsPerDst = destinations.stream().collect(Collectors.toMap(d -> d, d -> 0));
        Map<SourceDestPair, Integer> intactPathsPerPair = pairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
        Map<SourceDestPair, Map<String, Path>> chosenPathsMap = new HashMap<>();
        Map<Node, Set<Path>> pathsPerSrc = sources.stream().collect(Collectors.toMap(s -> s, s -> new HashSet<>()));
        Map<Node, Set<Path>> pathsPerDst = destinations.stream().collect(Collectors.toMap(d -> d, d -> new HashSet<>()));

        Set<Node> failedSrcs = new HashSet<>();
        Set<Node> protectedSrcs = new HashSet<>();
        Set<Node> failedDsts = new HashSet<>();
        Set<Node> protectedDsts = new HashSet<>();
        for(SourceDestPair pair : sortedPairs){
            Node src = pair.getSrc();
            Node dst = pair.getDst();
            if(failureIds.contains(src.getId())){
                failedSrcs.add(src);
            } else{
                protectedSrcs.add(src);
            }
            if(failureIds.contains(dst.getId())){
                failedDsts.add(dst);
            } else{
                protectedDsts.add(dst);
            }
            List<Path> pairPaths = pathsPerPair.get(pair);

            chosenPathsMap.put(pair, new HashMap<>());
            int pathId = 1;
            // Add the paths to the node path sets and store the paths
            for(Path path : pairPaths){
                /*pathsPerSrc.putIfAbsent(src, new HashSet<>());
                pathsPerSrc.get(src).add(path);

                pathsPerDest.putIfAbsent(dst, new HashSet<>());
                pathsPerDest.get(dst).add(path);*/

                chosenPathsMap.get(pair).put(String.valueOf(pathId), path);
                pathsPerSrc.get(src).add(path);
                pathsPerDst.get(dst).add(path);
                pathId++;
            }
            int numIntact = intactPathCount.get(pair);
            intactPathsPerPair.put(pair, numIntact);
            intactPathsPerSrc.put(src, intactPathsPerSrc.get(src) + numIntact);
            intactPathsPerDst.put(dst, intactPathsPerDst.get(dst) + numIntact);
            boolean finished = evaluateSolution(intactPathsPerPair, intactPathsPerSrc, intactPathsPerDst, protectedSrcs, failedSrcs, protectedDsts, failedDsts,
                    pathsPerSrc, pathsPerDst, reachMinS, reachMinD, srcMinConnMap, dstMinConnMap, pairMinConnMap,
                    failureGroups.get(0).size());
            if(finished){
                break;
            }
        }
        return chosenPathsMap;
    }

    private List<SourceDestPair> interleavePairs(List<SourceDestPair> sortedPairs, Set<Node> sources, Set<Node> destinations) {
        List<Node> sourceOrder = new ArrayList<>();
        List<Node> destOrder = new ArrayList<>();
        Map<Node, List<SourceDestPair>> orderedPairsPerSrc = new HashMap<>();
        Map<Node, List<SourceDestPair>> orderedPairsPerDst = new HashMap<>();
        List<SourceDestPair> outputList = new ArrayList<>();
        for(SourceDestPair pair : sortedPairs){
            Node src = pair.getSrc();
            Node dst = pair.getDst();
            // If this pair hasn't been seen yet, add it to the ordering
            if(!orderedPairsPerSrc.containsKey(src)){
                sourceOrder.add(src);
            }
            if(!orderedPairsPerDst.containsKey(dst)){
                destOrder.add(dst);
            }
            orderedPairsPerSrc.putIfAbsent(src, new ArrayList<>());
            orderedPairsPerSrc.get(src).add(pair);
            orderedPairsPerDst.putIfAbsent(dst, new ArrayList<>());
            orderedPairsPerDst.get(dst).add(pair);
        }
        // Go through the sources, interleave their backup pairs
        int numSatisfied = 0;
        Set<Node> usedSources = new HashSet<>();
        Set<Node> satisfiedDest = new HashSet<>();
        Set<SourceDestPair> usedPairs = new HashSet<>();
        while(numSatisfied < sourceOrder.size()){
            // Go until all the nodes are satisfied
            for(Node src : sourceOrder){
                List<SourceDestPair> pairs = orderedPairsPerSrc.get(src);
                if(pairs.size() == 0){
                    numSatisfied++;
                } else{
                    SourceDestPair nextPair = pairs.remove(0);
                    while(usedPairs.contains(nextPair) && pairs.size() > 0){
                        nextPair = pairs.remove(0);
                    }
                    if(!usedPairs.contains(nextPair)) {
                        outputList.add(nextPair);
                        usedPairs.add(nextPair);
                        usedSources.add(src);

                        Node dst = nextPair.getDst();
                        satisfiedDest.add(dst);
                    }
                }
                if(usedSources.size() == sources.size()){
                    // If you haven't gotten at least one pair including every dest yet
                    if(satisfiedDest.size() < destinations.size()){
                        // Go through those dests
                        for(Node dest : destOrder){
                            // If the dest is not satisfied
                            if(!satisfiedDest.contains(dest)){
                                // Get the pairs including that dest
                                List<SourceDestPair> destPairs = orderedPairsPerDst.get(dest);
                                for(SourceDestPair destPair : destPairs){
                                    // If that pair hasn't been used yet, use it, and mark this dest as satisfied
                                    if(!usedPairs.contains(destPair)){
                                        outputList.add(destPair);
                                        usedPairs.add(destPair);
                                        satisfiedDest.add(dest);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return outputList;
    }


    private boolean evaluateSolution(Map<SourceDestPair, Integer> intactPathsPerPair,
                                     Map<Node, Integer> intactPathsPerSrc, Map<Node, Integer> intactPathsPerDst,
                                     Set<Node> protectedSrcs, Set<Node> failedSrcs, Set<Node> protectedDsts, Set<Node> failedDsts,
                                     Map<Node, Set<Path>> pathsPerSrc, Map<Node, Set<Path>> pathsPerDst,
                                     Integer reachMinS, Integer reachMinD, Map<Node, Integer> srcMinConnMap,
                                     Map<Node, Integer> dstMinConnMap, Map<SourceDestPair, Integer> pairMinConnMap,
                                     Integer nfe) {
        // Terminating conditions
        // srcViable = minS <= srcSurvive - srcDisconn || srcConn = |S|
        // dstViable = minD <= dstSurvive - dstDisconn || dstConn = |D|
        // Combined viable: if(|S| > |D|) {srcViable = srcViable || (dstViable && D subset F)}
        //                  else if (|D| > |S|) {dstViable = dstViable || (srcViable && S subset F)}
        // minForS = minC_s <= numC_s - numDis_s forall s
        // minForD = minC_s <= numC_d - numDis_d forall d
        // minForSD = minC_sd <= numC_sd - numDis_sd forall sd
        Set<Node> sources = intactPathsPerSrc.keySet();
        boolean enoughSrcs = evaluateMemberSet(sources, protectedSrcs, failedSrcs, intactPathsPerSrc, pathsPerSrc, reachMinS, nfe);
        Set<Node> dsts = intactPathsPerDst.keySet();
        boolean enoughDsts = evaluateMemberSet(dsts, protectedDsts, failedDsts, intactPathsPerDst, pathsPerDst, reachMinD, nfe);


        /*
        long protectedIntactSrcs = intactPathsPerSrc.keySet().stream().filter(protectedSrcs::contains).filter(s -> intactPathsPerSrc.get(s) > 0).count();
        long protectedIntactDsts = intactPathsPerDst.keySet().stream().filter(protectedSrcs::contains).filter(d -> intactPathsPerDst.get(d) > 0).count();
        boolean enoughSrcs = numViableSrcs >= reachMinS;
        boolean enoughDsts = numViableDsts >= reachMinD;
        */

        boolean enoughForAllS = srcMinConnMap.keySet().stream()
                .allMatch(s -> intactPathsPerSrc.get(s) >= srcMinConnMap.get(s));

        boolean enoughForAllD = dstMinConnMap.keySet().stream()
                .allMatch(d -> intactPathsPerDst.get(d) >= dstMinConnMap.get(d));

        boolean enoughForAllPairs = pairMinConnMap.keySet().stream()
                .allMatch(p -> intactPathsPerPair.get(p) >= pairMinConnMap.get(p));

        return enoughSrcs && enoughDsts && enoughForAllS && enoughForAllD && enoughForAllPairs;
    }

    private boolean evaluateMemberSet(Set<Node> members, Set<Node> protectedMembers, Set<Node> failedMembers,
                                      Map<Node, Integer> intactPathsPerMember, Map<Node, Set<Path>> pathsPerMember,
                                      Integer reachMin, Integer nfe){
        int numProtectedIntact = 0;
        int numFailedIntact = 0;
        int numProtectedDisconn= 0;
        int numFailedDisconn = 0;
        for(Node mem : members){
            if(protectedMembers.contains(mem)){
                if(intactPathsPerMember.get(mem) > 0){
                    numProtectedIntact++;
                } else if(pathsPerMember.get(mem).size() > 0){
                    numProtectedDisconn++;
                }
            } else if(failedMembers.contains(mem)){
                if(intactPathsPerMember.get(mem) > 0){
                    numFailedIntact++;
                } else if(pathsPerMember.get(mem).size() > 0){
                    numFailedDisconn++;
                }
            }
        }
        int size = members.size();
        boolean enough = false;
        if(numProtectedIntact >= reachMin){
            enough = true;
        }
        else if(numProtectedIntact + numFailedIntact >= Math.min(reachMin + nfe, size)) {
            enough = true;
        }
        else if(numProtectedIntact + numFailedIntact + numProtectedDisconn >= Math.min(reachMin + nfe, size)){
            enough = true;
        }
        else if(numProtectedIntact + numFailedIntact + numProtectedDisconn + numFailedDisconn >= Math.min(reachMin + nfe, size)){
            enough = true;
        }
        return enough;
    }

    private List<SourceDestPair> sortPairs(Map<SourceDestPair, Integer> intactPathCount,
                                           Map<SourceDestPair, Double> totalWeights, Set<SourceDestPair> srcDstDontFail,
                                           Set<SourceDestPair> srcFails, Set<SourceDestPair> dstFails,
                                           Set<SourceDestPair> srcDstFail, int numSources, int numDsts) {
        List<SourceDestPair> sortedSrcFails = srcFails.stream()
                .sorted(Comparator.comparing(intactPathCount::get).reversed().thenComparing(totalWeights::get))
                .collect(Collectors.toList());
        List<SourceDestPair> sortedDstFails = dstFails.stream()
                .sorted(Comparator.comparing(intactPathCount::get).reversed().thenComparing(totalWeights::get))
                .collect(Collectors.toList());
        List<SourceDestPair> sortedBothFail = srcDstFail.stream()
                .sorted(Comparator.comparing(intactPathCount::get).reversed().thenComparing(totalWeights::get))
                .collect(Collectors.toList());

        List<SourceDestPair> sorted = srcDstDontFail.stream()
                .sorted(Comparator.comparing(intactPathCount::get).reversed().thenComparing(totalWeights::get))
                .collect(Collectors.toList());
        if(numSources < numDsts || numSources == numDsts){
            sorted.addAll(sortedDstFails);
            sorted.addAll(sortedSrcFails);
        }
        else{
            sorted.addAll(sortedSrcFails);
            sorted.addAll(sortedDstFails);
        }
        sorted.addAll(sortedBothFail);
        return sorted;
    }


    // Only consider non src/dst failures
    private int getNumDisconn(Node src, Node dst, Collection<Path> paths, List<List<Failure>> failureGroups){
        int greatestDisconn = 0;
        for(List<Failure> group : failureGroups){
            int disconnPaths = 0;
            List<Failure> filtered = group.stream()
                    .filter(f -> f.getLink() != null || !(f.getNode().equals(src) || f.getNode().equals(dst)))
                    .collect(Collectors.toList());
            for(Path path : paths) {
                List<Node> reachablenodes = pathMappingService.getReachableNodes(path, filtered);
                if(!reachablenodes.contains(dst)){
                    disconnPaths++;
                }
            }
            greatestDisconn = Math.max(greatestDisconn, disconnPaths);
        }
        return greatestDisconn;
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
