package netlab.storage.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.services.AnalysisService;
import netlab.analysis.services.HashingService;
import netlab.storage.services.FailureAdditionService;
import netlab.storage.services.StorageService;
import netlab.submission.request.Request;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class StorageController {

    private StorageService storageService;
    private FailureAdditionService failureAdditionService;
    private HashingService hashingService;

    @Autowired
    private StorageController(StorageService storageService, FailureAdditionService failureAdditionService, HashingService hashingService){
        this.storageService = storageService;
        this.failureAdditionService = failureAdditionService;
        this.hashingService = hashingService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/storage/raw/{requestSetId}/{useAwes}")
    public RequestSet getRequestSet(@PathVariable String requestSetId, @PathVariable Boolean useAws){

        return storageService.retrieveRequestSet(requestSetId, useAws);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/storage/analyzed/{requestSetId}/{useAwes}")
    public AnalyzedSet getAnalyzedSet(@PathVariable String requestSetId, @PathVariable Boolean useAws){

        return storageService.retrieveAnalyzedSet(requestSetId, useAws);
    }

    @RequestMapping(value = "/storage/analyzed/sets", method= RequestMethod.POST)
    @ResponseBody
    public List<AnalyzedSet> getAnalyzedSets(SimulationParameters params){
        //TODO: Retrieve items from local DB if it is up
        if(!params.getUseAws()){
            log.info("Feature only supported when using AWS!");
            return new ArrayList<>();
        }
        return storageService.getAnalyzedSets(params);
    }

    @RequestMapping(value = "/storage/create_failed_request_sets", method = RequestMethod.POST)
    @ResponseBody
    public String createFailedRequestSets(Long seed){
        // 1. Get all simulation parameters for a particular seed
        List<SimulationParameters> matchingParams = storageService.queryForSeed(seed);


        // 2. From those, get the zero failure parameters & requests
        List<SimulationParameters> zeroFailureParams = matchingParams.parallelStream()
                .filter(p -> !p.getGenerated())
                .filter(SimulationParameters::getIgnoreFailures)
                .collect(Collectors.toList());

        Map<String, List<SimulationParameters>> matchingMap = matchingParams.parallelStream()
                .filter(p -> !p.getGenerated())
                .filter(p -> p.getNumFailsAllowed() == 1)
                .filter(p -> !p.getIgnoreFailures())
                .collect(Collectors.groupingBy(p -> failureAdditionService.hashSimParams(p), Collectors.toList()));

        // 3. For each zero failure param/request
        // Create another parameters/request with a new ID, and matching failure stats, for each parameter
        return failureAdditionService.createNewParamsAndSets(zeroFailureParams, matchingMap);
    }

    @RequestMapping(value="/storage/delete_seed", method = RequestMethod.POST)
    @ResponseBody
    public Boolean deleteRecordsAndRequests(Long seed){
        return storageService.deleteRequests(seed);
    }

    @RequestMapping(value="storage/get_seed", method = RequestMethod.GET)
    @ResponseBody
    public List<SimulationParameters> getParameters(Long seed){
        return storageService.queryForSeed(seed);
    }

}
