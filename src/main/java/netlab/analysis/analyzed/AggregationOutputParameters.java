package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.analysis.enums.CachingType;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureScenario;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationOutputParameters implements Serializable{

    List<FailureScenario> failureScenarios;
    List<Integer> nfeValues;
    List<Integer> numS;
    List<Integer> numD;
    List<Algorithm> algorithms;
    List<String> topologies;

    List<CachingType> cachingTypes;

    List<String> beforeMetrics;
    List<String> afterMetrics;
    List<String> cachingMetrics;
}
