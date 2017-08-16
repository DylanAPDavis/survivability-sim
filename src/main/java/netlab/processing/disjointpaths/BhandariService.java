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
                                                 Boolean nodesCanFail, Set<Failure> failures, Boolean defaultBehavior)
    {
        if(numC == 0)
            return new ArrayList<>();

        // Bhandari's algorithm
        return computePaths(topo, source, dest, numC, numFA, nodesCanFail, failures, defaultBehavior);
    }

    private List<List<Link>> computePaths(Topology topo, Node source, Node dest, Integer numC, Integer numFA,
                                          Boolean nodesCanFail, Set<Failure> failures, Boolean defaultBehavior){

        // Modify topology, source and dest if necessary to find node-disjoint paths
        Topology modifiedTopo = nodesCanFail ?
                makeNodeFailTopo(topo) : new Topology(topo.getId(), new HashSet<>(topo.getNodes()), new HashSet<>(topo.getLinks()));
        Node src = nodesCanFail ? Node.builder().id(source.getId() + "-incoming").build() : source;
        Node dst = nodesCanFail ? Node.builder().id(dest.getId() + "-outgoing").build() : dest;


        // Find the first shortest path
        List<Link> shortestPath = bellmanFordService.shortestPath(modifiedTopo, src, dst);
        if(shortestPath.isEmpty()){
            log.info("No shortest path from " + src.getId() + " to " + dst.getId() + " found");
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


        // Convert failures to a set of links
        // Also adds an inverse link for each failure to enable consideration of bidirectional failures
        Set<Link> failureLinks = convertFailuresToLinks(failures);
        Set<Link> alreadyConsideredFailureLinks = new HashSet<>();
        for(Integer pIndex = 1; pIndex < k; pIndex++){

            // Get the previous shortest path
            List<Link> prevPath = tempPaths.get(pIndex-1);

            // Reverse and give negative weight to edges in shortest path
            boolean pathCanFail = false;
            for(Link pathEdge : prevPath){
                // If this link (or internal link) is in the set of failures, reverse it and give it negative weight
                // If default behavior flag is set and no failures are passed in, this will produce a basic link-disjoint solution
                if(failureLinks.contains(pathEdge) || defaultBehavior) {
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
            if(pathCanFail && k < numC + numFA){
                k++;
            }

            // Find the new shortest path
            List<Link> modShortestPath = bellmanFordService.shortestPath(modifiedTopo, src, dst);
            tempPaths.add(modShortestPath);
        }
        List<List<Link>> pathLinks = combine(shortestPath, tempPaths, reversedToOriginalMap, modifiedTopo, src, dst, k);
        return nodesCanFail ? convertToOriginalTopoLinks(pathLinks) : pathLinks;
    }

    private List<List<Link>> convertToOriginalTopoLinks(List<List<Link>> pathLinks) {
        List<List<Link>> modifiedPaths = new ArrayList<>();
        for(List<Link> path : pathLinks){
            List<Link> newPath = path.stream().filter(l -> !l.getId().contains("-internal")).map(l ->
                Link.builder()
                        .id(l.getId())
                        .origin(Node.builder().id(l.getOrigin().getId().replace("-outgoing", "")).build())
                        .target(Node.builder().id(l.getTarget().getId().replace("-incoming", "")).build())
                        .weight(l.getWeight())
                        .build()
            ).collect(Collectors.toList());
            modifiedPaths.add(newPath);
        }
        return modifiedPaths;
    }

    Topology makeNodeFailTopo(Topology topo) {
        // For each node n, create an "n-incoming" node and "n-outgoing" node, and connect them with an internal link
        // Connect all incoming edges to n to the new incoming ndoe, and all outgoing edges from n from the new outgoing node
        Set<Node> originalNodes = topo.getNodes();
        Set<Link> internalLinks = originalNodes.stream().map(this::buildInternalLink).collect(Collectors.toSet());
        Set<Node> modifiedNodes = new HashSet<>();
        for(Link internalLink : internalLinks){
            modifiedNodes.add(internalLink.getOrigin());
            modifiedNodes.add(internalLink.getTarget());
        }
        Set<Link> modifiedLinks = new HashSet<>(topo.getLinks());
        for(Link link : modifiedLinks){
            // Set origin to origin-outgoing
            Node originOutgoing = Node.builder().id(link.getOrigin().getId() + "-outgoing").build();
            link.setOrigin(originOutgoing);
            // Set target to target-incoming
            Node targetIncoming = Node.builder().id(link.getTarget().getId() + "-incoming").build();
            link.setTarget(targetIncoming);
        }
        modifiedLinks.addAll(internalLinks);
        return new Topology(topo.getId() + "-modified", modifiedNodes, modifiedLinks);
    }

    private Set<Link> convertFailuresToLinks(Set<Failure> failures) {
        Set<Link> failureLinks = new HashSet<>();
        for(Failure failure : failures){
            Link link = failure.getLink();
            Node node = failure.getNode();
            if(link != null){
                failureLinks.add(link);
                // Add inverse link
                Link inverse = new Link(link.getTarget(), link.getOrigin(), link.getWeight());
                failureLinks.add(inverse);
            }
            if(node != null){
                Node failNode = failure.getNode();
                Link internalLink = buildInternalLink(failNode);
                failureLinks.add(internalLink);
                // Add inverse link
                Link inverse = Link.builder()
                        .id(internalLink.getId())
                        .origin(internalLink.getTarget())
                        .target(internalLink.getOrigin())
                        .weight(internalLink.getWeight())
                        .build();
                failureLinks.add(inverse);
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
        /*if(paths.size() < k){
            paths = augmentPaths(paths, k);
        }*/
        return paths;
    }

    private List<List<Link>> augmentPaths(List<List<Link>> paths, int k) {
        if(k < paths.size()){
            return paths;
        }
        int minIndex = 0;
        long minWeight = Long.MAX_VALUE;
        for(int index = 0; index < paths.size(); index++){
            long totalWeight = paths.get(index).stream().map(Link::getWeight).reduce(0L, (w1, w2) -> w1 + w2);
            if(totalWeight < minWeight){
                minWeight = totalWeight;
                minIndex = index;
            }
        }
        // Duplicate the minimum weight path until there are k paths
        int numToDuplicate = k - paths.size();
        for(int i = 0; i < numToDuplicate; i++){
            paths.add(new ArrayList<>(paths.get(minIndex)));
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
