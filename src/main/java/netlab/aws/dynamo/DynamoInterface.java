package netlab.aws.dynamo;


import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
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
        database = AmazonDynamoDBClientBuilder.standard()
                .withRegion(regions)
                .withCredentials(new StaticCredentialsProvider(awsCreds))
                .build();
    }

    public List<String> listTables(){
        ListTablesResult listTablesResult = database.listTables();
        return listTablesResult.getTableNames();
    }


}
