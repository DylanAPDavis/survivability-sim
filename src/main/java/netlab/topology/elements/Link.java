package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Link {

    private String id;

    private Node origin;

    private Node target;

    private Long weight;

    public Link(Node origin, Node target, Integer linkNum){
        this.origin = origin;
        this.target = target;
        this.id = origin.getId() + "-" + target.getId() + "__" + linkNum;
        this.weight = 1L;
    }

    public Link(Node origin, Node target, Long weight, Integer linkNum){
        this.origin = origin;
        this.target = target;
        this.id = origin.getId() + "-" + target.getId() + "__" + linkNum;
        this.weight = weight;
    }

    public Link(Node origin, Node target){
        this.origin = origin;
        this.target = target;
        this.id = origin.getId() + "-" + target.getId();
        this.weight = 1L;
    }

    public Link(Node origin, Node target, Long weight){
        this.origin = origin;
        this.target = target;
        this.id = origin.getId() + "-" + target.getId();
        this.weight = weight;
    }
}
