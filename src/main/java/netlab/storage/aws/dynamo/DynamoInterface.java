package netlab.storage.aws.dynamo;


import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.storage.aws.config.AwsConfig;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        Map<String, AttributeValue> eav = createExpressionAttributeValueMap(params);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        for(String attributeName : eav.keySet()){
            AttributeValue value = eav.get(attributeName);
            scanExpression.addFilterCondition(attributeName, new Condition().withComparisonOperator(ComparisonOperator.EQ).withAttributeValueList(value));
        }

        List<SimulationParameters> paramList =  mapper.scan(SimulationParameters.class, scanExpression);
        return paramList.stream().map(SimulationParameters::getRequestSetId).collect(Collectors.toList());
    }

    public Map<String, AttributeValue> createExpressionAttributeValueMap(SimulationParameters params){
        Map<String, AttributeValue> eav = new HashMap<>();
        if(params.getRequestSetId() != null) {eav.put("requestSetId", new AttributeValue().withS(params.getRequestSetId()));}
        if(params.getSeed() != null) {eav.put("seed", new AttributeValue().withN(Long.toString(params.getSeed())));}
        if(params.getTopologyId() != null) {eav.put("topologyId", new AttributeValue().withS(params.getTopologyId()));}
        if(params.getProblemClass() != null) {eav.put("problemClass", new AttributeValue().withS(params.getProblemClass()));}
        if(params.getObjective() != null) {eav.put("objective", new AttributeValue().withS(params.getObjective()));}
        if(params.getAlgorithm() != null) {eav.put("algorithm", new AttributeValue().withS(params.getAlgorithm()));}
        if(params.getNumRequests() != null) {eav.put("numRequests", new AttributeValue().withN(Integer.toString(params.getNumRequests())));}
        if(params.getNumSources() != null) {eav.put("numSources", new AttributeValue().withN(Integer.toString(params.getNumSources())));}
        if(params.getNumDestinations() != null) {eav.put("numDestinations", new AttributeValue().withN(Integer.toString(params.getNumDestinations())));}
        if(params.getFailureSetSize() != null) {eav.put("failureSetSize", new AttributeValue().withN(Integer.toString(params.getFailureSetSize())));}
        if(params.getMinMaxFailures() != null && !params.getMinMaxFailures().isEmpty()) {
            eav.put("minMaxFailures", new AttributeValue().withNS(params.getMinMaxFailures().stream().map(Object::toString).collect(Collectors.toList())));
        }
        if(params.getFailureClass() != null) {eav.put("failureClass", new AttributeValue().withS(params.getFailureClass()));}
        if(params.getFailureProb() != null) {eav.put("failureProb", new AttributeValue().withN(Double.toString(params.getFailureProb())));}
        if(params.getMinMaxFailureProb() != null  && !params.getMinMaxFailureProb().isEmpty()) {
            eav.put("minMaxFailureProb", new AttributeValue().withNS(params.getMinMaxFailureProb().stream().map(Object::toString).collect(Collectors.toList())));
        }
        if(params.getMinConnectionsRange() != null  && !params.getMinConnectionsRange().isEmpty()) {
            eav.put("minConnectionsRange", new AttributeValue().withNS(params.getMinConnectionsRange().stream().map(Object::toString).collect(Collectors.toList())));
        }
        if(params.getMaxConnectionsRange() != null  && !params.getMaxConnectionsRange().isEmpty()) {
            eav.put("maxConnectionsRange", new AttributeValue().withNS(params.getMaxConnectionsRange().stream().map(Object::toString).collect(Collectors.toList())));
        }
        if(params.getNumFailsAllowed() != null) {eav.put("numFailsAllowed", new AttributeValue().withN(Integer.toString(params.getNumFailsAllowed())));}
        if(params.getMinMaxFailsAllowed() != null  && !params.getMinMaxFailsAllowed().isEmpty()) {
            eav.put("minMaxFailsAllowed", new AttributeValue().withNS(params.getMinMaxFailsAllowed().stream().map(Object::toString).collect(Collectors.toList())));
        }
        if(params.getProcessingType() != null) {eav.put("processingType", new AttributeValue().withS(params.getProcessingType()));}
        if(params.getPercentSrcAlsoDest() != null) {eav.put("percentSrcAlsoDest", new AttributeValue().withN(Double.toString(params.getPercentSrcAlsoDest())));}
        if(params.getPercentSrcFail() != null) {eav.put("percentSrcFail", new AttributeValue().withN(Double.toString(params.getPercentSrcFail())));}
        if(params.getPercentDestFail() != null) {eav.put("percentDestFail", new AttributeValue().withN(Double.toString(params.getPercentDestFail())));}
        if(params.getSdn() != null) {eav.put("sdn", new AttributeValue().withN(String.valueOf(params.getSdn() ? 1 : 0)));}
        if(params.getUseAws() != null) {eav.put("useAws", new AttributeValue().withN(String.valueOf(params.getUseAws() ? 1 : 0)));}
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

}
