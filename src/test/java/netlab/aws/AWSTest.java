package netlab.aws;


import com.amazonaws.services.dynamodbv2.model.ScanResult;
import netlab.TestConfiguration;
import netlab.analysis.analyzed.*;
import netlab.analysis.controller.AnalysisController;
import netlab.analysis.services.AnalysisService;
import netlab.processing.ProcessingService;
import netlab.processing.pathmapping.PathMappingService;
import netlab.processing.shortestPaths.MinimumCostPathService;
import netlab.storage.aws.dynamo.DynamoInterface;
import netlab.storage.aws.s3.S3Interface;
import netlab.storage.controller.StorageController;
import netlab.storage.services.StorageService;
import netlab.submission.controller.SubmissionController;
import netlab.submission.enums.*;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyAdjustmentService;
import netlab.topology.services.TopologyService;
import netlab.visualization.PrintingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class AWSTest {

    @Autowired
    private DynamoInterface dynamoInterface;

    @Autowired
    private S3Interface s3Interface;

    @Autowired
    private StorageService storageService;

    @Autowired
    private StorageController storageController;

    @Autowired
    private SubmissionController submissionController;

    @Autowired
    private AnalysisController analysisController;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private TopologyService topologyService;

    @Autowired
    private PrintingService printingService;

    @Autowired
    private ProcessingService processingService;

    @Autowired
    private MinimumCostPathService minimumCostPathService;

    @Autowired
    private TopologyAdjustmentService topologyAdjustmentService;

    //@Test
    public void updateRows(){
        SimulationParameters seedParams = SimulationParameters.builder().seed(1L).build();
        List<SimulationParameters> params = storageService.getMatchingSimulationParameters(seedParams);
        for(SimulationParameters param : params){
            storageService.putSimulationParameters(param);
        }
    }


    //@Test
    public void rerunRequests(){
        List<Long> seeds = LongStream.rangeClosed(1, 30).boxed().collect(Collectors.toList());
        submissionController.rerunRequests(seeds);
    }

    @Test
    public void deleteRequests(){
        List<Long> seeds = LongStream.rangeClosed(1, 30).boxed().collect(Collectors.toList());
        String algorithm = "tabu";
        String routing = "manytomany";
        boolean deleteRecords = true;
        boolean deleteAnalysis = true;
        for(Long seed : seeds) {
            long startTime = System.nanoTime();
            boolean success = storageController.deleteRecordsAndRequests(seed, algorithm, routing, deleteRecords, deleteAnalysis);
            assert (success);
            long endTime = System.nanoTime();
            double duration = (endTime - startTime)/1e9;
            System.out.println("Deleted seed " + seed + ". Took: " + duration + " seconds");
        }
    }

    @Test
    public void analysisGeneration(){
        List<Long> seeds = Collections.singletonList(25L);//LongStream.rangeClosed(1L, 30L).boxed().collect(Collectors.toList());
        String routingType = "manytomany";
        String topologyId = "tw";
        analysisController.analyzeSeeds(seeds, routingType, topologyId);
    }

    @Test
    public void aggregateAnalysis(){
        List<Long> seeds = LongStream.rangeClosed(1L, 30L).boxed().collect(Collectors.toList());//Arrays.asList(1L, 2L);
        analysisController.aggregateSeeds(seeds);
    }

    //@Test
    public void scanDynamoMetaDb() {
        if(dynamoInterface.allFieldsDefined()){
            ScanResult result = dynamoInterface.scanMetaTable();
            assert(result != null);
            System.out.println(result.toString());
        }
    }


    //@Test
    public void uploadToRawS3() {
        if(s3Interface.allFieldsDefined()){
            File f = new File("test.txt");
            try {
                if(!f.exists()){
                    assert(f.createNewFile());
                }
                Boolean success = s3Interface.uploadToRaw(f, "test.txt");
                assert(success);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //@Test
    public void downloadFromRaw() {
        if(s3Interface.allFieldsDefined()){
            File f = new File("test2.txt");
            f = s3Interface.downloadFromRaw(f, "test.txt");
            assert(f != null);
        }
    }

    //@Test
    public void uploadToAnalyzedS3() {
        if(s3Interface.allFieldsDefined()){
            File f = new File("test.txt");
            try {
                if(!f.exists()){
                    assert(f.createNewFile());
                }
                Boolean success = s3Interface.uploadToAnalyzed(f, "test.txt");
                assert(success);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testConnections(){
        List<Long> seeds = LongStream.rangeClosed(1L, 30L).boxed().collect(Collectors.toList());
        for(Long seed : seeds){
            String id = String.valueOf(seed) +"_tw_manytomany_ilp_5_3_5_5_1_3_none_quake2_both_2_none_allow_allow_false_8";
            Request r = storageController.getRequest(id, true);
            Topology topo = topologyService.getTopologyById(r.getTopologyId());
            Set<Node> failureNodes = r.getDetails().getFailures().getFailureSet().stream().filter(f -> f.getNode() != null).map(Failure::getNode).collect(Collectors.toSet());
            Set<Link> failureLinks = r.getDetails().getFailures().getFailureSet().stream().filter(f -> f.getLink() != null).map(Failure::getLink).collect(Collectors.toSet());
            Set<Node> sources = r.getDetails().getSources();
            Set<Node> dests = r.getDetails().getDestinations();
            Set<Node> safeDests = dests.stream().filter(d -> !failureNodes.contains(d)).collect(Collectors.toSet());

            Topology adjusted = topologyAdjustmentService.removeNodesFromTopology(topo, failureNodes);
            adjusted = topologyAdjustmentService.removeLinksFromTopology(adjusted, failureLinks);
            //boolean sourceCanFail = r.getDetails().getSources().stream().anyMatch(failureNodes::contains);
            //boolean allDestsCanFail = r.getDetails().getDestinations().stream().allMatch(failureNodes::contains);
            Map<SourceDestPair, Path> paths = new HashMap<>();
            for(Node src : sources){
                for(Node dest : safeDests) {
                    SourceDestPair pair = new SourceDestPair(src, dest);
                    Path path = adjusted.getNodes().contains(src) && adjusted.getNodes().contains(dest) ?
                            minimumCostPathService.findShortestPath(src, dest, adjusted) : new Path(new ArrayList<>());
                    paths.put(pair, path);
                }
            }
            Set<Node> satsifiedSources = new HashSet<>();
            for(SourceDestPair pair : paths.keySet()){
                System.out.println(pair + ": " + paths.get(pair));
                if(paths.get(pair).getNodes().size() > 0){
                    satsifiedSources.add(pair.getSrc());
                }
            }
            System.out.println(seed);
            System.out.println("Satisfied Sources: " + satsifiedSources);
            System.out.println("---------");
        }
    }

    @Test
    public void downloadFromAnalyzed() {
        if(s3Interface.allFieldsDefined()){
            //String id = "3_tw_manytomany_ilp_5_3_5_5_1_3_none_quake2_both_2_none_allow_allow_false_8";
            String id = "30_tw_manytomany_tabu_5_2_5_5_1_2_none_quake2_both_2_none_allow_allow_false_8";
            Request r = storageController.getRequest(id, true);
            //r = processingService.processRequest(r);
            Analysis a = storageController.getAnalysis(id, true);
            Analysis analysis = analysisService.analyzeRequest(r);
            //analysisController.analyzeRequest(AnalysisParameters.builder().requestId(id).useAws(true).build());
            System.out.println(printingService.outputPaths(r));
            //System.out.println(a);
            System.out.println(analysis);
        }
    }
}
