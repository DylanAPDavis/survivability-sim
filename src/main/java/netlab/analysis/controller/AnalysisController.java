package netlab.analysis.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AggregateAnalyzedSet;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.services.AnalysisService;
import netlab.storage.services.StorageService;
import netlab.submission.request.Request;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

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

    @RequestMapping(value = "/analyze/aggregate", method = RequestMethod.POST)
    @ResponseBody
    public AggregateAnalyzedSet aggregateAnalyzedSets(SimulationParameters params){

        List<AnalyzedSet> analyzedSets = storageService.getAnalyzedSets(params);
        return analysisService.aggregateAnalyzedSetsGivenParams(analyzedSets);
    }


    @RequestMapping(value = "/analyze/aggregate_seeds", method = RequestMethod.POST)
    @ResponseBody
    public String aggregateSeeds(List<Long> seeds){
        SimulationParameters searchParams = SimulationParameters.builder()
                .seed(seeds.get(0))
                .completed(true)
                .useAws(true)
                .build();
        List<SimulationParameters> firstSeedParams = storageService.getMatchingSimulationParameters(searchParams);
        for(SimulationParameters firstParams : firstSeedParams){

            List<SimulationParameters> otherParams = new ArrayList<>();
            for(int i = 1; i < seeds.size(); i++){
                SimulationParameters matchingParamsSeed = firstParams.clone();
                matchingParamsSeed.setSeed(seeds.get(i));
                matchingParamsSeed.setRequestSetId(null);
                matchingParamsSeed.setSubmittedDate(null);
                otherParams.addAll(storageService.getMatchingSimulationParameters(matchingParamsSeed));
            }
            // TODO: Aggregate these params
        }
        // TODO: Take all aggregate params, make a CSV (somehow)

        return "";
    }



}
