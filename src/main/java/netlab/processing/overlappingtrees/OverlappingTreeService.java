package netlab.processing.overlappingtrees;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.PathMappingService;
import netlab.processing.shortestPaths.ShortestPathService;
import netlab.submission.enums.RoutingType;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.Connections;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class OverlappingTreeService {

    private ShortestPathService shortestPathService;
    private PathMappingService pathMappingService;

    @Autowired
    public OverlappingTreeService(ShortestPathService shortestPathService, PathMappingService pathMappingService) {
        this.shortestPathService = shortestPathService;
        this.pathMappingService = pathMappingService;
    }

    public Details solve(Request request, Topology topo){
        Details details = request.getDetails();
        Connections connections = details.getConnections();
        // Requirements
        Integer useMinS = connections.getUseMinS();
        Integer useMinD = connections.getUseMinD();

        Set<SourceDestPair> pairs = details.getPairs();
        long startTime = System.nanoTime();
        Map<SourceDestPair, Map<String, Path>> pathMap = findPaths(request.getRoutingType(), pairs, topo, useMinS, useMinD, request.getTrafficCombinationType());

        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setChosenPaths(pathMap);
        details.setRunningTimeSeconds(duration);
        return details;
    }

    private Map<SourceDestPair,Map<String,Path>> findPaths(RoutingType routingType, Set<SourceDestPair> pairs,
                                                           Topology topo, Integer useMinS, Integer useMinD,
                                                           TrafficCombinationType trafficCombinationType) {
        Map<SourceDestPair, Map<String, Path>> primaryTree = shortestPathService.findPaths(routingType, pairs, topo, useMinS,
                useMinD, trafficCombinationType);
        Set<Link> treeLinks = pathMappingService.getLinksFromMap(primaryTree);
        Set<Map<SourceDestPair, Map<String, Path>>> trees = new HashSet<>();
        for(Link link : treeLinks){
            Set<Link> newLinks = topo.getLinks();
            newLinks.remove(link);
            Topology modifiedTopo = new Topology(topo.getId(), topo.getNodes(), topo.getLinks());
            Map<SourceDestPair, Map<String, Path>> backupTree = shortestPathService.findPaths(routingType, pairs,
                    modifiedTopo, useMinS, useMinD, trafficCombinationType);
            trees.add(backupTree);
        }
        trees.add(primaryTree);
        return pathMappingService.mergeMaps(trees);
    }

}
