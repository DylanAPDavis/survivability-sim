package netlab.storage.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.services.HashingService;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FailureAdditionService {

    private StorageService storageService;
    private HashingService hashingService;

    @Autowired
    private FailureAdditionService(StorageService storageService, HashingService hashingService){
        this.storageService = storageService;
        this.hashingService = hashingService;
    }

    public String createNewParamsAndSets(List<SimulationParameters> zeroFailureParams, Map<String, List<SimulationParameters>> matchingMap){
        String lastId = "";
        for(SimulationParameters zeroParam : zeroFailureParams){
            //System.out.println("Working on: " + zeroParam.getRequestId());
            lastId = zeroParam.getRequestSetId();
            List<SimulationParameters> matchingParameters = matchingMap.get(hashSimParams(zeroParam));
            //System.out.println("Got matching params");
            Request zeroSet = storageService.retrieveRequestSet(zeroParam.getRequestSetId(), true);
            //System.out.println("Got the request set");
            for(SimulationParameters matchParam : matchingParameters){
                String matchId = matchParam.getRequestSetId();
                if(matchId.equals(lastId)){
                    continue;
                }
                Request matchSet = storageService.retrieveRequestSet(matchId, true);
                String newId = UUID.randomUUID().toString();
                matchParam.setGenerated(true);
                matchParam.setIgnoreFailures(true);
                matchParam.setRequestSetId(newId);
                Request newSet = cloneAndModifyRequestSet(zeroSet, matchSet, newId);
                // 4. Store them in Dynamo / S3
                boolean uploadParam = storageService.putSimulationParameters(matchParam);
                boolean uploadSet = storageService.storeRequestSet(newSet, true);
                if(!uploadParam || !uploadSet){
                    System.out.println("Failed set: " + newId);
                    lastId += " : Failed on - " + newId;
                    return lastId;
                }
            }
            //System.out.println("Made the new request set and params");
        }
        return lastId;
    }

    private Request cloneAndModifyRequestSet(Request zeroSet, Request matchSet, String newId) {
        // Modify zeroSet to include failure params from matchSet
        Map<String, Details> newMap = new HashMap<>();
        Map<String, Details> matchMap = matchSet.getDetails();
        List<Details> options = new ArrayList<>(matchMap.values());

        zeroSet.setId(newId);
        zeroSet.setFailureClass(matchSet.getFailureClass());
        zeroSet.setPercentSrcFail(matchSet.getPercentSrcFail());
        zeroSet.setPercentDestFail(matchSet.getPercentDestFail());
        for(Details details : zeroSet.getDetails().values()){
            Details matchDetails = matchMap.getOrDefault(details.getId(), findMatchingRequest(details, options));
            details.setIgnoreFailures(true);
            details.setFailures(matchDetails.getFailures());
            details.setNumFailsAllowed(matchDetails.getNumFailsAllowed());
            newMap.put(details.getId(), details);
        }
        zeroSet.setDetails(newMap);
        return zeroSet;
    }

    private Details findMatchingRequest(Details details, List<Details> options) {
        List<Details> filtered = options.parallelStream()
                .filter(r -> r.getSources().equals(details.getSources()))
                .filter(r -> r.getDestinations().equals(details.getDestinations()))
                .filter(r -> r.getConnections().equals(details.getConnections()))
                .collect(Collectors.toList());
        if(filtered.size() > 0){
            return filtered.get(0);
        }
        return options.remove(options.size()-1);
    }

    public String hashSimParams(SimulationParameters params){
        return hashingService.hash(params.getAlgorithm(), params.getProblemClass(), String.valueOf(params.getCompleted()), params.getTopologyId(),
                params.getObjective(), String.valueOf(params.getNumRequests()), String.valueOf(params.getNumSources()), String.valueOf(params.getNumDestinations()),
                String.valueOf(params.getNumConnections()), String.valueOf(params.getMinConnectionsRange()),
                String.valueOf(params.getMaxConnectionsRange()), params.getProcessingType(),
                String.valueOf(params.getPercentSrcAlsoDest()), String.valueOf(params.getSdn()), String.valueOf(params.getUseAws()));
    }

}
