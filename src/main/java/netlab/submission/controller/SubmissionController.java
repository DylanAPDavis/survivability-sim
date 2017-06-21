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
        // Find solutions for the request set, as long as at least one request has been generated
        if(requestSet.getRequests().keySet().size() > 0) {
            // Store the request ID and sim params in Dynamo DB
            storageService.putSimulationParameters(simulationParameters);

            // Process request set
            requestSet = processingService.processRequestSet(requestSet);
            simulationParameters.setCompleted(true);

            // Store the request set
            storageService.storeRequestSet(requestSet, requestSet.isUseAws());

            // Store the request ID and sim params in Dynamo DB
            storageService.putSimulationParameters(simulationParameters);
        }

        // Return the request set ID
        return requestSet.getId();
    }

    @RequestMapping(value = "/submit_rerun_all", method = RequestMethod.POST)
    @ResponseBody
    public List<String> rerunAllRequestSets(){
        SimulationParameters searchParams = SimulationParameters.builder()
                .completed(false)
                .useAws(true)
                .build();
        return rerunParams(searchParams);
    }

    @RequestMapping(value = "/submit_rerun", method = RequestMethod.POST)
    @ResponseBody
    public List<String> rerunRequestSets(Long seed){
        SimulationParameters searchParams = SimulationParameters.builder()
                .seed(seed)
                .completed(false)
                .useAws(true)
                .build();
        return rerunParams(searchParams);
    }

    private List<String> rerunParams(SimulationParameters searchParams){
        List<SimulationParameters> matchingParams = storageService.getMatchingSimulationParameters(searchParams);
        List<String> ids = new ArrayList<>();
        for(SimulationParameters params : matchingParams){
            RequestSet generatedSet = generationService.generateFromSimParams(params);
            processingService.processRequestSet(generatedSet);
            ids.add(generatedSet.getId());
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
