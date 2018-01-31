package netlab.topology.services;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.disjointpaths.BhandariService;
import netlab.processing.shortestPaths.MinimumCostPathService;
import netlab.processing.shortestPaths.YensService;
import netlab.storage.services.StorageService;
import netlab.topology.elements.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TopologyMetricsService {

    private YensService yensService;
    private BhandariService bhandariService;
    private StorageService storageService;

    @Autowired
    public TopologyMetricsService(YensService yensService, BhandariService bhandariService, StorageService storageService){
        this.yensService = yensService;
        this.bhandariService = bhandariService;
        this.storageService = storageService;
    }

    public TopologyMetrics generateMetrics(Topology topo){
        TopologyMetrics tm = storageService.retrieveTopologyMetrics(topo.getId());
        if(tm != null){
            return tm;
        }
        Map<String, Path> pathIdMap = new HashMap<>();
        Map<SourceDestPair, List<String>> minCostPaths = new HashMap<>();
        Map<SourceDestPair, List<String>> linkDisjointPaths = new HashMap<>();
        Map<SourceDestPair, List<String>> nodeDisjointPaths = new HashMap<>();

        for(Node node : topo.getNodes()){
            for(Node otherNode : topo.getNodes()){
                // Only find paths between unique pairs
                if(!node.getId().equals(otherNode.getId())){
                    SourceDestPair pair = new SourceDestPair(node, otherNode);
                    // Get k shortest paths
                    List<Path> kShortestPaths = yensService.computeKPaths(topo, node, otherNode, 20);
                    List<String> kIds = kShortestPaths.stream().map(Path::getId).collect(Collectors.toList());
                    minCostPaths.put(pair, kIds);
                    // Get k disjoint paths
                    List<Path> kLinkDisjointPaths = bhandariService.computeDisjointPaths(topo, node, otherNode, 20, false);
                    List<String> lDisjointIds = kLinkDisjointPaths.stream().map(Path::getId).collect(Collectors.toList());
                    linkDisjointPaths.put(pair, lDisjointIds);
                    List<Path> kNodeDisjointPaths = bhandariService.computeDisjointPaths(topo, node, otherNode, 20, true);
                    List<String> nDisjointIds = kNodeDisjointPaths.stream().map(Path::getId).collect(Collectors.toList());
                    nodeDisjointPaths.put(pair, nDisjointIds);
                    // Store all path IDs
                    addToPathIdMap(pathIdMap, kShortestPaths);
                    addToPathIdMap(pathIdMap, kLinkDisjointPaths);
                    addToPathIdMap(pathIdMap, kNodeDisjointPaths);
                }
            }
        }

        tm = new TopologyMetrics(topo.getId(), pathIdMap, minCostPaths, linkDisjointPaths, nodeDisjointPaths);
        storageService.storeTopologyMetrics(tm);
        return tm;
    }

    private void addToPathIdMap(Map<String, Path> pathIdMap, List<Path> paths) {
        for(Path path : paths){
            pathIdMap.put(path.getId(), path);
        }
    }
}
