package netlab.processing.groupcast;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.cycles.CollapsedRingService;
import netlab.processing.shortestPaths.MinimumCostPathService;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/*
Cycle for Two (CFT)
Groupcast Protection
From "Protection Algorithms for Groupcast Sessions in Transparent Optical Networks with Mesh Topologies"
by Tania Panayiotou, Georgios Ellinas, Neo Antoniades - 2013
 */
@Service
@Slf4j
public class CycleForTwoService {

    private MinimumCostPathService minimumCostPathService;
    private TopologyAdjustmentService topologyAdjustmentService;
    private CollapsedRingService collapsedRingService;

    @Autowired
    public CycleForTwoService(MinimumCostPathService minimumCostPathService,
                              TopologyAdjustmentService topologyService, CollapsedRingService collapsedRingService) {
        this.minimumCostPathService = minimumCostPathService;
        this.topologyAdjustmentService = topologyService;
        this.collapsedRingService = collapsedRingService;
    }

    public Details solve(Request request, Topology topo){
        long startTime = System.nanoTime();
        Details details = findPaths(request, topo);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setRunningTimeSeconds(duration);
        return details;
    }

    /*
        Algorithm:
        1. Use CollapsedRing algorithm to create a cycle including all members.
        2. Remove links that are in the ring from the topology.
        3. Create arc-disjoint multicast trees as necessary to connect sources to destinations.
        4. Store the trees as primary paths, then build paths from the cycle
     */
    private Details findPaths(Request request, Topology topo) {
        Details details = request.getDetails();

        TrafficCombinationType trafficCombinationType = request.getTrafficCombinationType();
        Set<SourceDestPair> pairs = request.getDetails().getPairs();

        Set<Node> sources = details.getSources();
        Set<Node> dests = details.getDestinations();
        Map<SourceDestPair, Double> pathCostMap = topo.getMinimumPathCostMap();

        // Get all of the members
        Set<Node> members = new HashSet<>(sources);
        members.addAll(dests);

        // Pick a source
        Node src = sources.stream().sorted(Comparator.comparing(Node::getId)).collect(Collectors.toList()).get(0);

        List<Node> sortedDests = members.stream()
                .filter(d -> !d.equals(src))
                .sorted(Comparator.comparing(d -> pathCostMap.get(new SourceDestPair(src, d))))
                .collect(Collectors.toList());

        // Get the two main paths that make up the cycle
        List<Path> cyclePaths = collapsedRingService.findCollapsedRing(src, sortedDests, topo);
        if(dests.contains(src)) {
            collapsedRingService.augmentPathListWithPathsToSrc(cyclePaths);
        }
        //Set<Link> forwardCycleLinks = getLinksFromOneCycle(src, cyclePaths);//cyclePaths.stream().map(Path::getLinks).flatMap(Collection::stream).collect(Collectors.toSet());

        // Give these links Maximum weight
        Topology cycleRemovedTopo = topologyAdjustmentService.adjustWeightsToMax(topo, cyclePaths);

        // Find a shortestpath/tree solution for the problem without using the cycle links
        Map<SourceDestPair, Map<String, Path>> chosenPathsMap = minimumCostPathService.findPaths(details, request.getRoutingType(),
                pairs, cycleRemovedTopo, trafficCombinationType, false);

        // Convert any MAX_INT link weights back to their true weight
        topologyAdjustmentService.readjustLinkWeights(chosenPathsMap, topo);

        // Now, find paths for each source to each destination using the cycle links
        Set<Link> bothCycleLinks = cyclePaths.stream().map(Path::getLinks).flatMap(Collection::stream).collect(Collectors.toSet());
        Topology cycleOnlyTopo = topologyAdjustmentService.createTopologyWithLinkSubset(topo, bothCycleLinks);
        Map<SourceDestPair, Map<String, Path>> backupMap = getBackupPaths(pairs, cycleOnlyTopo);

        // Add backup paths to primary map
        for(SourceDestPair pair : chosenPathsMap.keySet()){
            if(backupMap.containsKey(pair)){
                Map<String, Path> establishedMap = chosenPathsMap.get(pair);
                Map<String, Path> backupMapForPair = backupMap.get(pair);
                Set<String> establishedIds = establishedMap.values().stream().map(Path::getId).collect(Collectors.toSet());
                for(Path path : backupMapForPair.values()){
                    if(!establishedIds.contains(path.getId())){
                        establishedMap.put(String.valueOf(establishedMap.size() + 1), path);
                    }
                }
            }
        }

        // Path filtering
        details.setChosenPaths(chosenPathsMap);
        details.setIsFeasible(true);

        return details;
    }


    private Map<SourceDestPair,Map<String,Path>> getBackupPaths(Set<SourceDestPair> pairs, Topology cycleOnlyTopo) {
        Map<SourceDestPair, Map<String, Path>> backupPathsMap = new HashMap<>();
        for(SourceDestPair pair : pairs){
            backupPathsMap.put(pair, new HashMap<>());
            Path sp = minimumCostPathService.findShortestPath(pair, cycleOnlyTopo);
            if(!sp.isEmpty()) {
                backupPathsMap.get(pair).put("1", sp);
                Topology prunedTopo = topologyAdjustmentService.removeLinksFromTopology(cycleOnlyTopo, sp.getLinks());
                Path secondSp = minimumCostPathService.findShortestPath(pair, prunedTopo);
                if(!secondSp.isEmpty()){
                    backupPathsMap.get(pair).put("2", secondSp);
                }
            }
        }
        return backupPathsMap;
    }

    private Set<Link> getLinksFromOneCycle(Node src, List<Path> cyclePaths) {
        List<Path> primaryPaths = new ArrayList<>();
        Node lastNode = null;
        for(Path path : cyclePaths){
            List<Node> nodes = path.getNodes();
            Node currLastNode = nodes.get(nodes.size()-1);
            // Get the forward path that includes all destinations
            if(primaryPaths.isEmpty()){
                if(lastNode == null){
                    lastNode = currLastNode;
                }
                else if(lastNode == currLastNode){
                    primaryPaths.add(path);
                }
            }
            // Get the path back to the source (if it exists) from the last destination
            if(currLastNode == src){
                Node firstNode = nodes.get(0);
                if(firstNode == lastNode){
                    primaryPaths.add(path);
                    break;
                }
            }
        }
        return primaryPaths.stream().map(Path::getLinks).flatMap(Collection::stream).collect(Collectors.toSet());
    }


}
