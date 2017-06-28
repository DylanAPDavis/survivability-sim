package netlab.analysis.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AggregateAnalyzedSet;
import netlab.analysis.analyzed.AggregationParameters;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.services.AnalysisService;
import netlab.storage.services.StorageService;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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
        return analysisService.aggregateAnalyzedSets(analyzedSets);
    }


    @RequestMapping(value = "/analyze/aggregate_seeds", method = RequestMethod.POST)
    @ResponseBody
    public String aggregateSeeds(AggregationParameters agParams){
        SimulationParameters searchParams = SimulationParameters.builder()
                .seed(agParams.getSeeds().get(0))
                .completed(true)
                .useAws(true)
                .build();
        List<SimulationParameters> firstSeedParams = storageService.getMatchingSimulationParameters(searchParams);
        List<AggregateAnalyzedSet> aggregateSets = new ArrayList<>();
        for(SimulationParameters firstParams : firstSeedParams){

            List<SimulationParameters> otherParams = new ArrayList<>();
            for(int i = 1; i < agParams.getSeeds().size(); i++){
                SimulationParameters matchingParamsSeed = firstParams.clone();
                matchingParamsSeed.setSeed(agParams.getSeeds().get(i));
                matchingParamsSeed.setRequestSetId(null);
                matchingParamsSeed.setSubmittedDate(null);
                otherParams.addAll(storageService.getMatchingSimulationParameters(matchingParamsSeed));
            }
            otherParams.add(firstParams);
            List<AnalyzedSet> analyzedSets = otherParams.stream()
                    .map(p -> storageService.retrieveAnalyzedSet(p.getRequestSetId(), true))
                    .collect(Collectors.toList());
            AggregateAnalyzedSet aggregateAnalyzedSet = analysisService.aggregateAnalyzedSets(analyzedSets);
            aggregateSets.add(aggregateAnalyzedSet);
        }
        // TODO: Take all aggregate params, make a CSV (somehow)
        Map<String, Map<String, Map<String, Map<String, Map<String, Map<String, List<AggregateAnalyzedSet>>>>>>> outputMap = new HashMap<>();
        for(int index = 0; index < firstSeedParams.size(); index++){
            SimulationParameters params = firstSeedParams.get(index);
            AggregateAnalyzedSet aggSet = aggregateSets.get(index);
            String topologyName = params.getTopologyId();
            String algorithm = params.getAlgorithm();
            String problemClass = params.getProblemClass();
            String objective = params.getObjective();
            Double sourceDestOverlap = params.getPercentSrcAlsoDest();
            String failureClass = params.getFailureClass();
            Integer failureSetSize = params.getFailureSetSize();
            Integer numFailsAllowed = params.getNumFailsAllowed();
            Double percentSrcFail = params.getPercentSrcFail();
            Double percentDstFail = params.getPercentDestFail();
            List<Integer> minC = params.getMinConnectionsRange();
            List<Integer> maxC = params.getMaxConnectionsRange();
            Integer numSources = params.getNumSources();
            Integer numDestinations = params.getNumDestinations();
        }

        return "";
    }



}
