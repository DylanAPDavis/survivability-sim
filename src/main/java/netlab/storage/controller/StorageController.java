package netlab.storage.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.services.HashingService;
import netlab.storage.services.StorageService;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Slf4j
@Controller
public class StorageController {

    private StorageService storageService;
    private HashingService hashingService;

    @Autowired
    private StorageController(StorageService storageService, HashingService hashingService){
        this.storageService = storageService;
        this.hashingService = hashingService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/storage/raw/{requestId}/{useAws}")
    public Request getRequest(@PathVariable String requestSetId, @PathVariable Boolean useAws){

        return storageService.retrieveRequestSet(requestSetId, useAws);
    }

    @RequestMapping(value = "/storage/raw/", method = RequestMethod.POST)
    @ResponseBody
    public boolean storeRequest(Request request){
        return storageService.storeRequestSet(request, request.isUseAws());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/storage/analyzed/{requestId}/{useAws}")
    public Analysis getAnalysis(@PathVariable String requestSetId, @PathVariable Boolean useAws){

        return storageService.retrieveAnalyzedSet(requestSetId, useAws);
    }

    @RequestMapping(value = "/storage/analyzed/match", method= RequestMethod.POST)
    @ResponseBody
    public List<Analysis> getMatchingAnalysis(SimulationParameters params){
        //TODO: Retrieve items from local DB if it is up
        if(!params.getUseAws()){
            log.info("Feature only supported when using AWS!");
            return new ArrayList<>();
        }
        return storageService.getAnalyzedSets(params);
    }


    @RequestMapping(value="/storage/delete_seed", method = RequestMethod.POST)
    @ResponseBody
    public Boolean deleteRecordsAndRequests(Long seed, String algorithm){
        return storageService.deleteRequests(seed, algorithm);
    }

    @RequestMapping(value="storage/get_seed", method = RequestMethod.GET)
    @ResponseBody
    public List<SimulationParameters> getParameters(Long seed){
        return storageService.queryForSeed(seed);
    }

}
