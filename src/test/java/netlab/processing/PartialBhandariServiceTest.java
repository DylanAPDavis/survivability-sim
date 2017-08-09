package netlab.processing;

import netlab.TestConfiguration;
import netlab.processing.disjointpaths.BhandariService;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.FailureGenerationService;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Node;
import netlab.topology.elements.Topology;
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
public class PartialBhandariServiceTest {

    @Autowired
    private BhandariService bhandariService;

    @Autowired
    private GenerationService generationService;

    @Test
    private void basicTest(){
        RequestSet requestSet = createRequestSet(1L, "NSFnet", 1, "PartialBhandari", "Flex",
                "TotalCost", 1, 1, 14, new ArrayList<>(), "Link", 1.0,
                new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(),
                2, new ArrayList<>(), "Solo", false, false, 0.0, 0.0, 0.0);
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
