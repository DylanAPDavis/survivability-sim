package netlab.processing.groupcast;


import lombok.extern.slf4j.Slf4j;
import netlab.processing.disjointpaths.BhandariService;
import netlab.processing.pathmapping.PathMappingService;
import netlab.processing.shortestPaths.MinimumCostPathService;
import netlab.processing.shortestPaths.MinimumRiskPathService;
import netlab.submission.enums.FailureScenario;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Service
@Slf4j
public class SurvivableHubBasedService {

    private MinimumRiskPathService minimumRiskPathService;
    private PathMappingService pathMappingService;
    private TopologyAdjustmentService topologyService;
    private BhandariService bhandariService;


    @Autowired
    public SurvivableHubBasedService(MinimumRiskPathService minimumRiskPathService, PathMappingService pathMappingService,
                                   TopologyAdjustmentService topologyService, BhandariService bhandariService) {
        this.minimumRiskPathService = minimumRiskPathService;
        this.pathMappingService = pathMappingService;
        this.topologyService = topologyService;
        this.bhandariService = bhandariService;
    }

    public Details solve(Request request, Topology topo){
        Details details = request.getDetails();


        long startTime = System.nanoTime();
        details = findPaths(request, topo);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setRunningTimeSeconds(duration);
        return details;
    }

    private Details findPaths(Request request, Topology topo) {
        Details details = request.getDetails();

        TrafficCombinationType trafficCombinationType = request.getTrafficCombinationType();
        Set<SourceDestPair> pairs = request.getDetails().getPairs();
        Set<Failure> failures = request.getDetails().getFailures().getFailureSet();
        FailureScenario failureScenario = request.getFailureScenario();
        int nfe = details.getNumFailureEvents().getTotalNumFailureEvents();

        Set<Node> sources = details.getSources();
        Set<Node> dests = details.getDestinations();
        Set<Node> options = new HashSet<>(topo.getNodes());
        Map<SourceDestPair, Double> pathRiskMap = createRiskMap(pairs, topo, failureScenario, failures);
        Map<Link, Double> linkRiskMap = minimumRiskPathService.createRiskMap(topo.getLinks(), failures);
        Map<Node, Set<Link>> nodeLinkMap = topo.getNodeLinkMap();

        Map<Node, List<Double>> distanceToEachNode = new HashMap<>();
        Map<Node, Map<Node, Double>> costFromNodeToDst = new HashMap<>();
        // Get the costs of each src to each other node
        for(Node src : sources){
            Map<Node, Double> mapToOtherNodes = createCostMap(src, options, pathRiskMap);
            for(Node node : mapToOtherNodes.keySet()){
                distanceToEachNode.putIfAbsent(node, new ArrayList<>());
                distanceToEachNode.get(node).add(mapToOtherNodes.get(node));
            }
        }
        // Get the costs of each node to each dst
        for(Node node : options){
            Map<Node, Double> mapToDests = createCostMap(node, dests, pathRiskMap);
            costFromNodeToDst.put(node, mapToDests);
        }


        // Pick the src bestS that minimizes the maximum distance any src would have to travel
        Map<Node, Double> worstCaseDistanceUsingNode = new HashMap<>();
        Map<Node, Double> totalDistanceUsingNode = new HashMap<>();
        for(Node node : options){
            List<Double> distancesToNode = distanceToEachNode.containsKey(node) ?
                    distanceToEachNode.get(node) : Collections.singletonList(0.0);
            Double maxDistanceToNode = Collections.max(distancesToNode);

            Map<Node, Double> distanceToDsts = costFromNodeToDst.containsKey(node) ?
                    costFromNodeToDst.get(node) : new HashMap<>();
            Double maxDistanceToDst = !distanceToDsts.isEmpty() ?
                    Collections.max(distanceToDsts.values()) : 99999999.99999;

            Double worstCase = maxDistanceToNode + maxDistanceToDst;
            worstCaseDistanceUsingNode.put(node, worstCase);

            Double total = distancesToNode.stream().mapToDouble(d -> d).sum()
                    + distanceToDsts.values().stream().mapToDouble(d -> d).sum();
            totalDistanceUsingNode.put(node, total);
        }

        Set<Node> failureNodes = failures.stream().filter(f -> f.getNode() != null).map(Failure::getNode)
                //.sorted(Comparator.comparing(worstCaseDistanceUsingNode::get).thenComparing(totalDistanceUsingNode::get))
                .collect(Collectors.toSet());
        boolean nodesCanFail = failureNodes.size() > 0;
        Set<Node> nonFailureNodes = options.stream().filter(n -> !failureNodes.contains(n)).collect(Collectors.toSet());
        Set<Node> nonFailureNonDests = nonFailureNodes.stream().filter(n -> !dests.contains(n)).collect(Collectors.toSet());
        Set<Node> nonFailureDests = options.stream().filter(dests::contains).collect(Collectors.toSet());
        // Ordering -> Non failure, non-dest nodes -> Non failure Dest Nodes -> Failure Nodes
        List<Node> sortedOptions = createSortedOptions(nonFailureNonDests, nonFailureDests, failureNodes,
                worstCaseDistanceUsingNode, totalDistanceUsingNode);

        List<Node> hubs = new ArrayList<>();

        // Goal: Pick options such that one will be left over after nfe failures
        // If you can pick one non-failure option -> you're done
        // Otherwise, you'll have to pick 1 + nfe options
        int numRiskyIncidentLinks = 0;
        int numNonRiskyIncidentLinks = 0;
        boolean usedANonFailure = false;
        for(Node option : sortedOptions){
            Set<Link> incidentLinks = nodeLinkMap.get(option);
            if(nonFailureNodes.contains(option)){
                hubs.add(option);
                usedANonFailure = true;
            }
            else{
                hubs.add(option);
                /*if(hubs.size() >= 1 + nfe){
                    break;
                }*/
            }
            for(Link link : incidentLinks){
                if(linkRiskMap.get(link) > 0.0){
                    numRiskyIncidentLinks++;
                } else{
                    numNonRiskyIncidentLinks++;
                }
            }
            if(numNonRiskyIncidentLinks > 0 && usedANonFailure){
                break;
            }
            if(numNonRiskyIncidentLinks + numRiskyIncidentLinks >= 1 + nfe && (usedANonFailure || hubs.size() >= 1 + nfe)){
                break;
            }
        }




        // Route each s in S to each destination combining the path to hub and the path from hub to each dst

        Map<SourceDestPair, Map<String, Path>> chosenPathsMap = new HashMap<>();
        for(Node hub : hubs) {
            for (Node src : sources) {
                boolean isMin = src.getId().equals(hub.getId());
                List<Path> toMinNode = isMin ? null : bhandariService.computeDisjointPathsFlex(topo, src, hub, 1, nfe, nodesCanFail, failures, false);
                //bhandariService.computeDisjointPaths(topo, src, hub, 1 + nfe, nodesCanFail);
                if(toMinNode != null){
                    // Filter out empty paths
                    toMinNode = toMinNode.stream().filter(p -> p.getLinks().size() > 0).collect(Collectors.toList());
                }
                for (Node dst : dests) {
                    boolean srcIsDst = src.getId().equals(dst.getId());
                    // If the source is this destination, don't find a path
                    if (srcIsDst) {
                        continue;
                    }
                    SourceDestPair srcDst = new SourceDestPair(src, dst);
                    List<Path> completePaths;
                    boolean minNodeIsDst = hub.getId().equals(dst.getId());
                    // If the min src is this destination, you already have a path to reach it
                    if (minNodeIsDst) {
                        completePaths = toMinNode;
                    }
                    // Otherwise, you need to find a path from the src to the destination
                    // Start at the minSrc, then prepend the path to the minSrc
                    else {
                        List<Path> toDstFromMinNode = bhandariService.computeDisjointPathsFlex(topo, hub, dst, 1, nfe, nodesCanFail, failures, false);
                        //bhandariService.computeDisjointPaths(topo, hub, dst, 1 + nfe, nodesCanFail);
                        toDstFromMinNode = toDstFromMinNode.stream().filter(p -> p.getLinks().size() > 0).collect(Collectors.toList());
                        completePaths = isMin ? toDstFromMinNode : combinePaths(toMinNode, toDstFromMinNode);
                    }
                    chosenPathsMap.putIfAbsent(srcDst, new HashMap<>());
                    if (completePaths != null) {
                        for(Path completePath : completePaths) {
                            chosenPathsMap.get(srcDst).put(completePath.getId(), completePath);
                        }
                    }
                }
            }
        }


        details.setChosenPaths(chosenPathsMap);
        details.setIsFeasible(true);

        return details;
    }

