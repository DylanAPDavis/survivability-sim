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
        long startTime = System.nanoTime();
        Map<SourceDestPair, Map<String, Path>> paths = findPaths(details.getPairs(), failCollection, nfeCollection.getTotalNumFailureEvents(), connCollection,
                request.getFailureClass(), request.getTrafficCombinationType(), topology, request.getSeed());
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
                                                           Long seed) {

        TopologyMetrics topologyMetrics = topologyMetricsService.generateMetrics(topology);

        Integer fitnessThreshold = calculateFitnessThreshold(connCollection);
        Solution bestSolution = new Solution(new HashSet<>(), Double.MAX_VALUE, 0);

        Solution currentSolution =  new Solution(new HashSet<>(), Double.MAX_VALUE, 0);

        Set<Set<String>> failureGroupIds = convertToIds(failCollection.getFailureGroups());
        int numInterationsWithoutChange = 0;
        while(numInterationsWithoutChange < 5){
            //TODO: Consider a tabu list while generating candidates
            List<Solution> candidateSolutions = generateCandidateSolutions(currentSolution,  topologyMetrics, pairs, failureGroupIds);
            Solution bestCandidate = pickBestCandidate(candidateSolutions);
            boolean changed = false;
            //TODO: Include change in a tabu list
            if(isBetter(bestCandidate, currentSolution)){
                currentSolution = bestCandidate;
                if(isBetter(bestCandidate, bestSolution)){
                    bestSolution = bestCandidate;
                    changed = true;
                }
            }
            if(!changed) {
                numInterationsWithoutChange++;
            }
        }

        return convertToMap(bestSolution, topologyMetrics.getPathIdMap());

    }

    private Set<Set<String>> convertToIds(List<List<Failure>> failureGroups) {
        Set<Set<String>> idGroups = new HashSet<>();
        for(List<Failure> failureGroup : failureGroups){
            Set<String> ids = failureGroup.stream().map(Failure::getId).collect(Collectors.toSet());
            idGroups.add(ids);
        }
        return idGroups;
    }


    //TODO: Adapt to double fitness values.
    private boolean isBetter(Solution candidate, Solution solution, Integer fitnessThreshold) {
        boolean equalFitBetterCost = candidate.getFitness() > 0 && candidate.getFitness().equals(solution.getFitness()) && candidate.getCost() > solution.getCost();
        boolean unnecessaryFitness = candidate.getFitness() > solution.getFitness() && solution.getFitness() >= fitnessThreshold && candidate.getCost() > solution.getCost();
        return (!unnecessaryFitness && candidate.getFitness() > solution.getFitness()) || equalFitBetterCost;
    }

    private Solution pickBestCandidate(List<Solution> candidateSolutions) {
        //TODO: Pick the best candidate based on fitness and cost
        //TODO: Introduce ability to select worse solutions to "reset" search
    }

    private List<Solution> generateCandidateSolutions(Solution currentSolution, TopologyMetrics topologyMetrics, Set<SourceDestPair> pairs,
                                                      Set<Set<String>> failureGroupIds, Long seed) {

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
                    Solution candidate = new Solution(candidatePathIds, getCost(candidatePathIds, pathIdMap), getFitness(candidatePathIds));
                }
            }
        }
        return new ArrayList<>();
    }

    //TODO: Change Fitness to Double, let you get partially more fit! For example, got a source closer to being protected
    private Integer getFitness(Set<String> candidatePathIds, Map<String, Path> pathIdMap, Set<Set<String>> failureGroupIds,
                               Connections connectionReqs, Map<Set<String>, Set<String>> fgToPathIdsMap){
        Set<Path> paths = candidatePathIds.stream().map(pathIdMap::get).collect(Collectors.toSet());
        Integer useMinS = connectionReqs.getUseMinS();
        Integer useMinD = connectionReqs.getUseMinD();
        Map<Node, Integer> minCsMap = connectionReqs.getSrcMinConnectionsMap();
        Map<Node, Integer> minCdMap = connectionReqs.getDstMinConnectionsMap();
        Map<SourceDestPair, Integer> minCsdMap = connectionReqs.getPairMinConnectionsMap();
        Set<String> worstFG = new HashSet<>();
        Set<String> worstDisconnectedPathIds = new HashSet<>();
        for(Set<String> failureGroup : failureGroupIds){
            Set<String> disconnIds = new HashSet<>();
            for(Path path : paths){
                for(String failId : failureGroup) {
                    if (path.getLinkIds().contains(failId) || path.getNodeIds().contains(failId)){
                        disconnIds.add(path.getId());
                        break;
                    }
                }
            }

            // Update fg -> Path IDs map to track which paths get disconnected by a failure group
            fgToPathIdsMap.putIfAbsent(failureGroup, new HashSet<>());
            fgToPathIdsMap.get(failureGroup).addAll(disconnIds);

            // If this is the "worst case" FG so far, then keep it
            if(disconnIds.size() > worstDisconnectedPathIds.size()){
                worstFG = failureGroup;
                worstDisconnectedPathIds = disconnIds;
            }
        }

        // Variables for tracking constraint satisfaction
        Set<Node> usedSrcs = new HashSet<>();
        Set<Node> usedDsts = new HashSet<>();
        Map<Node, Integer> numCPerSrc = new HashMap<>();
        Map<Node, Integer> numCPerDst = new HashMap<>();
        Map<SourceDestPair, Integer> numCPerPair = new HashMap<>();

        // With the worst FG, find if the requirements are met
        // Have to consider this on a pair by pair basis
        // Even if a path gets disconnected, it's still relevant
        for(Path path : paths){
            // If this path is not disconnected, consider it
            if(!worstDisconnectedPathIds.contains(path.getId())){
                List<Node> pathNodes = path.getNodes();
                Node src = pathNodes.get(0);
                Node dst = pathNodes.get(pathNodes.size()-1);

            }
        }
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
            Node src = path.getNodes().get(0);
            Node dst = path.getNodes().get(path.getNodes().size()-1);
            SourceDestPair pair = new SourceDestPair(src, dst);
            outputMap.putIfAbsent(pair, new HashMap<>());
            outputMap.get(pair).put(path.getId(), path);
        }
        return outputMap;
    }

    // Calculate how many constraints need to be satisfied
    // Number of sources that need at least one conn, number of dests that need at least one conn, number of pairs that
    // need at least one conn
    private Integer calculateFitnessThreshold(Connections connCollection) {
        Integer useMinS = connCollection.getUseMinS();
        Integer useMinD = connCollection.getUseMinD();
        Map<Node, Integer> minCsMap = connCollection.getSrcMinConnectionsMap();
        Map<Node, Integer> minCdMap = connCollection.getDstMinConnectionsMap();
        Map<SourceDestPair, Integer> minCsdMap = connCollection.getPairMinConnectionsMap();

        Integer numSWithAtLeastOneConn = Integer.parseInt(String.valueOf(minCsMap.values().stream().filter(v -> v > 0).count()));
        Integer numDWithAtLeastOneConn = Integer.parseInt(String.valueOf(minCdMap.values().stream().filter(v -> v > 0).count()));
        Integer numPairsWithAtLeastOneConn = Integer.parseInt(String.valueOf(minCsdMap.values().stream().filter(v -> v > 0).count()));

        // Take the max between num S that need a conn vs useMinS
        Integer neededS = Math.max(useMinS, numSWithAtLeastOneConn);
        // Take the max between numD that need a conn vs useMinD
        Integer neededD = Math.max(useMinD, numDWithAtLeastOneConn);

        return neededS + neededD + numPairsWithAtLeastOneConn;
    }
}
