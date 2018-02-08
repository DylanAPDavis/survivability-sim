package netlab.analysis;


import netlab.TestConfiguration;
import netlab.analysis.analyzed.*;
import netlab.analysis.services.AggregationAnalysisService;
import netlab.analysis.services.AnalysisService;
import netlab.processing.ProcessingService;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureScenario;
import netlab.submission.enums.RoutingType;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class ProcessingAndAnalysisTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ProcessingService processingService;

    @Autowired
    private AggregationAnalysisService aggregationAnalysisService;

    @Test
    public void AggregateTest(){
        List<Long> seeds = Arrays.asList(1L);
        AggregationParameters aggregationParameters = aggregationAnalysisService.makeDefaultParameters(seeds);
        List<String> topologyIds = aggregationParameters.getTopologyIds();
        List<RoutingType> routingTypes = aggregationParameters.getRoutingTypes();
        List<FailureScenario> failureScenarios = aggregationParameters.getFailureScenarios();
        List<Integer> nfeValues = aggregationParameters.getNfeValues();
        Map<RoutingType, List<Algorithm>> algorithmMap = aggregationParameters.getAlgorithmMap();
        Map<RoutingType, List<RoutingDescription>> routingDescriptionMap = aggregationParameters.getRoutingDescriptionMap();
        Map<RoutingType, List<TrafficCombinationType>> trafficCombinationTypeMap = aggregationParameters.getTrafficCombinationTypeMap();
        Set<Algorithm> algorithmsThatCanIgnoreFailures = aggregationParameters.getAlgorithmsThatCanIgnoreFailures();
        for(Long seed : seeds){
            for(String topologyId : topologyIds) {
                for (FailureScenario failureScenario : failureScenarios) {
                    for(Integer nfe : nfeValues) {
                        for (RoutingType routingType : routingTypes) {
                            List<Algorithm> algorithms = algorithmMap.get(routingType);
                            List<RoutingDescription> routingDescriptions = routingDescriptionMap.get(routingType);
                            List<TrafficCombinationType> trafficCombinationTypes = trafficCombinationTypeMap.get(routingType);
                            for (Algorithm algorithm : algorithms) {
                                if(algorithm.equals(Algorithm.ILP)){
                                    continue;
                                }
                                for (RoutingDescription routingDescription : routingDescriptions){
                                    for(TrafficCombinationType trafficCombinationType : trafficCombinationTypes){
                                        SimulationParameters params = makeParameters(seed, topologyId, failureScenario,
                                                nfe, routingType, algorithm, routingDescription, trafficCombinationType,
                                                algorithmsThatCanIgnoreFailures);
                                        Request request = generationService.generateFromSimParams(params);
                                        request = processingService.processRequest(request);
                                        Analysis analysis = analysisService.analyzeRequest(request);
                                        assert(analysis.getRunningTime() > 0.0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, FailureScenario failureScenario,
                                                Integer nfe, RoutingType routingType, Algorithm algorithm,
                                                RoutingDescription routingDescription,
                                                TrafficCombinationType trafficCombinationType,
                                                Set<Algorithm> algorithmsThatCanIgnoreFailures){

        return SimulationParameters.builder()
                .seed(seed)
                .topologyId(topologyId)
                .failureScenario(failureScenario.getCode())
                .numFailureEvents(nfe)
                .routingType(routingType.getCode())
                .algorithm(algorithm.getCode())
                .numSources(routingDescription.getNumSources())
                .numDestinations(routingDescription.getNumDestinations())
                .useMinS(routingDescription.getUseMinS())
                .useMaxS(routingDescription.getUseMaxS())
                .useMinD(routingDescription.getUseMinD())
                .useMaxD(routingDescription.getUseMaxD())
                .trafficCombinationType(trafficCombinationType.getCode())
                .ignoreFailures(algorithmsThatCanIgnoreFailures.contains(algorithm))
                .numThreads(8)
                .failureClass("Both")
                .sourceSubsetDestType("none")
                .sourceFailureType("allow")
                .destFailureType("allow")
                .useAws(false)
                .build();
    }
}
