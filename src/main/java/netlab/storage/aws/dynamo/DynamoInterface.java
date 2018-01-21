package netlab.storage.aws.dynamo;


import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import netlab.storage.aws.config.AwsConfig;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.lang.reflect.*;

@Controller
public class DynamoInterface {

    private AmazonDynamoDB database;
    private AwsConfig awsConfig;
    private DynamoDBMapper mapper;

    @Autowired
    public DynamoInterface(AwsConfig config){
        awsConfig = config;
        if(awsConfig.allFieldsDefined()){
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
            mapper = new DynamoDBMapper(database);
        }
        else{
            database = null;
        }
    }

    public boolean allFieldsDefined(){
        return database != null && awsConfig != null && mapper != null;
    }

    public boolean put(SimulationParameters params){
        if(mapper != null) {
            mapper.save(params);
            return true;
        }
        return false;
    }

    public SimulationParameters get(String requestSetId){
        if(mapper != null) {
            return mapper.load(SimulationParameters.class, requestSetId);
        }
        return null;
    }

    // Given full/partial definition of parameters, retrieve matching requestSetIds
    public List<String> getRequestSetIds(SimulationParameters params){
        return getSimulationParameters(params).stream().map(SimulationParameters::getRequestId).collect(Collectors.toList());
    }

    public List<SimulationParameters> queryForSeed(Long seed){
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withN(String.valueOf(seed)));

        DynamoDBQueryExpression<SimulationParameters> queryExpression = new DynamoDBQueryExpression<SimulationParameters>()
                .withKeyConditionExpression("seed = :val1").withExpressionAttributeValues(eav)
                .withIndexName("seed-index")
                .withConsistentRead(false);

        return mapper.query(SimulationParameters.class, queryExpression);
    }

    public List<SimulationParameters> queryForId(String requestSetId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withS(requestSetId));

        DynamoDBQueryExpression<SimulationParameters> queryExpression = new DynamoDBQueryExpression<SimulationParameters>()
                .withKeyConditionExpression("requestId = :val1").withExpressionAttributeValues(eav)
                .withConsistentRead(false);

        return mapper.query(SimulationParameters.class, queryExpression);
    }

    public List<SimulationParameters> getSimulationParameters(SimulationParameters params){
        Map<String, AttributeValue> eav = createExpressionAttributeValueMap(params);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        for(String attributeName : eav.keySet()){
            AttributeValue value = eav.get(attributeName);
            scanExpression.addFilterCondition(attributeName, new Condition().withComparisonOperator(ComparisonOperator.EQ).withAttributeValueList(value));
        }
        scanExpression.setIndexName("seed-index");

        return mapper.scan(SimulationParameters.class, scanExpression);
    }


    public Map<String, AttributeValue> createExpressionAttributeValueMap(SimulationParameters params){
        Map<String, AttributeValue> eav = new HashMap<>();

        Class c = params.getClass();
        Field[] fields = c.getDeclaredFields();
        for(Field field : fields){
            String name = field.getName();
            Type type = field.getType();
            try {
                Object value = field.get(params);
                if(type.getTypeName().equals("java.lang.String")){
                    if(value != null){
                        eav.put(name, new AttributeValue().withS(value.toString()));
                    }
                }
                if(type.getTypeName().equals("java.lang.Boolean")){
                    if(value != null){
                        eav.put(name, new AttributeValue().withN(String.valueOf((boolean)value ? 1 : 0)));
                    }
                }
                if(type.getTypeName().equals("java.lang.Integer")) {
                    if(value != null){
                        eav.put(name, new AttributeValue().withN(Integer.toString((int)value)));
                    }
                }
                if(type.getTypeName().equals("java.lang.Long")) {
                    if(value != null){
                        eav.put(name, new AttributeValue().withN(Long.toString((long)value)));
                    }
                }
                if(type.getTypeName().equals("java.lang.Double")) {
                    if(value != null){
                        eav.put(name, new AttributeValue().withN(Double.toString((double)value)));
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return eav;
    }


    public ScanResult scanMetaTable(){
        return scan(awsConfig.getMetaDb());
    }

    public ScanResult scan(String tableName){
        if(database == null){
            return null;
        }
        ScanRequest scanRequest = new ScanRequest(tableName);
        return database.scan(scanRequest);
    }

    public void shutdown(){
        if(database != null) database.shutdown();
    }


    public Boolean deleteRecords(List<SimulationParameters> requestParameters) {
        try {
            mapper.batchDelete(requestParameters);
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
