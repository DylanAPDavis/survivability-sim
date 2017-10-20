package netlab.submission.request;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.*;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName="TestsMeta")
public class SimulationParameters {

    @DynamoDBHashKey(attributeName="requestId")
    private String requestId;

    @DynamoDBIndexRangeKey(attributeName="seed", localSecondaryIndexName="seed-index")
    private Long seed;

    private Calendar submittedDate;
    private Boolean completed;

    private String topologyId;
    private String problemClass;
    private String objective;
    private String algorithm;
    // S
    private Integer numSources;
    // D
    private Integer numDestinations;

    // F - Total size of the failure set (shared by all connections)
    private Integer failureSetSize;
    // Type of elements in F (Link, Node, or Both)
    private String failureClass;
    // Failure probability -- apply to all elements
    private Double failureProb;
    // Failure Scenario -- apply unique probability to each element of a unique failure set
    private String failureScenario;
    // Number of failureSet that will occur
    private Integer numFailureEvents;

    // C - total number of connections
    private Integer minConnections;
    // Establish min and max connections between each SD pair
    private Integer minPairConnections;
    private Integer maxPairConnections;
    // Establish min and max connections from each source and to each destination
    private Integer minSrcConnections;
    private Integer maxSrcConnections;
    private Integer minDstConnections;
    private Integer maxDstConnections;

    private Integer useMinS;
    private Integer useMaxS;
    private Integer useMinD;
    private Integer useMaxD;

    private Double percentSrcAlsoDest;
    private Double percentSrcFail;
    private Double percentDstFail;

    private Boolean useAws;
    private Boolean ignoreFailures;
    private Boolean combineSrcTraffic;
    private Boolean combineDstTraffic;

    private Integer numThreads;

    public SimulationParameters clone(){
        try{
            return (SimulationParameters) super.clone();
        } catch (Exception e){
            return SimulationParameters.builder()
                    .requestId(requestId)
                    .seed(seed)
                    .submittedDate(submittedDate)
                    .completed(completed)
                    .topologyId(topologyId)
                    .problemClass(problemClass)
                    .objective(objective)
                    .algorithm(algorithm)
                    .numSources(numSources)
                    .numDestinations(numDestinations)
                    .failureSetSize(failureSetSize)
                    .failureClass(failureClass)
                    .failureProb(failureProb)
                    .failureScenario(failureScenario)
                    .numFailureEvents(numFailureEvents)
                    .minConnections(minConnections)
                    .minPairConnections(minPairConnections)
                    .maxPairConnections(maxPairConnections)
                    .minSrcConnections(minSrcConnections)
                    .maxSrcConnections(maxSrcConnections)
                    .minDstConnections(minDstConnections)
                    .maxDstConnections(maxDstConnections)
                    .useMinS(useMinS)
                    .useMaxS(useMaxS)
                    .useMinD(useMinD)
                    .useMaxD(useMaxD)
                    .percentSrcAlsoDest(percentSrcAlsoDest)
                    .percentSrcFail(percentSrcFail)
                    .percentDstFail(percentDstFail)
                    .useAws(useAws)
                    .ignoreFailures(ignoreFailures)
                    .combineSrcTraffic(combineSrcTraffic)
                    .combineDstTraffic(combineDstTraffic)
                    .numThreads(numThreads)
                    .build();
        }
    }

}
