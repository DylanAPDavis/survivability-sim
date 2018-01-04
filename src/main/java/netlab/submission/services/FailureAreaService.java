package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.FailureScenario;
import netlab.submission.request.FailureArea;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.*;
import java.util.List;

@Service
@Slf4j
public class FailureAreaService {

    private Map<String, FailureArea> failureAreaMap;

    public FailureAreaService(){
        failureAreaMap = new HashMap<>();
        FailureArea quake1 = new FailureArea(250, 500, 1500, false);
        FailureArea quake2 = new FailureArea(2200, 800, 1000, false);
        FailureArea quake3 = new FailureArea(3700, 700, 500, false);
        FailureArea nuke1 = new FailureArea(250, 500, 1500, true);
        FailureArea nuke2 = new FailureArea(2200, 800, 1000, true);
        FailureArea nuke3 = new FailureArea(3700, 700, 500, true);
        failureAreaMap.put("quake1", quake1);
        failureAreaMap.put("quake2", quake2);
        failureAreaMap.put("quake3", quake3);
        failureAreaMap.put("nuke1", nuke1);
        failureAreaMap.put("nuke2", nuke2);
        failureAreaMap.put("nuke3", nuke3);
    }


    public Set<Failure> generateFailures(FailureScenario failScenario, Set<Node> nodeOptions, Set<Link> links, FailureClass failureClass) {

        List<FailureArea> failureAreas = new ArrayList<>();
        switch(failScenario){
            case Quake_1:
                failureAreas.add(failureAreaMap.get("quake1"));
                break;
            case Quake_2:
                failureAreas.add(failureAreaMap.get("quake2"));
                break;
            case Quake_3:
                failureAreas.add(failureAreaMap.get("quake3"));
                break;
            case Quake_1_2:
                failureAreas.add(failureAreaMap.get("quake1"));
                failureAreas.add(failureAreaMap.get("quake2"));
                break;
            case Quake_1_3:
                failureAreas.add(failureAreaMap.get("quake1"));
                failureAreas.add(failureAreaMap.get("quake3"));
                break;
            case Quake_2_3:
                failureAreas.add(failureAreaMap.get("quake2"));
                failureAreas.add(failureAreaMap.get("quake3"));
                break;
            case Quake_1_2_3:
                failureAreas.add(failureAreaMap.get("quake1"));
                failureAreas.add(failureAreaMap.get("quake2"));
                failureAreas.add(failureAreaMap.get("quake3"));
                break;
            case Nuke_1:
                failureAreas.add(failureAreaMap.get("nuke1"));
                break;
            case Nuke_2:
                failureAreas.add(failureAreaMap.get("nuke2"));
                break;
            case Nuke_3:
                failureAreas.add(failureAreaMap.get("nuke3"));
                break;
            case Nuke_1_2:
                failureAreas.add(failureAreaMap.get("nuke1"));
                failureAreas.add(failureAreaMap.get("nuke2"));
                break;
            case Nuke_1_3:
                failureAreas.add(failureAreaMap.get("nuke1"));
                failureAreas.add(failureAreaMap.get("nuke3"));
                break;
            case Nuke_2_3:
                failureAreas.add(failureAreaMap.get("nuke2"));
                failureAreas.add(failureAreaMap.get("nuke3"));
                break;
            case Nuke_1_2_3:
                failureAreas.add(failureAreaMap.get("nuke1"));
                failureAreas.add(failureAreaMap.get("nuke2"));
                failureAreas.add(failureAreaMap.get("nuke3"));
                break;
        }
        return determineFailuresBasedOnArea(failureAreas, nodeOptions, links, failureClass);
    }

    private Set<Failure> determineFailuresBasedOnArea(List<FailureArea> failureAreas, Set<Node> nodes,
                                                      Set<Link> links, FailureClass failureClass) {
        Set<Failure> failures = new HashSet<>();
        if(failureClass.equals(FailureClass.Both)){
            failures.addAll(determineNodeFailures(failureAreas, nodes));
            failures.addAll(determineLinkFailures(failureAreas, links));
        } else if(failureClass.equals(FailureClass.Node)){
            failures.addAll(determineNodeFailures(failureAreas, nodes));
        } else if(failureClass.equals(FailureClass.Link)){
            failures.addAll(determineLinkFailures(failureAreas, links));
        }
        return failures;
    }

    private Set<Failure> determineNodeFailures(List<FailureArea> failureAreas, Set<Node> nodes){
        Set<Failure> failures = new HashSet<>();
        // Check each node
        for(Node node : nodes){
            Point nodePoint = node.getPoint();
            // For now, take the maximum weight from any failure area
            double max = 0.0;
            for(FailureArea fa : failureAreas){
                double distance = fa.getCenter().distance(nodePoint);
                double weight = Math.max(0.0, 1.0 - (distance)/fa.getRadius());
                // If you're not using distance based weighting, then the weight is 1.0 (node will fail)
                if(weight > 0.0 && fa.getMustFail()){
                    weight = 1.0;
                }
                if(weight > max){
                    max = weight;
                }
            }
            if(max > 0.0){
                Failure failure = new Failure(node, null, max);
                failures.add(failure);
            }
        }
        return failures;
    }

    private Set<Failure> determineLinkFailures(List<FailureArea> failureAreas, Set<Link> links){
        Set<Failure> failures = new HashSet<>();
        // Check each link
        for(Link link : links){
            Set<Point> points = link.getPoints();
            double max = 0.0;
            // Check each failure area
            for(FailureArea fa : failureAreas){
                Point center = fa.getCenter();
                // Get all of the distances from center to link points
                // Calculate the failure weights
                double runningProb = 1.0;
                for(Point linkPoint : points){
                    double distance = center.distance(linkPoint);
                    double weight = Math.max(0.0, 1.0 - (distance)/fa.getRadius());
                    runningProb *= (1 - weight);
                }
                double compoundWeight = 1.0 - runningProb;
                // If you're not using distance based weighting, then the weight is 1.0 (link will fail)
                if(compoundWeight > 0.0 && fa.getMustFail()){
                    compoundWeight = 1.0;
                }
                if(compoundWeight > max){
                    max = compoundWeight;
                }
            }
            if(max > 0.0){
                Failure failure = new Failure(null, link, max);
                failures.add(failure);
            }
        }
        return failures;
    }
}
