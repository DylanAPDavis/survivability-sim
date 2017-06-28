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
        Map<String, List<AggregateAnalyzedSet>> outputMap = new HashMap<>();
        for(int index = 0; index < firstSeedParams.size(); index++){
            SimulationParameters params = firstSeedParams.get(index);
            AggregateAnalyzedSet aggSet = aggregateSets.get(index);
            String topologyName = params.getTopologyId();
            String algorithm = params.getAlgorithm();
            String problemClass = params.getProblemClass();
            String objective = params.getObjective();
            Double sourceDestOverlap = params.getPercentSrcAlsoDest();
            String failureClass = params.getFailureClass();
            Double failureSetSize = params.getFailureSetSize() * 1.0;
            Double numFailsAllowed = params.getNumFailsAllowed() * 1.0;
            Double percentSrcFail = params.getPercentSrcFail();
            Double percentDstFail = params.getPercentDestFail();
            List<Integer> minC = params.getMinConnectionsRange();
            List<Integer> maxC = params.getMaxConnectionsRange();
            Integer numSources = params.getNumSources();
            Integer numDestinations = params.getNumDestinations();
            String hashString = hash(topologyName, algorithm, problemClass, objective,
                    String.valueOf(sourceDestOverlap), failureClass, String.valueOf(failureSetSize),
                    String.valueOf(numFailsAllowed), String.valueOf(percentSrcFail), String.valueOf(percentDstFail),
                    String.valueOf(minC), String.valueOf(maxC), String.valueOf(numSources), String.valueOf(numDestinations));
            outputMap.putIfAbsent(hashString, new ArrayList<>());
            outputMap.get(hashString).add(aggSet);
        }
        for(String topology : agParams.getTopologyIds()){
            for(String algorithm : agParams.getAlgorithms()){
                for(String problemClass : agParams.getProblemClasses()){
                    for(String objective : agParams.getObjectives()){
                        for(Double percentSrcAlsoDest : agParams.getPercentSrcAlsoDests()){
                            for(String failureClass : agParams.getFailureMap().keySet()){
                                List<List<Double>> allParamsPerClass = agParams.getFailureMap().get(failureClass);
                                for(List<Double> failureParams : allParamsPerClass){
                                    Double numFails = failureParams.get(0);
                                    Double numFailsAllowed = failureParams.get(1);
                                    Double srcFailPercent = failureParams.get(2);
                                    Double dstFailPercent = failureParams.get(3);
                                    for(List<Integer> minC : agParams.getMinConnectionRanges()){
                                        for(List<Integer> maxC : agParams.getMaxConnectionRanges()){
                                            for(Integer numS : agParams.getNumSources()){
                                                for(Integer numD : agParams.getNumDestinations()){
                                                    String hashString = hash(topology, algorithm, problemClass, objective,
                                                            String.valueOf(percentSrcAlsoDest), failureClass,
                                                            String.valueOf(numFails), String.valueOf(numFailsAllowed),
                                                            String.valueOf(srcFailPercent), String.valueOf(dstFailPercent),
                                                            String.valueOf(minC), String.valueOf(maxC),
                                                            String.valueOf(numS), String.valueOf(numD));
                                                    List<AggregateAnalyzedSet> agSets = outputMap.getOrDefault(hashString, new ArrayList<>());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return "";
    }

    private String hash(String... args){
        return String.join("_", args);
    }

    private String[] unhash(String hashString){
        return hashString.split("_");
    }



}
