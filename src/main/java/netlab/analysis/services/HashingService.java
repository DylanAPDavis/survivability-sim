package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.FailureDescription;
import netlab.analysis.analyzed.RoutingDescription;
import netlab.submission.enums.Objective;
import netlab.submission.request.SimulationParameters;
import netlab.topology.elements.Topology;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class HashingService {

    public String hash(Object... args){
        List<String> strings = new ArrayList<>();
        for(Object arg : args){
            strings.add(String.valueOf(arg).replace(" ", "").toLowerCase());
        }
        return String.join("_", strings);
    }

    public String createRequestId(SimulationParameters params) {
        return hash(params.getSeed(), params.getTopologyId(), params.getProblemClass(), params.getObjective(),
                params.getAlgorithm(), params.getNumSources(), params.getNumDestinations(), params.getMinConnections(),
                params.getMinPairConnections(), params.getMaxPairConnections(), params.getMinSrcConnections(),
                params.getMaxSrcConnections(), params.getMinDstConnections(), params.getMaxDstConnections(),
                params.getUseMinS(), params.getUseMaxS(), params.getUseMinD(), params.getUseMaxD(), params.getTrafficCombinationType(),
                params.getFailureSetSize(), params.getFailureClass(), params.getFailureProb(), params.getFailureScenario(),
                params.getNumFailureEvents(), params.getPercentSrcAlsoDest(), params.getPercentSrcFail(),
                params.getPercentDstFail(), params.getIgnoreFailures(), params.getNumThreads());
    }

    public String makeAggregationHash(SimulationParameters params) {
        return hash(params.getTopologyId(), params.getProblemClass(), params.getObjective(),
                params.getAlgorithm(), params.getNumSources(), params.getNumDestinations(), params.getMinConnections(),
                params.getUseMinS(), params.getUseMaxS(), params.getUseMinD(), params.getUseMaxD(), params.getTrafficCombinationType(),
                params.getFailureSetSize(), params.getFailureClass(), params.getFailureScenario(),
                params.getNumFailureEvents(), params.getPercentSrcAlsoDest(), params.getPercentSrcFail(),
                params.getPercentDstFail(), params.getIgnoreFailures(), params.getNumThreads());
    }

    public String makeAggregationHash(Topology topology, Integer threads, Objective objective,
                                      FailureDescription fd, RoutingDescription rd) {
        return hash(topology.getId(), rd.getProblemClass(), objective, rd.getAlgorithm(), rd.getNumSources(), rd.getNumDestinations(),
                rd.getMinConnections(), rd.getUseMinS(), rd.getUseMaxS(), rd.getUseMinD(), rd.getUseMaxD(), rd.getTrafficCombinationType(),
                fd.getFailureSetSize(), fd.getFailureClass(), fd.getFailureScenario(), fd.getNumFailureEvents(),
                rd.getPercentSrcAlsoDest(), fd.getPercentSrcFail(), fd.getPercentDstFail(), fd.getIgnoreFailures(),
                threads);
    }

    public String[] unhash(String hashString){
        return hashString.split("_");
    }


}
