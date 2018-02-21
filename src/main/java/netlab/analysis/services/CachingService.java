package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.CachingResult;
import netlab.processing.pathmapping.PathMappingService;
import netlab.submission.enums.RoutingType;
import netlab.topology.elements.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CachingService {

    private PathMappingService pathMappingService;

    @Autowired
    public CachingService(PathMappingService pathMappingService){
        this.pathMappingService = pathMappingService;
    }


    public void buildCacheMaps(List<CachingResult> cachingResults, Map<SourceDestPair, Map<String, Path>> chosenPathsMap,
                                Set<Failure> failures) {
        Map<SourceDestPair, Path> primaryPathMap = pathMappingService.buildPrimaryPathMap(chosenPathsMap);
        for(CachingResult cachingResult : cachingResults){
            Set<Node> cachingLocations = new HashSet<>();
            switch(cachingResult.getType()){
                case None:
                    cachingLocations = cacheAtDest(primaryPathMap);
                    break;
                case EntirePath:
                    cachingLocations = cacheAlongPath(primaryPathMap);
                    break;
                case SourceAdjacent:
                    cachingLocations = cacheNextToSource(primaryPathMap);
                    break;
                case FailureAware:
                    cachingLocations = cacheOutsideFailures(primaryPathMap, failures);
                    break;
                case BranchingPoint:
                    cachingLocations = cacheAtBranchingPoints(primaryPathMap, chosenPathsMap);
                    break;
                case LeaveCopyDown:
                    cachingLocations = cacheLeaveCopyDown(primaryPathMap);
            }
            cachingResult.setCachingCost(evaluateCost(cachingLocations));
            cachingResult.setCachingLocations(cachingLocations);
        }
    }


    private double evaluateCost(Map<SourceDestPair, Set<Node>> cacheMap) {
        Map<Node, Set<Node>> cachePointsPerDestination = new HashMap<>();
        for(SourceDestPair pair : cacheMap.keySet()){
            Node dest = pair.getDst();
            cachePointsPerDestination.putIfAbsent(dest, new HashSet<>());
            cachePointsPerDestination.get(dest).addAll(cacheMap.get(pair));
        }
        double count = 0.0;
        for(Node dest : cachePointsPerDestination.keySet()){
            Set<Node> cachePoints = cachePointsPerDestination.get(dest);
            count += cachePoints.size();
        }
        return count;
    }

    private Set<Node> cacheAtBranchingPoints(Map<SourceDestPair, Path> primaryPathMap, Map<SourceDestPair, Map<String, Path>> otherPathsMap) {

        Set<Node> cachingPoints = new HashSet<>();
        Set<Path> allPaths = otherPathsMap.values().stream().map(Map::values).flatMap(Collection::stream).collect(Collectors.toSet());
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Path primary = primaryPathMap.get(pair);
            List<Node> primaryNodes = primary.getNodes();
            allPaths.remove(primary);
            Set<Node> nodesInOtherPaths = allPaths.stream().map(Path::getNodes).flatMap(Collection::stream).collect(Collectors.toSet());
            // Check to see if any nodes from all other paths are shared by the primary path
            // If so, cache there
            for(Node primaryNode : primaryNodes){
                if(nodesInOtherPaths.contains(primaryNode)){
                    cachingPoints.add(primaryNode);
                }
            }
            cachingPoints.add(pair.getDst());
            // Add the primary path back to the set of all paths so other pairs can check it
            allPaths.add(primary);
        }
        return cachingPoints;
    }

    private Set<Node> cacheOutsideFailures(Map<SourceDestPair, Path> primaryPathMap,
                                      Set<Failure> failures) {
        // The goal is to avoid caching at locations that either can:
        // (a) Fail.
        // (b) Become disconnected from a source due to another failure.
        // For each path, determine which nodes remain reachable.
        // Cache at the closest one
        Set<Node> cachingPoints = new HashSet<>();
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Set<Node> cachingNodes = new HashSet<>();
            Path primary = primaryPathMap.get(pair);
            List<Node> reachableNodes = pathMappingService.getReachableNodes(primary, failures);
            // Cache at the first reachable node along the path
            if(!reachableNodes.isEmpty()){
                cachingNodes.addAll(reachableNodes);
            }
            cachingNodes.add(pair.getDst());
            cachingNodes.remove(pair.getSrc());
            cachingPoints.addAll(cachingNodes);
        }
        return cachingPoints;
    }

    private Set<Node> cacheAtDest(Map<SourceDestPair, Path> primaryPathMap){
        // Just cache at the destination
        Set<Node> cachingPoints = new HashSet<>();
        for(SourceDestPair pair : primaryPathMap.keySet()){
            if(primaryPathMap.get(pair) != null) {
                cachingPoints.add(pair.getDst());
            }
        }
        return cachingPoints;
    }

    private Set<Node> cacheAlongPath(Map<SourceDestPair, Path> primaryPathMap){
        // Cache at every node along the path (excluding the source)
        Set<Node> cachingPoints = new HashSet<>();
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Path primary = primaryPathMap.get(pair);
            if(primary != null) {
                cachingPoints.addAll(primary.getNodes());
                cachingPoints.remove(pair.getSrc());
                cachingPoints.add(pair.getDst());
            }
        }
        return cachingPoints;
    }

    private Set<Node> cacheNextToSource(Map<SourceDestPair, Path> primaryPathMap) {
        // Cache next to every source.
        // Cache at the first non-source node on every path
        Set<Node> cachingPoints = new HashSet<>();
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Path primary = primaryPathMap.get(pair);
            if(primary != null) {
                // Every path has at least two nodes
                cachingPoints.add(primary.getNodes().get(1));
                cachingPoints.add(pair.getDst());
            }
        }
        return cachingPoints;
    }

    private Set<Node> cacheLeaveCopyDown(Map<SourceDestPair, Path> primaryPathMap) {
        // Cache next to every destination
        Set<Node> cachingPoints = new HashSet<>();
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Path primary = primaryPathMap.get(pair);
            if(primary != null){
                List<Node> pathNodes = primary.getNodes();
                Node secondToLast = pathNodes.get(pathNodes.size()-2);
                // If the second to last is not the source (i.e. there is an intermediate node), then cache there
                if(!secondToLast.getId().equals(pair.getSrc().getId())) {
                    cachingPoints.add(secondToLast);
                }
                cachingPoints.add(pair.getDst());
            }
        }
        return cachingPoints;
    }


    public void evaluateContentAccessibility(List<CachingResult> cachingResults,
                                             Map<SourceDestPair, Map<String, Path>> chosenPaths,
                                             Collection<Failure> chosenFailures, Integer useMinD) {

        Map<SourceDestPair, Path> primaryPathMap = pathMappingService.buildPrimaryPathMap(chosenPaths);
        for (CachingResult cachingResult : cachingResults) {
            Set<Node> cachingLocations = cachingResult.getCachingLocations();

            Map<Node, Set<Path>> pathsPerSrc = new HashMap<>();
            Map<Node, Path> primaryPathPerSrc = new HashMap<>();
            // Go through all of the pairs, get all paths per source
            for (SourceDestPair pair : chosenPaths.keySet()) {
                Node src = pair.getSrc();
                Set<Path> paths = chosenPaths.get(pair).values().stream().collect(Collectors.toSet());
                if(paths.size() > 0) {
                    // Store all paths for this pair
                    pathsPerSrc.putIfAbsent(src, new HashSet<>());
                    pathsPerSrc.get(src).addAll(paths);
                    // Get the primary path for this pair
                    Path potentialPrimaryPath = primaryPathMap.get(pair);
                    if(primaryPathPerSrc.containsKey(src)){
                        // If this new potential primary path weighs less than the current, replace the current
                        if(potentialPrimaryPath.getTotalWeight() < primaryPathPerSrc.get(src).getTotalWeight()){
                            primaryPathPerSrc.put(src, potentialPrimaryPath);
                        }
                    } else{
                        primaryPathPerSrc.put(src, potentialPrimaryPath);
                    }
                }

            }

            Map<Node, Integer> hopCountBefore= new HashMap<>();
            Map<Node, Integer> hopCountAfter = new HashMap<>();
            Set<Node> reachOnPrimaryAfter= new HashSet<>();
            Set<Node> reachOnBackupAfter = new HashSet<>();
            Set<Node> onlyReachOnBackupAfter = new HashSet<>();
            // Check for content accesibility
            for(Node src : pathsPerSrc.keySet()){
                Integer hopCountToContentBefore = 0;
                Integer hopCountToContentAfter = 0;
                // Examine the primary path first
                Path primary = primaryPathPerSrc.get(src);
                Set<Path> allPaths = pathsPerSrc.get(src);
                allPaths.remove(primary);
                // First, get the hop count to content before failure
                for(Node node : primary.getNodes()){
                    if(checkIfHit(node, cachingLocations, src)){
                        break;
                    }
                    hopCountToContentBefore++;
                }

                // Next, see if we can reach the content on the primary path
                List<Node> reachableNodes = pathMappingService.getReachableNodes(primary, chosenFailures);
                boolean primaryHit = false;
                for(Node node : reachableNodes){
                    primaryHit = checkIfHit(node, cachingLocations, src);
                    if(primaryHit){
                        break;
                    }
                    hopCountToContentAfter++;
                }
                // Check if you were able to still reach the content on the primary path after failure
                if(primaryHit){
                    reachOnPrimaryAfter.add(src);
                }
                // If not, you can at best only reach it on the backup path(s)
                // You weren't able to get it on the primary path, so reset the after failure hop count
                // For the hop count, keep the minimum hop count that was successful
                hopCountToContentAfter = 0;
                boolean backupHit = false;
                for(Path path : allPaths){
                    List<Node> reachableBackupNodes = pathMappingService.getReachableNodes(primary, chosenFailures);
                    int tempHopCount = 0;
                    boolean pathHit = false;
                    for(Node node : reachableBackupNodes){
                        if(checkIfHit(node, cachingLocations, src)){
                            pathHit = true;
                            break;
                        }
                        tempHopCount++;
                    }
                    if(pathHit){
                        backupHit = true;
                        if(hopCountToContentAfter > tempHopCount){
                            hopCountToContentAfter = tempHopCount;
                        }
                    }
                }

                // Use the backup hit and hop counts to determine if content is reachable after failure!

            }
        }
    }

    private boolean checkIfHit(Node node, Set<Node> cachingLocations, Node src) {
        return cachingLocations.contains(node) && !node.equals(src);
    }
}
