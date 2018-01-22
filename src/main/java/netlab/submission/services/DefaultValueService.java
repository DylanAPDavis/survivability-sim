package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.FailureScenario;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class DefaultValueService {

    private RoutingParamAssignmentService routingParamAssignmentService;
    private EnumGenerationService enumGenerationService;

    @Autowired
    public DefaultValueService(RoutingParamAssignmentService routingParamAssignmentService, EnumGenerationService enumGenerationService){
        this.routingParamAssignmentService = routingParamAssignmentService;
        this.enumGenerationService = enumGenerationService;
    }


    public RequestParameters assignDefaults(RequestParameters params) {
        if(params.getTopologyId() == null){
            params.setTopologyId("NSFnet");
        }
        if(params.getSources() == null){
            params.setSources(new HashSet<>());
        }
        if(params.getDestinations() == null){
            params.setDestinations(new HashSet<>());
        }
        if(params.getProblemClass() == null){
            params.setProblemClass("combined");
        }
        if(params.getObjective() == null){
            params.setObjective("totalcost");
        }
        if(params.getAlgorithm() == null){
            params.setAlgorithm("ilp");
        }
        if(params.getRoutingType() == null){
            params.setRoutingType("default");
        }

        // Fill in a bunch of default values if they've selected a non-default option
        if(!params.getRoutingType().toLowerCase().equals("default")){
            routingParamAssignmentService.provideRoutingValues(params);
        }

        // F - Total size of the failure set (shared by all connections)
        if(params.getFailures() == null){
            params.setFailures(new HashSet<>());
        }

        // Failure probability - pick one field
        if(params.getFailureProbabilityMap() == null){
            params.setFailureProbabilityMap(new HashMap<>());
        } else{
            Map<String, Double> probMap = params.getFailureProbabilityMap();
            for(String fail : probMap.keySet()){
                probMap.put(fail, Math.min(1.0, probMap.get(fail)));
            }
        }

        // Number of failures that will occur
        if(params.getNumFailureEvents() == null || params.getNumFailureEvents() < 0){
            params.setNumFailureEvents(0);
        }

        // C - total number of connections
        if(params.getNumConnections() == null || params.getNumConnections() < 0){
            params.setNumConnections(0);
        }

        // Pairs
        if(params.getPairNumConnectionsMap() == null){
            params.setPairNumConnectionsMap(new HashMap<>());
        }
        if(params.getPairMinNumConnectionsMap() == null){
            params.setPairMinNumConnectionsMap(new HashMap<>());
        }
        if(params.getPairMaxNumConnectionsMap() == null){
            params.setPairMaxNumConnectionsMap(new HashMap<>());
        }

        // Source
        if(params.getSourceNumConnectionsMap() == null){
            params.setSourceNumConnectionsMap(new HashMap<>());
        }
        if(params.getSourceMinNumConnectionsMap() == null){
            params.setSourceMinNumConnectionsMap(new HashMap<>());
        }
        if(params.getSourceMaxNumConnectionsMap() == null){
            params.setSourceMaxNumConnectionsMap(new HashMap<>());
        }

        // Dest
        if(params.getDestNumConnectionsMap() == null){
            params.setDestNumConnectionsMap(new HashMap<>());
        }
        if(params.getDestMinNumConnectionsMap() == null){
            params.setDestMinNumConnectionsMap(new HashMap<>());
        }
        if(params.getDestMaxNumConnectionsMap() == null){
            params.setDestMaxNumConnectionsMap(new HashMap<>());
        }

        // Reachability - use min/max sources/dests
        if(params.getUseMinS() == null){
            params.setUseMinS(0);
        }
        if(params.getUseMaxS() == null){
            params.setUseMaxS(params.getSources().size());
        }
        if(params.getUseMinD() == null){
            params.setUseMinD(0);
        }
        if(params.getUseMaxD() == null){
            params.setUseMaxD(params.getDestinations().size());
        }

        if(params.getTrafficCombinationType() == null){
            params.setTrafficCombinationType("none");
        }

        if(params.getIgnoreFailures() == null){
            params.setIgnoreFailures(false);
        }
        if(params.getNumThreads() == null){
            params.setNumThreads(8);
        }
        return params;
    }


    public SimulationParameters assignDefaults(SimulationParameters params) {
        if(params.getSeed() == null){
            Random rng = new Random();
            params.setSeed(rng.nextLong());
        }
        if(params.getSubmittedDate() == null){
            params.setSubmittedDate(Calendar.getInstance());
        }
        if(params.getCompleted() == null || params.getCompleted()){
            params.setCompleted(false);
        }
        if(params.getTopologyId() == null){
            params.setTopologyId("NSFnet");
        }
        if(params.getNumSources() == null){
            params.setNumSources(0);
        }
        if(params.getNumDestinations() == null){
            params.setNumDestinations(0);
        }
        if(params.getProblemClass() == null){
            params.setProblemClass("combined");
        }
        if(params.getObjective() == null){
            params.setObjective("totalcost");
        }
        if(params.getAlgorithm() == null){
            params.setAlgorithm("minimumcost");
        }
        if(params.getRoutingType() == null){
            params.setRoutingType("default");
        }
        if(params.getSourceSubsetDestType() == null){
            params.setSourceSubsetDestType("none");
        }
        if(!params.getRoutingType().toLowerCase().equals("default")){
            routingParamAssignmentService.provideDefaultRoutingValues(params);
        }


        if(params.getFailureSetSize() == null || params.getFailureSetSize() < 0){
            params.setFailureSetSize(0);
        }
        if(params.getFailureClass() == null){
            params.setFailureClass("both");
        }
        if(params.getFailureProb() == null || params.getFailureProb() < 0 || params.getFailureProb() > 1){
            params.setFailureProb(1.0);
        }
        if(params.getFailureScenario() == null){
            params.setFailureScenario("default");
        }
        if(params.getNumFailureEvents() == null || params.getNumFailureEvents() < 0){
            params.setNumFailureEvents(0);
        }
        FailureScenario failureScenario = enumGenerationService.getFailureScenario(params.getFailureScenario());
        if(failureScenario != FailureScenario.Default){
            if(failureScenario.equals(FailureScenario.AllLinks)){
                params.setFailureClass("link");
            }
            if(failureScenario.equals(FailureScenario.AllNodes)){
                params.setFailureClass("node");
            }
        }


        if(params.getMinConnections() == null || params.getMinConnections() < 0){
            params.setMinConnections(0);
        }
        if(params.getMinPairConnections() == null){
            params.setMinPairConnections(0);
        }
        if(params.getMaxPairConnections() == null){
            params.setMaxPairConnections(params.getMinConnections());
        }
        if(params.getMinSrcConnections() == null){
            params.setMinSrcConnections(0);
        }
        if(params.getMaxSrcConnections() == null){
            params.setMaxSrcConnections(params.getMinConnections());
        }
        if(params.getMinDstConnections() == null){
            params.setMinDstConnections(0);
        }
        if(params.getMaxDstConnections() == null){
            params.setMaxDstConnections(params.getMinConnections());
        }
        if(params.getUseMinS() == null){
            params.setUseMinS(0);
        }
        if(params.getUseMaxS() == null){
            params.setUseMaxS(params.getNumSources());
        }
        if(params.getUseMinD() == null){
            params.setUseMinD(0);
        }
        if(params.getUseMaxD() == null){
            params.setUseMaxD(params.getNumDestinations());
        }

        if(params.getTrafficCombinationType() == null){
            params.setTrafficCombinationType("none");
        }

        if(params.getUseAws() == null){
            params.setUseAws(false);
        }


        if(params.getSourceFailureType() == null){
            params.setSourceFailureType("allow");
        }

        if(params.getDestFailureType() == null){
            params.setDestFailureType("allow");
        }
        if(params.getIgnoreFailures() == null){
            params.setIgnoreFailures(false);
        }
        if(params.getNumThreads() == null){
            params.setNumThreads(8);
        }

        // Default cutoff of one hour
        if(params.getCutoffTimeSeconds() == null || params.getCutoffTimeSeconds() < 0){
            params.setCutoffTimeSeconds(3600);
        }
        if(params.getTimedOut() == null){
            params.setTimedOut(false);
        }

        return params;
    }


}
