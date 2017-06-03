package netlab.submission.request;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationParameters {

    private Long seed;

    private String topologyId;

    private String problemClass;

    private String objective;

    private String algorithm;

    private Integer numRequests;

    // S
    private Integer numSources;
    // D
    private Integer numDestinations;

    // F - Total size of the failure set (shared by all connections)
    private Integer failureSetSize;
    // Alternatively, give a random number between values at index 0 and index 1
    private List<Integer> minMaxFailures;
    private String failureClass;
    // Failure probability - pick one field
    private Double failureProb;
    private List<Double> minMaxFailureProb;

    // C - total number of connections
    private Integer numConnections;
    // Alternatively, give a random number between values at index 0 and index 1
    private List<Integer> minConnectionsRange;
    private List<Integer> maxConnectionsRange;

    // Number of failureSet that will occur
    private Integer numFailsAllowed;
    // Alternatively, give a random number between values at index 0 and index 1
    private List<Integer> minMaxFailsAllowed;

    private String processingType;

    private Double percentSrcAlsoDest;

    private Double percentSrcFail;

    private Double percentDestFail;

    private Boolean sdn;

    private Boolean useAws;

}
