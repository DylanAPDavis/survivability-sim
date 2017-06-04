package netlab.analysis.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.services.AnalysisService;
import netlab.storage.services.StorageService;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashSet;
import java.util.Set;

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

    @RequestMapping(value = "/analyze/averages", method = RequestMethod.POST)
    @ResponseBody
    public AnalyzedSet averageAnalyzedSets(SimulationParameters params){

        //TODO: Implement this
        AnalyzedSet averageAnalyzedSet = analysisService.averageAnalyzedSetsGivenParams(params);

        return averageAnalyzedSet;
    }



}
