package netlab.processing;


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

import java.util.Map;
import java.util.Random;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class AmplServiceTest {


    private TopologyService topologyService;

    private AmplService amplService;

    private GenerationService generationService;

    @Autowired
    public AmplServiceTest(TopologyService topologyService, AmplService amplService, GenerationService generationService) {
        this.topologyService = topologyService;
        this.amplService = amplService;
        this.generationService = generationService;
    }

    @Test
    public void solve(){

        SimulationParameters params = new SimulationParameters();
        Topology topo = topologyService.getTopologyById("NSFnet");
        Map<String, Request> requests = generationService.createRequestsFromParameters(params);
        for(Request r : requests.values()){
            amplService.solve(r, topo);
        }
    }
}
