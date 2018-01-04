package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Node implements Serializable {

    private String id;
    private Point point;

    public Node(String id, int x, int y){
        this.id = id;
        this.point = new Point(x,y);
    }


    public String toString(){
        return id;
    }
}
