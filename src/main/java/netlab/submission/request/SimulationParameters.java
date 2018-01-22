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
    public String requestId;

    @DynamoDBIndexRangeKey(attributeName="seed", localSecondaryIndexName="seed-index")
    public Long seed;

    public Calendar submittedDate;
    public Boolean completed;

    public String topologyId;
    public String problemClass;
    public String objective;
    public String algorithm;

    // Can be used to override other connection parameters
    public String routingType;
    // S
    public Integer numSources;
    // D
    public Integer numDestinations;

    // F - Total size of the failure set (shared by all connections)
    public Integer failureSetSize;
    // Type of elements in F (Link, Node, or Both)
    public String failureClass;
    // Failure probability -- apply to all elements
    public Double failureProb;
    // Failure Scenario -- apply unique probability to each element of a unique failure set
    public String failureScenario;
    // Number of failureSet that will occur
    public Integer numFailureEvents;

    // C - total number of connections
    public Integer minConnections;
    // Establish min and max connections between each SD pair
    public Integer minPairConnections;
    public Integer maxPairConnections;
    // Establish min and max connections from each source and to each destination
    public Integer minSrcConnections;
    public Integer maxSrcConnections;
    public Integer minDstConnections;
    public Integer maxDstConnections;

    public Integer useMinS;
    public Integer useMaxS;
    public Integer useMinD;
    public Integer useMaxD;

    public String trafficCombinationType;

    public String sourceSubsetDestType;
    public String sourceFailureType;
    public String destFailureType;

    public Boolean useAws;
    public Boolean ignoreFailures;

    public Integer numThreads;

    public Integer cutoffTimeSeconds;
    public Boolean timedOut;

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
                    .sourceSubsetDestType(sourceSubsetDestType)
                    .sourceFailureType(sourceFailureType)
                    .destFailureType(destFailureType)
                    .useAws(useAws)
                    .ignoreFailures(ignoreFailures)
                    .trafficCombinationType(trafficCombinationType)
                    .numThreads(numThreads)
                    .cutoffTimeSeconds(cutoffTimeSeconds)
                    .timedOut(timedOut)
                    .build();
        }
    }

}
