package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.analyzed.RequestMetrics;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.topology.services.TopologyService;
import netlab.visualization.VisualizationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class FlowAndEndpointFailuresTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ProcessingService processingService;

    @Autowired
    private TopologyService topoService;

    @Autowired
    private VisualizationService visualizationService;


    @Test
    public void endpointOneNFAMaxOneC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Map<String, Integer> srcMinNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 0));
        Map<String, Integer> srcMaxNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 1));
        Map<String, Integer> dstMinNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 0));
        Map<String, Integer> dstMaxNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 1));
        Map<String, Set<String>> srcFailureMap = sources.stream()
                .collect(Collectors.toMap(s -> s, s -> pickFailureNodes(Collections.singletonList(s), destinations)));
        Map<String, Integer> srcNFAMap = sources.stream()
                .collect(Collectors.toMap(s -> s, s -> 1));
        Map<String, Set<String>> dstFailureMap = destinations.stream()
                .collect(Collectors.toMap(d -> d, d -> pickFailureNodes(Collections.singletonList(d), destinations)));
        Map<String, Integer> dstNFAMap = destinations.stream()
                .collect(Collectors.toMap(d -> d, d -> 1));
        RequestSet ers = createSetWithEndpoints(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
                dstMinNumConnections, dstMaxNumConnections, 5, srcFailureMap, srcNFAMap, dstFailureMap, dstNFAMap);
        analyze(ers, Arrays.asList(10), true, true);
    }

    @Test
    public void endpointOneNFAMaxOneCAllLinksFail(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Map<String, Integer> srcMinNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 0));
        Map<String, Integer> srcMaxNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 1));
        Map<String, Integer> dstMinNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 0));
        Map<String, Integer> dstMaxNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 1));
        Set<String> failures = topoService.getTopologyById("NSFnet").getLinkIdMap().keySet();
        Integer nfa = 1;
        Map<String, Set<String>> srcFailureMap = sources.stream().collect(Collectors.toMap(s -> s, s -> failures));
        Map<String, Integer> srcNFAMap = sources.stream().collect(Collectors.toMap(s -> s, s -> nfa));
        Map<String, Set<String>> dstFailureMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> failures));
        Map<String, Integer> dstNFAMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> nfa));
        RequestSet ers = createSetWithEndpoints(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
                dstMinNumConnections, dstMaxNumConnections, 5, srcFailureMap, srcNFAMap, dstFailureMap, dstNFAMap);
        analyze(ers, Arrays.asList(10), true, true);
    }

    @Test
    public void endpointVariedSrcNFAMaxThreeC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Boulder", "Houston"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Lincoln", "Salt Lake City"));
        Map<String, Integer> srcMinNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 0));
        Map<String, Integer> srcMaxNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 3));
        Map<String, Integer> dstMinNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 0));
        Map<String, Integer> dstMaxNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 3));

        List<String> sourceFailures = Arrays.asList("Champaign", "San Diego-Houston", "Houston-Boulder", "Lincoln-Boulder");
        Map<String, Set<String>> srcFailureMap = sources.stream()
                .collect(Collectors.toMap(s -> s, s -> pickFailureNodes(Collections.singletonList(s), sourceFailures)));
        Map<String, Integer> srcNFAMap = new HashMap<>();
        srcNFAMap.put("Boulder", 2);
        srcNFAMap.put("Houston", 3);
        List<String> destinationFailures = Arrays.asList("San Diego-Houston", "Boulder-Houston", "Boulder-Lincoln");
        Map<String, Set<String>> dstFailureMap = destinations.stream()
                .collect(Collectors.toMap(d -> d, d -> pickFailureNodes(Collections.singletonList(d), destinationFailures)));
        Map<String, Integer> dstNFAMap = new HashMap<>();
        dstNFAMap.put("Lincoln", 1);
        dstNFAMap.put("Salt Lake City", 1);
        RequestSet ers = createSetWithEndpoints(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
                dstMinNumConnections, dstMaxNumConnections, 5, srcFailureMap, srcNFAMap, dstFailureMap, dstNFAMap);
        analyze(ers, Arrays.asList(7), true, true);
    }

    @Test
    public void endpointVariedDstNFAMaxThreeC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Boulder", "Houston"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Lincoln", "Salt Lake City"));
        Map<String, Integer> srcMinNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 0));
        Map<String, Integer> srcMaxNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 3));
        Map<String, Integer> dstMinNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 0));
        Map<String, Integer> dstMaxNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 3));

        List<String> sourceFailures = Arrays.asList("Champaign", "San Diego-Houston", "Houston-Boulder", "Lincoln-Boulder");
        Map<String, Set<String>> srcFailureMap = sources.stream()
                .collect(Collectors.toMap(s -> s, s -> pickFailureNodes(Collections.singletonList(s), sourceFailures)));
        Map<String, Integer> srcNFAMap = new HashMap<>();
        srcNFAMap.put("Boulder", 1);
        srcNFAMap.put("Houston", 1);
        List<String> destinationFailures = Arrays.asList("San Diego-Houston", "Boulder-Houston", "Boulder-Lincoln");
        Map<String, Set<String>> dstFailureMap = destinations.stream()
                .collect(Collectors.toMap(d -> d, d -> pickFailureNodes(Collections.singletonList(d), destinationFailures)));
        Map<String, Integer> dstNFAMap = new HashMap<>();
        dstNFAMap.put("Lincoln", 2);
        dstNFAMap.put("Salt Lake City", 3);
        RequestSet ers = createSetWithEndpoints(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
                dstMinNumConnections, dstMaxNumConnections, 5, srcFailureMap, srcNFAMap, dstFailureMap, dstNFAMap);
        analyze(ers, Arrays.asList(7), true, true);
    }

    @Test
    public void endpointVariedSrcDstNFAMinVariedMaxVariedC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Boulder", "Houston"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Lincoln", "Salt Lake City"));
        Map<String, Integer> srcMinNumConnections = new HashMap<>();
        srcMinNumConnections.put("Boulder", 2);
        srcMinNumConnections.put("Houston", 1);
        Map<String, Integer> srcMaxNumConnections = new HashMap<>();
        srcMaxNumConnections.put("Boulder", 3);
        srcMaxNumConnections.put("Houston", 2);
        Map<String, Integer> dstMinNumConnections = new HashMap<>();
        dstMinNumConnections.put("Lincoln", 1);
        dstMinNumConnections.put("Salt Lake City", 2);
        Map<String, Integer> dstMaxNumConnections = new HashMap<>();
        dstMaxNumConnections.put("Lincoln", 2);
        dstMaxNumConnections.put("Salt Lake City", 3);

        List<String> sourceFailures = Arrays.asList("Champaign", "San Diego-Houston", "Houston-Boulder", "Lincoln-Boulder");
        List<String> sourceFailures2 = Arrays.asList("Champaign", "Houston-Boulder", "Houston-Atlanta");
        Map<String, Set<String>> srcFailureMap = new HashMap<>();
        srcFailureMap.put("Boulder", pickFailureNodes(Collections.singletonList("Boulder"), sourceFailures));
        srcFailureMap.put("Houston", pickFailureNodes(Collections.singletonList("Houston"), sourceFailures2));
        Map<String, Integer> srcNFAMap = new HashMap<>();
        srcNFAMap.put("Boulder", 1);
        srcNFAMap.put("Houston", 2);

        List<String> destinationFailures = Arrays.asList("San Diego-Houston", "Boulder-Houston", "Boulder-Lincoln");
        List<String> destinationFailures2 = Arrays.asList("Boulder-Houston", "Boulder-Lincoln");
        Map<String, Set<String>> dstFailureMap = new HashMap<>();
        dstFailureMap.put("Lincoln", pickFailureNodes(Collections.singletonList("Lincoln"), destinationFailures));
        dstFailureMap.put("Salt Lake City", pickFailureNodes(Collections.singletonList("Salt Lake City"), destinationFailures2));
        Map<String, Integer> dstNFAMap = new HashMap<>();
        dstNFAMap.put("Lincoln", 2);
        dstNFAMap.put("Salt Lake City", 3);
        RequestSet ers = createSetWithEndpoints(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
                dstMinNumConnections, dstMaxNumConnections, 5, srcFailureMap, srcNFAMap, dstFailureMap, dstNFAMap);
        analyze(ers, Arrays.asList(7), true, true);
    }

    @Test
    public void generatedEndpointVariedSrcDstNFAMinVariedMaxVariedCFeasible(){
        List<Integer> minConns = Arrays.asList(1,2);
        List<Integer> maxConns = Arrays.asList(2, 3);
        List<Integer> minMaxFails = Arrays.asList(5, 6);
        List<Integer> minMaxFailsAllowed = Arrays.asList(1,2);
        RequestSet ers = createSetWithGenService("Endpoint", 10, 10, 5,
                minConns, maxConns, minMaxFails, minMaxFailsAllowed, .60, .0, .0);
        analyze(ers, Arrays.asList(14,15), true, true);

        visualizationService.visualize(ers);
    }

    @Test
    public void generatedEndpointVariedSrcDstNFAMinVariedMaxVariedCFeasible2(){
        List<Integer> minConns = Arrays.asList(0, 0);
        List<Integer> maxConns = Arrays.asList(2, 3);
        List<Integer> minMaxFails = Arrays.asList(1,2);
        List<Integer> minMaxFailsAllowed = Arrays.asList(0, 1);
        RequestSet ers = createSetWithGenService("Endpoint", 10, 10, 10,
                minConns, maxConns, minMaxFails, minMaxFailsAllowed, .59, .10, .20);
        analyze(ers, Arrays.asList(10), true, true);
    }

    @Test
    public void flowOneNFAMaxOneC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<List<String>> pairs = createPairs(sources, destinations);
        Map<List<String>, Integer> pairMinNumConnections = pairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
        Map<List<String>, Integer> pairMaxNumConnections = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
        Map<List<String>, Set<String>> pairFailureMap = pairs.stream()
                .collect(Collectors.toMap(p -> p, p -> pickFailureNodes(p, destinations)));
        Map<List<String>, Integer> pairNFAMap = pairs.stream()
                .collect(Collectors.toMap(p -> p, p -> 1));
        RequestSet ers = createSetWithPairs(sources, destinations, pairMinNumConnections, pairMaxNumConnections,
                5, pairFailureMap, pairNFAMap);
        analyze(ers, Arrays.asList(5), true, true);
        visualizationService.visualize(ers);
    }

    @Test
    public void pairVariedSrcDstNFAMinVariedMaxVariedCInfeasible(){
        Set<String> sources = new HashSet<>(Arrays.asList("Boulder", "Houston"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Lincoln", "Salt Lake City"));
        Set<List<String>> pairs = createPairs(sources, destinations);
        Map<List<String>, Integer> pairMinNumConnections = new HashMap<>();
        pairMinNumConnections.put(Arrays.asList("Boulder", "Lincoln"), 0);
        pairMinNumConnections.put(Arrays.asList("Boulder", "Salt Lake City"), 1);
        pairMinNumConnections.put(Arrays.asList("Houston", "Lincoln"), 1);
        pairMinNumConnections.put(Arrays.asList("Houston", "Salt Lake City"), 1);
        Map<List<String>, Integer> pairMaxNumConnections = new HashMap<>();
        pairMaxNumConnections.put(Arrays.asList("Boulder", "Lincoln"), 0);
        pairMaxNumConnections.put(Arrays.asList("Boulder", "Salt Lake City"), 3);
        pairMaxNumConnections.put(Arrays.asList("Houston", "Lincoln"), 2);
        pairMaxNumConnections.put(Arrays.asList("Houston", "Salt Lake City"), 2);

        List<String> blFailures = Arrays.asList("Champaign", "San Diego-Houston", "Houston-Boulder", "Lincoln-Boulder", "Boulder-Houston", "Boulder-Lincoln");
        List<String> bsFailures = Arrays.asList("Champaign", "San Diego-Houston", "Houston-Boulder", "Lincoln-Boulder", "Boulder-Houston", "Boulder-Lincoln");
        List<String> hlFailures = Arrays.asList("Champaign", "Houston-Boulder", "Houston-Atlanta", "San Diego-Houston", "Boulder-Houston", "Boulder-Lincoln");
        List<String> hsFailures = Arrays.asList("Champaign", "Houston-Boulder", "Houston-Atlanta", "Boulder-Houston", "Boulder-Lincoln");

        Map<List<String>, Set<String>> pairFailureMap = new HashMap<>();
        pairFailureMap.put(Arrays.asList("Boulder", "Lincoln"), new HashSet<>(blFailures));
        pairFailureMap.put(Arrays.asList("Boulder", "Salt Lake City"),  new HashSet<>(bsFailures));
        pairFailureMap.put(Arrays.asList("Houston", "Lincoln"),  new HashSet<>(hlFailures));
        pairFailureMap.put(Arrays.asList("Houston", "Salt Lake City"),  new HashSet<>(hsFailures));
        Map<List<String>, Integer> pairNFAMap = new HashMap<>();
        pairNFAMap.put(Arrays.asList("Boulder", "Lincoln"), 2);
        pairNFAMap.put(Arrays.asList("Boulder", "Salt Lake City"), 3);
        pairNFAMap.put(Arrays.asList("Houston", "Lincoln"), 2);
        pairNFAMap.put(Arrays.asList("Houston", "Salt Lake City"), 3);


        RequestSet ers = createSetWithPairs(sources, destinations, pairMinNumConnections, pairMaxNumConnections,
                5, pairFailureMap, pairNFAMap);
        analyze(ers, Arrays.asList(0), false, false);
    }

    @Test
    public void pairVariedSrcDstNFAMinVariedMaxVariedCFeasible(){
        Set<String> sources = new HashSet<>(Arrays.asList("Boulder", "Houston"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Lincoln", "Salt Lake City"));
        Set<List<String>> pairs = createPairs(sources, destinations);
        Map<List<String>, Integer> pairMinNumConnections = new HashMap<>();
        pairMinNumConnections.put(Arrays.asList("Boulder", "Lincoln"), 1);
        pairMinNumConnections.put(Arrays.asList("Boulder", "Salt Lake City"), 1);
        pairMinNumConnections.put(Arrays.asList("Houston", "Lincoln"), 1);
        pairMinNumConnections.put(Arrays.asList("Houston", "Salt Lake City"), 1);
        Map<List<String>, Integer> pairMaxNumConnections = new HashMap<>();
        pairMaxNumConnections.put(Arrays.asList("Boulder", "Lincoln"), 1);
        pairMaxNumConnections.put(Arrays.asList("Boulder", "Salt Lake City"), 2);
        pairMaxNumConnections.put(Arrays.asList("Houston", "Lincoln"), 2);
        pairMaxNumConnections.put(Arrays.asList("Houston", "Salt Lake City"), 2);

        List<String> blFailures = Arrays.asList("Champaign", "San Diego-Houston", "Houston-Boulder", "Lincoln-Boulder", "Boulder-Houston", "Boulder-Lincoln");
        List<String> bsFailures = Arrays.asList("Champaign", "San Diego-Houston", "Houston-Boulder", "Lincoln-Boulder", "Boulder-Houston", "Boulder-Lincoln");
        List<String> hlFailures = Arrays.asList("Houston-Boulder", "Houston-Atlanta", "San Diego-Houston", "Boulder-Houston", "Boulder-Lincoln");
        List<String> hsFailures = Arrays.asList("Houston-San Diego", "Houston-Boulder", "Houston-Atlanta", "Boulder-Houston", "Boulder-Lincoln");

        Map<List<String>, Set<String>> pairFailureMap = new HashMap<>();
        pairFailureMap.put(Arrays.asList("Boulder", "Lincoln"), new HashSet<>(blFailures));
        pairFailureMap.put(Arrays.asList("Boulder", "Salt Lake City"),  new HashSet<>(bsFailures));
        pairFailureMap.put(Arrays.asList("Houston", "Lincoln"),  new HashSet<>(hlFailures));
        pairFailureMap.put(Arrays.asList("Houston", "Salt Lake City"),  new HashSet<>(hsFailures));
        Map<List<String>, Integer> pairNFAMap = new HashMap<>();
        pairNFAMap.put(Arrays.asList("Boulder", "Lincoln"), 1);
        pairNFAMap.put(Arrays.asList("Boulder", "Salt Lake City"), 3);
        pairNFAMap.put(Arrays.asList("Houston", "Lincoln"), 2);
        pairNFAMap.put(Arrays.asList("Houston", "Salt Lake City"), 3);


        RequestSet ers = createSetWithPairs(sources, destinations, pairMinNumConnections, pairMaxNumConnections,
                5, pairFailureMap, pairNFAMap);
        analyze(ers, Arrays.asList(6), true, true);
    }

    @Test
    public void generatedPairVariedSrcDstNFAMinVariedMaxVariedCFeasible(){
        List<Integer> minConns = Arrays.asList(0, 1);
        List<Integer> maxConns = Arrays.asList(2, 3);
        List<Integer> minMaxFails = Arrays.asList(5, 6);
        List<Integer> minMaxFailsAllowed = Arrays.asList(1,2);
        RequestSet ers = createSetWithGenService("Flow", 10, 10, 5,
                minConns, maxConns, minMaxFails, minMaxFailsAllowed, .59, .0, .0);
        analyze(ers, Arrays.asList(41), true, true);
    }

    @Test
    public void generatedPairVariedSrcDstNFAMinVariedMaxVariedCFeasible2(){
        List<Integer> minConns = Arrays.asList(0, 0);
        List<Integer> maxConns = Arrays.asList(2, 3);
        List<Integer> minMaxFails = Arrays.asList(1,2);
        List<Integer> minMaxFailsAllowed = Arrays.asList(0, 1);
        RequestSet ers = createSetWithGenService("Flow", 10, 10, 10,
                minConns, maxConns, minMaxFails, minMaxFailsAllowed, .59, .10, .20);
        analyze(ers, Arrays.asList(10), true, true);
    }

    @Test
    public void generatedEndpointSharedFOneNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = Arrays.asList(0,0);
        List<Integer> maxConns = Arrays.asList(2,3);
        Integer numFails = 42;
        Integer nfa = 1;
        String failureClass = "Link";
        RequestSet ers = createSetWithGenService("EndpointSharedF", 14, 14, 5,
                minConns, maxConns, numFails, nfa, failureClass,  1.0, .00, .00);
        analyze(ers, Arrays.asList(6), true, true);
    }

    @Test
    public void generatedEndpointSharedFTwoNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = Arrays.asList(0,0);
        List<Integer> maxConns = Arrays.asList(2,3);
        Integer numFails = 42;
        Integer nfa = 2;
        String failureClass = "Link";
        RequestSet ers = createSetWithGenService("EndpointSharedF", 14, 14, 5,
                minConns, maxConns, numFails, nfa, failureClass,  1.0, .00, .00);
        analyze(ers, Arrays.asList(7), true, true);
    }

    @Test
    public void endpointSharedFOneNFAMaxOneCAllLinksFail(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Map<String, Integer> srcMinNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 0));
        Map<String, Integer> srcMaxNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 1));
        Map<String, Integer> dstMinNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 0));
        Map<String, Integer> dstMaxNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 1));
        Set<String> failures = topoService.getTopologyById("NSFnet").getLinkIdMap().keySet();
        Integer nfa = 1;
        RequestSet ers = createSetWithEndpointsSharedF(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
                dstMinNumConnections, dstMaxNumConnections, 5, failures, nfa);
        analyze(ers, Arrays.asList(6), true, true);
    }


    @Test
    public void endpointSharedFTwoNFAMaxOneCAllLinksFail(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Map<String, Integer> srcMinNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 0));
        Map<String, Integer> srcMaxNumConnections = sources.stream().collect(Collectors.toMap(s -> s, s -> 1));
        Map<String, Integer> dstMinNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 0));
        Map<String, Integer> dstMaxNumConnections = destinations.stream().collect(Collectors.toMap(d -> d, d -> 1));
        Set<String> failures = topoService.getTopologyById("NSFnet").getLinkIdMap().keySet();
        Integer nfa = 2;
        RequestSet ers = createSetWithEndpointsSharedF(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
                dstMinNumConnections, dstMaxNumConnections, 5, failures, nfa);
        analyze(ers, Arrays.asList(7), true, true);
    }

    @Test
    public void flowSharedFOneNFAMaxOneC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<List<String>> pairs = createPairs(sources, destinations);
        Map<List<String>, Integer> pairMinNumConnections = pairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
        Map<List<String>, Integer> pairMaxNumConnections = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
        Set<String> failures = topoService.getTopologyById("NSFnet").getLinkIdMap().keySet();
        Integer nfa = 1;
        RequestSet ers = createSetWithPairsSharedF(sources, destinations, pairMinNumConnections, pairMaxNumConnections,
                5, failures, nfa);
        analyze(ers, Arrays.asList(6), true, true);
    }

    @Test
    public void flowSharedFTwoNFAMaxOneC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<List<String>> pairs = createPairs(sources, destinations);
        Map<List<String>, Integer> pairMinNumConnections = pairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
        Map<List<String>, Integer> pairMaxNumConnections = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
        Set<String> failures = topoService.getTopologyById("NSFnet").getLinkIdMap().keySet();
        Integer nfa = 2;
        RequestSet ers = createSetWithPairsSharedF(sources, destinations, pairMinNumConnections, pairMaxNumConnections,
                5, failures, nfa);
        analyze(ers, Arrays.asList(7), true, true);
    }

    @Test
    public void generatedFlowSharedFOneNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = Arrays.asList(0,0);
        List<Integer> maxConns = Arrays.asList(2,3);
        Integer numFails = 42;
        Integer nfa = 1;
        String failureClass = "Link";
        RequestSet ers = createSetWithGenService("FlowSharedF", 14, 14, 5,
                minConns, maxConns, numFails, nfa, failureClass,  1.0, .00, .00);
        analyze(ers, Arrays.asList(6), true, true);
    }

    @Test
    public void generatedFlowSharedFTwoNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = Arrays.asList(0,0);
        List<Integer> maxConns = Arrays.asList(2,3);
        Integer numFails = 42;
        Integer nfa = 2;
        String failureClass = "Link";
        RequestSet ers = createSetWithGenService("FlowSharedF", 14, 14, 5,
                minConns, maxConns, numFails, nfa, failureClass,  1.0, .00, .00);
        analyze(ers, Arrays.asList(7), true, true);
    }



    // Helper methods

    private Set<List<String>> createPairs(Set<String> sources, Set<String> destinations){
        Set<List<String>> pairSet = new HashSet<>();
        for(String source : sources){
            for(String dest: destinations){
                pairSet.add(Arrays.asList(source, dest));
            }
        }
        return pairSet;
    }

    private void analyze(RequestSet requestSet, List<Integer> numExpectedPaths, boolean survivable, boolean feasible){
        AnalyzedSet analyzedSet = analysisService.analyzeRequestSet(requestSet);
        for(RequestMetrics rm : analyzedSet.getRequestMetrics().values()){
            assert(rm.getIsSurvivable() == survivable);
            assert(numExpectedPaths.contains(rm.getNumPaths()));
            assert(rm.getIsFeasible() == feasible);
        }
        assert(requestSet.getRequests().values().stream()
                .allMatch(r ->
                        r.getChosenPaths().keySet().stream()
                                .filter(pair -> pair.getSrc().equals(pair.getDst()))
                                .allMatch(p -> r.getChosenPaths().get(p).values().size() == 0)));

    }

    private RequestSet createSetWithPairs(Set<String> sources, Set<String> destinations, Map<List<String>, Integer> pairMinNumConnections,
                                          Map<List<String>, Integer> pairMaxNumConnections, Integer numConnections,
                                          Map<List<String>, Set<String>> pairFailureMap, Map<List<String>, Integer> pairNumFailsAllowedMap){

        RequestParameters params = RequestParameters.builder()
                .topologyId("NSFnet")
                .problemClass("Flow")
                .algorithm("ServiceILP")
                .objective("LinksUsed")
                .sources(sources)
                .destinations(destinations)
                .pairMinNumConnectionsMap(pairMinNumConnections)
                .pairMaxNumConnectionsMap(pairMaxNumConnections)
                .numConnections(numConnections)
                .pairFailureMap(pairFailureMap)
                .pairNumFailsAllowedMap(pairNumFailsAllowedMap)
                .build();
        RequestSet rs = generationService.generateFromRequestParams(params);
        processingService.processRequestSet(rs);
        return rs;
    }

    private RequestSet createSetWithPairsSharedF(Set<String> sources, Set<String> destinations,
                                                 Map<List<String>, Integer> pairMinNumConnections,
                                                 Map<List<String>, Integer> pairMaxNumConnections, Integer numConnections,
                                                 Set<String> failures, Integer nfa) {
        RequestParameters params = RequestParameters.builder()
                .topologyId("NSFnet")
                .problemClass("FlowSharedF")
                .algorithm("ServiceILP")
                .objective("LinksUsed")
                .sources(sources)
                .destinations(destinations)
                .pairMinNumConnectionsMap(pairMinNumConnections)
                .pairMaxNumConnectionsMap(pairMaxNumConnections)
                .numConnections(numConnections)
                .failures(failures)
                .numFailsAllowed(nfa)
                .build();
        RequestSet rs = generationService.generateFromRequestParams(params);
        processingService.processRequestSet(rs);
        return rs;
    }

    private RequestSet createSetWithEndpoints(Set<String> sources, Set<String> destinations, Map<String, Integer> srcMinNumConnections,
                                              Map<String, Integer> srcMaxNumConnections, Map<String, Integer> dstMinNumConnections,
                                              Map<String, Integer> dstMaxNumConnections, Integer numConnections,
                                              Map<String, Set<String>> srcFailureMap, Map<String, Integer> srcNumFailsAllowedMap,
                                              Map<String, Set<String>> dstFailureMap, Map<String, Integer> dstNumFailsAllowedMap){

        RequestParameters params = RequestParameters.builder()
                .topologyId("NSFnet")
                .problemClass("Endpoint")
                .algorithm("ServiceILP")
                .objective("LinksUsed")
                .sources(sources)
                .destinations(destinations)
                .sourceMinNumConnectionsMap(srcMinNumConnections)
                .sourceMaxNumConnectionsMap(srcMaxNumConnections)
                .destMinNumConnectionsMap(dstMinNumConnections)
                .destMaxNumConnectionsMap(dstMaxNumConnections)
                .numConnections(numConnections)
                .sourceFailureMap(srcFailureMap)
                .sourceNumFailsAllowedMap(srcNumFailsAllowedMap)
                .destFailureMap(dstFailureMap)
                .destNumFailsAllowedMap(dstNumFailsAllowedMap)
                .build();
        RequestSet rs = generationService.generateFromRequestParams(params);
        processingService.processRequestSet(rs);
        return rs;
    }

    private RequestSet createSetWithEndpointsSharedF(Set<String> sources, Set<String> destinations, Map<String, Integer> srcMinNumConnections,
                                                     Map<String, Integer> srcMaxNumConnections, Map<String, Integer> dstMinNumConnections,
                                                     Map<String, Integer> dstMaxNumConnections, Integer numConnections, Set<String> failures, Integer nfa) {
        RequestParameters params = RequestParameters.builder()
                .topologyId("NSFnet")
                .problemClass("EndpointSharedF")
                .algorithm("ServiceILP")
                .objective("LinksUsed")
                .sources(sources)
                .destinations(destinations)
                .sourceMinNumConnectionsMap(srcMinNumConnections)
                .sourceMaxNumConnectionsMap(srcMaxNumConnections)
                .destMinNumConnectionsMap(dstMinNumConnections)
                .destMaxNumConnectionsMap(dstMaxNumConnections)
                .numConnections(numConnections)
                .failures(failures)
                .numFailsAllowed(nfa)
                .build();
        RequestSet rs = generationService.generateFromRequestParams(params);
        processingService.processRequestSet(rs);
        return rs;
    }

    private Set<String> pickFailureNodes(List<String> exclusionList, Collection<String> candidates) {
        Set<String> copy = new HashSet<>(candidates);
        copy.removeAll(exclusionList);
        return copy;
    }

    private RequestSet createSetWithGenService(String problemClass, int numSources, int numDestinations, int numConns, List<Integer> minConns,
                                 List<Integer> maxConns, List<Integer> minMaxFails, List<Integer> minMaxFailsAllowed,
                                               double percentSrcAlsoDest, double percentSrcFail, double percentDstFail){
        return createSetAndSolve(1L, "NSFnet", 1, "ServiceILP", problemClass, "LinksUsed", numSources, numDestinations, 0,
               minMaxFails, "Both", 0.0, new ArrayList<>(), numConns, minConns, maxConns,
                0, minMaxFailsAllowed, "Solo", false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail);

    }


    private RequestSet createSetWithGenService(String problemClass, int numSources, int numDestinations, int numConns,
                                               List<Integer> minConns, List<Integer> maxConns, Integer numFails,
                                               Integer nfa, String failureClass, double percentSrcAlsoDest, double percentSrcFail, double percentDstFail) {
        return createSetAndSolve(1L, "NSFnet", 1, "ServiceILP", problemClass, "LinksUsed", numSources, numDestinations, numFails,
                new ArrayList<>(), failureClass, 0.0, new ArrayList<>(), numConns, minConns, maxConns,
                nfa, new ArrayList<>(), "Solo", false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail);
    }

    private RequestSet createSetAndSolve(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                             String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                             List<Integer> minMaxFailures, String failureClass, Double failureProb,
                             List<Double> minMaxFailureProb, Integer numConnections,
                             List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                             Integer numFailsAllowed, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                             Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                             double percentDstFail){

        SimulationParameters params = makeParameters(seed, topologyId, numRequests, alg, problemClass, objective, numSources, numDestinations,
                fSetSize, minMaxFailures, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange, maxConnectionsRange,
                numFailsAllowed, minMaxFailsAllowed, processingType, sdn, useAws, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        RequestSet requestSet = generationService.generateFromSimParams(params);
        processingService.processRequestSet(requestSet);
        return requestSet;
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                Integer numFails, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                                Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                                double percentDstFail){
        return SimulationParameters.builder()
                .seed(seed)
                .topologyId(topologyId)
                .numRequests(numRequests)
                .algorithm(alg)
                .problemClass(problemClass)
                .objective(objective)
                .numSources(numSources)
                .numDestinations(numDestinations)
                .failureSetSize(fSetSize)
                .minMaxFailures(minMaxFailures)
                .failureClass(failureClass)
                .failureProb(failureProb)
                .minMaxFailureProb(minMaxFailureProb)
                .numConnections(numConnections)
                .minConnectionsRange(minConnectionsRange)
                .maxConnectionsRange(maxConnectionsRange)
                .numFailsAllowed(numFails)
                .minMaxFailsAllowed(minMaxFailsAllowed)
                .processingType(processingType)
                .sdn(sdn)
                .useAws(useAws)
                .percentSrcAlsoDest(percentSrcAlsoDest)
                .percentSrcFail(percentSrcFail)
                .percentDestFail(percentDstFail)
                .build();
    }
}
