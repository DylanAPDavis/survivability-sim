package netlab.analysis.controller;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.*;
import netlab.analysis.services.AggregationAnalysisService;
import netlab.analysis.services.AggregationOutputService;
import netlab.analysis.services.AnalysisService;
import netlab.analysis.services.HashingService;
import netlab.storage.services.StorageService;
import netlab.submission.enums.RoutingType;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
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
                               StorageService storageService, HashingService hashingService, AggregationOutputService aggregationOutputService){
        this.analysisService = analysisService;
        this.aggregationAnalysisService = aggregationAnalysisService;
        this.storageService = storageService;
        this.hashingService = hashingService;
        this.aggregationOutputService = aggregationOutputService;
    }

    private AnalysisService analysisService;
    private AggregationAnalysisService aggregationAnalysisService;
    private AggregationOutputService aggregationOutputService;
    private StorageService storageService;
    private HashingService hashingService;

    @RequestMapping(value = "/analyze", method = RequestMethod.POST)
    @ResponseBody
    public Analysis analyzeRequest(@RequestBody AnalysisParameters params){
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

    public void massAnalysis(MassAnalysisParameters massAnalysisParameters) {
        Long seed = massAnalysisParameters.getSeed();
        String topology = massAnalysisParameters.getTopology();
        String routingType = massAnalysisParameters.getRoutingType();
        List<Long> seeds = Collections.singletonList(seed);
        analyzeSeeds(seeds, routingType, topology);
    }

    @RequestMapping(value="/analyze_seed", method = RequestMethod.POST)
    @ResponseBody
    public String analyzeSeeds(@RequestBody List<Long> seeds, String routingType, String topologyId){
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Callable<Analysis>> analysisCallables = new ArrayList<>();
        for(Long seed : seeds) {
            List<SimulationParameters> seedParams = storageService.queryForSeed(seed);
            seedParams.stream()
                    .filter(SimulationParameters::getCompleted)
                    .filter(SimulationParameters::getUseAws)
                    .filter(sp -> sp.getRoutingType().toLowerCase().equals(routingType.toLowerCase()))
                    .filter(sp -> sp.getTopologyId().toLowerCase().equals(topologyId.toLowerCase()))
                    .filter(sp -> sp.getAlgorithm().toLowerCase().equals("ilp"))    // TEMP
                    .filter(sp -> sp.getNumFailureEvents().equals(2))              // TEMP
                    .map(SimulationParameters::getRequestId)
                    .forEach( id -> {
                        analysisCallables.add(analyze(id));
                    });
            try {
                Set<Analysis> analyses = executor.invokeAll(analysisCallables)
                        .stream()
                        .map(c -> {
                            try {
                                return c.get();
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .collect(Collectors.toSet());
                System.out.println("Analyzed seed: " + seed);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return "Success";
    }

    private Callable<Analysis> analyze(String id){
        return () -> {
            Analysis analysis = storageService.retrieveAnalyzedSet(id, true);
            if(analysis == null) {
                Request request = storageService.retrieveRequestSet(id, true);
                if(request != null) {
                    analysis = analysisService.analyzeRequest(request);
                    storageService.storeAnalyzedSet(analysis, true);
                    //System.out.println(id + " analyzed!");
                } else{
                    System.out.println("Details Set ID: " + id + " does not exist!");
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return analysis;
        };
    }


    @RequestMapping(value = "/analyze/aggregate_params", method = RequestMethod.POST)
    @ResponseBody
    public String aggregateWithParams(@RequestBody AggregationParameters agParams){
        long startTime = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        Map<String, List<Analysis>> analysisMap = buildAnalysisMap(agParams.getSeeds(), agParams.getRoutingTypes(), agParams.getTopologyIds(), executor);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        log.info("Analysis gathering took: " + duration + " seconds");
        if(analysisMap == null || analysisMap.isEmpty()){
            log.info("Analysis gathering failed! Aborting...");
            return "Failure!";
        }

        // We now has a map of matching analyses, each list should be an analysis per string
        // Use this to get an aggregate analysis per hash
        startTime = System.nanoTime();
        Map<String, AggregateAnalysis> aggregateAnalysisMap = buildAggregateAnalysisMap(analysisMap, executor);
        endTime = System.nanoTime();
        duration = (endTime - startTime)/1e9;
        log.info("Aggregation took: " + duration + " seconds");
        if(aggregateAnalysisMap == null){
            log.info("Aggregation Failed!");
            return "Failure!";
        }
        //return aggregationAnalysisService.createAggregationOutput(agParams, aggregateAnalysisMap);
        return aggregationOutputService.createAltAggregationOutput(aggregateAnalysisMap);
    }

    private Map<String, List<Analysis>> buildAnalysisMap(List<Long> seeds, List<RoutingType> routingTypes,
                                                         List<String> topologyIds, ExecutorService executor){
        List<Callable<Map<String, List<Analysis>>>> analysisCallables = new ArrayList<>();
        for(RoutingType routingType : routingTypes) {
            for(String topologyId : topologyIds) {
                for (Long seed : seeds) {
                    Callable<Map<String, List<Analysis>>> c = getAnalysis(seed, routingType, topologyId);
                    analysisCallables.add(c);
                }
            }
        }
        Set<Map<String, List<Analysis>>> analysisMaps = null;
        try {
            analysisMaps = executor.invokeAll(analysisCallables)
                    .stream()
                    .map(c -> {
                        try {
                            return c.get();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .collect(Collectors.toSet());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(analysisMaps == null){
            return null;
        }
        Map<String, List<Analysis>> totalMap = new HashMap<>();
        for(Map<String, List<Analysis>> analysisMap : analysisMaps){
            for(String hash : analysisMap.keySet()){
                totalMap.putIfAbsent(hash, new ArrayList<>());
                totalMap.get(hash).addAll(analysisMap.get(hash));
            }
        }
        return totalMap;
    }

    private Map<String, AggregateAnalysis> buildAggregateAnalysisMap(Map<String, List<Analysis>> analysisMap,
                                                                     ExecutorService executor){
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
        return aggregateAnalysisMap;
    }

    private Callable<AggregateAnalysis> aggregate(String hash, Map<String, List<Analysis>> analysisMap){
        return () -> {
            List<Analysis> analyses = analysisMap.get(hash);
            return aggregationAnalysisService.aggregateAnalyses(hash, analyses);
        };
    }

    private Callable<Map<String, List<Analysis>>> getAnalysis(Long seed, RoutingType routingType, String topologyId){
        return () -> {
            Map<String, List<Analysis>> analysisMap = new HashMap<>();
            List<SimulationParameters> seedParams = storageService.queryForSeed(seed);
            for(SimulationParameters params : seedParams){
                if(params.getRoutingType().toLowerCase().equals(routingType.getCode().toLowerCase())
                        && params.getTopologyId().toLowerCase().equals(topologyId.toLowerCase())) {
                    String id = params.getRequestId();
                    if (params.getCompleted()) {
                        Analysis analysis = storageService.retrieveAnalyzedSet(id, true, false);
                        if (analysis != null) {
                            String hash = hashingService.hashAnalysis(analysis);
                            analysisMap.putIfAbsent(hash, new ArrayList<>());
                            analysisMap.get(hash).add(analysis);
                        } else {
                            log.info("Analysis for ID: " + id + " could not be found!");
                        }
                    } else {
                        log.info("ID: " + id + " has not completed successfully!");
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return analysisMap;
        };
    }


    @RequestMapping(value = "/analyze/aggregate_seeds", method = RequestMethod.POST)
    @ResponseBody
    public String aggregateSeeds(@RequestBody List<Long> seeds){
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
