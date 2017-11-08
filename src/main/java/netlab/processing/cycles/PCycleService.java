package netlab.processing.cycles;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.Topology;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PCycleService {


    public Details solve(Request request, Topology topo) {

        return request.getDetails();
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
}
