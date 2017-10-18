package netlab.analysis.services;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.*;
import netlab.analysis.enums.MemberType;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.*;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalysisService {

    private HashingService hashingService;
    private TopologyService topologyService;

    @Autowired
    public AnalysisService(HashingService hashingService, TopologyService topologyService) {
        this.hashingService = hashingService;
        this.topologyService = topologyService;
    }

    public Analysis analyzeRequestSet(Request request) {

        Details details = request.getDetails();
        RequestMetrics requestMetrics = generateMetrics(details, request.getProblemClass());
        return generateAnalyzedSet(request, requestMetrics);
    }

    private Analysis generateAnalyzedSet(Request request, RequestMetrics requestMetrics) {

        return Analysis.builder()
                .requestSetId(request.getId())
                .seed(request.getSeed())
                .problemClass(request.getProblemClass())
                .algorithm(request.getAlgorithm())
                .objective(request.getObjective())
                .failureClass(request.getFailureClass())
                .requestMetrics(requestMetrics)
                .build();
    }


    private RequestMetrics generateMetrics(Details details, ProblemClass problemClass) {
        Map<SourceDestPair, Map<String, Path>> chosenPaths = details.getChosenPaths();
        Set<SourceDestPair> pairs = details.getPairs();
        Set<Node> sources = details.getSources();
        Set<Node> destinations = details.getDestinations();
        Connections connectionColl = details.getConnections();
        Failures failureColl = details.getFailures();
        Integer numConnections = connectionColl.getNumConnections();

        Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap = new HashMap<>();
        Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap = new HashMap<>();
        Boolean requestIsSurvivable = true;
        Integer numLinkUsages = 0;
        Integer numFailed = 0;
        Integer numPaths = 0;
        Long totalPathCost = 0L;

        // Check the high-level attributes
        if (problemClass.equals(ProblemClass.Flex) || problemClass.equals(ProblemClass.EndpointSharedF)
                || problemClass.equals(ProblemClass.FlowSharedF) || problemClass.equals(ProblemClass.Combined)) {
            List<List<Failure>> failureGroups = failureColl.getFailureGroups();
            Map<Node, List<List<Failure>>> srcFailureGroupsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> failureGroups));
            Map<Node, List<List<Failure>>> dstFailureGroupsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> failureGroups));

            // Must analyze paths based on total paths - choose just one failure group for everybody
            pathSetMetricsMap = analyzeAllPaths(chosenPaths, pairs, failureGroups);
            for (PathSetMetrics psm : pathSetMetricsMap.values()) {
                numLinkUsages += psm.getNumLinkUsages();
                numFailed += psm.getNumFailed();
                numPaths += psm.getNumPaths();
                totalPathCost += psm.getTotalLinkCost();
            }
            memberPathSetMetricsMap = analyzeAllPathsForEndpoints(chosenPaths, sources, destinations, pairs, srcFailureGroupsMap, dstFailureGroupsMap);
        }
        // Check the flow-level attributes
        if (problemClass.equals(ProblemClass.Flow)) {
            Map<SourceDestPair, Integer> minNumConnectionsMap = connectionColl.getPairMinConnectionsMap();
            Map<SourceDestPair, List<List<Failure>>> failuresMap = failureColl.getPairFailureGroupsMap();
            Map<Node, List<List<Failure>>> srcFailureGroupsMap = new HashMap<>();
            Map<Node, List<List<Failure>>> dstFailureGroupsMap = new HashMap<>();
            // Must analyze number of paths based on pairs
            for (SourceDestPair pair : pairs) {
                Map<String, Path> pathMap = chosenPaths.get(pair);
                numPaths += pathMap.values().size();

                Integer minConn = minNumConnectionsMap.get(pair);

                List<List<Failure>> failureGroups = failuresMap.get(pair);
                srcFailureGroupsMap.putIfAbsent(pair.getSrc(), new ArrayList<>());
                srcFailureGroupsMap.get(pair.getSrc()).addAll(failureGroups);
                dstFailureGroupsMap.putIfAbsent(pair.getDst(), new ArrayList<>());
                dstFailureGroupsMap.get(pair.getDst()).addAll(failureGroups);

                PathSetMetrics pathSetMetrics = getWorstCasePathSetMetrics(pathMap, failureGroups);
                numLinkUsages += pathSetMetrics.getNumLinkUsages();
                numFailed += pathSetMetrics.getNumFailed();
                totalPathCost += pathSetMetrics.getTotalLinkCost();
                pathSetMetricsMap.put(pair, pathSetMetrics);

                if (pathMap.values().size() - pathSetMetrics.getNumFailed() < minConn) {
                    requestIsSurvivable = false;
                }
            }
            memberPathSetMetricsMap = analyzeAllPathsForEndpoints(chosenPaths, sources, destinations, pairs, srcFailureGroupsMap, dstFailureGroupsMap);
        }
        if (problemClass.equals(ProblemClass.Endpoint)) {
            Map<Node, List<List<Failure>>> srcFailureGroupsMap = failureColl.getSrcFailureGroupsMap();
            Map<Node, List<List<Failure>>> dstFailureGroupMap = failureColl.getDstFailureGroupsMap();
            memberPathSetMetricsMap = analyzeAllPathsForEndpoints(chosenPaths, sources, destinations, pairs, srcFailureGroupsMap, dstFailureGroupMap);
            List<Path> paths = chosenPaths.values().stream().map(Map::values).flatMap(Collection::stream).collect(Collectors.toList());
            numPaths = paths.size();
            numLinkUsages = paths.stream().mapToInt(p -> p.getLinks().size()).sum();
            totalPathCost += paths.stream().map(Path::getLinks).flatMap(Collection::stream).mapToLong(Link::getWeight).sum();
            numFailed = determineMaxNumFailedAcrossMemberTypes(memberPathSetMetricsMap);

            // Get metrics for pairs
            for (SourceDestPair pair : pairs) {
                List<List<Failure>> failureGroups = srcFailureGroupsMap.get(pair.getSrc());
                failureGroups.addAll(dstFailureGroupMap.get(pair.getDst()));
                PathSetMetrics psm = getWorstCasePathSetMetrics(chosenPaths.get(pair), failureGroups);
                pathSetMetricsMap.put(pair, psm);
            }
        }

        if (numPaths - numFailed < numConnections) {
            requestIsSurvivable = false;
        }

        Double avgPathLength = numPaths > 0 ? numLinkUsages / numPaths * 1.0 : 0.0;
        Double avgPathCost = numPaths > 0 ? totalPathCost / numPaths * 1.0 : 0.0;
        Averages pairAverages = getAveragesForPairs(pairs, pathSetMetricsMap);
        Averages srcAverages = getAveragesForMember(sources, memberPathSetMetricsMap, true);
        Averages dstAverages = getAveragesForMember(destinations, memberPathSetMetricsMap, false);
        return RequestMetrics.builder()
                .requestId(details.getId())
                .isSurvivable(requestIsSurvivable)
                .isFeasible(details.getIsFeasible())
                .runningTimeSeconds(details.getRunningTimeSeconds())
                .numLinksUsed(numLinkUsages)
                .costLinksUsed(totalPathCost)
                .numPaths(numPaths)
                .numDisconnectedPaths(numFailed)
                .numIntactPaths(numPaths - numFailed)
                .avgPathLength(avgPathLength)
                .avgPathCost(avgPathCost)
                .averagesPerPair(pairAverages)
                .averagesPerSrc(srcAverages)
                .averagesPerDst(dstAverages)
                .pathSetMetricsMap(pathSetMetricsMap)
                .memberPathSetMetricsMap(memberPathSetMetricsMap)
                .build();
    }

    private Averages getAveragesForMember(Set<Node> members,
                                          Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap,
                                          boolean isSource) {
        Double totalPaths = 0.0;
        Double totalPathLength = 0.0;
        Double totalPathCost = 0.0;
        Double totalDisconnectedPaths = 0.0;
        Integer numChosen = 0;
        Map<Node, Map<SourceDestPair, PathSetMetrics>> memberMap = isSource ? memberPathSetMetricsMap.get(MemberType.Source) : memberPathSetMetricsMap.get(MemberType.Destination);
        for (Node member : members) {
            boolean isUsed = false;
            for (SourceDestPair pair : memberMap.get(member).keySet()) {
                PathSetMetrics psm = memberMap.get(member).get(pair);
                totalPaths += psm.getNumPaths();
                totalPathLength += psm.getNumLinkUsages();
                totalPathCost += psm.getTotalLinkCost();
                totalDisconnectedPaths += psm.getNumFailed();
                if (psm.getChosen()) {
                    isUsed = true;
                }
            }
            if (isUsed) {
                numChosen++;
            }
        }
        return Averages.builder()
                .avgPaths(totalPaths / members.size())
                .avgPathLength(totalPathLength / members.size())
                .avgPathCost(totalPathCost / members.size())
                .avgDisconnectedPaths(totalDisconnectedPaths / members.size())
                .avgPathsPerChosen(totalPaths / numChosen)
                .avgPathLengthPerChosen(totalPathLength / numChosen)
                .avgPathCostPerChosen(totalPathCost / numChosen)
                .avgDisconnectedPathsPerChosen(totalDisconnectedPaths / members.size())
                .forPair(false)
                .forSource(isSource)
                .forDest(!isSource)
                .build();
    }

    private Averages getAveragesForPairs(Set<SourceDestPair> pairs, Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap) {

        Double totalPaths = 0.0;
        Double totalPathLength = 0.0;
        Double totalPathCost = 0.0;
        Double totalDisconnectedPaths = 0.0;
        Integer numChosen = 0;
        for (SourceDestPair pair : pairs) {
            PathSetMetrics psm = pathSetMetricsMap.get(pair);
            totalPaths += psm.getNumPaths();
            totalPathLength += psm.getNumLinkUsages();
            totalPathCost += psm.getTotalLinkCost();
            totalDisconnectedPaths += psm.getNumFailed();
            if (psm.getChosen()) {
                numChosen++;
            }
        }
        return Averages.builder()
                .avgPaths(totalPaths / pairs.size())
                .avgPathLength(totalPathLength / pairs.size())
                .avgPathCost(totalPathCost / pairs.size())
                .avgDisconnectedPaths(totalDisconnectedPaths / pairs.size())
                .avgPathsPerChosen(numChosen > 0 ? totalPaths / numChosen : 0)
                .avgPathLengthPerChosen(numChosen > 0 ? totalPathLength / numChosen : 0)
                .avgPathCostPerChosen(numChosen > 0 ? totalPathCost / numChosen : 0)
                .avgDisconnectedPathsPerChosen(numChosen > 0 ? totalDisconnectedPaths / pairs.size() : 0)
                .forPair(true)
                .forSource(false)
                .forDest(false)
                .build();
    }

    private Integer determineMaxNumFailedAcrossMemberTypes(Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap) {
        Integer maxSum = 0;
        for (MemberType memberType : memberPathSetMetricsMap.keySet()) {
            Map<Node, Map<SourceDestPair, PathSetMetrics>> memberMetricsMap = memberPathSetMetricsMap.get(memberType);
            Integer memberSum = 0;
            for (Node node : memberMetricsMap.keySet()) {
                Map<SourceDestPair, PathSetMetrics> metricsMap = memberMetricsMap.get(node);
                memberSum += metricsMap.values().stream().mapToInt(PathSetMetrics::getNumFailed).sum();
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
        for (Node member : members) {
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
        for (SourceDestPair pair : pairs) {
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

        for (List<Failure> failureGroup : failureGroups) {
            Integer numFailedTemp = 0;
            Map<SourceDestPair, PathSetMetrics> tempMap = new HashMap<>();
            for (SourceDestPair pair : pairs) {
                Map<String, Path> pathMap = chosenPaths.get(pair);

                PathSetMetrics pathSetMetrics = getMetricsForPathSet(pathMap, failureGroup);

                numFailedTemp += pathSetMetrics.getNumFailed();
                tempMap.put(pair, pathSetMetrics);
            }
            if (numFailedTemp > worstNumFailed) {
                worstCasePathSetMap = new HashMap<>(tempMap);
                worstNumFailed = numFailedTemp;
            }
        }
        if (worstCasePathSetMap.isEmpty()) {
            for (SourceDestPair pair : pairs) {
                Map<String, Path> pathMap = chosenPaths.get(pair);
                PathSetMetrics pathSetMetrics = getMetricsForPathSet(pathMap, new ArrayList<>());
                worstNumFailed += pathSetMetrics.getNumFailed();
                worstCasePathSetMap.put(pair, pathSetMetrics);
            }
        }

        return worstCasePathSetMap;
    }

    private PathSetMetrics getWorstCasePathSetMetrics(Map<String, Path> pathMap, List<List<Failure>> failureGroups) {
        PathSetMetrics worstPathSetMetrics = null;
        for (List<Failure> group : failureGroups) {
            PathSetMetrics pathSetMetrics = getMetricsForPathSet(pathMap, group);
            if (worstPathSetMetrics == null || worstPathSetMetrics.getNumFailed() < pathSetMetrics.getNumFailed()) {
                worstPathSetMetrics = pathSetMetrics;
            }
        }
        if (worstPathSetMetrics == null) {
            worstPathSetMetrics = getMetricsForPathSet(pathMap, new ArrayList<>());
        }
        return worstPathSetMetrics;
    }

    private PathSetMetrics getMetricsForPathSet(Map<String, Path> pathMap, List<Failure> group) {
        Map<String, PathMetrics> pathMetrics = getPathMetrics(pathMap, group);
        return analyzePathMetrics(pathMetrics);
    }

    private Map<String, PathMetrics> getPathMetrics(Map<String, Path> pathMap, List<Failure> failures) {
        Map<String, PathMetrics> pathMetricsMap = new HashMap<>();
        for (String pathId : pathMap.keySet()) {
            Path path = pathMap.get(pathId);
            Integer numLinks = path.getLinks().size();
            Long totalCost = path.getLinks().stream().mapToLong(Link::getWeight).sum();
            Boolean survived = testSurvival(path, failures);
            PathMetrics metrics = PathMetrics.builder().numLinks(numLinks).survived(survived).cost(totalCost).build();
            pathMetricsMap.put(pathId, metrics);
        }
        return pathMetricsMap;
    }

    private Boolean testSurvival(Path path, List<Failure> failures) {
        Set<String> nodeIds = path.getNodeIds();
        Set<String> linkIds = path.getLinkIds();

        for (Failure failure : failures) {
            if (failure.getNode() != null) {
                if (nodeIds.contains(failure.getNode().getId())) {
                    return false;
                }
            }
            if (failure.getLink() != null) {
                if (linkIds.contains(failure.getLink().getId()) || linkIds.contains(invertLinkId(failure.getLink()))) {
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
        Long totalCost = 0L;
        Boolean chosen = false;
        if (!pathMetricsMap.isEmpty()) {
            chosen = true;
            for (PathMetrics pathMetrics : pathMetricsMap.values()) {
                numLinkUsages += pathMetrics.getNumLinks();
                if (!pathMetrics.getSurvived()) {
                    numFailed++;
                }
                numPaths++;
                totalCost += pathMetrics.getCost();
            }
        }

        return PathSetMetrics.builder()
                .pathMetricsMap(pathMetricsMap)
                .numLinkUsages(numLinkUsages)
                .numFailed(numFailed)
                .numPaths(numPaths)
                .totalLinkCost(totalCost)
                .chosen(chosen)
                .build();
    }


    public AggregateAnalysis aggregateAnalyzedSets(List<Analysis> analyses) {

        if (analyses.isEmpty()) {
            return AggregateAnalysis.builder().build();
        }

        List<String> requestSetIds = new ArrayList<>();
        List<Long> seeds = new ArrayList<>();
        ProblemClass problemClass = analyses.get(0).getProblemClass();
        Algorithm algorithm = analyses.get(0).getAlgorithm();
        Integer numRequests = analyses.get(0).getNumRequests();
        Objective objective = analyses.get(0).getObjective();
        FailureClass failureClass = analyses.get(0).getFailureClass();


        Double totalRunningTime = 0.0;
        Double totalRunningTimeSecondsForFeasible = 0.0;
        Double avgRunningTimeSeconds = 0.0;
        Double avgRunningTimeSecondsForFeasible = 0.0;
        Double totalSurvivable = 0.0;
        Double percentSurvivable = 0.0;
        Double percentSurvivableForFeasible = 0.0;
        Double totalFeasible = 0.0;
        Double percentFeasible = 0.0;
        Double totalFeasibleAndSurvivable = 0.0;
        Double totalLinksUsed = 0.0;
        Double avgLinksUsedForFeasible = 0.0;
        Double totalCostLinksUsed = 0.0;
        Double avgCostLinksUsedForFeasible = 0.0;
        Double totalNumPaths = 0.0;
        Double avgNumPathsForFeasible = 0.0;
        Double totalDisconnectedPaths = 0.0;
        Double avgDisconnectedPathsForFeasible = 0.0;
        Double totalIntactPaths = 0.0;
        Double avgIntactPathsForFeasible = 0.0;
        Double avgAvgPathLength = 0.0;
        Double avgAvgPathCost = 0.0;
        Double pairAvgPaths = 0.0;
        Double pairAvgPathLength = 0.0;
        Double pairAvgPathCost = 0.0;
        Double pairAvgDisconnectedPaths = 0.0;
        Double pairAvgPathsPerChosen = 0.0;
        Double pairAvgPathLengthPerChosen = 0.0;
        Double pairAvgPathCostPerChosen = 0.0;
        Double pairAvgDisconnectedPathsPerChosen = 0.0;
        Double srcAvgPaths = 0.0;
        Double srcAvgPathLength = 0.0;
        Double srcAvgPathCost = 0.0;
        Double srcAvgDisconnectedPaths = 0.0;
        Double srcAvgPathsPerChosen = 0.0;
        Double srcAvgPathLengthPerChosen = 0.0;
        Double srcAvgPathCostPerChosen = 0.0;
        Double srcAvgDisconnectedPathsPerChosen = 0.0;
        Double dstAvgPaths = 0.0;
        Double dstAvgPathLength = 0.0;
        Double dstAvgPathCost = 0.0;
        Double dstAvgDisconnectedPaths = 0.0;
        Double dstAvgPathsPerChosen = 0.0;
        Double dstAvgPathLengthPerChosen = 0.0;
        Double dstAvgPathCostPerChosen = 0.0;
        Double dstAvgDisconnectedPathsPerChosen = 0.0;

        int numSets = 0;
        for (Analysis analysis : analyses) {
            if (analysis == null){
                continue;
            }
            numSets++;
            requestSetIds.add(analysis.getRequestSetId());
            seeds.add(analysis.getSeed());
            totalRunningTime += analysis.getTotalRunningTimeSeconds();
            totalRunningTimeSecondsForFeasible += analysis.getTotalRunningTimeSecondsForFeasible();
            avgRunningTimeSeconds += analysis.getAvgRunningTimeSeconds();
            avgRunningTimeSecondsForFeasible += analysis.getAvgRunningTimeSecondsForFeasible();
            totalSurvivable += analysis.getTotalSurvivable();
            percentSurvivable += analysis.getPercentSurvivable();
            percentSurvivableForFeasible += analysis.getPercentSurvivableForFeasible();
            totalFeasible += analysis.getTotalFeasible();
            percentFeasible += analysis.getPercentFeasible();
            totalFeasibleAndSurvivable += analysis.getTotalFeasibleAndSurvivable();
            totalLinksUsed += analysis.getTotalLinksUsed();
            avgLinksUsedForFeasible += analysis.getAvgLinksUsedForFeasible();
            totalCostLinksUsed += analysis.getTotalCostLinksUsed();
            avgCostLinksUsedForFeasible += analysis.getAvgCostLinksUsedForFeasible();
            totalNumPaths += analysis.getTotalNumPaths();
            avgNumPathsForFeasible += analysis.getAvgNumPathsForFeasible();
            totalDisconnectedPaths += analysis.getTotalDisconnectedPaths();
            avgDisconnectedPathsForFeasible += analysis.getAvgDisconnectedPathsForFeasible();
            totalIntactPaths += analysis.getTotalIntactPaths();
            avgIntactPathsForFeasible += analysis.getAvgIntactPathsForFeasible();
            avgAvgPathLength += analysis.getAvgAvgPathLength();
            avgAvgPathCost += analysis.getAvgAvgPathCost();
            pairAvgPaths += analysis.getPairAvgPaths();
            pairAvgPathLength = analysis.getPairAvgPathLength();
            pairAvgPathCost = analysis.getPairAvgPathCost();
            pairAvgDisconnectedPaths = analysis.getPairAvgDisconnectedPaths();
            pairAvgPathsPerChosen = analysis.getPairAvgPathsPerChosen();
            pairAvgPathLengthPerChosen = analysis.getPairAvgPathLengthPerChosen();
            pairAvgPathCostPerChosen = analysis.getPairAvgPathCostPerChosen();
            pairAvgDisconnectedPathsPerChosen = analysis.getPairAvgDisconnectedPathsPerChosen();
            srcAvgPaths = analysis.getSrcAvgPaths();
            srcAvgPathLength = analysis.getSrcAvgPathLength();
            srcAvgPathCost = analysis.getSrcAvgPathCost();
            srcAvgDisconnectedPaths = analysis.getSrcAvgDisconnectedPaths();
            srcAvgPathsPerChosen = analysis.getSrcAvgPathsPerChosen();
            srcAvgPathLengthPerChosen = analysis.getSrcAvgPathLengthPerChosen();
            srcAvgPathCostPerChosen = analysis.getSrcAvgPathCostPerChosen();
            srcAvgDisconnectedPathsPerChosen = analysis.getSrcAvgDisconnectedPathsPerChosen();
            dstAvgPaths = analysis.getDstAvgPaths();
            dstAvgPathLength = analysis.getDstAvgPathLength();
            dstAvgPathCost = analysis.getDstAvgPathCost();
            dstAvgDisconnectedPaths = analysis.getDstAvgDisconnectedPaths();
            dstAvgPathsPerChosen = analysis.getDstAvgPathsPerChosen();
            dstAvgPathLengthPerChosen = analysis.getDstAvgPathLengthPerChosen();
            dstAvgPathCostPerChosen = analysis.getDstAvgPathCostPerChosen();
            dstAvgDisconnectedPathsPerChosen = analysis.getDstAvgDisconnectedPathsPerChosen();
        }

        AggregateAnalysis aggregateAnalysis = AggregateAnalysis.builder()
                .requestSetIds(requestSetIds)
                .seeds(seeds)
                .problemClass(problemClass)
                .algorithm(algorithm)
                .objective(objective)
                .failureClass(failureClass)
                .numRequests(numRequests)
                .totalRunningTimeSeconds(totalRunningTime / numSets)
                .totalRunningTimeSecondsForFeasible(totalRunningTimeSecondsForFeasible / numSets)
                .avgRunningTimeSeconds(avgRunningTimeSeconds / numSets)
                .avgRunningTimeSecondsForFeasible(avgRunningTimeSecondsForFeasible / numSets)
                .totalSurvivable(totalSurvivable / numSets)
                .percentSurvivable(percentSurvivable / numSets)
                .percentSurvivableForFeasible(percentSurvivableForFeasible / numSets)
                .totalFeasible(totalFeasible / numSets)
                .percentFeasible(percentFeasible / numSets)
                .totalFeasibleAndSurvivable(totalFeasibleAndSurvivable / numSets)
                .totalLinksUsed(totalLinksUsed / numSets)
                .avgLinksUsedForFeasible(avgLinksUsedForFeasible / numSets)
                .totalCostLinksUsed(totalCostLinksUsed / numSets)
                .avgCostLinksUsedForFeasible(avgCostLinksUsedForFeasible / numSets)
                .totalNumPaths(totalNumPaths / numSets)
                .avgNumPathsForFeasible(avgNumPathsForFeasible / numSets)
                .totalDisconnectedPaths(totalDisconnectedPaths / numSets)
                .avgDisconnectedPathsForFeasible(avgDisconnectedPathsForFeasible / numSets)
                .totalIntactPaths(totalIntactPaths / numSets)
                .avgIntactPathsForFeasible(avgIntactPathsForFeasible / numSets)
                .avgAvgPathLength(avgAvgPathLength / numSets)
                .avgAvgPathCost(avgAvgPathCost / numSets)
                .pairAvgPaths(pairAvgPaths / numSets)
                .pairAvgPathLength(pairAvgPathLength / numSets)
                .pairAvgPathCost(pairAvgPathCost / numSets)
                .pairAvgDisconnectedPaths(pairAvgDisconnectedPaths / numSets)
                .pairAvgPathsPerChosen(pairAvgPathsPerChosen / numSets)
                .pairAvgPathLengthPerChosen(pairAvgPathLengthPerChosen / numSets)
                .pairAvgPathCostPerChosen(pairAvgPathCostPerChosen / numSets)
                .pairAvgDisconnectedPathsPerChosen(pairAvgDisconnectedPathsPerChosen / numSets)
                .srcAvgPaths(srcAvgPaths / numSets)
                .srcAvgPathLength(srcAvgPathLength / numSets)
                .srcAvgPathCost(srcAvgPathCost / numSets)
                .srcAvgDisconnectedPaths(srcAvgDisconnectedPaths / numSets)
                .srcAvgPathsPerChosen(srcAvgPathsPerChosen / numSets)
                .srcAvgPathLengthPerChosen(srcAvgPathLengthPerChosen / numSets)
                .srcAvgPathCostPerChosen(srcAvgPathCostPerChosen / numSets)
                .srcAvgDisconnectedPathsPerChosen(srcAvgDisconnectedPathsPerChosen / numSets)
                .dstAvgPaths(dstAvgPaths / numSets)
                .dstAvgPathLength(dstAvgPathLength / numSets)
                .dstAvgPathCost(dstAvgPathCost / numSets)
                .dstAvgDisconnectedPaths(dstAvgDisconnectedPaths / numSets)
                .dstAvgPathsPerChosen(dstAvgPathsPerChosen / numSets)
                .dstAvgPathLengthPerChosen(dstAvgPathLengthPerChosen / numSets)
                .dstAvgPathCostPerChosen(dstAvgPathCostPerChosen / numSets)
                .dstAvgDisconnectedPathsPerChosen(dstAvgDisconnectedPathsPerChosen / numSets)
                .build();

        return calculateConfidenceIntervals(aggregateAnalysis, analyses);
    }

    private AggregateAnalysis calculateConfidenceIntervals(AggregateAnalysis agAnSet, List<Analysis> analyses) {

        agAnSet.setTotalRunningTimeSecondsConfInterval(calcConfInterval(agAnSet.getTotalRunningTimeSeconds(), analyses, "totalRunningTimeSeconds"));
        agAnSet.setTotalRunningTimeSecondsForFeasibleConfInterval(calcConfInterval(agAnSet.getTotalRunningTimeSecondsForFeasible(), analyses, "totalRunningTimeSecondsForFeasible"));
        agAnSet.setAvgRunningTimeSecondsConfInterval(calcConfInterval(agAnSet.getAvgRunningTimeSeconds(), analyses, "avgRunningTimeSeconds"));
        agAnSet.setAvgRunningTimeSecondsForFeasibleConfInterval(calcConfInterval(agAnSet.getAvgRunningTimeSecondsForFeasible(), analyses, "avgRunningTimeSecondsForFeasible"));
        agAnSet.setTotalSurvivableConfInterval(calcConfInterval(agAnSet.getTotalSurvivable(), analyses, "totalSurvivable"));
        agAnSet.setPercentSurvivableConfInterval(calcConfInterval(agAnSet.getPercentSurvivable(), analyses, "percentSurvivable"));
        agAnSet.setPercentSurvivableForFeasibleConfInterval(calcConfInterval(agAnSet.getPercentSurvivableForFeasible(), analyses, "percentSurvivableForFeasible"));
        agAnSet.setTotalFeasibleConfInterval(calcConfInterval(agAnSet.getTotalFeasible(), analyses, "totalFeasible"));
        agAnSet.setPercentFeasibleConfInterval(calcConfInterval(agAnSet.getPercentFeasible(), analyses, "percentFeasible"));
        agAnSet.setTotalFeasibleAndSurvivableConfInterval(calcConfInterval(agAnSet.getTotalFeasibleAndSurvivable(), analyses, "totalFeasibleAndSurvivable"));
        agAnSet.setTotalLinksUsedConfInterval(calcConfInterval(agAnSet.getTotalLinksUsed(), analyses, "totalLinksUsed"));
        agAnSet.setAvgLinksUsedForFeasibleConfInterval(calcConfInterval(agAnSet.getAvgLinksUsedForFeasible(), analyses, "avgLinksUsedForFeasible"));
        agAnSet.setTotalCostLinksUsedConfInterval(calcConfInterval(agAnSet.getTotalCostLinksUsed(), analyses, "totalCostLinksUsed"));
        agAnSet.setAvgCostLinksUsedForFeasibleConfInterval(calcConfInterval(agAnSet.getAvgCostLinksUsedForFeasible(), analyses, "avgCostLinksUsedForFeasible"));
        agAnSet.setTotalNumPathsConfInterval(calcConfInterval(agAnSet.getTotalNumPaths(), analyses, "totalNumPaths"));
        agAnSet.setAvgNumPathsForFeasibleConfInterval(calcConfInterval(agAnSet.getAvgNumPathsForFeasible(), analyses, "avgNumPathsForFeasible"));
        agAnSet.setTotalDisconnectedPathsConfInterval(calcConfInterval(agAnSet.getTotalDisconnectedPaths(), analyses, "totalDisconnectedPaths"));
        agAnSet.setAvgDisconnectedPathsForFeasibleConfInterval(calcConfInterval(agAnSet.getAvgDisconnectedPathsForFeasible(), analyses, "avgDisconnectedPathsForFeasible"));
        agAnSet.setTotalIntactPathsConfInterval(calcConfInterval(agAnSet.getTotalIntactPaths(), analyses, "totalIntactPaths"));
        agAnSet.setAvgIntactPathsForFeasibleConfInterval(calcConfInterval(agAnSet.getAvgIntactPathsForFeasible(), analyses, "avgIntactPathsForFeasible"));
        agAnSet.setAvgAvgPathLengthConfInterval(calcConfInterval(agAnSet.getAvgAvgPathLength(), analyses, "avgAvgPathLength"));
        agAnSet.setAvgAvgPathCostConfInterval(calcConfInterval(agAnSet.getAvgAvgPathCost(), analyses, "avgAvgPathCost"));

        agAnSet.setPairAvgPathsConfInterval(calcConfInterval(agAnSet.getPairAvgPaths(), analyses, "pairAvgPaths"));
        agAnSet.setPairAvgPathLengthConfInterval(calcConfInterval(agAnSet.getPairAvgPathLength(), analyses, "pairAvgPathLength"));
        agAnSet.setPairAvgPathCostConfInterval(calcConfInterval(agAnSet.getPairAvgPathCost(), analyses, "pairAvgPathCost"));
        agAnSet.setPairAvgDisconnectedPathsConfInterval(calcConfInterval(agAnSet.getPairAvgDisconnectedPaths(), analyses, "pairAvgDisconnectedPaths"));
        agAnSet.setPairAvgPathsPerChosenConfInterval(calcConfInterval(agAnSet.getPairAvgPathsPerChosen(), analyses, "pairAvgPathsPerChosen"));
        agAnSet.setPairAvgPathLengthPerChosenConfInterval(calcConfInterval(agAnSet.getPairAvgPathLengthPerChosen(), analyses, "pairAvgPathLengthPerChosen"));
        agAnSet.setPairAvgPathCostPerChosenConfInterval(calcConfInterval(agAnSet.getPairAvgPathCostPerChosen(), analyses, "pairAvgPathCostPerChosen"));
        agAnSet.setPairAvgDisconnectedPathsPerChosenConfInterval(calcConfInterval(agAnSet.getPairAvgDisconnectedPathsPerChosen(), analyses, "pairAvgDisconnectedPathsPerChosen"));

        agAnSet.setSrcAvgPathsConfInterval(calcConfInterval(agAnSet.getSrcAvgPaths(), analyses, "srcAvgPaths"));
        agAnSet.setSrcAvgPathLengthConfInterval(calcConfInterval(agAnSet.getSrcAvgPathLength(), analyses, "srcAvgPathLength"));
        agAnSet.setSrcAvgPathCostConfInterval(calcConfInterval(agAnSet.getSrcAvgPathCost(), analyses, "srcAvgPathCost"));
        agAnSet.setSrcAvgDisconnectedPathsConfInterval(calcConfInterval(agAnSet.getSrcAvgDisconnectedPaths(), analyses, "srcAvgDisconnectedPaths"));
        agAnSet.setSrcAvgPathsPerChosenConfInterval(calcConfInterval(agAnSet.getSrcAvgPathsPerChosen(), analyses, "srcAvgPathsPerChosen"));
        agAnSet.setSrcAvgPathLengthPerChosenConfInterval(calcConfInterval(agAnSet.getSrcAvgPathLengthPerChosen(), analyses, "srcAvgPathLengthPerChosen"));
        agAnSet.setSrcAvgPathCostPerChosenConfInterval(calcConfInterval(agAnSet.getSrcAvgPathCostPerChosen(), analyses, "srcAvgPathCostPerChosen"));
        agAnSet.setSrcAvgDisconnectedPathsPerChosenConfInterval(calcConfInterval(agAnSet.getSrcAvgDisconnectedPathsPerChosen(), analyses, "srcAvgDisconnectedPathsPerChosen"));

        agAnSet.setDstAvgPathsConfInterval(calcConfInterval(agAnSet.getDstAvgPaths(), analyses, "dstAvgPaths"));
        agAnSet.setDstAvgPathLengthConfInterval(calcConfInterval(agAnSet.getDstAvgPathLength(), analyses, "dstAvgPathLength"));
        agAnSet.setDstAvgPathCostConfInterval(calcConfInterval(agAnSet.getDstAvgPathCost(), analyses, "dstAvgPathCost"));
        agAnSet.setDstAvgDisconnectedPathsConfInterval(calcConfInterval(agAnSet.getDstAvgDisconnectedPaths(), analyses, "dstAvgDisconnectedPaths"));
        agAnSet.setDstAvgPathsPerChosenConfInterval(calcConfInterval(agAnSet.getDstAvgPathsPerChosen(), analyses, "dstAvgPathsPerChosen"));
        agAnSet.setDstAvgPathLengthPerChosenConfInterval(calcConfInterval(agAnSet.getDstAvgPathLengthPerChosen(), analyses, "dstAvgPathLengthPerChosen"));
        agAnSet.setDstAvgPathCostPerChosenConfInterval(calcConfInterval(agAnSet.getDstAvgPathCostPerChosen(), analyses, "dstAvgPathCostPerChosen"));
        agAnSet.setDstAvgDisconnectedPathsPerChosenConfInterval(calcConfInterval(agAnSet.getDstAvgDisconnectedPathsPerChosen(), analyses, "dstAvgDisconnectedPathsPerChosen"));
        return agAnSet;
    }


    private List<Double> calcConfInterval(Double metricMean, List<Analysis> analyses, String fieldName) {
        List<Double> confInterval = new ArrayList<>();
        Double squaredDifferenceSum = 0.0;
        int numSets = 0;
        for (Analysis as : analyses) {
            if(as == null){
                continue;
            }
            numSets++;
            try {
                Double metricValue = Double.valueOf(new PropertyDescriptor(fieldName, Analysis.class).getReadMethod().invoke(as).toString());
                squaredDifferenceSum += (metricValue - metricMean) * (metricValue - metricMean);
            } catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
                e.printStackTrace();
            }
        }
        Double variance = squaredDifferenceSum / numSets;
        Double stdDev = Math.sqrt(variance);
        Double confDist = 1.96 * stdDev / Math.sqrt(numSets);
        confInterval.add(metricMean - confDist);
        confInterval.add(metricMean + confDist);
        return confInterval;
    }

    public String aggregateSeeds(AggregationParameters agParams, List<SimulationParameters> baseParams,
                                 List<AggregateAnalysis> aggregateSets) {
        Map<String, AggregateAnalysis> outputMap = new HashMap<>();
        for (int index = 0; index < baseParams.size(); index++) {
            SimulationParameters params = baseParams.get(index);
            AggregateAnalysis aggSet = aggregateSets.get(index);
            String hashString = makeHash(params);
            outputMap.putIfAbsent(hashString, aggSet);
        }
        List<String[]> aggregationOutput = createAggregationOutput(agParams, outputMap);
        try {
            CSVWriter writer = new CSVWriter(new FileWriter("Output.csv"), ',');
            writer.writeAll(aggregationOutput);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String makeHash(SimulationParameters p) {
        return hashingService.hash(p.getTopologyId(), p.getAlgorithm(), p.getProblemClass(), p.getObjective(),
                String.valueOf(p.getPercentSrcAlsoDest()), p.getFailureClass(), String.valueOf(p.getFailureSetSize()),
                String.valueOf(p.getNumFailsAllowed()), String.valueOf(p.getPercentSrcFail()),
                String.valueOf(p.getPercentDestFail()), String.valueOf(p.getIgnoreFailures()), String.valueOf(p.getNumConnections()),
                String.valueOf(p.getMinConnectionsRange()), String.valueOf(p.getMaxConnectionsRange()),
                String.valueOf(p.getNumSources()), String.valueOf(p.getNumDestinations()));
    }


    private List<String[]> createAggregationOutput(AggregationParameters agParams, Map<String, AggregateAnalysis> outputMap) {
        List<String[]> outputLines = new ArrayList<>();
        int numPossible = 0;

        int lineNum = 1;
        for (Boolean ignoreFailures : agParams.getIgnoreFailures()) {
            outputLines.add(new String[]{"IGNORE F: " + ignoreFailures});
            lineNum++;
            for (String topology : agParams.getTopologyIds()) {
                outputLines.add(new String[]{"TOPOLOGY: " + topology});
                lineNum++;
                for (String algorithm : agParams.getAlgorithms()) {
                    outputLines.add(new String[]{"ALGORITHM: " + algorithm});
                    lineNum++;
                    for (String problemClass : agParams.getProblemClasses()) {
                        outputLines.add(new String[]{"PROBLEM CLASS: " + problemClass});
                        lineNum++;
                        for (String objective : agParams.getObjectives()) {
                            outputLines.add(new String[]{"OBJECTIVE: " + objective});
                            lineNum++;
                            for (Double percentSrcAlsoDest : agParams.getPercentSrcAlsoDests()) {
                                outputLines.add(new String[]{"SRC/DEST OVERLAP: " + percentSrcAlsoDest});
                                lineNum++;
                                List<String> failureClasses = agParams.getFailureMap().keySet().stream().sorted().collect(Collectors.toList());
                                for (String failureClass : failureClasses) {
                                    outputLines.add(new String[]{"FAILURE TYPE: " + failureClass});
                                    lineNum++;
                                    List<List<Double>> allParamsPerClass = agParams.getFailureMap().get(failureClass);
                                    for (List<Double> failureParams : allParamsPerClass) {
                                        Integer numFails = (int) Math.round(failureParams.get(0));
                                        Integer numFailsAllowed = (int) Math.round(failureParams.get(1));
                                        Double srcFailPercent = failureParams.get(2);
                                        Double dstFailPercent = failureParams.get(3);
                                        String failParam = "FSETSIZE: " + numFails + " NFA: " + numFailsAllowed;
                                        failParam += " SFAIL%: " + srcFailPercent + " DFAIL%: " + dstFailPercent;
                                        //outputLines.add(new String[]{failParam});
                                        for (Integer numC : agParams.getNumConnections()) {
                                            //outputLines.add(new String[]{"NUM C: " + numC});
                                            for (List<Integer> minC : agParams.getMinConnectionRanges()) {
                                                //outputLines.add(new String[]{"MINC: " + minC});
                                                for (List<Integer> maxC : agParams.getMaxConnectionRanges()) {
                                                    //outputLines.add(new String[]{"MAXC: " + maxC});
                                                    for (Integer numS : agParams.getNumSources()) {
                                                        //outputLines.add(generateAggregateHeadingLine(numS));
                                                        for (Integer numD : agParams.getNumDestinations()) {
                                                            if (!checkIfPossible(topology, algorithm, problemClass, objective,
                                                                    percentSrcAlsoDest, failureClass, numFails, numFailsAllowed,
                                                                    srcFailPercent, dstFailPercent, ignoreFailures, numC,
                                                                    minC, maxC, numS, numD)) {
                                                                continue;
                                                            }
                                                            numPossible++;
                                                            outputLines.add(new String[]{failParam});
                                                            lineNum++;
                                                            outputLines.add(new String[]{"NUM C: " + numC});
                                                            lineNum++;
                                                            outputLines.add(new String[]{"MINC: " + minC});
                                                            lineNum++;
                                                            outputLines.add(new String[]{"MAXC: " + maxC});
                                                            lineNum++;
                                                            outputLines.add(generateAggregateHeadingLine(numS));
                                                            lineNum++;
                                                            String hashString = hashingService.hash(topology,
                                                                    algorithm, problemClass, objective,
                                                                    String.valueOf(percentSrcAlsoDest), failureClass,
                                                                    String.valueOf(numFails),
                                                                    String.valueOf(numFailsAllowed), String.valueOf(srcFailPercent),
                                                                    String.valueOf(dstFailPercent), String.valueOf(ignoreFailures),
                                                                    String.valueOf(numC),
                                                                    String.valueOf(minC), String.valueOf(maxC),
                                                                    String.valueOf(numS), String.valueOf(numD));
                                                            AggregateAnalysis agSet = outputMap.getOrDefault(hashString, null);
                                                            outputLines.add(generateAggregateMetricLine(numD, agSet));
                                                            lineNum++;
                                                            System.out.println(hashString + ": " + lineNum);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        log.info("Possible combinations: " + numPossible);
        return outputLines;
    }

    private boolean checkIfPossible(String topology, String algorithm, String problemClass, String objective,
                                    Double percentSrcAlsoDest, String failureClass, Integer numFails,
                                    Integer numFailsAllowed, Double srcFailPercent, Double dstFailPercent,
                                    Boolean ignoreFailures, Integer numC, List<Integer> minC, List<Integer> maxC,
                                    Integer numS, Integer numD) {
        if(!Objects.equals(numS, numD) || (percentSrcAlsoDest == 1.0 && numS == 7)){
            return false;
        }
        int numSInD = (int) Math.ceil(percentSrcAlsoDest * numS);
        int exclusiveS = numS - numSInD;
        boolean completeOverlap = numSInD == numD && exclusiveS == 0;
        if (numSInD > numD || (completeOverlap && numD == 1) || (topologyService.getTopologyById(topology).getNodes().size() - exclusiveS < numD)) {
            return false;
        }
        if (failureClass.equals("Node")) {
            int numSFail = (int) Math.ceil(srcFailPercent * numS);
            int numDFail = (int) Math.ceil(dstFailPercent * numD);
            if (numSFail > numFails || numDFail > numFails || (numSFail - numSInD + numDFail > numFails))
                return false;
        }
        if (minC.isEmpty() || maxC.isEmpty()) {
            if (!problemClass.equals("Flex") || !(minC.isEmpty() && maxC.isEmpty())) {
                return false;
            }
        }
        else{
            if(problemClass.equals("Flex")){
                return false;
            }
        }
        return true;
    }


    private String[] generateAggregateHeadingLine(Integer numS) {
        return makeArray(
                "NUMS: " + numS,
                "AvgRunTime",
                "AvgRunTime-0",
                "AvgRunTime-1",
                "AvgRunTimeFeas",
                "AvgRunTimeFeas-0",
                "AvgRunTimeFeas-1",
                "%Feas",
                "%Feas-0",
                "%Feas-1",
                /*"%Surv",
                "%Surv-0",
                "%Surv-1",*/
                "%SurvFeas",
                "%SurvFeas-0",
                "%SurvFeas-1",
                /*"AvgLinksUsed",
                "AvgLinksUsed-0",
                "AvgLinksUsed-1",*/
                "AvgCostLinksUsed",
                "AvgCostLinksUsed-0",
                "AvgCostLinksUsed-1",
                "AvgNumPaths",
                "AvgNumPaths-0",
                "AvgNumPaths-1",
                "AvgPairNumPaths",
                "AvgPairNumPaths-0",
                "AvgPairNumPaths-1",
                "AvgSrcNumPaths",
                "AvgSrcNumPaths-0",
                "AvgSrcNumPaths-1",
                "AvgDstNumPaths",
                "AvgDstNumPaths-0",
                "AvgDstNumPaths-1",
                /*"AvgIntPaths",
                "AvgIntPaths-0",
                "AvgIntPaths-1",
                "AvgDisPaths",
                "AvgDisPaths-0",
                "AvgDisPaths-1",*/
                "AvgPathLen",
                "AvgPathLen-0",
                "AvgPathLen-1",
                "AvgPathCost",
                "AvgPathCost-0",
                "AvgPathCost-1"
        );
    }

    private String[] generateAggregateMetricLine(Integer numD, AggregateAnalysis agSet) {
        if (agSet == null) {
            return new String[]{"NUMD: " + String.valueOf(numD)};
        } else {
            return makeArray("NUMD: " + String.valueOf(numD),
                    agSet.getAvgRunningTimeSeconds(),
                    agSet.getAvgRunningTimeSecondsConfInterval().get(0),
                    agSet.getAvgRunningTimeSecondsConfInterval().get(1),
                    agSet.getAvgRunningTimeSecondsForFeasible(),
                    agSet.getAvgRunningTimeSecondsForFeasibleConfInterval().get(0),
                    agSet.getAvgRunningTimeSecondsForFeasibleConfInterval().get(1),
                    agSet.getPercentFeasible(),
                    agSet.getPercentFeasibleConfInterval().get(0),
                    agSet.getPercentFeasibleConfInterval().get(1),
                    /*agSet.getPercentSurvivable(),
                    agSet.getPercentSurvivableConfInterval().get(0),
                    agSet.getPercentSurvivableConfInterval().get(1),*/
                    agSet.getPercentSurvivableForFeasible(),
                    agSet.getPercentSurvivableForFeasibleConfInterval().get(0),
                    agSet.getPercentSurvivableForFeasibleConfInterval().get(1),
                    /*agSet.getAvgLinksUsedForFeasible(),
                    agSet.getAvgLinksUsedForFeasibleConfInterval().get(0),
                    agSet.getAvgLinksUsedForFeasibleConfInterval().get(1),*/
                    agSet.getAvgCostLinksUsedForFeasible(),
                    agSet.getAvgCostLinksUsedForFeasibleConfInterval().get(0),
                    agSet.getAvgCostLinksUsedForFeasibleConfInterval().get(1),
                    agSet.getAvgNumPathsForFeasible(),
                    agSet.getAvgNumPathsForFeasibleConfInterval().get(0),
                    agSet.getAvgNumPathsForFeasibleConfInterval().get(1),
                    agSet.getPairAvgPaths(),
                    agSet.getPairAvgPathsConfInterval().get(0),
                    agSet.getPairAvgPathsConfInterval().get(1),
                    agSet.getSrcAvgPaths(),
                    agSet.getSrcAvgPathsConfInterval().get(0),
                    agSet.getSrcAvgPathsConfInterval().get(1),
                    agSet.getDstAvgPaths(),
                    agSet.getDstAvgPathsConfInterval().get(0),
                    agSet.getDstAvgPathsConfInterval().get(1),
                    /*agSet.getAvgIntactPathsForFeasible(),
                    agSet.getAvgIntactPathsForFeasibleConfInterval().get(0),
                    agSet.getAvgIntactPathsForFeasibleConfInterval().get(1),
                    agSet.getAvgDisconnectedPathsForFeasible(),
                    agSet.getAvgDisconnectedPathsForFeasibleConfInterval().get(0),
                    agSet.getAvgDisconnectedPathsForFeasibleConfInterval().get(1),*/
                    agSet.getAvgAvgPathLength(),
                    agSet.getAvgAvgPathLengthConfInterval().get(0),
                    agSet.getAvgAvgPathLengthConfInterval().get(1),
                    agSet.getAvgAvgPathCost(),
                    agSet.getAvgAvgPathCostConfInterval().get(0),
                    agSet.getAvgAvgPathCostConfInterval().get(1)
            );
        }
    }

    private String[] makeArray(Object... args) {
        String[] line = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            line[i] = String.valueOf(args[i]);
        }
        return line;
    }

}
