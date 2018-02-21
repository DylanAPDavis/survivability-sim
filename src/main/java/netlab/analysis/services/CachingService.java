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
        Map<Node, Set<Path>> pathsPerSrc = getPathsPerSrc(chosenPathsMap);
        Map<Node, Path> primaryPathPerSrc = getPrimaryPathPerSrc(pathsPerSrc);
        Set<Node> destinations = chosenPathsMap.keySet().stream().map(SourceDestPair::getDst).collect(Collectors.toSet());
        for(CachingResult cachingResult : cachingResults){
            Set<Node> cachingLocations = new HashSet<>();
            switch(cachingResult.getType()){
                case None:
                    cachingLocations = cacheAtDest(primaryPathPerSrc);
                    break;
                case EntirePath:
                    cachingLocations = cacheAlongPath(primaryPathPerSrc);
                    break;
                case SourceAdjacent:
                    cachingLocations = cacheNextToSource(primaryPathPerSrc);
                    break;
                case FailureAware:
                    cachingLocations = cacheOutsideFailures(primaryPathPerSrc, failures);
                    break;
                case BranchingPoint:
                    cachingLocations = cacheAtBranchingPoints(primaryPathPerSrc, pathsPerSrc);
                    break;
                case LeaveCopyDown:
                    cachingLocations = cacheLeaveCopyDown(primaryPathPerSrc);
            }
            // Add destination caches
            cachingLocations.addAll(destinations);
            cachingResult.setCachingCost(evaluateCost(cachingLocations));
            cachingResult.setCachingLocations(cachingLocations);
        }
    }


    private double evaluateCost(Set<Node> cachingLocations) {
        return 1.0 * cachingLocations.size();
    }

    private Set<Node> cacheAtBranchingPoints(Map<Node, Path> primaryPathMap, Map<Node, Set<Path>> otherPathsMap) {

        Set<Node> cachingPoints = new HashSet<>();
        Set<Path> allPaths = otherPathsMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        for(Node src : primaryPathMap.keySet()){
            Path primary = primaryPathMap.get(src);
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
            // Add the destination
            cachingPoints.add(primaryNodes.get(primaryNodes.size()-1));
            cachingPoints.remove(src);
            // Add the primary path back to the set of all paths so other pairs can check it
            allPaths.add(primary);
        }
        return cachingPoints;
    }

    private Set<Node> cacheOutsideFailures(Map<Node, Path> primaryPathMap,
                                           Set<Failure> failures) {
        // The goal is to avoid caching at locations that either can:
        // (a) Fail.
        // (b) Become disconnected from a source due to another failure.
        // For each path, determine which nodes remain reachable.
        // Cache at the closest one
        Set<Node> cachingPoints = new HashSet<>();
        for(Node src : primaryPathMap.keySet()){
            Set<Node> cachingNodes = new HashSet<>();
            Path primary = primaryPathMap.get(src);
            List<Node> primaryNodes = primary.getNodes();
            List<Node> reachableNodes = pathMappingService.getReachableNodes(primary, failures);
            // Cache at the first reachable node along the path
            if(!reachableNodes.isEmpty()){
                cachingNodes.addAll(reachableNodes);
            }
            cachingNodes.add(primaryNodes.get(primaryNodes.size()-1));
            cachingNodes.remove(src);
            cachingPoints.addAll(cachingNodes);
        }
        return cachingPoints;
    }

    private Set<Node> cacheAtDest(Map<Node, Path> primaryPathMap){
        // Just cache at the destination
        Set<Node> cachingPoints = new HashSet<>();
        for(Node src : primaryPathMap.keySet()){
            List<Node> primaryNodes = primaryPathMap.get(src).getNodes();
            cachingPoints.add(primaryNodes.get(primaryNodes.size()-1));
        }
        return cachingPoints;
    }

    private Set<Node> cacheAlongPath(Map<Node, Path> primaryPathMap){
        // Cache at every node along the path (excluding the source)
        Set<Node> cachingPoints = new HashSet<>();
        for(Node src : primaryPathMap.keySet()){
            Path primary = primaryPathMap.get(src);
            if(primary != null) {
                List<Node> primaryNodes = primary.getNodes();
                cachingPoints.addAll(primaryNodes);
                cachingPoints.remove(src);
            }
        }
        return cachingPoints;
    }

    private Set<Node> cacheNextToSource(Map<Node, Path> primaryPathMap) {
        // Cache next to every source.
        // Cache at the first non-source node on every path
        Set<Node> cachingPoints = new HashSet<>();
        for(Node src : primaryPathMap.keySet()){
            Path primary = primaryPathMap.get(src);
            if(primary != null) {
                List<Node> primaryNodes = primary.getNodes();
                // Every path has at least two nodes
                cachingPoints.add(primaryNodes.get(1));
                cachingPoints.add(primaryNodes.get(primaryNodes.size()-1));
            }
        }
        return cachingPoints;
    }

    private Set<Node> cacheLeaveCopyDown(Map<Node, Path> primaryPathMap) {
        // Cache next to every destination
        Set<Node> cachingPoints = new HashSet<>();
        for(Node src : primaryPathMap.keySet()){
            Path primary = primaryPathMap.get(src);
            if(primary != null){
                List<Node> pathNodes = primary.getNodes();
                Node secondToLast = pathNodes.get(pathNodes.size()-2);
                // If the second to last is not the source (i.e. there is an intermediate node), then cache there
                if(!secondToLast.getId().equals(src.getId())) {
                    cachingPoints.add(secondToLast);
                }
                // And add the dest
                cachingPoints.add(pathNodes.get(pathNodes.size()-1));
            }
        }
        return cachingPoints;
    }


    public void evaluateContentAccessibility(List<CachingResult> cachingResults,
                                             Map<SourceDestPair, Map<String, Path>> chosenPaths,
                                             Collection<Failure> chosenFailures) {
        Map<Node, Set<Path>> pathsPerSrc = getPathsPerSrc(chosenPaths);
        Map<Node, Path> primaryPathPerSrc = getPrimaryPathPerSrc(pathsPerSrc);
        for (CachingResult cachingResult : cachingResults) {
            evaluateCachingResult(cachingResult, pathsPerSrc, primaryPathPerSrc, chosenFailures);
        }

    }

    private Map<Node,Path> getPrimaryPathPerSrc(Map<Node, Set<Path>> pathsPerSrc) {
        Map<Node, Path> primaryPathPerSrc = new HashMap<>();
        for(Node src : pathsPerSrc.keySet()){
            Set<Path> paths = pathsPerSrc.get(src);
            Path minPath = null;
            for(Path path : paths){
                if(minPath == null || minPath.getTotalWeight() > path.getTotalWeight()){
                    minPath = path;
                }
            }
            if(minPath != null){
                primaryPathPerSrc.put(src, minPath);
            }
        }
        return primaryPathPerSrc;
    }

    private Map<Node, Set<Path>> getPathsPerSrc(Map<SourceDestPair, Map<String, Path>> chosenPaths){
        Map<Node, Set<Path>> pathsPerSrc = new HashMap<>();
        // Go through all of the pairs, get all paths per source
        for (SourceDestPair pair : chosenPaths.keySet()) {
            Node src = pair.getSrc();
            Set<Path> paths = chosenPaths.get(pair).values().stream().collect(Collectors.toSet());
            if(paths.size() > 0) {
                // Store all paths for this pair
                pathsPerSrc.putIfAbsent(src, new HashSet<>());
                pathsPerSrc.get(src).addAll(paths);
            }
        }
        return pathsPerSrc;
    }

    private void evaluateCachingResult(CachingResult cachingResult, Map<Node, Set<Path>> pathsPerSrc,
                                      Map<Node, Path> primaryPathPerSrc, Collection<Failure> chosenFailures){
        Set<Node> cachingLocations = cachingResult.getCachingLocations();

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
            else {
                // If not, you can at best only reach it on the backup path(s)
                // You weren't able to get it on the primary path, so reset the after failure hop count
                // For the hop count, keep the minimum hop count that was successful
                hopCountToContentAfter = Integer.MAX_VALUE;
            }
            boolean backupHit = false;
            for(Path backupPath : allPaths){
                List<Node> reachableBackupNodes = pathMappingService.getReachableNodes(backupPath, chosenFailures);
                int tempHopCount = 0;
                boolean pathHit = false;
                for(Node node : reachableBackupNodes){
                    if(checkIfHit(node, cachingLocations, src)){
                        pathHit = true;
                        break;
                    }
                    tempHopCount++;
                }
                // If you've hit content on this path, you've hit content on at least one backup
                if(pathHit){
                    backupHit = true;
                    // If you get to this content more quickly than before, track that
                    if(hopCountToContentAfter > tempHopCount){
                        hopCountToContentAfter = tempHopCount;
                    }
                }
            }
            // Use the backup hit and hop counts to determine if content is reachable after failure!
            if(backupHit){
                reachOnBackupAfter.add(src);
                if(!primaryHit){
                    onlyReachOnBackupAfter.add(src);
                }
            }
            hopCountBefore.put(src, hopCountToContentBefore);
            hopCountAfter.put(src, hopCountToContentAfter);
        }

        Set<Node> srcsThatReachContentBefore = hopCountBefore.keySet().stream()
                .filter(h -> hopCountBefore.get(h) > 0 && hopCountBefore.get(h)  < Integer.MAX_VALUE)
                .collect(Collectors.toSet());
        Double averageHopBefore = calculateAverage(hopCountBefore, srcsThatReachContentBefore);
        Set<Node> srcsThatReachContentAfter = hopCountAfter.keySet().stream()
                .filter(h -> hopCountAfter.get(h) > 0 && hopCountAfter.get(h)  < Integer.MAX_VALUE)
                .collect(Collectors.toSet());
        Double averageHopAfter = calculateAverage(hopCountAfter, srcsThatReachContentAfter);

        Double avgReachOnPrimary = 1.0 * reachOnPrimaryAfter.size() / pathsPerSrc.keySet().size();
        Double avgReachOnBakcup = 1.0 * reachOnBackupAfter.size() / pathsPerSrc.keySet().size();
        Double avgReachOnlyBakcup = 1.0 * onlyReachOnBackupAfter.size() / pathsPerSrc.keySet().size();

        cachingResult.setAvgHopCountBefore(averageHopBefore);
        cachingResult.setAvgHopCountAfter(averageHopAfter);
        cachingResult.setReachOnPrimary(avgReachOnPrimary);
        cachingResult.setReachOnBackup(avgReachOnBakcup);
        cachingResult.setReachOnlyBackup(avgReachOnlyBakcup);
    }

    private Double calculateAverage(Map<Node, Integer> valueMap, Set<Node> validKeys) {
        double sum = 0.0;
        for(Node key : validKeys){
            Integer value = valueMap.get(key);
            sum += value;
        }
        return validKeys.size() > 0 ? sum / validKeys.size() : 0.0;
    }

    private boolean checkIfHit(Node node, Set<Node> cachingLocations, Node src) {
        return cachingLocations.contains(node) && !node.equals(src);
    }
}
