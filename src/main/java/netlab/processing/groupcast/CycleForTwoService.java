package netlab.processing.groupcast;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.cycles.CollapsedRingService;
import netlab.processing.pathmapping.PathMappingService;
import netlab.processing.shortestPaths.ShortestPathService;
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

    private ShortestPathService shortestPathService;
    private TopologyAdjustmentService topologyAdjustmentService;
    private CollapsedRingService collapsedRingService;

    @Autowired
    public CycleForTwoService(ShortestPathService shortestPathService,
                              TopologyAdjustmentService topologyService, CollapsedRingService collapsedRingService) {
        this.shortestPathService = shortestPathService;
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
        Map<SourceDestPair, Long> pathCostMap = topo.getMinimumPathCostMap();

        // Get all of the members
        Set<Node> members = new HashSet<>(sources);
        members.addAll(dests);

        // Pick a source
        Node src = sources.stream().sorted(Comparator.comparing(Node::getId)).collect(Collectors.toList()).get(0);

        List<Node> sortedDests = dests.stream()
                .sorted(Comparator.comparing(d -> pathCostMap.get(new SourceDestPair(src, d))))
                .collect(Collectors.toList());

        // Get the two main paths that make up the cycle
        List<Path> cyclePaths = collapsedRingService.findCollapsedRing(src, sortedDests, topo);
        if(dests.contains(src)) {
            collapsedRingService.augmentPathListWithPathsToSrc(cyclePaths);
        }
        Set<Link> cycleLinks = cyclePaths.stream().map(Path::getLinks).flatMap(Collection::stream).collect(Collectors.toSet());

        // Remove those links from the topology
        Topology cycleRemovedTopo = topologyAdjustmentService.removeLinksFromTopology(topo, cycleLinks);

        // Find a shortestpath/tree solution for the problem without using the cycle links
        Map<SourceDestPair, Map<String, Path>> chosenPathsMap = shortestPathService.findPaths(details, request.getRoutingType(),
                pairs, cycleRemovedTopo, trafficCombinationType, false);

        // Now, find paths for each source to each destination using the cycle links
        Topology cycleOnlyTopo = topologyAdjustmentService.createTopologyWithLinkSubset(topo, cycleLinks);
        Map<SourceDestPair, Map<String, Path>> backupMap = shortestPathService.findPaths(details, request.getRoutingType(),
                pairs, cycleOnlyTopo, trafficCombinationType, false);

        // Add backup paths to primary map
        for(SourceDestPair pair : chosenPathsMap.keySet()){
            if(backupMap.containsKey(pair)){
                Map<String, Path> backupMapForPair = backupMap.get(pair);
                for(Path path : backupMapForPair.values()){
                    chosenPathsMap.get(pair).put(String.valueOf(chosenPathsMap.get(pair).size() + 1), path);
                }
            }
        }

        // Path filtering
        details.setChosenPaths(chosenPathsMap);
        details.setIsFeasible(true);

        return details;
    }


}
