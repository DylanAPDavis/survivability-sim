package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.analysis.enums.MemberType;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;
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
    private ProblemClass problemClass;
    private Algorithm algorithm;
    private Objective objective;
    private FailureClass failureClass;

    private Boolean isFeasible;
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

    /*
    private Integer numLinksUsed;
    private Long costLinksUsed;
    private Integer numPaths;
    private Integer numDisconnectedPaths;
    private Integer numIntactPaths;
    private Double avgPathLength;
    private Double avgPathCost;

    private Averages averagesPerPair;
    private Averages averagesPerSrc;
    private Averages averagesPerDst;

    private Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap;
    private Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap;
    */
}
