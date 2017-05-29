package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.analysis.enums.MemberType;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;

import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestMetrics {

    private Long requestId;
    private Boolean isSurvivable;
    private Boolean isFeasible;
    private Double runningTimeSeconds;
    private Integer numLinksUsed;
    private Integer costLinksUsed;
    private Integer numPaths;
    private Integer numDisconnectedPaths;
    private Integer numIntactPaths;
    private Double avgPathLength;
    private Double avgPathCost;
    private Double avgPathsPerPair;
    private Double avgPathsPerSrc;
    private Double avgPathsPerDst;
    private Double avgDisconnectedPathsPerPair;
    private Double avgDisconnectedPathsPerSrc;
    private Double avgDisconnectedPathsPerDst;
    private Double avgPathsPerPairChosen;
    private Double avgPathsPerSrcChosen;
    private Double avgPathsPerDstChosen;
    private Double avgDisconnectedPathsPerPairChosen;
    private Double avgDisconnectedPathsPerSrcChosen;
    private Double avgDisconnectedPathsPerDstChosen;

    private Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap;
    private Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap;
}
