package netlab.processing;

import netlab.TestConfiguration;
import netlab.processing.shortestPaths.DijkstraService;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyAdjustmentService;
import netlab.topology.services.TopologyService;
import netlab.visualization.PrintingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class DijkstraServiceTest {

    @Autowired
    private DijkstraService dijkstraService;

    @Autowired
    private TopologyService topologyService;

    @Autowired
    ProcessingService processingService;

    @Autowired
    PrintingService printingService;

    @Autowired
    GenerationService generationService;

    @Autowired
    TopologyAdjustmentService topologyAdjustmentService;

    @Test
    public void spTest(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("minimumrisk")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("allLinks")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        Map<Link, Double> riskWeightMap = new HashMap<>();
        Map<String, Failure> failureIdMap = topologyAdjustmentService.createFailureIdMap(request.getDetails().getFailures().getFailureSet());
        for(Link link : topo.getLinks()){
            String linkId = link.getId();
            String origin = link.getOrigin().getId();
            String target = link.getTarget().getId();
            double originProb = failureIdMap.containsKey(origin) ? failureIdMap.get(origin).getProbability() : 0;
            double targetProb = failureIdMap.containsKey(target) ? failureIdMap.get(target).getProbability() : 0;
            double linkProb = 0.0;
            if(failureIdMap.containsKey(linkId)){
                linkProb = failureIdMap.get(linkId).getProbability();
            } else if(failureIdMap.containsKey(link.reverse().getId())){
                linkProb =  failureIdMap.get(link.reverse().getId()).getProbability();
            }
            double runningProb = 1.0;
            runningProb *= (1 -  originProb);
            runningProb *= (1 -  linkProb);
            runningProb *= (1 - targetProb);
            double compoundWeight = 1.0 - runningProb;
            riskWeightMap.put(link, compoundWeight);
        }
        Node src = request.getDetails().getSources().iterator().next();
        Node dst = request.getDetails().getDestinations().iterator().next();
        List<Link> sp = dijkstraService.shortestPath(topo, src, dst, riskWeightMap);
        System.out.println(sp);
    }
}
