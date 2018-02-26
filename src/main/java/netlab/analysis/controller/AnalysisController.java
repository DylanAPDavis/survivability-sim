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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

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
        log.info("Retrieved request for analysis: " + params.getRequestId());
        if(request == null){
            log.info("Request not found! Aborting...");
            return null;
        }
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


    @RequestMapping(value = "/analyze/aggregate_params", method = RequestMethod.POST)
    @ResponseBody
    public String aggregateWithParams(AggregationParameters agParams){
        Map<String, List<Analysis>> analysisMap = new HashMap<>();
        long startTime = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future> futures = new ArrayList<>();
        for(Long seed : agParams.getSeeds()){
            Future f = executor.submit(getAnalysis(seed, analysisMap));
            futures.add(f);
        }
        futures.forEach(f -> {
            try {
                f.get();
            }
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        log.info("Analysis gathering took: " + duration + " seconds");

        // We now has a map of matching analyses, each list should be an analysis per string
        // Use this to get an aggregate analysis per hash
        startTime = System.nanoTime();
        List<Callable<AggregateAnalysis>> aggregateCallables = new ArrayList<>();
        for(String hash : analysisMap.keySet()){
            Callable<AggregateAnalysis> c= aggregate(hash, analysisMap);
            aggregateCallables.add(c);
        }
        Map<String, AggregateAnalysis> aggregateAnalysisMap = null;
        try {
            aggregateAnalysisMap = executor.invokeAll(aggregateCallables)
                    .stream()
                    .map(c -> {
                        try {
                            return c.get();
                        }
                        catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .collect(Collectors.toMap(AggregateAnalysis::getHash, ag -> ag));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        endTime = System.nanoTime();
        duration = (endTime - startTime)/1e9;
        log.info("Aggregation took: " + duration + " seconds");
        if(aggregateAnalysisMap == null){
            log.info("Aggregation Failed!");
            return "Failure!";
        }
        return aggregationAnalysisService.createAggregationOutput(agParams, aggregateAnalysisMap);
    }

    private Callable<AggregateAnalysis> aggregate(String hash, Map<String, List<Analysis>> analysisMap){
        return () -> {
            List<Analysis> analyses = analysisMap.get(hash);
            return aggregationAnalysisService.aggregateAnalyses(hash, analyses);
        };
    }

    private synchronized Runnable getAnalysis(Long seed, Map<String, List<Analysis>> analysisMap){
        return () -> {
            List<SimulationParameters> seedParams = storageService.queryForSeed(seed);
            for(SimulationParameters params : seedParams){
                String id = params.getRequestId();
                if(params.getCompleted()) {
                    Analysis analysis = storageService.retrieveAnalyzedSet(id, true, false);
                    if (analysis != null) {
                        String hash = hashingService.hashAnalysis(analysis);
                        analysisMap.putIfAbsent(hash, new ArrayList<>());
                        analysisMap.get(hash).add(analysis);
                    } else {
                        log.info("Analysis for ID: " + id + " could not be found!");
                    }
                } else{
                    log.info("ID: " + id + " has not completed successfully!");
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
    }


    @RequestMapping(value = "/analyze/aggregate_seeds", method = RequestMethod.POST)
    @ResponseBody
    public String aggregateSeeds(List<Long> seeds){
        AggregationParameters agParams = aggregationAnalysisService.makeDefaultParameters(seeds);
        return aggregateWithParams(agParams);
    }

    @RequestMapping(value = "/analyze/defaultAggregate", method = RequestMethod.GET)
    @ResponseBody
    public AggregationParameters getDefaultAggregateParams(){
        List<Long> range = LongStream.range(1, 31).boxed().collect(Collectors.toList());
        return aggregationAnalysisService.makeDefaultParameters(range);
    }


}
