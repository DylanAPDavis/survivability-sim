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

    private Double totalRunningTimeSeconds;
    private Double avgRunningTimeSeconds;

    private Integer totalSurvivable;
    private Double percentSurvivable;

    private Integer totalFeasible;
    private Double percentFeasible;

    private Integer totalFeasibleAndSurvivable;

    private Integer totalLinksUsed;
    private Double avgLinksUsed;

    private Integer totalCostLinksUsed;
    private Double avgCostLinksUsed;

    private Integer totalNumPaths;
    private Double avgNumPaths;

    private Integer totalDisconnectedPaths;
    private Double avgDisconnectedPaths;

    private Integer totalIntactPaths;
    private Double avgIntactPaths;

    private Double avgAvgPathLength;
    private Double avgAvgPathCost;
    private Double avgAvgPathsPerPair;
    private Double avgAvgPathsPerSrc;
    private Double avgAvgPathsPerDst;
    private Double avgAvgDisconnectedPathsPerPair;
    private Double avgAvgDisconnectedPathsPerSrc;
    private Double avgAvgDisconnectedPathsPerDst;
    private Double avgAvgPathsPerPairChosen;
    private Double avgAvgPathsPerSrcChosen;
    private Double avgAvgPathsPerDstChosen;
    private Double avgAvgDisconnectedPathsPerPairChosen;
    private Double avgAvgDisconnectedPathsPerSrcChosen;
    private Double avgAvgDisconnectedPathsPerDstChosen;
}
