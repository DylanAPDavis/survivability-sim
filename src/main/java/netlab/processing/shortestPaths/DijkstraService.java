package netlab.processing.shortestPaths;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.GraphConversionService;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class DijkstraService {

    GraphConversionService graphConversionService;

    @Autowired
    public DijkstraService(GraphConversionService graphConversionService){
        this.graphConversionService = graphConversionService;
    }

    public List<Link> shortestPath(Topology topo, Node source, Node dest){
        Map<DefaultWeightedEdge, Link> edgeToLinkMap = new HashMap<>();
        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> graph = graphConversionService.convertToGraph(topo, edgeToLinkMap);
        DijkstraShortestPath<Node, DefaultWeightedEdge> shortestPath = new DijkstraShortestPath<>(graph);
        GraphPath<Node,DefaultWeightedEdge> graphPath = shortestPath.getPath(source, dest);
        return graphConversionService.convertToLinks(graphPath, edgeToLinkMap);
    }

    public Map<SourceDestPair, List<Link>> allShortestPaths(Topology topo) {
        Map<DefaultWeightedEdge, Link> edgeToLinkMap = new HashMap<>();

        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> graph = graphConversionService.convertToGraph(topo, edgeToLinkMap);
        DijkstraShortestPath<Node, DefaultWeightedEdge> allPaths = new DijkstraShortestPath<>(graph);

        return graphConversionService.allPathsConversion(allPaths, topo, edgeToLinkMap);

    }

    public List<Link> shortestPathWithAltWeights(Topology topo, Node source, Node dest, Map<Link, Double> riskMap){
        Map<Node, Double> distance = new HashMap<>();
        Map<Node, Double> cumulativeRisk = new HashMap<>();
        Map<Node, Link> prevLink = new HashMap<>();
        distance.put(source, 0.0);
        cumulativeRisk.put(source, 0.0);
        Comparator<Node> comparator = Comparator.comparing(cumulativeRisk::get);

        Set<Node> nodes = topo.getNodes();
        PriorityQueue<Node> queue = new PriorityQueue<>(nodes.size(), comparator);
        queue.add(source);
        for(Node node : nodes){
            if(!node.getId().equals(source.getId())){
                distance.put(node, Double.MAX_VALUE);
                cumulativeRisk.put(node, Double.MAX_VALUE);
            }
            //queue.add(node);
        }

        Map<Node, List<Link>> nodeLinkMap = topo.getNodeOrderedLinkMap();
        while(!queue.isEmpty()){
            Node u = queue.poll();
            for(Link link : nodeLinkMap.get(u)){
                Node currentTarget = link.getTarget();
                Double newDistanceToTarget = distance.get(u) + link.getWeight();
                Double newRiskToTarget = cumulativeRisk.get(u) + (riskMap.containsKey(link) ? riskMap.get(link) : 0.0);
                Double currentRiskToTarget = cumulativeRisk.get(currentTarget);
                Double currentDistanceToTarget = distance.get(currentTarget);
                if (compareLinks(newRiskToTarget, newDistanceToTarget, currentRiskToTarget, currentDistanceToTarget)) {
                    prevLink.put(currentTarget, link);
                    distance.put(currentTarget, newDistanceToTarget);
                    cumulativeRisk.put(currentTarget, newRiskToTarget);
                    if(!queue.contains(currentTarget)){
                        queue.add(currentTarget);
                    }
                }
                    //}
            }
        }
        // Starting from the dest, build the path back to source in reverse
        List<Link> path = new ArrayList<>();
        Node currentNode = dest;
        while(!currentNode.getId().equals(source.getId())){
            Link pLink = prevLink.get(currentNode);
            path.add(0, pLink);
            currentNode = pLink.getOrigin();
        }
        return path;
    }

    public boolean compareLinks(Double newRisk, Double newDistance, Double pRisk, Double pDistance){
        //return newDistance < pDistance;
        return newRisk < pRisk || (newRisk.equals(pRisk) && newDistance < pDistance);
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
