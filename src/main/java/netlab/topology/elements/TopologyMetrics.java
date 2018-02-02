package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopologyMetrics implements Serializable {

    String topologyId;
    Map<String, Path> pathIdMap;
    Map<SourceDestPair, List<String>> minCostPaths;
    Map<SourceDestPair, List<String>> linkDisjointPaths;
    Map<SourceDestPair, List<String>> nodeDisjointPaths;
    //Map<String, Set<String>> notLinkDisjointFromPath;
    //Map<String, Set<String>> notNodeDisjointFromPath;
}
