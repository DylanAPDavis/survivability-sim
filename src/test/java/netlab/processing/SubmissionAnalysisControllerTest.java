package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.AggregateAnalyzedSet;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.controller.AnalysisController;
import netlab.storage.controller.StorageController;
import netlab.submission.controller.SubmissionController;
import netlab.submission.request.SimulationParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class SubmissionAnalysisControllerTest {

    @Autowired
    SubmissionController submissionController;

    @Autowired
    AnalysisController analysisController;

    @Autowired
    StorageController storageController;


    @Test
    public void submitTest1(){
        SimulationParameters params = makeParameters(1L, "NSFnet", 5, "ServiceILP",
                "Flex", "TotalCost", 3, 2, 4, new ArrayList<>(),
                "Link", 1.0, new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(),
                1, new ArrayList<>(), "Solo", false, true, 0.0,
                0.0, 0.0);
        String requestSetId = submissionController.submitRequestSet(params);
        AnalysisParameters analysisParameters = AnalysisParameters.builder().requestSetId(requestSetId).useAws(true).build();
        analysisController.analyzeRequestSet(analysisParameters);
        // Submit and analyze a new one
        requestSetId = submissionController.submitRequestSet(params);
        analysisParameters = AnalysisParameters.builder().requestSetId(requestSetId).useAws(true).build();
        analysisController.analyzeRequestSet(analysisParameters);

        // Get all the analyzed sets back from AWS
        params = SimulationParameters.builder().useAws(true).objective("TotalCost").build();
        List<AnalyzedSet> analyzedSets = storageController.getAnalyzedSets(params);
        assert(analyzedSets.size()>=2);
    }

    @Test
    public void submitTest2(){
        SimulationParameters params = makeParameters(2L, "NSFnet", 5, "ServiceILP",
                "Flex", "TotalCost", 3, 2, 4, new ArrayList<>(),
                "Link", 1.0, new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(),
                1, new ArrayList<>(), "Solo", false, true, 0.0,
                0.0, 0.0);
        String requestSetId = submissionController.submitRequestSet(params);
        AnalysisParameters analysisParameters = AnalysisParameters.builder().requestSetId(requestSetId).useAws(true).build();
        analysisController.analyzeRequestSet(analysisParameters);
        List<AnalyzedSet> analyzedSets = storageController.getAnalyzedSets(params);
        assert(analyzedSets.size()>=1);
    }

    @Test
    public void aggregateTest(){
        SimulationParameters params = makeParameters(null, "NSFnet", 5, "ServiceILP",
                "Flex", "TotalCost", 3, 2, 4, new ArrayList<>(),
                "Link", 1.0, new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(),
                1, new ArrayList<>(), "Solo", false, true, 0.0,
                0.0, 0.0);
        AggregateAnalyzedSet aas = analysisController.aggregateAnalyzedSets(params);
        System.out.println(aas);
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
