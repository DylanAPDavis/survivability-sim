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

import java.util.Collection;
import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
@Slf4j
public class TabuSearchServiceTest {

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
                .algorithm("tabu")
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
    public void unicastAllLinksTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("tabu")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("allLinks")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastAllLinks2Test(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("tabu")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("allLinks")
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastAllLinks1TestTW(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("tabu")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("allLinks")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void unicastAllLinks2TestTW(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("tabu")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("allLinks")
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        assert(request.getDetails().getChosenPaths().values().stream().anyMatch(v -> v.size() == 2));
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void anycastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("tabu")
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
    }

    @Test
    public void anycastAllLinksTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("tabu")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(13)
                .failureScenario("allLinks")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void anycastAllNodesTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("tabu")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(13)
                .failureScenario("allNodes")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Request request = generationService.generateFromSimParams(params);
        request = processingService.processRequest(request);
        assert(request.getDetails().getIsFeasible());
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void anycastAllNodes2FailsTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("tabu")
                .objective("totalcost")
                .routingType("anycast")
                .numSources(1)
                .numDestinations(13)
                .failureScenario("allNodes")
                .numFailureEvents(2)
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
                .algorithm("tabu")
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
        log.info("Total Cost: " + totalCost);
        System.out.println(printingService.outputPaths(request));
    }

    @Test
    public void manyToManyAllNodes1TestTW(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("tw")
                .algorithm("tabu")
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
    }

}
