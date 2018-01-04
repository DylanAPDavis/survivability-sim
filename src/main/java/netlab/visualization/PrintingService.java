package netlab.visualization;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.Request;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PrintingService {

    public String outputPaths(Request request){
        Map<SourceDestPair, Map<String, Path>> pathMap = request.getDetails().getChosenPaths();
        Map<String, Map<SourceDestPair, List<String>>> stringsPerSource = new HashMap<>();
        StringBuilder pathBuilder = new StringBuilder();
        for(SourceDestPair pair : pathMap.keySet()){
            Map<String, Path> pathIdMap = pathMap.get(pair);
            List<String> pathStrings = new ArrayList<>();
            String sourceId = pair.getSrc().getId();
            if(pathIdMap.size() > 0){
                for(String pathId : pathIdMap.keySet()){
                    Path path = pathIdMap.get(pathId);
                    String pathString = "\t\t" + pathId + ": " + path.toString() + "\n";
                    pathStrings.add(pathString);
                }
            }
            stringsPerSource.putIfAbsent(sourceId, new HashMap<>());
            stringsPerSource.get(sourceId).put(pair, pathStrings);
        }
        for(String sourceId : stringsPerSource.keySet()){
            Map<SourceDestPair, List<String>> stringsPerPair = stringsPerSource.get(sourceId);
            pathBuilder.append(sourceId).append(": \n");
            for(SourceDestPair pair : stringsPerPair.keySet()) {
                List<String> pathStrings = stringsPerPair.get(pair);
                if (!pathStrings.isEmpty()) {
                    pathBuilder.append("\t").append(pair.toString()).append(": \n");
                    for (String pathString : pathStrings) {
                        pathBuilder.append(pathString);
                    }
                }
            }
        }
        return pathBuilder.toString();
    }

    public String outputFailures(Collection<Failure> failures){
        List<Failure> sortedFailures = failures.stream()
                .sorted(Comparator.comparing(Failure::getProbability))
                .collect(Collectors.toList());
        String output = "";
        for(Failure failure : sortedFailures){
            output += "\n" + failure;
        }
        return output;
    }
}
