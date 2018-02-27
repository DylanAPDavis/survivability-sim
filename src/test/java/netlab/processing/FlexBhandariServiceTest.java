package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.TestConfiguration;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.*;
import netlab.visualization.PrintingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
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
    
    @Autowired
    private PrintingService printingService;

    @Autowired
    private AnalysisService analysisService;

    @Test
    public void unicastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
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
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void unicastFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("quake1")
                .minConnections(1)
                .numFailureEvents(2)
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
    public void anycastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
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
    public void anycastFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(2)
                .failureScenario("quake1")
                .minConnections(1)
                .numFailureEvents(2)
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
    public void anycastTwoQuakesTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(2)
                .failureScenario("quake12")
                .minConnections(1)
                .numFailureEvents(2)
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
    public void anycastAllNodes2Test(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(13)
                .failureScenario("allNodes")
                .minConnections(1)
                .numFailureEvents(2)
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
    public void anycastAllLinks3Test(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(2)
                .failureScenario("allLinks")
                .minConnections(1)
                .numFailureEvents(2)
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
                .algorithm("flexbhandari")
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
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void manycastLinkFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
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
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void manycastNodeFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
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
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void manycastDestsFailTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(2)
                .useMaxD(2)
                .failureScenario("destinations")
                .numFailureEvents(2)
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
    public void manycastSourceFailTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(2)
                .useMaxD(2)
                .failureScenario("sources")
                .numFailureEvents(2)
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
    public void multicastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(2L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
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
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void multicastCombineTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(2L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
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
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }


    @Test
    public void manyToOneTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
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
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void manyToOneSourceFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("manyToOne")
                .numSources(3)
                .numDestinations(1)
                .useMinS(2)
                .failureScenario("sources")
                .numFailureEvents(2)
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
    public void manyToOneDestFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("manyToOne")
                .numSources(3)
                .numDestinations(1)
                .useMinS(2)
                .failureScenario("destinations")
                .numFailureEvents(2)
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
    public void manyToManyAllLinks1TestTW(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("manyToMany")
                .numSources(5)
                .numDestinations(5)
                .failureScenario("allLinks")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        Double totalCost = 0.0;
        for(SourceDestPair pair : request.getDetails().getChosenPaths().keySet()){
            Collection<Path> paths = request.getDetails().getChosenPaths().get(pair).values();
            Double sum = paths.stream().mapToDouble(Path::getTotalWeight).sum();
            totalCost += sum;
        }
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void manyToManyAllNodes1TestTW(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("flexBhandari")
                .objective("totalcost")
                .routingType("manyToMany")
                .numSources(5)
                .numDestinations(5)
                .failureScenario("allNodes")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        Double totalCost = 0.0;
        for(SourceDestPair pair : request.getDetails().getChosenPaths().keySet()){
            Collection<Path> paths = request.getDetails().getChosenPaths().get(pair).values();
            Double sum = paths.stream().mapToDouble(Path::getTotalWeight).sum();
            totalCost += sum;
        }
        assert(request.getDetails().getIsFeasible());
        log.info("Total Cost: " + totalCost);
        System.out.println(printingService.outputPaths(request));
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void manyToManyAllNodes1TestTWUseMinD1(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("flexBhandari")
                .objective("totalcost")
                .routingType("manyToMany")
                .numSources(5)
                .numDestinations(5)
                .useMinD(1)
                .failureScenario("allNodes")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        Double totalCost = 0.0;
        for(SourceDestPair pair : request.getDetails().getChosenPaths().keySet()){
            Collection<Path> paths = request.getDetails().getChosenPaths().get(pair).values();
            Double sum = paths.stream().mapToDouble(Path::getTotalWeight).sum();
            totalCost += sum;
        }
        assert(request.getDetails().getIsFeasible());
        log.info("Total Cost: " + totalCost);
        System.out.println(printingService.outputPaths(request));
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }


    @Test
    public void broadcast(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
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
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void broadcastOverlap(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
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
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void broadcastOverlapFailures(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
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
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }

    @Test
    public void broadcastOverlapLinkFailures(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("broadcast")
                .numSources(4)
                .numDestinations(4)
                .failureScenario("alllinks")
                .numFailureEvents(1)
                .sourceSubsetDestType("half")
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
    public void broadcastOverlapPartialFailures(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("flexbhandari")
                .objective("totalcost")
                .routingType("broadcast")
                .numSources(4)
                .numDestinations(4)
                .failureScenario("quake1")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        System.out.println(printingService.outputFailures(request.getDetails().getFailures().getFailureSet()));
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
        Analysis analysis = analysisService.analyzeRequest(request);
        System.out.println(analysis);
    }
}
