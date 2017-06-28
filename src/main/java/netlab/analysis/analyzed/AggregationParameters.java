package netlab.analysis.analyzed;

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
public class AggregationParameters {

    List<Long> seeds;
    List<String> topologyIds;
    List<String> problemClasses;
    List<String> objectives;
    List<String> algorithms;
    List<Integer> numRequests;
    List<Integer> numSources;
    List<Integer> numDestinations;
    Map<String, List<List<Double>>> failureMap;
    List<Integer> numConnections;
    List<List<Integer>> minConnectionRanges;
    List<List<Integer>> maxConnectionRanges;
    List<Integer> percentSrcAlsoDests;
    List<Boolean> ignoreFailures;
    
}
