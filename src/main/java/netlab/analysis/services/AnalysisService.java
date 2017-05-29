package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.analyzed.PathMetrics;
import netlab.analysis.analyzed.PathSetMetrics;
import netlab.analysis.analyzed.RequestMetrics;
import netlab.analysis.enums.MemberType;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.*;
import netlab.topology.elements.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
        return AnalyzedSet.builder().requestSetId(requestSet.getId()).requestMetrics(requestMetricsMap).build();
    }

    private RequestMetrics generateMetrics(Request request, ProblemClass problemClass) {
        Map<SourceDestPair, Map<String, Path>> chosenPaths = request.getChosenPaths();
        Set<SourceDestPair> pairs = request.getPairs();
        Set<Node> sources = request.getSources();
        Set<Node> destinations = request.getDestinations();
        Connections connectionColl = request.getConnections();
        Failures failureColl = request.getFailures();
        Integer numConnections = connectionColl.getNumConnections();

        Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap = new HashMap<>();
        Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap = new HashMap<>();
        Boolean requestIsSurvivable = true;
        Integer numLinkUsages = 0;
        Integer numFailed = 0;
        Integer numPaths = 0;

        // Check the high-level attributes
        if(problemClass.equals(ProblemClass.Flex) || problemClass.equals(ProblemClass.EndpointSharedF) || problemClass.equals(ProblemClass.FlowSharedF)){
            List<List<Failure>> failureGroups = failureColl.getFailureGroups();
            // Must analyze paths based on total paths - choose just one failure group for everybody
            pathSetMetricsMap = analyzeAllPaths(chosenPaths, pairs, failureGroups);
            for(PathSetMetrics psm : pathSetMetricsMap.values()){
                numLinkUsages += psm.getNumLinkUsages();
                numFailed += psm.getNumFailed();
                numPaths += psm.getNumPaths();
            }
        }
        // Check the flow-level attributes
        if(problemClass.equals(ProblemClass.Flow)){
            Map<SourceDestPair, Integer> minNumConnectionsMap = connectionColl.getPairMinConnectionsMap();
            Map<SourceDestPair, List<List<Failure>>> failuresMap = failureColl.getPairFailureGroupsMap();
            // Must analyze number of paths based on pairs
            for(SourceDestPair pair : pairs){
                Map<String, Path> pathMap = chosenPaths.get(pair);
                numPaths += pathMap.values().size();

                Integer minConn = minNumConnectionsMap.get(pair);

                List<List<Failure>> failureGroups = failuresMap.get(pair);

                PathSetMetrics pathSetMetrics = getWorstCasePathSetMetrics(pathMap, failureGroups);
                numLinkUsages += pathSetMetrics.getNumLinkUsages();
                numFailed += pathSetMetrics.getNumFailed();
                pathSetMetricsMap.put(pair, pathSetMetrics);

                if(pathMap.values().size() - pathSetMetrics.getNumFailed() < minConn){
                    requestIsSurvivable = false;
                }
            }
        }
        if(problemClass.equals(ProblemClass.Endpoint)){
            Map<Node, List<List<Failure>>> srcFailureGroupsMap = failureColl.getSrcFailureGroupsMap();
            Map<Node, List<List<Failure>>> dstFailureGroupMap = failureColl.getDstFailureGroupsMap();
            memberPathSetMetricsMap = analyzeAllPathsForEndpoints(chosenPaths, sources, destinations, pairs, srcFailureGroupsMap, dstFailureGroupMap);
            List<Path> paths = chosenPaths.values().stream().map(Map::values).flatMap(Collection::stream).collect(Collectors.toList());
            numPaths = paths.size();
            numLinkUsages = paths.stream().map(p -> p.getLinks().size()).reduce(0, (pLength1, pLength2) -> pLength1 + pLength2);
            numFailed = determineMaxNumFailedAcrossMemberTypes(memberPathSetMetricsMap);
        }

        if(numPaths - numFailed < numConnections){
            requestIsSurvivable = false;
        }

        return RequestMetrics.builder()
                .isSurvivable(requestIsSurvivable)
                .isFeasible(request.getIsFeasible())
                .numLinksUsed(numLinkUsages)
                .numDisconnectedPaths(numFailed)
                .numPaths(numPaths)
                .pathSetMetricsMap(pathSetMetricsMap)
                .memberPathSetMetricsMap(memberPathSetMetricsMap)
                .build();
    }

    private Integer determineMaxNumFailedAcrossMemberTypes(Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap) {
        Integer maxSum = 0;
        for(MemberType memberType : memberPathSetMetricsMap.keySet()){
            Map<Node, Map<SourceDestPair, PathSetMetrics>> memberMetricsMap = memberPathSetMetricsMap.get(memberType);
            Integer memberSum = 0;
            for(Node node : memberMetricsMap.keySet()){
                Map<SourceDestPair, PathSetMetrics> metricsMap = memberMetricsMap.get(node);
                memberSum +=  metricsMap.values().stream().mapToInt(PathSetMetrics::getNumFailed).sum();
            }
            maxSum = maxSum < memberSum ? memberSum : maxSum;
        }
        return maxSum;
    }

    private Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> analyzeAllPathsForEndpoints(Map<SourceDestPair, Map<String, Path>> chosenPaths,
                                                                                        Set<Node> sources, Set<Node> destinations,
                                                                                        Set<SourceDestPair> pairs,
                                                                                        Map<Node, List<List<Failure>>> srcFailureGroupsMap,
                                                                                        Map<Node, List<List<Failure>>> dstFailureGroupMap) {
        // Must analyze number of paths based on source/dest
        // Pick worst case failure group per member
        Map<Node, Map<SourceDestPair, PathSetMetrics>> metricsBySource = getPathSetMetricsPerMember(sources, pairs, srcFailureGroupsMap, chosenPaths, true);
        Map<Node, Map<SourceDestPair, PathSetMetrics>> metricsByDest = getPathSetMetricsPerMember(destinations, pairs, dstFailureGroupMap, chosenPaths, false);

        Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberMetricsMap = new HashMap<>();
        memberMetricsMap.put(MemberType.Source, metricsBySource);
        memberMetricsMap.put(MemberType.Destination, metricsByDest);
        return memberMetricsMap;
    }

    private Map<Node, Map<SourceDestPair, PathSetMetrics>> getPathSetMetricsPerMember(Set<Node> members, Set<SourceDestPair> pairs,
                                                                Map<Node, List<List<Failure>>> srcFailureGroupsMap,
                                                                Map<SourceDestPair, Map<String, Path>> chosenPaths, boolean bySource) {
        Map<Node, Set<SourceDestPair>> pairsByMemberMap = getPairsByMember(pairs, bySource);
        Map<Node, Map<SourceDestPair, PathSetMetrics>> metricsMap = new HashMap<>();
        // Must analyze number of paths based on source/dest
        // Pick worst case failure group per member
        for(Node member : members){
            List<List<Failure>> failureGroups = srcFailureGroupsMap.get(member);
            Set<SourceDestPair> pairsWithSrc = pairsByMemberMap.get(member);
            // This gives us the metrics for the worst-case failure group
            Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap = analyzeAllPaths(chosenPaths, pairsWithSrc, failureGroups);
            metricsMap.put(member, pathSetMetricsMap);
        }
        return metricsMap;
    }

    private Map<Node, Set<SourceDestPair>> getPairsByMember(Set<SourceDestPair> pairs, boolean bySource) {
        Map<Node, Set<SourceDestPair>> pairMap = new HashMap<>();
        for(SourceDestPair pair : pairs){
            Node member = bySource ? pair.getSrc() : pair.getDst();
            pairMap.putIfAbsent(member, new HashSet<>());
            pairMap.get(member).add(pair);
        }
        return pairMap;
    }

    private Map<SourceDestPair, PathSetMetrics> analyzeAllPaths(Map<SourceDestPair, Map<String, Path>> chosenPaths, Set<SourceDestPair> pairs,
                                 List<List<Failure>> failureGroups) {
        Map<SourceDestPair, PathSetMetrics> worstCasePathSetMap = new HashMap<>();
        Integer worstNumFailed = 0;

        for(List<Failure> failureGroup : failureGroups){
            Integer numFailedTemp = 0;
            Map<SourceDestPair, PathSetMetrics> tempMap = new HashMap<>();
            for(SourceDestPair pair : pairs){
                Map<String, Path> pathMap = chosenPaths.get(pair);

                PathSetMetrics pathSetMetrics = getMetricsForPathSet(pathMap, failureGroup);

                numFailedTemp += pathSetMetrics.getNumFailed();
                tempMap.put(pair, pathSetMetrics);
            }
            if(numFailedTemp > worstNumFailed){
                worstCasePathSetMap = new HashMap<>(tempMap);
                worstNumFailed = numFailedTemp;
            }
        }
        if(worstCasePathSetMap.isEmpty()){
            for(SourceDestPair pair : pairs){
                Map<String, Path> pathMap = chosenPaths.get(pair);
                PathSetMetrics pathSetMetrics = getMetricsForPathSet(pathMap, new ArrayList<>());
                worstNumFailed += pathSetMetrics.getNumFailed();
                worstCasePathSetMap.put(pair, pathSetMetrics);
            }
        }

        return worstCasePathSetMap;
    }

    private PathSetMetrics getWorstCasePathSetMetrics(Map<String, Path> pathMap, List<List<Failure>> failureGroups){
        PathSetMetrics worstPathSetMetrics = null;
        for(List<Failure> group : failureGroups){
            PathSetMetrics pathSetMetrics = getMetricsForPathSet(pathMap, group);
            if(worstPathSetMetrics == null || worstPathSetMetrics.getNumFailed() < pathSetMetrics.getNumFailed()){
                worstPathSetMetrics = pathSetMetrics;
            }
        }
        if(worstPathSetMetrics == null){
            worstPathSetMetrics = getMetricsForPathSet(pathMap, new ArrayList<>());
        }
        return worstPathSetMetrics;
    }

    private PathSetMetrics getMetricsForPathSet(Map<String, Path> pathMap, List<Failure> group){
        Map<String, PathMetrics> pathMetrics = getPathMetrics(pathMap, group);
        return analyzePathMetrics(pathMetrics);
    }

    private Map<String, PathMetrics> getPathMetrics(Map<String, Path> pathMap, List<Failure> failures) {
        Map<String, PathMetrics> pathMetricsMap = new HashMap<>();
        for(String pathId : pathMap.keySet()){
            Path path = pathMap.get(pathId);
            Integer numLinks = path.getLinks().size();
            Boolean survived = testSurvival(path, failures);
            PathMetrics metrics = PathMetrics.builder().numLinks(numLinks).survived(survived).build();
            pathMetricsMap.put(pathId, metrics);
        }
        return pathMetricsMap;
    }

    private Boolean testSurvival(Path path, List<Failure> failures) {
        Set<String> nodeIds = path.getNodeIds();
        Set<String> linkIds = path.getLinkIds();

        for(Failure failure : failures){
            if(failure.getNode() != null){
                if(nodeIds.contains(failure.getNode().getId())){
                    return false;
                }
            }
            if(failure.getLink() != null){
                if(linkIds.contains(failure.getLink().getId()) || linkIds.contains(invertLinkId(failure.getLink()))){
                    return false;
                }
            }
        }
        return true;
    }

    private String invertLinkId(Link link) {
        return link.getTarget().getId() + "-" + link.getOrigin().getId();
    }

    private PathSetMetrics analyzePathMetrics(Map<String, PathMetrics> pathMetricsMap) {

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

        return PathSetMetrics.builder()
                .pathMetricsMap(pathMetricsMap)
                .numLinkUsages(numLinkUsages)
                .numFailed(numFailed)
                .numPaths(numPaths)
                .build();
    }


}
