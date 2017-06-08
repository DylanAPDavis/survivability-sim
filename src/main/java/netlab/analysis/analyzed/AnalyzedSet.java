package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.ProblemClass;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzedSet implements Serializable {

    private String requestSetId;
    private Long seed;
    private ProblemClass problemClass;
    private Algorithm algorithm;

    private Map<String, RequestMetrics> requestMetrics;

    private Integer numRequests;

    private Double totalRunningTimeSeconds;
    private Double totalRunningTimeSecondsForFeasible;
    private Double avgRunningTimeSeconds;
    private Double avgRunningTimeSecondsForFeasible;

    private Integer totalSurvivable;
    private Double percentSurvivable;
    private Double percentSurvivableForFeasible;

    private Integer totalFeasible;
    private Double percentFeasible;

    private Integer totalFeasibleAndSurvivable;

    private Integer totalLinksUsed;
    private Double avgLinksUsedForFeasible;

    private Long totalCostLinksUsed;
    private Double avgCostLinksUsedForFeasible;

    private Integer totalNumPaths;
    private Double avgNumPathsForFeasible;

    private Integer totalDisconnectedPaths;
    private Double avgDisconnectedPathsForFeasible;

    private Integer totalIntactPaths;
    private Double avgIntactPathsForFeasible;

    private Double avgAvgPathLength;
    private Double avgAvgPathCost;

    private Double pairAvgPaths;
    private Double pairAvgPathLength;
    private Double pairAvgPathCost;
    private Double pairAvgDisconnectedPaths;
    private Double pairAvgPathsPerChosen;
    private Double pairAvgPathLengthPerChosen;
    private Double pairAvgPathCostPerChosen;
    private Double pairAvgDisconnectedPathsPerChosen;

    private Double srcAvgPaths;
    private Double srcAvgPathLength;
    private Double srcAvgPathCost;
    private Double srcAvgDisconnectedPaths;
    private Double srcAvgPathsPerChosen;
    private Double srcAvgPathLengthPerChosen;
    private Double srcAvgPathCostPerChosen;
    private Double srcAvgDisconnectedPathsPerChosen;

    private Double dstAvgPaths;
    private Double dstAvgPathLength;
    private Double dstAvgPathCost;
    private Double dstAvgDisconnectedPaths;
    private Double dstAvgPathsPerChosen;
    private Double dstAvgPathLengthPerChosen;
    private Double dstAvgPathCostPerChosen;
    private Double dstAvgDisconnectedPathsPerChosen;
    

}
