package netlab.processing.cycles;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.PathMappingService;
import netlab.processing.shortestPaths.MinimumCostPathService;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/*
Minimum Cost Collapsed Ring (MC-CR)
Multicast Protection
From "Protection of Multicast Sessions in WDM Mesh Optical Networks" by Tanvir Rahman, Georgios Ellinas - 2005
 */
@Service
@Slf4j
public class CollapsedRingService {

    private MinimumCostPathService minimumCostPathService;
    private PathMappingService pathMappingService;
    private TopologyAdjustmentService topologyService;

    @Autowired
    public CollapsedRingService(MinimumCostPathService minimumCostPathService, PathMappingService pathMappingService,
                                TopologyAdjustmentService topologyService) {
        this.minimumCostPathService = minimumCostPathService;
        this.pathMappingService = pathMappingService;
        this.topologyService = topologyService;
    }

    public Details solve(Request request, Topology topo){
        long startTime = System.nanoTime();
        Details details = findPaths(request, topo);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setRunningTimeSeconds(duration);
        return details;
    }

    private Details findPaths(Request request, Topology topo) {
        Details details = request.getDetails();

        Set<Node> sources = details.getSources();
        Node src = sources.iterator().next();
        Set<Node> dests = details.getDestinations();

        Map<SourceDestPair, Map<String, Path>> chosenPathsMap = new HashMap<>();

        Map<SourceDestPair, Long> minimumCostPathMap = topo.getMinimumPathCostMap();


        List<Node> sortedDests = dests.stream()
                .sorted(Comparator.comparing(d -> minimumCostPathMap.get(new SourceDestPair(src, d))))
                .collect(Collectors.toList());

        List<Path> forwardAndReverse = findCollapsedRing(src, sortedDests, topo);

        for(Path path : forwardAndReverse){
            Node dst = path.getNodes().get(path.getNodes().size()-1);
            SourceDestPair pair = new SourceDestPair(src, dst);
            chosenPathsMap.putIfAbsent(pair, new HashMap<>());
            chosenPathsMap.get(pair).put(String.valueOf(chosenPathsMap.get(pair).size()+1), path);
        }

        // Path filtering
        chosenPathsMap = pathMappingService.filterMap(chosenPathsMap, details);
        details.setChosenPaths(chosenPathsMap);
        details.setIsFeasible(true);

        return details;
    }

    public List<Path> findCollapsedRing(Node src, List<Node> destinations, Topology topo){
        destinations.remove(src);
        // Find a path from src to each destination in order.
        // If a destination ends up as an intermediate node, remove it from the list
        Set<Node> reachedDestinations = new HashSet<>();
        Path firstSrcPath = null;
        List<Path> destToDestPaths = new ArrayList<>();
        Node pathOrigin = src;
        Node lastDest = null;
        for(Node dest : destinations){
            // If the dest is not the source, and it hasn't been reached already
            if(dest != src && !reachedDestinations.contains(dest)){
                SourceDestPair pair = SourceDestPair.builder().src(pathOrigin).dst(dest).build();
                // Get the Shortest Path
                Path sp = minimumCostPathService.findShortestPath(pair, topo);
                // Add all reached destinations to set
                for(Node pathNode : sp.getNodes()){
                    if(destinations.contains(pathNode)){
                        reachedDestinations.add(pathNode);
                    }
                }
                if(firstSrcPath == null){
                    firstSrcPath = sp;
                }
                else{
                    destToDestPaths.add(sp);
                }
                pathOrigin = dest;
                lastDest = dest;
            }
        }

        // Now we have src -> d1 -> d2 -> ... -> dn paths
        // Need to find a src -> dn path, then store all of the reverse paths
        Path secondSrcPath = null;
        List<Path> reverseDestToDestPaths = new ArrayList<>(destToDestPaths);
        Collections.reverse(reverseDestToDestPaths);
        if(lastDest != null) {
            // Remove the links used in the primary paths
            List<Link> linksToRemove = firstSrcPath.getLinks();
            linksToRemove.addAll(destToDestPaths.stream().map(Path::getLinks).flatMap(Collection::stream).collect(Collectors.toList()));
            Topology primaryRemovedTopo = topologyService.removeLinksFromTopology(topo, new HashSet<>(linksToRemove));
            // Find a path to the last connected dest
            SourceDestPair srcLastDstPair = SourceDestPair.builder().src(src).dst(lastDest).build();
            secondSrcPath = minimumCostPathService.findShortestPath(srcLastDstPair, primaryRemovedTopo);
            // Reverse the primary paths
            reverseDestToDestPaths = reverseDestToDestPaths.stream()
                    .map(Path::reverse)
                    .collect(Collectors.toList());
        }

        // Now, combine the forward paths to create the src -> dst paths
        List<Path> finalPaths = combinePaths(firstSrcPath, destToDestPaths);
        finalPaths.addAll(combinePaths(secondSrcPath, reverseDestToDestPaths));
        return finalPaths;
    }

    public List<Path> combinePaths(Path firstPath, List<Path> followupPaths){
        List<Path> combinedPathList = new ArrayList<>();
        combinedPathList.add(firstPath);
        for(int i = 0; i < followupPaths.size(); i++){
            Path newPath = followupPaths.get(i);
            Path prePath = combinedPathList.get(i);
            Path combinedPath = prePath.combinePaths(newPath);
            combinedPathList.add(combinedPath);
        }
        return combinedPathList;
    }

    public void augmentPathListWithPathsToSrc( List<Path> cyclePaths) {
        // Create a path to the src using the existing paths
        // Find the two shortest link-disjoint paths that start at the src
        List<Path> pathsToReverse = new ArrayList<>();
        cyclePaths.sort(Comparator.comparing(Path::getTotalWeight));
        for(Path path : cyclePaths){
            if(pathsToReverse.isEmpty()){
                pathsToReverse.add(path);
            }
            else{
                if(pathsToReverse.get(0).isDisjoint(path, false)){
                    pathsToReverse.add(path);
                    break;
                }
            }
        }
        // Reverse those paths and add them to the list
        for(Path path : pathsToReverse){
            cyclePaths.add(path.reverse());
        }
    }
}
