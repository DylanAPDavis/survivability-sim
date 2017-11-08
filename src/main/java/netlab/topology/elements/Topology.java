package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
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

    Map<Node, List<Link>> nodeOrderedLinkMap;

    Map<Node, Set<Node>> neighborMap;

    Map<SourceDestPair, Long> minimumPathCostMap;

    public Topology(String id, Set<Node> nodes, Set<Link> links){
        this.id = id;
        this.nodes = nodes;
        this.links = links;
        nodeIdMap = nodes.stream().collect(Collectors.toMap(Node::getId, node -> node));
        linkIdMap = links.stream().collect(Collectors.toMap(Link::getId, link -> link));
        nodeLinkMap = makeNodeLinkMap(nodes, links);
        nodeOrderedLinkMap = makeNodeOrderedLinkMap(nodes, links);
        neighborMap = makeNeighborMap(links);
    }

    public void setLinks(Set<Link> links){
        this.links = links;
        this.nodeLinkMap = makeNodeLinkMap(this.nodes, this.links);
        this.nodeOrderedLinkMap = makeNodeOrderedLinkMap(this.nodes, this.links);
        neighborMap = makeNeighborMap(this.links);
    }

    public void setNodes(Set<Node> nodes){
        this.nodes = nodes;
        this.nodeLinkMap = makeNodeLinkMap(this.nodes, this.links);
        this.nodeOrderedLinkMap = makeNodeOrderedLinkMap(this.nodes, this.links);
        neighborMap = makeNeighborMap(this.links);
    }

    public void copyPathCosts(Topology otherTopo){
        this.minimumPathCostMap = otherTopo.getMinimumPathCostMap();
    }

    private Map<Node, Set<Link>> makeNodeLinkMap(Set<Node> nodes, Set<Link> links){
        Map<Node, Set<Link>> nodeLinkMap = nodes.stream().collect(Collectors.toMap(n -> n, n -> new HashSet<>()));
        for(Link link : links){
            nodeLinkMap.get(link.getOrigin()).add(link);
        }
        return nodeLinkMap;
    }

    private Map<Node,List<Link>> makeNodeOrderedLinkMap(Set<Node> nodes, Set<Link> links) {
        Map<Node, List<Link>> orderedMap = nodes.stream().collect(Collectors.toMap(n -> n, n -> new ArrayList<>()));
        List<Link> sortedLinks = links
                .stream()
                .sorted(Comparator.comparing(Link::getWeight))
                .sorted(Comparator.comparing(Link::getId))
                .collect(Collectors.toList());
        for(Link link : sortedLinks){
            orderedMap.get(link.getOrigin()).add(link);
        }
        return orderedMap;
    }

    private Map<Node,Set<Node>> makeNeighborMap(Set<Link> links) {
        Map<Node, Set<Node>> neighborMap = new HashMap<>();
        for(Link link : links){
            Node origin = link.getOrigin();
            Node target = link.getTarget();
            neighborMap.putIfAbsent(origin, new HashSet<>());
            neighborMap.get(origin).add(target);
        }
        return neighborMap;
    }
}
