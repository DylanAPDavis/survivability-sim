package netlab.processing.shortestPaths;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.PathMappingService;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.RoutingType;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YensService {

    private MinimumCostPathService minimumCostPathService;
    private TopologyAdjustmentService topologyAdjustmentService;
    private PathMappingService pathMappingService;

    @Autowired
    public YensService(MinimumCostPathService minimumCostPathService, TopologyAdjustmentService topologyAdjustmentService,
                       PathMappingService pathMappingService){
        this.minimumCostPathService = minimumCostPathService;
        this.topologyAdjustmentService = topologyAdjustmentService;
        this.pathMappingService = pathMappingService;
    }

    public Details solve(Request request, Topology topo){
        Details details = request.getDetails();
        long startTime = System.nanoTime();
        details = findPaths(details, details.getPairs(), topo, request.getTrafficCombinationType());
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setRunningTimeSeconds(duration);
        return details;
    }

    private Details findPaths(Details details, Collection<SourceDestPair> pairs, Topology topo,
                              TrafficCombinationType trafficCombinationType){

        Map<SourceDestPair, Map<String, Path>> pathMap = new HashMap<>();
        Map<Node, Set<Path>> srcPathsMap = new HashMap<>();
        Map<Node, Set<Path>> dstPathsMap = new HashMap<>();
        // Iterate through each pair
        // For each pair, find two paths between that pair by traversing the cycle
        for(SourceDestPair pair : pairs){
            pathMap.put(pair, new HashMap<>());
            int k = Math.max(1, details.getConnections().getPairMinConnectionsMap().get(pair))
                    + details.getNumFailureEvents().getTotalNumFailureEvents();
            List<Path> paths = findPathSet(pair, topo, srcPathsMap, dstPathsMap, trafficCombinationType, k);
            int id = 1;
            for(Path path : paths){
                pathMap.get(pair).put(String.valueOf(id), path);
                id++;
            }
        }
        pathMappingService.setOriginalWeights(pathMap, topo.getLinkIdMap());

        if(pairs.size() > 1) {
            pathMappingService.filterMap(pathMap, details);
        }
        details.setChosenPaths(pathMap);
        details.setIsFeasible(true);
        return details;
    }

    private List<Path> findPathSet(SourceDestPair pair, Topology topo, Map<Node, Set<Path>> srcPathsMap,
                                   Map<Node, Set<Path>> dstPathsMap, TrafficCombinationType trafficCombinationType, int k) {
        Node src = pair.getSrc();
        Node dst = pair.getDst();


        Topology modifiedTopo = topologyAdjustmentService.adjustWeightsUsingTrafficCombination(topo, trafficCombinationType, src, dst,
                srcPathsMap, dstPathsMap);

        return computeKPaths(modifiedTopo, src, dst, k);

    }

    private List<Path> computeKPaths(Topology topology, Node src, Node dst, int K) {
        // Determine the shortest path from the source to the sink.
        //A[0] = Dijkstra(Graph, source, sink);
        List<Path> paths = new ArrayList<>();
        Set<String> pathIds = new HashSet<>();
        Path sp = minimumCostPathService.findShortestPath(src, dst, topology);
        paths.add(sp);
        String spId = sp.getLinks().stream().map(Link::getId).reduce("", (l1, l2) -> l1 + l2);
        pathIds.add(spId);
        // Initialize the heap to store the potential kth shortest path.
        //B = [];
        Set<Path> potentialPaths = new HashSet<>();

        Topology topo = topologyAdjustmentService.adjustWeightsToMaxWithLinks(topology, new HashSet<>());
        /*
        for k from 1 to K:
         */
        for(int k = 1; k < K; k++){
            /*
            // The spur node ranges from the first node to the next to last node in the previous k-shortest path.
            for i from 0 to size(A[k − 1]) − 1:
            */
            Path prevPath = paths.get(k-1);
            List<Link> prevPathLinks = prevPath.getLinks();
            List<Node> prevPathNodes = prevPath.getNodes();
            int numNodes = prevPathNodes.size();
            for(int i = 0; i < numNodes; i++){
                Path potentialPath = createPotentialPath(topo, i, prevPathNodes, prevPathLinks, paths, dst);
                if(potentialPath != null) {
                    potentialPaths.add(potentialPath);
                }
            }
            if(potentialPaths.isEmpty()){
                break;
            }
            else{
                // Sort the potential k-shortest paths by cost.
                //B.sort();
                Map<Path, String> combinedIdMap = potentialPaths.stream()
                        .collect(Collectors.toMap(
                                p -> p,
                                p -> p.getLinks().stream().map(Link::getId).reduce("", (l1, l2) -> l1 + l2))
                        );
                List<Path> sorted = potentialPaths.stream()
                        .sorted(Comparator.comparing(Path::getTotalWeight).thenComparing(combinedIdMap::get).reversed())
                        .collect(Collectors.toList());
                // Add the lowest cost path becomes the k-shortest path.
                //A[k] = B[0];
                //B.pop();
                while(!sorted.isEmpty()) {
                    Path newPath = sorted.remove(sorted.size() - 1);
                    String pathId = combinedIdMap.get(newPath);
                    if (!pathIds.contains(pathId)) {
                        paths.add(newPath);
                        pathIds.add(pathId);
                        break;
                    }
                }
            }

        }

        return paths;
    }

    private Path createPotentialPath(Topology topo, int i, List<Node> prevPathNodes, List<Link> prevPathLinks, List<Path> paths,
                                     Node dst){
               /* Spur node is retrieved from the previous k-shortest path, k − 1.
               spurNode = A[k-1].node(i);*/
        Node spurNode = prevPathNodes.get(i);
               /* The sequence of nodes from the source to the spur node of the previous k-shortest path.
               rootPath = A[k-1].nodes(0, i);*/
        List<Node> rootPath = prevPathNodes.subList(0, i);
        List<Link> rootLinks = prevPathLinks.subList(0, i);
               /*
               for each path p in A:
                */
        Set<Link> linksToGiveMaxWeight = new HashSet<>();
        for(Path path : paths){
                   /*if rootPath == p.nodes(0, i):
                       // Remove the links that are part of the previous shortest paths which share the same root path.
                       remove p.edge(i,i + 1) from Graph;*/
            List<Node> pathNodes = path.getNodes();
            List<Link> pathLinks = path.getLinks();
            if(i < pathLinks.size() && rootPath.equals(pathNodes.subList(0, i))){
                List<Link> rootLinksForThisPath = pathLinks.subList(0, i);
                if(pathLinks.size() > rootLinksForThisPath.size()){
                    Link nextLink = pathLinks.get(i);
                    linksToGiveMaxWeight.add(nextLink);
                }
            }
        }
       /*
       for each node rootPathNode in rootPath except spurNode:
           remove rootPathNode from Graph;
        */
        Set<Node> nodesToKeep = new HashSet<>(topo.getNodes());
        nodesToKeep.removeAll(rootPath);
        nodesToKeep.add(spurNode);
        // Get the topology with maximum weight links (when necessary) and removed root path nodes
        Topology modifiedTopo = topologyAdjustmentService.adjustWeightsToMaxWithLinksAndNodes(topo, nodesToKeep, linksToGiveMaxWeight);

        // Calculate the spur path from the spur node to the sink.
        //spurPath = Dijkstra(Graph, spurNode, sink);
        List<Link> spurPath = minimumCostPathService.findShortestPathLinks(spurNode, dst, modifiedTopo);
        if(spurPath.size() == 0){
            return null;
        }

        // Entire path is made up of the root path and spur path.
        // totalPath = rootPath + spurPath;
        List<Link> totalPath = new ArrayList<>(rootLinks);
        totalPath.addAll(spurPath);
        // Add the potential k-shortest path to the heap.
        //B.append(totalPath);
        return pathMappingService.convertToPath(totalPath, topo.getLinkIdMap());
    }
}
