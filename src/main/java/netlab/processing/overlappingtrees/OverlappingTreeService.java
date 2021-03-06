package netlab.processing.overlappingtrees;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.PathMappingService;
import netlab.processing.shortestPaths.MinimumCostPathService;
import netlab.submission.enums.RoutingType;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.Connections;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class OverlappingTreeService {

    private MinimumCostPathService minimumCostPathService;
    private PathMappingService pathMappingService;
    private TopologyAdjustmentService topologyService;

    @Autowired
    public OverlappingTreeService(MinimumCostPathService minimumCostPathService, PathMappingService pathMappingService, TopologyAdjustmentService topologyService) {
        this.minimumCostPathService = minimumCostPathService;
        this.pathMappingService = pathMappingService;
        this.topologyService = topologyService;
    }

    public Details solve(Request request, Topology topo){
        Details details = request.getDetails();
        Connections connections = details.getConnections();
        // Requirements
        Integer useMinS = connections.getUseMinS();
        Integer useMinD = connections.getUseMinD();


        // Sort pairs by shortest path cost
        List<SourceDestPair> pairs = topologyService.sortPairsByPathCost(details.getPairs(), topo);

        long startTime = System.nanoTime();
        details = findPaths(details, request.getRoutingType(), pairs, topo, useMinS, useMinD, request.getTrafficCombinationType());
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setRunningTimeSeconds(duration);
        return details;
    }

    private Details findPaths(Details details, RoutingType routingType, Collection<SourceDestPair> pairs,
                              Topology topo, Integer useMinS, Integer useMinD, TrafficCombinationType trafficCombinationType) {
        Map<SourceDestPair, Map<String, Path>> primaryTree = minimumCostPathService.findPaths(details, routingType, pairs,
                topo,  trafficCombinationType, true);
        Set<Link> treeLinks = pathMappingService.getLinksFromMap(primaryTree);
        List<Map<SourceDestPair, Map<String, Path>>> trees = new ArrayList<>();
        boolean feasible = true;
        for(Link link : treeLinks){
            Set<Link> newLinks = new HashSet<>(topo.getLinks());
            newLinks.remove(link);
            Topology modifiedTopo = new Topology(topo.getId(), topo.getNodes(), newLinks);
            Map<SourceDestPair, Map<String, Path>> backupTree = minimumCostPathService.findPaths(details, routingType, pairs,
                    modifiedTopo, trafficCombinationType, true);
            if(pathMappingService.countPaths(backupTree) == 0){
                feasible = false;
            }
            trees.add(backupTree);
        }
        trees.add(primaryTree);
        details.setChosenPaths(pathMappingService.mergeMaps(trees));
        details.setIsFeasible(feasible);
        return details;
    }

}
