package netlab.storage;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.processing.ProcessingService;
import netlab.storage.aws.dynamo.DynamoInterface;
import netlab.storage.aws.s3.S3Interface;
import netlab.submission.request.RequestSet;
import netlab.submission.services.GenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        /*List<String> nameComponents = new ArrayList<>();
        nameComponents.add(requestSet.getProblemClass().getCode());
        nameComponents.add(requestSet.getAlgorithm().getCode());
        nameComponents.add(requestSet.getObjective().getCode());
        nameComponents.add(requestSet.getFailureClass().getCode());
        nameComponents.add(Long.toString(requestSet.getSeed()));
        nameComponents.add(requestSet.getId());*/
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

    public void storeAnalyzedSet(AnalyzedSet analyzedSet){

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
