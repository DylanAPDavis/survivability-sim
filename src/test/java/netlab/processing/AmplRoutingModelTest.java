package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.TestConfiguration;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import netlab.visualization.PrintingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
@Slf4j
public class AmplRoutingModelTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private ProcessingService processingService;

    @Autowired
    private PrintingService printingService;

    @Autowired
    private TopologyService topologyService;

    @Autowired
    private AnalysisService analysisService;

    @Test
    public void unicastTest(){

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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastTestTW(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastSourceTrafficTest(){

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
                .trafficCombinationType("source")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastLinkFailuresTest(){

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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastLinkFailuresTestTW(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastDestTrafficLinkFailuresTest(){

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
                .trafficCombinationType("dest")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastDestTraffic3LinkFailuresTest(){

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
                .numFailureEvents(3)
                .trafficCombinationType("dest")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(!request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastNodeFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastNodeFailuresTestTW(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastQuake1FailuresTestTW(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("quake1")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastBothTrafficNodeFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .trafficCombinationType("both")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void anycastTest(){

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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void anycastLinkFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(3)
                .failureScenario("alllinks")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void anycast2LinkFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(2)
                .failureScenario("alllinks")
                .trafficCombinationType("both")
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        Topology topo = topologyService.getTopologyById("NSFnet");
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void anycast3D2LinkFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(3)
                .failureScenario("alllinks")
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void anycastQuake2FailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(3)
                .failureScenario("quake2")
                .numFailureEvents(2)
                .ignoreFailures(false)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void anycastNodeFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(3)
                .failureScenario("allnodes")
                .trafficCombinationType("both")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void anycast2NodeFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(3)
                .failureScenario("allnodes")
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }


    @Test
    public void anycastNodeNoDestFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(2)
                .failureSetSize(11)
                .failureScenario("allNodes")
                .destFailureType("prevent")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void anycastOnlyMembersFail(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(3)
                .failureScenario("members")
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }


    @Test
    public void manycastTest(){

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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manycastSourceTrafficTest(){

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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manycastDestTrafficTest(){

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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manycastBothTrafficTest(){

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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manycastLinkFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(2)
                .useMaxD(2)
                .failureScenario("alllinks")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manycastNodeFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(2L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(2)
                .useMaxD(2)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void multicastNodeFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("multicast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(3)
                .useMaxD(3)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void multicastSourceTrafficNodeFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(6L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("multicast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(3)
                .useMaxD(3)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .useAws(false)
                .destFailureType("prevent")
                .trafficCombinationType("source")
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void multicastDestTrafficNodeFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(6L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("multicast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(3)
                .useMaxD(3)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .useAws(false)
                .destFailureType("prevent")
                .trafficCombinationType("dest")
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void multicastNoTrafficCombinationTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(6L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("multicast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(3)
                .useMaxD(3)
                .numFailureEvents(0)
                .useAws(false)
                .destFailureType("prevent")
                .trafficCombinationType("none")
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manyToOneSourceTrafficTest(){

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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manyToOneDestTrafficTest(){

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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manyToOneBothTrafficTest(){

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
                .trafficCombinationType("both")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manyToManyAllLinks1TestTW(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("ilp")
                .objective("totalcost")
                .routingType("manyToMany")
                .numSources(5)
                .useMinS(5)
                .numDestinations(3)
                .failureScenario("allLinks")
                .trafficCombinationType("none")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void manyToOneNodeFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manytoone")
                .numSources(3)
                .numDestinations(1)
                .useMinS(3)
                .useMaxS(3)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manyToManyNodeFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manytomany")
                .numSources(7)
                .numDestinations(7)
                .useMinS(6)
                .useMaxS(7)
                .useMinD(6)
                .useMaxD(7)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manyToManyAllSrcsNodeFailuresTest(){
        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manytomany")
                .numSources(7)
                .numDestinations(7)
                .useMinS(7)
                .useMaxS(7)
                .useMinD(1)
                .useMaxD(7)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    // 1_tw_manytomany_ilp_5_1_5_5_1_1_none_alllinks_both_1_none_allow_allow_false_8
    @Test
    public void manyToManyTwAllLinks1S5D1(){
        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manytomany")
                .numSources(5)
                .numDestinations(1)
                .useMinS(5)
                .useMaxS(5)
                .useMinD(1)
                .useMaxD(1)
                .failureScenario("alllinks")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manyToManyTwQuake2S5D3(){
        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manytomany")
                .numSources(5)
                .numDestinations(3)
                .useMinS(5)
                .useMaxS(5)
                .useMinD(1)
                .useMaxD(3)
                .failureScenario("quake2")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void broadcastNoOverlap(){

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
                .sourceSubsetDestType("none")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void broadcastSourceTrafficNoOverlap(){

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
                .sourceSubsetDestType("none")
                .trafficCombinationType("source")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void broadcastDestTrafficNoOverlap(){

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
                .sourceSubsetDestType("none")
                .trafficCombinationType("dest")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void broadcastBothTrafficNoOverlap(){

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
                .sourceSubsetDestType("none")
                .trafficCombinationType("both")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void broadcastHalfOverlap(){

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
                .sourceSubsetDestType("half")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void broadcastFullOverlap(){

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
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    //@Test
    public void broadcastFullOverlapNodesFail(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("broadcast")
                .numSources(4)
                .numDestinations(4)
                .failureScenario("allnodes")
                .sourceFailureType("prevent")
                .destFailureType("prevent")
                .numFailureEvents(1)
                .sourceSubsetDestType("all")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }



}
