package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.AggregateAnalysis;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.controller.AnalysisController;
import netlab.storage.controller.StorageController;
import netlab.submission.controller.SubmissionController;
import netlab.submission.request.SimulationParameters;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class SubmissionAnalysisControllerTest {

    @Autowired
    SubmissionController submissionController;

    @Autowired
    AnalysisController analysisController;

    @Autowired
    StorageController storageController;


    //@Test
    public void submitTest1(){
        SimulationParameters params = makeParameters(1L, false, "NSFnet", 5, "ILP",
                "Flex", "TotalCost", 3, 2, 4, new ArrayList<>(),
                "Link", 1.0, new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(),
                1, new ArrayList<>(), "Solo", false, true, "none",
                "prevent", "prevent");
        String requestSetId = submissionController.submitRequest(params);
        AnalysisParameters analysisParameters = AnalysisParameters.builder().requestId(requestSetId).useAws(true).build();
        analysisController.analyzeRequest(analysisParameters);

        // Get all the analyzed sets back from AWS
        params = SimulationParameters.builder().useAws(true).objective("TotalCost").build();
        List<Analysis> analyses = storageController.getMatchingAnalysis(params);
        assert(analyses.size()>=1);
    }

    //@Test
    public void submitTest2(){
        SimulationParameters params = makeParameters(2L, false, "NSFnet", 5, "ILP",
                "Flex", "TotalCost", 3, 2, 4, new ArrayList<>(),
                "Link", 1.0, new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(),
                1, new ArrayList<>(), "Solo", false, true, "none",
                "prevent", "prevent");
        String requestSetId = submissionController.submitRequest(params);
        AnalysisParameters analysisParameters = AnalysisParameters.builder().requestId(requestSetId).useAws(true).build();
        analysisController.analyzeRequest(analysisParameters);
        params = SimulationParameters.builder().useAws(true).objective("TotalCost").build();
        List<Analysis> analyses = storageController.getMatchingAnalysis(params);
        assert(analyses.size()>=1);
    }

    //@Test
    public void aggregateTest(){
        SimulationParameters params = makeParameters(null, true, "NSFnet", 5, "ILP",
                "Flex", "TotalCost", 3, 2, 4, new ArrayList<>(),
                "Link", 1.0, new ArrayList<>(), 2, new ArrayList<>(), new ArrayList<>(),
                1, new ArrayList<>(), "Solo", false, true, "none",
                "prevent", "prevent");
        AggregateAnalysis aas = analysisController.aggregateAnalyses(params);
        System.out.println(aas);
    }


    private SimulationParameters makeParameters(Long seed, Boolean completed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                Integer numFails, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
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
