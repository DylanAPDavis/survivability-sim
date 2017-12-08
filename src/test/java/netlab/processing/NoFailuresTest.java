package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.Request;
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
    public void oneSrcFourteenDestOneC(){
        solveAndAnalyzeSrcDestOverlap(1, 14, 1, "all");
    }

    @Test
    public void oneSrcFourteenDestFourteenC(){
        solveAndAnalyze(1, 14, 14);
    }

    @Test
    public void fourteenSrcFourteenDestOneC(){
        solveAndAnalyzeSrcDestOverlap(14, 14, 1, "all");
    }

    @Test
    public void fourteenSrcFourteenDestFourteenC(){
        solveAndAnalyzeSrcDestOverlap(14, 14, 14, "all");
    }

    @Test
    public void threeSrcThreeDestOneCThreeSrcAlsoDest(){
        solveAndAnalyzeSrcDestOverlap(3, 3, 3, "all");
}

    @Test
    public void clusterFailureSeed2(){
        solve(2L, "NSFnet", 10, "ILP", "Endpoint", "LinksUsed", 7, 7,
                0, new ArrayList<>(), "Link", 1.0, new ArrayList<>(),
                1, Arrays.asList(0,0), Arrays.asList(14,14), 0, new ArrayList<>(),
                "Solo", false, false, "half", "prevent", "prevent");
    }

    private void solveAndAnalyzeSrcDestOverlap(Integer numSources, Integer numDestinations, Integer numConnections,
                                               String sourceSubsetDestType){
        Request rs1 = solve(1L, "NSFnet", 1, "ILP", "Flex",
                "TotalCost", numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, sourceSubsetDestType, "prevent", "prevent");
        analyze(rs1, numConnections, true);
        // Endpoint
        Request rs2 = solve(1L, "NSFnet", 1, "ILP", "Endpoint",
                "TotalCost", numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, sourceSubsetDestType, "prevent", "prevent");
        analyze(rs2, numConnections, true);
        // Flow
        Request rs3 = solve(1L, "NSFnet", 1, "ILP", "Flow",
                "TotalCost", numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, sourceSubsetDestType, "prevent", "prevent");
        analyze(rs3, numConnections, true);
        analyzeMultiSet(Arrays.asList(rs1, rs2, rs3));
    }

    private void solveAndAnalyze(Integer numSources, Integer numDestinations, Integer numConnections){
        // Flex
        Request rs1 = solve(1L, "NSFnet", 1, "ILP", "Flex",
                "TotalCost", numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, "none", "prevent", "prevent");
        analyze(rs1, numConnections, true);
        // Endpoint
        Request rs2 = solve(1L, "NSFnet", 1, "ILP", "Endpoint",
                "TotalCost",numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, "none", "prevent", "prevent");
        analyze(rs2, numConnections, true);
        // Flow
        Request rs3 = solve(1L, "NSFnet", 1, "ILP", "Flow",
                "TotalCost", numSources, numDestinations, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), numConnections, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, "none", "prevent", "prevent");
        analyze(rs3, numConnections, true);
        analyzeMultiSet(Arrays.asList(rs1, rs2, rs3));
    }

    private void analyzeMultiSet(List<Request> requests) {
        Request rs1 = requests.get(0);
        Map<SourceDestPair, Map<String, Path>> chosenPaths1 = rs1.getDetails().getChosenPaths();
        Integer numLinkUsages1 = chosenPaths1.values().stream().map(Map::values).flatMap(Collection::stream).map(p -> p.getLinks().size()).reduce(0, (p1, p2) -> p1 + p2);
        for(Request rs : requests){
            Map<SourceDestPair, Map<String, Path>> chosenPaths = rs.getDetails().getChosenPaths();
            Integer numLinkUsages = chosenPaths.values().stream().map(Map::values).flatMap(Collection::stream).map(p -> p.getLinks().size()).reduce(0, (p1, p2) -> p1 + p2);
            assert(Objects.equals(numLinkUsages, numLinkUsages1));
        }
    }

    private void analyze(Request request, int numExpectedPaths, boolean survivable){
        Analysis analysis = analysisService.analyzeRequest(request);
        /*assert(analysis.getRequestMetrics().values().stream().allMatch(rsm -> rsm.getIsSurvivable() == survivable));
        assert(analysis.getRequestMetrics().values().stream().allMatch(rsm -> rsm.getNumPaths() == numExpectedPaths));
        assert(request.getDetails().values().stream()
                .allMatch(r ->
                        r.getChosenPaths().keySet().stream()
                                .filter(pair -> pair.getSrc().equals(pair.getDst()))
                                .allMatch(p -> r.getChosenPaths().get(p).values().size() == 0)));*/
    }

    private Request solve(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                          String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                          List<Integer> minMaxFailures, String failureClass, Double failureProb,
                          List<Double> minMaxFailureProb, Integer numConnections,
                          List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                          Integer numFails, List<Integer> minMaxFails, String processingType, Boolean sdn,
                          Boolean useAws, String sourceSubsetDestType, String sourceFailureType,
                          String destFailureType){

        SimulationParameters params = makeParameters(seed, topologyId, numRequests, alg, problemClass, objective, numSources, numDestinations,
                fSetSize, minMaxFailures, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange, maxConnectionsRange,
                numFails, minMaxFails, processingType, sdn, useAws, sourceSubsetDestType, sourceFailureType, destFailureType);
        Request request = generationService.generateFromSimParams(params);
        processingService.processRequest(request);
        return request;
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                Integer numFails, List<Integer> minMaxFails, String processingType, Boolean sdn,
                                                Boolean useAws, String sourceSubsetDestType, String sourceFailureType,
                                                String destFailureType){
        return SimulationParameters.builder()
                .seed(seed)
                .topologyId(topologyId)
                .algorithm(alg)
                .problemClass(problemClass)
                .objective(objective)
                .numSources(numSources)
                .numDestinations(numDestinations)
                .failureSetSize(fSetSize)
                .failureClass(failureClass)
                .failureProb(failureProb)
                .minConnections(numConnections)
                .numFailureEvents(numFails)
                .useAws(useAws)
                .sourceSubsetDestType(sourceSubsetDestType)
                .sourceFailureType(sourceFailureType)
                .destFailureType(destFailureType)
                .build();
    }
}
