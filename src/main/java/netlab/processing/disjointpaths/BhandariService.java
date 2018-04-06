package netlab.processing.disjointpaths;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.PathMappingService;
import netlab.processing.shortestPaths.BellmanFordService;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.RoutingType;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.Connections;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BhandariService {

    private BellmanFordService bellmanFordService;
    private TopologyAdjustmentService topologyService;
    private PathMappingService pathMappingService;

    @Autowired
    public BhandariService(BellmanFordService bellmanFordService, TopologyAdjustmentService topologyService, PathMappingService pathMappingService){
        this.bellmanFordService = bellmanFordService;
        this.topologyService = topologyService;
        this.pathMappingService = pathMappingService;
    }

    public Details solve(Request request, Topology topo){
        Details details = request.getDetails();
        Map<SourceDestPair, Map<String, Path>> pathMap = new HashMap<>();

        // Get sorted pairs
        List<SourceDestPair> pairs = topologyService.sortPairsByPathCost(details.getPairs(), topo);

        FailureClass failureClass;
        Set<Failure> failures = request.getDetails().getFailures().getFailureSet();
        if(failures.stream().anyMatch(f -> f.getNode() != null)){
            failureClass = FailureClass.Both;
        } else{
            failureClass = FailureClass.Link;
        }
        long startTime = System.nanoTime();
        if(request.getRoutingType().equals(RoutingType.Unicast)){
            SourceDestPair pair = pairs.iterator().next();
            List<Path> paths = findPathSet(pair, topo, new HashMap<>(), new HashMap<>(), TrafficCombinationType.None,
                    failureClass);
            if(!paths.isEmpty()){
                Map<String, Path> idMap = new HashMap<>();
                int id = 1;
                for(Path path : paths) {
                    idMap.put(String.valueOf(id), path);
                    id++;
                }
                pathMap.put(pair, idMap);
                details.setIsFeasible(true);
            }
            else{
                details.setIsFeasible(false);
            }
            details.setChosenPaths(pathMap);
        }
        else{
            details = findPaths(details, pairs, topo, request.getTrafficCombinationType(),
                    failureClass);
        }
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setRunningTimeSeconds(duration);
        return details;
    }

    private Details findPaths(Details details, Collection<SourceDestPair> pairs, Topology topo,
                              TrafficCombinationType trafficCombinationType, FailureClass failureClass){

        Map<SourceDestPair, Map<String, Path>> pathMap = new HashMap<>();
        Map<Node, Set<Path>> srcPathsMap = new HashMap<>();
        Map<Node, Set<Path>> dstPathsMap = new HashMap<>();
        // Iterate through each pair
        // For each pair, find two paths between that pair by traversing the cycle
        for(SourceDestPair pair : pairs){
            pathMap.put(pair, new HashMap<>());
            List<Path> paths = findPathSet(pair, topo, srcPathsMap,dstPathsMap, trafficCombinationType, failureClass);
            int id = 1;
            for(Path path : paths){
                pathMap.get(pair).put(String.valueOf(id), path);
                id++;
            }
        }

        if(pairs.size() > 1) {
            pathMappingService.filterMap(pathMap, details);
        }
        details.setChosenPaths(pathMap);
        details.setIsFeasible(evaluatePathMap(pathMap, details));
        return details;
    }

    private Boolean evaluatePathMap(Map<SourceDestPair, Map<String, Path>> pathMap, Details details) {
        Connections connections = details.getConnections();
        int useMinS = connections.getUseMinS();
        int useMinD = connections.getUseMinD();
        int useMaxS = connections.getUseMaxS();
        int useMaxD = connections.getUseMaxD();
        Set<Node> usedSources = new HashSet<>();
        Set<Node> usedDestinations = new HashSet<>();
        Map<SourceDestPair, Integer> minPerPairMap = connections.getPairMinConnectionsMap();
        Map<SourceDestPair, Integer> numPerPairMap = minPerPairMap.keySet().stream().collect(Collectors.toMap(p -> p, p -> 0));
        boolean feasible = false;
        for (SourceDestPair pair : pathMap.keySet()) {
            Map<String, Path> paths = pathMap.get(pair);
            if(paths.size() > 0) {
                Node src = pair.getSrc();
                Node dst = pair.getDst();
                usedSources.add(src);
                usedDestinations.add(dst);
                numPerPairMap.put(pair, Integer.MAX_VALUE);
            }
        }
        if(usedSources.size() >= useMinS && usedSources.size() <= useMaxS && usedDestinations.size() >= useMinD && usedDestinations.size() <= useMaxD){
            if(numPerPairMap.keySet().stream().allMatch(p -> numPerPairMap.get(p) >= minPerPairMap.get(p))){
                feasible = true;
            }
        }
        return feasible;
    }


    private List<Path> findPathSet(SourceDestPair pair, Topology topo, Map<Node, Set<Path>> srcPathsMap,
                                   Map<Node, Set<Path>> dstPathsMap, TrafficCombinationType trafficType,
                                   FailureClass failureClass) {

        Node src = pair.getSrc();
        Node dst = pair.getDst();
        boolean nodesFail = failureClass.equals(FailureClass.Node) || failureClass.equals(FailureClass.Both);


        Topology modifiedTopo = topologyService.adjustWeightsUsingTrafficCombination(topo, trafficType, src, dst,
                srcPathsMap, dstPathsMap);

        List<List<Link>> linkLists = computeDisjointPaths(modifiedTopo, src, dst, 1, 1, nodesFail,
                new HashSet<>(), true);

        return pathMappingService.convertToPaths(linkLists, topo.getLinkIdMap());
    }

    public List<Path> computeDisjointPaths(Topology topo, Node src, Node dst, Integer k, Boolean nodesCanFail){
        if(k == 0)
            return new ArrayList<>();

        List<List<Link>> linkLists = computeDisjointPaths(topo, src, dst, 1, k-1, nodesCanFail, new HashSet<>(), true);
        return pathMappingService.convertToPaths(linkLists, topo.getLinkIdMap());
    }

    public List<Path> computeDisjointPathsFlex(Topology topo, Node source, Node dest, Integer numC, Integer nfe,
                                           Boolean nodesCanFail, Set<Failure> failures, Boolean defaultBehavior){
        List<List<Link>> disjointPaths = computeDisjointPaths(topo, source, dest, numC, nfe, nodesCanFail, failures, defaultBehavior);
        return pathMappingService.convertToPaths(disjointPaths, topo.getLinkIdMap());
    }

    public List<List<Link>> computeDisjointPaths(Topology topo, Node source, Node dest, Integer numC, Integer nfe,
                                                 Boolean nodesCanFail, Set<Failure> failures, Boolean defaultBehavior)
    {
        if(numC + nfe == 0)
            return new ArrayList<>();

        // Bhandari's algorithm
        return computePaths(topo, source, dest, numC, nfe, nodesCanFail, failures, defaultBehavior);
    }

    private List<List<Link>> computePaths(Topology topo, Node source, Node dest, Integer numPaths, Integer nfe,
                                          Boolean nodesCanFail, Set<Failure> failures, Boolean defaultBehavior){

        // Modify topology, source and dest if necessary to find node-disjoint paths
        Topology modifiedTopo = nodesCanFail ?
                makeNodeFailTopo(topo) : new Topology(topo.getId(), new HashSet<>(topo.getNodes()), new HashSet<>(topo.getLinks()));
        Node src = nodesCanFail ? new Node(source.getId() + "-incoming", source.getPoint()) : source;
        Node dst = nodesCanFail ? new Node(dest.getId() + "-outgoing", dest.getPoint()) : dest;


        // Find the first shortest path
        List<Link> shortestPath = bellmanFordService.shortestPath(modifiedTopo, src, dst);
        if(shortestPath.isEmpty()){
            log.info("No shortest path from " + src.getId() + " to " + dst.getId() + " found");
            return new ArrayList<>();
        }

        List<List<Link>> paths = new ArrayList<>();
        paths.add(shortestPath);

        int k = numPaths;

        if(k == 1 && nfe == 0){
            return convertToOriginalTopoLinks(paths);
        }

        List<List<Link>> tempPaths = new ArrayList<>(paths);
        Map<Link, Link> reversedToOriginalMap = new HashMap<>();


        // Convert failures to a set of links
        // Also adds an inverse link for each failure to enable consideration of bidirectional failures
        Set<Link> failureLinks = convertFailuresToLinks(failures, nodesCanFail);
        Set<String> failureIds = failureLinks.stream().map(Link::getId).collect(Collectors.toSet());
        Set<Link> alreadyConsideredFailureLinks = new HashSet<>();
        for(Integer pIndex = 0; pIndex < k; pIndex++){

            // Get the previous shortest path
            List<Link> prevPath = tempPaths.get(pIndex);

            // Reverse and give negative weight to edges in shortest path
            boolean pathCanFail = false;
            for(Link pathEdge : prevPath){
                // If this link (or internal link) is in the set of failures, reverse it and give it negative weight
                // If default behavior flag is set and no failures are passed in, this will produce a basic link-disjoint solution
                // Do not reverse the interal edge for src/dest
                boolean isInternalEndpoint = nodesCanFail && (checkIfInternalEndpoint(pathEdge, src, dst));
                if(!isInternalEndpoint && (failureIds.contains(pathEdge.getId()) || defaultBehavior)) {
                    Double reversedMetric = -1 * pathEdge.getWeight();
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


            if(k < numPaths + nfe){
                if(pathCanFail && nfe > 0) {
                    k++;
                }
                // Find the new shortest path
                List<Link> modShortestPath = bellmanFordService.shortestPath(modifiedTopo, src, dst);
                tempPaths.add(modShortestPath);
            }



        }
        return  combine(shortestPath, tempPaths, reversedToOriginalMap, modifiedTopo, src, dst, k);
    }

    private boolean checkIfInternalEndpoint(Link link, Node src, Node dst){
        // Is internal if origin and target share the same root node name
        String originId = link.getOrigin().getId().replace("-incoming", "").replace("-outgoing", "");
        String targetId = link.getTarget().getId().replace("-incoming", "").replace("-outgoing", "");
        return originId.contains(targetId) && (src.getId().contains(originId) || dst.getId().contains(originId));
    }

    private List<List<Link>> convertToOriginalTopoLinks(List<List<Link>> pathLinks) {
        List<List<Link>> modifiedPaths = new ArrayList<>();
        for(List<Link> path : pathLinks){
            List<Link> newPath = path.stream().filter(l -> !l.getId().contains("-internal")).map(l ->
                    Link.builder()
                            .id(l.getId())
                            .origin(new Node(l.getOrigin().getId().replace("-outgoing", ""), l.getOrigin().getPoint()))
                            .target(new Node(l.getTarget().getId().replace("-incoming", ""), l.getTarget().getPoint()))
                            .weight(l.getWeight())
                            .points(l.getPoints())
                            .build()
            ).collect(Collectors.toList());
            modifiedPaths.add(newPath);
        }
        return modifiedPaths;
    }

    private Topology makeNodeFailTopo(Topology topo) {
        // For each node n, create an "n-incoming" node and "n-outgoing" node, and connect them with an internal link
        // Connect all incoming edges to n to the new incoming ndoe, and all outgoing edges from n from the new outgoing node
        Set<Node> originalNodes = topo.getNodes();
        Set<Link> internalLinks = originalNodes.stream().map(this::buildInternalLink).collect(Collectors.toSet());
        Set<Node> modifiedNodes = new HashSet<>();
        for(Link internalLink : internalLinks){
            modifiedNodes.add(internalLink.getOrigin());
            modifiedNodes.add(internalLink.getTarget());
        }
        Set<Link> modifiedLinks = new HashSet<>();
        for(Link l : topo.getLinks()){
            Link newLink = Link.builder()
                    .id(l.getId())
                    .origin(new Node(l.getOrigin().getId() + "-outgoing", l.getOrigin().getPoint()))
                    .target(new Node(l.getTarget().getId() + "-incoming", l.getTarget().getPoint()))
                    .weight(l.getWeight())
                    .points(l.getPoints())
                    .build();
            modifiedLinks.add(newLink);
        }
        modifiedLinks.addAll(internalLinks);
        return new Topology(topo.getId() + "-modified", modifiedNodes, modifiedLinks);
    }

    private Set<Link> convertFailuresToLinks(Set<Failure> failures, Boolean nodesCanFail) {
        Set<Link> failureLinks = new HashSet<>();
        for(Failure failure : failures){
            Link link = failure.getLink();
            Node node = failure.getNode();
            if(link != null){
                //Link externalLink = nodesCanFail ? makeExternalLink(link) : link;

                failureLinks.add(link);
                // Add inverse link
                Link inverse =  new Link(link.getTarget(), link.getOrigin(), link.getWeight());
                /*if(nodesCanFail){
                    inverse = makeExternalLink(inverse);
                }*/
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
                        .points(internalLink.getPoints())
                        .build();
                failureLinks.add(inverse);
            }
        }
        return failureLinks;
    }

    private Link makeExternalLink(Link link) {
        Node origin = link.getOrigin();
        Node target = link.getTarget();
        Node newOrigin = new Node(origin.getId() + "-outgoing", origin.getPoint());
        Node newTarget = new Node(target.getId() + "-incoming", origin.getPoint());
        return new Link(newOrigin, newTarget, link.getWeight());
    }

    private Link buildInternalLink(Node node){
        Node incomingNode = new Node(node.getId() + "-incoming", node.getPoint());
        Node outgoingNode = new Node(node.getId() + "-outgoing", node.getPoint());
        return Link.builder()
                .id(node.getId() + "-internal")
                .origin(incomingNode)
                .target(outgoingNode)
                .weight(0.0)
                .build();
    }


    private Set<Link> findAllBetweenPair(Node src, Node dst, Set<Link> edges){
        return edges.stream()
                .filter(e -> e.getOrigin().equals(src) && e.getTarget().equals(dst)|| e.getOrigin().equals(dst) && e.getTarget().equals(src))
                .collect(Collectors.toSet());
    }

    private List<List<Link>> combine(List<Link> shortestPath, List<List<Link>> tempPaths,
                                     Map<Link, Link> reversedToOriginalMap, Topology topo,
                                     Node source, Node dest, Integer k) {
        // Remove all inverse edges taken in new shortest path (along with mapped edge in original shortest path)
        /*Set<Link> combinedEdges = new HashSet<>();
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

        combinedEdges.addAll(shortestPath);*/
        Set<Link> combinedEdges = new HashSet<>();
        combinedEdges.addAll(tempPaths.stream().flatMap(Collection::stream).collect(Collectors.toList()));
        for(int i = 0; i < tempPaths.size(); i++){
            List<Link> tempPath = tempPaths.get(i);
            for(Link link : tempPath){
                if (reversedToOriginalMap.containsKey(link)) {
                    Link origLink = reversedToOriginalMap.get(link);
                    combinedEdges.remove(link);
                    combinedEdges.remove(origLink);
                }
            }
        }

        // Convert edges if node-disjoint algorithm was used
        topo = convertFromNodeDisjoint(topo, combinedEdges);

        source.setId(source.getId().replace("-incoming", ""));
        dest.setId(dest.getId().replace("-outgoing", ""));

        return createPaths(topo, source, dest, k);
    }


    private Topology  convertFromNodeDisjoint(Topology topo, Set<Link> combinedEdges) {
        Set<Link> newLinks = new HashSet<>();
        for(Link link : combinedEdges){
            if(!link.getId().contains("internal")){
                link.getOrigin().setId(link.getOrigin().getId().replace("-incoming", "").replace("-outgoing", ""));
                link.getTarget().setId(link.getTarget().getId().replace("-incoming", "").replace("-outgoing", ""));
                newLinks.add(link);
            }
        }
        Set<Node> nodes = topo.getNodes();
        Set<Node> newNodes = new HashSet<>();
        for(Node node : nodes){
            String id = node.getId();
            id = id.replace("-incoming", "").replace("-outgoing", "");
            node.setId(id);
            newNodes.add(node);
        }

        newLinks = removeForwardReversePairs(newLinks);

        // Use the edges from paths that had to be split up
        return new Topology(topo.getId(), newNodes, newLinks);
    }

    private Set<Link> removeForwardReversePairs(Set<Link> links) {
        Set<Link> filteredLinks = new HashSet<>();
        for(Link link1 : links){
            boolean noMatch = true;
            for(Link link2 : links){
                if(Objects.equals(link1.getOrigin().getId(), link2.getTarget().getId())){
                    if(Objects.equals(link1.getTarget().getId(), link2.getOrigin().getId())){
                        noMatch = false;
                        break;
                    }
                }
            }
            if(noMatch){
                filteredLinks.add(link1);
            }
        }
        return filteredLinks;
    }

    private List<List<Link>> createPaths(Topology topo, Node source, Node dest, Integer k) {
        Map<Node, List<Link>> nodeLinkMap = topo.getNodeOrderedLinkMap();

        // Track all links used while performing a depth-first search
        /*List<Link> linksAlongPaths = new ArrayList<>();
        depthFirstSearch(nodeLinkMap, source, dest, linksAlongPaths);

        List<List<Link>> paths = new ArrayList<>();
        int pathIndex = 0;
        for(Link link : linksAlongPaths){
            if(link.getOrigin().equals(source)){
                paths.add(pathIndex, new ArrayList<>());
            }
            // If we've hit a sub-path that should be used later on
            if(pathIndex == paths.size()){

            }
            paths.get(pathIndex).add(link);
            if(link.getTarget().equals(dest)){
                pathIndex++;
            }
        }
        */
        //List<List<Link>> paths = bellmanFordService.kShortestPaths(topo, source, dest, k);
        Set<Set<String>> allLinksIds = new HashSet<>();
        List<List<Link>> paths = new ArrayList<>();
        Topology adjustedTopo = topologyService.adjustWeightsToMax(topo, new HashSet<>());
        for(int i = 0; i < k; i++){
            List<Link> sp = bellmanFordService.shortestPath(adjustedTopo, source, dest);
            Set<String> linkIds = sp.stream().map(Link::getId).collect(Collectors.toSet());
            // Only add a path if it's unique
            if(!allLinksIds.contains(linkIds)){
                paths.add(sp);
                adjustedTopo = topologyService.adjustWeightsToMaxWithLinks(adjustedTopo, new HashSet<>(sp));
                allLinksIds.add(linkIds);
            }
        }
        return topologyService.readjustLinkWeights(paths, topo);
    }

    private List<List<Link>> augmentPaths(List<List<Link>> paths, int k) {
        if(k < paths.size()){
            return paths;
        }
        int minIndex = 0;
        double minWeight = Double.MAX_VALUE;
        for(int index = 0; index < paths.size(); index++){
            double totalWeight = paths.get(index).stream().map(Link::getWeight).reduce(0.0, (w1, w2) -> w1 + w2);
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
