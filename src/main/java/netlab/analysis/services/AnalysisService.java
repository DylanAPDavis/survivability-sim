package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.analyzed.PathMetrics;
import netlab.analysis.analyzed.PathSetMetrics;
import netlab.analysis.analyzed.RequestMetrics;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.*;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class AnalysisService {
    public AnalyzedSet analyzeRequestSet(RequestSet requestSet) {

        Map<String, Request> requestMap = requestSet.getRequests();
        Map<String, RequestMetrics> requestMetricsMap = new HashMap<>();
        for(String requestId : requestMap.keySet()){
            Request request = requestMap.get(requestId);
            RequestMetrics requestMetrics = generateMetrics(request, requestSet.getProblemClass());
            requestMetricsMap.put(requestId, requestMetrics);
        }
        return AnalyzedSet.builder().id(requestSet.getId()).requestMetrics(requestMetricsMap).build();
    }

    private RequestMetrics generateMetrics(Request request, ProblemClass problemClass) {
        Map<SourceDestPair, Map<String, Path>> chosenPaths = request.getChosenPaths();
        Set<SourceDestPair> pairs = request.getPairs();
        Connections connectionColl = request.getConnections();
        Failures failureColl = request.getFailures();
        Integer numConnections = connectionColl.getNumConnections();

        Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap = new HashMap<>();
        Boolean requestIsSurvivable = true;
        Integer numLinkUsages = 0;
        Integer numFailed = 0;
        Integer numPaths = 0;

        // Check the high-level attributes
        if(problemClass.equals(ProblemClass.Flex)){
            List<List<Failure>> failureGroups = failureColl.getFailureGroups();
            for(SourceDestPair pair : pairs){
                Map<String, Path> pathMap = chosenPaths.get(pair);
                numPaths += pathMap.values().size();

                Map<String, PathMetrics> pathMetrics = getPathMetrics(pathMap, failureGroups);
                PathSetMetrics pathSetMetrics = analyzePathMetrics(pathMetrics, 0, numConnections);

                numLinkUsages += pathSetMetrics.getNumLinkUsages();
                numFailed += pathSetMetrics.getNumFailed();
                pathSetMetricsMap.put(pair, pathSetMetrics);
            }
        }
        // Check the flow-level attributes
        else if(problemClass.equals(ProblemClass.Flow)){
            Map<SourceDestPair, Integer> minNumConnectionsMap = connectionColl.getPairMinConnectionsMap();
            Map<SourceDestPair, Integer> maxNumConnectionsMap = connectionColl.getPairMaxConnectionsMap();
            Map<SourceDestPair, List<List<Failure>>> failureGroupsMap = failureColl.getPairFailureGroupsMap();
            for(SourceDestPair pair : pairs){
                Map<String, Path> pathMap = chosenPaths.get(pair);
                numPaths += pathMap.values().size();

                Integer minConn = minNumConnectionsMap.get(pair);
                Integer maxConn = maxNumConnectionsMap.get(pair);
                List<List<Failure>> failureGroups = failureGroupsMap.get(pair);

                Map<String, PathMetrics> pathMetrics = getPathMetrics(pathMap, failureGroups);
                PathSetMetrics pathSetMetrics = analyzePathMetrics(pathMetrics, minConn, maxConn);

                numLinkUsages += pathSetMetrics.getNumLinkUsages();
                numFailed += pathSetMetrics.getNumFailed();
                pathSetMetricsMap.put(pair, pathSetMetrics);

                if(pathMap.values().size() - pathSetMetrics.getNumFailed() < minConn){
                    requestIsSurvivable = false;
                }
            }
        }

        if(numPaths - numFailed < numConnections){
            requestIsSurvivable = false;
        }

        return RequestMetrics.builder()
                .requestIsSurvivable(requestIsSurvivable)
                .numLinkUsages(numLinkUsages)
                .numFailed(numFailed)
                .numPaths(numPaths)
                .pathSetMetricsMap(pathSetMetricsMap)
                .build();
    }

    private Map<String, PathMetrics> getPathMetrics(Map<String, Path> pathMap, List<List<Failure>> failureGroups) {
        Map<String, PathMetrics> pathMetricsMap = new HashMap<>();
        for(String pathId : pathMap.keySet()){
            Path path = pathMap.get(pathId);
            Integer numLinks = path.getLinks().size();
            Boolean survived = testSurvival(path, failureGroups);
            PathMetrics metrics = PathMetrics.builder().numLinks(numLinks).survived(survived).build();
            pathMetricsMap.put(pathId, metrics);
        }
        return pathMetricsMap;
    }

    private Boolean testSurvival(Path path, List<List<Failure>> failureGroups) {
        Set<String> nodeIds = path.getNodeIds();
        Set<String> linkIds = path.getLinkIds();

        for(List<Failure> group : failureGroups){
            for(Failure failure : group){
                if(failure.getNode() != null){
                    if(nodeIds.contains(failure.getNode().getId())){
                        return false;
                    }
                }
                if(failure.getLink() != null){
                    if(linkIds.contains(failure.getLink().getId())){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private PathSetMetrics analyzePathMetrics(Map<String, PathMetrics> pathMetricsMap, Integer minConn, Integer maxConn) {

        Integer numLinkUsages = 0;
        Integer numFailed = 0;
        Integer numPaths = 0;
        if(!pathMetricsMap.isEmpty()) {
            for (PathMetrics pathMetrics : pathMetricsMap.values()) {
                numLinkUsages += pathMetrics.getNumLinks();
                if (!pathMetrics.getSurvived()) {
                    numFailed++;
                }
                numPaths++;
            }
        }

        Boolean atLeastMinConn = numPaths - numFailed >= minConn;
        Boolean atMostMaxConn = numPaths - numFailed <= maxConn;

        return PathSetMetrics.builder()
                .pathMetricsMap(pathMetricsMap)
                .numLinkUsages(numLinkUsages)
                .numFailed(numFailed)
                .atLeastMinConn(atLeastMinConn)
                .atMostMaxConn(atMostMaxConn)
                .numPaths(numPaths)
                .build();
    }


}
