package netlab.processing.shortestPaths;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.GraphConversionService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BellmanFordService {

    GraphConversionService graphConversionService;

    @Autowired
    public BellmanFordService(GraphConversionService graphConversionService){
        this.graphConversionService = graphConversionService;
    }

    public List<Link> shortestPath(Topology topo, Node source, Node dest){

        Map<DefaultWeightedEdge, Link> edgeToLinkMap = new HashMap<>();
        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> graph = graphConversionService.convertToGraph(topo, edgeToLinkMap);

        BellmanFordShortestPath<Node, DefaultWeightedEdge> shortestPath = new BellmanFordShortestPath<>(graph);
        GraphPath<Node,DefaultWeightedEdge> graphPath = shortestPath.getPath(source, dest);
        if(graphPath != null) {
            List<DefaultWeightedEdge> edgeList = graphPath.getEdgeList();
            return edgeList.stream().map(edgeToLinkMap::get).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<List<Link>> kShortestPaths(Topology topo, Node source, Node dest, int k){
        Map<DefaultWeightedEdge, Link> edgeToLinkMap = new HashMap<>();
        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> graph = graphConversionService.convertToGraph(topo, edgeToLinkMap);

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
        Map<DefaultWeightedEdge, Link> edgeToLinkMap = new HashMap<>();

        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> graph = graphConversionService.convertToGraph(topo, edgeToLinkMap);
        BellmanFordShortestPath<Node, DefaultWeightedEdge> allPaths = new BellmanFordShortestPath<>(graph);

        return graphConversionService.allPathsConversion(allPaths, topo, edgeToLinkMap);
        /*
        Map<SourceDestPair, List<Link>> shortestPathMap = new HashMap<>();
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
        */
    }


}
