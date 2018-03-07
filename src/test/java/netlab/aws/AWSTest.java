package netlab.aws;


import com.amazonaws.services.dynamodbv2.model.ScanResult;
import netlab.TestConfiguration;
import netlab.analysis.analyzed.*;
import netlab.analysis.controller.AnalysisController;
import netlab.analysis.services.AnalysisService;
import netlab.storage.aws.dynamo.DynamoInterface;
import netlab.storage.aws.s3.S3Interface;
import netlab.storage.controller.StorageController;
import netlab.storage.services.StorageService;
import netlab.submission.controller.SubmissionController;
import netlab.submission.enums.*;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
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

    //@Test
    public void deleteRequests(){
        List<Long> seeds = LongStream.rangeClosed(1, 30).boxed().collect(Collectors.toList());
        String algorithm = null;
        boolean deleteRecords = true;
        boolean deleteAnalysis = false;
        for(Long seed : seeds) {
            long startTime = System.nanoTime();
            boolean success = storageController.deleteRecordsAndRequests(seed, algorithm, deleteRecords, deleteAnalysis);
            assert (success);
            long endTime = System.nanoTime();
            double duration = (endTime - startTime)/1e9;
            System.out.println("Deleted seed " + seed + ". Took: " + duration + " seconds");
        }
    }

    //@Test
    public void analysisGeneration(){
        List<Long> seeds = Arrays.asList(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L);
        analysisController.analyzeSeeds(seeds);
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
    public void downloadFromAnalyzed() {
        if(s3Interface.allFieldsDefined()){
            String id = "24_tw_anycast_ilp_1_3_1_1_1_1_none_alllinks_both_2_none_allow_allow_false_8";
            Request r = storageController.getRequest(id, true);
            Analysis a = storageController.getAnalysis(id, true);
            Analysis analysis = analysisService.analyzeRequest(r);
            //analysisController.analyzeRequest(AnalysisParameters.builder().requestId(id).useAws(true).build());
            System.out.println(r);
            System.out.println(a);
            System.out.println(analysis);
        }
    }
}
