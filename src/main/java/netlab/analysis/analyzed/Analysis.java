package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.analysis.enums.MemberType;
import netlab.submission.enums.*;
import netlab.submission.request.NumFailureEvents;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analysis implements Serializable {

    private String requestId;
    private Long seed;
    private Algorithm algorithm;
    private RoutingType routingType;
    private Objective objective;
    private FailureScenario failureScenario;
    private FailureClass failureClass;
    private Integer numFailuresEvents;
    private TrafficCombinationType trafficCombinationType;

    private Boolean isFeasible;
    private Double runningTime;
    private Double totalCost;
    private Double totalLinksUsed;
    private Double totalPaths;
    private Double averagePrimaryHops;
    private Double averagePrimaryCost;

    private Double averagePathRisk;
    private Double averagePathRiskPerPair;
    private Double averageMinRiskPerPair;
    private Double averageMaxRiskPerPair;

    // After failure simulation
    private List<String> chosenFailures;
    private Double averagePrimaryHopsPostFailure;
    private Double averagePrimaryCostPostFailure;
    private Double pathsSevered;
    private Double pathsIntact;
    private Double connectionsSevered;
    private Double connectionsIntact;

    private List<CachingResult> cachingResults;


    public String toString(){
        String output = "ID: " + requestId + "\n" +
                "Seed: " + seed + "\n" +
                "Algorithm: " + algorithm.getCode() + "\n" +
                "Routing Type: " + routingType.getCode() + "\n" +
                "Traffic Combination Type: " + trafficCombinationType.getCode() + "\n" +
                "Objective: " + objective.getCode() + "\n" +
                "Failure Scenario: " + failureScenario.getCode() + "\n" +
                "Failure Class: " + failureClass.getCode() + "\n" +
                "NFE: " + numFailuresEvents + "\n" +
                "---Metrics---" + "\n" +
                "Feasible: " + isFeasible + "\n" +
                "Running Time: " + runningTime + "\n" +
                "Total Cost: " + totalCost + "\n" +
                "Total Links Used: " + totalLinksUsed + "\n" +
                "Total Paths: " + totalPaths + "\n" +
                "Avg Primary Hops: " + averagePrimaryHops + "\n" +
                "Avg Primary Cost: " + averagePrimaryCost + "\n" +
                "-Risk-" + "\n" +
                "Avg Path Risk: " + averagePathRisk + "\n" +
                "Avg Path Risk Per Pair: " + averagePathRiskPerPair + "\n" +
                "Avg Min Path Risk Per Pair: " + averageMinRiskPerPair + "\n" +
                "Avg Max Path Risk Per Pair: " + averageMaxRiskPerPair + "\n" +
                "-Post Failure-" + "\n" +
                "PF Avg Primary Hops: " + averagePrimaryHopsPostFailure + "\n" +
                "PF Avg Primary Cost: " + averagePrimaryCostPostFailure + "\n" +
                "Paths Intact: " + pathsIntact + "\n" +
                "Paths Severed: " + pathsSevered + "\n" +
                "Connections Intact: " + connectionsIntact + "\n" +
                "Connections Severed: " + connectionsSevered + "\n" +
                "Chosen Failures: " + chosenFailures.toString() + "\n";
        for(CachingResult result : cachingResults){
            output += "Caching: " + result.toString() + "\n";
        }
        return output;
    }
}
