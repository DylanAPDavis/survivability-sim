package netlab.analysis;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.controller.AnalysisController;
import netlab.processing.pathmapping.PathMappingService;
import netlab.storage.controller.StorageController;
import netlab.storage.services.StorageService;
import netlab.submission.controller.SubmissionController;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.visualization.PrintingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class MetricsTest {

    @Autowired
    SubmissionController submissionController;

    @Autowired
    AnalysisController analysisController;

    @Autowired
    StorageController storageController;

    @Autowired
    PrintingService printingService;

    // Baseline Algorithms

    @Test
    public void shortestPathTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("shortestpath")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("default")
                .numFailureEvents(0)
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void bhandariTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("bhandari")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("default")
                .numFailureEvents(0)
                .useAws(false)
                .build();
        evaluate(params);

    }

    @Test
    public void overlappingTreesTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("overlappingTrees")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("default")
                .numFailureEvents(0)
                .useAws(false)
                .build();
        evaluate(params);

    }

    @Test
    public void hamiltonianTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("hamiltonian")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("default")
                .numFailureEvents(0)
                .useAws(false)
                .build();
        evaluate(params);

    }

    // Baseline Algorithms with Failure
    @Test
    public void shortestPathFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("shortestpath")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("allLinks")
                .numFailureEvents(5)
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void bhandariFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("bhandari")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("allLinks")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        evaluate(params);

    }


    // Baseline Algorithms with TrafficCombination
    @Test
    public void multicastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(4L)
                .topologyId("NSFnet")
                .algorithm("shortestpath")
                .objective("totalcost")
                .routingType("multicast")
                .numSources(1)
                .numDestinations(3)
                .numFailureEvents(0)
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void multicastCombineTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(4L)
                .topologyId("NSFnet")
                .algorithm("shortestpath")
                .objective("totalcost")
                .routingType("multicast")
                .numSources(1)
                .numDestinations(3)
                .numFailureEvents(0)
                .trafficCombinationType("source")
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void multicastCombineDestTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(4L)
                .topologyId("NSFnet")
                .algorithm("shortestpath")
                .objective("totalcost")
                .routingType("multicast")
                .numSources(1)
                .numDestinations(3)
                .numFailureEvents(0)
                .trafficCombinationType("dest")
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void multicastCombineBothTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(4L)
                .topologyId("NSFnet")
                .algorithm("shortestpath")
                .objective("totalcost")
                .routingType("multicast")
                .numSources(1)
                .numDestinations(3)
                .numFailureEvents(0)
                .trafficCombinationType("both")
                .useAws(false)
                .build();
        evaluate(params);
    }
    // ILPs
    @Test
    public void ilpUnicastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureSetSize(0)
                .failureClass("both")
                .numFailureEvents(0)
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void ilpAnycastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(3)
                .failureScenario("default")
                .numFailureEvents(0)
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void ilpBoadcastFullOverlap(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("broadcast")
                .numSources(4)
                .numDestinations(4)
                .failureScenario("default")
                .numFailureEvents(0)
                .sourceSubsetDestType("all")
                .useAws(false)
                .build();
        evaluate(params);
    }

    // ILPs with Failure
    @Test
    public void ilpUnicastLinkFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("alllinks")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        evaluate(params);
    }

    // ILPs with TrafficCombination
    @Test
    public void ilpManycastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(6L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(2)
                .useMaxD(2)
                .failureScenario("default")
                .numFailureEvents(0)
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void ilpManycastSourceTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(6L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(2)
                .useMaxD(2)
                .failureScenario("default")
                .numFailureEvents(0)
                .trafficCombinationType("source")
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void ilpManycastDestTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(6L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(2)
                .useMaxD(2)
                .failureScenario("default")
                .numFailureEvents(0)
                .trafficCombinationType("dest")
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void ilpManycastBothTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(6L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(2)
                .useMaxD(2)
                .failureScenario("default")
                .numFailureEvents(0)
                .trafficCombinationType("both")
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void ilpManyToOneSourceTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manytoone")
                .numSources(3)
                .numDestinations(1)
                .useMinS(2)
                .useMaxS(2)
                .numFailureEvents(0)
                .trafficCombinationType("source")
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void ilpManyToOneDestTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manytoone")
                .numSources(3)
                .numDestinations(1)
                .useMinS(2)
                .useMaxS(2)
                .numFailureEvents(0)
                .trafficCombinationType("dest")
                .useAws(false)
                .build();
        evaluate(params);
    }


    public void evaluate(SimulationParameters params){
        String requestId = submissionController.submitRequest(params);
        Request request = storageController.getRequest(requestId, false);

        AnalysisParameters analysisParameters = AnalysisParameters.builder()
                .requestId(requestId)
                .useAws(false)
                .build();

        Analysis analysis = analysisController.analyzeRequest(analysisParameters);
        System.out.println(analysis.toString());
        System.out.println(printingService.outputPaths(request));
    }
}
