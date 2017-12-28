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
            Map<SourceDestPair, Set<Node>> cacheMap = cachingResult.getCachingMap();
            switch(cachingResult.getType()){
                case None:
                    cacheAtDest(cacheMap, primaryPathMap);
                    break;
                case EntirePath:
                    cacheAlongPath(cacheMap, primaryPathMap);
                    break;
                case SourceAdjacent:
                    cacheNextToSource(cacheMap, primaryPathMap);
                    break;
                case FailureAware:
                    cacheOutsideFailures(cacheMap, primaryPathMap, failures);
                    break;
                case BranchingPoint:
                    cacheAtBranchingPoints(cacheMap, primaryPathMap);
            }
        }
    }

    private void cacheAtBranchingPoints(Map<SourceDestPair, Set<Node>> cacheMap, Map<SourceDestPair, Path> primaryPathMap) {
        // The goal is to find any points that are shared by multiple paths going to the same destination,
        // and caching there.
        // For each path that does not have one of these points, just cache next to the source.
        Map<Node, Set<Path>> pathsToDest = new HashMap<>();
        Map<Node, Set<String>> branchingPointMap = new HashMap<>();
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Node dst = pair.getDst();
            branchingPointMap.putIfAbsent(dst, new HashSet<>());
            pathsToDest.putIfAbsent(dst, new HashSet<>());
            if(primaryPathMap.get(pair) != null) {
                pathsToDest.get(dst).add(primaryPathMap.get(pair));
            }
        }
        // We now have all paths that go to each dest
        // With these paths, we find any overlapping nodes, and put them in the branching points map
        for(Node dst : branchingPointMap.keySet()){
            Set<Path> paths = pathsToDest.get(dst);
            Set<String> overlap = pathMappingService.findOverlap(paths);
            overlap.remove(dst.getId());
            branchingPointMap.get(dst).addAll(overlap);
        }
        // Now we have the overlapping nodes per destination
        // Go back through the pair, and assign caching nodes
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Node dst = pair.getDst();
            Set<String> branchingPointIds = branchingPointMap.get(dst);
            Set<Node> cachingPoints = new HashSet<>();
            if(primaryPathMap.get(pair) != null){
                Path primary = primaryPathMap.get(pair);
                // Cache at a branching point if possible
                boolean atleastOneCache = false;
                for (Node node : primary.getNodes()) {
                    if (node != pair.getSrc() && branchingPointIds.contains(node.getId())) {
                        cachingPoints.add(node);
                        atleastOneCache = true;
                    }
                }
                // Cache next to the source otherwise
                if (!atleastOneCache) {
                    cachingPoints.add(primary.getNodes().get(1));
                }
            }
            cacheMap.put(pair, cachingPoints);
        }
    }

    private void cacheOutsideFailures(Map<SourceDestPair, Set<Node>> cacheMap, Map<SourceDestPair, Path> primaryPathMap,
                                      Set<Failure> failures) {
        // The goal is to avoid caching at locations that either can:
        // (a) Fail.
        // (b) Become disconnected from a source due to another failure.
        // For each path, determine which nodes remain reachable.
        // Cache at the closest one
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Set<Node> cachingNodes = new HashSet<>();
            Path primary = primaryPathMap.get(pair);
            List<Node> reachableNodes = pathMappingService.getReachableNodes(primary, failures);
            // Cache at the first reachable node along the path
            if(!reachableNodes.isEmpty()){
                cachingNodes.add(reachableNodes.get(0));
            }
            // If there are none, cache next to the source
            else{
                if(primary != null) {
                    cachingNodes.add(primary.getNodes().get(1));
                }
            }
            cachingNodes.remove(pair.getSrc());
            cacheMap.put(pair, cachingNodes);
        }
    }

    private void cacheAtDest(Map<SourceDestPair, Set<Node>> cacheMap,  Map<SourceDestPair, Path> primaryPathMap){
        // Just cache at the destination
        for(SourceDestPair pair : primaryPathMap.keySet()){
            if(primaryPathMap.get(pair) != null) {
                cacheMap.put(pair, new HashSet<>());
                cacheMap.get(pair).add(pair.getDst());
            }
        }
    }

    private void cacheAlongPath(Map<SourceDestPair, Set<Node>> cacheMap,  Map<SourceDestPair, Path> primaryPathMap){
        // Cache at every node along the path (excluding the source)
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Set<Node> cachingNodes = new HashSet<>();
            Path primary = primaryPathMap.get(pair);
            if(primary != null) {
                cachingNodes.addAll(primary.getNodes());
                cachingNodes.remove(pair.getSrc());
                cacheMap.put(pair, cachingNodes);
            }
        }
    }

    private void cacheNextToSource(Map<SourceDestPair, Set<Node>> cacheMap, Map<SourceDestPair, Path> primaryPathMap) {
        // Cache next to every source.
        // Cache at the first non-source node on every path
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Set<Node> cachingNodes = new HashSet<>();
            Path primary = primaryPathMap.get(pair);
            if(primary != null) {
                // Every path has at least two nodes
                cachingNodes.add(primary.getNodes().get(1));
                cacheMap.put(pair, cachingNodes);
            }
        }
    }


    public void evaluateContentAccessibility(List<CachingResult> cachingResults,
                                             Map<SourceDestPair, Map<String, Path>> chosenPaths,
                                             Set<Failure> failureSet, Integer useMinD) {

        Map<SourceDestPair, Path> primaryPathMap = pathMappingService.buildPrimaryPathMap(chosenPaths);
        Map<SourceDestPair, List<Node>> reachableNodeMap = primaryPathMap.keySet().stream()
                .filter(pair -> primaryPathMap.get(pair) != null)
                .collect(Collectors.toMap(pair -> pair,
                        pair -> pathMappingService.getReachableNodes(primaryPathMap.get(pair), failureSet)));
        for(CachingResult cachingResult : cachingResults){

            Map<Node, Integer> numContentReachablePerSource = new HashMap<>();
            Map<SourceDestPair, Set<Node>> cacheMap = cachingResult.getCachingMap();
            int totalHopCount = 0;
            for(SourceDestPair pair : primaryPathMap.keySet()){
                Node src = pair.getSrc();
                List<Node> reachableNodes = reachableNodeMap.get(pair);
                Set<Node> cachingLocations = cacheMap.get(pair);
                int hopCount = 0;
                boolean hit = false;
                for(Node node : reachableNodes){
                    hopCount++;
                    if(cachingLocations.contains(node)){
                        // Cache hit
                        hit = true;
                        break;
                    }
                }
                if(!hit){
                    hopCount = 0;
                }
                // Add to hop count
                totalHopCount+= hopCount;
                // If there was a hit, increase the num content reachable per source
                numContentReachablePerSource.putIfAbsent(src, 0);
                if(hit) {
                    numContentReachablePerSource.put(src, numContentReachablePerSource.get(src) + 1);
                }
            }

            int totalThatCanReachAllContent = 0;
            int totalThatCanReachSomeContent = 0;
            double totalAccessibility = 0.0;
            for(Node src : numContentReachablePerSource.keySet()){
                int numContentReachable = numContentReachablePerSource.get(src);
                int totalContent = useMinD;
                double reachPercent;
                if(numContentReachable > totalContent){
                    reachPercent = 1.0;
                } else if(totalContent == 0){
                    reachPercent = numContentReachable > 0 ? 1.0 : 0.0;
                } else {
                    reachPercent = numContentReachable / totalContent;
                }

                if(reachPercent == 1.0){
                    totalThatCanReachAllContent++;
                }
                if(reachPercent > 0.0){
                    totalThatCanReachSomeContent++;
                }
                totalAccessibility += reachPercent;
            }
            // Calculate the following metrics:
            //Content Reachability: The percentage of sources that can still reach all of their desired content.
            double reachability = totalThatCanReachAllContent / numContentReachablePerSource.keySet().size();
            // Average Content Accessibility: The average percentage of content that can still be accessed per source.
            // For example, if a source wants to access content from three destinations, and can only access content from two
            // of them (either from the destination itself, or from a cached location), then it has an accessibility percentage of 66%.
            double avgAccessibility = totalAccessibility / numContentReachablePerSource.keySet().size();
            // Average Hop Count to Content: The average hop count that will be traversed after failure to access content, per source.
            double avgHopCountToContent = totalThatCanReachSomeContent > 0 ? totalHopCount / totalThatCanReachSomeContent : 0.0;
            cachingResult.setReachability(reachability);
            cachingResult.setAvgAccessibility(avgAccessibility);
            cachingResult.setAvgHopCountToContent(avgHopCountToContent);
        }
    }
}
