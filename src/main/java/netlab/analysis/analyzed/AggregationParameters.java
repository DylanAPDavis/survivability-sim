package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.Objective;
import netlab.submission.request.SimulationParameters;
import netlab.topology.elements.Topology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationParameters {

    private List<Long> seeds;
    private List<Topology> topologies;
    private List<Objective> objectives;

    private List<FailureDescription> failureDescriptions;
    private List<RoutingDescription> routingDescriptions;


    public List<Integer> numThreads;
}
