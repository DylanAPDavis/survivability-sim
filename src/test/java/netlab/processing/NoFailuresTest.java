package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.analyzed.RequestMetrics;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

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
    public void oneSourceOneDest(){
        String problemClass = "Flex";
        // 1 S, 1 D, 0 F, 1 C, 0 NF
        RequestSet requestSet = solve(1L, "NSFnet", 1, "ServiceILP", problemClass,
                1, 1, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), 1, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, 0.0, 0.0, 0.0);
        analyze(requestSet, 1, true);

        // 1 S, 1 D, 0 F, 2 C, 0 NF
        requestSet = solve(1L, "NSFnet", 1, "ServiceILP", problemClass,
                1, 1, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, 0.0, 0.0, 0.0);
        analyze(requestSet, 2, true);
    }

    @Test
    public void oneSourceTwoDest(){
        // 1 S, 2 D, 0 F, 1 C, 0 NF
        RequestSet requestSet = solve(1L, "NSFnet", 1, "ServiceILP", "Flex",
                1, 2, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), 1, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, 0.0, 0.0, 0.0);
        analyze(requestSet, 1, true);

        // Endpoint
        requestSet = solve(1L, "NSFnet", 1, "ServiceILP", "Endpoint",
                1, 2, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), 1, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, 0.0, 0.0, 0.0);
        analyze(requestSet, 1, true);

        // Flow
        requestSet = solve(1L, "NSFnet", 1, "ServiceILP", "Flow",
                1, 2, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), 1, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, 0.0, 0.0, 0.0);
        analyze(requestSet, 1, true);

        // 1 S, 2 D, 0 F, 2 C, 0 NF
        requestSet = solve(1L, "NSFnet", 1, "ServiceILP", "Flex",
                1, 2, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, 0.0, 0.0, 0.0);
        analyze(requestSet, 2, true);

        // 1 S, 2 D, 0 F, 3 C, 0 NF
        requestSet = solve(1L, "NSFnet", 1, "ServiceILP", "Flex",
                1, 2, 0, new ArrayList<>(), "Both", 1.0,
                new ArrayList<>(), 3, new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), "Solo",
                false, false, 0.0, 0.0, 0.0);
        analyze(requestSet, 3, true);
    }

    private void analyze(RequestSet requestSet, int numExpectedPaths, boolean survivable){
        AnalyzedSet analyzedSet = analysisService.analyzeRequestSet(requestSet);
        assert(analyzedSet.getRequestMetrics().values().stream().allMatch(rsm -> rsm.getRequestIsSurvivable() == survivable));
        assert(analyzedSet.getRequestMetrics().values().stream().allMatch(rsm -> rsm.getNumPaths() == numExpectedPaths));
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
