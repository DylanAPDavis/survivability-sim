package netlab.processing.disjointpaths;

import lombok.extern.slf4j.Slf4j;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.Topology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BhandariService {

    private BellmanFordService bellmanFordService;

    @Autowired
    public BhandariService(BellmanFordService bellmanFordService){
        this.bellmanFordService = bellmanFordService;
    }


    public List<List<Link>> computeDisjointPaths(Topology topo, Node source, Node dest, Integer k, Boolean partial, Set<Failure> failures)
    {
        if(k == 0)
            return new ArrayList<>();

        // Bhandari's algorithm
        return computePaths(topo, source, dest, k, partial, failures);
    }

    private List<List<Link>> computePaths(Topology topo, Node source, Node dest, Integer k, Boolean partial, Set<Failure> failures){

        // Find the first shortest path
        List<Link> shortestPath = bellmanFordService.shortestPath(topo, source, dest);
        if(shortestPath.isEmpty()){
            log.info("No shortest path from " + source.getId() + " to " + dest.getId() + " found");
            return new ArrayList<>();
        }
        //logPath(shortestPath, "First Shortest Path");

        List<List<Link>> paths = new ArrayList<>();
        paths.add(shortestPath);

        if(k == 1){
            return paths;
        }

        List<List<Link>> tempPaths = new ArrayList<>(paths);
        Map<Link, Link> reversedToOriginalMap = new HashMap<>();

        // Modify the topology
        Topology modifiedTopo = new Topology(topo.getId(), new HashSet<>(topo.getNodes()), new HashSet<>(topo.getLinks()));

        for(Integer pIndex = 1; pIndex < k; pIndex++){

            // Get the previous shortest path
            List<Link> prevPath = tempPaths.get(pIndex-1);

            // Reverse and give negative weight to edges in shortest path
            for(Link pathEdge : prevPath){
                Long reversedMetric = -1 * pathEdge.getWeight();
                Link reversedEdge = new Link(pathEdge.getTarget(), pathEdge.getOrigin(), reversedMetric);
                reversedToOriginalMap.put(reversedEdge, pathEdge);
                Set<Link> allBetweenPair = findAllBetweenPair(pathEdge.getOrigin(), pathEdge.getTarget(), modifiedTopo.getLinks());
                modifiedTopo.getLinks().removeAll(allBetweenPair);
                modifiedTopo.getLinks().add(reversedEdge);
            }

            // Find the new shortest path
            List<Link> modShortestPath = bellmanFordService.shortestPath(modifiedTopo, source, dest);
            //(modShortestPath, "SP on modified topology for (" + source.getUrn() + "," + dest.getUrn() + ")");
            tempPaths.add(modShortestPath);
        }
        return combine(shortestPath, tempPaths, reversedToOriginalMap, modifiedTopo, source, dest, k);

    }

    private Set<Link> findAllBetweenPair(Node src, Node dst, Set<Link> edges){
        return edges.stream()
                .filter(e -> e.getOrigin().equals(src) && e.getTarget().equals(dst) || e.getOrigin().equals(dst) && e.getTarget().equals(src))
                .collect(Collectors.toSet());
    }

    private List<List<Link>> combine(List<Link> shortestPath, List<List<Link>> tempPaths,
                                         Map<Link, Link> reversedToOriginalMap, Topology topo,
                                         Node source, Node dest, Integer k) {

        List<List<Link>> paths = new ArrayList<>();

        // Remove all inverse edges taken in new shortest path (along with mapped edge in original shortest path)
        boolean removedFromSP = false;
        Set<Link> combinedEdges = new HashSet<>();
        for (Integer index = 1; index < tempPaths.size(); index++) {
            List<Link> outputPath = new ArrayList<>(tempPaths.get(index));
            boolean removedFromThisPath = false;
            for (Link modSpEdge : tempPaths.get(index)) {
                if (reversedToOriginalMap.containsKey(modSpEdge)) {
                    Link origSpEdge = reversedToOriginalMap.get(modSpEdge);
                    shortestPath.remove(origSpEdge);
                    removedFromSP = true;
                    removedFromThisPath = true;
                }
            }
            if(removedFromThisPath){
                combinedEdges.addAll(outputPath);
            }
            else{
                paths.add(outputPath);
            }
        }
        if(removedFromSP) {
            combinedEdges.addAll(shortestPath);
        }
        else{
            paths.add(shortestPath);
        }

        if(paths.size() == k){
            return paths;
        }

        // Use the edges from paths that had to be split up
        topo.setLinks(combinedEdges);

        // With those edges, perform a DFS to build up k - |paths| paths


        return paths;
    }

    /*
    private List<List<Link>> getPaths(Topology topo, Node source, Node destination){
        Map<Node, Set<Link>> nodeLinkMap = topo.getNodeLinkMap();
        Set<Link> sourceLinks = nodeLinkMap.get(source);
        Set<Link> destLinks = nodeLinkMap.get(destination);

        Map<String, List<Link>> paths = new ArrayList<>();
        int pathNum = 0;
        for(Link link : sourceLinks){
            // Build a new path
            List<Link> newPath = new ArrayList<>();
            newPath.add(link);
            String pathId = String.valueOf(pathNum);
            paths.put(pathId, newPath);
            pathNum++;
            // Get the next node in the path
            Node target = link.getTarget();
            Set<Link> targetLinks = nodeLinkMap.get(target);
            while(!target.equals(destination)) {

            }

        }

    }
    */

    private void logPath(List<Link> path, String title){
        log.info(title + ": " + path.stream().map(e -> "(" + e.getOrigin().getId() + ", " + e.getTarget().getId() + ")").collect(Collectors.toList()).toString());
    }
}
