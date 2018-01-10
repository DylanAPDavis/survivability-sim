package netlab.processing.pathmapping;

import lombok.extern.slf4j.Slf4j;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GraphConversionService {

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

    public List<Link> convertToLinks(GraphPath<Node,DefaultWeightedEdge> graphPath, Map<DefaultWeightedEdge, Link> edgeToLinkMap){
        if(graphPath != null) {
            List<DefaultWeightedEdge> edgeList = graphPath.getEdgeList();
            return edgeList.stream().map(edgeToLinkMap::get).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public Map<SourceDestPair, List<Link>> allPathsConversion(ShortestPathAlgorithm<Node, DefaultWeightedEdge> allPaths,
                                                              Topology topo, Map<DefaultWeightedEdge, Link> edgeToLinkMap){
        Map<SourceDestPair, List<Link>> shortestPathMap = new HashMap<>();
        for(Node src : topo.getNodes()){
            ShortestPathAlgorithm.SingleSourcePaths<Node, DefaultWeightedEdge> srcPaths = allPaths.getPaths(src);
            for(Node dst : topo.getNodes()){
                if(src != dst){
                    SourceDestPair pair = new SourceDestPair(src, dst);
                    GraphPath<Node,DefaultWeightedEdge> path = srcPaths.getPath(dst);
                    if(path != null){
                        List<Link> edgeList = convertToLinks(path, edgeToLinkMap);
                        shortestPathMap.put(pair, edgeList);
                    }
                }
            }
        }
        return shortestPathMap;
    }
}
