package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.TestConfiguration;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.analyzed.PathSetMetrics;
import netlab.analysis.analyzed.RequestMetrics;
import netlab.analysis.enums.MemberType;
import netlab.analysis.services.AnalysisService;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.*;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
@Slf4j
public class CombinedModelTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private ProcessingService processingService;

    @Autowired
    private AnalysisService analysisService;


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
        boolean survivable = true;
        RequestSet rs = createCombinedRequestSet(numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Flow: 1 Min for each pair
    @Test
    public void flow1MinPair(){
        int numSources = 4;
        int numDestinations = 4;
        int fSetSize = 12;
        String failureClass = "Link";
        int numConnections = 7;
        List<Integer> minConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxConnectionsRange = Arrays.asList(7, 7);
        List<Integer> minSrcConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(7, 7);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(7, 7);
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        RequestSet rs = createCombinedRequestSet(numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Flow: 1 Max for each pair
    @Test
    public void flow1MaxPair(){
        int numSources = 7;
        int numDestinations = 7;
        int fSetSize = 12;
        String failureClass = "Link";
        int numConnections = 7;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(7, 7);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(7, 7);
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        RequestSet rs = createCombinedRequestSet(numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Flow + Link-Disjoint Backup Path for each member
    @Test
    public void flow1MaxPairLinkDisjointBackup(){
        int numSources = 3;
        int numDestinations = 3;
        int fSetSize = 21;
        String failureClass = "Link";
        int numConnections = 9;
        List<Integer> minConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(7, 7);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(7, 7);
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        RequestSet rs = createCombinedRequestSet(numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Flow - Max 1 per pair, no Failures
    @Test
    public void flow1MaxPair3Conns(){
        int numSources = 3;
        int numDestinations = 3;
        int fSetSize = 0;
        String failureClass = "Link";
        int numConnections = 9;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(7, 7);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(7, 7);
        int numFailsAllowed = 0;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        RequestSet rs = createCombinedRequestSet(numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Endpoint: 1 Min for each source, 1 min for each destination, 2 max for each source, 3 max for each destination
    // Flow - Max 1 per pair, no Failures
    @Test
    public void endpoint1Min2MaxSrc2Min3MaxDst(){
        int numSources = 3;
        int numDestinations = 3;
        int fSetSize = 21;
        String failureClass = "Link";
        int numConnections = 6;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(9, 9);
        List<Integer> minSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(2, 2);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(3, 3);
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        RequestSet rs = createCombinedRequestSet(numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);

    }

    // Flow + Endpoint Restrictions

    // Unicast

    // Unicast + Link-Disjoint Backup Path

    // Unicast + Node-Disjoint Backup Path

    // Anycast

    // Anycast + Link-Disjoint Backup Path

    // Anycast + Node-Disjoint Backup Path

    // Multicast

    // Multicast + Link-Disjoint Paths

    // Multicast + Node-Disjoint Paths

    // Multicast + Destination-Node Disjoint Paths

    // Manycast

    // Broadcast

    // Many-to-Many (Min 1 from each source)

    // Many-to-Many (Min 1 to each destination)

    // Many-to-Many (Min 1 from each s, 1 to each d)

    // Many-to-Many (Min 1 from each s, 1 to each d, 1 backup path for each)

    private void printFailureSet(Set<Failure> failures){
        System.out.println("Failure set: " + failures.stream().map(f -> {
            if(f.getLink() != null){
                return String.format("(%s, %s)", f.getLink().getOrigin().getId(), f.getLink().getTarget().getId());
            }
            else{
                return f.getNode().getId();
            }
        }).collect(Collectors.joining(", ")));
    }

    private void printNodeSet(Set<Node> nodes, String title){
        System.out.println(title + ": " + nodes.stream().map(Node::getId).collect(Collectors.joining(", ")));
    }

    private void printMap(Map<SourceDestPair, Map<String, Path>> pathMap) {
        for(SourceDestPair pair : pathMap.keySet()){;
            Map<String, Path> paths = pathMap.get(pair);
            if(paths.size() > 0) {
                System.out.println(String.format("Pair: (%s, %s)", pair.getSrc().getId(), pair.getDst().getId()));
                for (String pathId : paths.keySet()) {
                    System.out.println(pathId + ": " + paths.get(pathId).toString());
                }
                System.out.println("-----");
            }
        }
    }

    private void testSolution(RequestSet rs, AnalyzedSet as, Boolean survivable, int numConnections,
                              List<Integer> minConnectionsRange, List<Integer> minSrcConnectionsRange,
                              List<Integer> minDstConnectionsRange) {

        Request r = rs.getRequests().values().iterator().next();
        printNodeSet(r.getSources(), "Sources");
        printNodeSet(r.getDestinations(), "Destinations");
        printFailureSet(r.getFailures().getFailureSet());
        printMap(r.getChosenPaths());

        assert(survivable ? as.getTotalSurvivable() == 1 : as.getTotalSurvivable() == 0);

        RequestMetrics rm = as.getRequestMetrics().values().iterator().next();
        assert(rm.getNumIntactPaths() >= numConnections);

        Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap = rm.getMemberPathSetMetricsMap();
        for(MemberType memberType : memberPathSetMetricsMap.keySet()){
            Map<Node, Map<SourceDestPair, PathSetMetrics>> pathsSetMapsForType = memberPathSetMetricsMap.get(memberType);
            for(Node node : pathsSetMapsForType.keySet()){
                Map<SourceDestPair, PathSetMetrics> pathMap = pathsSetMapsForType.get(node);
                int nodePaths = pathMap.values().stream().map(PathSetMetrics::getNumPaths).reduce(0, (m1, m2) -> m1 + m2);
                assert(memberType.equals(MemberType.Source) ? nodePaths >= minSrcConnectionsRange.get(0) : nodePaths >= minDstConnectionsRange.get(0));
            }
        }

        Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap = rm.getPathSetMetricsMap();
        for(SourceDestPair pair : pathSetMetricsMap.keySet()){
            PathSetMetrics psm = pathSetMetricsMap.get(pair);
            assert(psm.getNumPaths() >= minConnectionsRange.get(0));
        }
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
