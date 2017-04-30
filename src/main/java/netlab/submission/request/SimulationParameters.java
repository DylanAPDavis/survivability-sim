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
    private String algorithm;

    // S
    @NonNull
    private Integer numSources;
    // D
    @NonNull
    private Integer numDestinations;

    // F
    private Integer numFailures;
    private List<Integer> minMaxFailures;

    private String failureClass;

    // C
    private Integer numConnections;
    private List<Integer> minConnectionsRange;
    private List<Integer> maxConnectionsRange;

    // k - pick one field
    private Integer numCuts;
    private List<Integer> minMaxCuts;

    // Failure probability - pick one field
    private Double failureProb;
    private List<Double> minMaxFailureProb;

    private String processingType;

    private Boolean sdn;

    private Boolean useAws;

    private Boolean srcFailuresAllowed;

    private Boolean dstFailuresAllowed;

    private String srcDstOverlap;

}
