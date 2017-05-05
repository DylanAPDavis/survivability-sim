package netlab.processing;


import lombok.NonNull;
import netlab.TestConfiguration;
import netlab.processing.ampl.AmplService;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.Request;
import netlab.submission.request.RequestSet;
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

        String problemClass = "Flow";
        SimulationParameters params = makeParameters(2L, "NSFnet", 1, "ServiceILP", problemClass,
                3, 3, null, Arrays.asList(0, 1), "Both", 1.0, new ArrayList<>(),
                10, Arrays.asList(0, 0), Arrays.asList(1, 1), null, Arrays.asList(1, 2), "Solo",
                false, false, 0.0, 0.0, 0.0);
        Topology topo = topologyService.getTopologyById("NSFnet");
        RequestSet requestSet = generationService.generateRequests(params);
        for(Request request : requestSet.getRequests().values()){
            amplService.solve(request, requestSet.getProblemClass(), topo);
        }
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                Integer numSources, Integer numDestinations, Integer numFailures,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                Integer numFails, List<Integer> minMaxFails, String processingType, Boolean sdn,
                                                Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                                double percentDstFail){
        return SimulationParameters.builder()
                .seed(seed)
                .topologyId(topologyId)
                .numRequests(numRequests)
                .algorithm(alg)
                .problemClass(problemClass)
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
                .numFails(numFails)
                .minMaxFails(minMaxFails)
                .processingType(processingType)
                .sdn(sdn)
                .useAws(useAws)
                .percentSrcAlsoDest(percentSrcAlsoDest)
                .percentSrcFail(percentSrcFail)
                .percentDestFail(percentDstFail)
                .build();
    }
}
