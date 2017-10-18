package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.TestConfiguration;
import netlab.analysis.analyzed.Analysis;
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
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    @Test
    public void compareModels(){
        int numSources = 7;
        int numDestinations = 7;
        int fSetSize = 21;
        String failureClass = "Link";
        int numConnections = 7;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> largeMaxConnectionsRange = Arrays.asList(100, 100);
        List<Integer> minSrcConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(2, 2);
        List<Integer> largeMaxSrcConnectionsRange = Arrays.asList(100, 100);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(2, 2);
        List<Integer> largeMaxDstConnectionsRange = Arrays.asList(100, 100);
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;

        System.out.println("Combined");
        Request rsCombined = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize,
                failureClass, numConnections, minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rsCombined = processingService.processRequestSet(rsCombined);
        verify(rsCombined, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        Analysis asCombined = analysisService.analyzeRequest(rsCombined);
        testSolution(rsCombined, asCombined, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);


        System.out.println("------------------------------");
        System.out.println("Flex");
        Request rsFlex = createCombinedRequestSet("Flex", numSources, numDestinations, fSetSize,
                failureClass, numConnections, minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rsFlex = processingService.processRequestSet(rsFlex);
        Analysis asFlex = analysisService.analyzeRequest(rsFlex);
        testSolution(rsFlex, asFlex, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);

        System.out.println("------------------------------");
        System.out.println("Flow");
        Request rsFlow = createCombinedRequestSet("FlowSharedF", numSources, numDestinations, fSetSize,
                failureClass, numConnections, minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, largeMaxSrcConnectionsRange,
                minDstConnectionsRange, largeMaxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rsFlow = processingService.processRequestSet(rsFlow);
        Analysis asFlow = analysisService.analyzeRequest(rsFlow);
        testSolution(rsFlow, asFlow, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);


        System.out.println("------------------------------");
        System.out.println("Combined - Flow");
        Request rsCombinedFlow = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize,
                failureClass, numConnections, minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, largeMaxSrcConnectionsRange,
                minDstConnectionsRange, largeMaxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rsCombinedFlow = processingService.processRequestSet(rsCombinedFlow);
        verify(rsCombinedFlow, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, largeMaxSrcConnectionsRange,
                minDstConnectionsRange, largeMaxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        Analysis asCombinedFlow = analysisService.analyzeRequest(rsCombinedFlow);
        testSolution(rsCombinedFlow, asCombinedFlow, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);


        System.out.println("------------------------------");
        System.out.println("Endpoint");
        Request rsEndpoint = createCombinedRequestSet("EndpointSharedF", numSources, numDestinations, fSetSize,
                failureClass, numConnections, minConnectionsRange, largeMaxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rsEndpoint = processingService.processRequestSet(rsEndpoint);
        Analysis asEndpoint = analysisService.analyzeRequest(rsEndpoint);
        testSolution(rsEndpoint, asEndpoint, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);

        System.out.println("------------------------------");
        System.out.println("Combined - Endpoint");
        Request rsCombinedEndpoint = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize,
                failureClass, numConnections, minConnectionsRange, largeMaxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rsCombinedEndpoint = processingService.processRequestSet(rsCombinedEndpoint);
        verify(rsCombinedEndpoint, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, largeMaxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        Analysis asCombinedEndpoint = analysisService.analyzeRequest(rsCombinedEndpoint);
        testSolution(rsCombinedEndpoint, asCombinedEndpoint, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);

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
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
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
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
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
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
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
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Endpoint: 1 Min for each source, 1 min for each destination, 2 max for each source, 3 max for each destination
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
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);

    }

    // Flow + Endpoint Restrictions
    @Test
    public void flowAndEndpointRestrictions(){
        int numSources = 3;
        int numDestinations = 3;
        int fSetSize = 21;
        String failureClass = "Link";
        int numConnections = 6;
        List<Integer> minConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(3, 3);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(3, 3);
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);

    }

    // Unicast
    @Test
    public void unicast(){
        int numSources = 1;
        int numDestinations = 1;
        int fSetSize = 0;
        String failureClass = "Link";
        int numConnections = 1;
        List<Integer> minConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        int numFailsAllowed = 0;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Unicast + Link-Disjoint Backup Path
    @Test
    public void unicastLinkDisjointBackup(){
        int numSources = 1;
        int numDestinations = 1;
        int fSetSize = 21;
        String failureClass = "Link";
        int numConnections = 1;
        List<Integer> minConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Unicast + Node-Disjoint Backup Path
    @Test
    public void unicastNodeDisjointBackup(){
        int numSources = 1;
        int numDestinations = 1;
        int fSetSize = 12;
        String failureClass = "Node";
        int numConnections = 1;
        List<Integer> minConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Anycast
    @Test
    public void anycast(){
        int numSources = 1;
        int numDestinations = 3;
        int fSetSize = 0;
        String failureClass = "Node";
        int numConnections = 1;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        Integer reachMinS = 1;
        Integer reachMaxS = 1;
        Integer reachMinD = 1;
        Integer reachMaxD = 1;
        int numFailsAllowed = 0;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, reachMinS, reachMaxS, reachMinD, reachMaxD,
                numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Anycast + Link-Disjoint Backup Path
    @Test
    public void anycastLinkDisjointBackup(){
        int numSources = 1;
        int numDestinations = 3;
        int fSetSize = 21;
        String failureClass = "Link";
        int numConnections = 1;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        Integer reachMinS = 1;
        Integer reachMaxS = 1;
        Integer reachMinD = 1;
        Integer reachMaxD = 1;
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, reachMinS, reachMaxS, reachMinD, reachMaxD,
                numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Anycast + Node-Disjoint Backup Path
    @Test
    public void anycastNodeDisjointBackup(){
        int numSources = 1;
        int numDestinations = 3;
        int fSetSize = 10;
        String failureClass = "Node";
        int numConnections = 1;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        Integer reachMinS = 1;
        Integer reachMaxS = 1;
        Integer reachMinD = 1;
        Integer reachMaxD = 1;
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, reachMinS, reachMaxS, reachMinD, reachMaxD,
                numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Multicast
    @Test
    public void multicast(){
        int numSources = 1;
        int numDestinations = 4;
        int numConnections = 4;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(4, 4);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(4, 4);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        int fSetSize = 0;
        String failureClass = "Node";
        int numFailsAllowed = 0;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        Integer reachMinS = 1;
        Integer reachMaxS = 1;
        Integer reachMinD = 3;
        Integer reachMaxD = 4;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, reachMinS, reachMaxS, reachMinD, reachMaxD,
                numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Multicast + Link-Disjoint Paths
    @Test
    public void multicastLinkDisjoint(){
        int numSources = 1;
        int numDestinations = 4;
        int numConnections = 4;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(4, 4);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(4, 4);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        int fSetSize = 21;
        String failureClass = "Link";
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        Integer reachMinS = 1;
        Integer reachMaxS = 1;
        Integer reachMinD = 4;
        Integer reachMaxD = 4;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, reachMinS, reachMaxS, reachMinD, reachMaxD,
                numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Multicast + Node-Disjoint Paths
    @Test
    public void multicastNodeDisjoint(){
        int numSources = 1;
        int numDestinations = 2;
        int numConnections = 2;
        List<Integer> minConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxConnectionsRange = Arrays.asList(2, 2);
        List<Integer> minSrcConnectionsRange = Arrays.asList(2, 2);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(3, 3);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(2, 2);
        Integer reachMinS = 1;
        Integer reachMaxS = 1;
        Integer reachMinD = 2;
        Integer reachMaxD = 2;
        int fSetSize = 13;
        String failureClass = "Node";
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 1.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, reachMinS, reachMaxS, reachMinD, reachMaxD,
                numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Multicast + Destination-Node Disjoint Paths - Kinda a weird one
    @Test
    public void multicastDestinationNodeDisjoint(){
        int numSources = 1;
        int numDestinations = 3;
        int numConnections = 2;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(2, 2);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(3, 3);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        int fSetSize = 3;
        String failureClass = "Node";
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 1.0;
        Integer reachMinS = 1;
        Integer reachMaxS = 1;
        Integer reachMinD = 2;
        Integer reachMaxD = 3;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, reachMinS, reachMaxS, reachMinD, reachMaxD,
                numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Manycast
    @Test
    public void manycast(){
        int numSources = 1;
        int numDestinations = 3;
        int numConnections = 2;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(2, 2);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(3, 3);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        Integer reachMinS = 1;
        Integer reachMaxS = 1;
        Integer reachMinD = 2;
        Integer reachMaxD = 2;
        int fSetSize = 0;
        String failureClass = "Node";
        int numFailsAllowed = 0;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 1.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, reachMinS, reachMaxS, reachMinD, reachMaxD,
                numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Broadcast
    @Test
    public void broadcast(){
        int numSources = 10;
        int numDestinations = 10;
        int numConnections = 90;
        List<Integer> minConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(9, 9);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(90, 90);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(90, 90);
        int fSetSize = 0;
        String failureClass = "Node";
        int numFailsAllowed = 0;
        double percentSrcAlsoDest = 1.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Many-to-Many (Min 1 from each source)
    @Test
    public void manyToManyMin1Src(){
        int numSources = 3;
        int numDestinations = 3;
        int numConnections = 3;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minDstConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxDstConnectionsRange = Arrays.asList(10, 10);
        int fSetSize = 0;
        String failureClass = "Node";
        int numFailsAllowed = 0;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Many-to-Many (Min 1 to each destination)
    @Test
    public void manyToManyMin1Dst(){
        int numSources = 3;
        int numDestinations = 3;
        int numConnections = 3;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(10, 10);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        int fSetSize = 0;
        String failureClass = "Node";
        int numFailsAllowed = 0;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Many-to-Many (Min 1 from each s, 1 to each d)
    @Test
    public void manyToManyMin1SrcMin1Dst(){
        int numSources = 3;
        int numDestinations = 3;
        int numConnections = 3;
        List<Integer> minConnectionsRange = Arrays.asList(0, 0);
        List<Integer> maxConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(1, 1);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        int fSetSize = 0;
        String failureClass = "Node";
        int numFailsAllowed = 0;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Many-to-Many (Min 1 from each s, 1 to each d, 1 backup path for each)
    @Test
    public void manyToManyMin1SrcMin1Dst1BackupEach(){
        int numSources = 3;
        int numDestinations = 3;
        int numConnections = 3;
        List<Integer> minConnectionsRange = Arrays.asList(0);
        List<Integer> maxConnectionsRange = Arrays.asList(1);
        List<Integer> minSrcConnectionsRange = Arrays.asList(1);
        List<Integer> maxSrcConnectionsRange = Arrays.asList(1);
        List<Integer> minDstConnectionsRange = Arrays.asList(1, 1);
        List<Integer> maxDstConnectionsRange = Arrays.asList(1, 1);
        int fSetSize = 21;
        String failureClass = "Link";
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.0;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

    // Src/Dest Failure - Have two paths left over
    @Test
    public void srcDestFailTwoPathsLeftOver(){
        int numSources = 1;
        int numDestinations = 3;
        int numConnections = 2;
        List<Integer> minConnectionsRange = Collections.singletonList(0);
        List<Integer> maxConnectionsRange = Collections.singletonList(1);
        List<Integer> minSrcConnectionsRange = Collections.singletonList(2);
        List<Integer> maxSrcConnectionsRange = Collections.singletonList(3);
        List<Integer> minDstConnectionsRange = Collections.singletonList(0);
        List<Integer> maxDstConnectionsRange = Collections.singletonList(1);
        int fSetSize = 1;
        String failureClass = "Node";
        int numFailsAllowed = 1;
        double percentSrcAlsoDest = 0.0;
        double percentSrcFail = 0.0;
        double percentDstFail = 0.3;
        boolean survivable = true;
        Request rs = createCombinedRequestSet("Combined", numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        verify(rs, numSources, numDestinations, fSetSize, failureClass, numConnections,
                minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange, maxSrcConnectionsRange,
                minDstConnectionsRange, maxDstConnectionsRange, numFailsAllowed, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        rs = processingService.processRequestSet(rs);
        Analysis as = analysisService.analyzeRequest(rs);
        testSolution(rs, as, survivable, numConnections, minConnectionsRange, minSrcConnectionsRange, minDstConnectionsRange);
    }

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

    private void testSolution(Request rs, Analysis as, Boolean survivable, int numConnections,
                              List<Integer> minConnectionsRange, List<Integer> minSrcConnectionsRange,
                              List<Integer> minDstConnectionsRange) {

        Details r = rs.getDetails().values().iterator().next();
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
            if(pair.getSrc() != pair.getDst()) {
                PathSetMetrics psm = pathSetMetricsMap.get(pair);
                assert (psm.getNumPaths() >= minConnectionsRange.get(0));
            }
        }
    }

    private void verify(Request rs, int numSources, int numDestinations, int fSetSize, String failureClass,
                        int numConnections, List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                        List<Integer> minSrcConnectionsRange, List<Integer> maxSrcConnectionsRange,
                        List<Integer> minDstConnectionsRange, List<Integer> maxDstConnectionsRange,
                        int numFailsAllowed, double percentSrcAlsoDest, double percentSrcFail,
                        double percentDstFail){
        assert(rs.getProblemClass().equals(ProblemClass.Combined));
        Details r = rs.getDetails().values().iterator().next();
        assert(r.getSources().size() == numSources);
        assert(r.getDestinations().size() == numDestinations);
        assert(rs.getFailureClass().equals(FailureClass.valueOf(failureClass)));
        Connections connections = r.getConnections();
        assert(connections.getNumConnections() == numConnections);
        assert(connections.getPairMinConnectionsMap().values().stream().allMatch(c -> c.equals(minConnectionsRange.get(0)) || c.equals(0)));
        assert(connections.getPairMaxConnectionsMap().values().stream().allMatch(c -> c.equals(maxConnectionsRange.get(0))  || c.equals(0)));
        assert(connections.getSrcMinConnectionsMap().values().stream().allMatch(c -> c.equals(minSrcConnectionsRange.get(0))  || c.equals(0)));
        assert(connections.getSrcMaxConnectionsMap().values().stream().allMatch(c -> c.equals(maxSrcConnectionsRange.get(0))  || c.equals(0)));
        assert(connections.getDstMinConnectionsMap().values().stream().allMatch(c -> c.equals(minDstConnectionsRange.get(0))  || c.equals(0)));
        assert(connections.getDstMaxConnectionsMap().values().stream().allMatch(c -> c.equals(maxDstConnectionsRange.get(0))  || c.equals(0)));
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

    private Request createCombinedRequestSet(String problemClass, int numSources, int numDestinations, int fSetSize, String failureClass,
                                             int numConnections, List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                             List<Integer> minSrcConnectionsRange, List<Integer> maxSrcConnectionsRange,
                                             List<Integer> minDstConnectionsRange, List<Integer> maxDstConnectionsRange,
                                             Integer reachMinS, Integer reachMaxS, Integer reachMinD, Integer reachMaxD,
                                             int numFailsAllowed, double percentSrcAlsoDest, double percentSrcFail,
                                             double percentDstFail){
        return createRequestSet(3L, "NSFnet", 1, "ServiceILP", problemClass,
                "TotalCost", numSources, numDestinations, fSetSize, new ArrayList<>(), failureClass, 1.0,
                new ArrayList<>(), numConnections, minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange,
                maxSrcConnectionsRange, minDstConnectionsRange, maxDstConnectionsRange,
                reachMinS, reachMaxS, reachMinD, reachMaxD, numFailsAllowed, new ArrayList<>(),
                "Solo", false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail);
    }

    private Request createCombinedRequestSet(String problemClass, int numSources, int numDestinations, int fSetSize, String failureClass,
                                             int numConnections, List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                             List<Integer> minSrcConnectionsRange, List<Integer> maxSrcConnectionsRange,
                                             List<Integer> minDstConnectionsRange, List<Integer> maxDstConnectionsRange,
                                             int numFailsAllowed, double percentSrcAlsoDest, double percentSrcFail,
                                             double percentDstFail){
        return createRequestSet(3L, "NSFnet", 1, "ServiceILP", problemClass,
                "TotalCost", numSources, numDestinations, fSetSize, new ArrayList<>(), failureClass, 1.0,
                new ArrayList<>(), numConnections, minConnectionsRange, maxConnectionsRange, minSrcConnectionsRange,
                maxSrcConnectionsRange, minDstConnectionsRange, maxDstConnectionsRange,
                0, numSources, 0, numDestinations, numFailsAllowed, new ArrayList<>(),
                "Solo", false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail);
    }

    private Request createRequestSet(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                     String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                     List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                     List<Double> minMaxFailureProb, Integer numConnections,
                                     List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                     List<Integer> minSrcConnectionsRange, List<Integer> maxSrcConnectionsRange,
                                     List<Integer> minDstConnectionsRange, List<Integer> maxDstConnectionsRange,
                                     Integer reachMinS, Integer reachMaxS, Integer reachMinD, Integer reachMaxD,
                                     Integer numFailsAllowed, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                     Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                     double percentDstFail){

        SimulationParameters params = makeParameters(seed, topologyId, numRequests, alg, problemClass, objective, numSources, numDestinations,
                fSetSize, minMaxFailures, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange, maxConnectionsRange,
                minSrcConnectionsRange, maxSrcConnectionsRange, minDstConnectionsRange, maxDstConnectionsRange,
                reachMinS, reachMaxS, reachMinD, reachMaxD, numFailsAllowed,
                minMaxFailsAllowed, processingType, sdn, useAws, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        Request request = generationService.generateFromSimParams(params);
        return request;
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                List<Integer> minSrcConnectionsRange, List<Integer> maxSrcConnectionsRange,
                                                List<Integer> minDstConnectionsRange, List<Integer> maxDstConnectionsRange,
                                                Integer reachMinS, Integer reachMaxS, Integer reachMinD, Integer reachMaxD,
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
                .reachMinS(reachMinS)
                .reachMaxS(reachMaxS)
                .reachMinD(reachMinD)
                .reachMaxD(reachMaxD)
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
