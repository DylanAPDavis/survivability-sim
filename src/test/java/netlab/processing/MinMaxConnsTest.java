package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.analyzed.PathSetMetrics;
import netlab.analysis.analyzed.RequestMetrics;
import netlab.analysis.enums.MemberType;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.Request;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class MinMaxConnsTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ProcessingService processingService;


    @Test
    public void zeroMinOneMaxOneSourceOneDestOneC(){
        RequestSet r1 = createSet("Endpoint", 1, 1, 1, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r1, 1, true);
        RequestSet r2 = createSet("Flow", 1, 1, 1, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r2, 1, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinOneMaxOneSourceOneDestTwoC(){
        RequestSet r1 = createSet("Endpoint", 1, 1, 2, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r1, 0, false);
        RequestSet r2 = createSet("Flow", 1, 1, 2, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r2, 0, false);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void oneMinOneMaxOneSourceOneDestTwoC(){
        RequestSet r1 = createSet("Endpoint", 1, 1, 2, Arrays.asList(1,1),
                Arrays.asList(1,1), 0.0) ;
        analyze(r1, 0, false);
        RequestSet r2 = createSet("Flow", 1, 1, 2, Arrays.asList(1,1),
                Arrays.asList(1,1), 0.0) ;
        analyze(r2, 0, false);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinTwoMaxOneSourceOneDestTwoC(){
        RequestSet r1 = createSet("Endpoint", 1, 1, 2, Arrays.asList(0,0),
                Arrays.asList(2,2), 0.0) ;
        analyze(r1, 2, true);
        RequestSet r2 = createSet("Flow", 1, 1, 2, Arrays.asList(0,0),
                Arrays.asList(2,2), 0.0) ;
        analyze(r2, 2, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinOneMaxOneSourceTwoDestOneC(){
        RequestSet r1 = createSet("Endpoint", 1, 2, 1, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r1, 1, true);
        RequestSet r2 = createSet("Flow", 1, 2, 1, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r2, 1, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    public void zeroMinZeroToOneMaxOneSourceTwoDestOneC(){
        RequestSet r1 = createSet("Endpoint", 1, 2, 1, Arrays.asList(0,0),
                Arrays.asList(0,1), 0.0) ;
        analyze(r1, 1, true);
        RequestSet r2 = createSet("Flow", 1, 2, 1, Arrays.asList(0,0),
                Arrays.asList(0,1), 0.0) ;
        analyze(r2, 1, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    private RequestSet createSet(String problemClass, int numSources, int numDestinations, int numConns, List<Integer> minConns,
                                List<Integer> maxConns, double percentSrcAlsoDest){
        return solve(1L, "NSFnet", 1, "ServiceILP", problemClass, numSources, numDestinations, 0,
                new ArrayList<>(), "Both", 0.0, new ArrayList<>(), numConns, minConns, maxConns,
                0, new ArrayList<>(), "Solo", false, false, percentSrcAlsoDest, 0.0, 0.0);

    }


    private void analyzeMultiSet(List<RequestSet> requestSets) {
        RequestSet rs1 = requestSets.get(0);
        Map<SourceDestPair, Map<String, Path>> chosenPaths1 = rs1.getRequests().values().iterator().next().getChosenPaths();
        Integer numLinkUsages1 = chosenPaths1.values().stream().map(Map::values).flatMap(Collection::stream).map(p -> p.getLinks().size()).reduce(0, (p1, p2) -> p1 + p2);
        for(RequestSet rs : requestSets){
            Map<SourceDestPair, Map<String, Path>> chosenPaths = rs.getRequests().values().iterator().next().getChosenPaths();
            Integer numLinkUsages = chosenPaths.values().stream().map(Map::values).flatMap(Collection::stream).map(p -> p.getLinks().size()).reduce(0, (p1, p2) -> p1 + p2);
            assert(Objects.equals(numLinkUsages, numLinkUsages1));
        }
    }

    private void analyze(RequestSet requestSet, int numExpectedPaths, boolean survivable){
        AnalyzedSet analyzedSet = analysisService.analyzeRequestSet(requestSet);
        for(RequestMetrics rm : analyzedSet.getRequestMetrics().values()){
            assert(rm.getRequestIsSurvivable() == survivable);
            assert(rm.getNumPaths() == numExpectedPaths);
        }
        assert(requestSet.getRequests().values().stream()
                .allMatch(r ->
                        r.getChosenPaths().keySet().stream()
                                .filter(pair -> pair.getSrc().equals(pair.getDst()))
                                .allMatch(p -> r.getChosenPaths().get(p).values().size() == 0)));

    }


    private RequestSet solve(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                             Integer numSources, Integer numDestinations, Integer fSetSize,
                             List<Integer> minMaxFailures, String failureClass, Double failureProb,
                             List<Double> minMaxFailureProb, Integer numConnections,
                             List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                             Integer numFails, List<Integer> minMaxFails, String processingType, Boolean sdn,
                             Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                             double percentDstFail){

        SimulationParameters params = makeParameters(seed, topologyId, numRequests, alg, problemClass, numSources, numDestinations,
                fSetSize, minMaxFailures, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange, maxConnectionsRange,
                numFails, minMaxFails, processingType, sdn, useAws, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        RequestSet requestSet = generationService.generateRequests(params);
        processingService.processRequestSet(requestSet);
        return requestSet;
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                Integer numSources, Integer numDestinations, Integer fSetSize,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                Integer numFails, List<Integer> minMaxFails, String processingType, Boolean sdn,
                                                Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                                double percentDstFail){
        return SimulationParameters.builder()
                .seed(seed)
                .topologyId(topologyId)
                .numRequests(numRequests)
                .algorithm(alg)
                .problemClass(problemClass)
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
                .minMaxFailsAllowed(minMaxFails)
                .processingType(processingType)
                .sdn(sdn)
                .useAws(useAws)
                .percentSrcAlsoDest(percentSrcAlsoDest)
                .percentSrcFail(percentSrcFail)
                .percentDestFail(percentDstFail)
                .build();
    }
}
