package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.topology.elements.SourceDestPair;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestMetrics {

    private Boolean requestIsSurvivable;
    private Integer numLinkUsages;
    private Integer numFailed;
    private Integer numPaths;

    private Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap;
}
