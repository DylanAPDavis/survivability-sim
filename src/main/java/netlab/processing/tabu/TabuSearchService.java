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

    static int partitionSize = 20;
    static int swapChance = 0;
    static int pruneChance = 50;

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

    private Map<SourceDestPair,Map<String,Path>> findPaths(Set<SourceDestPair> pairs, Failures failCollection,
                                                           Integer nfe, Connections connCollection,
                                                           FailureClass failureClass,
                                                           TrafficCombinationType trafficCombinationType, Topology topology,
                                                           Long seed, TopologyMetrics topologyMetrics) {

        Set<String> failureIds = convertFailuresToIds(failCollection.getFailureSet());
        Double fitnessThreshold = calculateFitnessThreshold(connCollection, failureIds, nfe);
        Random random = new Random(seed);
        Solution bestSolution = tabuSearch(topologyMetrics, pairs, failureIds, nfe, fitnessThreshold, connCollection,
                random, failureClass);

        return convertToMap(bestSolution, topologyMetrics.getPathIdMap());

    }

    private Solution tabuSearch(TopologyMetrics topologyMetrics, Set<SourceDestPair> pairs,
                                Set<String> failureIds, Integer nfe, Double fitnessThreshold,
                                Connections connectReqs, Random random, FailureClass failureClass){
        Solution bestSolution = new Solution(new HashSet<>(), Double.MAX_VALUE, 0.0, new HashMap<>());
        Solution currentSolution =  new Solution(new HashSet<>(), Double.MAX_VALUE, 0.0, new HashMap<>());

        Set<String> disconnPathIds = new HashSet<>();

        Map<Set<String>, Double> pathSetFitnessMap = new HashMap<>();
        pathSetFitnessMap.put(new HashSet<>(), 0.00000000001);
        Map<Set<String>, Double> pathSetCostMap = new HashMap<>();
        pathSetCostMap.put(new HashSet<>(), Double.MAX_VALUE);
        int numInterationsWithoutChange = 0;
        while(numInterationsWithoutChange < 5){
            //TODO: Consider a tabu list while generating candidates
            List<Solution> candidateSolutions = generateCandidateSolutions(currentSolution,  topologyMetrics, pairs,
                    failureIds, nfe, connectReqs, disconnPathIds, pathSetFitnessMap, pathSetCostMap, random, failureClass,
                    fitnessThreshold);
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
            // If there's been a change, reset the counter
            numInterationsWithoutChange = changed ? 0 : numInterationsWithoutChange + 1;
        }

        return bestSolution;
    }


    private Set<String> convertFailuresToIds(Collection<Failure> failures){
        return failures.stream().map(Failure::getId).collect(Collectors.toSet());
    }

    private boolean isBetter(Solution candidate, Solution solution, Double fitnessThreshold) {
        return isBetter(candidate.getCost(), candidate.getFitness(), solution.getCost(), solution.getFitness(), fitnessThreshold);
    }

    private boolean isBetter(double canCost, double canFit, double curCost, double curFit, double thresh){
        boolean equalCostMoreFit = canCost == curCost && canFit > curFit;
        boolean lowerCostEnoughFit = canCost < curCost && canFit >= thresh;
        boolean moreFitBothBelowThreshold = canFit > curFit && canFit < thresh && curFit < thresh;
        boolean greaterCostFitEnough = curFit >= thresh && canCost > curCost;
        double candidateRatio = canCost / canFit;
        double solutionRatio = curCost / curFit;
        return !greaterCostFitEnough &&
                (equalCostMoreFit || lowerCostEnoughFit || moreFitBothBelowThreshold || candidateRatio < solutionRatio);
    }

    private Solution pickBestCandidate(List<Solution> candidateSolutions, Double fitnessThreshold) {
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
                                                      Set<String> disconnPaths, Map<Set<String>, Double> pathSetFitnessMap,
                                                      Map<Set<String>, Double> pathSetCostMap, Random random, FailureClass failureClass,
                                                      Double fitThreshold) {

        Map<SourceDestPair, List<String>> kShortestPaths = topologyMetrics.getMinCostPaths();
        //Map<SourceDestPair, List<String>> lDisjointPaths = topologyMetrics.getLinkDisjointPaths();
        //Map<SourceDestPair, List<String>> nDisjointPaths = topologyMetrics.getNodeDisjointPaths();
        Map<String, Path> pathIdMap = topologyMetrics.getPathIdMap();
        Set<String> currentPathIds = currentSolution.getPathIds();

        List<Solution> candidates = new ArrayList<>();

        boolean swap = random.nextInt(100) < swapChance && currentPathIds.size() > 1;
        String worstPath = swap ?  pickWorst(currentSolution, pathIdMap, failureIds, nfe, connectionReqs, disconnPaths, pathSetFitnessMap,
                pathSetCostMap, fitThreshold) : "";
        // Otherwise
        for(SourceDestPair pair : pairs){
            List<String> kPathsForPair = kShortestPaths.get(pair);
            List<String> subset = getSubset(kPathsForPair, partitionSize, random);
            //List<String> disjointSubset = getDisjointSubset(lDisjointPaths.get(pair), nDisjointPaths.get(pair), partitionSize/2, random,
            //        failureClass, failureIds.size());
            //subset.addAll(disjointSubset);
            for(String pathId : subset){
                // If this path isn't currently in use, create a new solution using this path
                Set<String> candidatePathIds = swap ? swapPath(currentSolution, pathId, worstPath) : addPath(currentSolution, pathId);
                Solution candidate = makeCandidate(candidatePathIds, pathIdMap, failureIds, nfe, connectionReqs,
                        disconnPaths, pathSetFitnessMap, pathSetCostMap);
                pathSetFitnessMap.putIfAbsent(candidatePathIds, candidate.getFitness());
                pathSetCostMap.putIfAbsent(candidatePathIds, candidate.getCost());
                candidates.add(candidate);
            }
        }
        return candidates;
    }


    private List<String> getDisjointSubset(List<String> lDisjointPaths, List<String> nDisjointPaths, int subsetSize,
                                           Random random, FailureClass failureClass, int numFails) {
        List<String> subset = new ArrayList<>();
        if(numFails != 0) {
            if (failureClass.equals(FailureClass.Both) || failureClass.equals(FailureClass.Node)) {
                subset.addAll(getSubset(nDisjointPaths, subsetSize, random));
            }
            if (failureClass.equals(FailureClass.Both) || failureClass.equals(FailureClass.Link)) {
                subset.addAll(getSubset(lDisjointPaths, subsetSize, random));
            }
        }
        return subset;
    }

    private List<String> getSubset(List<String> candidates, int subsetSize, Random random) {
        int numPartitions = (int) Math.ceil(1.0 * candidates.size() / subsetSize);
        int partitionPos = random.nextInt(numPartitions);
        int lowerBound = partitionPos * subsetSize;
        int upperBound = lowerBound + subsetSize <= candidates.size() ? lowerBound + subsetSize : candidates.size();
        return candidates.subList(lowerBound, upperBound);
    }

    private Solution makeCandidate(Set<String> candidatePathIds, Map<String, Path> pathIdMap, Set<String> failureIds,
                                   Integer nfe, Connections connectionReqs, Set<String> disconnPaths,
                                   Map<Set<String>, Double> pathSetFitnessMap, Map<Set<String>, Double> pathSetCostMap){
        Map<SourceDestPair, Set<String>> pairPathMap = makePairPathMap(candidatePathIds, pathIdMap);
        Double cost = pathSetCostMap.containsKey(candidatePathIds) ? pathSetCostMap.get(candidatePathIds) :
                getCost(candidatePathIds, pathIdMap);
        Double fitness = pathSetFitnessMap.containsKey(candidatePathIds) ? pathSetFitnessMap.get(candidatePathIds) :
                getFitness(candidatePathIds, pathIdMap, failureIds, nfe, connectionReqs, disconnPaths, pairPathMap);

        return new Solution(candidatePathIds, cost, fitness, pairPathMap);
    }

    private Set<String> addPath(Solution base, String pathId){
        Set<String> candidatePathIds = new HashSet<>(base.getPathIds());
        candidatePathIds.add(pathId);
        return candidatePathIds;
    }

    private Set<String> swapPath(Solution base, String newPathId, String worstPath){
        Set<String> pathIds = new HashSet<>(base.getPathIds());
        if(worstPath.equals("")){
            pathIds.add(newPathId);
        } else{
            pathIds.remove(worstPath);
            pathIds.add(newPathId);
        }
        return pathIds;
    }

    private Set<String> prunePath(Solution base, Map<String, Path> pathIdMap, Set<String> failureIds,
                                  Integer nfe, Connections connectionReqs, Set<String> disconnPaths,
                                  Map<Set<String>, Double> pathSetFitnessMap, Map<Set<String>, Double> pathSetCostMap,
                                  Double fitnessThreshold){
        Set<String> pathIds = new HashSet<>(base.getPathIds());
        String worstPath = pickWorst(base, pathIdMap, failureIds, nfe, connectionReqs, disconnPaths, pathSetFitnessMap,
                pathSetCostMap, fitnessThreshold);
        if(!worstPath.equals("")){
            pathIds.remove(worstPath);
        }
        return pathIds;
    }

    private String pickWorst(Solution base, Map<String, Path> pathIdMap, Set<String> failureIds,
                             Integer nfe, Connections connectionReqs, Set<String> disconnPaths,
                             Map<Set<String>, Double> pathSetFitnessMap, Map<Set<String>, Double> pathSetCostMap,
                             Double fitnessThreshold){
        String worstPath = "";
        double worstCost = 0.0;
        double worstFit = Double.MAX_VALUE;
        for(String pathId : base.getPathIds()){
            Set<String> modPathIds = new HashSet<>(base.getPathIds());
            modPathIds.remove(pathId);
            Double cost;
            Double fitness;
            if(pathSetCostMap.containsKey(modPathIds) && pathSetFitnessMap.containsKey(modPathIds)){
                cost = pathSetCostMap.get(modPathIds);
                fitness = pathSetFitnessMap.get(modPathIds);
            }
            else{
                Solution modCandidate = makeCandidate(modPathIds, pathIdMap, failureIds, nfe, connectionReqs,
                        disconnPaths, pathSetFitnessMap, pathSetCostMap);
                cost = modCandidate.getCost();
                fitness = modCandidate.getFitness();
                // Store this new pathset's cost and fitness for later use
                pathSetCostMap.put(modPathIds, cost);
                pathSetFitnessMap.put(modPathIds, fitness);
            }
            // If the current worst is better than this, make this the new current worst
            if(isBetter(worstCost, worstFit, cost, fitness, fitnessThreshold)){
                worstPath = pathId;
                worstCost = cost;
                worstFit = fitness;
            }
        }
        return worstPath;
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

    private double getFitness(Set<String> candidatePathIds, Map<String, Path> pathIdMap, Set<String> failureIds, Integer nfe,
                              Connections connectionReqs, Set<String> disconnIds, Map<SourceDestPair, Set<String>> pairPathMap){
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

        return calculateFitness(pairPathMap, pathIdMap, failureIds, nfe, disconnIds, minCsMap, minCdMap, minCsdMap);

    }

    private Double calculateFitness(Map<SourceDestPair, Set<String>> pairPathMap, Map<String, Path> pathIdMap,
                                    Set<String> failuresIds, Integer nfe, Set<String> disconnIds,
                                    Map<Node, Integer> minCsMap, Map<Node, Integer> minCdMap,
                                    Map<SourceDestPair, Integer> minCsdMap){
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
        for(Node node : protectedCPerSrc.keySet()) {
            double score = getScore(node, protectedCPerSrc, fgDisjointCPerSrc, nfeBase);
            if(score >= 1){
                satisfiedSCount++;
            }
            if(score > minCsMap.get(node)){
                //score = minCsMap.get(node);
            }
            totalScore += score;
        }
        for(Node node : protectedCPerDst.keySet()){
            double score = getScore(node, protectedCPerDst, fgDisjointCPerDst, nfeBase);
            if(score >= 1){
                satisfiedDCount++;
            }
            if(score > minCdMap.get(node)){
                //score = minCdMap.get(node);
            }
            totalScore += score;
        }
        // Add to the score for each pair
        for(SourceDestPair pair : protectedCPerPair.keySet()){
            double score = protectedCPerPair.get(pair).size() + (1.0 * fgDisjointCPerPair.get(pair).size() / nfeBase);
            if(score > minCsdMap.get(pair)){
                //score = minCsdMap.get(pair);
            }
            totalScore += score;
        }
        totalScore += satisfiedSCount;
        totalScore += satisfiedDCount;

        return totalScore;
    }

    private double getScore(Node node, Map<Node, Set<String>> protectedMap, Map<Node, Set<String>> disjointMap, int nfeBase) {
        double score = 0.0;
        if (protectedMap.containsKey(node)) {
            score = protectedMap.get(node).size() + (1.0 * disjointMap.get(node).size() / nfeBase);
        }
        return score;
    }

    private void fillInPathMaps(Map<Node, Set<String>> protectedCPerSrc, Map<Node, Set<String>> protectedCPerDst,
                                Map<SourceDestPair, Set<String>> protectedCPerPair, Map<Node, Set<String>> fgDisjointCPerSrc,
                                Map<Node, Set<String>> fgDisjointCPerDst, Map<SourceDestPair, Set<String>> fgDisjointCPerPair,
                                Map<SourceDestPair, Set<String>> pairPathMap, Map<String, Path> pathIdMap,
                                Set<String> fails, Set<String> disconnIds) {
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
                if(disconnIds.contains(pathId)){
                    // Compare this new path to the current roster of FG-disjoint paths
                    Path current = pathIdMap.get(pathId);
                    // FOR NOW: Do not include src or dst when considering viability
                    Set<String> fgInCurrent = fails.stream()
                            //.filter(f -> !src.getId().equals(f) && !dst.getId().equals(f))
                            .filter(current::containsFailureId)
                            .collect(Collectors.toSet());
                    // Add this path to the ongoing sets if it is FG-disjoint from current sets
                    fgInCurrent.remove(src.getId());
                    srcFGDisjointPaths = evaluateFGDisjointPaths(srcFGDisjointPaths, current, fgInCurrent, pathIdMap);
                    fgInCurrent.remove(dst.getId());
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
            totalCost +=  path.getTotalWeight();
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

        int neededS = calculateNeededMembers(minCsMap.keySet(), useMinS, failureIds, nfe);
        int neededD = calculateNeededMembers(minCdMap.keySet(), useMinD, failureIds, nfe);
        int neededSPaths = minCsMap.keySet().stream().mapToInt(minCsMap::get).sum();
        int neededDPaths = minCdMap.keySet().stream().mapToInt(minCdMap::get).sum();
        int neededSDPaths = minCsdMap.keySet().stream().mapToInt(minCsdMap::get).sum();
        return 1.0 * neededS + neededD + neededSPaths + neededDPaths + neededSDPaths;
    }

    private int calculateNeededMembers(Set<Node> nodes, Integer useMin, Set<String> failureIds, Integer nfe) {
        Set<String> nodesInF = nodes.stream().map(Node::getId).filter(failureIds::contains).collect(Collectors.toSet());
        int minFailures = Math.min(nodesInF.size(), nfe);
        int maxNotFailed = useMin;
        return Math.min(minFailures + maxNotFailed, nodes.size());
    }


    private SourceDestPair getPairFromPath(Path path) {
        Node src = path.getNodes().get(0);
        Node dst = path.getNodes().get(path.getNodes().size()-1);
        return new SourceDestPair(src, dst);
    }

}
