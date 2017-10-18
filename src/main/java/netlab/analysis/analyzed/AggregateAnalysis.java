package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateAnalysis implements Serializable {

    private List<String> requestSetIds;
    private List<Long> seeds;
    private ProblemClass problemClass;
    private Algorithm algorithm;
    private Objective objective;
    private FailureClass failureClass;

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

    private Double pairAvgPaths;
    private List<Double> pairAvgPathsConfInterval;

    private Double pairAvgPathLength;
    private List<Double> pairAvgPathLengthConfInterval;

    private Double pairAvgPathCost;
    private List<Double> pairAvgPathCostConfInterval;

    private Double pairAvgDisconnectedPaths;
    private List<Double> pairAvgDisconnectedPathsConfInterval;

    private Double pairAvgPathsPerChosen;
    private List<Double> pairAvgPathsPerChosenConfInterval;

    private Double pairAvgPathLengthPerChosen;
    private List<Double> pairAvgPathLengthPerChosenConfInterval;

    private Double pairAvgPathCostPerChosen;
    private List<Double> pairAvgPathCostPerChosenConfInterval;

    private Double pairAvgDisconnectedPathsPerChosen;
    private List<Double> pairAvgDisconnectedPathsPerChosenConfInterval;

    private Double srcAvgPaths;
    private List<Double> srcAvgPathsConfInterval;

    private Double srcAvgPathLength;
    private List<Double> srcAvgPathLengthConfInterval;

    private Double srcAvgPathCost;
    private List<Double> srcAvgPathCostConfInterval;

    private Double srcAvgDisconnectedPaths;
    private List<Double> srcAvgDisconnectedPathsConfInterval;

    private Double srcAvgPathsPerChosen;
    private List<Double> srcAvgPathsPerChosenConfInterval;

    private Double srcAvgPathLengthPerChosen;
    private List<Double> srcAvgPathLengthPerChosenConfInterval;

    private Double srcAvgPathCostPerChosen;
    private List<Double> srcAvgPathCostPerChosenConfInterval;

    private Double srcAvgDisconnectedPathsPerChosen;
    private List<Double> srcAvgDisconnectedPathsPerChosenConfInterval;

    private Double dstAvgPaths;
    private List<Double> dstAvgPathsConfInterval;

    private Double dstAvgPathLength;
    private List<Double> dstAvgPathLengthConfInterval;

    private Double dstAvgPathCost;
    private List<Double> dstAvgPathCostConfInterval;

    private Double dstAvgDisconnectedPaths;
    private List<Double> dstAvgDisconnectedPathsConfInterval;

    private Double dstAvgPathsPerChosen;
    private List<Double> dstAvgPathsPerChosenConfInterval;

    private Double dstAvgPathLengthPerChosen;
    private List<Double> dstAvgPathLengthPerChosenConfInterval;

    private Double dstAvgPathCostPerChosen;
    private List<Double> dstAvgPathCostPerChosenConfInterval;

    private Double dstAvgDisconnectedPathsPerChosen;
    private List<Double> dstAvgDisconnectedPathsPerChosenConfInterval;

}
