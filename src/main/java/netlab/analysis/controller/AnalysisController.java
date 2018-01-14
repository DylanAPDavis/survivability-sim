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
    public String aggregateSeeds(List<Long> seeds){
        AggregationParameters agParams = aggregationAnalysisService.makeDefaultParameters(seeds);
        Map<String, List<Analysis>> analysisMap = new HashMap<>();
        for(Long seed : agParams.getSeeds()){
            List<SimulationParameters> seedParams = storageService.queryForSeed(seed);
            for(SimulationParameters params : seedParams){
                String id = params.getRequestId();
                Analysis analysis = storageService.retrieveAnalyzedSet(id, true, true);
                if(analysis != null) {
                    String hash = hashingService.hashAnalysis(analysis);
                    analysisMap.putIfAbsent(hash, new ArrayList<>());
                    analysisMap.get(hash).add(analysis);
                } else{
                    log.info("Analysis for ID: " + id + " could not be found!");
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // We now has a map of matching analyses, each list should be an analysis per string
        // Use this to get an aggregate analysis per hash
        Map<String, AggregateAnalysis> aggregateAnalysisMap = new HashMap<>();
        for(String hash : analysisMap.keySet()){
            List<Analysis> analyses = analysisMap.get(hash);
            AggregateAnalysis aggregateAnalysis = aggregationAnalysisService.aggregateAnalyses(analyses);
            aggregateAnalysisMap.put(hash, aggregateAnalysis);
        }

        return aggregationAnalysisService.createAggregationOutput(agParams, aggregateAnalysisMap);

    }





}
