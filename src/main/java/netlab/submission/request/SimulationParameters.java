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

    // F - pick one field
    @NonNull
    private Integer numFailures;
    @NonNull
    private List<Integer> minMaxFailures;
    @NonNull
    private String failureClass;

    // C - pick one field
    @NonNull
    private Integer numConnections;
    @NonNull
    private List<Integer> minMaxConnections;

    // k - pick one field
    @NonNull
    private Integer numCuts;
    @NonNull
    private List<Integer> minMaxCuts;

    // Failure probability - pick one field
    @NonNull
    private Double failureProb;
    @NonNull
    private List<Double> minMaxFailureProb;

    @NonNull
    private String batchType;

    @NonNull
    private Boolean sdn;

    @NonNull
    private Boolean useAws;

    @NonNull
    private Boolean srcFailuresAllowed;

    @NonNull
    private Boolean dstFailuresAllowed;

    @NonNull
    private Boolean sourceDestCanOverlap;

}
