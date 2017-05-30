package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.ProblemClass;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzedSet {

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

    private Averages avgAveragesPerPair;
    private Averages avgAveragesPerSrc;
    private Averages avgAveragesPerDst;

}
