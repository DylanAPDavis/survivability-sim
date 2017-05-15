package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.analyzed.RequestMetrics;
import netlab.analysis.services.AnalysisService;
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
public class NoFailuresTest {


    @Autowired
    private GenerationService generationService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ProcessingService processingService;

    @Test
    public void oneSrcOneDstOneC(){
        // 1 S, 1 D, 0 F, 1 C, 0 NF
        solveAndAnalyze(1, 1, 1);
    }

    @Test
    public void oneSrcOneDstTwoC(){
        // 1 S, 1 D, 0 F, 2 C, 0 NF
        solveAndAnalyze(1, 1, 2);
    }

    @Test
    public void oneSrcTwoDstOneC() {
        // 1 S, 2 D, 0 F, 1 C, 0 NF
        solveAndAnalyze(1, 2, 1);
    }

    @Test
    public void oneSrcTwoDstTwoC() {
        // 1 S, 2 D, 0 F, 2 C, 0 NF
        solveAndAnalyze(1, 2, 2);
    }

    @Test
    public void oneSrcTwoDstThreeC(){
        // 1 S, 2 D, 0 F, 3 C, 0 NF
        solveAndAnalyze(1, 2, 3);
    }

    @Test
    public void oneSrcThreeDestOneC(){
        solveAndAnalyze(1, 3, 1);
    }

    @Test
    public void oneSrcThreeDestTwoC(){
        solveAndAnalyze(1, 3, 2);
    }

    @Test
    public void oneSrcThreeDestThreeC(){
        solveAndAnalyze(1, 3, 3);
    }

    @Test
    public void twoSrcOneDestOneC(){
        solveAndAnalyze(2, 1, 1);
    }

    @Test
    public void twoSrcOneDestTwoC(){
        solveAndAnalyze(2, 1, 2);
    }

    @Test
    public void twoSrcTwoDestOneC(){
        solveAndAnalyze(2, 2, 1);
    }

    @Test
    public void twoSrcTwoDestTwoC(){
        solveAndAnalyze(2, 2, 2);
    }

    @Test
    public void twoSrcTwoDestThreeC(){
        solveAndAnalyze(2, 2, 2);
    }

    @Test
    public void threeSrcOneDestOneC(){
        solveAndAnalyze(3, 1, 1);
    }

    @Test
    public void threeSrcOneDestTwoC(){
        solveAndAnalyze(3, 1, 2);
    }

    @Test
    public void threeSrcOneDestThreeC(){
        solveAndAnalyze(3, 1, 3);
    }

    @Test
    public void threeSrcTwoDestOneC(){
        solveAndAnalyze(3, 2, 1);
    }

    @Test
    public void threeSrcTwoDestTwoC(){
        solveAndAnalyze(3, 2, 2);
    }

    @Test
    public void threeSrcTwoDestThreeC(){
        solveAndAnalyze(3, 2, 3);
    }

    @Test
    public void threeSrcThreeDestOneC(){
        solveAndAnalyze(3, 3, 1);
    }

    @Test
    public void threeSrcThreeDestTwoC(){
        solveAndAnalyze(3, 3, 2);
    }

    @Test
    public void threeSrcThreeDestThreeC(){
        solveAndAnalyze(3, 3, 3);
    }

    @Test
    public void fourteenSrcOneDestOneC(){
        solveAndAnalyze(14, 1, 1);
    }

    @Test
    public void fourteenSrcOneDestFourteenC(){
        solveAndAnalyze(14, 1, 14);
    }

    @Test
    public void oneSrcFourteenDestOneC(){
        solveAndAnalyze(1, 14, 1);
    }

    @Test
    public void oneSrcFourteenDestFourteenC(){
        solveAndAnalyze(1, 14, 14);
    }

    @Test
    public void fourteenSrcFourteenDestOneC(){
        solveAndAnalyze(14, 14, 1);
    }

    @Test
    public void fourteenSrcFourteenDestFourteenC(){
        solveAndAnalyze(14, 14, 14);
    }

    @Test
    public void threeSrcThreeDestOneCOneSrcAlsoDest(){
        solveAndAnalyzeSrcDestOverlap(3, 3, 3, .33);
    }

    @Test
    public void threeSrcThreeDestOneCTwoSrcAlsoDest(){
        solveAndAnalyzeSrcDestOverlap(3, 3, 3, .66);
    }

    @Test
    public void threeSrcThreeDestOneCThreeSrcAlsoDest(){
        solveAndAnalyzeSrcDestOverlap(3, 3, 3, 1.0);
    }

    private void solveAndAnalyzeSrcDestOverlap(Integer numSources, Integer numDestinations, Integer numConnections, Double percentSrcAlsoDest){
        RequestSet rs1 = solve(1L, "NSFnet", 1, "ServiceILP", "Flex",
                numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, percentSrcAlsoDest, 0.0, 0.0);
        analyze(rs1, numConnections, true);
        // Endpoint
        RequestSet rs2 = solve(1L, "NSFnet", 1, "ServiceILP", "Endpoint",
                numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, percentSrcAlsoDest, 0.0, 0.0);
        analyze(rs2, numConnections, true);
        // Flow
        RequestSet rs3 = solve(1L, "NSFnet", 1, "ServiceILP", "Flow",
                numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, percentSrcAlsoDest, 0.0, 0.0);
        analyze(rs3, numConnections, true);
        analyzeMultiSet(Arrays.asList(rs1, rs2, rs3));
    }

    private void solveAndAnalyze(Integer numSources, Integer numDestinations, Integer numConnections){
        RequestSet rs1 = solve(1L, "NSFnet", 1, "ServiceILP", "Flex",
                numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, 0.0, 0.0, 0.0);
        analyze(rs1, numConnections, true);
        // Endpoint
        RequestSet rs2 = solve(1L, "NSFnet", 1, "ServiceILP", "Endpoint",
                numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, 0.0, 0.0, 0.0);
        analyze(rs2, numConnections, true);
        // Flow
        RequestSet rs3 = solve(1L, "NSFnet", 1, "ServiceILP", "Flow",
                numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, 0.0, 0.0, 0.0);
        analyze(rs3, numConnections, true);
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