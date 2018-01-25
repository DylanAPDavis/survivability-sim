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
    private Location point;

    // ID, Latitude, Longitude
    public Node(String id, double lat, double lon){
        this.id = id;
        this.point = new Location(lat,lon);
    }


    public String toString(){
        return id;
    }
}
