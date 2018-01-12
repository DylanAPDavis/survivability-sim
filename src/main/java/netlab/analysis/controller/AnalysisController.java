package netlab.analysis.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AggregateAnalysis;
import netlab.analysis.analyzed.AggregationParameters;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.services.AggregationAnalysisService;
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
    private AnalysisController(AnalysisService analysisService, AggregationAnalysisService aggregationAnalysisService,
                               StorageService storageService, HashingService hashingService){
        this.analysisService = analysisService;
        this.aggregationAnalysisService = aggregationAnalysisService;
        this.storageService = storageService;
        this.hashingService = hashingService;
    }

    private AnalysisService analysisService;
    private AggregationAnalysisService aggregationAnalysisService;
    private StorageService storageService;
    private HashingService hashingService;

    @RequestMapping(value = "/analyze", method = RequestMethod.POST)
    @ResponseBody
    public Analysis analyzeRequest(AnalysisParameters params){
        Request request = storageService.retrieveRequestSet(params.getRequestId(), params.getUseAws());
        log.info("Retrived request for analysis: " + params.getRequestId());
        if(!request.isCompleted()) {
            log.info("Request not completed! Aborting...");
            return null;
        }
        Analysis analysis = analysisService.analyzeRequest(request);
        log.info(analysis.toString());
        // Store the analyzed set
        boolean stored = storageService.storeAnalyzedSet(analysis, params.getUseAws());
        if(stored){
            log.info("Analysis storage succeeded!");
        }
        else{
            log.info("Analysis storage FAILED!");
        }
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
                    .map(SimulationParameters::getRequestId)
                    .forEach( id -> {
                        Analysis analysis = storageService.retrieveAnalyzedSet(id, true);
                        if(analysis == null) {
                            Request request = storageService.retrieveRequestSet(id, true);
                            if(request != null) {
                                analysis = analysisService.analyzeRequest(request);
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
    public AggregateAnalysis aggregateAnalyses(SimulationParameters params){

        List<Analysis> analyses = storageService.getAnalyzedSets(params);
        return aggregationAnalysisService.aggregateAnalyses(analyses);
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
                    .map(p -> storageService.retrieveAnalyzedSet(p.getRequestId(), true))
                    .collect(Collectors.toList());
            AggregateAnalysis aggregateAnalysis = aggregationAnalysisService.aggregateAnalyses(analyses);
            aggregateSets.add(aggregateAnalysis);
        }

        return aggregationAnalysisService.aggregateSeeds(agParams, primaryParamList, aggregateSets);

    }

    private String makeHash(SimulationParameters p) {
        return hashingService.hash(p.getTopologyId(), p.getAlgorithm(), p.getProblemClass(), p.getObjective(),
                p.getSourceSubsetDestType(), p.getFailureClass(), String.valueOf(p.getFailureSetSize()),
                String.valueOf(p.getNumFailureEvents()), p.getSourceFailureType(),
                p.getDestFailureType(), String.valueOf(p.getIgnoreFailures()), String.valueOf(p.getMinConnections()),
                String.valueOf(p.getMinPairConnections()), String.valueOf(p.getMaxPairConnections()),
                String.valueOf(p.getNumSources()), String.valueOf(p.getNumDestinations()));
    }

    /*
    public Optional<SimulationParameters> findMatchingSimParams(SimulationParameters searchParams, List<SimulationParameters> candidates){
        // Filter by everything except requestId and submittedDate
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
                .filter(p -> p.getMinConnections().equals(searchParams.getMinConnections()))
                .filter(p -> p.getMinPairConnections().equals(searchParams.getMinPairConnections()))
                .filter(p -> p.getMaxPairConnections().equals(searchParams.getMaxPairConnections()))
                .filter(p -> p.getProcessingType().equals(searchParams.getProcessingType()))
                .filter(p -> p.getPercentSrcAlsoDest().equals(searchParams.getPercentSrcAlsoDest()))
                .filter(p -> p.getSdn().equals(searchParams.getSdn()))
                .filter(p -> p.getUseAws().equals(searchParams.getUseAws()))
                .filter(p -> p.getFailureSetSize().equals(searchParams.getFailureSetSize()))
                .filter(p -> p.getMinMaxFailures().equals(searchParams.getMinMaxFailures()))
                .filter(p -> p.getFailureClass().equals(searchParams.getFailureClass()))
                .filter(p -> p.getFailureProb().equals(searchParams.getFailureProb()))
                .filter(p -> p.getMinMaxFailureProb().equals(searchParams.getMinMaxFailureProb()))
                .filter(p -> p.getNumFailureEvents().equals(searchParams.getNumFailureEvents()))
                .filter(p -> p.getMinMaxFailsAllowed().equals(searchParams.getMinMaxFailsAllowed()))
                .filter(p -> p.getPercentSrcFail().equals(searchParams.getPercentSrcFail()))
                .filter(p -> p.getPercentDstFail().equals(searchParams.getPercentDstFail()))
                .filter(p -> p.getIgnoreFailures().equals(searchParams.getIgnoreFailures()))
                .findFirst();
    }*/





}
