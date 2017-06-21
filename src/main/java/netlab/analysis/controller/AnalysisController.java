package netlab.analysis.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AggregateAnalyzedSet;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.services.AnalysisService;
import netlab.storage.services.StorageService;
import netlab.submission.request.Request;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class AnalysisController {

    @Autowired
    private AnalysisController(AnalysisService analysisService, StorageService storageService){
        this.analysisService = analysisService;
        this.storageService = storageService;
    }

    private AnalysisService analysisService;
    private StorageService storageService;

    @RequestMapping(value = "/analyze", method = RequestMethod.POST)
    @ResponseBody
    public AnalyzedSet analyzeRequestSet(AnalysisParameters params){
        RequestSet requestSet = storageService.retrieveRequestSet(params.getRequestSetId(), params.getUseAws());

        AnalyzedSet analyzedSet = analysisService.analyzeRequestSet(requestSet);

        // Store the analyzed set
        storageService.storeAnalyzedSet(analyzedSet, params.getUseAws());

        // Return the request set ID
        return analyzedSet;
    }

    @RequestMapping(value = "/analyze/aggregate", method = RequestMethod.POST)
    @ResponseBody
    public AggregateAnalyzedSet aggregateAnalyzedSets(SimulationParameters params){

        List<AnalyzedSet> analyzedSets = storageService.getAnalyzedSets(params);
        return analysisService.aggregateAnalyzedSetsGivenParams(analyzedSets);
    }

    @RequestMapping(value = "/analyze/create_failed_request_sets", method = RequestMethod.POST)
    @ResponseBody
    public String createFailedRequestSets(Long seed){
        // 1. Get all simulation parameters for a particular seed
        SimulationParameters filterParams = SimulationParameters.builder()
                .seed(seed)
                .generated(false)
                .build();
        List<SimulationParameters> matchingParams = storageService.getMatchingSimulationParameters(filterParams);

        // 2. From those, get the zero failure parameters & requests
        List<SimulationParameters> zeroFailureParams = matchingParams.parallelStream().filter(SimulationParameters::getIgnoreFailures).collect(Collectors.toList());

        // 3. For each zero failure param/request
        // Create another parameters/request with a new ID, and matching failure stats, for each parameter
        return createNewParamsAndSets(zeroFailureParams, matchingParams);
    }

    private String createNewParamsAndSets(List<SimulationParameters> zeroFailureParams, List<SimulationParameters> matchingParams){
        String lastId = "";
        for(SimulationParameters zeroParam : zeroFailureParams){
            System.out.println("Working on: " + zeroParam.getRequestSetId());
            lastId = zeroParam.getRequestSetId();
            List<SimulationParameters> matchingParameters = matchingParams.parallelStream()
                    .filter(p -> !p.getIgnoreFailures())
                    .filter(p -> !p.getGenerated())
                    .filter(p -> p.getAlgorithm().equals(zeroParam.getAlgorithm()))
                    .filter(p -> p.getProblemClass().equals(zeroParam.getProblemClass()))
                    .filter(SimulationParameters::getCompleted)
                    .filter(p -> p.getTopologyId().equals(zeroParam.getTopologyId()))
                    .filter(p -> p.getObjective().equals(zeroParam.getObjective()))
                    .filter(p -> p.getNumRequests().equals(zeroParam.getNumRequests()))
                    .filter(p -> p.getNumSources().equals(zeroParam.getNumSources()))
                    .filter(p -> p.getNumDestinations().equals(zeroParam.getNumDestinations()))
                    .filter(p -> p.getNumConnections().equals(zeroParam.getNumConnections()))
                    .filter(p -> p.getMinConnectionsRange().equals(zeroParam.getMinConnectionsRange()))
                    .filter(p -> p.getMaxConnectionsRange().equals(zeroParam.getMaxConnectionsRange()))
                    .filter(p -> p.getProcessingType().equals(zeroParam.getProcessingType()))
                    .filter(p -> p.getPercentSrcAlsoDest().equals(zeroParam.getPercentSrcAlsoDest()))
                    .filter(p -> p.getSdn().equals(zeroParam.getSdn()))
                    .filter(p -> p.getUseAws().equals(zeroParam.getUseAws()))
                    .collect(Collectors.toList());
            System.out.println("Got matching params");
            RequestSet zeroSet = storageService.retrieveRequestSet(zeroParam.getRequestSetId(), true);
            System.out.println("Got the request set");
            for(SimulationParameters matchParam : matchingParameters){
                String matchId = matchParam.getRequestSetId();
                RequestSet matchSet = storageService.retrieveRequestSet(matchId, true);
                String newId = UUID.randomUUID().toString();
                matchParam.setGenerated(true);
                matchParam.setIgnoreFailures(true);
                matchParam.setRequestSetId(newId);
                RequestSet newSet = cloneAndModifyRequestSet(zeroSet, matchSet, newId);
                // 4. Store them in Dynamo / S3
                boolean uploadParam = storageService.putSimulationParameters(matchParam);
                boolean uploadSet = storageService.storeRequestSet(newSet, true);
                if(!uploadParam || !uploadSet){
                    System.out.println("Failed set: " + newId);
                    lastId += " : Failed on - " + newId;
                    return lastId;
                }
            }
            System.out.println("Made the new request set and params");
        }
        return lastId;
    }

    private RequestSet cloneAndModifyRequestSet(RequestSet zeroSet, RequestSet matchSet, String newId) {
        // Modify zeroSet to include failure params from matchSet
        Map<String, Request> newMap = new HashMap<>();
        Map<String, Request> matchMap = matchSet.getRequests();
        List<Request> options = new ArrayList<>(matchMap.values());

        zeroSet.setId(newId);
        zeroSet.setFailureClass(matchSet.getFailureClass());
        zeroSet.setPercentSrcFail(matchSet.getPercentSrcFail());
        zeroSet.setPercentDestFail(matchSet.getPercentDestFail());
        for(Request request : zeroSet.getRequests().values()){
            Request matchRequest = matchMap.getOrDefault(request.getId(), findMatchingRequest(request, options));
            request.setIgnoreFailures(true);
            request.setFailures(matchRequest.getFailures());
            request.setNumFailsAllowed(matchRequest.getNumFailsAllowed());
            newMap.put(request.getId(), request);
        }
        zeroSet.setRequests(newMap);
        return zeroSet;
    }

    private Request findMatchingRequest(Request request, List<Request> options) {
        List<Request> filtered = options.parallelStream()
                .filter(r -> r.getSources().equals(request.getSources()))
                .filter(r -> r.getDestinations().equals(request.getDestinations()))
                .filter(r -> r.getConnections().equals(request.getConnections()))
                .collect(Collectors.toList());
        if(filtered.size() > 0){
            return filtered.get(0);
        }
        return options.remove(options.size()-1);
    }


}
