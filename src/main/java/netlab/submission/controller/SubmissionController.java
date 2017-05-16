package netlab.submission.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.services.AnalysisService;
import netlab.processing.ProcessingService;
import netlab.storage.StorageService;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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

    @RequestMapping(value = "/submit_set", method = RequestMethod.POST)
    @ResponseBody
    public String submitRequestSet(SimulationParameters simulationParameters){
        RequestSet requestSet = generationService.generateRequests(simulationParameters);
        // Find solutions for the request set, as long as at least one request has been generated
        if(requestSet.getRequests().keySet().size() > 0){
            requestSet = processingService.processRequestSet(requestSet);
        }
        // Store the request set
        storageService.storeRequestSet(requestSet);

        // Return the request set ID
        return requestSet.getId();
    }

    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    @ResponseBody
    public String submitRequestSet(RequestParameters requestParameters){
        RequestSet requestSet = generationService.generateSetFromRequest(requestParameters);

        if(requestSet.getRequests().keySet().size() > 0){
            requestSet = processingService.processRequestSet(requestSet);
        }

        storageService.storeRequestSet(requestSet);

        return requestSet.getId();
    }
}
