package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Topology {

    String id;

    Set<Node> nodes;

    Set<Link> links;

    Map<String, Node> nodeIdMap;

    Map<String, Link> linkIdMap;

    Map<Node, Set<Link>> nodeLinkMap;

    public Topology(String id, Set<Node> nodes, Set<Link> links){
        this.id = id;
        this.nodes = nodes;
        this.links = links;
        nodeIdMap = nodes.stream().collect(Collectors.toMap(Node::getId, node -> node));
        linkIdMap = links.stream().collect(Collectors.toMap(Link::getId, link -> link));
        nodeLinkMap = makeNodeLinkMap(nodes, links);
    }

    public void setLinks(Set<Link> links){
        this.links = links;
        this.nodeLinkMap = makeNodeLinkMap(this.nodes, this.links);
    }

    public void setNodes(Set<Node> nodes){
        this.nodes = nodes;
        this.nodeLinkMap = makeNodeLinkMap(this.nodes, this.links);
    }

    private Map<Node, Set<Link>> makeNodeLinkMap(Set<Node> nodes, Set<Link> links){
        Map<Node, Set<Link>> nodeLinkMap = nodes.stream().collect(Collectors.toMap(n -> n, n -> new HashSet<>()));
        for(Link link : links){
            nodeLinkMap.get(link.getOrigin()).add(link);
        }
        return nodeLinkMap;
    }
}
