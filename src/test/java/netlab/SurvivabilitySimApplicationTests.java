package netlab;

import com.amazonaws.services.dynamodbv2.model.ScanResult;
import netlab.aws.dynamo.DynamoInterface;
import netlab.aws.s3.S3Interface;
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
public class SurvivabilitySimApplicationTests {

	@Autowired
	private DynamoInterface dynamoInterface;

	@Autowired
	private S3Interface s3Interface;

	@Test
	public void contextLoads() {
	}

	@Test
	public void scanDynamoMetaDb() {
		ScanResult result = dynamoInterface.scanMetaTable();
		assert(result != null);
		System.out.println(result.toString());
	}

	@Test
	public void uploadToRawS3() {
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

	@Test
	public void downloadFromRaw() {
		File f = new File("test2.txt");
		f = s3Interface.downloadFromRaw(f, "test.txt");
		assert(f != null);
	}

	@Test
	public void uploadToAnalyzedS3() {
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

	@Test
	public void downloadFromAnalyzed() {
		File f = new File("test4.txt");
		f = s3Interface.downloadFromAnalyzed(f, "test.txt");
		assert(f != null);
	}

}
