package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.transform.Source;
import java.awt.*;
import java.util.*;
import java.util.List;
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

    Map<SourceDestPair, List<Link>> neighborLinkMap;

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
        neighborLinkMap = makeNeighborLinkMap(links);
    }


    public void setLinks(Set<Link> links){
        this.links = links;
        nodeLinkMap = makeNodeLinkMap(this.nodes, this.links);
        nodeOrderedLinkMap = makeNodeOrderedLinkMap(this.nodes, this.links);
        neighborMap = makeNeighborMap(this.links);
        neighborLinkMap = makeNeighborLinkMap(this.links);
    }

    public void setNodes(Set<Node> nodes){
        this.nodes = nodes;
        nodeLinkMap = makeNodeLinkMap(this.nodes, this.links);
        nodeOrderedLinkMap = makeNodeOrderedLinkMap(this.nodes, this.links);
        neighborMap = makeNeighborMap(this.links);
        neighborLinkMap = makeNeighborLinkMap(this.links);
    }

    public void copyPathCosts(Topology otherTopo){
        this.minimumPathCostMap = otherTopo.getMinimumPathCostMap() != null ? new HashMap<>(otherTopo.getMinimumPathCostMap()) : new HashMap<>();
    }

    private Map<Node, Set<Link>> makeNodeLinkMap(Set<Node> nodes, Set<Link> links){
        Map<Node, Set<Link>> nodeLinkMap = nodes.stream().collect(Collectors.toMap(n -> n, n -> new HashSet<>()));
        for(Link link : links){
            nodeLinkMap.putIfAbsent(link.getOrigin(), new HashSet<>());
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
            orderedMap.putIfAbsent(link.getOrigin(), new ArrayList<>());
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

    private Map<SourceDestPair, List<Link>> makeNeighborLinkMap(Set<Link> links) {
        Map<SourceDestPair, List<Link>> neighborMap = new HashMap<>();
        for(Link link : links){
            Node origin = link.getOrigin();
            Node target = link.getTarget();
            SourceDestPair pair = new SourceDestPair(origin, target);
            neighborMap.putIfAbsent(pair, new ArrayList<>());
            neighborMap.get(pair).add(link);
        }
        for(SourceDestPair pair : neighborMap.keySet()){
            List<Link> sortedLinks = neighborMap.get(pair).stream().sorted(Comparator.comparing(Link::getWeight)).collect(Collectors.toList());
            neighborMap.put(pair, sortedLinks);
        }
        return neighborMap;
    }

    public Node getNodeById(String id){
        return nodeIdMap.get(id);
    }

    public Link getLinkById(String id){
        return linkIdMap.get(id);
    }
}
