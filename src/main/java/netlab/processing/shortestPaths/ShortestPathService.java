package netlab.processing.shortestPaths;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.Connections;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ShortestPathService {

    public Details solve(Request request, Topology topo){
        Details details = request.getDetails();
        Map<SourceDestPair, Map<String, Path>> paths = new HashMap<>();
        Connections connections = details.getConnections();
        // Requirements
        Integer reqNumConnections = connections.getNumConnections();
        Integer useMinS = connections.getUseMinS();
        Integer useMaxS = connections.getUseMaxS();
        Integer useMinD = connections.getUseMinD();
        Integer useMaxD = connections.getUseMaxD();

        Map<SourceDestPair, Integer> pairMinConnectionsMap = connections.getPairMinConnectionsMap();
        Map<SourceDestPair, Integer> pairMaxConnectionsMap = connections.getPairMaxConnectionsMap();

        Map<Node, Integer> srcMinConnectionsMap = connections.getSrcMinConnectionsMap();
        Map<Node, Integer> srcMaxConnectionsMap = connections.getSrcMaxConnectionsMap();

        Map<Node, Integer> dstMinConnectionsMap = connections.getDstMinConnectionsMap();
        Map<Node, Integer> dstMaxConnectionsMap = connections.getDstMaxConnectionsMap();

        // Variables for tracking completion
        int numConnections = 0;
        int numSUsed = 0;
        int numDUsed = 0;
        Map<SourceDestPair, Integer> pairConnectionsMap = pairMinConnectionsMap.keySet()
                .stream()
                .collect(Collectors.toMap(p -> p, p -> 0));
        Map<Node, Integer> srcConnectionsMap = srcMinConnectionsMap.keySet()
                .stream()
                .collect(Collectors.toMap(s -> s, s -> 0));
        Map<Node, Integer> dstConnectionsMap = dstMaxConnectionsMap.keySet()
                .stream()
                .collect(Collectors.toMap(d -> d, d -> 0));

        boolean allSatisfied = false;

        // Need to add connections as we go through the pairs
        // Possibility all pairs may have a min of 0, so you need to add connections even when not "needed"
        // The variables that will influence this are minConnections, useMin/MaxS and useMin/MaxD
        // Get the set of pairs, sources, and dests that have a minimum > 0
        Set<Node> sourcesWithMin = srcMinConnectionsMap.keySet().stream()
                .filter(s -> srcMinConnectionsMap.get(s) > 0)
                .collect(Collectors.toSet());
        Set<Node> destsWithMin = dstMinConnectionsMap.keySet().stream()
                .filter(d -> dstMinConnectionsMap.get(d) > 0)
                .collect(Collectors.toSet());
        Set<SourceDestPair> pairsWithMin = pairMinConnectionsMap.keySet().stream()
                .filter(p -> pairMinConnectionsMap.get(p) > 0)
                .collect(Collectors.toSet());
        long startTime = System.nanoTime();
        while(!allSatisfied) {
            if(sourcesWithMin.isEmpty() && destsWithMin.isEmpty() && pairsWithMin.isEmpty()){

            }
            else{
                
            }
            allSatisfied = testSatisfication(reqNumConnections, useMinS, useMaxS, useMinD, useMaxD, pairMinConnectionsMap,
                    pairMaxConnectionsMap, srcMinConnectionsMap, srcMaxConnectionsMap, dstMinConnectionsMap, dstMaxConnectionsMap,
                    numConnections, numSUsed, numDUsed, pairConnectionsMap, srcConnectionsMap, dstConnectionsMap);
        }
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        details.setChosenPaths(paths);
        details.setRunningTimeSeconds(duration);
        return details;
    }

    // Confirm that you've met all requirements
    private boolean testSatisfication(Integer reqNumConnections, Integer useMinS, Integer useMaxS, Integer useMinD,
                                      Integer useMaxD, Map<SourceDestPair, Integer> pairMinConnectionsMap,
                                      Map<SourceDestPair, Integer> pairMaxConnectionsMap,
                                      Map<Node, Integer> srcMinConnectionsMap, Map<Node, Integer> srcMaxConnectionsMap,
                                      Map<Node, Integer> dstMinConnectionsMap, Map<Node, Integer> dstMaxConnectionsMap,
                                      int numConnections, int numSUsed, int numDUsed,
                                      Map<SourceDestPair, Integer> pairConnectionsMap,
                                      Map<Node, Integer> srcConnectionsMap, Map<Node, Integer> dstConnectionsMap) {

        boolean enoughConns = numConnections >= reqNumConnections;
        boolean enoughS = numSUsed >= useMinS && numSUsed <= useMaxS;
        boolean enoughD = numDUsed >= useMinD && numDUsed <= useMaxD;
        boolean enoughPairs = pairConnectionsMap.keySet()
                .stream()
                .allMatch(p -> pairConnectionsMap.get(p) >= pairMinConnectionsMap.get(p)
                        && pairConnectionsMap.get(p) <= pairMaxConnectionsMap.get(p));
        boolean enoughSConn = srcConnectionsMap.keySet()
                .stream()
                .allMatch(s -> srcConnectionsMap.get(s) >= srcMinConnectionsMap.get(s)
                        && srcConnectionsMap.get(s) <= srcMaxConnectionsMap.get(s));
        boolean enoughDConn = dstConnectionsMap.keySet()
                .stream()
                .allMatch(d -> dstConnectionsMap.get(d) >= dstMinConnectionsMap.get(d)
                        && dstConnectionsMap.get(d) <= dstMaxConnectionsMap.get(d));

        return enoughConns && enoughS && enoughD && enoughPairs && enoughSConn && enoughDConn;
    }
}
