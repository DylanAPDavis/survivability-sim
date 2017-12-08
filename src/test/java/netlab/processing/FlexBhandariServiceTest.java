package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.TestConfiguration;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
@Slf4j
public class FlexBhandariServiceTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private ProcessingService processingService;

    @Test
    public void halfNodeFailTest(){
        Request request = createRequestSet(1L, "NSFnet", 1, "PartialBhandari", "Flex",
                "TotalCost", 1, 1, 6, new ArrayList<>(), "Node", 1.0,
                new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(),
                2, new ArrayList<>(), "Solo", false, false,
                "none", "prevent", "prevent");
        processingService.processRequest(request);
        Map<SourceDestPair, Map<String, Path>> pathMap = request.getDetails().getChosenPaths();
        log.info("Failure set: " + request.getDetails().getFailures().getFailureSet());
        printMap(pathMap);
    }

    @Test
    public void halfLinkFailTest(){
        Request request = createRequestSet(1L, "NSFnet", 1, "PartialBhandari", "Flex",
                "TotalCost", 1, 1, 10, new ArrayList<>(), "Link", 1.0,
                new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(),
                2, new ArrayList<>(), "Solo", false, false,
                "none", "prevent", "prevent");
        processingService.processRequest(request);
        Details details = request.getDetails();
        Map<SourceDestPair, Map<String, Path>> pathMap = details.getChosenPaths();
        printFailureSet(details.getFailures().getFailureSet());
        printMap(pathMap);
    }

    private void printMap(Map<SourceDestPair, Map<String, Path>> pathMap) {
        for(SourceDestPair pair : pathMap.keySet()){;
            Map<String, Path> paths = pathMap.get(pair);
            if(paths.size() > 0) {
                log.info(String.format("Pair: (%s, %s)", pair.getSrc().getId(), pair.getDst().getId()));
                for (String pathId : paths.keySet()) {
                    log.info(pathId + ": " + paths.get(pathId).toString());
                }
            }
        }
    }

    private void printFailureSet(Set<Failure> failures){
        log.info("Failure set: " + failures.stream().map(f -> {
            if(f.getLink() != null){
                return String.format("(%s, %s)", f.getLink().getOrigin().getId(), f.getLink().getTarget().getId());
            }
            else{
                return f.getNode().getId();
            }
        }).collect(Collectors.joining(", ")));
    }

    private Request createRequestSet(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                     String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                     List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                     List<Double> minMaxFailureProb, Integer numConnections,
                                     List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                     Integer numFailsAllowed, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                     Boolean useAws, String sourceSubsetDestType, String sourceFailureType,
                                     String destFailureType){

        SimulationParameters params = makeParameters(seed, topologyId, numRequests, alg, problemClass, objective, numSources, numDestinations,
                fSetSize, minMaxFailures, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange, maxConnectionsRange,
                numFailsAllowed, minMaxFailsAllowed, processingType, sdn, useAws, sourceSubsetDestType, sourceFailureType, destFailureType);
        Request request = generationService.generateFromSimParams(params);
        return request;
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                Integer numFails, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                                Boolean useAws, String sourceSubsetDestType, String sourceFailureType,
                                                String destFailureType){
        return SimulationParameters.builder()
                .seed(seed)
                .topologyId(topologyId)
                .algorithm(alg)
                .problemClass(problemClass)
                .objective(objective)
                .numSources(numSources)
                .numDestinations(numDestinations)
                .failureSetSize(fSetSize)
                .failureClass(failureClass)
                .failureProb(failureProb)
                .minConnections(numConnections)
                .numFailureEvents(numFails)
                .useAws(useAws)
                .sourceSubsetDestType(sourceSubsetDestType)
                .sourceFailureType(sourceFailureType)
                .destFailureType(destFailureType)
                .build();
    }
}
