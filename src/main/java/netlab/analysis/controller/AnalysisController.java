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

    @RequestMapping(value="/analyze_seed", method = RequestMethod.POST)
    @ResponseBody
    public String analyzeSeeds(List<Long> seeds){
        for(Long seed : seeds) {
            List<SimulationParameters> seedParams = storageService.queryForSeed(seed);
            seedParams.stream()
                    .filter(SimulationParameters::getCompleted)
                    .filter(SimulationParameters::getUseAws)
                    .map(SimulationParameters::getRequestSetId)
                    .forEach( id -> {
                        AnalyzedSet analyzedSet = storageService.retrieveAnalyzedSet(id, true);
                        if(analyzedSet == null) {
                            RequestSet requestSet = storageService.retrieveRequestSet(id, true);
                            analyzedSet = analysisService.analyzeRequestSet(requestSet);
                            storageService.storeAnalyzedSet(analyzedSet, true);
                        }
                    });
            System.out.println("Analyzed seed: " + seed);
        }
        return "Success";
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
        List<List<SimulationParameters>> paramsBySeed = new ArrayList<>();
        for(Long seed : agParams.getSeeds()) {
            List<SimulationParameters> seedParams = storageService.queryForSeed(seed)
                    .stream()
                    .filter(SimulationParameters::getCompleted)
                    .collect(Collectors.toList());
            paramsBySeed.add(seedParams);
        }
        List<AggregateAnalyzedSet> aggregateSets = new ArrayList<>();
        List<SimulationParameters> primaryParamList = paramsBySeed.get(0);
        for(SimulationParameters primaryParams : primaryParamList){
            List<SimulationParameters> paramsToBeAnalyzed = new ArrayList<>();
            for(int i = 1; i < agParams.getSeeds().size(); i++){
                Optional<SimulationParameters> matchingParam = findMatchingSimParams(primaryParams, paramsBySeed.get(i));
                matchingParam.ifPresent(paramsToBeAnalyzed::add);
            }
            paramsToBeAnalyzed.add(primaryParams);
            List<AnalyzedSet> analyzedSets = paramsToBeAnalyzed.stream()
                    .map(p -> storageService.retrieveAnalyzedSet(p.getRequestSetId(), true))
                    .collect(Collectors.toList());
            AggregateAnalyzedSet aggregateAnalyzedSet = analysisService.aggregateAnalyzedSets(analyzedSets);
            aggregateSets.add(aggregateAnalyzedSet);
        }

        return analysisService.aggregateSeeds(agParams, primaryParamList, aggregateSets);

    }

    public Optional<SimulationParameters> findMatchingSimParams(SimulationParameters searchParams, List<SimulationParameters> candidates){
        // Filter by everything except requestSetId and submittedDate
        return candidates.parallelStream()
                .filter(p -> p.getCompleted().equals(searchParams.getCompleted()))
                .filter(p -> p.getGenerated().equals(searchParams.getGenerated()))
                .filter(p -> p.getAlgorithm().equals(searchParams.getAlgorithm()))
                .filter(p -> p.getProblemClass().equals(searchParams.getProblemClass()))
                .filter(p -> p.getTopologyId().equals(searchParams.getTopologyId()))
                .filter(p -> p.getObjective().equals(searchParams.getObjective()))
                .filter(p -> p.getNumRequests().equals(searchParams.getNumRequests()))
                .filter(p -> p.getNumSources().equals(searchParams.getNumSources()))
                .filter(p -> p.getNumDestinations().equals(searchParams.getNumDestinations()))
                .filter(p -> p.getNumConnections().equals(searchParams.getNumConnections()))
                .filter(p -> p.getMinConnectionsRange().equals(searchParams.getMinConnectionsRange()))
                .filter(p -> p.getMaxConnectionsRange().equals(searchParams.getMaxConnectionsRange()))
                .filter(p -> p.getProcessingType().equals(searchParams.getProcessingType()))
                .filter(p -> p.getPercentSrcAlsoDest().equals(searchParams.getPercentSrcAlsoDest()))
                .filter(p -> p.getSdn().equals(searchParams.getSdn()))
                .filter(p -> p.getUseAws().equals(searchParams.getUseAws()))
                .filter(p -> p.getFailureSetSize().equals(searchParams.getFailureSetSize()))
                .filter(p -> p.getMinMaxFailures().equals(searchParams.getMinMaxFailures()))
                .filter(p -> p.getFailureClass().equals(searchParams.getFailureClass()))
                .filter(p -> p.getFailureProb().equals(searchParams.getFailureProb()))
                .filter(p -> p.getMinMaxFailureProb().equals(searchParams.getMinMaxFailureProb()))
                .filter(p -> p.getNumFailsAllowed().equals(searchParams.getNumFailsAllowed()))
                .filter(p -> p.getMinMaxFailsAllowed().equals(searchParams.getMinMaxFailsAllowed()))
                .filter(p -> p.getPercentSrcFail().equals(searchParams.getPercentSrcFail()))
                .filter(p -> p.getPercentDestFail().equals(searchParams.getPercentDestFail()))
                .filter(p -> p.getIgnoreFailures().equals(searchParams.getIgnoreFailures()))
                .findFirst();
    }





}
