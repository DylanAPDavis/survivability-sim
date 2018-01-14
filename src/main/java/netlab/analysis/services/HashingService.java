package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AggregationParameters;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.analyzed.FailureDescription;
import netlab.analysis.analyzed.RoutingDescription;
import netlab.submission.enums.*;
import netlab.submission.request.SimulationParameters;
import netlab.topology.elements.Topology;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.apache.coyote.http11.Constants.a;

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
        return hash(params.getSeed(), params.getTopologyId(),  params.getRoutingType(), params.getAlgorithm(),
                params.getNumSources(), params.getNumDestinations(),
                params.getUseMinS(), params.getUseMaxS(), params.getUseMinD(), params.getUseMaxD(),
                /*params.getMinConnections(), params.getMinPairConnections(), params.getMaxPairConnections(), params.getMinSrcConnections(),
                params.getMaxSrcConnections(), params.getMinDstConnections(), params.getMaxDstConnections(),*/
                params.getTrafficCombinationType(),
                params.getFailureScenario(), /*params.getFailureSetSize(),*/ params.getFailureClass(), /*params.getFailureProb(),*/
                params.getNumFailureEvents(), params.getSourceSubsetDestType(), params.getSourceFailureType(),
                params.getDestFailureType(),  params.getIgnoreFailures(), /*params.getProblemClass(), params.getObjective(),*/
                params.getNumThreads());
    }


    /*
        private String topologyId;
        private Algorithm algorithm;
        private RoutingType routingType;
        private FailureScenario failureScenario;
        private Integer numFailuresEvents;
        private TrafficCombinationType trafficCombinationType;
        private RoutingDescription routingDescription;
        private Boolean ignoreFailures;
     */
    public String hashAnalysis(Analysis a){
        return hash(a.getTopologyId(), a.getAlgorithm(), a.getRoutingType(), a.getFailureScenario(), a.getNumFailuresEvents(),
                a.getTrafficCombinationType(), a.getRoutingDescription(), a.getIgnoreFailures());
    }

    public String hashForAggregation(String topologyId, Algorithm algorithm, RoutingType routingType, FailureScenario failureScenario,
                                     Integer nfe, TrafficCombinationType trafficCombinationType, RoutingDescription routingDescription,
                                     Boolean ignoreFailures){
        return hash(topologyId, algorithm, routingType, failureScenario, nfe, trafficCombinationType, routingDescription, ignoreFailures);
    }


    public String[] unhash(String hashString){
        return hashString.split("_");
    }


}
