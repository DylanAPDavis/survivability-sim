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

    private Boolean requestIsSurvivable;
    private Boolean isFeasible;
    private Integer numLinkUsages;
    private Integer numFailed;
    private Integer numPaths;

    private Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap;
    private Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap;
}
