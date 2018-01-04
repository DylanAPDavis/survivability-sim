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

    public String toString(){
        return (node != null ? node.getId() : link.getId()) + ": " + probability;
    }
}
