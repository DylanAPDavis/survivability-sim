package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.analyzed.RequestMetrics;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.Request;
import netlab.submission.request.RequestParameters;
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
        Request ers = createSetWithEndpoints(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
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
        Request ers = createSetWithEndpoints(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
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
        Request ers = createSetWithEndpoints(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
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
        Request ers = createSetWithEndpoints(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
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
        Request ers = createSetWithEndpoints(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
                dstMinNumConnections, dstMaxNumConnections, 5, srcFailureMap, srcNFAMap, dstFailureMap, dstNFAMap);
        analyze(ers, Arrays.asList(7), true, true);
    }

    @Test
    public void generatedEndpointVariedSrcDstNFAMinVariedMaxVariedCFeasible(){
        List<Integer> minConns = new ArrayList<>();
        List<Integer> maxConns = new ArrayList<>();
        List<Integer> minSrcConns = Arrays.asList(1, 2);
        List<Integer> maxSrcConns = Arrays.asList(2, 3);
        List<Integer> minDstConns = Arrays.asList(1, 2);
        List<Integer> maxDstConns = Arrays.asList(2, 3);
        List<Integer> minMaxFails = Arrays.asList(5, 6);
        List<Integer> minMaxFailsAllowed = Arrays.asList(1,2);
        Request ers = createSetWithGenService("Endpoint", 10, 10, 5,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, minMaxFails, minMaxFailsAllowed, .60, .0, .0, false);
        analyze(ers, Arrays.asList(18, 19, 20), true, true);
    }

    @Test
    public void generatedEndpointVariedSrcDstNFAMinVariedMaxVariedCFeasible2(){
        List<Integer> minConns = new ArrayList<>();
        List<Integer> maxConns = new ArrayList<>();
        List<Integer> minSrcConns = Arrays.asList(0, 0);
        List<Integer> maxSrcConns = Arrays.asList(2, 3);
        List<Integer> minDstConns = Arrays.asList(0, 0);
        List<Integer> maxDstConns = Arrays.asList(2, 3);
        List<Integer> minMaxFails = Arrays.asList(1,2);
        List<Integer> minMaxFailsAllowed = Arrays.asList(0, 1);
        Request ers = createSetWithGenService("Endpoint", 10, 10, 10,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, minMaxFails, minMaxFailsAllowed, .59, .10, .20, false);
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
        Request ers = createSetWithPairs(sources, destinations, pairMinNumConnections, pairMaxNumConnections,
                5, pairFailureMap, pairNFAMap);
        analyze(ers, Arrays.asList(5), true, true);
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


        Request ers = createSetWithPairs(sources, destinations, pairMinNumConnections, pairMaxNumConnections,
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


        Request ers = createSetWithPairs(sources, destinations, pairMinNumConnections, pairMaxNumConnections,
                5, pairFailureMap, pairNFAMap);
        analyze(ers, Arrays.asList(6), true, true);
    }

    @Test
    public void generatedPairVariedSrcDstNFAMinVariedMaxVariedCFeasible(){
        List<Integer> minConns = Arrays.asList(0, 1);
        List<Integer> maxConns = Arrays.asList(2, 3);
        List<Integer> minSrcConns = new ArrayList<>();
        List<Integer> maxSrcConns = new ArrayList<>();
        List<Integer> minDstConns = new ArrayList<>();
        List<Integer> maxDstConns = new ArrayList<>();
        List<Integer> minMaxFails = Arrays.asList(5, 6);
        List<Integer> minMaxFailsAllowed = Arrays.asList(1,2);
        Request ers = createSetWithGenService("Flow", 10, 10, 5,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, minMaxFails, minMaxFailsAllowed, .59, .0, .0, false);
        analyze(ers, Arrays.asList(41), true, true);
    }

    @Test
    public void generatedPairVariedSrcDstNFAMinVariedMaxVariedCFeasible2(){
        List<Integer> minConns = Arrays.asList(0, 0);
        List<Integer> maxConns = Arrays.asList(2, 3);
        List<Integer> minSrcConns = new ArrayList<>();
        List<Integer> maxSrcConns = new ArrayList<>();
        List<Integer> minDstConns = new ArrayList<>();
        List<Integer> maxDstConns = new ArrayList<>();
        List<Integer> minMaxFails = Arrays.asList(1,2);
        List<Integer> minMaxFailsAllowed = Arrays.asList(0, 1);
        Request ers = createSetWithGenService("Flow", 10, 10, 10,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, minMaxFails, minMaxFailsAllowed, .59, .10, .20, false);
        analyze(ers, Arrays.asList(10), true, true);
    }

    @Test
    public void generatedEndpointSharedFOneNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = new ArrayList<>();
        List<Integer> maxConns = new ArrayList<>();
        List<Integer> minSrcConns = Arrays.asList(0,0);
        List<Integer> maxSrcConns = Arrays.asList(2,3);
        List<Integer> minDstConns = Arrays.asList(0,0);
        List<Integer> maxDstConns = Arrays.asList(2,3);
        Integer numFails = 21;
        Integer nfa = 1;
        String failureClass = "Link";
        Request ers = createSetWithGenService("EndpointSharedF", 14, 14, 5,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, numFails, nfa, failureClass,  1.0, .00, .00, false);
        analyze(ers, Arrays.asList(6), true, true);
    }

    @Test
    public void generatedEndpointSharedFTwoNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = new ArrayList<>();
        List<Integer> maxConns = new ArrayList<>();
        List<Integer> minSrcConns = Arrays.asList(0,0);
        List<Integer> maxSrcConns = Arrays.asList(2,3);
        List<Integer> minDstConns = Arrays.asList(0,0);
        List<Integer> maxDstConns = Arrays.asList(2,3);
        Integer numFails = 21;
        Integer nfa = 2;
        String failureClass = "Link";
        Request ers = createSetWithGenService("EndpointSharedF", 14, 14, 5,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, numFails, nfa, failureClass,  1.0, .00, .00, false);
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
        Request ers = createSetWithEndpointsSharedF(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
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
        Request ers = createSetWithEndpointsSharedF(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
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
        Request ers = createSetWithPairsSharedF(sources, destinations, pairMinNumConnections, pairMaxNumConnections,
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
        Request ers = createSetWithPairsSharedF(sources, destinations, pairMinNumConnections, pairMaxNumConnections,
                5, failures, nfa);
        analyze(ers, Arrays.asList(7), true, true);
    }

    @Test
    public void generatedFlowSharedFOneNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = Arrays.asList(0,0);
        List<Integer> maxConns = Arrays.asList(2,3);
        List<Integer> minSrcConns = new ArrayList<>();
        List<Integer> maxSrcConns = new ArrayList<>();
        List<Integer> minDstConns = new ArrayList<>();
        List<Integer> maxDstConns = new ArrayList<>();
        Integer numFails = 21;
        Integer nfa = 1;
        String failureClass = "Link";
        Request ers = createSetWithGenService("FlowSharedF", 14, 14, 5,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, numFails, nfa, failureClass,  1.0, .00, .00, false);
        analyze(ers, Arrays.asList(6), true, true);
    }

    @Test
    public void generatedFlowSharedFTwoNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = Arrays.asList(0,0);
        List<Integer> maxConns = Arrays.asList(2,3);
        List<Integer> minSrcConns = new ArrayList<>();
        List<Integer> maxSrcConns = new ArrayList<>();
        List<Integer> minDstConns = new ArrayList<>();
        List<Integer> maxDstConns = new ArrayList<>();
        Integer numFails = 21;
        Integer nfa = 2;
        String failureClass = "Link";
        Request ers = createSetWithGenService("FlowSharedF", 14, 14, 5,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, numFails, nfa, failureClass,  1.0, .00, .00, false);
        analyze(ers, Arrays.asList(7), true, true);
    }

    @Test
    public void generatedFlowSharedFIgnoreFTwoNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = Arrays.asList(0,0);
        List<Integer> maxConns = Arrays.asList(2,3);
        List<Integer> minSrcConns = new ArrayList<>();
        List<Integer> maxSrcConns = new ArrayList<>();
        List<Integer> minDstConns = new ArrayList<>();
        List<Integer> maxDstConns = new ArrayList<>();
        Integer numFails = 21;
        Integer nfa = 2;
        String failureClass = "Link";
        Request ers = createSetWithGenService("FlowSharedF", 14, 14, 5,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, numFails, nfa, failureClass,  1.0, .00, .00, true);
        analyze(ers, Arrays.asList(5), false, true);
    }

    @Test
    public void generatedFlowIgnoreFTwoNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = Arrays.asList(0,0);
        List<Integer> maxConns = Arrays.asList(2,3);
        List<Integer> minSrcConns = new ArrayList<>();
        List<Integer> maxSrcConns = new ArrayList<>();
        List<Integer> minDstConns = new ArrayList<>();
        List<Integer> maxDstConns = new ArrayList<>();
        Integer numFails = 21;
        Integer nfa = 2;
        String failureClass = "Link";
        Request ers = createSetWithGenService("Flow", 14, 14, 5,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, numFails, nfa, failureClass,  1.0, .00, .00, true);
        analyze(ers, Arrays.asList(5), false, true);
    }

    @Test
    public void generatedEndpointIgnoreFTwoNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = new ArrayList<>();
        List<Integer> maxConns = new ArrayList<>();
        List<Integer> minSrcConns = Arrays.asList(0,0);
        List<Integer> maxSrcConns = Arrays.asList(2,3);
        List<Integer> minDstConns = Arrays.asList(0,0);
        List<Integer> maxDstConns = Arrays.asList(2,3);
        Integer numFails = 21;
        Integer nfa = 2;
        String failureClass = "Link";
        Request ers = createSetWithGenService("Endpoint", 14, 14, 5,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, numFails, nfa, failureClass,  1.0, .00, .00, true);
        analyze(ers, Arrays.asList(5), false, true);
    }

    @Test
    public void generatedEndpointSharedFIgnoreFTwoNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = new ArrayList<>();
        List<Integer> maxConns = new ArrayList<>();
        List<Integer> minSrcConns = Arrays.asList(0,0);
        List<Integer> maxSrcConns = Arrays.asList(2,3);
        List<Integer> minDstConns = Arrays.asList(0,0);
        List<Integer> maxDstConns = Arrays.asList(2,3);
        Integer numFails = 21;
        Integer nfa = 2;
        String failureClass = "Link";
        Request ers = createSetWithGenService("EndpointSharedF", 14, 14, 5,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, numFails, nfa, failureClass,  1.0, .00, .00, true);
        analyze(ers, Arrays.asList(5), false, true);
    }

    @Test
    public void generatedFlexIgnoreFTwoNFAMaxOneCAllLinksFail(){
        List<Integer> minConns = new ArrayList<>();
        List<Integer> maxConns = new ArrayList<>();
        List<Integer> minSrcConns = new ArrayList<>();
        List<Integer> maxSrcConns = new ArrayList<>();
        List<Integer> minDstConns = new ArrayList<>();
        List<Integer> maxDstConns = new ArrayList<>();
        Integer numFails = 21;
        Integer nfa = 2;
        String failureClass = "Link";
        Request ers = createSetWithGenService("Flex", 14, 14, 5,
                minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, numFails, nfa, failureClass,  1.0, .00, .00, true);
        analyze(ers, Arrays.asList(5), false, true);
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

    private void analyze(Request request, List<Integer> numExpectedPaths, boolean survivable, boolean feasible){
        Analysis analysis = analysisService.analyzeRequestSet(request);
        for(RequestMetrics rm : analysis.getRequestMetrics().values()){
            assert(rm.getIsSurvivable() == survivable);
            assert(numExpectedPaths.contains(rm.getNumPaths()));
            assert(rm.getIsFeasible() == feasible);
        }
        assert(request.getDetails().values().stream()
                .allMatch(r ->
                        r.getChosenPaths().keySet().stream()
                                .filter(pair -> pair.getSrc().equals(pair.getDst()))
                                .allMatch(p -> r.getChosenPaths().get(p).values().size() == 0)));

    }

    private Request createSetWithPairs(Set<String> sources, Set<String> destinations, Map<List<String>, Integer> pairMinNumConnections,
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
        Request rs = generationService.generateFromRequestParams(params);
        processingService.processRequestSet(rs);
        return rs;
    }

    private Request createSetWithPairsSharedF(Set<String> sources, Set<String> destinations,
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
        Request rs = generationService.generateFromRequestParams(params);
        processingService.processRequestSet(rs);
        return rs;
    }

    private Request createSetWithEndpoints(Set<String> sources, Set<String> destinations, Map<String, Integer> srcMinNumConnections,
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
        Request rs = generationService.generateFromRequestParams(params);
        processingService.processRequestSet(rs);
        return rs;
    }

    private Request createSetWithEndpointsSharedF(Set<String> sources, Set<String> destinations, Map<String, Integer> srcMinNumConnections,
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
        Request rs = generationService.generateFromRequestParams(params);
        processingService.processRequestSet(rs);
        return rs;
    }

    private Set<String> pickFailureNodes(List<String> exclusionList, Collection<String> candidates) {
        Set<String> copy = new HashSet<>(candidates);
        copy.removeAll(exclusionList);
        return copy;
    }

    private Request createSetWithGenService(String problemClass, int numSources, int numDestinations, int numConns, List<Integer> minConns,
                                            List<Integer> maxConns, List<Integer> minSrcConns, List<Integer> maxSrcConns,
                                            List<Integer> minDstConns, List<Integer> maxDstConns,
                                            List<Integer> minMaxFails, List<Integer> minMaxFailsAllowed,
                                            double percentSrcAlsoDest, double percentSrcFail, double percentDstFail,
                                            boolean ignoreFailures){
        return createSetAndSolve(1L, "NSFnet", 1, "ServiceILP", problemClass,
                "LinksUsed", numSources, numDestinations, 0, minMaxFails, "Both",
                0.0, new ArrayList<>(), numConns, minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns,
                0, minMaxFailsAllowed, "Solo", false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail, ignoreFailures);

    }


    private Request createSetWithGenService(String problemClass, int numSources, int numDestinations, int numConns,
                                            List<Integer> minConns, List<Integer> maxConns,
                                            List<Integer> minSrcConns, List<Integer> maxSrcConns,
                                            List<Integer> minDstConns, List<Integer> maxDstConns, Integer numFails,
                                            Integer nfa, String failureClass, double percentSrcAlsoDest, double percentSrcFail,
                                            double percentDstFail, boolean ignoreFailures) {
        return createSetAndSolve(1L, "NSFnet", 1, "ServiceILP", problemClass, "LinksUsed",
                numSources, numDestinations, numFails, new ArrayList<>(), failureClass, 0.0, new ArrayList<>(),
                numConns, minConns, maxConns, minSrcConns, maxSrcConns, minDstConns, maxDstConns, nfa, new ArrayList<>(),
                "Solo", false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail, ignoreFailures);
    }

    private Request createSetAndSolve(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                      String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                      List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                      List<Double> minMaxFailureProb, Integer numConnections,
                                      List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                      List<Integer> minSrcConnectionsRange, List<Integer> maxSrcConnectionsRange,
                                      List<Integer> minDstConnectionsRange, List<Integer> maxDstConnectionsRange,
                                      Integer numFailsAllowed, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                      Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                      double percentDstFail, boolean ignoreFailures){

        SimulationParameters params = makeParameters(seed, topologyId, numRequests, alg, problemClass, objective, numSources, numDestinations,
                fSetSize, minMaxFailures, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange, maxConnectionsRange,
                minSrcConnectionsRange, maxSrcConnectionsRange, minDstConnectionsRange, maxDstConnectionsRange,
                numFailsAllowed, minMaxFailsAllowed, processingType, sdn, useAws, percentSrcAlsoDest, percentSrcFail, percentDstFail, ignoreFailures);
        Request request = generationService.generateFromSimParams(params);
        processingService.processRequestSet(request);
        return request;
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                List<Integer> minSrcConnectionsRange, List<Integer> maxSrcConnectionsRange,
                                                List<Integer> minDstConnectionsRange, List<Integer> maxDstConnectionsRange,
                                                Integer numFails, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                                Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                                double percentDstFail, boolean ignoreFailues){
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
                .minSrcConnectionsRange(minSrcConnectionsRange)
                .maxSrcConnectionsRange(maxSrcConnectionsRange)
                .minDstConnectionsRange(minDstConnectionsRange)
                .maxDstConnectionsRange(maxDstConnectionsRange)
                .numFailsAllowed(numFails)
                .minMaxFailsAllowed(minMaxFailsAllowed)
                .processingType(processingType)
                .sdn(sdn)
                .useAws(useAws)
                .percentSrcAlsoDest(percentSrcAlsoDest)
                .percentSrcFail(percentSrcFail)
                .percentDestFail(percentDstFail)
                .ignoreFailures(ignoreFailues)
                .build();
    }
}
