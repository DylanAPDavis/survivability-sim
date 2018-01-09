package netlab.processing.shortestPaths;

import lombok.extern.slf4j.Slf4j;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.alg.shortestpath.KShortestPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BellmanFordService {

    public List<Link> shortestPath(Topology topo, Node source, Node dest){

        Map<DefaultWeightedEdge, Link> edgeToLinkMap = new HashMap<>();
        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> graph = convertToGraph(topo, edgeToLinkMap);

        BellmanFordShortestPath<Node, DefaultWeightedEdge> shortestPath = new BellmanFordShortestPath<>(graph);
        GraphPath<Node,DefaultWeightedEdge> graphPath = shortestPath.getPath(source, dest);
        if(graphPath != null) {
            List<DefaultWeightedEdge> edgeList = graphPath.getEdgeList();
            return edgeList.stream().map(edgeToLinkMap::get).collect(Collectors.toList());
        }
        return new ArrayList<>();
        /*
        Map<Node, Link> edgeMap = bellmanFord(topo, source);
        if(edgeMap.isEmpty()){
            return new ArrayList<>();
        }

        return buildPath(dest, source, edgeMap);
        */
    }

    public List<List<Link>> kShortestPaths(Topology topo, Node source, Node dest, int k){
        Map<DefaultWeightedEdge, Link> edgeToLinkMap = new HashMap<>();
        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> graph = convertToGraph(topo, edgeToLinkMap);

        KShortestPaths<Node, DefaultWeightedEdge> shortestPath = new KShortestPaths<>(graph, k);
        List<GraphPath<Node,DefaultWeightedEdge>> graphPaths = shortestPath.getPaths(source, dest);
        List<List<Link>> pathLinks = new ArrayList<>();
        if(graphPaths != null) {
            for(GraphPath<Node, DefaultWeightedEdge> graphPath : graphPaths) {
                List<DefaultWeightedEdge> edgeList = graphPath.getEdgeList();
                List<Link> path = edgeList.stream().map(edgeToLinkMap::get).collect(Collectors.toList());
                pathLinks.add(path);
            }
        }
        return pathLinks;
    }

    public Map<SourceDestPair, List<Link>> allShortestPaths(Topology topo){
        Map<SourceDestPair, List<Link>> shortestPathMap = new HashMap<>();
        Map<DefaultWeightedEdge, Link> edgeToLinkMap = new HashMap<>();

        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> graph = convertToGraph(topo, edgeToLinkMap);
        BellmanFordShortestPath<Node, DefaultWeightedEdge> allPaths = new BellmanFordShortestPath<>(graph);

        for(Node src : topo.getNodes()){
            ShortestPathAlgorithm.SingleSourcePaths<Node, DefaultWeightedEdge> srcPaths = allPaths.getPaths(src);
            for(Node dst : topo.getNodes()){
                if(src != dst){
                    SourceDestPair pair = new SourceDestPair(src, dst);
                    GraphPath<Node,DefaultWeightedEdge> path = srcPaths.getPath(dst);
                    if(path != null){
                        List<Link> edgeList = path.getEdgeList().stream().map(edgeToLinkMap::get).collect(Collectors.toList());
                        shortestPathMap.put(pair, edgeList);
                    }
                }
            }
        }
        return shortestPathMap;
    }

    public DirectedWeightedMultigraph<Node, DefaultWeightedEdge> convertToGraph(Topology topo, Map<DefaultWeightedEdge, Link> edgeToLinkMap){
        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> graph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);
        for(Node node : topo.getNodes()){
            graph.addVertex(node);
        }
        for(Link link : topo.getLinks()){
            DefaultWeightedEdge e = graph.addEdge(link.getOrigin(), link.getTarget());
            graph.setEdgeWeight(e, link.getWeight());
            edgeToLinkMap.put(e, link);
        }
        return graph;
    }

    public Map<Node, List<Link>> allShortestPaths(Topology topo, Node source){

        Map<Node, Link> edgeMap = bellmanFord(topo, source);
        if(edgeMap.isEmpty()){
            return new HashMap<>();
        }

        Map<Node, List<Link>> allPaths = new HashMap<>();

        Set<Node> vertices = topo.getNodes();

        for(Node vertex : vertices){
            allPaths.put(vertex, buildPath(vertex, source, edgeMap));
        }

        return allPaths;
    }

    private List<Link> buildPath(Node vertex, Node source, Map<Node, Link> edgeMap){
        List<Link> path = new ArrayList<>();
        Node currentNode = vertex;
        while(!currentNode.equals(source)){
            if(!edgeMap.containsKey(currentNode)){
                path = new ArrayList<>();
                break;
            }
            Link edge = edgeMap.get(currentNode);
            path.add(edge);
            currentNode = edge.getOrigin();
        }
        Collections.reverse(path);
        return path;
    }

    private Map<Node, Link> bellmanFord(Topology topo, Node source){

        Map<Node, Long> distanceMap = new HashMap<>();
        Map<Node, Link> edgeMap = new HashMap<>();

        Set<Node> vertices = topo.getNodes();
        List<Link> edges = topo
                .getLinks()
                .stream()
                .sorted((a,z) -> a.getOrigin().getId().compareToIgnoreCase(z.getOrigin().getId()))
                .collect(Collectors.toList());

        for(Node vertex : vertices){
            distanceMap.put(vertex, 999999999L);
        }

        distanceMap.put(source, 0L);

        for(Integer i = 0; i < vertices.size()-1; i++){
            boolean noChanges = true;
            for(Link edge : edges){
                Node a = edge.getOrigin();
                Node z = edge.getTarget();

                if(!distanceMap.containsKey(a) || !distanceMap.containsKey(z))
                {
                    log.info("At least one vertex on an edge does not exist in topology");
                    log.info("Edge: " + edge);
                    return new HashMap<>();
                }

                Long weight = distanceMap.get(a) + edge.getWeight();
                if(weight < distanceMap.get(z)){
                    distanceMap.put(z, weight);
                    edgeMap.put(z, edge);
                    noChanges = false;
                }
            }
            if(noChanges){
                break;
            }
        }

        return edgeMap;
    }
}
