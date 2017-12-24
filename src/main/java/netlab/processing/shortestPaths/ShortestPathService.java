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
public class ShortestPathService {

    private BellmanFordService bellmanFordService;
    private TopologyAdjustmentService topologyAdjustmentService;
    private PathMappingService pathMappingService;

    @Autowired
    public ShortestPathService(BellmanFordService bellmanFordService, TopologyAdjustmentService topologyAdjustmentService,
                               PathMappingService pathMappingService){
        this.bellmanFordService = bellmanFordService;
        this.topologyAdjustmentService = topologyAdjustmentService;
        this.pathMappingService = pathMappingService;
    }

    public Details solve(Request request, Topology topo){
        Details details = request.getDetails();
        Map<SourceDestPair, Map<String, Path>> pathMap = new HashMap<>();

        List<SourceDestPair> pairs = topologyAdjustmentService.sortPairsByPathCost(details.getPairs(), topo);
        long startTime = System.nanoTime();
        switch(request.getRoutingType()){
            case Unicast:
                SourceDestPair pair = pairs.iterator().next();
                Path path = findShortestPath(pair, topo, new HashMap<>(), new HashMap<>(), TrafficCombinationType.None);
                if(!path.getLinks().isEmpty()){
                    Map<String, Path> idMap = new HashMap<>();
                    idMap.put("1", path);
                    pathMap.put(pair, idMap);
                }
                break;
            default:
                pathMap = findPaths(request.getDetails(), request.getRoutingType(), pairs, topo, request.getTrafficCombinationType(), true);
                break;
        }
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setChosenPaths(pathMap);
        details.setRunningTimeSeconds(duration);
        details.setIsFeasible(true);
        return details;
    }

    public Map<SourceDestPair,Map<String,Path>> findPaths(Details details, RoutingType routingType, Collection<SourceDestPair> pairs, Topology topo,
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


        /*
        // Sort the paths by weight
        usedSources = new HashMap<>();
        usedDestinations = new HashMap<>();
        potentialPaths = potentialPaths.stream().sorted(Comparator.comparingLong(Path::getTotalWeight)).collect(Collectors.toList());
        // Pick a subset of the paths to satisfy the min constraints
        for(Path path : potentialPaths){
            SourceDestPair pair = potentialPathMap.get(path);
            if(!usedSources.containsKey(pair.getSrc()) || !usedDestinations.containsKey(pair.getDst())) {
                usedSources.putIfAbsent(pair.getSrc(), new HashSet<>());
                usedSources.get(pair.getSrc()).add(path);
                usedDestinations.putIfAbsent(pair.getDst(), new HashSet<>());
                usedDestinations.get(pair.getDst()).add(path);
                pathMap.get(pair).put(String.valueOf(pathMap.get(pair).size() + 1), path);
            }
            if(usedSources.size() >= useMinS && usedDestinations.size() >= useMinD){
                break;
            }
        }


        if(routingType.equals(RoutingType.ManyToMany)){
            // Prune out potentially unneeded paths
            for(Node src : usedSources.keySet()){
                // If this source has more than one path, some could potentially be removed
                if(usedSources.get(src).size() > 1){
                    // Get the connected destinations
                    Set<Path> srcPaths = usedSources.get(src);
                    int pathsRemaining = srcPaths.size();
                    for(Path srcPath : srcPaths){
                        Node dest = srcPath.getNodes().get(srcPath.getNodes().size()-1);
                        if(usedDestinations.get(dest).size() > 1){
                            // If they do have more than one path, we now know that both this source and this dest
                            // are the endpoints for multiple paths.
                            // This means we can remove the path
                            usedSources.get(src).remove(srcPath);
                            usedDestinations.get(dest).remove(srcPath);
                            pathsRemaining--;
                            potentialPathMap.remove(srcPath);
                        }
                        // If there's just one path left for this source, move on to the next one
                        if(pathsRemaining == 1){
                            break;
                        }
                    }
                }
            }
            return pathMappingService.formatPathMap(potentialPathMap);
        }
        return pathMap;
        */

    }

    public Path findShortestPath(SourceDestPair pair, Topology topo){
        return findShortestPath(pair, topo, new HashMap<>(), new HashMap<>(), TrafficCombinationType.None);
    }

    public Path findShortestPath(SourceDestPair pair, Topology topo, Map<Node, Set<Path>> srcPathsMap,
                                 Map<Node, Set<Path>> dstPathsMap, TrafficCombinationType trafficType){

        Node src = pair.getSrc();
        Node dst = pair.getDst();

        Topology modifiedTopo = topologyAdjustmentService.adjustWeightsUsingTrafficCombination(topo, trafficType, src, dst,
                srcPathsMap, dstPathsMap);
        List<Link> pathLinks = bellmanFordService.shortestPath(modifiedTopo, src, dst);

        return pathMappingService.convertToPath(pathLinks, topo.getLinkIdMap());
    }

    public Map<SourceDestPair, Path> findAllShortestPaths(Topology topo){
        Map<SourceDestPair, List<Link>> allPathsMap = bellmanFordService.allShortestPaths(topo);
        return allPathsMap.keySet().stream()
                .collect(Collectors.toMap(p -> p, p -> new Path(allPathsMap.get(p))));
    }


    // Confirm that you've met all requirements
    private boolean testSatisfication(Integer reqNumConnections, Integer useMinS, Integer useMaxS, Integer useMinD,
                                      Integer useMaxD, Map<SourceDestPair, Integer> pairMinConnectionsMap,
                                      Map<SourceDestPair, Integer> pairMaxConnectionsMap,
                                      Map<Node, Integer> srcMinConnectionsMap, Map<Node, Integer> srcMaxConnectionsMap,
                                      Map<Node, Integer> dstMinConnectionsMap, Map<Node, Integer> dstMaxConnectionsMap,
                                      int numConnections, int numSUsed, int numDUsed,
                                      Map<SourceDestPair, Integer> pairConnectionsMap,
                                      Map<Node, Integer> srcConnectionsMap, Map<Node, Integer> dstConnectionsMap) {

        boolean enoughConns = numConnections >= reqNumConnections;
        boolean enoughS = numSUsed >= useMinS && numSUsed <= useMaxS;
        boolean enoughD = numDUsed >= useMinD && numDUsed <= useMaxD;
        boolean enoughPairs = pairConnectionsMap.keySet()
                .stream()
                .allMatch(p -> pairConnectionsMap.get(p) >= pairMinConnectionsMap.get(p)
                        && pairConnectionsMap.get(p) <= pairMaxConnectionsMap.get(p));
        boolean enoughSConn = srcConnectionsMap.keySet()
                .stream()
                .allMatch(s -> srcConnectionsMap.get(s) >= srcMinConnectionsMap.get(s)
                        && srcConnectionsMap.get(s) <= srcMaxConnectionsMap.get(s));
        boolean enoughDConn = dstConnectionsMap.keySet()
                .stream()
                .allMatch(d -> dstConnectionsMap.get(d) >= dstMinConnectionsMap.get(d)
                        && dstConnectionsMap.get(d) <= dstMaxConnectionsMap.get(d));

        return enoughConns && enoughS && enoughD && enoughPairs && enoughSConn && enoughDConn;
    }
}
