package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathSetMetrics implements Serializable {

    private Map<String, PathMetrics> pathMetricsMap;
    private Integer numFailed;
    private Integer numLinkUsages;
    private Integer numPaths;
    private Long totalLinkCost;
    private Boolean chosen;
}
