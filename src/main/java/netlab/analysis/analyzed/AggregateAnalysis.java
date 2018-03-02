package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.*;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateAnalysis implements Serializable {

    private List<String> requestSetIds;
    private String hash;
    private List<Long> seeds;
    private String topologyId;
    private Algorithm algorithm;
    private RoutingType routingType;
    private FailureScenario failureScenario;
    private Integer numFailuresEvents;
    private TrafficCombinationType trafficCombinationType;
    private RoutingDescription routingDescription;
    private Boolean ignoreFailures;


    private Double totalFeasible;
    private Double percentFeasible;

    private Double runningTime;
    private Double totalCost;
    private Double totalLinksUsed;
    private Double totalPaths;
    private Double totalPrimaryPaths;
    private Double totalBackupPaths;
    private Double connectionsSevered;
    private Double connectionsIntact;
    private Double pathsSevered;
    private Double pathsIntact;
    private Double primaryPathsSevered;
    private Double primaryPathsIntact;
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

    // Caching
    private List<CachingResult> cachingResults;


}
