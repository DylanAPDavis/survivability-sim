package netlab.submission.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.RequestGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
public class SubmissionController {

    @Autowired
    public SubmissionController(RequestGenerationService requestGenerationService) {
        this.requestGenerationService = requestGenerationService;
    }

    private RequestGenerationService requestGenerationService;

    @RequestMapping(value = "/resv/connection/add", method = RequestMethod.POST)
    @ResponseBody
    public RequestSet submitSimulationRequest(SimulationParameters simulationParameters){
        RequestSet requestSet = requestGenerationService.generateRequests(simulationParameters);
        if(requestSet == null){

        }
    }
}
