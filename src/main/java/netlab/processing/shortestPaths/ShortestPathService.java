package netlab.processing.shortestPaths;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.RoutingType;
import netlab.submission.request.Connections;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ShortestPathService {

    public Details solve(Request request, Topology topo){
        Details details = request.getDetails();
        Map<SourceDestPair, Map<String, Path>> pathMap = new HashMap<>();
        Connections connections = details.getConnections();
        // Requirements
        Integer useMinS = connections.getUseMinS();
        Integer useMaxS = connections.getUseMaxS();
        Integer useMinD = connections.getUseMinD();
        Integer useMaxD = connections.getUseMaxD();

        Set<SourceDestPair> pairs = details.getPairs();
        long startTime = System.nanoTime();
        switch(request.getRoutingType()){
            case Unicast:
                SourceDestPair pair = pairs.iterator().next();
                Path path = findShortestPath(pair, topo);
                if(!path.getLinks().isEmpty()){
                    Map<String, Path> idMap = new HashMap<>();
                    idMap.put("1", path);
                    pathMap.put(pair, idMap);
                }
                break;
            default:
                pathMap = findPaths(request.getRoutingType(), pairs, details.getSources(), details.getDestinations(), topo, useMinS, useMaxS,
                        useMinD, useMaxD);
                break;
        }
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setChosenPaths(pathMap);
        details.setRunningTimeSeconds(duration);
        return details;
    }

    private Map<SourceDestPair,Map<String,Path>> findPaths(RoutingType routingType, Set<SourceDestPair> pairs,
                                                           Set<Node> sources, Set<Node> destinations, Topology topo,
                                                           Integer useMinS, Integer useMaxS, Integer useMinD, Integer useMaxD) {
        Map<SourceDestPair, Map<String, Path>> pathMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> new HashMap<>()));
        Map<Node, Set<Path>> usedSources = new HashMap<>();
        Map<Node, Set<Path>> usedDestinations = new HashMap<>();

        Map<Path, SourceDestPair> potentialPathMap = new HashMap<>();
        List<Path> potentialPaths = new ArrayList<>();
        for(SourceDestPair pair : pairs){
            Path sp = findShortestPath(pair, topo);
            potentialPaths.add(sp);
            potentialPathMap.put(sp, pair);
        }

        // If you're doing Broadcast or Multicast, you're done
        if(routingType.equals(RoutingType.Broadcast) || routingType.equals(RoutingType.Multicast)){
            return formatPathMap(potentialPathMap);
        }


        // Sort the paths by weight
        potentialPaths = potentialPaths.stream().sorted(Comparator.comparingLong(Path::getTotalWeight)).collect(Collectors.toList());
        // Pick a subset of the paths to satisfy the min constraints
        for(Path path : potentialPaths){
            SourceDestPair pair = potentialPathMap.get(path);
            if(!usedSources.containsKey(pair.getSrc()) || !usedDestinations.containsKey(pair.getDst())) {
                usedSources.get(pair.getSrc()).add(path);
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
            return formatPathMap(potentialPathMap);
        }

        return pathMap;
    }

    private Map<SourceDestPair,Map<String,Path>> formatPathMap(Map<Path, SourceDestPair> potentialPathMap) {
        Map<SourceDestPair, Map<String, Path>> pathMap = new HashMap<>();
        for(Path path : potentialPathMap.keySet()){
            SourceDestPair pair = potentialPathMap.get(path);
            Map<String, Path> mapForPair = pathMap.getOrDefault(pair, new HashMap<>());
            String id = String.valueOf(mapForPair.size() + 1);
            mapForPair.put(id, path);
            pathMap.put(pair, mapForPair);
        }
        return pathMap;
    }

    public Path findShortestPath(SourceDestPair pair, Topology topo){

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
