package netlab.submission.request;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.*;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName="TestsMeta")
public class SimulationParameters {

    @DynamoDBHashKey(attributeName="requestSetId")
    private String requestSetId;

    private Long seed;

    private Calendar submittedDate;

    private Boolean completed;

    // Keeps track of if the request was generated using failure set of another request
    private Boolean generated;

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

    private Boolean ignoreFailures;

}
