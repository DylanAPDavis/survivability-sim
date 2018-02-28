package netlab.submission.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.ProcessingService;
import netlab.storage.services.StorageService;
import netlab.submission.request.Request;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.submission.services.SimulateService;
import netlab.submission.simulate.SimRequest;
import netlab.submission.simulate.SimResponse;
import netlab.visualization.PrintingService;
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
                                StorageService storageService, PrintingService printingService, SimulateService simulateService) {
        this.generationService = generationService;
        this.processingService = processingService;
        this.storageService = storageService;
        this.printingService = printingService;
        this.simulateService = simulateService;
    }

    private GenerationService generationService;

    private ProcessingService processingService;

    private StorageService storageService;

    private PrintingService printingService;

    private SimulateService simulateService;

    @RequestMapping(value = "/submit_sim", method = RequestMethod.POST)
    @ResponseBody
    public String submitRequest(SimulationParameters simulationParameters){
        Request request = generationService.generateFromSimParams(simulationParameters);
        log.info("Generated request set: " + request.getId());
        // Find solutions as long as request has successfully been generated
        boolean useAws = simulationParameters.getUseAws();
        if(useAws) {
            List<SimulationParameters> matchingParams = storageService.queryForId(request.getId());
            if (!matchingParams.isEmpty()) {
                SimulationParameters previousRun = matchingParams.get(0);
                log.info("Details Set ID: " + request.getId() + " is already in Dynamo DB!");
                if (previousRun.getCompleted()) {
                    log.info("Already completed, exiting...");
                    return request.getId();
                }
                log.info("Has not been completed, rerunning now...");
            }
        }
        // Store the request ID and sim params in Dynamo DB
        if(useAws) {
            storageService.putSimulationParameters(simulationParameters);
            log.info("Stored params");
        }

        // Process request
        request = processingService.processRequest(request);
        simulationParameters.setCompleted(true);
        simulationParameters.setTimedOut(request.getTimedOut());
        request.setCompleted(true);
        System.out.println(printingService.outputPaths(request));
        log.info("Processed request set");

        // Store the request set
        storageService.storeRequestSet(request, request.isUseAws());
        log.info("Stored request set");

        // Store the request ID and sim params in Dynamo DB
        if(useAws) {
            storageService.putSimulationParameters(simulationParameters);
            log.info("Updated params with ID: " + request.getId());
        }

        // Return the request set ID
        return request.getId();
    }

    @RequestMapping(value = "/submit_rerun", method = RequestMethod.POST)
    @ResponseBody
    public List<String> rerunRequests(List<Long> seeds){
        List<String> ids = new ArrayList<>();
        for(Long seed : seeds) {
            List<SimulationParameters> matchingParams = storageService.queryForSeed(seed).parallelStream().filter(p -> !p.getCompleted()).collect(Collectors.toList());
            ids.addAll(matchingParams.parallelStream().map(SimulationParameters::getRequestId).collect(Collectors.toList()));
            matchingParams.forEach(this::submitRequest);
        }
        return ids;
    }

    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    @ResponseBody
    public String submitRequest(RequestParameters requestParameters){
        Request request = generationService.generateFromRequestParams(requestParameters, null);

        if(request != null) {
            request = processingService.processRequest(request);

            storageService.storeRequestSet(request, request.isUseAws());
        }

        return request != null ? request.getId() : "Failed!";
    }

    @RequestMapping(value = "/simulate", method = RequestMethod.POST)
    @ResponseBody
    public SimResponse simulateRequest(SimRequest simRequest){
        Request request = simulateService.generateRequest(simRequest);


        if(request != null){
            request = processingService.processRequest(request, simRequest.getNetwork());
        }
        // Translate request into sim response
        return simulateService.formatResponse(request);
    }
}
