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

    // Number of paths to consider at once
    static int partitionSize = 5;
    static int minThresholdKeepAfterRestart = 5; // only keep parts of the best solution that have been around for this many iterations

    // Thresholds for stopping or changing large parts of the solution
    static int tabuTime = 10;       // if a path has been added or removed, the opposite action cannot be performed
    static int noImprovement = 20; // no improvement on best, stop running
    static int notFitEnough = 3;   // solution is not improving enough, inject some disjoint paths
    static int restartFromBest = 10;     // after so many iterations, restart from best subset of the best solution

    // Make connecting sources in solution a priority
    static int srcMultiplier = 100;

    // Update the Tabu list after injecting and resetting, along with baseline add, swap, and remove
    static boolean updateTabuAfterSpecialAction = true;

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
        Set<String> sourceIds = details.getSources().stream().map(Node::getId).collect(Collectors.toSet());
        Set<String> destIds = details.getDestinations().stream().map(Node::getId).collect(Collectors.toSet());
        long startTime = System.nanoTime();
        Map<SourceDestPair, Map<String, Path>> paths = findPaths(details.getPairs(), failCollection,
                nfeCollection.getTotalNumFailureEvents(), connCollection, request.getFailureClass(),  request.getSeed(),
                topologyMetrics, sourceIds, destIds);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        //log.info("Solution took: " + duration + " seconds");
        details.setChosenPaths(paths);
        details.setRunningTimeSeconds(duration);
        details.setIsFeasible(paths.values().stream().noneMatch(Map::isEmpty));
        return details;
    }

    private Map<SourceDestPair,Map<String,Path>> findPaths(Set<SourceDestPair> pairs, Failures failCollection,
                                                           Integer nfe, Connections connCollection,
                                                           FailureClass failureClass,
                                                           Long seed, TopologyMetrics topologyMetrics,
                                                           Set<String> sources, Set<String> destinations) {

        Set<String> failureIds = convertFailuresToIds(failCollection.getFailureSet());
        Double fitnessThreshold = calculateFitnessThreshold(connCollection, failureIds, nfe);
        Random random = new Random(seed);
        Solution bestSolution = tabuSearch(topologyMetrics, pairs, failureIds, nfe, fitnessThreshold, connCollection,
                random, failureClass, sources, destinations);

        return convertToMap(bestSolution, topologyMetrics.getPathIdMap());

    }

    private Solution tabuSearch(TopologyMetrics topologyMetrics, Set<SourceDestPair> pairs,
                                Set<String> failureIds, Integer nfe, Double fitnessThreshold,
                                Connections connectReqs, Random random, FailureClass failureClass, Set<String> sources,
                                Set<String> destinations){
        Solution bestSolution = new Solution(new HashSet<>(), Double.MAX_VALUE, -Double.MIN_VALUE, new HashMap<>(), new HashMap<>(), new HashMap<>());
        Solution currentSolution =  new Solution(new HashSet<>(), Double.MAX_VALUE, -Double.MIN_VALUE, new HashMap<>(), new HashMap<>(), new HashMap<>());

        Set<String> disconnPathIds = new HashSet<>();
        Map<String, Integer> tabuMap = new HashMap<>();

        Map<Set<String>, Double> pathSetFitnessMap = new HashMap<>();
        pathSetFitnessMap.put(new HashSet<>(), -Double.MIN_VALUE);
        Map<Set<String>, Double> pathSetCostMap = new HashMap<>();
        pathSetCostMap.put(new HashSet<>(), Double.MAX_VALUE);
        int iterationsWithoutImprovement = 0;
        int iterationsUnderThreshold = 0;
        int totalIterations = 0;
        Set<Node> usedSourcesForInjection = new HashSet<>();
        Set<SourceDestPair> usedPairsForInjection = new HashSet<>();
        Map<String, Integer> pathBeenInBestMap = new HashMap<>();
        currentSolution = injectDisjointPaths(currentSolution, topologyMetrics, pairs,
                failureIds,  nfe, connectReqs, new HashSet<>(), pathSetFitnessMap, pathSetCostMap, failureClass,
                sources, destinations, usedPairsForInjection, usedSourcesForInjection);

        Solution previousSolution = currentSolution.copy();
        while(iterationsWithoutImprovement < noImprovement){
            boolean changed = false;

            // If you've been under the threshold for too long, inject some disjoint paths
            /*if(iterationsUnderThreshold == notFitEnough){
                // Store the previous solution before changing the current
                previousSolution = currentSolution.copy();
                currentSolution = injectDisjointPaths(currentSolution, topologyMetrics, pairs,
                         failureIds,  nfe, connectReqs, disconnPathIds, pathSetFitnessMap, pathSetCostMap, failureClass,
                        sources, destinations, usedPairsForInjection, usedSourcesForInjection);
                iterationsUnderThreshold = 0;
            }
            // Otherwise, generate candidate solutions through remove/swap/add moves
            else {*/
                List<Solution> candidateSolutions = generateCandidateSolutions(currentSolution, topologyMetrics, pairs,
                        failureIds, nfe, connectReqs, disconnPathIds, pathSetFitnessMap, pathSetCostMap, random,
                        sources, destinations, tabuMap.keySet());
                Solution bestCandidate = pickBestCandidate(candidateSolutions, fitnessThreshold);
                if (isBetter(bestCandidate, currentSolution, fitnessThreshold)) {
                    // Store the previous solution before changing the current
                    previousSolution = currentSolution.copy();
                    currentSolution = bestCandidate;
                }
                // If you're only updating tabus on add/removes/swaps, do it here
                if(!updateTabuAfterSpecialAction) {
                    updateTabuMap(tabuMap, previousSolution, currentSolution);
                }
            //}
            // Update best solution (if applicable)
            if (isBetter(currentSolution, bestSolution, fitnessThreshold)) {
                bestSolution = currentSolution;
                changed = true;
            }
            iterationsWithoutImprovement = changed ? 0 : iterationsWithoutImprovement + 1;
            totalIterations++;

            // Update the counts for paths that have been in the best solution
            updatePathBeenInBestMap(pathBeenInBestMap, bestSolution);
            // If you've gone long enough, restart from the best paths
            if(totalIterations % restartFromBest == 0){
                previousSolution = currentSolution.copy();
                currentSolution = restartFromBestSubset(pathBeenInBestMap, topologyMetrics.getPathIdMap(), failureIds, nfe,
                        connectReqs, disconnPathIds, pathSetFitnessMap, pathSetCostMap, sources, destinations);
            }
            // If the current solution is not fit enough, track that
            if(currentSolution.getFitness() < fitnessThreshold){
                iterationsUnderThreshold++;
            }
            // Update tabus
            if(updateTabuAfterSpecialAction) {
                updateTabuMap(tabuMap, previousSolution, currentSolution);
            }
        }

        //log.info("Total iterations: " + totalIterations);
        return bestSolution;
    }

    private Solution restartFromBestSubset(Map<String, Integer> pathBeenInBestMap, Map<String, Path> pathIdMap,
                                           Set<String> failureIds, Integer nfe, Connections connectionReqs,
                                           Set<String> disconnPaths, Map<Set<String>, Double> pathSetFitnessMap,
                                           Map<Set<String>, Double> pathSetCostMap, Set<String> sources, Set<String> destinations) {

        // Get the path IDs that have been in the best solution long enough
        Set<String> bestPathIds = pathBeenInBestMap.keySet().stream()
                .filter(p -> pathBeenInBestMap.get(p) >= minThresholdKeepAfterRestart)
                .collect(Collectors.toSet());
        // Reset the counts for all paths
        for(String pathInMap : pathBeenInBestMap.keySet()){
            pathBeenInBestMap.put(pathInMap, 0);
        }

        // Return a new solution that is made up of "only the best paths"
        return createCandidateUpdateMaps(bestPathIds, pathIdMap, failureIds, nfe, connectionReqs,
                disconnPaths, pathSetFitnessMap, pathSetCostMap, sources, destinations);
    }

    private void updatePathBeenInBestMap(Map<String, Integer> pathBeenInBestMap, Solution bestSolution) {
        Set<String> pathIds = bestSolution.getPathIds();
        for(String pathId : pathIds){
            if(pathBeenInBestMap.containsKey(pathId)){
                pathBeenInBestMap.put(pathId, pathBeenInBestMap.get(pathId) + 1);
            } else{
                pathBeenInBestMap.put(pathId, 1);
            }
        }
        for(String pathInMap : pathBeenInBestMap.keySet()){
            if(!pathIds.contains(pathInMap)){
                pathBeenInBestMap.put(pathInMap, 0);
            }
        }
    }

    private void updateTabuMap(Map<String, Integer> tabuMap, Solution previousSolution,
                                Solution currentSolution) {
        Set<String> pathIds = currentSolution.getPathIds();
        Set<String> previousPathIds = previousSolution.getPathIds();
        decrementAndExpireTabus(tabuMap);
        // Compare current solution to previous solution to check for additions
        for(String pathId : pathIds){
            // If this is a new path, then put it in the TabuToRemove map
            if(!previousPathIds.contains(pathId)){
                tabuMap.put(pathId, tabuTime);
            }
        }
        // Compare previous solution to current solution to check for removals
        for(String prevPathId : previousPathIds){
            // If this path has been removed, then put it in the TabuToAdd map
            if(!pathIds.contains(prevPathId)){
                tabuMap.put(prevPathId, tabuTime);
            }
        }
    }

    private void decrementAndExpireTabus(Map<String, Integer> tabuMap){
        // Decrement tabus
        Set<String> tabusToRemove = new HashSet<>();
        for(String tabu : tabuMap.keySet()){
            Integer tabuCount = tabuMap.get(tabu) - 1;
            // Filter out the ones that have reached zero
            if(tabuCount > 0){
                tabuMap.put(tabu, tabuCount);
            } else{
                tabusToRemove.add(tabu);
            }
        }
        for(String tabu : tabusToRemove){
            tabuMap.remove(tabu);
        }
    }

    private Solution injectDisjointPaths(Solution currentSolution, TopologyMetrics topologyMetrics, Set<SourceDestPair> pairs,
                                         Set<String> failureIds, Integer nfe, Connections connectionReqs,
                                         Set<String> disconnPaths, Map<Set<String>, Double> pathSetFitnessMap,
                                         Map<Set<String>, Double> pathSetCostMap, FailureClass failureClass,
                                         Set<String> sources, Set<String> destinations, Set<SourceDestPair> usedPairsForInjection,
                                         Set<Node> usedSourcesForInjection) {
        Set<String> pathIds = new HashSet<>(currentSolution.getPathIds());
        Map<String, Path> pathIdMap = topologyMetrics.getPathIdMap();
        for(SourceDestPair pair : pairs){
            Set<String> pairPathIds = new HashSet<>();
            if (failureClass.equals(FailureClass.Both) || failureClass.equals(FailureClass.Node)) {
                pairPathIds.addAll(topologyMetrics.getNodeDisjointPaths().get(pair));
            }
            if (failureClass.equals(FailureClass.Both) || failureClass.equals(FailureClass.Link)) {
                pairPathIds.addAll(topologyMetrics.getNodeDisjointPaths().get(pair));
            }
            if(pairPathIds.size() > 0){
                pathIds.addAll(pairPathIds);
            }
        }
        /*Map<Node, Set<String>> srcPathMap = currentSolution.getSrcPathMap();
        List<SourceDestPair> filteredPairs = pairs.stream()
                .filter(p -> !usedPairsForInjection.contains(p))
                .filter(p -> !usedSourcesForInjection.contains(p.getSrc()))
                .sorted(Comparator.comparing(p -> srcPathMap.containsKey(p.getSrc()) ? srcPathMap.get(p.getSrc()).size() : 0))
                .collect(Collectors.toList());
        if(failureIds.size() != 0 && filteredPairs.size() > 0) {
            int numPairsChecked = 0;
            boolean pairChosen = false;
            while(!pairChosen && numPairsChecked < filteredPairs.size()) {
                SourceDestPair pair = filteredPairs.get(0);
                Set<String> pairPathIds = new HashSet<>();
                if (failureClass.equals(FailureClass.Both) || failureClass.equals(FailureClass.Node)) {
                    pairPathIds.addAll(topologyMetrics.getNodeDisjointPaths().get(pair));
                }
                if (failureClass.equals(FailureClass.Both) || failureClass.equals(FailureClass.Link)) {
                    pairPathIds.addAll(topologyMetrics.getNodeDisjointPaths().get(pair));
                }
                if(pairPathIds.size() > 0) {
                    pathIds.addAll(pairPathIds);
                    usedPairsForInjection.add(pair);
                    usedSourcesForInjection.add(pair.getSrc());
                    pairChosen = true;
                }
                else {
                    numPairsChecked++;
                }
            }
        }
        */
        return makeCandidate(pathIds, pathIdMap, failureIds, nfe, connectionReqs,
                disconnPaths, pathSetFitnessMap, pathSetCostMap, sources, destinations);
    }


    private Set<String> convertFailuresToIds(Collection<Failure> failures){
        return failures.stream().map(Failure::getId).collect(Collectors.toSet());
    }

    private boolean isBetter(Solution candidate, Solution solution, Double fitnessThreshold) {
        return isBetter(candidate.getCost(), candidate.getFitness(), solution.getCost(), solution.getFitness(), fitnessThreshold);
    }

    /* What makes a solution better?
       If their fitness is tied, then go with the lower cost one.
       If they're both below the fitness threshold, pick the more fit one.
       If one's above the threshold, and the other's not, pick that one.
       If they're both above the threshold, pick the lower cost one.
     */
    private boolean isBetter(double canCost, double canFit, double curCost, double curFit, double thresh){
        if(canFit == curFit){
            return canCost < curCost;
        }
        if(canFit < thresh && curFit < thresh){
            /*if(Math.abs(canFit - curFit) <= .1){
                return canCost < curCost;
            }*/
            return canFit > curFit;
        }
        if(canFit >= thresh && curFit >= thresh){
            return canCost < curCost;
        }
        else{
            return canFit > curFit;
        }
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
        return best;
    }

    private List<Solution> generateCandidateSolutions(Solution currentSolution, TopologyMetrics topologyMetrics, Set<SourceDestPair> pairs,
                                                      Set<String> failureIds, Integer nfe, Connections connectionReqs,
                                                      Set<String> disconnPaths, Map<Set<String>, Double> pathSetFitnessMap,
                                                      Map<Set<String>, Double> pathSetCostMap, Random random,
                                                      Set<String> sources, Set<String> destinations,
                                                      Set<String> tabu) {

        Map<SourceDestPair, List<String>> kShortestPaths = topologyMetrics.getMinCostPaths();
        Map<String, Path> pathIdMap = topologyMetrics.getPathIdMap();
        Set<String> currentPathIds = currentSolution.getPathIds();

        List<Solution> candidates = new ArrayList<>();
        List<String> currentAllowedToRemove = currentPathIds.stream()
                .filter(p -> !tabu.contains(p))
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

        // Create a solution by removing a candidate at random
        Solution removeCandidate = createRemoveCandidate(currentPathIds, currentAllowedToRemove, random, pathIdMap, failureIds, nfe, connectionReqs,
                disconnPaths, pathSetFitnessMap, pathSetCostMap, sources, destinations);
        candidates.add(removeCandidate);

        // Go through each pair
        for(SourceDestPair pair : pairs){
            // Select the k shortest paths for that pair
            List<String> kPathsForPair = kShortestPaths.get(pair);
            // Filter out the tabu paths and the locked in paths
            List<String> candidatePathsForPair = kPathsForPair.stream()
                    .filter(p -> !tabu.contains(p))
                    .collect(Collectors.toList());
            // Pick a random subset based on the partition size
            List<String> subset = getSubset(candidatePathsForPair, partitionSize, random);
            // Go through each path that from that subset
            for(String pathId : subset){
                // Swap this path for a randomly chosen path in the current solution
                Solution swapCandidate = createSwapCandidate(currentAllowedToRemove, random, currentPathIds, pathId,
                        pathIdMap, failureIds, nfe, connectionReqs, disconnPaths, pathSetFitnessMap, pathSetCostMap,
                        sources, destinations);
                candidates.add(swapCandidate);
                // Add in this new path if it's not currently contained in the solution
                Solution addCandidate = createAddCandidate(currentPathIds, pathId, pathIdMap, failureIds, nfe, connectionReqs,
                        disconnPaths, pathSetFitnessMap, pathSetCostMap, sources, destinations);
                candidates.add(addCandidate);
            }
        }
        return candidates;
    }

    private Solution createAddCandidate(Set<String> currentPathIds, String pathId, Map<String, Path> pathIdMap,
                                         Set<String> failureIds, Integer nfe, Connections connectionReqs, Set<String> disconnPaths,
                                         Map<Set<String>, Double> pathSetFitnessMap, Map<Set<String>, Double> pathSetCostMap,
                                         Set<String> sources, Set<String> destinations) {
        Set<String> addCandidatePathIds = addPath(currentPathIds, pathId);
        return createCandidateUpdateMaps(addCandidatePathIds, pathIdMap, failureIds, nfe, connectionReqs,
                disconnPaths, pathSetFitnessMap, pathSetCostMap, sources, destinations);
    }

    private Solution createSwapCandidate(List<String> currentAllowedToRemove, Random random, Set<String> currentPathIds,
                                         String pathId, Map<String, Path> pathIdMap, Set<String> failureIds, Integer nfe,
                                         Connections connectionReqs, Set<String> disconnPaths,
                                         Map<Set<String>, Double> pathSetFitnessMap, Map<Set<String>, Double> pathSetCostMap,
                                         Set<String> sources, Set<String> destinations) {
        String pathToSwapOut = chooseAtRandom(currentAllowedToRemove, random);
        Set<String> swapCandidatePathIds = swapPath(currentPathIds, pathId, pathToSwapOut);
        return createCandidateUpdateMaps(swapCandidatePathIds, pathIdMap, failureIds, nfe, connectionReqs,
                disconnPaths, pathSetFitnessMap, pathSetCostMap, sources, destinations);
    }

    private Solution createRemoveCandidate(Set<String> currentPathIds, List<String> currentAllowedToRemove, Random random,
                                           Map<String, Path> pathIdMap, Set<String> failureIds, Integer nfe,
                                           Connections connectionReqs, Set<String> disconnPaths,
                                           Map<Set<String>, Double> pathSetFitnessMap, Map<Set<String>, Double> pathSetCostMap,
                                           Set<String> sources, Set<String> destinations) {
        String pathToRemove = chooseAtRandom(currentAllowedToRemove, random);
        Set<String> removeCandidatePathIds = removePath(currentPathIds, pathToRemove);
        return createCandidateUpdateMaps(removeCandidatePathIds, pathIdMap, failureIds, nfe, connectionReqs,
                disconnPaths, pathSetFitnessMap, pathSetCostMap, sources, destinations);
    }

    private Solution createCandidateUpdateMaps(Set<String> candidatePathIds, Map<String, Path> pathIdMap, Set<String> failureIds,
                                               Integer nfe, Connections connectionReqs, Set<String> disconnPaths,
                                               Map<Set<String>, Double> pathSetFitnessMap, Map<Set<String>, Double> pathSetCostMap,
                                               Set<String> sources, Set<String> destinations){
        Solution candidate = makeCandidate(candidatePathIds, pathIdMap, failureIds, nfe, connectionReqs,
                disconnPaths, pathSetFitnessMap, pathSetCostMap, sources, destinations);
        pathSetFitnessMap.putIfAbsent(candidatePathIds, candidate.getFitness());
        pathSetCostMap.putIfAbsent(candidatePathIds, candidate.getCost());
        return candidate;
    }

    private String chooseAtRandom(List<String> candidates, Random random) {
        if(candidates.size() == 0){
            return "";
        } else{
            int pos = random.nextInt(candidates.size());
            return candidates.get(pos);
        }
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
                                   Map<Set<String>, Double> pathSetFitnessMap, Map<Set<String>, Double> pathSetCostMap,
                                   Set<String> sources, Set<String> destinations){
        Map<SourceDestPair, Set<String>> pairPathMap = new HashMap<>();
        Map<Node, Set<String>> srcPathMap = new HashMap<>();
        Map<Node, Set<String>> dstPathMap = new HashMap<>();
        makePathMaps(candidatePathIds, pathIdMap, pairPathMap, srcPathMap, dstPathMap);
        Double cost = pathSetCostMap.containsKey(candidatePathIds) ? pathSetCostMap.get(candidatePathIds) :
                getCost(candidatePathIds, pathIdMap);
        Double fitness = pathSetFitnessMap.containsKey(candidatePathIds) ? pathSetFitnessMap.get(candidatePathIds) :
                getFitness(candidatePathIds, pathIdMap, failureIds, nfe, connectionReqs, disconnPaths, pairPathMap,
                        srcPathMap, dstPathMap, sources, destinations);

        return new Solution(candidatePathIds, cost, fitness, pairPathMap, srcPathMap, dstPathMap);
    }

    private Set<String> removePath(Set<String> basePathIds, String pathId){
        Set<String> candidatePathIds = new HashSet<>(basePathIds);
        candidatePathIds.remove(pathId);
        return candidatePathIds;
    }

    private Set<String> addPath(Set<String> basePathIds, String pathId){
        Set<String> candidatePathIds = new HashSet<>(basePathIds);
        candidatePathIds.add(pathId);
        return candidatePathIds;
    }

    private Set<String> swapPath(Set<String> basePathIds, String newPathId, String worstPath){
        Set<String> pathIds = new HashSet<>(basePathIds);
        if(worstPath.equals("")){
            pathIds.add(newPathId);
        } else{
            pathIds.remove(worstPath);
            pathIds.add(newPathId);
        }
        return pathIds;
    }

    private void makePathMaps(Set<String> candidatePathIds, Map<String, Path> pathIdMap, Map<SourceDestPair, Set<String>> pairPathMap,
                              Map<Node, Set<String>> srcPathMap, Map<Node, Set<String>> dstPathMap) {
        for(String pathId : candidatePathIds){
            Path path = pathIdMap.get(pathId);
            SourceDestPair pair = getPairFromPath(path);
            pairPathMap.putIfAbsent(pair, new HashSet<>());
            pairPathMap.get(pair).add(pathId);
            srcPathMap.putIfAbsent(pair.getSrc(), new HashSet<>());
            dstPathMap.putIfAbsent(pair.getDst(), new HashSet<>());
        }
    }

    private double getFitness(Set<String> candidatePathIds, Map<String, Path> pathIdMap, Set<String> failureIds, Integer nfe,
                              Connections connectionReqs, Set<String> disconnIds, Map<SourceDestPair, Set<String>> pairPathMap,
                              Map<Node, Set<String>> srcPathMap, Map<Node, Set<String>> dstPathMap,
                              Set<String> sources, Set<String> destinations){
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
                }
            }
        }

        return calculateFitness(pairPathMap, srcPathMap, dstPathMap, pathIdMap, failureIds, nfe, disconnIds, minCsMap,
                minCdMap, minCsdMap, useMinS, useMinD, sources, destinations);

    }

    private Double calculateFitness(Map<SourceDestPair, Set<String>> pairPathMap, Map<Node, Set<String>> srcPathMap,
                                    Map<Node, Set<String>> dstPathMap, Map<String, Path> pathIdMap,
                                    Set<String> failuresIds, Integer nfe, Set<String> disconnIds,
                                    Map<Node, Integer> minCsMap, Map<Node, Integer> minCdMap,
                                    Map<SourceDestPair, Integer> minCsdMap, Integer useMinS, Integer useMinD,
                                    Set<String> sources, Set<String> destinations){
        // Variables for tracking constraint satisfaction
        Map<Node, Set<String>> protectedCPerSrc = new HashMap<>();
        Map<Node, Set<String>> protectedCPerDst = new HashMap<>();
        Map<SourceDestPair, Set<String>> protectedCPerPair = new HashMap<>();
        Map<Node, Set<String>> fgDisjointCPerSrc = new HashMap<>();
        Map<Node, Set<String>> fgDisjointCPerDst = new HashMap<>();
        Map<SourceDestPair, Set<String>> fgDisjointCPerPair = new HashMap<>();

        // Determine which paths per src/dst/pair are protected, or can fail but are FG-disjoint from each other
        fillInPathMaps(protectedCPerSrc, protectedCPerDst, protectedCPerPair, fgDisjointCPerSrc, fgDisjointCPerDst, fgDisjointCPerPair,
                pairPathMap, srcPathMap, dstPathMap, pathIdMap, failuresIds, disconnIds, sources, destinations);
        // With the worst FG, find if the requirements are met
        // Have to consider this on a pair by pair basis
        // Even if a path gets disconnected, it's still relevant

        // Fitness:
        // numSatisfiedS + numSatisfiedD + sum( (Protected + |FG-disjoint / nfe|) foreach s, d, (s,d) )
        // s in s or d in d is satisfied if there is at least one path left over after FG removal
        Set<Node> memberNodes = new HashSet<>(protectedCPerSrc.keySet());
        memberNodes.addAll(protectedCPerDst.keySet());
        Set<String> memberIds = memberNodes.stream().map(Node::getId).collect(Collectors.toSet());
        int satisfiedSCount = 0;
        int satisfiedDCount = 0;
        double totalScore = 0.0;
        int nfeBase = nfe + 1;
        // Add to the score for each source and destination node
        for(Node node : protectedCPerSrc.keySet()) {
            double score = getScore(node, protectedCPerSrc, fgDisjointCPerSrc, nfeBase, srcPathMap.get(node).size());
            if(score >= 1){
                satisfiedSCount++;
            }
            if(score > minCsMap.get(node)){
                score = Math.min(1, score);
            }
            totalScore += score;
        }
        for(Node node : protectedCPerDst.keySet()){
            double score = getScore(node, protectedCPerDst, fgDisjointCPerDst, nfeBase, dstPathMap.get(node).size());
            if(score >= 1){
                satisfiedDCount++;
            }
            if(score > minCdMap.get(node)){
                score = Math.min(1, score);
            }
            totalScore += score;
        }
        // Add to the score for each pair
        for(SourceDestPair pair : protectedCPerPair.keySet()){
            double score = protectedCPerPair.get(pair).size() + (1.0 * fgDisjointCPerPair.get(pair).size() / nfeBase);
            if(score > minCsdMap.get(pair)){
                score = Math.min(1, score);
            }
            totalScore += score;
        }
        totalScore += satisfiedSCount * srcMultiplier;
        totalScore += satisfiedDCount;

        return totalScore;
    }

    private double getScore(Node node, Map<Node, Set<String>> protectedMap, Map<Node, Set<String>> disjointMap, int nfeBase, int numPaths) {
        double score = 0.0;
        if (protectedMap.containsKey(node)) {
            score = (protectedMap.get(node).size() + (1.0 * disjointMap.get(node).size()  / nfeBase ));
        }
        return score;
    }

    private void fillInPathMaps(Map<Node, Set<String>> protectedCPerSrc, Map<Node, Set<String>> protectedCPerDst,
                                Map<SourceDestPair, Set<String>> protectedCPerPair, Map<Node, Set<String>> fgDisjointCPerSrc,
                                Map<Node, Set<String>> fgDisjointCPerDst, Map<SourceDestPair, Set<String>> fgDisjointCPerPair,
                                Map<SourceDestPair, Set<String>> pairPathMap, Map<Node, Set<String>> pathsPerSrc, Map<Node, Set<String>> pathsPerDst,
                                Map<String, Path> pathIdMap,
                                Set<String> fails, Set<String> disconnIds, Set<String> sources, Set<String> dests) {

        boolean ignoreSrcFailForDsts = sources.size() == 1 && fails.contains(sources.iterator().next());
        boolean ignoreDstFailForSrcs = dests.size() == 1 && fails.contains(dests.iterator().next());
        for(SourceDestPair pair : pairPathMap.keySet()){
            Set<String> pathIds = pairPathMap.get(pair);
            Node src = pair.getSrc();
            Node dst = pair.getDst();
            pathsPerSrc.putIfAbsent(src, new HashSet<>());
            pathsPerDst.putIfAbsent(dst, new HashSet<>());
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
            pathsPerSrc.get(src).addAll(pathIds);
            pathsPerDst.get(dst).addAll(pathIds);

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
                    Set<String> onlySrcOrDest = fgInCurrent.stream()
                            .filter(f -> src.getId().equals(f) || dst.getId().equals(f))
                            .collect(Collectors.toSet());
                    // If there's just one dest, then you can ignore it and the source's failures and count it as a protected
                    // path for that source (only if there are no other failures in the path)
                    if(ignoreDstFailForSrcs && onlySrcOrDest.size() >= fgInCurrent.size()){
                        srcProtectedPaths.add(pathId);
                    } else{
                        fgInCurrent.remove(src.getId());
                        srcFGDisjointPaths = evaluateFGDisjointPaths(srcFGDisjointPaths, current, fgInCurrent, pathIdMap);
                        fgInCurrent.add(src.getId());
                    }
                    // If there's just one source, then you can ignore it and the dest's failures and count it as a protected
                    // path for that dest (only if there are no other failures in the path)
                    if(ignoreSrcFailForDsts && onlySrcOrDest.size() >= fgInCurrent.size()){
                        dstProtectedPaths.add(pathId);
                    } else{
                        fgInCurrent.remove(dst.getId());
                        dstFGDisjointPaths = evaluateFGDisjointPaths(dstFGDisjointPaths, current, fgInCurrent, pathIdMap);
                        fgInCurrent.add(dst.getId());
                    }
                    // Same deal for the pair
                    if((ignoreSrcFailForDsts || ignoreDstFailForSrcs) && onlySrcOrDest.size() >= fgInCurrent.size()){
                        pairProtectedPaths.add(pathId);
                    }
                    else {
                        fgInCurrent.remove(src.getId());
                        fgInCurrent.remove(dst.getId());
                        pairFGDisjointPaths = evaluateFGDisjointPaths(pairFGDisjointPaths, current, fgInCurrent, pathIdMap);
                    }
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

        int neededS = calculateNeededMembers(minCsMap.keySet(), useMinS, failureIds, nfe)  * srcMultiplier;
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
