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

    private Long weight;

    private Set<Point> points;

    public Link(Node origin, Node target, Integer linkNum){
        this.origin = origin;
        this.target = target;
        this.id = origin.getId() + "-" + target.getId() + "-" + linkNum;
        this.weight = 1L;
        setPoints();
    }

    public Link(Node origin, Node target, Long weight, Integer linkNum){
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
        this.weight = 1L;
        setPoints();
    }

    public Link(Node origin, Node target, Long weight){
        this.origin = origin;
        this.target = target;
        this.id = origin.getId() + "-" + target.getId();
        this.weight = weight;
        setPoints();
    }

    public Link reverse(){
        return new Link(this.target, this.origin, this.weight);
    }

    private void setPoints(){
        Point originPoint = origin.getPoint();
        Point targetPoint = target.getPoint();
        int originX = (int)originPoint.getX();
        int originY = (int)originPoint.getY();
        int targetX = (int)targetPoint.getX();
        int targetY = (int)targetPoint.getY();

        Long numDivisions = weight / 10;
        double xStep = 1.0 * Math.abs(originX - targetX) / numDivisions;
        double yStep = 1.0 * Math.abs(originY - targetY) / numDivisions;

        points = new HashSet<>();
        points.add(originPoint);

        double oldX = originX;
        double oldY = originY;
        for(long i = 0; i < numDivisions; i++){
            double newX = oldX;
            double newY = oldY;
            if(oldX < targetX){
                newX += Math.min(xStep, targetX - oldX);
            } else if(oldX > targetX){
                newX -= Math.min(xStep, oldX - targetX);
            }
            if(oldY < targetY){
                newY += Math.min(yStep, targetY - oldY);
            } else if(oldY > targetY){
                newY -= Math.min(yStep, oldY - targetY);
            }
            Point newPoint = new Point();
            newPoint.setLocation(newX, newY);
            points.add(newPoint);
            oldX = newX;
            oldY = newY;
        }
        points.add(targetPoint);
    }
}
