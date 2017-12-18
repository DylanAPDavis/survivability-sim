package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.controller.AnalysisController;
import netlab.submission.controller.SubmissionController;
import netlab.submission.request.SimulationParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class MetricsTest {

    @Autowired
    SubmissionController submissionController;

    @Autowired
    AnalysisController analysisController;


    @Test
    public void shortestPathTest(){

        SimulationParameters params = SimulationParameters.builder()
                .seed(1L)
                .topologyId("NSFnet")
                .algorithm("shortestpath")
                .problemClass("combined")
                .objective("totalcost")
                .routingType("broadcast")
                .numSources(4)
                .numDestinations(4)
                .failureScenario("default")
                .numFailureEvents(0)
                .sourceSubsetDestType("half")
                .useAws(false)
                .build();
        String requestId = submissionController.submitRequest(params);

        AnalysisParameters analysisParameters = AnalysisParameters.builder()
                .requestId(requestId)
                .useAws(false)
                .build();

        Analysis analysis = analysisController.analyzeRequest(analysisParameters);
        assert(analysis.getIsFeasible());
    }
}
