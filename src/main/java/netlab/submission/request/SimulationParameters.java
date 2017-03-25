package netlab.submission.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationParameters {

    private Long simulationId;

    private Long seed;

    private String topologyId;
    private Integer numRequests;
    private String algorithm;

    // S
    private Integer numSources;
    // D
    private Integer numDestinations;

    // F - pick one field
    private Integer numFailures;
    private Map<String, Map<String, Integer>> numFailuresPairs;
    private List<Integer> minMaxFailures;

    // C - pick one field
    private Integer numConnections;
    private Map<String, Map<String, Integer>> numConnectionsPairs;
    private List<Integer> minMaxConnections;

    // k - pick one field
    private Integer numCuts;
    private Map<String, Map<String, Integer>> numCutsPairs;
    private List<Integer> minMaxCuts;

    // Failure probability - pick one field
    private Double failureProb;
    private Map<String, Map<String, Double>> failureProbPairs;
    private List<Double> minMaxFailureProb;

    private boolean batchProcessing;

    private String batchType;

    private boolean sdn;

    private boolean useAws;

    private boolean srcFailuresAllowed;

    private Integer numSrcFailures;

    private boolean dstFailuresAllowed;

    private Integer numDstFailures;

    private boolean sourceDestOverlap;

}
