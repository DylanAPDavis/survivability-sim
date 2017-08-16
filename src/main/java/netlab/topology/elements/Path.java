package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private Set<String> nodeIds;

    private Long totalWeight;

    public Path(List<Link> links){
        this.links = links;
        this.nodes = getNodes(links);
        this.linkIds = links.stream().map(Link::getId).collect(Collectors.toSet());
        this.nodeIds = this.nodes.stream().map(Node::getId).collect(Collectors.toSet());
        totalWeight = links.stream().map(Link::getWeight).reduce(0L, (w1, w2) -> w1 + w2);
    }


    private List<Node> getNodes(List<Link> links){
        List<Node> nodes = new ArrayList<>();
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

}
