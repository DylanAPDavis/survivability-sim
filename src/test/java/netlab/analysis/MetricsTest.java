package netlab.analysis;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.controller.AnalysisController;
import netlab.processing.pathmapping.PathMappingService;
import netlab.storage.controller.StorageController;
import netlab.storage.services.StorageService;
import netlab.submission.controller.SubmissionController;
import netlab.submission.request.Failures;
import netlab.submission.request.NumFailureEvents;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.FailureGenerationService;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Link;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import netlab.visualization.PrintingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @Autowired
    FailureGenerationService failureGenerationService;

    @Autowired
    TopologyService topologyService;

    // Baseline Algorithms

    @Test
    public void shortestPathTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
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
    public void shortestPathQuakeTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("quake2")
                .numFailureEvents(2)
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
    public void bhandariQuake1Test(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("bhandari")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("quake1")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        evaluate(params);

    }

    @Test
    public void bhandariMulticastQuake1Test(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("bhandari")
                .objective("totalcost")
                .routingType("multicast")
                .numSources(1)
                .numDestinations(4)
                .failureScenario("quake1")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        evaluate(params);

    }

    @Test
    public void bhandariManycastQuake1Test(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("bhandari")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(4)
                .useMinD(2)
                .failureScenario("quake1")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        evaluate(params);

    }

    @Test
    public void bhandariManycastQuake3Test(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("bhandari")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(4)
                .useMinD(2)
                .failureScenario("quake3")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        evaluate(params);

    }

    @Test
    public void bhandariManycastQuake2Test(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("bhandari")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(4)
                .useMinD(2)
                .failureScenario("quake2")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        evaluate(params);

    }

    @Test
    public void bhandariManycastAllLinksTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("bhandari")
                .objective("totalcost")
                .routingType("manycast")
                .numSources(1)
                .numDestinations(4)
                .useMinD(2)
                .failureScenario("allLinks")
                .numFailureEvents(1)
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

    @Test
    public void hamiltonianNoContentOnPrimaryAfterFailure(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("hamiltonian")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Topology topo = topologyService.getTopologyById("NSFnet");
        Map<String, Link> linkIdMap = topo.getLinkIdMap();
        Set<Failure> failures = new HashSet<>();
        failures.add(Failure.builder().node(null).link(linkIdMap.get("Palo Alto-Salt Lake City")).probability(1.0).build());
        evaluate(params, failures);
    }

    @Test
    public void hamiltonianNoContentAccessAfterFailure(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("hamiltonian")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Topology topo = topologyService.getTopologyById("NSFnet");
        Map<String, Link> linkIdMap = topo.getLinkIdMap();
        Set<Failure> failures = new HashSet<>();
        failures.add(new Failure(null, linkIdMap.get("Palo Alto-Salt Lake City"), 1.0));
        failures.add(new Failure(null, linkIdMap.get("Seattle-Champaign"), 1.0));
        evaluate(params, failures);
    }

    @Test
    public void memberForwardingBroadcastTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(10L)
                .topologyId("NSFnet")
                .algorithm("memberForwarding")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("broadcast")
                .numSources(4)
                .numDestinations(4)
                .sourceSubsetDestType("all")
                .failureScenario("default")
                .numFailureEvents(0)
                .useAws(false)
                .build();
        evaluate(params);

    }

    @Test
    public void memberForwardingBroadcastCombineTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(10L)
                .topologyId("NSFnet")
                .algorithm("memberForwarding")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("broadcast")
                .numSources(4)
                .numDestinations(4)
                .sourceSubsetDestType("all")
                .failureScenario("default")
                .trafficCombinationType("both")
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
                .algorithm("minimumcost")
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
                .algorithm("minimumcost")
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
                .algorithm("minimumcost")
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
                .algorithm("minimumcost")
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
                .algorithm("minimumcost")
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

    @Test
    public void manyToOneTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(4L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("manytoone")
                .numSources(3)
                .numDestinations(1)
                .numFailureEvents(0)
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void manyToOneFailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(4L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("manytoone")
                .numSources(3)
                .numDestinations(1)
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Topology topo = topologyService.getTopologyById("NSFnet");
        Map<String, Link> linkIdMap = topo.getLinkIdMap();
        Set<Failure> failures = new HashSet<>();
        failures.add(new Failure(null, linkIdMap.get("College Park-Ithaca"), 1.0));
        evaluate(params, failures);
    }

    @Test
    public void manyToOneTwoConnectionsCanFailTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(6L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("manytoone")
                .numSources(3)
                .numDestinations(1)
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Topology topo = topologyService.getTopologyById("NSFnet");
        Map<String, Link> linkIdMap = topo.getLinkIdMap();
        Set<Failure> failures = new HashSet<>();
        failures.add(Failure.builder().node(null).link(linkIdMap.get("Lincoln-Champaign")).probability(1.0).build());
        evaluate(params, failures);
    }

    @Test
    public void manyToOneAllConnectionsCanFailTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(9L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("manytoone")
                .numSources(3)
                .numDestinations(1)
                .numFailureEvents(2)
                .useAws(false)
                .build();
        Topology topo = topologyService.getTopologyById("NSFnet");
        Map<String, Link> linkIdMap = topo.getLinkIdMap();
        Set<Failure> failures = new HashSet<>();
        failures.add(new Failure(null, linkIdMap.get("Pittsburgh-Ithaca"), 1.0));
        failures.add(new Failure(null, linkIdMap.get("College Park-Ithaca"), 1.0));
        evaluate(params, failures);
    }

    @Test
    public void manyToOneAllConnectionsFailTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(9L)
                .topologyId("NSFnet")
                .algorithm("minimumcost")
                .objective("totalcost")
                .routingType("manytoone")
                .numSources(3)
                .numDestinations(1)
                .numFailureEvents(1)
                .useAws(false)
                .build();
        Topology topo = topologyService.getTopologyById("NSFnet");
        Map<String, Link> linkIdMap = topo.getLinkIdMap();
        Set<Failure> failures = new HashSet<>();
        failures.add(new Failure(null, linkIdMap.get("Seattle-Champaign"), 1.0));
        failures.add(new Failure(null, linkIdMap.get("Champaign-Pittsburgh"), 1.0));
        failures.add(new Failure(null, linkIdMap.get("Houston-College Park"), 1.0));
        evaluate(params, failures);
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

    @Test
    public void ilpUnicastLink2FailuresTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("TW")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("unicast")
                .numSources(1)
                .numDestinations(1)
                .failureScenario("alllinks")
                .numFailureEvents(2)
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

    @Test
    public void ilpManyToManyDestLinkFailureTrafficTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("TW")
                .algorithm("ilp")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("manytomany")
                .numSources(5)
                .numDestinations(3)
                .useMinS(5)
                .useMaxS(5)
                .useMinD(1)
                .failureScenario("AllLinks")
                .numFailureEvents(1)
                .useAws(false)
                .build();
        evaluate(params);
    }

    @Test
    public void flexBhandariUnicastFailuresTest(){

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
        evaluate(params);
    }

    public void evaluate(SimulationParameters params, Set<Failure> failureSet){
        String requestId = submissionController.submitRequest(params);
        Request request = storageController.getRequest(requestId, false);
        if(!failureSet.isEmpty()){
            updateFailures(request, failureSet);
            storageController.storeRequest(request);
        }

        AnalysisParameters analysisParameters = AnalysisParameters.builder()
                .requestId(requestId)
                .useAws(false)
                .build();

        //assert(request.getDetails().getChosenPaths().values().stream().anyMatch(pm -> pm.values().size() > 0));
        Analysis analysis = analysisController.analyzeRequest(analysisParameters);
        assert(analysis.getSeed() > 0L);
        //System.out.println(analysis.toString());
        //System.out.println(printingService.outputPaths(request));
    }

    public void evaluate(SimulationParameters params){
        evaluate(params, new HashSet<>());
    }

    private void updateFailures(Request request, Set<Failure> failureSet) {
        Failures failures = request.getDetails().getFailures();
        int nfe = request.getDetails().getNumFailureEvents().getTotalNumFailureEvents();
        failures.setFailureSet(failureSet);
        failures.setFailureSetSize(failureSet.size());
        failures.setFailureGroups(failureGenerationService.generateFailureGroups(nfe, failureSet));
    }
}
