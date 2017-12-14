package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.TestConfiguration;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import netlab.visualization.PrintingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                .numFailureEvents(1)
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
    public void manycastTest(){

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
                .destFailureType("prevent")
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
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
                .useMinS(2)
                .useMaxS(2)
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
                .numSources(3)
                .numDestinations(3)
                .useMinS(2)
                .useMaxS(2)
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

    @Test
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
