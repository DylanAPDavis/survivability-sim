package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.analyzed.RequestMetrics;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
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
public class FailuresTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ProcessingService processingService;

    /*A - 1 F, 1 NF
    A - 2 F, 1 NF,
    A - 2 F, 2 NF
    A - 3 F, 1 NF
    A - 3 F, 2 NF
    A - 3 F, 3 NF
    A - 1 F c S, 1 NF X
    A - 1 F c D, 1 NF X*/

    @Test
    public void oneSrcOneDstOneFailOneConnOneNFA(){
        solveAndAnalyzeSrcDestOverlap(1, 1, 1, 1, 1, "Node", 0.0, 0.0, 0.0, true);
    }

    @Test
    public void oneSrcOneDst14FailOneConnOneNFA(){
        // Only can get 12
        solveAndAnalyzeSrcDestOverlap(1, 1, 1, 14, 1, "Node", 0.0, 0.0, 0.0, true);
    }

    @Test
    public void oneSrcOneDst14FailOneConnOneNFASrcFails(){
        // Only can get 12
        solveAndAnalyzeSrcDestOverlap(1, 1, 1, 14, 1, "Node", 0.0, 1.0, 0.0, true);
    }

    private void solveAndAnalyzeSrcDestOverlap(Integer numSources, Integer numDestinations, Integer numConnections,
                                               Integer fSize, Integer nfa, String failureClass,
                                               Double percentSrcAlsoDest, Double percentSrcFail,
                                               Double percentDstFail, Boolean survivable){
        RequestSet rs1 = solve(1L, "NSFnet", 1, "ServiceILP", "Flex",
                numSources, numDestinations, fSize, new ArrayList<>(), failureClass, 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), nfa, new ArrayList<>(), "Solo",
                false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        analyze(rs1, numConnections, survivable);
        // Endpoint
        RequestSet rs2 = solve(1L, "NSFnet", 1, "ServiceILP", "Endpoint",
                numSources, numDestinations, fSize, new ArrayList<>(), failureClass, 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), nfa, new ArrayList<>(), "Solo",
                false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        analyze(rs2, numConnections, survivable);
        // Flow
        RequestSet rs3 = solve(1L, "NSFnet", 1, "ServiceILP", "Flow",
                numSources, numDestinations, fSize, new ArrayList<>(), failureClass, 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), nfa, new ArrayList<>(), "Solo",
                false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        analyze(rs3, numConnections, survivable);
        analyzeMultiSet(Arrays.asList(rs1, rs2, rs3));
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
        assert(analyzedSet.getRequestMetrics().values().stream().allMatch(rsm -> rsm.getRequestIsSurvivable() == survivable));
        assert(analyzedSet.getRequestMetrics().values().stream().allMatch(rsm -> rsm.getNumPaths() == numExpectedPaths));
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
