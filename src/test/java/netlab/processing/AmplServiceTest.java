package netlab.processing;


import lombok.NonNull;
import netlab.TestConfiguration;
import netlab.processing.ampl.AmplService;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class AmplServiceTest {

    @Autowired
    private TopologyService topologyService;

    @Autowired
    private AmplService amplService;

    @Autowired
    private GenerationService generationService;


    @Test
    public void solve(){


        SimulationParameters params = makeParameters(2L, "NSFnet", 1, "FixedILP",
                3, 3, null, Arrays.asList(1, 2), "Both", 1.0, new ArrayList<>(),
                null, Arrays.asList(0, 1), Arrays.asList(1, 2), null, Arrays.asList(1, 2), "Solo",
                false, false, false, false, "Partial");
        Topology topo = topologyService.getTopologyById("NSFnet");
        Map<String, Request> requests = generationService.createRequestsFromParameters(params);
        for(Request r : requests.values()){
            amplService.solve(r, topo);
        }
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg,
                                                Integer numSources, Integer numDestinations, Integer numFailures,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                Integer numCuts, List<Integer> minMaxCuts, String processingType, Boolean sdn,
                                                Boolean useAws, Boolean srcFailuresAllowed, Boolean dstFailuresAllowed,
                                                String srcDestOverlap){
        return SimulationParameters.builder()
                .seed(seed)
                .topologyId(topologyId)
                .numRequests(numRequests)
                .algorithm(alg)
                .numSources(numSources)
                .numDestinations(numDestinations)
                .numFailures(numFailures)
                .minMaxFailures(minMaxFailures)
                .failureClass(failureClass)
                .failureProb(failureProb)
                .minMaxFailureProb(minMaxFailureProb)
                .numConnections(numConnections)
                .minConnectionsRange(minConnectionsRange)
                .maxConnectionsRange(maxConnectionsRange)
                .numCuts(numCuts)
                .minMaxCuts(minMaxCuts)
                .processingType(processingType)
                .sdn(sdn)
                .useAws(useAws)
                .srcFailuresAllowed(srcFailuresAllowed)
                .dstFailuresAllowed(dstFailuresAllowed)
                .srcDstOverlap(srcDestOverlap)
                .build();
    }
}
