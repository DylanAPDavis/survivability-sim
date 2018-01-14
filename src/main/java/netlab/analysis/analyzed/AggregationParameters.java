package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.*;
import netlab.submission.request.SimulationParameters;
import netlab.topology.elements.Topology;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationParameters implements Serializable{

    private List<Long> seeds;
    private List<String> topologyIds;
    private List<RoutingType> routingTypes;
    private List<FailureScenario> failureScenarios;
    private List<Integer> nfeValues;
    private Map<RoutingType, List<Algorithm>> algorithmMap;
    private Map<RoutingType, List<RoutingDescription>> routingDescriptionMap;
    private Map<RoutingType, List<TrafficCombinationType>> trafficCombinationTypeMap;
    private Set<Algorithm> algorithmsThatCanIgnoreFailures;
}
