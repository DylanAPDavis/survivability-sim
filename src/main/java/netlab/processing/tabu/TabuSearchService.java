package netlab.processing.tabu;


import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.*;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TabuSearchService {

    private TopologyMetricsService topologyMetricsService;

    @Autowired
    public TabuSearchService(TopologyMetricsService topologyMetricsService){
        this.topologyMetricsService = topologyMetricsService;
    }


    public Details solve(Request request, Topology topology) {
        Details details = request.getDetails();

        Failures failCollection = details.getFailures();
        NumFailureEvents nfeCollection = details.getNumFailureEvents();
        Connections connCollection = details.getConnections();
        TopologyMetrics topologyMetrics = topologyMetricsService.generateMetrics(topology);
        long startTime = System.nanoTime();
        Map<SourceDestPair, Map<String, Path>> paths = findPaths(details.getPairs(), failCollection, nfeCollection.getTotalNumFailureEvents(), connCollection,
                request.getFailureClass(), request.getTrafficCombinationType(), topology, request.getSeed(), topologyMetrics);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        log.info("Solution took: " + duration + " seconds");
        details.setChosenPaths(paths);
        details.setRunningTimeSeconds(duration);
        details.setIsFeasible(paths.values().stream().noneMatch(Map::isEmpty));
        return details;
    }

    //TODO: Consider failure class
    //TODO: Consider traffic combination type
    //TODO: Use seed to pick randomly
    private Map<SourceDestPair,Map<String,Path>> findPaths(Set<SourceDestPair> pairs, Failures failCollection,
                                                           Integer nfe, Connections connCollection,
                                                           FailureClass failureClass,
                                                           TrafficCombinationType trafficCombinationType, Topology topology,
                                                           Long seed, TopologyMetrics topologyMetrics) {

        Set<String> failureIds = convertFailuresToIds(failCollection.getFailureSet());
        Double fitnessThreshold = calculateFitnessThreshold(connCollection, failureIds, nfe);
        Set<Set<String>> failureGroupIds = convertGroupToIds(failCollection.getFailureGroups());
        Solution bestSolution = tabuSearch(topologyMetrics, pairs, failureIds, nfe, fitnessThreshold, connCollection);

        return convertToMap(bestSolution, topologyMetrics.getPathIdMap());

    }

    private Solution tabuSearch(TopologyMetrics topologyMetrics, Set<SourceDestPair> pairs,
                                Set<String> failureIds, Integer nfe, Double fitnessThreshold,
                                Connections connectReqs){
        Solution bestSolution = new Solution(new HashSet<>(), Double.MAX_VALUE, 0.0, new HashMap<>());
        Solution currentSolution =  new Solution(new HashSet<>(), Double.MAX_VALUE, 0.0, new HashMap<>());

        Set<String> disconnPathIds = new HashSet<>();

        int numInterationsWithoutChange = 0;
        while(numInterationsWithoutChange < 5){
            //TODO: Consider a tabu list while generating candidates
            List<Solution> candidateSolutions = generateCandidateSolutions(currentSolution,  topologyMetrics, pairs,
                    failureIds, nfe, connectReqs, disconnPathIds);
            Solution bestCandidate = pickBestCandidate(candidateSolutions, fitnessThreshold);
            boolean changed = false;
            //TODO: Include change in a tabu list
            if(isBetter(bestCandidate, currentSolution, fitnessThreshold)){
                currentSolution = bestCandidate;
                if(isBetter(bestCandidate, bestSolution, fitnessThreshold)){
                    bestSolution = bestCandidate;
                    changed = true;
                }
            }
            if(!changed) {
                numInterationsWithoutChange++;
            }
        }

        return bestSolution;
    }


    private Set<Set<String>> convertGroupToIds(Collection<List<Failure>> failureGroups) {
        Set<Set<String>> idGroups = new HashSet<>();
        for(Collection<Failure> failureGroup : failureGroups){
            Set<String> ids = convertFailuresToIds(failureGroup);
            idGroups.add(ids);
        }
        return idGroups;
    }

    private Set<String> convertFailuresToIds(Collection<Failure> failures){
        return failures.stream().map(Failure::getId).collect(Collectors.toSet());
    }

    private boolean isBetter(Solution candidate, Solution solution, Double fitnessThreshold) {
        return candidate.getCost().equals(solution.getCost()) && candidate.getFitness() > solution.getFitness() ||
                (candidate.getCost() / candidate.getFitness()) < (solution.getCost() / solution.getFitness());

    }

    private Solution pickBestCandidate(List<Solution> candidateSolutions, Double fitnessThreshold) {
        //TODO: Pick the best candidate based on fitness and cost
        Solution best = candidateSolutions.get(0);
        if(candidateSolutions.size() > 1) {
            for (int i = 1; i < candidateSolutions.size(); i++) {
                Solution otherSolution = candidateSolutions.get(i);
                if(isBetter(otherSolution, best, fitnessThreshold)){
                    best = otherSolution;
                }
            }
        }
        //TODO: Introduce ability to select worse solutions to "reset" search
        return best;
    }

    private List<Solution> generateCandidateSolutions(Solution currentSolution, TopologyMetrics topologyMetrics, Set<SourceDestPair> pairs,
                                                      Set<String> failureIds, Integer nfe, Connections connectionReqs,
                                                      Set<String> disconnPaths) {

        Map<SourceDestPair, List<String>> kShortestPaths = topologyMetrics.getMinCostPaths();
        Map<String, Path> pathIdMap = topologyMetrics.getPathIdMap();
        Set<String> currentPathIds = currentSolution.getPathIds();

        List<Solution> candidates = new ArrayList<>();

        for(SourceDestPair pair : pairs){
            List<String> kPathsForPair = kShortestPaths.get(pair);
            for(String pathId : kPathsForPair){
                // If this path isn't currently in use, create a new solution using this path
                if(!currentPathIds.contains(pathId)){
                    Set<String> candidatePathIds = new HashSet<>(currentPathIds);
                    candidatePathIds.add(pathId);
                    Map<SourceDestPair, Set<String>> pairPathMap = makePairPathMap(candidatePathIds, pathIdMap);
                    Double fitness = getFitness(candidatePathIds, pathIdMap, failureIds, nfe, connectionReqs, disconnPaths, pairPathMap);
                    Solution candidate = new Solution(candidatePathIds, getCost(candidatePathIds, pathIdMap), fitness, pairPathMap);
                    candidates.add(candidate);
                }
            }
        }
        return candidates;
    }

    private Map<SourceDestPair,Set<String>> makePairPathMap(Set<String> candidatePathIds, Map<String, Path> pathIdMap) {
        Map<SourceDestPair, Set<String>> pairPathMap = new HashMap<>();
        for(String pathId : candidatePathIds){
            Path path = pathIdMap.get(pathId);
            SourceDestPair pair = getPairFromPath(path);
            pairPathMap.putIfAbsent(pair, new HashSet<>());
            pairPathMap.get(pair).add(pathId);
        }
        return pairPathMap;
    }

    private Double getFitness(Set<String> candidatePathIds, Map<String, Path> pathIdMap, Set<String> failureIds, Integer nfe,
                              Connections connectionReqs, Set<String> disconnIds, Map<SourceDestPair,Set<String>> pairPathMap){
        Set<Path> paths = candidatePathIds.stream().map(pathIdMap::get).collect(Collectors.toSet());
        Integer useMinS = connectionReqs.getUseMinS();
        Integer useMinD = connectionReqs.getUseMinD();
        Map<Node, Integer> minCsMap = connectionReqs.getSrcMinConnectionsMap();
        Map<Node, Integer> minCdMap = connectionReqs.getDstMinConnectionsMap();
        Map<SourceDestPair, Integer> minCsdMap = connectionReqs.getPairMinConnectionsMap();
        for(Path path : paths){
            if(!disconnIds.contains(path.getId())) {
                if (path.containsFailureIds(failureIds)) {
                    disconnIds.add(path.getId());
                    break;
                }
            }
        }

        return calculateFitness(pairPathMap, pathIdMap, failureIds, nfe, disconnIds);

    }

    private Double calculateFitness(Map<SourceDestPair, Set<String>> pairPathMap, Map<String, Path> pathIdMap,
                                    Set<String> failuresIds, Integer nfe, Set<String> disconnIds){
        // Variables for tracking constraint satisfaction
        Map<Node, Set<String>> protectedCPerSrc = new HashMap<>();
        Map<Node, Set<String>> protectedCPerDst = new HashMap<>();
        Map<SourceDestPair, Set<String>> protectedCPerPair = new HashMap<>();
        Map<Node, Set<String>> fgDisjointCPerSrc = new HashMap<>();
        Map<Node, Set<String>> fgDisjointCPerDst = new HashMap<>();
        Map<SourceDestPair, Set<String>> fgDisjointCPerPair = new HashMap<>();

        // Determine which paths per src/dst/pair are protected, or can fail but are FG-disjoint from each other
        fillInPathMaps(protectedCPerSrc, protectedCPerDst, protectedCPerPair, fgDisjointCPerSrc, fgDisjointCPerDst, fgDisjointCPerPair,
                pairPathMap, pathIdMap, failuresIds, disconnIds);
        // With the worst FG, find if the requirements are met
        // Have to consider this on a pair by pair basis
        // Even if a path gets disconnected, it's still relevant

        // Fitness:
        // numSatisfiedS + numSatisfiedD + sum( (Protected + |FG-disjoint / nfe|) foreach s, d, (s,d) )
        // s in s or d in d is satisfied if there is at least one path left over after FG removal
        Set<Node> memberNodes = new HashSet<>(protectedCPerSrc.keySet());
        memberNodes.addAll(protectedCPerDst.keySet());
        int satisfiedSCount = 0;
        int satisfiedDCount = 0;
        double totalScore = 0.0;
        int nfeBase = nfe + 1;
        // Add to the score for each source and destination node
        for(Node node : memberNodes){
            double score = 0.0;
            if(protectedCPerSrc.containsKey(node)){
                score = protectedCPerSrc.get(node).size() + (1.0 * fgDisjointCPerSrc.get(node).size() / nfeBase);
                if(score >= 1){
                    satisfiedSCount++;
                }
            }
            if(protectedCPerDst.containsKey(node)){
                score = protectedCPerDst.get(node).size() + (1.0 * fgDisjointCPerDst.get(node).size() / nfeBase);
                if(score >= 1){
                    satisfiedDCount++;
                }
            }
            totalScore += score;
        }
        // Add to the score for each pair
        for(SourceDestPair pair : protectedCPerPair.keySet()){
            double score = protectedCPerPair.get(pair).size() + (1.0 * fgDisjointCPerPair.get(pair).size() / nfeBase);
            totalScore += score;
        }
        totalScore+= satisfiedSCount;
        totalScore += satisfiedDCount;

        return totalScore;
    }

    private void fillInPathMaps(Map<Node, Set<String>> protectedCPerSrc, Map<Node, Set<String>> protectedCPerDst,
                                Map<SourceDestPair, Set<String>> protectedCPerPair, Map<Node, Set<String>> fgDisjointCPerSrc,
                                Map<Node, Set<String>> fgDisjointCPerDst, Map<SourceDestPair, Set<String>> fgDisjointCPerPair,
                                Map<SourceDestPair, Set<String>> pairPathMap, Map<String, Path> pathIdMap,
                                Set<String> worstFG, Set<String> worstDisconnectedPathIds) {
        for(SourceDestPair pair : pairPathMap.keySet()){
            Set<String> pathIds = pairPathMap.get(pair);
            Node src = pair.getSrc();
            Node dst = pair.getDst();
            protectedCPerSrc.putIfAbsent(src, new HashSet<>());
            protectedCPerDst.putIfAbsent(dst, new HashSet<>());
            protectedCPerPair.putIfAbsent(pair, new HashSet<>());
            fgDisjointCPerSrc.putIfAbsent(src, new HashSet<>());
            fgDisjointCPerDst.putIfAbsent(dst, new HashSet<>());
            fgDisjointCPerPair.putIfAbsent(pair, new HashSet<>());
            Set<String> srcProtectedPaths = protectedCPerSrc.get(src);
            Set<String> dstProtectedPaths =  protectedCPerDst.get(dst);
            Set<String> pairProtectedPaths =  protectedCPerPair.get(pair);
            Set<String> srcFGDisjointPaths = fgDisjointCPerSrc.get(src);
            Set<String> dstFGDisjointPaths =  fgDisjointCPerDst.get(dst);
            Set<String> pairFGDisjointPaths =  fgDisjointCPerPair.get(pair);

            // Evaluate each path, consider if it can or cannot be disconnected
            for(String pathId : pathIds){
                if(worstDisconnectedPathIds.contains(pathId)){
                    // Compare this new path to the current roster of FG-disjoint paths
                    Path current = pathIdMap.get(pathId);
                    // FOR NOW: Do not include src or dst when considering viability
                    Set<String> fgInCurrent = worstFG.stream().filter(f -> !src.getId().equals(f) && !dst.getId().equals(f)).filter(current::containsFailureId).collect(Collectors.toSet());
                    // Add this path to the ongoing sets if it is FG-disjoint from current sets
                    srcFGDisjointPaths = evaluateFGDisjointPaths(srcFGDisjointPaths, current, fgInCurrent, pathIdMap);
                    dstFGDisjointPaths = evaluateFGDisjointPaths(dstFGDisjointPaths, current, fgInCurrent, pathIdMap);
                    pairFGDisjointPaths = evaluateFGDisjointPaths(pairFGDisjointPaths, current, fgInCurrent, pathIdMap);
                } else{
                    srcProtectedPaths.add(pathId);
                    dstProtectedPaths.add(pathId);
                    pairProtectedPaths.add(pathId);
                }
            }
        }
    }

    private Set<String> evaluateFGDisjointPaths(Set<String> fgDisjointPaths, Path current, Set<String> fgInCurrent,
                                                Map<String, Path> pathIdMap) {
        boolean notShared = true;
        for(String disjointId : fgDisjointPaths){
            Path disjointPath = pathIdMap.get(disjointId);
            if(disjointPath.containsFailureIds(fgInCurrent)){
                notShared = false;
                break;
            }
        }
        if(notShared){
            fgDisjointPaths.add(current.getId());
        }
        return fgDisjointPaths;
    }

    private Double getCost(Set<String> candidatePathIds, Map<String, Path> pathIdMap) {
        Double totalCost = 0.0;
        for(String id : candidatePathIds){
            Path path = pathIdMap.get(id);
            totalCost += path.getTotalWeight();
        }
        return totalCost;
    }

    private Map<SourceDestPair,Map<String,Path>> convertToMap(Solution bestSolution,
                                                              Map<String, Path> pathIdMap) {
        Map<SourceDestPair,Map<String,Path>> outputMap = new HashMap<>();
        for(String id : bestSolution.getPathIds()){
            Path path = pathIdMap.get(id);
            SourceDestPair pair = getPairFromPath(path);
            outputMap.putIfAbsent(pair, new HashMap<>());
            outputMap.get(pair).put(path.getId(), path);
        }
        return outputMap;
    }

    // Calculate how many constraints need to be satisfied
    // Number of sources that need at least one conn, number of dests that need at least one conn, number of pairs that
    // need at least one conn
    private Double calculateFitnessThreshold(Connections connCollection, Set<String> failureIds, Integer nfe) {
        Integer useMinS = connCollection.getUseMinS();
        Integer useMinD = connCollection.getUseMinD();
        Map<Node, Integer> minCsMap = connCollection.getSrcMinConnectionsMap();
        Map<Node, Integer> minCdMap = connCollection.getDstMinConnectionsMap();
        Map<SourceDestPair, Integer> minCsdMap = connCollection.getPairMinConnectionsMap();

        /*Integer numSWithAtLeastOneConn = Integer.parseInt(String.valueOf(minCsMap.values().stream().filter(v -> v > 0).count()));
        Integer numDWithAtLeastOneConn = Integer.parseInt(String.valueOf(minCdMap.values().stream().filter(v -> v > 0).count()));
        Integer numPairsWithAtLeastOneConn = Integer.parseInt(String.valueOf(minCsdMap.values().stream().filter(v -> v > 0).count()));


        // Take the max between num S that need a conn vs useMinS
        Integer neededS = Math.max(useMinS, numSWithAtLeastOneConn);
        // Take the max between numD that need a conn vs useMinD
        Integer neededD = Math.max(useMinD, numDWithAtLeastOneConn);

        */
        int neededS = calculateNeededMembers(minCsMap.keySet(), useMinS, failureIds, nfe);
        int neededD = calculateNeededMembers(minCdMap.keySet(), useMinD, failureIds, nfe);
        int neededSPaths = minCsMap.keySet().stream().mapToInt(minCsMap::get).sum();
        int neededDPaths = minCdMap.keySet().stream().mapToInt(minCdMap::get).sum();
        int neededSDPaths = minCsdMap.keySet().stream().mapToInt(minCsdMap::get).sum();
        return 1.0 * neededS + neededD + neededSPaths + neededDPaths + neededSDPaths;
    }

    private int calculateNeededMembers(Set<Node> nodes, Integer useMin, Set<String> failureIds, Integer nfe) {
        /*
        param minDestFailures = min(card(DinF), nfe);
        param maxDestsNotFailed = useMinD;#min(useMinD, card(DnotF));
        param dRequired = minDestFailures + maxDestsNotFailed;
         */
        Set<String> nodesInF = nodes.stream().map(Node::getId).filter(failureIds::contains).collect(Collectors.toSet());
        int minFailures = Math.min(nodesInF.size(), nfe);
        int maxNotFailed = useMin;
        return minFailures + maxNotFailed;
    }


    private SourceDestPair getPairFromPath(Path path) {
        Node src = path.getNodes().get(0);
        Node dst = path.getNodes().get(path.getNodes().size()-1);
        return new SourceDestPair(src, dst);
    }
}
