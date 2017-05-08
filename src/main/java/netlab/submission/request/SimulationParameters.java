package netlab.submission.request;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationParameters {

    @NonNull
    private Long seed;

    @NonNull
    private String topologyId;

    @NonNull
    private Integer numRequests;

    @NonNull
    private String problemClass;

    @NonNull
    private String algorithm;

    // S
    @NonNull
    private Integer numSources;
    // D
    @NonNull
    private Integer numDestinations;

    // F - Total number of possible failures
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

    // Number of failures that will occur
    private Integer numFails;

    // Alternatively, give a random number between values at index 0 and index 1
    private List<Integer> minMaxFails;

    private String processingType;

    private Double percentSrcAlsoDest;

    private Double percentSrcFail;

    private Double percentDestFail;

    private Boolean sdn;

    private Boolean useAws;


}
