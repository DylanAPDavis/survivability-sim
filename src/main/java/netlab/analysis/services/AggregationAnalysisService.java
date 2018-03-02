package netlab.analysis.services;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.*;
import netlab.analysis.enums.CachingType;
import netlab.submission.enums.*;
import org.springframework.stereotype.Service;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AggregationAnalysisService {

    private final String primaryCost = "primaryCost";
    private final String avgBackupCost = "avgBackupCost";
    private final String totalPaths = "totalPaths";
    private final String runningTime = "runningTime";
    private final String destsConnected = "destsConnected";
    private final String primaryIntact = "primaryIntact";
    private final String connectionsIntact = "connectionsIntact";
    private final String postFailureCost = "postFailureCost";
    private final String reachOnPrimary = "reachOnPrimary";
    private final String reachOnBackup = "reachOnBackup";
    private final String reachOnlyBackup = "reachOnlyBackup";
    private final String cachingCost = "cachingCost";

    private HashingService hashingService;

    public AggregationAnalysisService(HashingService hashingService){
        this.hashingService = hashingService;
    }

    public AggregationParameters makeDefaultParameters(List<Long> seeds){
        List<String> topologyIds = Arrays.asList("nsfnet", "tw");
        /*List<RoutingType> routingTypes = Arrays.asList(RoutingType.Unicast, RoutingType.Anycast, RoutingType.Manycast, RoutingType.Multicast,
                RoutingType.ManyToOne, RoutingType.ManyToMany, RoutingType.Broadcast);*/
        List<RoutingType> routingTypes = Arrays.asList(RoutingType.Unicast, RoutingType.Anycast);
        List<FailureScenario> failureScenarios = Arrays.asList(FailureScenario.AllLinks, FailureScenario.AllNodes,
                FailureScenario.Quake_2);
        List<Integer> nfeValues = Arrays.asList(1, 2);
        Map<RoutingType, List<Algorithm>> algorithmMap = makeAlgorithmMap();
        Map<RoutingType, List<RoutingDescription>> routingDescriptionMap = makeRoutingDescriptionMap();
        Map<RoutingType, List<TrafficCombinationType>> trafficCombinationTypeMap = makeTrafficCombinationMap();
        Set<Algorithm> algorithmsThatCanIgnoreFailures = new HashSet<>();
        //algorithmsThatCanIgnoreFailures.add(Algorithm.ILP);
        /*
        # Only use a nfe of 0 if you're doing default scenario
        if nfe == 0 and scenario != "default":
            continue
        if scenario == "default" and nfe != 0:
            continue
        if nfe == 0 and ignore == "true":
            continue
        */

        return new AggregationParameters(seeds, topologyIds, routingTypes, failureScenarios, nfeValues, algorithmMap, routingDescriptionMap,
                trafficCombinationTypeMap, algorithmsThatCanIgnoreFailures);
    }

    public Map<RoutingType, List<Algorithm>> makeAlgorithmMap(){
        Map<RoutingType, List<Algorithm>> algorithmMap = new HashMap<>();
        algorithmMap.put(RoutingType.Unicast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens, Algorithm.Tabu));
        algorithmMap.put(RoutingType.Anycast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens, Algorithm.Tabu));
        algorithmMap.put(RoutingType.Multicast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo, Algorithm.Tabu));
        algorithmMap.put(RoutingType.Manycast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo, Algorithm.Tabu));
        algorithmMap.put(RoutingType.ManyToOne, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo, Algorithm.Tabu));
        algorithmMap.put(RoutingType.ManyToMany, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo, Algorithm.Tabu));
        algorithmMap.put(RoutingType.Broadcast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo, Algorithm.Tabu));
        return algorithmMap;
    }

    public Map<RoutingType, List<RoutingDescription>> makeRoutingDescriptionMap(){
        Map<RoutingType, List<RoutingDescription>> routingTypeListMap = new HashMap<>();
        routingTypeListMap.put(RoutingType.Unicast, makeRoutingList(RoutingType.Unicast));
        routingTypeListMap.put(RoutingType.Anycast, makeRoutingList(RoutingType.Anycast));
        routingTypeListMap.put(RoutingType.Multicast, makeRoutingList(RoutingType.Multicast));
        routingTypeListMap.put(RoutingType.Manycast, makeRoutingList(RoutingType.Manycast));
        routingTypeListMap.put(RoutingType.ManyToOne, makeRoutingList(RoutingType.ManyToOne));
        routingTypeListMap.put(RoutingType.ManyToMany, makeRoutingList(RoutingType.ManyToMany));
        routingTypeListMap.put(RoutingType.Broadcast, makeRoutingList(RoutingType.Broadcast));
        return routingTypeListMap;
    }

    private List<RoutingDescription> makeRoutingList(RoutingType routingType) {
        List<RoutingDescription> routingList = new ArrayList<>();
        switch(routingType){
            case Unicast:
                routingList.add(new RoutingDescription(1, 1, 1, 1, 1, 1));
                break;
            case Anycast:
                routingList.add(new RoutingDescription(1, 2, 1, 1, 1, 1));
                routingList.add(new RoutingDescription(1, 3, 1, 1, 1, 1));
                break;
            case Multicast:
                routingList.add(new RoutingDescription(1, 2, 1, 1, 2, 2));
                routingList.add(new RoutingDescription(1, 3, 1, 1, 3, 3));
                break;
            case Manycast:
                routingList.add(new RoutingDescription(1, 3, 1, 1, 2, 2));
                routingList.add(new RoutingDescription(1, 4, 1, 1, 3, 3));
                break;
            case ManyToOne:
                //routingList.add(new RoutingDescription(2, 1, 1, 1, 1, 1));
                routingList.add(new RoutingDescription(2, 1, 2, 2, 1, 1));
                //routingList.add(new RoutingDescription(3, 1, 1, 1, 1, 1));
                //routingList.add(new RoutingDescription(3, 1, 2, 2, 1, 1));
                routingList.add(new RoutingDescription(3, 1, 3, 3, 1, 1));
                break;
            case ManyToMany:
                //routingList.add(new RoutingDescription(2, 2, 2, 2, 1, 1));
                routingList.add(new RoutingDescription(2, 2, 2, 2, 1, 2));
                routingList.add(new RoutingDescription(2, 2, 2, 2, 2, 2));
                //routingList.add(new RoutingDescription(3, 3, 3, 3, 1, 1));
                //routingList.add(new RoutingDescription(3, 3, 3, 3, 1, 2));
                //routingList.add(new RoutingDescription(3, 3, 3, 3, 2, 2));
                routingList.add(new RoutingDescription(3, 3, 3, 3, 1, 3));
                routingList.add(new RoutingDescription(3, 3, 3, 3, 2, 3));
                routingList.add(new RoutingDescription(3, 3, 3, 3, 3, 3));
                break;
            case Broadcast:
                routingList.add(new RoutingDescription(2, 2, 2, 2, 2, 2));
                routingList.add(new RoutingDescription(3, 3, 3, 3, 3, 3));
                routingList.add(new RoutingDescription(4, 4, 4, 4, 4, 4));
                break;
        }
        return routingList;
    }

    public Map<RoutingType, List<TrafficCombinationType>> makeTrafficCombinationMap(){
        Map<RoutingType, List<TrafficCombinationType>> trafficCombinationMap = new HashMap<>();
        /*List<TrafficCombinationType> trafficList = Arrays.asList(TrafficCombinationType.None,
                TrafficCombinationType.Source, TrafficCombinationType.Destination, TrafficCombinationType.Both);
                */
        List<TrafficCombinationType> trafficList = Collections.singletonList(TrafficCombinationType.None);
        trafficCombinationMap.put(RoutingType.Unicast, trafficList);
        trafficCombinationMap.put(RoutingType.Anycast, trafficList);
        trafficCombinationMap.put(RoutingType.Multicast, trafficList);
        trafficCombinationMap.put(RoutingType.Manycast, trafficList);
        trafficCombinationMap.put(RoutingType.ManyToOne, trafficList);
        trafficCombinationMap.put(RoutingType.ManyToMany, trafficList);
        trafficCombinationMap.put(RoutingType.Broadcast, trafficList);
        return trafficCombinationMap;
    }


    public AggregateAnalysis aggregateAnalyses(String hash, List<Analysis> analysisList) {

        if (analysisList.isEmpty()) {
            return AggregateAnalysis.builder().build();
        }



        List<String> requestSetIds = new ArrayList<>();
        List<Long> seeds = new ArrayList<>();
        String topologyId = analysisList.get(0).getTopologyId();
        Algorithm algorithm = analysisList.get(0).getAlgorithm();
        RoutingType routingType = analysisList.get(0).getRoutingType();
        FailureScenario failureScenario = analysisList.get(0).getFailureScenario();
        Integer numFailureEvents = analysisList.get(0).getNumFailuresEvents();
        TrafficCombinationType trafficCombinationType = analysisList.get(0).getTrafficCombinationType();
        RoutingDescription routingDescription = analysisList.get(0).getRoutingDescription();
        Boolean ignoreFailures = analysisList.get(0).getIgnoreFailures();

        Double totalWithResults = 0.0;

        Double numFeasible = 0.0;
        Double sumRunningTime = 0.0;
        Double sumCost = 0.0;
        Double sumLinksUsed = 0.0;
        Double sumPaths = 0.0;
        Double sumPrimaryPaths = 0.0;
        Double sumBackupPaths = 0.0;
        Double sumConnectionsSevered = 0.0;
        Double sumConnectionsIntact = 0.0;
        Double sumPathsSevered = 0.0;
        Double sumPathsIntact = 0.0;
        Double sumPrimaryPathsSevered = 0.0;
        Double sumPrimaryPathsIntact = 0.0;
        Double sumDestsConnected = 0.0;

        Double sumAveragePrimaryHops = 0.0;
        Double sumAveragePrimaryCost = 0.0;
        Double sumAveragePrimaryRisk = 0.0;
        Double sumAverageBackupHops = 0.0;
        Double sumAverageBackupCost = 0.0;
        Double sumAverageBackupRisk = 0.0;
        Double sumAverageBackupPaths = 0.0;
        Double sumAveragePrimaryHopsPostFailure = 0.0;
        Double sumAveragePrimaryCostPostFailure = 0.0;
        Double sumAverageBackupPathsIntact = 0.0;
        Double sumAverageBackupPathsSevered = 0.0;

        List<CachingResult> aggregateCaching = new ArrayList<>();
        Map<CachingType, Integer> cachingIndices = new HashMap<>();

        int numWithConnectionsIntact = 0;
        int numRequests = 0;
        for (Analysis analysis : analysisList) {
            if (analysis == null){
                continue;
            }
            numRequests++;
            requestSetIds.add(analysis.getRequestId());
            seeds.add(analysis.getSeed());
            if(analysis.getIsFeasible()) {
                numFeasible++;
            }
            else{
                updateCachingResults(aggregateCaching, analysis.getCachingResults(), cachingIndices);
            }
            if(analysis.getTotalCost() > 0.0){
                totalWithResults++;

                sumRunningTime += analysis.getRunningTime();
                sumCost += analysis.getTotalCost();
                sumLinksUsed += analysis.getTotalLinksUsed();
                sumPaths += analysis.getTotalPaths();
                sumPrimaryPaths += analysis.getTotalPrimaryPaths();
                sumBackupPaths += analysis.getTotalBackupPaths();
                sumConnectionsSevered += analysis.getConnectionsSevered();
                sumConnectionsIntact += analysis.getConnectionsIntact();
                sumPathsSevered += analysis.getPathsSevered();
                sumPathsIntact += analysis.getPathsIntact();
                sumPrimaryPathsSevered += analysis.getPrimaryPathsSevered();
                sumPrimaryPathsIntact += analysis.getPrimaryPathsIntact();
                sumDestsConnected += analysis.getDestsConnected();

                sumAveragePrimaryCost += analysis.getAveragePrimaryCost();
                sumAveragePrimaryHops += analysis.getAveragePrimaryHops();
                sumAveragePrimaryRisk += analysis.getAveragePrimaryRisk();
                sumAverageBackupCost += analysis.getAverageBackupCost();
                sumAverageBackupHops += analysis.getAverageBackupHops();
                sumAverageBackupRisk += analysis.getAverageBackupRisk();
                sumAverageBackupPaths += analysis.getAverageBackupPaths();
                sumAverageBackupPathsIntact += analysis.getAverageBackupPathsIntact();
                sumAverageBackupPathsSevered += analysis.getAverageBackupPathsSevered();

                if(analysis.getConnectionsIntact() > 0) {
                    sumAveragePrimaryCostPostFailure += analysis.getAveragePrimaryCostPostFailure();
                    sumAveragePrimaryHopsPostFailure += analysis.getAveragePrimaryHopsPostFailure();
                    numWithConnectionsIntact++;
                }

                updateCachingResults(aggregateCaching, analysis.getCachingResults(), cachingIndices);
            }
        }

        averageCachingResults(aggregateCaching, totalWithResults);

        double totalResultsDivisor = totalWithResults > 0 ? totalWithResults : 1.0;
        double totalIntactDivisor = numWithConnectionsIntact > 0 ? numWithConnectionsIntact : 1.0;

        return AggregateAnalysis.builder()
                .hash(hash)
                .requestSetIds(requestSetIds)
                .seeds(seeds)
                .topologyId(topologyId)
                .algorithm(algorithm)
                .routingType(routingType)
                .failureScenario(failureScenario)
                .numFailuresEvents(numFailureEvents)
                .trafficCombinationType(trafficCombinationType)
                .routingDescription(routingDescription)
                .ignoreFailures(ignoreFailures)
                .totalFeasible(numFeasible)
                .percentFeasible(numFeasible / numRequests)
                .runningTime(sumRunningTime / totalResultsDivisor)
                .totalCost(sumCost / totalResultsDivisor)
                .totalLinksUsed(sumLinksUsed / totalResultsDivisor)
                .totalPaths(sumPaths / totalResultsDivisor)
                .totalPrimaryPaths(sumPrimaryPaths / totalResultsDivisor)
                .totalBackupPaths(sumBackupPaths / totalResultsDivisor)
                .connectionsSevered(sumConnectionsSevered / totalResultsDivisor)
                .connectionsIntact(sumConnectionsIntact / totalResultsDivisor)
                .primaryPathsIntact(sumPrimaryPathsIntact / totalResultsDivisor)
                .primaryPathsSevered(sumPrimaryPathsSevered / totalResultsDivisor)
                .pathsSevered(sumPathsSevered / totalResultsDivisor)
                .pathsIntact(sumPathsIntact / totalResultsDivisor)
                .destsConnected(sumDestsConnected / totalResultsDivisor)
                .averagePrimaryHops(sumAveragePrimaryHops / totalResultsDivisor)
                .averagePrimaryCost(sumAveragePrimaryCost / totalResultsDivisor)
                .averagePrimaryRisk(sumAveragePrimaryRisk / totalResultsDivisor)
                .averageBackupHops(sumAverageBackupHops / totalResultsDivisor)
                .averageBackupCost(sumAverageBackupCost / totalResultsDivisor)
                .averageBackupRisk(sumAverageBackupRisk / totalResultsDivisor)
                .averageBackupPaths(sumAverageBackupPaths / totalResultsDivisor)
                .averagePrimaryHopsPostFailure(sumAveragePrimaryHopsPostFailure / totalIntactDivisor)
                .averagePrimaryCostPostFailure(sumAveragePrimaryCostPostFailure / totalIntactDivisor)
                .averageBackupPathsIntact(sumAverageBackupPathsIntact / totalResultsDivisor)
                .averageBackupPathsSevered(sumAverageBackupPathsSevered / totalResultsDivisor)
                .cachingResults(aggregateCaching)
                .build();
    }

    private void updateCachingResults(List<CachingResult> aggregateCaching, List<CachingResult> cachingResults,
                                      Map<CachingType, Integer> cachingResultIndices) {
        if(aggregateCaching.isEmpty()){
            for(int i = 0; i < cachingResults.size(); i++){
                aggregateCaching.add(cachingResults.get(i));
                cachingResultIndices.put(cachingResults.get(i).getType(), i);
            }
        }
        else{
            for(CachingResult result : cachingResults){
                CachingResult agResult = aggregateCaching.get(cachingResultIndices.get(result.getType()));
                agResult.setCachingCost(agResult.getCachingCost() + result.getCachingCost());
                agResult.setAvgHopCountBefore(agResult.getAvgHopCountBefore() + result.getAvgHopCountBefore());
                agResult.setAvgHopCountAfter(agResult.getAvgHopCountAfter() + result.getAvgHopCountAfter());
                agResult.setReachOnPrimary(agResult.getReachOnPrimary() + result.getReachOnPrimary());
                agResult.setReachOnBackup(agResult.getReachOnBackup() + result.getReachOnBackup());
                agResult.setReachOnlyBackup(agResult.getReachOnlyBackup() + result.getReachOnlyBackup());
            }
        }
    }


    private void averageCachingResults(List<CachingResult> aggregateCaching, double numToDivideBy){
        for(CachingResult agResult : aggregateCaching){
            agResult.setCachingCost(numToDivideBy > 0 ? agResult.getCachingCost() / numToDivideBy : 0.0);
            agResult.setAvgHopCountBefore(numToDivideBy > 0 ? agResult.getAvgHopCountBefore() / numToDivideBy : 0.0);
            agResult.setAvgHopCountAfter(numToDivideBy > 0 ? agResult.getAvgHopCountAfter() / numToDivideBy : 0.0);
            agResult.setReachOnPrimary(numToDivideBy > 0 ? agResult.getReachOnPrimary() / numToDivideBy : 0.0);
            agResult.setReachOnBackup(numToDivideBy > 0 ? agResult.getReachOnBackup() / numToDivideBy : 0.0);
            agResult.setReachOnlyBackup(numToDivideBy > 0 ? agResult.getReachOnlyBackup() / numToDivideBy : 0.0);
        }
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

    /*
        Table Formats
        Failure Scenario - NFE
        Anycast # | LP | FB | Tabu | MinDist | MinRisk | Yen | Bhan | HC
        Primary Cost
        Running Time
        Total Paths
        ------------------------------------------------------------------
        Anycast #
        Met 1
        Met 2
        Met 3
        -------------------------------------------------------------------
        Anycast #
        Met 1
        Met 2
        Met 3
        -------------------------------------------------------------------

     */

    public String createAltAggregationOutput(Map<String, AggregateAnalysis> outputMap){
        String fileName = "aggregatedTables";

        List<FailureScenario> failureScenarios = Arrays.asList(FailureScenario.AllLinks, FailureScenario.AllNodes,
                FailureScenario.Quake_2);
        List<Integer> nfeValues = Arrays.asList(1, 2);
        List<Integer> numD = Arrays.asList(1, 2, 3);
        List<Algorithm> algs = Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.Tabu, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Yens, Algorithm.Bhandari, Algorithm.Hamlitonian);
        Map<Algorithm, Integer> algOrder = new HashMap<>();
        for(int i = 0; i < algs.size(); i++){
            algOrder.put(algs.get(i), i);
        }
        List<String> topologies = Arrays.asList("nsfnet", "tw");


        Map<String, Map<Integer, List<AggregateAnalysis>>> tableMap = buildTableMap(outputMap);

        List<CachingType> cachingTypes = Arrays.asList(CachingType.EntirePath, CachingType.LeaveCopyDown,
                CachingType.SourceAdjacent, CachingType.FailureAware, CachingType.BranchingPoint);

        List<String> beforeMetrics = Arrays.asList(primaryCost, avgBackupCost, totalPaths, runningTime);
        List<String> afterMetrics = Arrays.asList(primaryIntact, connectionsIntact, postFailureCost);
        List<String> cachingMetrics = Arrays.asList(reachOnPrimary, reachOnBackup, reachOnlyBackup, cachingCost);

        Map<Integer, List<String>> metricCategories = new HashMap<>();
        metricCategories.put(0, beforeMetrics);
        metricCategories.put(1, afterMetrics);
        metricCategories.put(2, cachingMetrics);


        DecimalFormat format = new DecimalFormat("#####.##");
        List<String[]> output = new ArrayList<>();
        for(String topology : topologies) {
            for (FailureScenario failureScenario : failureScenarios) {
                for (Integer nfe : nfeValues) {
                    output.add(new String[]{"------"});
                    output.add(new String[]{"------"});
                    String hash = hashingService.hash(topology, failureScenario.getCode(), nfe);
                    Map<Integer, List<AggregateAnalysis>> mapForTable = tableMap.get(hash);
                    for(int categoryNumber = 0; categoryNumber < metricCategories.size(); categoryNumber++) {
                        // Add header line for this table
                        output.add(makeArray(topology, failureScenario.getCode(), nfe, determineCategory(categoryNumber)));
                        List<String> metrics = metricCategories.get(categoryNumber);
                        if(categoryNumber == 2){
                            // Caching Metrics
                            // Only do this for anycast 1/3
                            for(CachingType cachingType : cachingTypes) {
                                String[] cachingHeader = makeCachingHeader(algs, cachingType);
                                output.add(cachingHeader);
                                List<AggregateAnalysis> agForD = mapForTable.get(3);
                                // Sort these analyses by which algorithm they're using
                                agForD.sort(Comparator.comparing(ag -> algOrder.get(ag.getAlgorithm())));
                                // Get the lines for the caching metrics
                                List<String[]> metricsLines = makeMetricLines(metrics, agForD, format, cachingType);
                                for(String[] metricLine : metricsLines){
                                    output.add(metricLine);
                                }
                            }
                        } else {
                            for (Integer d : numD) {
                                // Sub header for Anycast # and Algorithms
                                String[] anycastHeader = makeAnycastHeader(algs, d);
                                output.add(anycastHeader);
                                List<AggregateAnalysis> agForD = mapForTable.get(d);
                                // Sort these analyses by which algorithm they're using
                                agForD.sort(Comparator.comparing(ag -> algOrder.get(ag.getAlgorithm())));
                                // For each line: metric - value for Ag 1 - value for Ag 2 - value for Ag 3 - ...
                                List<String[]> metricsLines = makeMetricLines(metrics, agForD, format, null);
                                for(String[] metricLine : metricsLines){
                                    output.add(metricLine);
                                }
                            }
                        }
                        output.add(new String[]{});
                    }
                }
            }
        }
        try {
            CSVWriter writer =  new CSVWriter(new FileWriter(fileName + ".csv"), ',');
            writer.writeAll(output);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileName;
    }

    private List<String[]> makeMetricLines(List<String> metrics, List<AggregateAnalysis> agForD, DecimalFormat format,
                                           CachingType cachingType){
        List<String[]> output = new ArrayList<>();
        for (String metric : metrics) {
            List<String> metricValueList = new ArrayList<>();
            metricValueList.add(metric);
            for (AggregateAnalysis agAn : agForD) {
                // For each aggregate analysis, get metrics
                Double value;
                if(cachingType != null){
                    List<CachingResult> matchingResult = agAn.getCachingResults().stream()
                            .filter(c -> cachingType.equals(c.getType()))
                            .collect(Collectors.toList());
                    value = getMetricValue(metric, matchingResult.get(0));
                }
                else{
                    value = getMetricValue(metric, agAn);
                }
                metricValueList.add(format.format(value));
            }
            String[] metricLine = makeArrayFromList(metricValueList);
            output.add(metricLine);
        }
        return output;
    }

    private String[] makeCachingHeader(List<Algorithm> algs, CachingType cachingType) {
        String cache = "Caching: " + cachingType;
        List<String> temp = algs.stream().map(Algorithm::getCode).collect(Collectors.toList());
        temp.add(0, cache);
        return makeArrayFromList(temp);
    }

    private String[] makeAnycastHeader(List<Algorithm> algs, Integer d){
        String any = "Anycast 1/" + d;
        List<String> temp = algs.stream().map(Algorithm::getCode).collect(Collectors.toList());
        temp.add(0, any);
        return makeArrayFromList(temp);
    }

    private Object determineCategory(int categoryNumber) {
        switch(categoryNumber){
            case 0:
                return "Before Failure";
            case 1:
                return "After Failure";
            case 2:
                return "Caching";
        }
        return "Error";
    }

    public Map<String, Map<Integer, List<AggregateAnalysis>>> buildTableMap(Map<String, AggregateAnalysis> outputMap){
        Map<String, Map<Integer, List<AggregateAnalysis>>> tableMap = new HashMap<>();

        // Categorize by Topology - Failure Scenario - NFE - Anycast #
        for(AggregateAnalysis aggregateAnalysis : outputMap.values()){
            if(aggregateAnalysis.getIgnoreFailures()){
                continue;
            }
            String topology = aggregateAnalysis.getTopologyId();
            FailureScenario failureScenario = aggregateAnalysis.getFailureScenario();
            Integer nfe = aggregateAnalysis.getNumFailuresEvents();
            Integer d = aggregateAnalysis.getRoutingDescription().getNumDestinations();
            String hash = hashingService.hash(topology, failureScenario.getCode(), nfe);

            tableMap.putIfAbsent(hash, new HashMap<>());
            Map<Integer, List<AggregateAnalysis>> numDMap = tableMap.get(hash);
            numDMap.putIfAbsent(d, new ArrayList<>());
            numDMap.get(d).add(aggregateAnalysis);
        }
        return tableMap;
    }



    public Double getMetricValue(String metric, AggregateAnalysis agAn){
        switch(metric){
            case primaryCost:
                return agAn.getAveragePrimaryCost();
            case runningTime:
                return agAn.getRunningTime();
            case totalPaths:
                return agAn.getTotalPaths();
            case avgBackupCost:
                return agAn.getAverageBackupCost();
            case destsConnected:
                return agAn.getDestsConnected();
            case primaryIntact:
                return agAn.getPrimaryPathsIntact();
            case connectionsIntact:
                return agAn.getConnectionsIntact();
            case postFailureCost:
                return agAn.getAveragePrimaryCostPostFailure();
        }
        return -1.0;
    }

    public Double getMetricValue(String metric, CachingResult cachingResult){
        switch(metric){
            case reachOnPrimary:
                return cachingResult.getReachOnPrimary();
            case reachOnBackup:
                return cachingResult.getReachOnBackup();
            case reachOnlyBackup:
                return cachingResult.getReachOnlyBackup();
            case cachingCost:
                return cachingResult.getCachingCost();
        }
        return -1.0;
    }


    public String createAggregationOutput(AggregationParameters agParams, Map<String, AggregateAnalysis> outputMap) {

        List<String> topologyIds = agParams.getTopologyIds();
        List<RoutingType> routingTypes = agParams.getRoutingTypes();
        List<FailureScenario> failureScenarios = agParams.getFailureScenarios();
        Map<RoutingType, List<Algorithm>> algorithmMap = agParams.getAlgorithmMap();
        Map<RoutingType, List<TrafficCombinationType>> trafficCombinationTypeMap = agParams.getTrafficCombinationTypeMap();
        Map<RoutingType, List<RoutingDescription>> routingDescriptionMap = agParams.getRoutingDescriptionMap();

        List<String> fileNames = new ArrayList<>();
        List<Future> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(16);
        long startTime = System.nanoTime();
        for(String topoId : topologyIds) {
            for (RoutingType routingType : routingTypes) {
                // CSV per Topology-RoutingType-FailureScenario combo
                List<Algorithm> algorithms = algorithmMap.get(routingType);
                List<TrafficCombinationType> trafficCombinationTypes = trafficCombinationTypeMap.get(routingType);
                List<RoutingDescription> routingDescriptions = routingDescriptionMap.get(routingType);
                for(FailureScenario failureScenario : failureScenarios) {
                    // Make a CSV
                    Future f = executor.submit(outputResults(agParams, topoId, routingType, failureScenario, algorithms,
                            trafficCombinationTypes, routingDescriptions, outputMap));
                    futures.add(f);
                    String name = "results/aggregated/" + topoId + "_" + routingType + "_" + failureScenario.toString();
                    fileNames.add(name + ".csv");
                }
            }
        }
        futures.forEach(f -> {
            try {
                f.get();
            }
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        log.info("File output took: " + duration + " seconds");
        return fileNames.toString();
    }

    private Runnable outputResults(AggregationParameters agParams, String topoId, RoutingType routingType,
                                   FailureScenario failureScenario, List<Algorithm> algorithms,
                                   List<TrafficCombinationType> trafficCombinationTypes,
                                   List<RoutingDescription> routingDescriptions, Map<String, AggregateAnalysis> outputMap){
        List<Integer> nfeValues = agParams.getNfeValues();
        Set<Algorithm> algorithmsThatCanIgnoreFailures = agParams.getAlgorithmsThatCanIgnoreFailures();
        return () -> {
            List<String[]> output = new ArrayList<>();
            for(Integer nfe : nfeValues){

                for(Algorithm algorithm : algorithms){
                    List<Boolean> ignoreFValues = algorithmsThatCanIgnoreFailures.contains(algorithm) ?
                            Arrays.asList(true, false) : Collections.singletonList(false);

                    for(Boolean ignoreF : ignoreFValues){
                        if(nfe == 0 && (!failureScenario.equals(FailureScenario.Default) || ignoreF)){
                            continue;
                        }
                        else if(nfe != 0 && failureScenario.equals(FailureScenario.Default)){
                            continue;
                        }
                        for(TrafficCombinationType trafficCombinationType : trafficCombinationTypes){
                            String[] line = makeArray("NFE: " + nfe,  "Algorithm: " + algorithm,
                                    "IgnoreF: " + ignoreF, "TCombo: " + trafficCombinationType);
                            output.add(line);
                            for (RoutingDescription routingDescription : routingDescriptions) {
                                // Get the matching results from the outputMap
                                String hash = hashingService.hashForAggregation(topoId, algorithm, routingType,
                                        failureScenario, nfe, trafficCombinationType, routingDescription, ignoreF);
                                AggregateAnalysis aggregateAnalysis = outputMap.get(hash);
                                if(aggregateAnalysis != null) {
                                    output.addAll(createLines(aggregateAnalysis));
                                }
                            }
                        }
                    }
                }
            }
            try {
                String name = "results/aggregated/" + topoId + "_" + routingType + "_" + failureScenario.toString();
                CSVWriter writer = new CSVWriter(new FileWriter(name + ".csv"), ',');
                writer.writeAll(output);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        };
    }

    /*
        private Double totalFeasible;
        private Double percentFeasible;

        private Double runningTime;
        private Double totalCost;
        private Double totalLinksUsed;
        private Double totalPaths;
        private Double totalPrimaryPaths;
        private Double totalBackupPaths;
        private Double connectionsSevered;
        private Double connectionsIntact;
        private Double pathsSevered;
        private Double pathsIntact;
        private Double primaryPathsSevered;
        private Double primaryPathsIntact;


        private Double averagePrimaryHops;
        private Double averagePrimaryCost;
        private Double averagePrimaryRisk;
        private Double averageBackupHops;
        private Double averageBackupCost;
        private Double averageBackupRisk;
        private Double averageBackupPaths;
        private Double averagePrimaryHopsPostFailure;
        private Double averagePrimaryCostPostFailure;
        private Double averageBackupPathsIntact;
        private Double averageBackupPathsSevered;

        // Caching
        private List<CachingResult> cachingResults;
     */
    private List<String[]> createLines(AggregateAnalysis ag) {
        RoutingDescription rd = ag.getRoutingDescription();
        String[] header = makeArray( "S: " +rd.getNumSources(), "D: " + rd.getNumDestinations(),
                "MinS: " + rd.getUseMinS(), "MaxS: " + rd.getUseMaxS(), "MinD: " + rd.getUseMinD(), "MaxD: " + rd.getUseMaxD());
        String[] metricHeader = makeMetricHeader(ag.getCachingResults());
        String[] data = makeDataLine(ag);
        return Arrays.asList(header, metricHeader, data, new String[]{});
    }

    private String[] makeMetricHeader(List<CachingResult> cachingResults){
        List<String> headers = Arrays.asList("%Feasible", "runningTime", "totalCost", "totalLinksUsed", "totalPaths",
                "totalPrim", "totalBack", "connsSevered", "connsIntact", "pathsSevered", "pathsIntact", "primSevered",
                "primIntact", "primHops", "primCost", "primRisk", "backHops", "backCost", "backRisk", "avgBackPaths",
                "primHopsPF", "primCostPF", "avgBackSevered", "avgBackIntact");
        List<String> cachingHeaders = new ArrayList<>();
        for(CachingResult cachingResult : cachingResults){
            String cType = cachingResult.getType().getCode();
            cachingHeaders.add(cType + "_" + "cCost");
            cachingHeaders.add(cType + "_" + "crPrim");
            cachingHeaders.add(cType + "_" + "crBack");
            cachingHeaders.add(cType + "_" + "crOBack");
            cachingHeaders.add(cType + "_" + "cHopB");
            cachingHeaders.add(cType + "_" + "cHopA");
    }
        List<String> combined = new ArrayList<>(headers);
        combined.addAll(cachingHeaders);
        return combined.toArray(new String[headers.size()]);
    }

    private String[] makeDataLine(AggregateAnalysis ag){
        List<String> dataList = makeList(ag.getPercentFeasible(), ag.getRunningTime(), ag.getTotalCost(), ag.getTotalLinksUsed(),
                ag.getTotalPaths(), ag.getTotalPrimaryPaths(), ag.getTotalBackupPaths(),
                ag.getConnectionsSevered(), ag.getConnectionsIntact(), ag.getPathsSevered(),
                ag.getPathsIntact(), ag.getPrimaryPathsSevered(), ag.getPrimaryPathsIntact(),
                ag.getAveragePrimaryHops(), ag.getAveragePrimaryCost(), ag.getAveragePrimaryRisk(),
                ag.getAverageBackupHops(), ag.getAverageBackupCost(), ag.getAverageBackupRisk(), ag.getAverageBackupPaths(),
                ag.getAveragePrimaryHopsPostFailure(), ag.getAveragePrimaryCostPostFailure(), ag.getAverageBackupPathsSevered(),
                ag.getAverageBackupPathsIntact());
        for(CachingResult cachingResult : ag.getCachingResults()){
            dataList.add(String.valueOf(cachingResult.getCachingCost()));
            dataList.add(String.valueOf(cachingResult.getReachOnPrimary()));
            dataList.add(String.valueOf(cachingResult.getReachOnBackup()));
            dataList.add(String.valueOf(cachingResult.getReachOnlyBackup()));
            dataList.add(String.valueOf(cachingResult.getAvgHopCountBefore()));
            dataList.add(String.valueOf(cachingResult.getAvgHopCountAfter()));
        }
        return dataList.toArray(new String[dataList.size()]);
    }

    private String[] makeArrayFromList(List<String> objects){
        String[] array = new String[objects.size()];
        for (int i = 0; i < objects.size(); i++) {
            array[i] = objects.get(i);
        }
        return array;
    }

    private String[] makeArray(Object... args) {
        String[] line = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            line[i] = String.valueOf(args[i]);
        }
        return line;
    }

    private List<String> makeList(Object... args){
        List<String> data = new ArrayList<>();
        for(Object arg : args){
            data.add(String.valueOf(arg));
        }
        return data;
    }
}
