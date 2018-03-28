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


    public AggregationParameters makeDefaultParameters(List<Long> seeds){
        List<String> topologyIds = Arrays.asList("tw");
        /*List<RoutingType> routingTypes = Arrays.asList(RoutingType.Unicast, RoutingType.Anycast, RoutingType.Manycast, RoutingType.Multicast,
                RoutingType.ManyToOne, RoutingType.ManyToMany, RoutingType.Broadcast);*/
        List<RoutingType> routingTypes = Arrays.asList(RoutingType.ManyToMany);
        List<FailureScenario> failureScenarios = Arrays.asList(FailureScenario.AllLinks, FailureScenario.Quake_2);
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
                Algorithm.MinimumRiskPath, Algorithm.Bhandari,  Algorithm.Yens, Algorithm.Tabu));
        algorithmMap.put(RoutingType.Anycast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Yens, Algorithm.Tabu));
        algorithmMap.put(RoutingType.Multicast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari,  Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo, Algorithm.Tabu));
        algorithmMap.put(RoutingType.Manycast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari,  Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo, Algorithm.Tabu));
        algorithmMap.put(RoutingType.ManyToOne, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari,  Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo, Algorithm.Tabu));
        algorithmMap.put(RoutingType.ManyToMany, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari,  Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo, Algorithm.Tabu));
        algorithmMap.put(RoutingType.Broadcast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari,  Algorithm.Yens,
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
                routingList.add(new RoutingDescription(5, 1, 5, 5, 1, 1));
                routingList.add(new RoutingDescription(10, 1, 10, 10, 1, 1));
                routingList.add(new RoutingDescription(5, 2, 5, 5, 1, 2));
                routingList.add(new RoutingDescription(10, 2, 10, 10, 1, 2));
                routingList.add(new RoutingDescription(5, 2, 5, 5, 1, 3));
                routingList.add(new RoutingDescription(10, 2, 10, 10, 1, 3));
                /*
                {"num_s": 5, "num_d": 1, "use_min_s": 5, "use_max_s": 5, "use_min_d": 1, "use_max_d": 1},
                {"num_s": 10, "num_d": 1, "use_min_s": 10, "use_max_s": 10, "use_min_d": 1, "use_max_d": 1},
                {"num_s": 5, "num_d": 2, "use_min_s": 5, "use_max_s": 5, "use_min_d": 1, "use_max_d": 2},
                {"num_s": 10, "num_d": 2, "use_min_s": 10, "use_max_s": 10, "use_min_d": 1, "use_max_d": 2},
                {"num_s": 5, "num_d": 3, "use_min_s": 5, "use_max_s": 5, "use_min_d": 1, "use_max_d": 3},
                {"num_s": 10, "num_d": 3, "use_min_s": 10, "use_max_s": 10, "use_min_d": 1, "use_max_d": 3},
                 */
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
        Double totalWithBackup = 0.0;

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

        Double sumPrimaryIntactPerSrc = 0.0;
        Double sumDestsConnectedPerSrc = 0.0;


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
                if(analysis.getTotalBackupPaths() > 0){
                    totalWithBackup++;
                }
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

                sumDestsConnectedPerSrc += analysis.getDestsConnectedPerSrc();
                sumPrimaryIntactPerSrc += analysis.getPrimaryIntactPerSrc();

                if(analysis.getConnectionsIntact() > 0) {
                    numWithConnectionsIntact++;
                    sumAveragePrimaryCostPostFailure += analysis.getAveragePrimaryCostPostFailure();
                    sumAveragePrimaryHopsPostFailure += analysis.getAveragePrimaryHopsPostFailure();
                }

                updateCachingResults(aggregateCaching, analysis.getCachingResults(), cachingIndices);
            }
        }

        averageCachingResults(aggregateCaching, totalWithResults);

        double totalResultsDivisor = totalWithResults > 0 ? totalWithResults : 1.0;
        double totalIntactDivisor = numWithConnectionsIntact > 0 ? numWithConnectionsIntact : 1.0;
        double totalBackupDivisor = totalWithBackup > 0 ? totalWithBackup : 1.0;

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
                .averageBackupHops(sumAverageBackupHops / totalBackupDivisor)
                .averageBackupCost(sumAverageBackupCost / totalBackupDivisor)
                .averageBackupRisk(sumAverageBackupRisk / totalBackupDivisor)
                .averageBackupPaths(sumAverageBackupPaths / totalBackupDivisor)
                .averagePrimaryHopsPostFailure(sumAveragePrimaryHopsPostFailure / totalIntactDivisor)
                .averagePrimaryCostPostFailure(sumAveragePrimaryCostPostFailure / totalIntactDivisor)
                .averageBackupPathsIntact(sumAverageBackupPathsIntact / totalResultsDivisor)
                .averageBackupPathsSevered(sumAverageBackupPathsSevered / totalResultsDivisor)
                .destsConnectedPerSrc(sumDestsConnectedPerSrc / totalResultsDivisor)
                .primaryIntactPerSrc(sumPrimaryIntactPerSrc / totalResultsDivisor)
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


}
