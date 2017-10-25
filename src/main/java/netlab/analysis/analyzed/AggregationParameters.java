package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.request.SimulationParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationParameters {

    private List<Long> seeds;
    private List<String> topologies;
    private List<String> problemClasses;
    private List<String> objectives;
    private List<String> algorithms;
    private List<String> numSources;
    private List<String> numDestinations;
    private List<String> failureSetSizes;
    private List<String> failureProbs;


    // Type of elements in F (Link, Node, or Both)
    public String failureClass;
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

    public Double percentSrcAlsoDest;
    public Double percentSrcFail;
    public Double percentDstFail;

    public Boolean useAws;
    public Boolean ignoreFailures;

    public Integer numThreads;
}
