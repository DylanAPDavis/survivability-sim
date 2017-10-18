package netlab.analysis.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AggregateAnalysis;
import netlab.analysis.analyzed.AggregationParameters;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.services.AnalysisService;
import netlab.analysis.services.HashingService;
import netlab.storage.services.StorageService;
import netlab.submission.request.Request;
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
    private AnalysisController(AnalysisService analysisService, StorageService storageService, HashingService hashingService){
        this.analysisService = analysisService;
        this.storageService = storageService;
        this.hashingService = hashingService;
    }

    private AnalysisService analysisService;
    private StorageService storageService;
    private HashingService hashingService;

    @RequestMapping(value = "/analyze", method = RequestMethod.POST)
    @ResponseBody
    public Analysis analyzeRequestSet(AnalysisParameters params){
        Request request = storageService.retrieveRequestSet(params.getRequestSetId(), params.getUseAws());

        Analysis analysis = analysisService.analyzeRequestSet(request);

        // Store the analyzed set
        storageService.storeAnalyzedSet(analysis, params.getUseAws());

        // Return the request set ID
        return analysis;
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
                        Analysis analysis = storageService.retrieveAnalyzedSet(id, true);
                        if(analysis == null) {
                            Request request = storageService.retrieveRequestSet(id, true);
                            if(request != null) {
                                analysis = analysisService.analyzeRequestSet(request);
                                storageService.storeAnalyzedSet(analysis, true);
                            } else{
                                System.out.println("Details Set ID: " + id + " does not exist!");
                            }
                        }
                    });
            System.out.println("Analyzed seed: " + seed);
        }
        return "Success";
    }

    @RequestMapping(value = "/analyze/aggregate", method = RequestMethod.POST)
    @ResponseBody
    public AggregateAnalysis aggregateAnalyzedSets(SimulationParameters params){

        List<Analysis> analyses = storageService.getAnalyzedSets(params);
        return analysisService.aggregateAnalyzedSets(analyses);
    }


    @RequestMapping(value = "/analyze/aggregate_seeds", method = RequestMethod.POST)
    @ResponseBody
    public String aggregateSeeds(AggregationParameters agParams){
        List<List<SimulationParameters>> paramsBySeed = new ArrayList<>();
        Map<String, List<SimulationParameters>> paramMap = new HashMap<>();
        for(Long seed : agParams.getSeeds()) {
            List<SimulationParameters> seedParams = storageService.queryForSeed(seed);
            for(SimulationParameters params : seedParams){
                if(params.getCompleted()){
                    String hash = makeHash(params);
                    paramMap.putIfAbsent(hash, new ArrayList<>());
                    paramMap.get(hash).add(params);
                }
            }
            paramsBySeed.add(seedParams);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        List<AggregateAnalysis> aggregateSets = new ArrayList<>();
        List<SimulationParameters> primaryParamList = paramsBySeed.get(0);
        for(SimulationParameters primaryParams : primaryParamList){
            List<SimulationParameters> paramsToBeAnalyzed = paramMap.get(makeHash(primaryParams));
            List<Analysis> analyses = paramsToBeAnalyzed.stream()
                    .map(p -> storageService.retrieveAnalyzedSet(p.getRequestSetId(), true))
                    .collect(Collectors.toList());
            AggregateAnalysis aggregateAnalysis = analysisService.aggregateAnalyzedSets(analyses);
            aggregateSets.add(aggregateAnalysis);
        }

        return analysisService.aggregateSeeds(agParams, primaryParamList, aggregateSets);

    }

    private String makeHash(SimulationParameters p) {
        return hashingService.hash(p.getTopologyId(), p.getAlgorithm(), p.getProblemClass(), p.getObjective(),
                String.valueOf(p.getPercentSrcAlsoDest()), p.getFailureClass(), String.valueOf(p.getFailureSetSize()),
                String.valueOf(p.getNumFailsAllowed()), String.valueOf(p.getPercentSrcFail()),
                String.valueOf(p.getPercentDestFail()), String.valueOf(p.getIgnoreFailures()), String.valueOf(p.getNumConnections()),
                String.valueOf(p.getMinConnectionsRange()), String.valueOf(p.getMaxConnectionsRange()),
                String.valueOf(p.getNumSources()), String.valueOf(p.getNumDestinations()));
    }

    /*
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
    }*/





}
