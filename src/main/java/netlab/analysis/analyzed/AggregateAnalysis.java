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
    private List<Long> seeds;
    private Algorithm algorithm;
    private RoutingType routingType;
    private Objective objective;
    private FailureScenario failureScenario;
    private FailureClass failureClass;
    private Integer numFailuresEvents;
    private TrafficCombinationType trafficCombinationType;

    private Integer numRequests;

    private Double totalFeasible;
    private Double percentFeasible;

    private Double runningTime;
    private Double totalCost;
    private Double totalLinksUsed;
    private Double totalPaths;
    private Double averagePrimaryHops;
    private Double averagePrimaryCost;

    // After failure simulation
    private Double averagePrimaryHopsPostFailure;
    private Double averagePrimaryCostPostFailure;
    private Double pathsSevered;
    private Double pathsIntact;
    private Double connectionsSevered;
    private Double connectionsIntact;

}
