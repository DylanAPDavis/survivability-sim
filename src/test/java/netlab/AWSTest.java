package netlab;


import com.amazonaws.services.dynamodbv2.model.ScanResult;
import netlab.analysis.controller.AnalysisController;
import netlab.analysis.services.AnalysisService;
import netlab.storage.aws.dynamo.DynamoInterface;
import netlab.storage.aws.s3.S3Interface;
import netlab.storage.services.StorageService;
import netlab.submission.request.SimulationParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
    private AnalysisController analysisController;

    //@Test
    public void updateRows(){
        SimulationParameters seedParams = SimulationParameters.builder().seed(1L).build();
        List<SimulationParameters> params = storageService.getMatchingSimulationParameters(seedParams);
        for(SimulationParameters param : params){
            param.setGenerated(false);
            storageService.putSimulationParameters(param);
        }
        System.out.println("Yo");
    }

    @Test
    public void generateFailureRequests(){
        Long seed = 1L;
        analysisController.createFailedRequestSets(seed);
        System.out.println("Yo");
    }

    @Test
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

    //@Test
    public void downloadFromAnalyzed() {
        if(s3Interface.allFieldsDefined()){
            File f = new File("test4.txt");
            f = s3Interface.downloadFromAnalyzed(f, "test.txt");
            assert(f != null);
        }
    }
}
