package netlab.processing.shortestPaths;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.GraphConversionService;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.ALTAdmissibleHeuristic;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AStarService {

    GraphConversionService graphConversionService;

    @Autowired
    public AStarService(GraphConversionService graphConversionService){
        this.graphConversionService = graphConversionService;
    }

    public List<Link> shortestPath(Topology topo, Node source, Node dest){
        Map<DefaultWeightedEdge, Link> edgeToLinkMap = new HashMap<>();
        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> graph = graphConversionService.convertToGraph(topo, edgeToLinkMap);
        ALTAdmissibleHeuristic<Node, DefaultWeightedEdge> heuristic = new ALTAdmissibleHeuristic<>(graph, topo.getNodes());
        AStarShortestPath<Node, DefaultWeightedEdge> shortestPath = new AStarShortestPath<>(graph,heuristic);
        GraphPath<Node,DefaultWeightedEdge> graphPath = shortestPath.getPath(source, dest);
        return graphConversionService.convertToLinks(graphPath, edgeToLinkMap);
    }

    public Map<SourceDestPair, List<Link>> allShortestPaths(Topology topo) {
        Map<DefaultWeightedEdge, Link> edgeToLinkMap = new HashMap<>();

        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> graph = graphConversionService.convertToGraph(topo, edgeToLinkMap);
        ALTAdmissibleHeuristic<Node, DefaultWeightedEdge> heuristic = new ALTAdmissibleHeuristic<>(graph, topo.getNodes());
        AStarShortestPath<Node, DefaultWeightedEdge> allPaths = new AStarShortestPath<>(graph,heuristic);

        return graphConversionService.allPathsConversion(allPaths, topo, edgeToLinkMap);

    }
}
