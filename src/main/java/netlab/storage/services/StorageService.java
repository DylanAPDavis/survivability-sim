package netlab.storage.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.Analysis;
import netlab.storage.aws.dynamo.DynamoInterface;
import netlab.storage.aws.s3.S3Interface;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import netlab.topology.elements.TopologyMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StorageService {

    private S3Interface s3Interface;
    private DynamoInterface dynamoInterface;

    @Autowired
    public StorageService(S3Interface s3Interface, DynamoInterface dynamoInterface) {
        this.s3Interface = s3Interface;
        this.dynamoInterface = dynamoInterface;
    }

    public boolean storeRequestSet(Request request, boolean useAws) {
        File outputFile = createFile(request.getId(),"/results/raw/");
        if(useAws){
            writeLocal(request, outputFile);
            return s3Interface.uploadToRaw(outputFile, request.getId());
        }
        else {
            return writeLocal(request, outputFile);
        }
    }

    public Request retrieveRequestSet(String requestSetId, boolean useAws){
        Request rs = null;
        File f = new File(System.getProperty("user.dir") + "/results/raw/" + requestSetId);
        if(useAws){
            f = s3Interface.downloadFromRaw(f, requestSetId);
        }
        if(f != null && f.exists()){
            rs = readRequestSetLocal(f);
        }
        return rs;
    }

    public boolean storeAnalyzedSet(Analysis analysis, boolean useAws){
        File outputFile = createFile(analysis.getRequestId(), "/results/raw/");
        if(useAws){
            writeLocal(analysis, outputFile);
            return s3Interface.uploadToAnalyzed(outputFile, analysis.getRequestId());
        }
        else {
            return writeLocal(analysis, outputFile);
        }
    }

    public Analysis retrieveAnalyzedSet(String requestSetId, boolean useAws){
        return retrieveAnalyzedSet(requestSetId, useAws, false);
    }

    public Analysis retrieveAnalyzedSet(String requestSetId, boolean useAws, boolean deleteAfter){
        Analysis as = null;
        File f = new File(System.getProperty("user.dir") + "/results/analyzed/" + requestSetId);
        if(useAws && !f.exists()){
            f = s3Interface.downloadFromAnalyzed(f, requestSetId);
        }
        if(f != null && f.exists()){
            as = readAnalyzedSetLocal(f);
            if(deleteAfter){
                f.delete();
            }
        }
        return as;
    }

    public boolean storeTopologyMetrics(TopologyMetrics topologyMetrics){
        String fileName = topologyMetrics.getTopologyId() + "_metrics";
        File outputFile = createFile(fileName, "/config/topologies/" + topologyMetrics.getTopologyId() + "/");
        return writeLocal(topologyMetrics, outputFile);
    }

    public TopologyMetrics retrieveTopologyMetrics(String topologyId){
        TopologyMetrics tm = null;
        String fileName = topologyId + "_metrics";
        File f = createFile(fileName, "/config/topologies/" + topologyId + "/");
        //new File(System.getProperty("user.dir") + "/config/topologies/" + topologyId + "/" + fileName);
        if(f.exists()){
            tm = readTopologyMetricsLocal(f);
        }
        return tm;
    }

    public boolean putSimulationParameters(SimulationParameters params){
        return dynamoInterface.put(params);
    }

    public List<SimulationParameters> getMatchingSimulationParameters(SimulationParameters params){
        return dynamoInterface.getSimulationParameters(params);
    }

    public List<SimulationParameters> queryForSeed(Long seed){
        return dynamoInterface.queryForSeed(seed);
    }

    public List<SimulationParameters> queryForId(String requestSetId){
        return dynamoInterface.queryForId(requestSetId);
    }


    public List<Analysis> getAnalyzedSets(SimulationParameters params){
        List<String> requestSetIds = dynamoInterface.getRequestSetIds(params);
        List<Analysis> sets = new ArrayList<>();
        for(String id : requestSetIds){
            Analysis set = retrieveAnalyzedSet(id, params.getUseAws());
            if(set != null){
                sets.add(set);
            }
        }
        return sets;
    }

    public Boolean deleteRequests(Long seed, String algorithm, String routing, boolean deleteRecords, boolean deleteAnalysis){
        List<SimulationParameters> matchingParams = dynamoInterface.queryForSeed(seed);
        //List<String> requestSetIds = matchingParams.stream().map(SimulationParameters::getRequestId).collect(Collectors.toList());
        //Boolean deleteRequests = s3Interface.deleteFromBucket(requestSetIds, "raw") && s3Interface.deleteFromBucket(requestSetIds, "analyzed");

        List<SimulationParameters> paramsToDelete = matchingParams;
        if(algorithm != null) {
            paramsToDelete = matchingParams.stream()
                    .filter(p -> p.getAlgorithm().toLowerCase().equals(algorithm.toLowerCase()))
                    .filter(p -> p.getRoutingType().toLowerCase().equals(routing.toLowerCase()))
                    .collect(Collectors.toList());
        }
        boolean success = true;
        if(deleteRecords) {
            success =  dynamoInterface.deleteRecords(paramsToDelete);
        }
        if(deleteAnalysis){
            List<String> requestSetIds = paramsToDelete.stream().map(SimulationParameters::getRequestId).collect(Collectors.toList());
            success = s3Interface.deleteFromBucket(requestSetIds, "analyzed");
        }
        return success;
    }

    // Private subfunctions

    private Request readRequestSetLocal(File file){
        return (Request) readLocal(file);
    }

    private Analysis readAnalyzedSetLocal(File file){
        return (Analysis) readLocal(file);
    }

    private TopologyMetrics readTopologyMetricsLocal(File file) {return (TopologyMetrics) readLocal(file);}

    private Object readLocal(File file){
        Object obj = null;
        try{
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream oi = new ObjectInputStream(fi);
            // Read object
            obj = oi.readObject();
            oi.close();
            fi.close();

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    public boolean writeLocal(Object object, File outputFile){
        try {
            FileOutputStream f = new FileOutputStream(outputFile);
            ObjectOutputStream o = new ObjectOutputStream(f);

            // Write object to file
            o.writeObject(object);

            o.close();
            f.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public File createFile(String id, String path){
        //String fileName = nameComponents.stream().reduce("", (s1, s2) -> s1 + "_" + s2);
        //fileName = fileName.substring(1);
        String outputPath = System.getProperty("user.dir") + path;
        if(Files.notExists(Paths.get(outputPath))){
            try {
                Files.createDirectory(Paths.get(outputPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new File(outputPath + id);
    }

}
