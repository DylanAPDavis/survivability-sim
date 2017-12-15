package netlab.processing.cycles;

import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import netlab.processing.disjointpaths.BhandariService;
import netlab.processing.pathmapping.PathMappingService;
import netlab.submission.enums.FailureClass;
import netlab.submission.request.Connections;
import netlab.submission.request.Details;
import netlab.submission.request.NumFailureEvents;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HamiltonianCycleService {

    TopologyAdjustmentService topologyAdjustmentService;

    BhandariService bhandariService;

    PathMappingService pathMappingService;

    @Autowired
    public HamiltonianCycleService(TopologyAdjustmentService topologyAdjustmentService, BhandariService bhandariService,
                                   PathMappingService pathMappingService){
        this.topologyAdjustmentService = topologyAdjustmentService;
        this.bhandariService = bhandariService;
        this.pathMappingService = pathMappingService;
    }


    public Details solve(Request request, Topology topo) {


        boolean feasible = true;
        Map<SourceDestPair, Map<String, Path>> pathMap = new HashMap<>();
        long startTime = System.nanoTime();
        List<Node> hamiltonianCycle = findHamiltonianCycle(topo);
        if(hamiltonianCycle.size() != topo.getNodes().size() + 1){
            feasible = false;
        }
        else {
           pathMap = createPathsFromCycle(request, topo, hamiltonianCycle);
           //pathMap = addPathsForSurvivability(request, topo, pathMap);
        }
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;

        Details details = request.getDetails();
        details.setChosenPaths(pathMap);
        details.setIsFeasible(feasible);
        details.setRunningTimeSeconds(duration);
        return details;
    }


    public Map<SourceDestPair,Map<String,Path>> createPathsFromCycle(Request request, Topology topo,
                                                                      List<Node> hamiltonianCycle) {
        Map<SourceDestPair, List<Link>> neighborLinkMap = topo.getNeighborLinkMap();
        Details details = request.getDetails();
        Set<SourceDestPair> pairs = details.getPairs();
        // Get the cycle in the reverse direction
        List<Node> reverseCycle = new ArrayList<>(hamiltonianCycle);
        Collections.reverse(reverseCycle);

        Map<SourceDestPair, Map<String, Path>> pathMap = new HashMap<>();
        // Iterate through each pair
        // For each pair, find two paths between that pair by traversing the cycle
        for(SourceDestPair pair : pairs){
            pathMap.putIfAbsent(pair, new HashMap<>());
            List<Link> forwardPathLinks = findPathFromNodes(pair, hamiltonianCycle, neighborLinkMap);
            List<Link> reversePathLinks = findPathFromNodes(pair, reverseCycle, neighborLinkMap);
            Path forwardPath = new Path(forwardPathLinks);
            Path reversePath = new Path(reversePathLinks);
            if(forwardPath.getTotalWeight() >= reversePath.getTotalWeight()){
                pathMap.get(pair).put("1", forwardPath);
                pathMap.get(pair).put("2", reversePath);
            }
            else{
                pathMap.get(pair).put("1", reversePath);
                pathMap.get(pair).put("2", forwardPath);
            }
        }
        if(pairs.size() > 1) {
            pathMappingService.filterMap(pathMap, details);
        }
        return pathMap;
    }
    private List<Link> findPathFromNodes(SourceDestPair pair, List<Node> cycle, Map<SourceDestPair, List<Link>> neighborLinkMap) {
        List<Link> pathLinks = new ArrayList<>();
        Node src = pair.getSrc();
        Node dst = pair.getDst();
        int srcPosition = -1;
        int dstPosition = -1;
        for(int i = 0; i < cycle.size(); i++){
            Node cycleNode = cycle.get(i);
            if(cycleNode.equals(src)){
                srcPosition = i;
            }
            if(cycleNode.equals(dst)){
                dstPosition = i;
            }
        }

        // Now we know where the src is and where the dst is, so we can find a path between them
        int position = srcPosition;
        while(position != dstPosition){
            if(position == cycle.size()-1){
                position = 0;
            }
            Node thisNode = cycle.get(position);
            Node nextNode = cycle.get(position + 1);
            Link nextLink = getConnectingLink(thisNode, nextNode, neighborLinkMap);
            pathLinks.add(nextLink);
            position++;
        }
        /*if(srcPosition < dstPosition){
            // Either advance through the cycle to get to the dst
            for(int i = srcPosition; i < dstPosition; i++){
                pathLinks.add(getConnectingLink(cycle.get(i), cycle.get(i+1), neighborLinkMap));
            }
        }
        else{
            // Or advance backwards through the cycle
            for(int i = srcPosition; i > dstPosition; i--){
                pathLinks.add(getConnectingLink(cycle.get(i), cycle.get(i-1), neighborLinkMap));
            }
        }*/
        return pathLinks;
    }

    public Link getConnectingLink(Node origin, Node target, Map<SourceDestPair, List<Link>> neighborLinkMap){
        SourceDestPair neighborPair = new SourceDestPair(origin, target);
        List<Link> links = neighborLinkMap.get(neighborPair);
        return links.get(0);
    }

    public List<Node> findHamiltonianCycle(Topology topo){
        Set<Node> nodes = topo.getNodes();
        Map<Node, Set<Node>> neighborMap = topo.getNeighborMap();
        Map<Node, List<Link>> orderedLinkMap = topo.getNodeOrderedLinkMap();
        List<Node> orderedNodes = nodes.stream().sorted(Comparator.comparing(Node::getId)).collect(Collectors.toList());

        List<Node> path = new ArrayList<>();
        path.add(orderedNodes.get(0));
        Set<Node> included = new HashSet<>();
        included.add(orderedNodes.get(0));
        if(canBuildCycle(orderedLinkMap, neighborMap, path, included, 1)){
            path.add(orderedNodes.get(0));
            return path;
        }
        return new ArrayList<>();
    }

    private boolean canBuildCycle(Map<Node, List<Link>> orderedLinkMap, Map<Node, Set<Node>> neighborMap,
                                  List<Node> path, Set<Node> included, int pos) {
        // If all vertices are included in the Hamiltonian cycle
        if(pos == orderedLinkMap.size()){
            // If there is an edge from the last included vertex to the first vertex, then this is a cycle!
            Node lastNode = path.get(path.size()-1);
            return neighborMap.get(lastNode).contains(path.get(0));
        }

        // Try different vertices as the next candidate in Hamiltonian cycle.
        // Check adjacent vertices to current last node
        for(Link link : orderedLinkMap.get(path.get(pos-1))){
            Node target = link.getTarget();
            // If the target node hasn't been included yet
            if(!included.contains(target)){
                path.add(target);
                included.add(target);
                // See if you can build the rest of the cycle
                if(canBuildCycle(orderedLinkMap, neighborMap, path, included, pos + 1)){
                    return true;
                }
                // If adding this node doesn't lead to a solution, then remove it
                path.remove(pos);
                included.remove(target);
            }
        }

        // If you reach this point, there's no way to build the cycle from the final node in the path
        return false;
    }

    private Map<SourceDestPair, Map<String,Path>> addPathsForSurvivability(Request request, Topology topo,
                                                                          Map<SourceDestPair, Map<String, Path>> pathMap) {
        NumFailureEvents nfe = request.getDetails().getNumFailureEvents();
        boolean nodesCanFail = !request.getFailureClass().equals(FailureClass.Link);
        Set<Failure> failureSet = request.getDetails().getFailures().getFailureSet();
        // If you only need to survive one failure, the hamiltonian cycle accomplishes that goal
        if(nfe.getTotalNumFailureEvents() <= 1){
            return pathMap;
        }

        // Otherwise, increase cost of all links that are already used, then get a bhandari's solution that will try
        // to avoid those links
        Set<Path> originalPaths = pathMap.values().stream().map(Map::values).flatMap(Collection::stream).collect(Collectors.toSet());
        Topology modifiedTopo = topologyAdjustmentService.adjustWeightsToMax(topo, originalPaths);
        Set<SourceDestPair> pairs = request.getDetails().getPairs();
        for(SourceDestPair pair : pairs){
            // Add NFE - 1 disjoint paths
            List<List<Link>> pathLinks = bhandariService.computeDisjointPaths(modifiedTopo, pair.getSrc(), pair.getDst(), 1,
                    nfe.getTotalNumFailureEvents()-1, nodesCanFail, failureSet, false);
            List<Path> paths = pathMappingService.convertToPaths(pathLinks, topo.getLinkIdMap());
            // Add these new paths
            Map<String, Path> pathIdMap = pathMap.get(pair);
            for(Path path : paths){
                pathIdMap.put(String.valueOf(pathIdMap.size() + 1), path);
            }
            pathMap.put(pair, pathIdMap);
        }

        return pathMap;
    }
}
