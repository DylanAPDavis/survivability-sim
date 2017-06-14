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

    // F - Total size of the failure set (shared by all connections)
    private Set<String> failures;
    private Map<List<String>, Set<String>> pairFailureMap;
    private Map<String, Set<String>> sourceFailureMap;
    private Map<String, Set<String>> destFailureMap;

    // Failure probability - pick one field
    private Map<String, Double> failureProbabilityMap;
    private Map<List<String>, Map<String, Double>> pairFailureProbabilityMap;
    private Map<String, Map<String, Double>> sourceFailureProbabilityMap;
    private Map<String, Map<String, Double>> destFailureProbabilityMap;

    // C - total number of connections
    private Integer numConnections;
    // Alternatively, give a random number between values at index 0 and index 1
    private Map<List<String>, Integer> pairNumConnectionsMap;
    private Map<List<String>, Integer> pairMinNumConnectionsMap;
    private Map<List<String>, Integer> pairMaxNumConnectionsMap;
    private Map<String, Integer> sourceNumConnectionsMap;
    private Map<String, Integer> sourceMinNumConnectionsMap;
    private Map<String, Integer> sourceMaxNumConnectionsMap;
    private Map<String, Integer> destNumConnectionsMap;
    private Map<String, Integer> destMinNumConnectionsMap;
    private Map<String, Integer> destMaxNumConnectionsMap;

    // Number of failureSet that will occur
    private Integer numFailsAllowed;
    // Alternatively, give a random number between values at index 0 and index 1
    private Map<List<String>, Integer> pairNumFailsAllowedMap;
    private Map<String, Integer> sourceNumFailsAllowedMap;
    private Map<String, Integer> destNumFailsAllowedMap;

    private Boolean sdn;

    private Boolean useAws;

    private Boolean ignoreFailures;

}
