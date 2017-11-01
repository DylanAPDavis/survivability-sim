package netlab.processing.shortestPaths;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.Connections;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ShortestPathService {

    public Details solve(Request request, Topology topo){
        Details details = request.getDetails();
        Map<SourceDestPair, Map<String, Path>> paths = new HashMap<>();
        long startTime = System.nanoTime();
        // A* Algorithm
        for(SourceDestPair pair : details.getPairs()){
            Connections connections = details.getConnections();
        }
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setChosenPaths(paths);
        details.setRunningTimeSeconds(duration);
        return details;
    }
}
