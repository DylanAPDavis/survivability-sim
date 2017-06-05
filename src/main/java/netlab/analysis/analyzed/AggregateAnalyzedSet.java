package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.ProblemClass;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateAnalyzedSet implements Serializable {

    private List<String> requestSetIds;
    private List<Long> seeds;
    private ProblemClass problemClass;
    private Algorithm algorithm;

    private Integer numRequests;

    private Double totalRunningTimeSeconds;
    private List<Double> totalRunningTimeSecondsConfInterval;

    private Double totalRunningTimeSecondsForFeasible;
    private List<Double> totalRunningTimeSecondsForFeasibleConfInterval;

    private Double avgRunningTimeSeconds;
    private List<Double> avgRunningTimeSecondsConfInterval;

    private Double avgRunningTimeSecondsForFeasible;
    private List<Double> avgRunningTimeSecondsForFeasibleConfInterval;

    private Double totalSurvivable;
    private List<Double> totalSurvivableConfInterval;

    private Double percentSurvivable;
    private List<Double> percentSurvivableConfInterval;

    private Double percentSurvivableForFeasible;
    private List<Double> percentSurvivableForFeasibleConfInterval;

    private Double totalFeasible;
    private List<Double> totalFeasibleConfInterval;

    private Double percentFeasible;
    private List<Double> percentFeasibleConfInterval;

    private Double totalFeasibleAndSurvivable;
    private List<Double> totalFeasibleAndSurvivableConfInterval;

    private Double totalLinksUsed;
    private List<Double> totalLinksUsedConfInterval;

    private Double avgLinksUsedForFeasible;
    private List<Double> avgLinksUsedForFeasibleConfInterval;

    private Double totalCostLinksUsed;
    private List<Double> totalCostLinksUsedConfInterval;

    private Double avgCostLinksUsedForFeasible;
    private List<Double> avgCostLinksUsedForFeasibleConfInterval;

    private Double totalNumPaths;
    private List<Double> totalNumPathsConfInterval;

    private Double avgNumPathsForFeasible;
    private List<Double> avgNumPathsForFeasibleConfInterval;

    private Double totalDisconnectedPaths;
    private List<Double> totalDisconnectedPathsConfInterval;

    private Double avgDisconnectedPathsForFeasible;
    private List<Double> avgDisconnectedPathsForFeasibleConfInterval;

    private Double totalIntactPaths;
    private List<Double> totalIntactPathsConfInterval;

    private Double avgIntactPathsForFeasible;
    private List<Double> avgIntactPathsForFeasibleConfInterval;

    private Double avgAvgPathLength;
    private List<Double> avgAvgPathLengthConfInterval;

    private Double avgAvgPathCost;
    private List<Double> avgAvgPathCostConfInterval;

    private AggregateAverages avgAveragesPerPair;
    private AggregateAverages avgAveragesPerSrc;
    private AggregateAverages avgAveragesPerDst;

}
