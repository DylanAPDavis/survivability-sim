package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.SimulationParameters;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class DefaultValueService {


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
            params.setProblemClass("Flow");
        }
        if(params.getObjective() == null){
            params.setObjective("LinkCost");
        }
        if(params.getAlgorithm() == null){
            params.setAlgorithm("ServiceILP");
        }


        // F - Total size of the failure set (shared by all connections)
        if(params.getFailures() == null){
            params.setFailures(new HashSet<>());
        }
        if(params.getPairFailureMap() == null){
            params.setPairFailureMap(new HashMap<>());
        }
        if(params.getSourceFailureMap() == null){
            params.setSourceFailureMap(new HashMap<>());
        }
        if(params.getDestFailureMap() == null){
            params.setDestFailureMap(new HashMap<>());
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
        if(params.getPairFailureProbabilityMap() == null){
            params.setPairFailureProbabilityMap(new HashMap<>());
        }else{
            Map<List<String>, Map<String, Double>> probMap = params.getPairFailureProbabilityMap();
            for(List<String> pair : probMap.keySet()) {
                for (String fail : probMap.get(pair).keySet()) {
                    probMap.get(pair).put(fail, Math.min(1.0, probMap.get(pair).get(fail)));
                }
            }
        }
        if(params.getSourceFailureProbabilityMap() == null){
            params.setSourceFailureProbabilityMap(new HashMap<>());
        }else{
            Map<String, Map<String, Double>> probMap = params.getSourceFailureProbabilityMap();
            for(String member : probMap.keySet()) {
                for (String fail : probMap.get(member).keySet()) {
                    probMap.get(member).put(fail, Math.min(1.0, probMap.get(member).get(fail)));
                }
            }
        }
        if(params.getDestFailureProbabilityMap() == null){
            params.setDestFailureProbabilityMap(new HashMap<>());
        }else{
            Map<String, Map<String, Double>> probMap = params.getDestFailureProbabilityMap();
            for(String member : probMap.keySet()) {
                for (String fail : probMap.get(member).keySet()) {
                    probMap.get(member).put(fail, Math.min(1.0, probMap.get(member).get(fail)));
                }
            }
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

        // Reachability - reach min/max sources/dests
        if(params.getReachMinSources() == null){
            params.setReachMinSources(0);
        }
        if(params.getReachMaxSources() == null){
            params.setReachMaxSources(params.getSources().size());
        }
        if(params.getReachMinDestinations() == null){
            params.setReachMinDestinations(0);
        }
        if(params.getReachMaxDestinations() == null){
            params.setReachMaxDestinations(params.getDestinations().size());
        }

        // Number of failureSet that will occur
        if(params.getNumFailsAllowed() == null || params.getNumFailsAllowed() < 0){
            params.setNumFailsAllowed(0);
        }
        if(params.getPairNumFailsAllowedMap() == null){
            params.setPairNumFailsAllowedMap(new HashMap<>());
        }
        if(params.getSourceNumFailsAllowedMap() == null){
            params.setSourceNumFailsAllowedMap(new HashMap<>());
        }
        if(params.getDestNumFailsAllowedMap() == null){
            params.setDestNumFailsAllowedMap(new HashMap<>());
        }

        if(params.getSdn() == null){
            params.setSdn(false);
        }
        if(params.getUseAws() == null){
            params.setUseAws(false);
        }
        if(params.getIgnoreFailures() == null){
            params.setIgnoreFailures(false);
        }
        if(params.getNumThreads() == null){
            params.setNumThreads(6);
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
        if(params.getGenerated() == null || params.getGenerated()){
            params.setGenerated(false);
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
            params.setProblemClass("Flow");
        }
        if(params.getObjective() == null){
            params.setObjective("TotalCost");
        }
        if(params.getAlgorithm() == null){
            params.setObjective("ServiceILP");
        }
        if(params.getNumRequests() == null || params.getNumRequests() < 0){
            params.setNumRequests(0);
        }

        if(params.getFailureSetSize() == null || params.getFailureSetSize() < 0){
            params.setFailureSetSize(0);
        }
        if(params.getMinMaxFailures() == null){
            params.setMinMaxFailures(new ArrayList<>());
        }
        if(params.getFailureClass() == null){
            params.setFailureClass("Both");
        }
        if(params.getMinConnections() == null || params.getMinConnections() < 0){
            params.setMinConnections(0);
        }
        if(params.getMinPairConnections() == null){
            params.setMinPairConnections(new ArrayList<>());
        }
        if(params.getMaxPairConnections() == null){
            params.setMaxPairConnections(new ArrayList<>());
        }
        if(params.getMinSrcConnections() == null){
            params.setMinSrcConnections(new ArrayList<>());
        }
        if(params.getMaxSrcConnections() == null){
            params.setMaxSrcConnections(new ArrayList<>());
        }
        if(params.getMinDstConnections() == null){
            params.setMinDstConnections(new ArrayList<>());
        }
        if(params.getMaxDstConnections() == null){
            params.setMaxDstConnections(new ArrayList<>());
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

        if(params.getNumFailureEvents() == null || params.getNumFailureEvents() < 0){
            params.setNumFailureEvents(0);
        }
        if(params.getMinMaxFailsAllowed() == null){
            params.setMinMaxFailsAllowed(new ArrayList<>());
        }
        if(params.getFailureProb() == null || params.getFailureProb() < 0 || params.getFailureProb() > 1){
            params.setFailureProb(1.0);
        }
        if(params.getMinMaxFailureProb() == null){
            params.setMinMaxFailureProb(new ArrayList<>());
        }
        if(params.getProcessingType() == null){
            params.setProcessingType("Solo");
        }
        if(params.getSdn() == null){
            params.setSdn(false);
        }
        if(params.getUseAws() == null){
            params.setUseAws(false);
        }

        if(params.getPercentSrcAlsoDest() == null || params.getPercentSrcAlsoDest() < 0.0){
            params.setPercentSrcAlsoDest(0.0);
        }
        else if(params.getPercentSrcAlsoDest() > 1.0){
            params.setPercentSrcAlsoDest(1.0);
        }

        if(params.getPercentSrcFail() == null || params.getPercentSrcFail() < 0.0){
            params.setPercentSrcFail(0.0);
        }
        else if(params.getPercentSrcFail() > 1.0){
            params.setPercentSrcFail(1.0);
        }

        if(params.getPercentDstFail() == null || params.getPercentDstFail() < 0.0){
            params.setPercentDstFail(0.0);
        }
        else if(params.getPercentDstFail() > 1.0){
            params.setPercentDstFail(1.0);
        }
        if(params.getIgnoreFailures() == null){
            params.setIgnoreFailures(false);
        }
        if(params.getNumThreads() == null){
            params.setNumThreads(6);
        }

        return params;
    }

}
