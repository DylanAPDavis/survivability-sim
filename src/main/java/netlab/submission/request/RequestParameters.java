package netlab.submission.request;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestParameters {

    private String topologyId;

    // S
    private Set<String> sources;
    // D
    private Set<String> destinations;

    private String problemClass;

    private String objective;

    private String algorithm;

    // F - Failure set (shared by all connections)
    private Set<String> failures;

    // Failure probability map
    private Map<String, Double> failureProbabilityMap;

    // Number of failures that will occur
    private Integer numFailureEvents;

    // C - total number of connections
    private Integer numConnections;

    private Map<List<String>, Integer> pairNumConnectionsMap;
    private Map<List<String>, Integer> pairMinNumConnectionsMap;
    private Map<List<String>, Integer> pairMaxNumConnectionsMap;

    private Map<String, Integer> sourceNumConnectionsMap;
    private Map<String, Integer> sourceMinNumConnectionsMap;
    private Map<String, Integer> sourceMaxNumConnectionsMap;

    private Map<String, Integer> destNumConnectionsMap;
    private Map<String, Integer> destMinNumConnectionsMap;
    private Map<String, Integer> destMaxNumConnectionsMap;

    private Integer useMinS;
    private Integer useMaxS;
    private Integer useMinD;
    private Integer useMaxD;

    public String trafficCombinationType;

    private Boolean ignoreFailures;
    private Integer numThreads;

}
