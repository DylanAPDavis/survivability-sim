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


    public List<List<Link>> computeDisjointPaths(Topology topo, Node source, Node dest, Integer numC, Integer numFA,
                                                 Boolean nodesCanFail, Set<Failure> failures)
    {
        if(numC == 0)
            return new ArrayList<>();

        // Bhandari's algorithm
        return computePaths(topo, source, dest, numC, numFA, nodesCanFail, failures);
    }

    private List<List<Link>> computePaths(Topology topo, Node source, Node dest, Integer numC, Integer numFA, Boolean nodesCanFail, Set<Failure> failures){

        // Find the first shortest path
        List<Link> shortestPath = bellmanFordService.shortestPath(topo, source, dest);
        if(shortestPath.isEmpty()){
            log.info("No shortest path from " + source.getId() + " to " + dest.getId() + " found");
            return new ArrayList<>();
        }

        List<List<Link>> paths = new ArrayList<>();
        paths.add(shortestPath);

        int k = numC;

        if(k == 1){
            return paths;
        }

        List<List<Link>> tempPaths = new ArrayList<>(paths);
        Map<Link, Link> reversedToOriginalMap = new HashMap<>();

        //TODO: Modify the topology (if necessary) by removing nodes and replacing with incoming/outgoing nodes
        Topology modifiedTopo = nodesCanFail ?
                makeNodeFailTopo(topo) : new Topology(topo.getId(), new HashSet<>(topo.getNodes()), new HashSet<>(topo.getLinks()));

        //TODO: Consider bidirectional failures
        // Convert failures to a set of links
        Set<Link> failureLinks = convertFailuresToLinks(failures);
        Set<Link> alreadyConsideredFailureLinks = new HashSet<>();
        for(Integer pIndex = 1; pIndex < k && k < numC + numFA; pIndex++){

            // Get the previous shortest path
            List<Link> prevPath = tempPaths.get(pIndex-1);

            // Reverse and give negative weight to edges in shortest path
            boolean pathCanFail = false;
            for(Link pathEdge : prevPath){
                // If this link (or internal link) is in the set of failures, reverse it and give it negative weight
                if(failureLinks.contains(pathEdge) || failureLinks.size() == 0) {
                    Long reversedMetric = -1 * pathEdge.getWeight();
                    Link reversedEdge = new Link(pathEdge.getTarget(), pathEdge.getOrigin(), reversedMetric);
                    reversedToOriginalMap.put(reversedEdge, pathEdge);
                    Set<Link> allBetweenPair = findAllBetweenPair(pathEdge.getOrigin(), pathEdge.getTarget(), modifiedTopo.getLinks());
                    modifiedTopo.getLinks().removeAll(allBetweenPair);
                    modifiedTopo.getLinks().add(reversedEdge);
                    // If this is a new failure link, increase the number of paths that you will have to get
                    // (Up until numC + numFA)
                    if(!alreadyConsideredFailureLinks.contains(pathEdge)){
                        pathCanFail = true;
                        alreadyConsideredFailureLinks.add(pathEdge);
                    }
                }
            }
            if(pathCanFail){
                k++;
            }

            // Find the new shortest path
            List<Link> modShortestPath = bellmanFordService.shortestPath(modifiedTopo, source, dest);
            tempPaths.add(modShortestPath);
        }
        return combine(shortestPath, tempPaths, reversedToOriginalMap, modifiedTopo, source, dest, k);

    }

    private Topology makeNodeFailTopo(Topology topo) {
        // For each node n, create an "n-incoming" node and "n-outgoing" node, and connect them with an internal link
        // Connect all incoming edges to n to the new incoming ndoe, and all outgoing edges from n from the new outgoing node
        Set<Node> originalNodes = topo.getNodes();
        Set<Link> internalLinks = originalNodes.stream().map(this::buildInternalLink).collect(Collectors.toSet());
        Set<Node> incomingNodes = new HashSet<>();
        Set<Node> outgoingNodes = new HashSet<>();
        for(Link internalLink : internalLinks){
            incomingNodes.add(internalLink.getOrigin());
            outgoingNodes.add(internalLink.getTarget());
        }
        Set<Link> modifiedLinks = new HashSet<>(topo.getLinks());
        for(Link link : modifiedLinks){

        }
    }

    private Set<Link> convertFailuresToLinks(Set<Failure> failures) {
        Set<Link> failureLinks = new HashSet<>();
        for(Failure failure : failures){
            if(failure.getLink() != null){
                failureLinks.add(failure.getLink());
            }
            if(failure.getNode() != null){
                Node failNode = failure.getNode();
                failureLinks.add(buildInternalLink(failNode));
            }
        }
        return failureLinks;
    }

    private Link buildInternalLink(Node node){
        Node incomingNode = Node.builder()
                .id(node.getId() + "-incoming")
                .build();
        Node outgoingNode = Node.builder()
                .id(node.getId() + "-outgoing")
                .build();
        return Link.builder()
                .id(node.getId() + "-internal")
                .origin(incomingNode)
                .target(outgoingNode)
                .weight(0L)
                .build();
    }


    private Set<Link> findAllBetweenPair(Node src, Node dst, Set<Link> edges){
        return edges.stream()
                .filter(e -> e.getOrigin().equals(src) && e.getTarget().equals(dst) || e.getOrigin().equals(dst) && e.getTarget().equals(src))
                .collect(Collectors.toSet());
    }

    private List<List<Link>> combine(List<Link> shortestPath, List<List<Link>> tempPaths,
                                         Map<Link, Link> reversedToOriginalMap, Topology topo,
                                         Node source, Node dest, Integer k) {
        // Remove all inverse edges taken in new shortest path (along with mapped edge in original shortest path)
        Set<Link> combinedEdges = new HashSet<>();
        for (Integer index = 1; index < tempPaths.size(); index++) {
            List<Link> outputPath = new ArrayList<>(tempPaths.get(index));
            for (Link modSpEdge : tempPaths.get(index)) {
                if (reversedToOriginalMap.containsKey(modSpEdge)) {
                    Link origSpEdge = reversedToOriginalMap.get(modSpEdge);
                    shortestPath.remove(origSpEdge);
                    outputPath.remove(modSpEdge);
                }
            }
            combinedEdges.addAll(outputPath);
        }

        combinedEdges.addAll(shortestPath);

        // Use the edges from paths that had to be split up
        topo.setLinks(combinedEdges);

        return createPaths(topo, source, dest, k);
    }

    private List<List<Link>> createPaths(Topology topo, Node source, Node dest, Integer k) {
        Map<Node, List<Link>> nodeLinkMap = topo.getNodeOrderedLinkMap();

        // Track all links used while performing a depth-first search
        List<Link> linksAlongPaths = new ArrayList<>();
        depthFirstSearch(nodeLinkMap, source, dest, linksAlongPaths);

        List<List<Link>> paths = new ArrayList<>();
        int pathIndex = 0;
        for(Link link : linksAlongPaths){
            if(link.getOrigin().equals(source)){
                paths.add(pathIndex, new ArrayList<>());
            }
            paths.get(pathIndex).add(link);
            if(link.getTarget().equals(dest)){
                pathIndex++;
            }
        }
        return paths;
    }

    private void depthFirstSearch(Map<Node, List<Link>> nodeLinkMap, Node v, Node dest, List<Link> linksAlongPaths){
        //discovered.add(v);
        for(Link link : nodeLinkMap.get(v)){
            Node target = link.getTarget();
            linksAlongPaths.add(link);
            if(!target.equals(dest)){
                depthFirstSearch(nodeLinkMap, target, dest, linksAlongPaths);
            }
        }
    }


    private void logPath(List<Link> path, String title){
        log.info(title + ": " + path.stream().map(e -> "(" + e.getOrigin().getId() + ", " + e.getTarget().getId() + ")").collect(Collectors.toList()).toString());
    }
}
