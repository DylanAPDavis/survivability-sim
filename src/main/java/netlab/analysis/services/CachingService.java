package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.CachingResult;
import netlab.processing.pathmapping.PathMappingService;
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
        Map<Node, Set<Path>> pathsPerSrc = pathMappingService.getPathsPerSrc(chosenPathsMap);
        Map<Node, Path> primaryPathPerSrc = pathMappingService.getPrimaryPathPerSrc(pathsPerSrc);
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
            // Remove all source and destination caches
            cachingLocations.removeAll(destinations);
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
            List<Node> primaryNodes = primary.getNodes().stream().collect(Collectors.toList());

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
        Set<Node> failureNodes = failures.stream().filter(f -> f.getNode() != null).map(Failure::getNode).collect(Collectors.toSet());
        for(Node src : primaryPathMap.keySet()){
            Set<Node> cachingNodes = new HashSet<>();
            Path primary = primaryPathMap.get(src);
            List<Node> primaryNodes = primary.getNodes();
            List<Node> nonFailureNodes = primaryNodes.stream().filter(n -> !failureNodes.contains(n)).collect(Collectors.toList());
            //List<Node> reachableNodes = pathMappingService.getReachableNodes(primary, failures);
            // Cache at all non-failure nodes
            if(!nonFailureNodes.isEmpty()){
                cachingNodes.addAll(nonFailureNodes);
            }
            // Remove the src and the dest
            cachingNodes.remove(primaryNodes.get(primaryNodes.size()-1));
            cachingNodes.remove(src);
            cachingPoints.addAll(cachingNodes);
        }
        return cachingPoints;
    }

    private Set<Node> cacheAtDest(Map<Node, Path> primaryPathMap){
        Set<Node> cachingPoints = new HashSet<>();
        /*for(Node src : primaryPathMap.keySet()){
            List<Node> primaryNodes = primaryPathMap.get(src).getNodes();
            cachingPoints.add(primaryNodes.get(primaryNodes.size()-1));
        }*/
        return cachingPoints;
    }

    private Set<Node> cacheAlongPath(Map<Node, Path> primaryPathMap){
        // Cache at every node along the path (excluding the source and destination)
        Set<Node> cachingPoints = new HashSet<>();
        for(Node src : primaryPathMap.keySet()){
            Path primary = primaryPathMap.get(src);
            if(primary != null) {
                List<Node> primaryNodes = primary.getNodes();
                cachingPoints.addAll(primaryNodes);
                cachingPoints.remove(src);
                cachingPoints.remove(primaryNodes.get(primaryNodes.size()-1));
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
                if(primaryNodes.get(1) != src) {
                    cachingPoints.add(primaryNodes.get(1));
                }
                //cachingPoints.add(primaryNodes.get(primaryNodes.size()-1));
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
                //cachingPoints.add(pathNodes.get(pathNodes.size()-1));
            }
        }
        return cachingPoints;
    }


    public void evaluateContentAccessibility(List<CachingResult> cachingResults,
                                             Map<SourceDestPair, Map<String, Path>> chosenPaths,
                                             Collection<Failure> chosenFailures, Set<Node> dests, boolean additive) {
        Map<Node, Set<Path>> pathsPerSrc = pathMappingService.getPathsPerSrc(chosenPaths);
        Map<Node, Path> primaryPathPerSrc = pathMappingService.getPrimaryPathPerSrc(pathsPerSrc);
        for (CachingResult cachingResult : cachingResults) {
            evaluateCachingResult(cachingResult, pathsPerSrc, primaryPathPerSrc, chosenFailures, dests, additive);
        }

    }


    private void evaluateCachingResult(CachingResult cachingResult, Map<Node, Set<Path>> pathsPerSrc,
                                      Map<Node, Path> primaryPathPerSrc, Collection<Failure> chosenFailures, Set<Node> dests,
                                       boolean additive){
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
                if(checkIfHit(node, cachingLocations, src, dests)){
                    break;
                }
                hopCountToContentBefore++;
            }

            // Next, see if we can reach the content on the primary path
            List<Node> reachableNodes = pathMappingService.getReachableNodes(primary, chosenFailures);
            boolean primaryHit = false;
            for(Node node : reachableNodes){
                primaryHit = checkIfHit(node, cachingLocations, src, dests);
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
                    if(checkIfHit(node, cachingLocations, src, dests)){
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

        Double divisor = pathsPerSrc.keySet().size() > 0 ? pathsPerSrc.keySet().size() : 1.0;
        Double avgReachOnPrimary = 1.0 * reachOnPrimaryAfter.size() / divisor;
        Double avgReachOnBackup = 1.0 * reachOnBackupAfter.size() / divisor;
        Double avgReachOnlyBackup = 1.0 * onlyReachOnBackupAfter.size() / divisor;

        if(!additive) {
            cachingResult.setAvgHopCountBefore(averageHopBefore);
            cachingResult.setAvgHopCountAfter(averageHopAfter);
            cachingResult.setReachOnPrimary(avgReachOnPrimary);
            cachingResult.setReachOnBackup(avgReachOnBackup);
            cachingResult.setReachOnlyBackup(avgReachOnlyBackup);
        } else{
            cachingResult.setAvgHopCountBefore(cachingResult.getAvgHopCountBefore() + averageHopBefore);
            cachingResult.setAvgHopCountAfter(cachingResult.getAvgHopCountAfter() + averageHopAfter);
            cachingResult.setReachOnPrimary(cachingResult.getReachOnPrimary() + avgReachOnPrimary);
            cachingResult.setReachOnBackup(cachingResult.getReachOnBackup() + avgReachOnBackup);
            cachingResult.setReachOnlyBackup(cachingResult.getReachOnlyBackup() + avgReachOnlyBackup);
        }
    }

    private Double calculateAverage(Map<Node, Integer> valueMap, Set<Node> validKeys) {
        double sum = 0.0;
        for(Node key : validKeys){
            Integer value = valueMap.get(key);
            sum += value;
        }
        return validKeys.size() > 0 ? sum / validKeys.size() : 0.0;
    }

    private boolean checkIfHit(Node node, Set<Node> cachingLocations, Node src, Set<Node> dests) {
        return !node.equals(src) && (cachingLocations.contains(node) || dests.contains(node));
    }

    public void averageContentAccessibility(List<CachingResult> cachingResults, int size) {
        int divisor = size > 0 ? size : 1;
        for(CachingResult cachingResult : cachingResults){
            cachingResult.setAvgHopCountBefore(cachingResult.getAvgHopCountBefore() / divisor);
            cachingResult.setAvgHopCountAfter(cachingResult.getAvgHopCountAfter() / divisor);
            cachingResult.setReachOnPrimary(cachingResult.getReachOnPrimary() / divisor);
            cachingResult.setReachOnBackup(cachingResult.getReachOnBackup() / divisor);
            cachingResult.setReachOnlyBackup(cachingResult.getReachOnlyBackup() / divisor);
        }
    }
}
