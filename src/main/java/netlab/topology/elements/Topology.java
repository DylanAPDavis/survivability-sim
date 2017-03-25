package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
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

    public Topology(Set<Node> nodes, Set<Link> links){
        this.nodes = nodes;
        this.links = links;
        nodeIdMap = nodes.stream().collect(Collectors.toMap(Node::getId, node -> node));
        linkIdMap = links.stream().collect(Collectors.toMap(Link::getId, link -> link));
        nodeLinkMap = new HashMap<>();
    }
}
