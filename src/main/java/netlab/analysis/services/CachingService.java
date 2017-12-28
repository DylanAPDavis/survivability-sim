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
            branchingPointMap.get(dst).addAll(pathMappingService.findOverlap(paths));
        }
        // Now we have the overlapping nodes per destination
        // Go back through the pair, and assign caching nodes
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Node dst = pair.getDst();
            Set<String> branchingPointIds = branchingPointMap.get(dst);
            Set<Node> cachingPoints = cacheMap.get(pair);
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
        }
    }

    private void cacheOutsideFailures(Map<SourceDestPair, Set<Node>> cacheMap, Map<SourceDestPair, Path> primaryPathMap, Set<Failure> failures) {
        // The goal is to avoid caching at locations that either can:
        // (a) Fail.
        // (b) Become disconnected from a source due to another failure.
        // For each path, determine which nodes remain reachable.
        // Cache at the closest one
        Set<Node> failureNodes = new HashSet<>();
        Set<Link> failureLinks = new HashSet<>();
        for(Failure failure : failures){
            if(failure.getNode() != null){
                failureNodes.add(failure.getNode());
            }
            else{
                failureLinks.add(failure.getLink());
            }
        }
        for(SourceDestPair pair : primaryPathMap.keySet()){
            Set<Node> cachingNodes = new HashSet<>();
            Path primary = primaryPathMap.get(pair);
            List<Node> reachableNodes = getReachableNodes(primary, failureNodes, failureLinks);
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

    private List<Node> getReachableNodes(Path path, Set<Node> failureNodes, Set<Link> failureLinks) {
        // Get all the links that do not fail and are not attached to failing nodes
        List<Link> pathLinks = path.getLinks().stream()
                .filter(l -> !failureLinks.contains(l) && !failureNodes.contains(l.getOrigin()) && !failureNodes.contains(l.getTarget()))
                .collect(Collectors.toList());
        // If the first link does not start at the src, or there are no links, then there are no reachable nodes
        if(pathLinks.isEmpty() || pathLinks.get(0).getOrigin() != path.getNodes().get(0)){
            return new ArrayList<>();
        }
        List<Node> reachableNodes = new ArrayList<>();
        Node prevNode = pathLinks.get(0).getOrigin();
        for(Link link : pathLinks){
            // If the origin isn't the target of the previous link, then there was a removed link somewhere in the path
            Node origin = link.getOrigin();
            if(origin != prevNode){
                break;
            }
            // Store the target, and designate it as the new prevNode
            Node target = link.getTarget();
            reachableNodes.add(target);
            prevNode = target;
        }
        return reachableNodes;
    }

    public void evaluateContentAccessibility(List<CachingResult> cachingResults,
                                             Map<SourceDestPair, Map<String, Path>> chosenPaths,
                                             Set<Failure> failureSet) {
        // Calculate the following metrics:
        //Content Reachability: The percentage of sources that can still reach all of their desired content.
        double reachability = 0.0;
        // Average Content Accessibility: The average percentage of content that can still be accessed per source.
        // For example, if a source wants to access content from three destinations, and can only access content from two
        // of them (either from the destination itself, or from a cached location), then it has an accessibility percentage of 66%.
        double avgAccessibility = 0.0;
        // Average Hop Count to Content: The average hop count that will be traversed after failure to access content, per source.
        double avgHopCountToContent = 0.0;
    }
}
