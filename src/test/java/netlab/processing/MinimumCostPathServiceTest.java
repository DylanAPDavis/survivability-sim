package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.visualization.PrintingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class MinimumCostPathServiceTest {

    @Autowired
    ProcessingService processingService;

    @Autowired
    PrintingService printingService;

    @Autowired
    GenerationService generationService;

    @Autowired
    AnalysisService analysisService;

    @Test
    public void unicastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .numFailureEvents(0)
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
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(3)
                .numFailureEvents(0)
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
    public void manycastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(2)
                .useMaxD(3)
                .numFailureEvents(0)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void multicastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(4L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("multicast")
                .numSources(1)
                .numDestinations(3)
                .numFailureEvents(0)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void multicastCombineTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(4L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("multicast")
                .numSources(1)
                .numDestinations(3)
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
    public void manyToOneTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("manyToOne")
                .numSources(3)
                .numDestinations(1)
                .useMinS(2)
                .numFailureEvents(0)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void broadcast(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("broadcast")
                .numSources(3)
                .numDestinations(3)
                .numFailureEvents(0)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void broadcastOverlap(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("broadcast")
                .numSources(4)
                .numDestinations(4)
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
    public void broadcastOverlapFailures(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("broadcast")
                .numSources(4)
                .numDestinations(4)
                .failureScenario("allnodes")
                .numFailureEvents(1)
                .sourceSubsetDestType("half")
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }
}
