package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Path implements Serializable {

    private List<Link> links;
    private List<Node> nodes;

    private Set<String> linkIds;
    private Set<String> reverseLinkIds;
    private Set<String> nodeIds;

    private Double totalWeight;

    private String id;

    public Path(List<Link> links){
        this.links = links;
        this.nodes = getNodes(links);
        this.linkIds = links.stream().map(Link::getId).collect(Collectors.toSet());
        this.reverseLinkIds = links.stream().map(Link::reverseId).collect(Collectors.toSet());
        this.nodeIds = this.nodes.stream().map(Node::getId).collect(Collectors.toSet());
        this.id = links.stream().map(Link::getId).reduce((i1, i2) -> i1 + "_" + i2).orElse("EMPTY");
        totalWeight = links.size() > 0 ? links.stream().mapToDouble(Link::getWeight).sum() : 0L;
    }

    public void appendNode(Node node){
        this.nodes.add(node);
        this.nodeIds.add(node.getId());
    }

    public void appendLink(Link link){
        this.links.add(link);
        this.linkIds.add(link.getId());
        if(!nodeIds.contains(link.getOrigin().getId())){
            appendNode(link.getOrigin());
        }
        appendNode(link.getTarget());
        this.totalWeight += link.getWeight();
    }

    public Path combinePaths(Path otherPath){
        List<Link> allLinks = new ArrayList<>(this.links);
        allLinks.addAll(otherPath.getLinks());
        return new Path(allLinks);
    }


    private List<Node> getNodes(List<Link> links){
        List<Node> nodes = new ArrayList<>();
        if(links.size() == 0){
            return nodes;
        }
        for(Link link : links){
            nodes.add(link.getOrigin());
        }
        nodes.add(links.get(links.size()-1).getTarget());
        return nodes;
    }

    public String toString(){
        //return this.links.stream().map(l -> String.format("(%s, %s)", l.getOrigin().getId(), l.getTarget().getId())).collect(joining(" "));
        return this.nodes.stream().map(Node::getId).collect(joining(", "));
    }

    public boolean containsFailureId(String f){
        return linkIds.contains(f) || reverseLinkIds.contains(f)||  nodeIds.contains(f);
    }

    public boolean containsFailureIds(Collection<String> failureIds){
        return failureIds.stream().anyMatch(this::containsFailureId);
    }

    public boolean containsFailures(Collection<Failure> failures){
        for(Failure failure : failures){
            String failureId = failure.getNode() != null ? failure.getNode().getId() : failure.getLink().getId();
            String reverseLinkId = failure.getLink() != null ? failure.getLink().reverse().getId() : "";
            if(linkIds.contains(failureId) || linkIds.contains(reverseLinkId) || nodeIds.contains(failureId)){
                return true;
            }
        }
        return false;
    }

    public boolean isDisjoint(Path otherPath, boolean nodeDisjoint){
        if(nodeDisjoint){
            // Node disjoint if no node id in other path is in this path
            return otherPath.nodeIds.stream().noneMatch(this.nodeIds::contains);
        }
        // Link disjoint if no link id in other path is in this path
        return  otherPath.linkIds.stream().noneMatch(this.linkIds::contains);
    }

    public Path reverse(){
        List<Link> pathLinks = new ArrayList<>(this.links);
        Collections.reverse(pathLinks);
        return new Path(pathLinks.stream().map(Link::reverse).collect(Collectors.toList()));
    }

    public boolean isEmpty(){
        return this.links.isEmpty();
    }

}
