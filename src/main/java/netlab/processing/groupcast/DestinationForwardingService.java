package netlab.processing.groupcast;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.PathMappingService;
import netlab.processing.shortestPaths.ShortestPathService;
import netlab.submission.enums.RoutingType;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.Connections;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class DestinationForwardingService {

    private ShortestPathService shortestPathService;
    private PathMappingService pathMappingService;
    private TopologyAdjustmentService topologyService;

    @Autowired
    public DestinationForwardingService(ShortestPathService shortestPathService, PathMappingService pathMappingService,
                                        TopologyAdjustmentService topologyService) {
        this.shortestPathService = shortestPathService;
        this.pathMappingService = pathMappingService;
        this.topologyService = topologyService;
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

        Set<Node> sources = details.getSources();
        Set<Node> dests = details.getDestinations();
        Map<SourceDestPair, Long> pathCostMap = topo.getMinimumPathCostMap();

        // First, get the costs to get from each src to each dest, then each dest to each other dest
        Map<Node, Map<Node, Long>> costToDestMap = new HashMap<>();
        Map<Node, Map<Node, Long>> costFromDestMap = new HashMap<>();
        for(SourceDestPair pair : pathCostMap.keySet()) {
            Node origin = pair.getSrc();
            Node target = pair.getDst();
            Long cost = pathCostMap.get(pair);
            if (sources.contains(origin) && dests.contains(target)) {
                costToDestMap.putIfAbsent(target, new HashMap<>());
                costToDestMap.get(target).put(origin, cost);
            }
            if (dests.contains(origin) && dests.contains(target)) {
                costFromDestMap.putIfAbsent(origin, new HashMap<>());
                costFromDestMap.get(origin).put(target, cost);
            }
        }

        // Goal: Route from each source to one of the destinations
        // Then, route from that destination to the remaining destinations

        // Pick the destination with the lowest minimum cost to get to from the sources
        // AND to get to the other destinations
        Node minimumCostDest = null;
        Long minimumCost = Long.MAX_VALUE;
        for(Node dest : dests){
            Map<Node, Long> costToMap = costToDestMap.get(dest);
            Map<Node, Long> costFromMap = costFromDestMap.get(dest);
            Long sum = costToMap != null ? costToMap.values().stream().reduce(0L, (c1, c2) -> c1 + c2) : 0L;
            sum += costFromMap != null ? costFromMap.values().stream().reduce(0L, (c1, c2) -> c1 + c2) : 0L;
            if(sum < minimumCost){
                minimumCost = sum;
                minimumCostDest = dest;
            }
        }

        if(minimumCostDest == null){
            details.setIsFeasible(false);
            return details;
        }

        // You now have a dest with minimum cost to reach/transmit from, so save the necessary routes
        Map<Node, Set<Path>> srcPathsMap = new HashMap<>();
        Map<Node, Set<Path>> dstPathsMap = new HashMap<>();
        Map<SourceDestPair, Map<String, Path>> chosenPathsMap = new HashMap<>();
        // Get a path from each source to the minimum cost dest
        for(Node src : sources){
            if(src != minimumCostDest){
                SourceDestPair pair = SourceDestPair.builder().src(src).dst(minimumCostDest).build();
                Path sp = shortestPathService.findShortestPath(pair, topo, srcPathsMap, dstPathsMap, trafficCombinationType);
                updateMaps(pair, sp, srcPathsMap, dstPathsMap, chosenPathsMap);
            }
        }
        // Get a path from the minmimum cost dest to each other dest
        for(Node otherDest : dests){
            if(otherDest != minimumCostDest){
                SourceDestPair minDestToOtherDestPair = SourceDestPair.builder().src(minimumCostDest).dst(otherDest).build();
                Path minDestToOtherDestPath = shortestPathService.findShortestPath(minDestToOtherDestPair, topo, srcPathsMap, dstPathsMap, trafficCombinationType);
                // Then, with that path create a new src -> other dest path for each source
                for(Node src : sources) {
                    if(src != otherDest) {
                        SourceDestPair srcOtherDestPair = SourceDestPair.builder().src(src).dst(otherDest).build();
                        SourceDestPair srcToMinDestPair = SourceDestPair.builder().src(src).dst(minimumCostDest).build();
                        // If you've established a src -> min dest path, append on that new min dest -> other dest path
                        if(chosenPathsMap.containsKey(srcToMinDestPair)){
                            Path combinedPath = chosenPathsMap.get(srcToMinDestPair).values().iterator().next().combinePaths(minDestToOtherDestPath);
                            updateMaps(srcOtherDestPair, combinedPath, srcPathsMap, dstPathsMap, chosenPathsMap);
                        } else if(src == minimumCostDest && pairs.contains(srcOtherDestPair)){
                            updateMaps(srcOtherDestPair, minDestToOtherDestPath, srcPathsMap, dstPathsMap, chosenPathsMap);
                        }
                    }
                }
            }
        }
        details.setChosenPaths(chosenPathsMap);
        details.setIsFeasible(true);

        return details;
    }

    public void updateMaps(SourceDestPair pair, Path path, Map<Node, Set<Path>> srcPathsMap, Map<Node, Set<Path>> dstPathsMap,
                           Map<SourceDestPair, Map<String, Path>> chosenPathMap){
        srcPathsMap.putIfAbsent(pair.getSrc(), new HashSet<>());
        srcPathsMap.get(pair.getSrc()).add(path);
        dstPathsMap.putIfAbsent(pair.getDst(), new HashSet<>());
        dstPathsMap.get(pair.getDst()).add(path);
        chosenPathMap.putIfAbsent(pair, new HashMap<>());
        chosenPathMap.get(pair).put(String.valueOf(chosenPathMap.get(pair).size() + 1), path);
    }
}
