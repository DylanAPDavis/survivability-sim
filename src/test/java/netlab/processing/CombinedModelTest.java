package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.TestConfiguration;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.*;
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
@Slf4j
public class CombinedModelTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private ProcessingService processingService;


    @Test
    public void createRequestSetTest(){
        int numSources = 4;
        int numDestinations = 4;
        int fSetSize = 4;
        String failureClass = "Link";
        int numConnections = 7;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(7, 7);
        List<Integer> minSrcConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(7, 7);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(7, 7);
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        RequestSet rs = createCombinedRequestSet(numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);

    }

    private void verify(RequestSet rs, int numSources, int numDestinations, int fSetSize, String failureClass,
                        int numConnections, List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                        List<Integer> minSrcConnectionsRange, List<Integer> maxSrcConnectionsRange,
                        List<Integer> minDstConnectionsRange, List<Integer> maxDstConnectionsRange,
                        int numFailsAllowed, double percentSrcAlsoDest, double percentSrcFail,
                        double percentDstFail){
        assert(rs.getProblemClass().equals(ProblemClass.Combined));
        Request r = rs.getRequests().values().iterator().next();
        assert(r.getSources().size() == numSources);
        assert(r.getDestinations().size() == numDestinations);
        assert(rs.getFailureClass().equals(FailureClass.valueOf(failureClass)));
        Connections connections = r.getConnections();
        assert(connections.getNumConnections() == numConnections);
        assert(connections.getPairMinConnectionsMap().values().stream().allMatch(c -> c.equals(minConnectionsRange.get(0))));
        assert(connections.getPairMaxConnectionsMap().values().stream().allMatch(c -> c.equals(maxConnectionsRange.get(0))));
        assert(connections.getSrcMinConnectionsMap().values().stream().allMatch(c -> c.equals(minSrcConnectionsRange.get(0))));
        assert(connections.getSrcMaxConnectionsMap().values().stream().allMatch(c -> c.equals(maxSrcConnectionsRange.get(0))));
        assert(connections.getDstMinConnectionsMap().values().stream().allMatch(c -> c.equals(minDstConnectionsRange.get(0))));
        assert(connections.getDstMaxConnectionsMap().values().stream().allMatch(c -> c.equals(maxDstConnectionsRange.get(0))));
        Failures failures = r.getFailures();
        assert(failures.getFailureSet().size() == fSetSize);
        assert(failures.getPairFailuresMap().values().stream().noneMatch(fails -> fails.size() > 0));
        assert(failures.getSrcFailuresMap().values().stream().noneMatch(fails -> fails.size() > 0));
        assert(failures.getDstFailuresMap().values().stream().noneMatch(fails -> fails.size() > 0));
        assert(failures.getFailureGroups().size() == fSetSize);
        assert(failures.getPairFailureGroupsMap().values().stream().noneMatch(fails -> fails.size() > 0));
        assert(failures.getSrcFailureGroupsMap().values().stream().noneMatch(fails -> fails.size() > 0));
        assert(failures.getDstFailureGroupsMap().values().stream().noneMatch(fails -> fails.size() > 0));
        NumFailsAllowed nfa = r.getNumFailsAllowed();
        assert(nfa.getTotalNumFailsAllowed() == numFailsAllowed);
        assert(nfa.getPairNumFailsAllowedMap().values().stream().noneMatch(fa -> fa.equals(0)));
        assert(nfa.getSrcNumFailsAllowedMap().values().stream().noneMatch(fa -> fa.equals(0)));
        assert(nfa.getDstNumFailsAllowedMap().values().stream().noneMatch(fa -> fa.equals(0)));
        assert(rs.getPercentSrcFail() == percentSrcFail);
        assert(rs.getPercentDestFail() == percentDstFail);
        assert(rs.getPercentSrcAlsoDest() == percentSrcAlsoDest);
    }

    private RequestSet createCombinedRequestSet(int numSources, int numDestinations, int fSetSize, String failureClass,
                                                int numConnections, List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                List<Integer> minSrcConnectionsRange, List<Integer> maxSrcConnectionsRange,
                                                List<Integer> minDstConnectionsRange, List<Integer> maxDstConnectionsRange,
                                                int numFailsAllowed, double percentSrcAlsoDest, double percentSrcFail,
                                                double percentDstFail){
        return createRequestSet(1L, "NSFnet", 1, "ServiceILP", "Combined",
                "TotalCost", numSources, numDestinations, fSetSize, new ArrayList<>(), failureClass, 1.0,
                new ArrayList<>(), numConnections, minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange,
                maxSrcConnectionsRange, minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, new ArrayList<>(),
                "Solo", false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail);
    }

    private RequestSet createRequestSet(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                        String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                        List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                        List<Double> minMaxFailureProb, Integer numConnections,
                                        List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                        List<Integer> minSrcConnectionsRange, List<Integer> maxSrcConnectionsRange,
                                        List<Integer> minDstConnectionsRange, List<Integer> maxDstConnectionsRange,
                                        Integer numFailsAllowed, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                        Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                        double percentDstFail){

        SimulationParameters params = makeParameters(seed, topologyId, numRequests, alg, problemClass, objective, numSources, numDestinations,
                fSetSize, minMaxFailures, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange, maxConnectionsRange,
                minSrcConnectionsRange, maxSrcConnectionsRange, minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed,
                minMaxFailsAllowed, processingType, sdn, useAws, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        RequestSet requestSet = generationService.generateFromSimParams(params);
        return requestSet;
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                List<Integer> minSrcConnectionsRange, List<Integer> maxSrcConnectionsRange,
                                                List<Integer> minDstConnectionsRange, List<Integer> maxDstConnectionsRange,
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
                .minSrcConnectionsRange(minSrcConnectionsRange)
                .maxSrcConnectionsRange(maxSrcConnectionsRange)
                .minDstConnectionsRange(minDstConnectionsRange)
                .maxDstConnectionsRange(maxDstConnectionsRange)
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
