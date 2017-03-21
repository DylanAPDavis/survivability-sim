package netlab;

import com.amazonaws.services.dynamodbv2.model.ScanResult;
import netlab.aws.dynamo.DynamoInterface;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class SurvivabilitySimApplicationTests {

	@Autowired
	private DynamoInterface dynamoInterface;

	@Test
	public void contextLoads() {
	}

	@Test
	public void createDynamoInstance() {
		ScanResult result = dynamoInterface.scan("TestsMeta");
		System.out.println(result.toString());

		//List<String> tablesList = dynamoInterface.listTables();
		//System.out.println(tablesList);
	}

}
