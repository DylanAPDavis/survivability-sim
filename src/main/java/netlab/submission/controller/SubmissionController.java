package netlab.submission.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.ProcessingService;
import netlab.storage.services.StorageService;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class SubmissionController {

    @Autowired
    public SubmissionController(GenerationService generationService, ProcessingService processingService,
                                StorageService storageService) {
        this.generationService = generationService;
        this.processingService = processingService;
        this.storageService = storageService;
    }

    private GenerationService generationService;

    private ProcessingService processingService;

    private StorageService storageService;

    @RequestMapping(value = "/submit_sim", method = RequestMethod.POST)
    @ResponseBody
    public String submitRequestSet(SimulationParameters simulationParameters){
        RequestSet requestSet = generationService.generateFromSimParams(simulationParameters);
        log.info("Generated request set: " + requestSet.getId());
        // Find solutions for the request set, as long as at least one request has been generated
        if(requestSet.getRequests().keySet().size() > 0) {
            List<SimulationParameters> matchingParams = storageService.queryForId(requestSet.getId());
            if(!matchingParams.isEmpty()){
                SimulationParameters previousRun = matchingParams.get(0);
                log.info("Request Set ID: " + requestSet.getId() + " is already in Dynamo DB!");
                if(previousRun.getCompleted()) {
                    log.info("Already completed, exiting...");
                    return requestSet.getId();
                }
                log.info("Has not been completed, rerunning now...");
            }
            // Store the request ID and sim params in Dynamo DB
            storageService.putSimulationParameters(simulationParameters);
            log.info("Stored params");

            // Process request set
            requestSet = processingService.processRequestSet(requestSet);
            simulationParameters.setCompleted(true);
            log.info("Processed request set");

            // Store the request set
            storageService.storeRequestSet(requestSet, requestSet.isUseAws());
            log.info("Stored request set");

            // Store the request ID and sim params in Dynamo DB
            storageService.putSimulationParameters(simulationParameters);
            log.info("Updated params with ID: " + requestSet.getId());
        }

        // Return the request set ID
        return requestSet.getId();
    }

    @RequestMapping(value = "/submit_rerun", method = RequestMethod.POST)
    @ResponseBody
    public List<String> rerunRequestSets(List<Long> seeds){
        List<String> ids = new ArrayList<>();
        for(Long seed : seeds) {
            List<SimulationParameters> matchingParams = storageService.queryForSeed(seed).parallelStream().filter(p -> !p.getCompleted()).collect(Collectors.toList());
            ids.addAll(matchingParams.parallelStream().map(SimulationParameters::getRequestSetId).collect(Collectors.toList()));
            matchingParams.forEach(this::submitRequestSet);
        }
        return ids;
    }

    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    @ResponseBody
    public String submitRequestSet(RequestParameters requestParameters){
        RequestSet requestSet = generationService.generateFromRequestParams(requestParameters);

        if(requestSet.getRequests().keySet().size() > 0){
            requestSet = processingService.processRequestSet(requestSet);
        }

        storageService.storeRequestSet(requestSet, requestSet.isUseAws());

        return requestSet.getId();
    }
}
