package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.analysis.enums.MemberType;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestMetrics implements Serializable {

    private String requestId;
    private Boolean isSurvivable;
    private Boolean isFeasible;
    private Double runningTimeSeconds;
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
}
