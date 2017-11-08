package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class AnalysisTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ProcessingService processingService;


    @Test
    public void allFeasibleNoFailures(){
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(1, 2);
        Request rs = createSetWithGenService("Flex", 3, 3, 2, minConns, maxConns, 0,
                0, "Both", 0.0, 0.0, 0.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void allFeasibleFlexFailures(){
        Integer numFails = 10;
        Integer nfa = 2;
        Request rs = createSetWithGenService("Flex", 3, 3, 2, new ArrayList<>(),
                new ArrayList<>(), numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void allFeasibleFlowSharedFFailures(){
        Integer numFails = 10;
        Integer nfa = 1;
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Request rs = createSetWithGenService("FlowSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void allFeasibleEndpointSharedFFailures(){
        Integer numFails = 10;
        Integer nfa = 1;
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Request rs = createSetWithGenService("EndpointSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void allFeasibleFlowFailures(){
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Integer numFails = 10;
        Integer nfa = 1;
        Request rs = createSetWithGenService("Flow", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void allFeasibleEndpointFailures(){
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Integer numFails = 10;
        Integer nfa = 1;
        Request rs = createSetWithGenService("Endpoint", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    // < 100% feasible
    @Test
    public void ninetyPercentFeasibleFlexFailures(){
        List<Integer> minConns = new ArrayList<>();
        List<Integer> maxConns = new ArrayList<>();
        Integer numFails = 10;
        Integer nfa = 3;
        Request rs = createSetWithGenService("Flex", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void ninetyPercentFeasibleFlowSharedFFailures(){
        Integer numFails = 24;
        Integer nfa = 2;
        List<Integer> minConns = Arrays.asList(0, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Request rs = createSetWithGenService("FlowSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 0.0, 0.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void ninetyPercentFeasibleEndpointSharedFFailures(){
        Integer numFails = 24;
        Integer nfa = 2;
        List<Integer> minConns = Arrays.asList(0, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Request rs = createSetWithGenService("EndpointSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 0.0, 0.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void halfFeasibleFlowFailures(){
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(2,3);
        Integer numFails = 10;
        Integer nfa = 2;
        Request rs = createSetWithGenService("Flow", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void eightyPercentFeasibleEndpointFailures(){
        Integer numFails = 5;
        Integer nfa = 3;
        List<Integer> minConns = Arrays.asList(2, 2);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Request rs = createSetWithGenService("Endpoint", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 0.0, 0.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    // All infeasible
    @Test
    public void infeasibleFlexFailures(){
        List<Integer> minConns = new ArrayList<>();
        List<Integer> maxConns = new ArrayList<>();
        Integer numFails = 14;
        Integer nfa = 14;
        Request rs = createSetWithGenService("Flex", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Node", 0.0, 1.0, 1.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void infeasibleFlowSharedFFailures(){
        Integer numFails = 14;
        Integer nfa = 14;
        List<Integer> minConns = Arrays.asList(0, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Request rs = createSetWithGenService("FlowSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 1.0, 1.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void infeasibleEndpointSharedFFailures(){
        Integer numFails = 14;
        Integer nfa = 14;
        List<Integer> minConns = Arrays.asList(0, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Request rs = createSetWithGenService("EndpointSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 1.0, 1.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void infeasibleFlowFailures(){
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(2,3);
        Integer numFails = 14;
        Integer nfa = 14;
        Request rs = createSetWithGenService("Flow", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 1.0, 1.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void infeasibleEndpointFailures(){
        Integer numFails = 14;
        Integer nfa = 14;
        List<Integer> minConns = Arrays.asList(2, 2);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Request rs = createSetWithGenService("Endpoint", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 1.0, 1.0, 10);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    @Test
    public void exceptionTest(){
        Request rs = createSetAndSolve(1L, "NSFnet", 1, "ILP", "Flow",
                "TotalCost", 1, 7, 35, new ArrayList<>(), "Both",
                1.0, new ArrayList<>(), 7, Arrays.asList(1,1), Arrays.asList(1,1), 2,
                new ArrayList<>(), "Solo", false, false, 1.0, 1.0, 1.0);
        Analysis as = analysisService.analyzeRequest(rs);
    }

    private Request createSetWithGenService(String problemClass, int numSources, int numDestinations, int numConns,
                                            List<Integer> minConns, List<Integer> maxConns, Integer numFails,
                                            Integer nfa, String failureClass, double percentSrcAlsoDest, double percentSrcFail,
                                            double percentDstFail, int numRequests) {
        return createSetAndSolve(1L, "NSFnet", numRequests, "ILP", problemClass, "LinksUsed", numSources, numDestinations, numFails,
                new ArrayList<>(), failureClass, 0.0, new ArrayList<>(), numConns, minConns, maxConns,
                nfa, new ArrayList<>(), "Solo", false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail);
    }

    private Request createSetAndSolve(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                      String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                      List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                      List<Double> minMaxFailureProb, Integer numConnections,
                                      List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                      Integer numFailsAllowed, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                      Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                      double percentDstFail){

        SimulationParameters params = makeParameters(seed, topologyId, numRequests, alg, problemClass, objective, numSources, numDestinations,
                fSetSize, minMaxFailures, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange, maxConnectionsRange,
                numFailsAllowed, minMaxFailsAllowed, processingType, sdn, useAws, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        Request request = generationService.generateFromSimParams(params);
        processingService.processRequest(request);
        return request;
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
                .percentSrcAlsoDest(percentSrcAlsoDest)
                .percentSrcFail(percentSrcFail)
                .percentDstFail(percentDstFail)
                .build();
    }
}
