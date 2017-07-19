package netlab;


import com.amazonaws.services.dynamodbv2.model.ScanResult;
import netlab.analysis.analyzed.AggregationParameters;
import netlab.analysis.controller.AnalysisController;
import netlab.analysis.services.AnalysisService;
import netlab.storage.aws.dynamo.DynamoInterface;
import netlab.storage.aws.s3.S3Interface;
import netlab.storage.controller.StorageController;
import netlab.storage.services.StorageService;
import netlab.submission.controller.SubmissionController;
import netlab.submission.request.SimulationParameters;
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
import java.util.stream.Stream;

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

    //@Test
    public void updateRows(){
        SimulationParameters seedParams = SimulationParameters.builder().seed(1L).build();
        List<SimulationParameters> params = storageService.getMatchingSimulationParameters(seedParams);
        for(SimulationParameters param : params){
            param.setGenerated(false);
            storageService.putSimulationParameters(param);
        }
    }

    //@Test
    public void generateFailureRequests(){
        List<Long> seeds = Arrays.asList(1L);
        for(Long seed : seeds) {
            storageController.createFailedRequestSets(seed);
        }
    }

    @Test
    public void rerunRequests(){
       List<Long> seeds = Arrays.asList(1L);
        submissionController.rerunRequestSets(seeds);
    }

    @Test
    public void deleteRequests(){
        Long seed = 1L;
        storageController.deleteRecordsAndRequests(seed);
    }

    @Test
    public void analysisGeneration(){
        List<Long> seeds = Arrays.asList(29L, 30L);
        analysisController.analyzeSeeds(seeds);
    }

    @Test
    public void scanDynamoMetaDb() {
        if(dynamoInterface.allFieldsDefined()){
            ScanResult result = dynamoInterface.scanMetaTable();
            assert(result != null);
            System.out.println(result.toString());
        }
    }

    @Test
    public void aggregateAnalysis(){
        /*
        seeds = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30]
        topology_ids = ["NSFnet"]
        problem_classes = ["Flex", "Flow", "FlowSharedF", "EndpointSharedF", "Endpoint"]
        objectives = ["TotalCost"]
        algorithms = ["ServiceILP"]
        num_requests = [1]
        num_sources = [1, 7, 14]
        num_dests = [1, 7, 14]
        failure_set_dict = {
            "Link": [[0, 0, 0.0, 0.0], [1, 1, 0.0, 0.0], [21, 1, 0.0, 0.0], [21, 2, 0.0, 0.0]],
            "Node": [[1, 1, 0.0, 0.0], [1, 1, 0.0714, 0.0], [1, 1, 0.0, 0.0714], [14, 1, 1.0, 1.0], [14, 2, 1.0, 1.0]],
            "Both": [[35, 1, 1.0, 1.0]]
        }  # Includes failure class, num fails, num fails allowed, percent src fail, percent dst fail
        num_conns = [1, 7, 14]
        min_connection_ranges = [[0, 0], [1, 1]]
        max_connection_ranges = [[1, 1], [2, 2]]
        percent_src_also_dests = [0.0, 1.0]
        ignore_failures = [True, False]
         */
        //List<Long> seeds = LongStream.rangeClosed(1, 30).boxed().collect(Collectors.toList());
        List<Long> seeds = Arrays.asList(1L, 2L);
        List<String> topologyIds = Collections.singletonList("NSFnet");
        List<String> problemClasses = Arrays.asList("Flex", "Flow", "FlowSharedF", "EndpointSharedF", "Endpoint");
        List<String> objectives = Collections.singletonList("TotalCost");
        List<String> algorithms = Collections.singletonList("ServiceILP");
        List<Integer> numRequests = Collections.singletonList(1);
        List<Integer> numSources = Arrays.asList(1,7,14);
        List<Integer> numDests = Arrays.asList(1,7,14);
        Map<String, List<List<Double>>> failureMap = new HashMap<>();
        failureMap.put("Link", Arrays.asList(Arrays.asList(0.0, 0.0, 0.0, 0.0), Arrays.asList(1.0, 1.0, 0.0, 0.0), Arrays.asList(21.0, 1.0, 0.0, 0.0), Arrays.asList(21.0, 2.0, 0.0, 0.0)));
        failureMap.put("Node", Arrays.asList(Arrays.asList(1.0, 1.0, 0.0, 0.0), Arrays.asList(1.0, 1.0, 0.0714, 0.0),
                Arrays.asList(1.0, 1.0, 0.0, 0.0714), Arrays.asList(14.0, 1.0, 1.0, 1.0), Arrays.asList(14.0, 2.0, 1.0, 1.0)));
        failureMap.put("Both", Collections.singletonList(Arrays.asList(35.0, 1.0, 1.0, 1.0)));
        List<Integer> numConnections = Arrays.asList(1, 7, 14);
        List<List<Integer>> minConnectionRanges = Arrays.asList(Arrays.asList(0,0), Arrays.asList(1,1));
        List<List<Integer>> maxConnectionRanges = Arrays.asList(Arrays.asList(1,1), Arrays.asList(2,2));
        List<Double> percentSrcAlsoDests = Arrays.asList(0.0, 1.0);
        List<Boolean> ignoreFailures = Arrays.asList(true, false);
        AggregationParameters agParams = AggregationParameters.builder()
                .seeds(seeds)
                .topologyIds(topologyIds)
                .problemClasses(problemClasses)
                .objectives(objectives)
                .algorithms(algorithms)
                .numRequests(numRequests)
                .numSources(numSources)
                .numDestinations(numDests)
                .failureMap(failureMap)
                .numConnections(numConnections)
                .minConnectionRanges(minConnectionRanges)
                .maxConnectionRanges(maxConnectionRanges)
                .percentSrcAlsoDests(percentSrcAlsoDests)
                .ignoreFailures(ignoreFailures)
                .build();
        analysisController.aggregateSeeds(agParams);
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

    //@Test
    public void downloadFromAnalyzed() {
        if(s3Interface.allFieldsDefined()){
            File f = new File("test4.txt");
            f = s3Interface.downloadFromAnalyzed(f, "test.txt");
            assert(f != null);
        }
    }
}