    private List<Path> combinePaths(List<Path> toHubPaths, List<Path> toDstFromHubPaths) {
        List<Path> combinedPaths = new ArrayList<>();
        for(Path toHub : toHubPaths){
            for(Path toDst : toDstFromHubPaths){
                Path combined = toHub.combinePaths(toDst);
                combinedPaths.add(combined);
            }
        }
        return combinedPaths;
    }

    private List<Node> createSortedOptions(Set<Node> nonFailureNonDests, Set<Node> nonFailureDests, Set<Node> failureNodes,
                                           Map<Node, Double> worstCaseDistanceUsingNode, Map<Node, Double> totalDistanceUsingNode){
        List<Node> sortedOptions = new ArrayList<>(nonFailureNonDests);
        sortedOptions.sort(Comparator.comparing(worstCaseDistanceUsingNode::get).thenComparing(totalDistanceUsingNode::get));
        List<Node> sortedNonFailureDests = nonFailureDests.stream()
                .sorted(Comparator.comparing(worstCaseDistanceUsingNode::get).thenComparing(totalDistanceUsingNode::get))
                .collect(Collectors.toList());
        List<Node> sortedFailureNodes = failureNodes.stream()
                .sorted(Comparator.comparing(worstCaseDistanceUsingNode::get).thenComparing(totalDistanceUsingNode::get))
                .collect(Collectors.toList());
        sortedOptions.addAll(sortedNonFailureDests);
        sortedOptions.addAll(sortedFailureNodes);
        return sortedOptions;
    }


    private Map<SourceDestPair,Double> createRiskMap(Set<SourceDestPair> pairs, Topology topo, FailureScenario failureScenario, Set<Failure> failures) {
        return pairs.stream()
                .collect(Collectors.toMap(p -> p, p -> minimumRiskPathService.findRisk(p, topo, failureScenario, failures)));
    }

    public Map<Node, Double> createCostMap(Node src, Set<Node> otherNodes, Map<SourceDestPair, Double> pathCostMap){
        Map<Node, Double> distanceMap = new HashMap<>();
        for(Node otherNode : otherNodes){
            String otherId = otherNode.getId();
            if(!src.getId().equals(otherId)){
                SourceDestPair pair = new SourceDestPair(src, otherNode);
                if(pathCostMap.containsKey(pair)){
                    Double distance = pathCostMap.get(pair);
                    distanceMap.put(otherNode, distance);
                }
            }
        }
        return distanceMap;
    }

}
