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
    private String topologyId;
    private Algorithm algorithm;
    private RoutingType routingType;
    private FailureScenario failureScenario;
    private Integer numFailuresEvents;
    private TrafficCombinationType trafficCombinationType;
    private RoutingDescription routingDescription;
    private Boolean ignoreFailures;

    private Boolean isFeasible;
    private Double runningTime;
    private Double totalCost;
    private Double totalLinksUsed;
    private Double totalPaths;
    private Double totalPrimaryPaths;
    private Double totalBackupPaths;
    private Double connectionsSevered;
    private Double connectionsIntact;
    private Double pathsIntact;
    private Double pathsSevered;
    private Double primaryPathsIntact;
    private Double primaryPathsSevered;
    private Double destsConnected;

    private Double averagePrimaryHops;
    private Double averagePrimaryCost;
    private Double averagePrimaryRisk;
    private Double averageBackupHops;
    private Double averageBackupCost;
    private Double averageBackupRisk;
    private Double averageBackupPaths;
    private Double averagePrimaryHopsPostFailure;
    private Double averagePrimaryCostPostFailure;
    private Double averageBackupPathsIntact;
    private Double averageBackupPathsSevered;

    private Double primaryIntactPerSrc;
    private Double destsConnectedPerSrc;
    private Double numFoundBackup;

    private List<String> chosenFailures;
    private List<CachingResult> cachingResults;


    public String toString(){
        String output = "ID: " + requestId + "\n" +
                "Seed: " + seed + "\n" +
                "Algorithm: " + algorithm.getCode() + "\n" +
                "Routing Type: " + routingType.getCode() + "\n" +
                "Routing Description: " + routingDescription + "\n" +
                "Traffic Combination Type: " + trafficCombinationType.getCode() + "\n" +
                "Failure Scenario: " + failureScenario.getCode() + "\n" +
                "NFE: " + numFailuresEvents + "\n" +
                "---Metrics---" + "\n" +
                "Feasible: " + isFeasible + "\n" +
                "Running Time: " + runningTime + "\n" +
                "Total Cost: " + totalCost + "\n" +
                "Total Links Used: " + totalLinksUsed + "\n" +
                "Total Paths: " + totalPaths + "\n" +
                "Total Primary Paths: " + totalPrimaryPaths + "\n" +
                "Total Backup Paths: " + totalBackupPaths + "\n" +
                "Avg Backup Paths: " + averageBackupPaths + "\n" +
                "Avg Primary Hops: " + averagePrimaryHops + "\n" +
                "Avg Primary Cost: " + averagePrimaryCost + "\n" +
                "Avg Primary Path Risk: " + averagePrimaryRisk + "\n" +
                "Avg Backup Hops: " + averageBackupHops + "\n" +
                "Avg Backup Cost: " + averageBackupCost + "\n" +
                "Avg Backup Path Risk: " + averageBackupRisk + "\n" +
                "Dests Connected: " + destsConnected + "\n" +
                "Dests Connected Per Src: " + destsConnectedPerSrc + "\n" +
                "-Post Failure-" + "\n" +
                "Num Found Backup: " + numFoundBackup + "\n" +
                "PF Avg Primary Hops: " + averagePrimaryHopsPostFailure + "\n" +
                "PF Avg Primary Cost: " + averagePrimaryCostPostFailure + "\n" +
                "Connections Intact: " + connectionsIntact + "\n" +
                "Connections Severed: " + connectionsSevered + "\n" +
                "Paths Intact: " + pathsIntact + "\n" +
                "Paths Severed: " + pathsSevered + "\n" +
                "Primary Paths Intact: " + primaryPathsIntact + "\n" +
                "Primary Paths Severed: " + primaryPathsSevered + "\n" +
                "Primary Paths Intact Per Src: " + primaryIntactPerSrc + "\n" +
                "Avg Backup Paths Intact: " + averageBackupPathsIntact + "\n" +
                "Avg Backup Paths Severed: " + averageBackupPathsSevered + "\n" +
                "Chosen Failures: " + chosenFailures.toString() + "\n";
        for(CachingResult result : cachingResults){
            output += "Caching: " + result.toString() + "\n";
        }
        return output;
    }
}
