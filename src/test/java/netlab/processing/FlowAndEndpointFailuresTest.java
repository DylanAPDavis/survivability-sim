package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.analyzed.RequestMetrics;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.RequestSet;
import netlab.submission.services.GenerationService;
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
        analyze(ers, 10, true, true);
    }

    @Test
    public void endpointTwoNFAMaxOneC(){
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
                .collect(Collectors.toMap(s -> s, s -> 2));
        Map<String, Set<String>> dstFailureMap = destinations.stream()
                .collect(Collectors.toMap(d -> d, d -> pickFailureNodes(Collections.singletonList(d), destinations)));
        Map<String, Integer> dstNFAMap = destinations.stream()
                .collect(Collectors.toMap(d -> d, d -> 2));
        RequestSet ers = createSetWithEndpoints(sources, destinations, srcMinNumConnections, srcMaxNumConnections,
                dstMinNumConnections, dstMaxNumConnections, 5, srcFailureMap, srcNFAMap, dstFailureMap, dstNFAMap);
        analyze(ers, 10, true, true);
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
        analyze(ers, 5, true, true);
    }

    private Set<List<String>> createPairs(Set<String> sources, Set<String> destinations){
        Set<List<String>> pairSet = new HashSet<>();
        for(String source : sources){
            for(String dest: destinations){
                pairSet.add(Arrays.asList(source, dest));
            }
        }
        return pairSet;
    }

    private void analyze(RequestSet requestSet, int numExpectedPaths, boolean survivable, boolean feasible){
        AnalyzedSet analyzedSet = analysisService.analyzeRequestSet(requestSet);
        for(RequestMetrics rm : analyzedSet.getRequestMetrics().values()){
            assert(rm.getRequestIsSurvivable() == survivable);
            assert(rm.getNumPaths() == numExpectedPaths);
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
        RequestSet rs = generationService.generateSetFromRequest(params);
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
        RequestSet rs = generationService.generateSetFromRequest(params);
        processingService.processRequestSet(rs);
        return rs;
    }

    private Set<String> pickFailureNodes(List<String> exclusionList, Set<String> candidates) {
        Set<String> copy = new HashSet<>(candidates);
        copy.removeAll(exclusionList);
        return copy;
    }
}
