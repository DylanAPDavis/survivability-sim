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
    private List<String> topologyIds;
    private List<String> problemClasses;
    private List<String> objectives;
    private List<String> algorithms;
    private List<Integer> numRequests;
    private List<Integer> numSources;
    private List<Integer> numDestinations;
    private Map<String, List<List<Double>>> failureMap;
    private List<Integer> numConnections;
    private List<List<Integer>> minConnectionRanges;
    private List<List<Integer>> maxConnectionRanges;
    private List<Double> percentSrcAlsoDests;
    private List<Boolean> ignoreFailures;
}
