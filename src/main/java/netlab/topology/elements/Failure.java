package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Failure implements Serializable {

    private Node node;
    private Link link;
    private Double probability;
    private String id;

    public Failure(Node node, Link link, Double probability){
        this.node = node;
        this.link = link;
        this.probability = probability;
        if(node != null){
            this.id = node.getId();
        }
        if(link != null){
            this.id = link.getId();
        }
    }

    public String toString(){
        return id + ": " + probability;
    }
}
