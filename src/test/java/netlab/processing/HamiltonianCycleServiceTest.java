package netlab.processing;

import netlab.TestConfiguration;
import netlab.processing.cycles.HamiltonianCycleService;
import netlab.submission.enums.FailureClass;
import netlab.submission.request.*;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyService;
import netlab.visualization.PrintingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class HamiltonianCycleServiceTest {

    @Autowired
    HamiltonianCycleService hamiltonianCycleService;

    @Autowired
    TopologyService topologyService;

    @Autowired
    GenerationService generationService;

    @Autowired
    ProcessingService processingService;

    @Autowired
    PrintingService printingService;

    @Test
    public void NSFCycleTest(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        List<Node> path = hamiltonianCycleService.findHamiltonianCycle(topo);
        assert(path.get(0).equals(path.get(path.size()-1)));
        System.out.println(path);
    }

    @Test
    public void NSFCyclePathMapTest(){
        Topology topo = topologyService.getTopologyById("NSFnet");

        Set<SourceDestPair> pairs = new HashSet<>();
        pairs.add(new SourceDestPair(topo.getNodeById("Palo Alto"), topo.getNodeById("Atlanta")));

        NumFailureEvents nfe = NumFailureEvents.builder()
                .totalNumFailureEvents(2)
                .build();
        Set<Failure> failureSet = topo.getLinks().stream().map(l -> Failure.builder().link(l).build()).collect(Collectors.toSet());
        Failures failures = Failures.builder()
                .failureSet(failureSet)
                .build();
        Details details = Details.builder()
                .failures(failures)
                .numFailureEvents(nfe)
                .pairs(pairs)
                .build();
        Request request = Request.builder()
                .failureClass(FailureClass.Link)
                .details(details)
                .build();
        details = hamiltonianCycleService.solve(request, topo);
        Map<SourceDestPair, Map<String, Path>> pathMap = details.getChosenPaths();
        assert(pathMap.values().stream().allMatch(m -> m.size() == 2));
    }

    @Test
    public void unicastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("hamiltonian")
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
                .algorithm("hamiltonian")
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
    public void manycastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(5L)
                .topologyId("NSFnet")
                .algorithm("hamiltonian")
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
    public void manycastCombineTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(5L)
                .topologyId("NSFnet")
                .algorithm("hamiltonian")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(3)
                .useMinD(2)
                .useMaxD(3)
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
    public void multicastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("hamiltonian")
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
    public void manyToOneTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("hamiltonian")
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
                .algorithm("hamiltonian")
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
                .algorithm("hamiltonian")
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
                .algorithm("hamiltonian")
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
