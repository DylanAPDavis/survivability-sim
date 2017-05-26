package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.analyzed.RequestMetrics;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import netlab.topology.elements.Node;
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
public class FlowAndEndpointTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ProcessingService processingService;


    @Test
    public void zeroMinOneMaxOneSourceOneDestOneC(){
        RequestSet r1 = createSet("Endpoint", 1, 1, 1, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r1, 1, true);
        RequestSet r2 = createSet("Flow", 1, 1, 1, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r2, 1, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinOneMaxOneSourceOneDestTwoC(){
        RequestSet r1 = createSet("Endpoint", 1, 1, 2, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r1, 0, false);
        RequestSet r2 = createSet("Flow", 1, 1, 2, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r2, 0, false);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void oneMinOneMaxOneSourceOneDestTwoC(){
        RequestSet r1 = createSet("Endpoint", 1, 1, 2, Arrays.asList(1,1),
                Arrays.asList(1,1), 0.0) ;
        analyze(r1, 0, false);
        RequestSet r2 = createSet("Flow", 1, 1, 2, Arrays.asList(1,1),
                Arrays.asList(1,1), 0.0) ;
        analyze(r2, 0, false);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinTwoMaxOneSourceOneDestTwoC(){
        RequestSet r1 = createSet("Endpoint", 1, 1, 2, Arrays.asList(0,0),
                Arrays.asList(2,2), 0.0) ;
        analyze(r1, 2, true);
        RequestSet r2 = createSet("Flow", 1, 1, 2, Arrays.asList(0,0),
                Arrays.asList(2,2), 0.0) ;
        analyze(r2, 2, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinOneMaxOneSourceTwoDestOneC(){
        RequestSet r1 = createSet("Endpoint", 1, 2, 1, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r1, 1, true);
        RequestSet r2 = createSet("Flow", 1, 2, 1, Arrays.asList(0,0),
                Arrays.asList(1,1), 0.0) ;
        analyze(r2, 1, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinHalfOneMaxOneSourceTwoDestOneC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Atlanta", "Princeton"));
        Integer numC = 1;
        // Endpoints
        Map<String, Integer> srcMinNumC = new HashMap<>();
        srcMinNumC.put("Seattle", 0);
        Map<String, Integer> srcMaxNumC = new HashMap<>();
        srcMaxNumC.put("Seattle", 1);
        Map<String, Integer> dstMinNumC= new HashMap<>();
        dstMinNumC.put("Atlanta", 0);
        dstMinNumC.put("Princeton", 0);
        Map<String, Integer> dstMaxNumC = new HashMap<>();
        dstMaxNumC.put("Atlanta", 1);
        dstMaxNumC.put("Princeton", 0);
        RequestSet r1 = createSetWithEndpoints(sources, destinations, srcMinNumC, srcMaxNumC, dstMinNumC, dstMaxNumC, numC);
        analyze(r1, 1, true);
        // Flow
        Map<List<String>, Integer> pairMinNumC = new HashMap<>();
        pairMinNumC.put(Arrays.asList("Seattle", "Atlanta"), 0);
        pairMinNumC.put(Arrays.asList("Seattle", "Princeton"), 0);
        Map<List<String>, Integer> pairMaxNumC = new HashMap<>();
        pairMaxNumC.put(Arrays.asList("Seattle", "Atlanta"), 1);
        pairMaxNumC.put(Arrays.asList("Seattle", "Princeton"), 0);
        RequestSet r2 = createSetWithPairs(sources, destinations, pairMinNumC, pairMaxNumC, numC);
        analyze(r2, 1, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinHalfOneMaxOneSourceTwoDestTwoC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Atlanta", "Princeton"));
        Integer numC = 2;
        // Endpoints
        Map<String, Integer> srcMinNumC = new HashMap<>();
        srcMinNumC.put("Seattle", 0);
        Map<String, Integer> srcMaxNumC = new HashMap<>();
        srcMaxNumC.put("Seattle", 1);
        Map<String, Integer> dstMinNumC= new HashMap<>();
        dstMinNumC.put("Atlanta", 0);
        dstMinNumC.put("Princeton", 0);
        Map<String, Integer> dstMaxNumC = new HashMap<>();
        dstMaxNumC.put("Atlanta", 1);
        dstMaxNumC.put("Princeton", 0);
        RequestSet r1 = createSetWithEndpoints(sources, destinations, srcMinNumC, srcMaxNumC, dstMinNumC, dstMaxNumC, numC);
        analyze(r1, 0, false);
        // Flow
        Map<List<String>, Integer> pairMinNumC = new HashMap<>();
        pairMinNumC.put(Arrays.asList("Seattle", "Atlanta"), 0);
        pairMinNumC.put(Arrays.asList("Seattle", "Princeton"), 0);
        Map<List<String>, Integer> pairMaxNumC = new HashMap<>();
        pairMaxNumC.put(Arrays.asList("Seattle", "Atlanta"), 1);
        pairMaxNumC.put(Arrays.asList("Seattle", "Princeton"), 0);
        RequestSet r2 = createSetWithPairs(sources, destinations, pairMinNumC, pairMaxNumC, numC);
        analyze(r2, 0, false);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinTwoMaxOneSourceTwoDestTwoC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Atlanta", "Princeton"));
        Integer numC = 2;
        // Endpoints
        Map<String, Integer> srcMinNumC = new HashMap<>();
        srcMinNumC.put("Seattle", 0);
        Map<String, Integer> srcMaxNumC = new HashMap<>();
        srcMaxNumC.put("Seattle", 2);
        Map<String, Integer> dstMinNumC= new HashMap<>();
        dstMinNumC.put("Atlanta", 0);
        dstMinNumC.put("Princeton", 0);
        Map<String, Integer> dstMaxNumC = new HashMap<>();
        dstMaxNumC.put("Atlanta", 1);
        dstMaxNumC.put("Princeton", 1);
        RequestSet r1 = createSetWithEndpoints(sources, destinations, srcMinNumC, srcMaxNumC, dstMinNumC, dstMaxNumC, numC);
        analyze(r1, 2, true);
        // Flow
        Map<List<String>, Integer> pairMinNumC = new HashMap<>();
        pairMinNumC.put(Arrays.asList("Seattle", "Atlanta"), 0);
        pairMinNumC.put(Arrays.asList("Seattle", "Princeton"), 0);
        Map<List<String>, Integer> pairMaxNumC = new HashMap<>();
        pairMaxNumC.put(Arrays.asList("Seattle", "Atlanta"), 1);
        pairMaxNumC.put(Arrays.asList("Seattle", "Princeton"), 1);
        RequestSet r2 = createSetWithPairs(sources, destinations, pairMinNumC, pairMaxNumC, numC);
        analyze(r2, 2, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinOneMaxThreeSourceThreeDestOneC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Atlanta", "Princeton", "Champaign"));
        Integer numC = 1;
        // Endpoints
        Map<String, Integer> srcMinNumC = new HashMap<>();
        srcMinNumC.put("Seattle", 0);
        srcMinNumC.put("Boulder", 0);
        srcMinNumC.put("Houston", 0);
        Map<String, Integer> srcMaxNumC = new HashMap<>();
        srcMaxNumC.put("Seattle", 1);
        srcMaxNumC.put("Boulder", 1);
        srcMaxNumC.put("Houston", 1);
        Map<String, Integer> dstMinNumC= new HashMap<>();
        dstMinNumC.put("Atlanta", 0);
        dstMinNumC.put("Princeton", 0);
        dstMinNumC.put("Champaign", 0);
        Map<String, Integer> dstMaxNumC = new HashMap<>();
        dstMaxNumC.put("Atlanta", 0);
        dstMaxNumC.put("Princeton", 1);
        dstMaxNumC.put("Champaign", 0);
        RequestSet r1 = createSetWithEndpoints(sources, destinations, srcMinNumC, srcMaxNumC, dstMinNumC, dstMaxNumC, numC);
        analyze(r1, 1, true);
        // Flow
        Map<List<String>, Integer> pairMinNumC = new HashMap<>();
        pairMinNumC.put(Arrays.asList("Seattle", "Atlanta"), 0);
        pairMinNumC.put(Arrays.asList("Seattle", "Princeton"), 0);
        pairMinNumC.put(Arrays.asList("Seattle", "Champaign"), 0);
        pairMinNumC.put(Arrays.asList("Boulder", "Atlanta"), 0);
        pairMinNumC.put(Arrays.asList("Boulder", "Princeton"), 0);
        pairMinNumC.put(Arrays.asList("Boulder", "Champaign"), 0);
        pairMinNumC.put(Arrays.asList("Houston", "Atlanta"), 0);
        pairMinNumC.put(Arrays.asList("Houston", "Princeton"), 0);
        pairMinNumC.put(Arrays.asList("Houston", "Champaign"), 0);
        Map<List<String>, Integer> pairMaxNumC = new HashMap<>();
        pairMaxNumC.put(Arrays.asList("Seattle", "Atlanta"), 1);
        pairMaxNumC.put(Arrays.asList("Seattle", "Princeton"), 1);
        pairMaxNumC.put(Arrays.asList("Seattle", "Champaign"), 0);
        pairMaxNumC.put(Arrays.asList("Boulder", "Atlanta"), 1);
        pairMaxNumC.put(Arrays.asList("Boulder", "Princeton"), 1);
        pairMaxNumC.put(Arrays.asList("Boulder", "Champaign"), 0);
        pairMaxNumC.put(Arrays.asList("Houston", "Atlanta"), 0);
        pairMaxNumC.put(Arrays.asList("Houston", "Princeton"), 1);
        pairMaxNumC.put(Arrays.asList("Houston", "Champaign"), 0);
        RequestSet r2 = createSetWithPairs(sources, destinations, pairMinNumC, pairMaxNumC, numC);
        analyze(r2, 1, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    Node seattle = new Node("Seattle");
    Node paloAlto = new Node("Palo Alto");
    Node sanDiego = new Node("San Diego");
    Node saltLakeCity = new Node("Salt Lake City");
    Node boulder = new Node("Boulder");
    Node houston = new Node("Houston");
    Node lincoln = new Node("Lincoln");
    Node champaign = new Node("Champaign");
    Node annArbor = new Node("Ann Arbor");
    Node pittsburgh = new Node("Pittsburgh");
    Node atlanta = new Node("Atlanta");
    Node collegePark = new Node("College Park");
    Node ithaca = new Node("Ithaca");
    Node princeton = new Node("Princeton");

    @Test
    public void zeroMinOneMaxFourteenSourceOneDestFourteenCInfeasible(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Champaign"));
        Integer numC = 14;
        // Endpoints
        Map<String, Integer> srcMinNumC = new HashMap<>();
        srcMinNumC.put("Champaign", 1);
        Map<String, Integer> srcMaxNumC = new HashMap<>();
        srcMaxNumC.put("Seattle", 1);
        srcMaxNumC.put("Boulder", 1);
        srcMaxNumC.put("Houston", 1);
        srcMaxNumC.put("Palo Alto", 1);
        srcMaxNumC.put("San Diego", 1);
        srcMaxNumC.put("Salt Lake City", 1);
        srcMaxNumC.put("Lincoln", 1);
        srcMaxNumC.put("Champaign", 1);
        srcMaxNumC.put("Ann Arbor", 1);
        srcMaxNumC.put("Pittsburgh", 1);
        srcMaxNumC.put("Atlanta", 1);
        srcMaxNumC.put("College Park", 1);
        srcMaxNumC.put("Ithaca", 1);
        srcMaxNumC.put("Princeton", 1);
        Map<String, Integer> dstMinNumC= new HashMap<>();
        Map<String, Integer> dstMaxNumC = new HashMap<>();
        RequestSet r1 = createSetWithEndpoints(sources, destinations, srcMinNumC, srcMaxNumC, dstMinNumC, dstMaxNumC, numC);
        analyze(r1, 0, false);
        // Flow
        Map<List<String>, Integer> pairMinNumC = new HashMap<>();
        pairMinNumC.put(Arrays.asList("Champaign", "Champaign"), 1);
        Map<List<String>, Integer> pairMaxNumC = new HashMap<>();
        pairMaxNumC.put(Arrays.asList("Seattle", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Boulder", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Houston", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Palo Alto", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("San Diego", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Salt Lake City", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Lincoln", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Champaign"), 0);
        pairMaxNumC.put(Arrays.asList("Ann Arbor", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Pittsburgh", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Atlanta", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("College Park", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Ithaca", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Princeton", "Champaign"), 1);
        RequestSet r2 = createSetWithPairs(sources, destinations, pairMinNumC, pairMaxNumC, numC);
        analyze(r2, 0, false);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinOneMaxFourteenSourceOneDestFourteenC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Champaign"));
        Integer numC = 14;
        // Endpoints
        Map<String, Integer> srcMinNumC = new HashMap<>();
        Map<String, Integer> srcMaxNumC = new HashMap<>();
        srcMaxNumC.put("Seattle", 1);
        srcMaxNumC.put("Boulder", 1);
        srcMaxNumC.put("Houston", 1);
        srcMaxNumC.put("Palo Alto", 1);
        srcMaxNumC.put("San Diego", 2);
        srcMaxNumC.put("Salt Lake City", 1);
        srcMaxNumC.put("Lincoln", 1);
        srcMaxNumC.put("Champaign", 1);
        srcMaxNumC.put("Ann Arbor", 1);
        srcMaxNumC.put("Pittsburgh", 1);
        srcMaxNumC.put("Atlanta", 1);
        srcMaxNumC.put("College Park", 1);
        srcMaxNumC.put("Ithaca", 1);
        srcMaxNumC.put("Princeton", 1);
        Map<String, Integer> dstMinNumC= new HashMap<>();
        Map<String, Integer> dstMaxNumC = new HashMap<>();
        RequestSet r1 = createSetWithEndpoints(sources, destinations, srcMinNumC, srcMaxNumC, dstMinNumC, dstMaxNumC, numC);
        analyze(r1, 14, true);
        // Flow
        Map<List<String>, Integer> pairMinNumC = new HashMap<>();
        Map<List<String>, Integer> pairMaxNumC = new HashMap<>();
        pairMaxNumC.put(Arrays.asList("Seattle", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Boulder", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Houston", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Palo Alto", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("San Diego", "Champaign"), 2);
        pairMaxNumC.put(Arrays.asList("Salt Lake City", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Lincoln", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Champaign"), 0);
        pairMaxNumC.put(Arrays.asList("Ann Arbor", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Pittsburgh", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Atlanta", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("College Park", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Ithaca", "Champaign"), 1);
        pairMaxNumC.put(Arrays.asList("Princeton", "Champaign"), 1);
        RequestSet r2 = createSetWithPairs(sources, destinations, pairMinNumC, pairMaxNumC, numC);
        analyze(r2, 14, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinOneMaxOneSourceFourteenDestFourteenCInfeasible(){
        Set<String> sources = new HashSet<>(Arrays.asList("Champaign"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Integer numC = 14;
        // Endpoints
        Map<String, Integer> srcMinNumC = new HashMap<>();
        Map<String, Integer> srcMaxNumC = new HashMap<>();
        Map<String, Integer> dstMinNumC= new HashMap<>();
        Map<String, Integer> dstMaxNumC = new HashMap<>();
        dstMaxNumC.put("Seattle", 1);
        dstMaxNumC.put("Boulder", 1);
        dstMaxNumC.put("Houston", 1);
        dstMaxNumC.put("Palo Alto", 1);
        dstMaxNumC.put("San Diego", 1);
        dstMaxNumC.put("Salt Lake City", 1);
        dstMaxNumC.put("Lincoln", 1);
        dstMaxNumC.put("Champaign", 1);
        dstMaxNumC.put("Ann Arbor", 1);
        dstMaxNumC.put("Pittsburgh", 1);
        dstMaxNumC.put("Atlanta", 1);
        dstMaxNumC.put("College Park", 1);
        dstMaxNumC.put("Ithaca", 1);
        dstMaxNumC.put("Princeton", 1);
        RequestSet r1 = createSetWithEndpoints(sources, destinations, srcMinNumC, srcMaxNumC, dstMinNumC, dstMaxNumC, numC);
        analyze(r1, 0, false);
        // Flow
        Map<List<String>, Integer> pairMinNumC = new HashMap<>();
        Map<List<String>, Integer> pairMaxNumC = new HashMap<>();
        pairMaxNumC.put(Arrays.asList("Champaign", "Seattle"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Boulder"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Houston"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Palo Alto"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "San Diego"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Salt Lake City"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Lincoln"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Champaign"), 0);
        pairMaxNumC.put(Arrays.asList("Champaign", "Ann Arbor"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Pittsburgh"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Atlanta"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "College Park"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Ithaca"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Princeton"), 1);
        RequestSet r2 = createSetWithPairs(sources, destinations, pairMinNumC, pairMaxNumC, numC);
        analyze(r2, 0, false);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinOneMaxOneSourceFourteenDestFourteenC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Champaign"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Integer numC = 14;
        // Endpoints
        Map<String, Integer> srcMinNumC = new HashMap<>();
        Map<String, Integer> srcMaxNumC = new HashMap<>();
        Map<String, Integer> dstMinNumC= new HashMap<>();
        Map<String, Integer> dstMaxNumC = new HashMap<>();
        dstMaxNumC.put("Seattle", 1);
        dstMaxNumC.put("Boulder", 1);
        dstMaxNumC.put("Houston", 1);
        dstMaxNumC.put("Palo Alto", 1);
        dstMaxNumC.put("San Diego", 1);
        dstMaxNumC.put("Salt Lake City", 2);
        dstMaxNumC.put("Lincoln", 1);
        dstMaxNumC.put("Champaign", 1);
        dstMaxNumC.put("Ann Arbor", 1);
        dstMaxNumC.put("Pittsburgh", 1);
        dstMaxNumC.put("Atlanta", 1);
        dstMaxNumC.put("College Park", 1);
        dstMaxNumC.put("Ithaca", 1);
        dstMaxNumC.put("Princeton", 1);
        RequestSet r1 = createSetWithEndpoints(sources, destinations, srcMinNumC, srcMaxNumC, dstMinNumC, dstMaxNumC, numC);
        analyze(r1, 14, true);
        // Flow
        Map<List<String>, Integer> pairMinNumC = new HashMap<>();
        Map<List<String>, Integer> pairMaxNumC = new HashMap<>();
        pairMaxNumC.put(Arrays.asList("Champaign", "Seattle"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Boulder"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Houston"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Palo Alto"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "San Diego"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Salt Lake City"), 2);
        pairMaxNumC.put(Arrays.asList("Champaign", "Lincoln"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Champaign"), 0);
        pairMaxNumC.put(Arrays.asList("Champaign", "Ann Arbor"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Pittsburgh"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Atlanta"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "College Park"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Ithaca"), 1);
        pairMaxNumC.put(Arrays.asList("Champaign", "Princeton"), 1);
        RequestSet r2 = createSetWithPairs(sources, destinations, pairMinNumC, pairMaxNumC, numC);
        analyze(r2, 14, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinOneMaxFourteenSourceFourteenDestFourteenC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Integer numC = 14;
        // Endpoints
        Map<String, Integer> srcMinNumC = new HashMap<>();
        Map<String, Integer> srcMaxNumC = new HashMap<>();
        srcMaxNumC.put("Seattle", 1);
        srcMaxNumC.put("Boulder", 1);
        srcMaxNumC.put("Houston", 1);
        srcMaxNumC.put("Palo Alto", 1);
        srcMaxNumC.put("San Diego", 1);
        srcMaxNumC.put("Salt Lake City", 1);
        srcMaxNumC.put("Lincoln", 1);
        srcMaxNumC.put("Champaign", 1);
        srcMaxNumC.put("Ann Arbor", 1);
        srcMaxNumC.put("Pittsburgh", 1);
        srcMaxNumC.put("Atlanta", 1);
        srcMaxNumC.put("College Park", 1);
        srcMaxNumC.put("Ithaca", 1);
        srcMaxNumC.put("Princeton", 1);
        Map<String, Integer> dstMinNumC= new HashMap<>();
        Map<String, Integer> dstMaxNumC = new HashMap<>();
        dstMaxNumC.put("Seattle", 1);
        dstMaxNumC.put("Boulder", 1);
        dstMaxNumC.put("Houston", 1);
        dstMaxNumC.put("Palo Alto", 1);
        dstMaxNumC.put("San Diego", 1);
        dstMaxNumC.put("Salt Lake City", 1);
        dstMaxNumC.put("Lincoln", 1);
        dstMaxNumC.put("Champaign", 1);
        dstMaxNumC.put("Ann Arbor", 1);
        dstMaxNumC.put("Pittsburgh", 1);
        dstMaxNumC.put("Atlanta", 1);
        dstMaxNumC.put("College Park", 1);
        dstMaxNumC.put("Ithaca", 1);
        dstMaxNumC.put("Princeton", 1);
        RequestSet r1 = createSetWithEndpoints(sources, destinations, srcMinNumC, srcMaxNumC, dstMinNumC, dstMaxNumC, numC);
        analyze(r1, 14, true);
        // Flow
        Map<List<String>, Integer> pairMinNumC = new HashMap<>();
        Map<List<String>, Integer> pairMaxNumC = new HashMap<>();
        for(String source : sources){
            for(String dest : destinations){
                pairMaxNumC.put(Arrays.asList(source, dest), 1);
            }
        }
        RequestSet r2 = createSetWithPairs(sources, destinations, pairMinNumC, pairMaxNumC, numC);
        analyze(r2, 14, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinThirteenMaxFourteenSourceFourteenDestBroadcastC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Integer numC = 182;
        // Endpoints
        Map<String, Integer> srcMinNumC = new HashMap<>();
        srcMinNumC.put("Seattle", 13);
        srcMinNumC.put("Boulder", 13);
        srcMinNumC.put("Houston", 13);
        srcMinNumC.put("Palo Alto", 13);
        srcMinNumC.put("San Diego", 13);
        srcMinNumC.put("Salt Lake City", 13);
        srcMinNumC.put("Lincoln", 13);
        srcMinNumC.put("Champaign", 13);
        srcMinNumC.put("Ann Arbor", 13);
        srcMinNumC.put("Pittsburgh", 13);
        srcMinNumC.put("Atlanta", 13);
        srcMinNumC.put("College Park", 13);
        srcMinNumC.put("Ithaca", 13);
        srcMinNumC.put("Princeton", 13);
        Map<String, Integer> srcMaxNumC = new HashMap<>();
        srcMaxNumC.put("Seattle", 13);
        srcMaxNumC.put("Boulder", 13);
        srcMaxNumC.put("Houston", 13);
        srcMaxNumC.put("Palo Alto", 13);
        srcMaxNumC.put("San Diego", 13);
        srcMaxNumC.put("Salt Lake City", 13);
        srcMaxNumC.put("Lincoln", 13);
        srcMaxNumC.put("Champaign", 13);
        srcMaxNumC.put("Ann Arbor", 13);
        srcMaxNumC.put("Pittsburgh", 13);
        srcMaxNumC.put("Atlanta", 13);
        srcMaxNumC.put("College Park", 13);
        srcMaxNumC.put("Ithaca", 13);
        srcMaxNumC.put("Princeton", 13);
        Map<String, Integer> dstMinNumC= new HashMap<>();
        dstMinNumC.put("Seattle", 13);
        dstMinNumC.put("Boulder", 13);
        dstMinNumC.put("Houston", 13);
        dstMinNumC.put("Palo Alto", 13);
        dstMinNumC.put("San Diego", 13);
        dstMinNumC.put("Salt Lake City", 13);
        dstMinNumC.put("Lincoln", 13);
        dstMinNumC.put("Champaign", 13);
        dstMinNumC.put("Ann Arbor", 13);
        dstMinNumC.put("Pittsburgh", 13);
        dstMinNumC.put("Atlanta", 13);
        dstMinNumC.put("College Park", 13);
        dstMinNumC.put("Ithaca", 13);
        dstMinNumC.put("Princeton", 13);
        Map<String, Integer> dstMaxNumC = new HashMap<>();
        dstMaxNumC.put("Seattle", 13);
        dstMaxNumC.put("Boulder", 13);
        dstMaxNumC.put("Houston", 13);
        dstMaxNumC.put("Palo Alto", 13);
        dstMaxNumC.put("San Diego", 13);
        dstMaxNumC.put("Salt Lake City", 13);
        dstMaxNumC.put("Lincoln", 13);
        dstMaxNumC.put("Champaign", 13);
        dstMaxNumC.put("Ann Arbor", 13);
        dstMaxNumC.put("Pittsburgh", 13);
        dstMaxNumC.put("Atlanta", 13);
        dstMaxNumC.put("College Park", 13);
        dstMaxNumC.put("Ithaca", 13);
        dstMaxNumC.put("Princeton", 13);
        RequestSet r1 = createSetWithEndpoints(sources, destinations, srcMinNumC, srcMaxNumC, dstMinNumC, dstMaxNumC, numC);
        analyze(r1, 182, true);
        // Flow
        Map<List<String>, Integer> pairMinNumC = new HashMap<>();
        for(String source : sources){
            for(String dest : destinations){
                pairMinNumC.put(Arrays.asList(source, dest), 0);
            }
        }
        Map<List<String>, Integer> pairMaxNumC = new HashMap<>();
        for(String source : sources){
            for(String dest : destinations){
                    pairMaxNumC.put(Arrays.asList(source, dest), 13);
            }
        }
        RequestSet r2 = createSetWithPairs(sources, destinations, pairMinNumC, pairMaxNumC, numC);
        analyze(r2, 182, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }

    @Test
    public void zeroMinTwoMaxFourteenSourceFourteenDestTwentyEightC(){
        Set<String> sources = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Set<String> destinations = new HashSet<>(Arrays.asList("Seattle", "Boulder", "Houston", "Palo Alto", "San Diego",
                "Salt Lake City", "Lincoln", "Champaign", "Ann Arbor", "Pittsburgh", "Atlanta", "College Park", "Ithaca", "Princeton"));
        Integer numC = 28;
        // Endpoints
        Map<String, Integer> srcMinNumC = new HashMap<>();
        srcMinNumC.put("Seattle", 2);
        srcMinNumC.put("Boulder", 2);
        srcMinNumC.put("Houston", 2);
        srcMinNumC.put("Palo Alto", 2);
        srcMinNumC.put("San Diego", 2);
        srcMinNumC.put("Salt Lake City", 2);
        srcMinNumC.put("Lincoln", 2);
        srcMinNumC.put("Champaign", 2);
        srcMinNumC.put("Ann Arbor", 2);
        srcMinNumC.put("Pittsburgh", 2);
        srcMinNumC.put("Atlanta", 2);
        srcMinNumC.put("College Park", 2);
        srcMinNumC.put("Ithaca", 2);
        srcMinNumC.put("Princeton", 2);
        Map<String, Integer> srcMaxNumC = new HashMap<>();
        srcMaxNumC.put("Seattle", 2);
        srcMaxNumC.put("Boulder", 2);
        srcMaxNumC.put("Houston", 2);
        srcMaxNumC.put("Palo Alto", 2);
        srcMaxNumC.put("San Diego", 2);
        srcMaxNumC.put("Salt Lake City", 2);
        srcMaxNumC.put("Lincoln", 2);
        srcMaxNumC.put("Champaign", 2);
        srcMaxNumC.put("Ann Arbor", 2);
        srcMaxNumC.put("Pittsburgh", 2);
        srcMaxNumC.put("Atlanta", 2);
        srcMaxNumC.put("College Park", 2);
        srcMaxNumC.put("Ithaca", 2);
        srcMaxNumC.put("Princeton", 2);
        Map<String, Integer> dstMinNumC= new HashMap<>();
        dstMinNumC.put("Seattle", 2);
        dstMinNumC.put("Boulder", 2);
        dstMinNumC.put("Houston", 2);
        dstMinNumC.put("Palo Alto", 2);
        dstMinNumC.put("San Diego", 2);
        dstMinNumC.put("Salt Lake City", 2);
        dstMinNumC.put("Lincoln", 2);
        dstMinNumC.put("Champaign", 2);
        dstMinNumC.put("Ann Arbor", 2);
        dstMinNumC.put("Pittsburgh", 2);
        dstMinNumC.put("Atlanta", 2);
        dstMinNumC.put("College Park", 2);
        dstMinNumC.put("Ithaca", 2);
        dstMinNumC.put("Princeton", 2);
        Map<String, Integer> dstMaxNumC = new HashMap<>();
        dstMaxNumC.put("Seattle", 2);
        dstMaxNumC.put("Boulder", 2);
        dstMaxNumC.put("Houston", 2);
        dstMaxNumC.put("Palo Alto", 2);
        dstMaxNumC.put("San Diego", 2);
        dstMaxNumC.put("Salt Lake City", 2);
        dstMaxNumC.put("Lincoln", 2);
        dstMaxNumC.put("Champaign", 2);
        dstMaxNumC.put("Ann Arbor", 2);
        dstMaxNumC.put("Pittsburgh", 2);
        dstMaxNumC.put("Atlanta", 2);
        dstMaxNumC.put("College Park", 2);
        dstMaxNumC.put("Ithaca", 2);
        dstMaxNumC.put("Princeton", 2);
        RequestSet r1 = createSetWithEndpoints(sources, destinations, srcMinNumC, srcMaxNumC, dstMinNumC, dstMaxNumC, numC);
        analyze(r1, 28, true);
        // Flow
        Map<List<String>, Integer> pairMinNumC = new HashMap<>();
        for(String source : sources){
            for(String dest : destinations){
                pairMinNumC.put(Arrays.asList(source, dest), 0);
            }
        }
        Map<List<String>, Integer> pairMaxNumC = new HashMap<>();
        for(String source : sources){
            for(String dest : destinations){
                pairMaxNumC.put(Arrays.asList(source, dest), 2);
            }
        }
        RequestSet r2 = createSetWithPairs(sources, destinations, pairMinNumC, pairMaxNumC, numC);
        analyze(r2, 28, true);
        analyzeMultiSet(Arrays.asList(r1, r2));
    }



    private RequestSet createSetWithPairs(Set<String> sources, Set<String> destinations, Map<List<String>, Integer> pairMinNumConnections,
                                          Map<List<String>, Integer> pairMaxNumConnections, Integer numConnections){

        RequestParameters params = RequestParameters.builder()
                .topologyId("NSFnet")
                .problemClass("Flow")
                .algorithm("ServiceILP")
                .objective("LinksUsed")
                .sources(sources)
                .destinations(destinations)
                .pairMinNumConnectionsMap(pairMinNumConnections)
                .pairMaxNumConnectionsMap(pairMaxNumConnections)
                .numConnections(numConnections)
                .build();
        RequestSet rs = generationService.generateFromRequestParams(params);
        processingService.processRequestSet(rs);
        return rs;
    }

    private RequestSet createSetWithEndpoints(Set<String> sources, Set<String> destinations, Map<String, Integer> srcMinNumConnections,
                                          Map<String, Integer> srcMaxNumConnections, Map<String, Integer> dstMinNumConnections,
                                              Map<String, Integer> dstMaxNumConnections, Integer numConnections){

        RequestParameters params = RequestParameters.builder()
                .topologyId("NSFnet")
                .problemClass("Endpoint")
                .algorithm("ServiceILP")
                .objective("LinksUsed")
                .sources(sources)
                .destinations(destinations)
                .sourceMinNumConnectionsMap(srcMinNumConnections)
                .sourceMaxNumConnectionsMap(srcMaxNumConnections)
                .destMinNumConnectionsMap(dstMinNumConnections)
                .destMaxNumConnectionsMap(dstMaxNumConnections)
                .numConnections(numConnections)
                .build();
        RequestSet rs = generationService.generateFromRequestParams(params);
        processingService.processRequestSet(rs);
        return rs;
    }

    private RequestSet createSet(String problemClass, int numSources, int numDestinations, int numConns, List<Integer> minConns,
                                List<Integer> maxConns, double percentSrcAlsoDest){
        return solve(1L, "NSFnet", 1, "ServiceILP", problemClass, "LinksUsed", numSources, numDestinations, 0,
                new ArrayList<>(), "Both", 0.0, new ArrayList<>(), numConns, minConns, maxConns,
                0, new ArrayList<>(), "Solo", false, false, percentSrcAlsoDest, 0.0, 0.0);

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
        for(RequestMetrics rm : analyzedSet.getRequestMetrics().values()){
            assert(rm.getRequestIsSurvivable() == survivable);
            assert(rm.getNumPaths() == numExpectedPaths);
        }
        assert(requestSet.getRequests().values().stream()
                .allMatch(r ->
                        r.getChosenPaths().keySet().stream()
                                .filter(pair -> pair.getSrc().equals(pair.getDst()))
                                .allMatch(p -> r.getChosenPaths().get(p).values().size() == 0)));

    }


    private RequestSet solve(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                             String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                             List<Integer> minMaxFailures, String failureClass, Double failureProb,
                             List<Double> minMaxFailureProb, Integer numConnections,
                             List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                             Integer numFails, List<Integer> minMaxFails, String processingType, Boolean sdn,
                             Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                             double percentDstFail){

        SimulationParameters params = makeParameters(seed, topologyId, numRequests, alg, problemClass, objective, numSources, numDestinations,
                fSetSize, minMaxFailures, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange, maxConnectionsRange,
                numFails, minMaxFails, processingType, sdn, useAws, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        RequestSet requestSet = generationService.generateFromSimParams(params);
        processingService.processRequestSet(requestSet);
        return requestSet;
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
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
