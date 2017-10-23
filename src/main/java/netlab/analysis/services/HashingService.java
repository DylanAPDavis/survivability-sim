package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.SimulationParameters;
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

    public String[] unhash(String hashString){
        return hashString.split("_");
    }
}
