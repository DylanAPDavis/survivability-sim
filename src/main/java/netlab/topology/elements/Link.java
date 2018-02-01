package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Link implements Serializable {

    private String id;

    private Node origin;

    private Node target;

    private Double weight;

    private Set<Location> points;

    public Link(Node origin, Node target, Integer linkNum){
        this.origin = origin;
        this.target = target;
        this.id = origin.getId() + "-" + target.getId() + "-" + linkNum;
        this.weight = 1.0;
        setPoints();
    }

    public Link(Node origin, Node target, Double weight, Integer linkNum){
        this.origin = origin;
        this.target = target;
        this.id = origin.getId() + "-" + target.getId() + "-" + linkNum;
        this.weight = weight;
        setPoints();
    }

    public Link(Node origin, Node target){
        this.origin = origin;
        this.target = target;
        this.id = origin.getId() + "-" + target.getId();
        this.weight = 1.0;
        setPoints();
    }

    public Link(Node origin, Node target, Double weight){
        this.origin = origin;
        this.target = target;
        this.id = origin.getId() + "-" + target.getId();
        this.weight = weight;
        setPoints();
    }

    public Link(Node origin, Node target, Double weight, Set<Location> points){
        this.origin = origin;
        this.target = target;
        this.id = origin.getId() + "-" + target.getId();
        this.weight = weight;
        this.points = points;
    }

    public Link reverse(){
        return new Link(this.target, this.origin, this.weight, new HashSet<>(this.points));
    }

    public String reverseId(){
        String[] split = this.id.split("-");
        return split[1] + "-" + split[0];
    }

    private void setPoints(){
        Location originPoint = origin.getPoint();
        Location targetPoint = target.getPoint();


        double numDivisions = weight / 50;
        double stepDistance = weight / numDivisions;

        List<Location> orderedPoints = new ArrayList<>();
        orderedPoints.add(originPoint);

        double distanceSoFar = stepDistance;
        while(distanceSoFar < weight){
            Location newPoint = originPoint.locationBetweenGivenDistanceKM(targetPoint, distanceSoFar);
            orderedPoints.add(newPoint);
            distanceSoFar += stepDistance;
        }
        orderedPoints.add(targetPoint);
        points = new HashSet<>(orderedPoints);
    }
}
