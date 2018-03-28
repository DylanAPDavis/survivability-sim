package netlab.processing.groupcast;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.PathMappingService;
import netlab.processing.shortestPaths.MinimumCostPathService;
import netlab.submission.enums.TrafficCombinationType;
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
public class MemberForwardingService {

    private MinimumCostPathService minimumCostPathService;
    private PathMappingService pathMappingService;
    private TopologyAdjustmentService topologyService;

    @Autowired
    public MemberForwardingService(MinimumCostPathService minimumCostPathService, PathMappingService pathMappingService,
                                   TopologyAdjustmentService topologyService) {
        this.minimumCostPathService = minimumCostPathService;
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
        Set<Node> options = new HashSet<>(topo.getNodes());
        options.removeAll(dests);
        Map<SourceDestPair, Double> pathCostMap = topo.getMinimumPathCostMap();

        Map<Node, List<Double>> distanceToEachNode = new HashMap<>();
        Map<Node, Map<Node, Double>> costFromNodeToDst = new HashMap<>();
        // Get the costs of each src to each other node
        for(Node src : sources){
            Map<Node, Double> mapToOtherNodes = createDistanceMap(src, options, pathCostMap);
            for(Node node : mapToOtherNodes.keySet()){
                distanceToEachNode.putIfAbsent(node, new ArrayList<>());
                distanceToEachNode.get(node).add(mapToOtherNodes.get(node));
            }
        }
        // Get the costs of each node to each dst
        for(Node node : options){
            Map<Node, Double> mapToDests = createDistanceMap(node, dests, pathCostMap);
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

        Node minNode = null;
        for(Node node : options){
            if(minNode == null){
                minNode = node;
            } else{
                if(worstCaseDistanceUsingNode.get(node) <= worstCaseDistanceUsingNode.get(minNode)){
                    if(worstCaseDistanceUsingNode.get(node) < worstCaseDistanceUsingNode.get(minNode)
                            || totalDistanceUsingNode.get(node) < totalDistanceUsingNode.get(minNode)){
                        minNode = node;
                    }
                }
            }
        }

        // Route each s in S to each destination combining the path to bestS and the path from bestS to each dst

        Map<SourceDestPair, Map<String, Path>> chosenPathsMap = new HashMap<>();
        if(minNode != null){
            for(Node src : sources){
                boolean isMin = src.getId().equals(minNode.getId());
                Path toMinNode = isMin ? null : minimumCostPathService.findShortestPath(src, minNode, topo);
                for(Node dst : dests){
                    boolean srcIsDst = src.getId().equals(dst.getId());
                    // If the source is this destination, don't find a path
                    if(srcIsDst){
                        continue;
                    }
                    SourceDestPair srcDst = new SourceDestPair(src, dst);
                    Path completePath;
                    boolean minNodeIsDst = minNode.getId().equals(dst.getId());
                    // If the min src is this destination, you already have a path to reach it
                    if(minNodeIsDst){
                        completePath = toMinNode;
                    }
                    // Otherwise, you need to find a path from the src to the destination
                    // Start at the minSrc, then prepend the path to the minSrc
                    else{
                        Path toDstFromMinNode = minimumCostPathService.findShortestPath(minNode, dst, topo);
                        completePath = isMin ? toDstFromMinNode : toMinNode.combinePaths(toDstFromMinNode);
                    }
                    chosenPathsMap.putIfAbsent(srcDst, new HashMap<>());
                    chosenPathsMap.get(srcDst).put(completePath.getId(), completePath);
                }
            }

        }


        /*

        // First, get the costs to get from each src to each dest, then each dest to each other dest
        Map<Node, Map<Node, Double>> costToMemberMap = new HashMap<>();
        Map<Node, Map<Node, Double>> costFromMemberMap = new HashMap<>();
        for(SourceDestPair pair : pathCostMap.keySet()) {
            Node origin = pair.getSrc();
            Node target = pair.getDst();
            Double cost = pathCostMap.get(pair);
            if (sources.contains(origin) && members.contains(target)) {
                costToMemberMap.putIfAbsent(target, new HashMap<>());
                costToMemberMap.get(target).put(origin, cost);
            }
            if (members.contains(origin) && dests.contains(target)) {
                costFromMemberMap.putIfAbsent(origin, new HashMap<>());
                costFromMemberMap.get(origin).put(target, cost);
            }
        }

        // Goal: Route from each source to one of the destinations
        // Then, route from that destination to the remaining destinations

        // Pick the destination with the lowest minimum cost to get to from the sources
        // AND to get to the other destinations
        Node minimumCostMember = null;
        Double minimumCost = Double.MAX_VALUE;
        for(Node member : members){
            Map<Node, Double> costToMap = costToMemberMap.get(member);
            Map<Node, Double> costFromMap = costToMemberMap.get(member);
            Double sum = costToMap != null ? costToMap.values().stream().reduce(0.0, (c1, c2) -> c1 + c2) : Double.MAX_VALUE;
            sum += costFromMap != null ? costFromMap.values().stream().reduce(0.0, (c1, c2) -> c1 + c2) : Double.MAX_VALUE;
            if(sum < minimumCost){
                minimumCost = sum;
                minimumCostMember = member;
            }
        }

        if(minimumCostMember == null){
            details.setIsFeasible(false);
            return details;
        }

        // You now have a dest with minimum cost to reach/transmit from, so save the necessary routes
        Map<Node, Set<Path>> srcPathsMap = new HashMap<>();
        Map<Node, Set<Path>> dstPathsMap = new HashMap<>();
        Map<SourceDestPair, Map<String, Path>> chosenPathsMap = new HashMap<>();
        Map<SourceDestPair, Path> temporaryPathMap = new HashMap<>();
        // Get a path from each source to the minimum cost dest
        for(Node src : sources){
            if(src != minimumCostMember){
                SourceDestPair pair = SourceDestPair.builder().src(src).dst(minimumCostMember).build();
                Path sp = minimumCostPathService.findShortestPath(pair, topo, srcPathsMap, dstPathsMap, trafficCombinationType);
                updateMaps(pair, sp, srcPathsMap, dstPathsMap, chosenPathsMap, dests.contains(minimumCostMember));
                temporaryPathMap.put(pair, sp);
            }
        }
        // Get a path from the minimum cost dest to each other dest
        for(Node otherDest : dests){
            if(otherDest != minimumCostMember){
                SourceDestPair minDestToOtherDestPair = SourceDestPair.builder().src(minimumCostMember).dst(otherDest).build();
                Path minDestToOtherDestPath = minimumCostPathService.findShortestPath(minDestToOtherDestPair, topo, srcPathsMap, dstPathsMap, trafficCombinationType);
                // Then, with that path create a new src -> other dest path for each source
                for(Node src : sources) {
                    if(src != otherDest) {
                        SourceDestPair srcOtherDestPair = SourceDestPair.builder().src(src).dst(otherDest).build();
                        SourceDestPair srcToMinDestPair = SourceDestPair.builder().src(src).dst(minimumCostMember).build();
                        // If you've established a src -> min dest path, append on that new min dest -> other dest path
                        if(temporaryPathMap.containsKey(srcToMinDestPair)){
                            Path combinedPath = temporaryPathMap.get(srcToMinDestPair).combinePaths(minDestToOtherDestPath);
                            updateMaps(srcOtherDestPair, combinedPath, srcPathsMap, dstPathsMap, chosenPathsMap, true);
                        }
                        // If this source is also a minimum cost dest, just use the direct MinCostDest -> OtherDest route
                        else if(src == minimumCostMember && pairs.contains(srcOtherDestPair)){
                            updateMaps(srcOtherDestPair, minDestToOtherDestPath, srcPathsMap, dstPathsMap, chosenPathsMap, true);
                        }
                    }
                }
            }
        }

        // Path filtering
        chosenPathsMap = pathMappingService.filterMap(chosenPathsMap, details);
        */
        details.setChosenPaths(chosenPathsMap);
        details.setIsFeasible(true);

        return details;
    }

    public Map<Node, Double> createDistanceMap(Node src, Set<Node> otherNodes, Map<SourceDestPair, Double> pathCostMap){
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

    public void updateMaps(SourceDestPair pair, Path path, Map<Node, Set<Path>> srcPathsMap, Map<Node, Set<Path>> dstPathsMap,
                           Map<SourceDestPair, Map<String, Path>> chosenPathMap, boolean updateChosenPaths){
        srcPathsMap.putIfAbsent(pair.getSrc(), new HashSet<>());
        srcPathsMap.get(pair.getSrc()).add(path);
        dstPathsMap.putIfAbsent(pair.getDst(), new HashSet<>());
        dstPathsMap.get(pair.getDst()).add(path);
        if(updateChosenPaths) {
            chosenPathMap.putIfAbsent(pair, new HashMap<>());
            chosenPathMap.get(pair).put(String.valueOf(chosenPathMap.get(pair).size() + 1), path);
        }
    }
}
