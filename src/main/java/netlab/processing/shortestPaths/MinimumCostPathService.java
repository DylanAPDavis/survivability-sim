package netlab.processing.shortestPaths;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.PathMappingService;
import netlab.submission.enums.RoutingType;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.Connections;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyAdjustmentService;
import netlab.topology.services.TopologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MinimumCostPathService {

    private AStarService aStarService;
    private TopologyAdjustmentService topologyAdjustmentService;
    private PathMappingService pathMappingService;

    @Autowired
    public MinimumCostPathService(AStarService aStarService, TopologyAdjustmentService topologyAdjustmentService,
                                  PathMappingService pathMappingService){
        this.aStarService = aStarService;
        this.topologyAdjustmentService = topologyAdjustmentService;
        this.pathMappingService = pathMappingService;
    }

    public Details solve(Request request, Topology topo){
        Details details = request.getDetails();

        List<SourceDestPair> pairs = topologyAdjustmentService.sortPairsByPathCost(details.getPairs(), topo);
        long startTime = System.nanoTime();
        Map<SourceDestPair, Map<String, Path>> pathMap =  findPaths(request.getDetails(), request.getRoutingType(),
                pairs, topo, request.getTrafficCombinationType(), true);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setChosenPaths(pathMap);
        details.setRunningTimeSeconds(duration);
        details.setIsFeasible(true);
        return details;
    }

    public Map<SourceDestPair, Map<String,Path>> findPaths(Details details, RoutingType routingType, Collection<SourceDestPair> pairs, Topology topo,
                                                          TrafficCombinationType trafficCombinationType, Boolean filter) {
        Map<Node, Set<Path>> usedSources = new HashMap<>();
        Map<Node, Set<Path>> usedDestinations = new HashMap<>();

        Map<Path, SourceDestPair> potentialPathMap = new HashMap<>();
        for(SourceDestPair pair : pairs){
            Path sp = findShortestPath(pair, topo, usedSources, usedDestinations, trafficCombinationType);
            if(sp.getLinks().size() > 0) {
                potentialPathMap.put(sp, pair);
                usedSources.putIfAbsent(pair.getSrc(), new HashSet<>());
                usedSources.get(pair.getSrc()).add(sp);
                usedDestinations.putIfAbsent(pair.getDst(), new HashSet<>());
                usedDestinations.get(pair.getDst()).add(sp);
            }
        }

        // If you're doing Broadcast or Multicast, you're done
        Map<SourceDestPair, Map<String, Path>> pathMap = pathMappingService.formatPathMap(potentialPathMap);
        if(routingType.equals(RoutingType.Broadcast) || routingType.equals(RoutingType.Multicast) || pairs.size() == 1){
            return pathMap;
        }

        return filter ? pathMappingService.filterMap(pathMap, details) : pathMap;

    }

    public Path findShortestPath(SourceDestPair pair, Topology topo){
        return findShortestPath(pair, topo, new HashMap<>(), new HashMap<>(), TrafficCombinationType.None);
    }

    public Path findShortestPath(Node src, Node dst, Topology topo){
        return pathMappingService.convertToPath(aStarService.shortestPath(topo, src, dst), topo.getLinkIdMap());
    }

    public List<Link> findShortestPathLinks(Node src, Node dst, Topology topo){
        return aStarService.shortestPath(topo, src, dst);
    }

    public Path findShortestPath(SourceDestPair pair, Topology topo, Map<Node, Set<Path>> srcPathsMap,
                                 Map<Node, Set<Path>> dstPathsMap, TrafficCombinationType trafficType){

        Node src = pair.getSrc();
        Node dst = pair.getDst();

        Topology modifiedTopo = topologyAdjustmentService.adjustWeightsUsingTrafficCombination(topo, trafficType, src, dst,
                srcPathsMap, dstPathsMap);
        List<Link> pathLinks = findShortestPathLinks(src, dst, modifiedTopo);

        return pathMappingService.convertToPath(pathLinks, topo.getLinkIdMap());
    }

    public Map<SourceDestPair, Path> findAllShortestPaths(Topology topo){
        Map<SourceDestPair, List<Link>> allPathsMap = aStarService.allShortestPaths(topo);
        return allPathsMap.keySet().stream()
                .collect(Collectors.toMap(p -> p, p -> new Path(allPathsMap.get(p))));
    }

}
