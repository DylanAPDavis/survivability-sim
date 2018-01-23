package netlab.processing.shortestPaths;

import lombok.extern.slf4j.Slf4j;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.Topology;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class DijkstraService {

    public List<Link> shortestPath(Topology topo, Node source, Node dest){

        return shortestPath(topo, source, dest, new HashMap<>());
    }

    public List<Link> shortestPath(Topology topo, Node source, Node dest, Map<Link, Double> riskMap){
        Map<Node, Long> distance = new HashMap<>();
        //Map<Node, Node> prev = new HashMap<>();
        Map<Node, Link> prevLink = new HashMap<>();
        distance.put(source, 0L);
        Comparator<Node> comparator = Comparator.comparing(distance::get);

        Set<Node> nodes = topo.getNodes();
        PriorityQueue<Node> queue = new PriorityQueue<>(nodes.size(), comparator);
        for(Node node : nodes){
            if(!node.getId().equals(source.getId())){
                distance.put(node, Long.MAX_VALUE);
            }
            queue.add(node);
        }

        Map<Node, List<Link>> nodeLinkMap = topo.getNodeOrderedLinkMap();
        while(!queue.isEmpty()){
            Node u = queue.poll();
            for(Link link : nodeLinkMap.get(u)){
                Node target = link.getTarget();
                if(queue.contains(target)){
                    Long newDistanceToTarget =  distance.get(u) + link.getWeight();
                    Double risk = riskMap.containsKey(link) ? riskMap.get(link) : 0.0;
                    // If the node hasn't been reached yet, just keep this link
                    if(!prevLink.containsKey(target)){
                        //prev.put(target, link.getOrigin());
                        prevLink.put(target, link);
                        distance.put(target, newDistanceToTarget);
                        queue.add(target);
                    }
                    // Otherwise, compare the alt weights first, then the
                    else{
                        Link pLink = prevLink.get(target);
                        Double pRisk = riskMap.containsKey(pLink) ? riskMap.get(pLink) : 0.0;
                        if(risk < pRisk || (risk.equals(pRisk) && newDistanceToTarget < distance.get(target)) ){
                            //prev.put(target, link.getOrigin());
                            prevLink.put(target, link);
                            distance.put(target, newDistanceToTarget);
                            queue.add(target);
                        }
                    }
                }
            }
        }
        List<Link> path = new ArrayList<>();
        Node currentNode = dest;
        while(currentNode != source){
            Link pLink = prevLink.get(currentNode);
            path.add(0, pLink);
            currentNode = pLink.getOrigin();
        }
        return path;
    }

    /*
14     while Q is not empty:                              // The main loop
15         u ← Q.extract_min()                            // Remove and return best vertex
16         for each neighbor v of u:                      // only v that is still in Q
17             alt ← dist[u] + length(u, v)
18             if alt < dist[v]
19                 dist[v] ← alt
20                 prev[v] ← u
21                 Q.decrease_priority(v, alt)
22
23     return dist, prev
     */
}
