package netlab.submission.rest;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.RequestGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

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
    public String submitSimulationRequest(SimulationParameters simulationParameters){
        List<Request> requests = requestGenerationService.
    }
}
