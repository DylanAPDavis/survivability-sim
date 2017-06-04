package netlab.storage.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.processing.ProcessingService;
import netlab.storage.aws.dynamo.DynamoInterface;
import netlab.storage.aws.s3.S3Interface;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
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

    public boolean storeRequestSet(RequestSet requestSet, boolean useAws) {
        File outputFile = createFile(requestSet.getId(), "raw");
        if(useAws){
            writeLocal(requestSet, outputFile);
            return s3Interface.uploadToRaw(outputFile, requestSet.getId());
        }
        else {
            return writeLocal(requestSet, outputFile);
        }
    }

    public RequestSet retrieveRequestSet(String requestSetId, boolean useAws){
        RequestSet rs = null;
        File f = new File(System.getProperty("user.dir") + "/results/raw/" + requestSetId);
        if(useAws){
            f = s3Interface.downloadFromRaw(f, requestSetId);
        }
        if(f != null && f.exists()){
            rs = readRequestSetLocal(f);
        }
        return rs;
    }

    public boolean storeAnalyzedSet(AnalyzedSet analyzedSet, boolean useAws){
        File outputFile = createFile(analyzedSet.getRequestSetId(), "analyzed");
        if(useAws){
            writeLocal(analyzedSet, outputFile);
            return s3Interface.uploadToAnalyzed(outputFile, analyzedSet.getRequestSetId());
        }
        else {
            return writeLocal(analyzedSet, outputFile);
        }
    }

    public AnalyzedSet retrieveAnalyzedSet(String requestSetId, boolean useAws){
        AnalyzedSet as = null;
        File f = new File(System.getProperty("user.dir") + "/results/analyzed/" + requestSetId);
        if(useAws){
            f = s3Interface.downloadFromAnalyzed(f, requestSetId);
        }
        if(f != null && f.exists()){
            as = readAnalyzedSetLocal(f);
        }
        return as;
    }

    public boolean putSimulationParameters(SimulationParameters params){
        return dynamoInterface.put(params);
    }

    public List<AnalyzedSet> getAnalyzedSets(SimulationParameters params){
        List<String> requestSetIds = dynamoInterface.getRequestSetIds(params);
        List<AnalyzedSet> sets = new ArrayList<>();
        for(String id : requestSetIds){
            AnalyzedSet set = retrieveAnalyzedSet(id, params.getUseAws());
            if(set != null){
                sets.add(set);
            }
        }
        return sets;
    }

    // Private subfunctions

    private RequestSet readRequestSetLocal(File file){
        return (RequestSet) readLocal(file);
    }

    private AnalyzedSet readAnalyzedSetLocal(File file){
        return (AnalyzedSet) readLocal(file);
    }

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
        } catch (IOException e) {
            System.out.println("Error initializing stream");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return obj;
    }

    private boolean writeLocal(Object object, File outputFile){
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
            System.out.println("Error initializing stream");
            return false;
        }
        return true;
    }

    private File createFile(String id, String subDir){
        //String fileName = nameComponents.stream().reduce("", (s1, s2) -> s1 + "_" + s2);
        //fileName = fileName.substring(1);
        String outputPath = System.getProperty("user.dir") + "/results/" + subDir + "/";
        return new File(outputPath + id);
    }

}
