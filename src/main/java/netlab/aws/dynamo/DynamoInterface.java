package netlab.aws.dynamo;


import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import netlab.aws.config.AwsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class DynamoInterface {

    private AmazonDynamoDB database;

    @Autowired
    public DynamoInterface(AwsConfig awsConfig){
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsConfig.getAccessKeyId(), awsConfig.getSecretAccessKey());
        Regions regions = Regions.fromName(awsConfig.getRegion());
        AWSSecurityTokenServiceClient client = new AWSSecurityTokenServiceClient(awsCreds);
        STSAssumeRoleSessionCredentialsProvider provider = new STSAssumeRoleSessionCredentialsProvider
                .Builder(awsConfig.getRoleArn(), awsConfig.getRoleSessionName())
                .withStsClient(client)
                .build();
        database = AmazonDynamoDBClientBuilder.standard()
                .withRegion(regions)
                .withCredentials(provider)
                .build();
    }

    public List<String> listTables(){
        ListTablesResult listTablesResult = database.listTables();
        return listTablesResult.getTableNames();
    }

    public ScanResult scan(String tableName){
        ScanRequest scanRequest = new ScanRequest(tableName);
        return database.scan(scanRequest);
    }


}
