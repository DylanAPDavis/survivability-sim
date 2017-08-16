package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.TestConfiguration;
import netlab.processing.disjointpaths.BhandariService;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.FailureGenerationService;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
@Slf4j
public class PartialBhandariServiceTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private ProcessingService processingService;

    @Test
    public void basicTest(){
        RequestSet requestSet = createRequestSet(1L, "NSFnet", 1, "PartialBhandari", "Flex",
                "TotalCost", 1, 1, 6, new ArrayList<>(), "Node", 1.0,
                new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(),
                2, new ArrayList<>(), "Solo", false, false, 0.0, 0.0, 0.0);
        processingService.processRequestSet(requestSet);
        Map<SourceDestPair, Map<String, Path>> pathMap = requestSet.getRequests().values().iterator().next().getChosenPaths();
        log.info("Failure set: " + requestSet.getRequests().values().iterator().next().getFailures().getFailureSet());
        printMap(pathMap);
    }

    private void printMap(Map<SourceDestPair, Map<String, Path>> pathMap) {
        for(SourceDestPair pair : pathMap.keySet()){
            log.info(String.format("Pair: (%s, %s)", pair.getSrc().getId(), pair.getDst().getId()));
            Map<String, Path> paths = pathMap.get(pair);
            for(String pathId : paths.keySet()){
                log.info(pathId + ": " + paths.get(pathId).toString());
            }
        }
    }

    private RequestSet createRequestSet(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                         String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                         List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                         List<Double> minMaxFailureProb, Integer numConnections,
                                         List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                         Integer numFailsAllowed, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                         Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                         double percentDstFail){

        SimulationParameters params = makeParameters(seed, topologyId, numRequests, alg, problemClass, objective, numSources, numDestinations,
                fSetSize, minMaxFailures, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange, maxConnectionsRange,
                numFailsAllowed, minMaxFailsAllowed, processingType, sdn, useAws, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        RequestSet requestSet = generationService.generateFromSimParams(params);
        return requestSet;
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                Integer numFails, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                                Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                                double percentDstFail){
        return SimulationParameters.builder()
                .seed(seed)
                .topologyId(topologyId)
                .numRequests(numRequests)
                .algorithm(alg)
                .problemClass(problemClass)
                .objective(objective)
                .numSources(numSources)
                .numDestinations(numDestinations)
                .failureSetSize(fSetSize)
                .minMaxFailures(minMaxFailures)
                .failureClass(failureClass)
                .failureProb(failureProb)
                .minMaxFailureProb(minMaxFailureProb)
                .numConnections(numConnections)
                .minConnectionsRange(minConnectionsRange)
                .maxConnectionsRange(maxConnectionsRange)
                .numFailsAllowed(numFails)
                .minMaxFailsAllowed(minMaxFailsAllowed)
                .processingType(processingType)
                .sdn(sdn)
                .useAws(useAws)
                .percentSrcAlsoDest(percentSrcAlsoDest)
                .percentSrcFail(percentSrcFail)
                .percentDestFail(percentDstFail)
                .build();
    }
}
