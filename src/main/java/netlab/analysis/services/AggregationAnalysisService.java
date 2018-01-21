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
import java.util.*;

@Service
@Slf4j
public class AggregationAnalysisService {


    private HashingService hashingService;

    public AggregationAnalysisService(HashingService hashingService){
        this.hashingService = hashingService;
    }

    public AggregationParameters makeDefaultParameters(List<Long> seeds){
        List<String> topologyIds = Collections.singletonList("NSFnet");
        /*List<RoutingType> routingTypes = Arrays.asList(RoutingType.Unicast, RoutingType.Anycast, RoutingType.Manycast, RoutingType.Multicast,
                RoutingType.ManyToOne, RoutingType.ManyToMany, RoutingType.Broadcast);*/
        List<RoutingType> routingTypes = Arrays.asList(RoutingType.Unicast);
        List<FailureScenario> failureScenarios = Arrays.asList(FailureScenario.Default, FailureScenario.AllLinks, FailureScenario.AllNodes,
                FailureScenario.Quake_1, FailureScenario.Quake_2, FailureScenario.Quake_3);
        List<Integer> nfeValues = Arrays.asList(0, 1, 2, 3, 9999);
        Map<RoutingType, List<Algorithm>> algorithmMap = makeAlgorithmMap();
        Map<RoutingType, List<RoutingDescription>> routingDescriptionMap = makeRoutingDescriptionMap();
        Map<RoutingType, List<TrafficCombinationType>> trafficCombinationTypeMap = makeTrafficCombinationMap();
        Set<Algorithm> algorithmsThatCanIgnoreFailures = new HashSet<>();
        algorithmsThatCanIgnoreFailures.add(Algorithm.ILP);
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
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens));
        algorithmMap.put(RoutingType.Anycast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens));
        algorithmMap.put(RoutingType.Multicast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo));
        algorithmMap.put(RoutingType.Manycast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo));
        algorithmMap.put(RoutingType.ManyToOne, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo));
        algorithmMap.put(RoutingType.ManyToMany, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo));
        algorithmMap.put(RoutingType.Broadcast, Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.MinimumCostPath,
                Algorithm.MinimumRiskPath, Algorithm.Bhandari, Algorithm.Hamlitonian, Algorithm.Yens,
                Algorithm.OverlappingTrees, Algorithm.MemberForwarding, Algorithm.CycleForTwo));
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
        List<TrafficCombinationType> trafficList = Arrays.asList(TrafficCombinationType.None,
                TrafficCombinationType.Destination);
        trafficCombinationMap.put(RoutingType.Unicast, Collections.singletonList(TrafficCombinationType.None));
        trafficCombinationMap.put(RoutingType.Anycast, trafficList);
        trafficCombinationMap.put(RoutingType.Multicast, trafficList);
        trafficCombinationMap.put(RoutingType.Manycast, trafficList);
        trafficCombinationMap.put(RoutingType.ManyToOne, trafficList);
        trafficCombinationMap.put(RoutingType.ManyToMany, trafficList);
        trafficCombinationMap.put(RoutingType.Broadcast, trafficList);
        return trafficCombinationMap;
    }


    public AggregateAnalysis aggregateAnalyses(List<Analysis> analyses) {

        if (analyses.isEmpty()) {
            return AggregateAnalysis.builder().build();
        }



        List<String> requestSetIds = new ArrayList<>();
        List<Long> seeds = new ArrayList<>();
        String topologyId = analyses.get(0).getTopologyId();
        Algorithm algorithm = analyses.get(0).getAlgorithm();
        RoutingType routingType = analyses.get(0).getRoutingType();
        FailureScenario failureScenario = analyses.get(0).getFailureScenario();
        Integer numFailureEvents = analyses.get(0).getNumFailuresEvents();
        TrafficCombinationType trafficCombinationType = analyses.get(0).getTrafficCombinationType();
        RoutingDescription routingDescription = analyses.get(0).getRoutingDescription();
        Boolean ignoreFailures = analyses.get(0).getIgnoreFailures();

        Double totalWithResults = 0.0;

        Double sumRunningTime = 0.0;
        Double numFeasible = 0.0;
        Double sumLinksUsed = 0.0;
        Double sumCost = 0.0;
        Double sumPaths = 0.0;
        Double sumAveragePrimaryHops = 0.0;
        Double sumAveragePrimaryCost = 0.0;

        Double sumAveragePathRisk = 0.0;
        Double sumAverageMinRiskPerPair = 0.0;
        Double sumAverageMaxRiskPerPair = 0.0;
        Double sumAveragePrimaryRisk = 0.0;

        Double sumAveragePrimaryHopsPostFailure = 0.0;
        Double sumAveragePrimaryCostPostFailure = 0.0;
        Double sumPathsSevered = 0.0;
        Double sumPathsIntact = 0.0;
        Double sumConnectionsSevered = 0.0;
        Double sumConnectionsIntact = 0.0;

        List<CachingResult> aggregateCaching = new ArrayList<>();
        Map<CachingType, Integer> cachingIndices = new HashMap<>();

        int numWithConnectionsIntact = 0;
        int numRequests = 0;
        for (Analysis analysis : analyses) {
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
                sumLinksUsed += analysis.getTotalLinksUsed();
                sumCost += analysis.getTotalCost();
                sumPaths += analysis.getTotalPaths();
                sumAveragePrimaryCost += analysis.getAveragePrimaryCost();
                sumAveragePrimaryHops += analysis.getAveragePrimaryHops();

                sumAveragePathRisk += analysis.getAveragePathRisk();
                sumAverageMinRiskPerPair += analysis.getAverageMinRiskPerPair();
                sumAverageMaxRiskPerPair += analysis.getAverageMaxRiskPerPair();
                sumAveragePrimaryRisk += analysis.getAveragePrimaryRisk();

                sumPathsSevered += analysis.getPathsSevered();
                sumPathsIntact += analysis.getPathsIntact();
                sumConnectionsSevered += analysis.getConnectionsSevered();
                sumConnectionsIntact += analysis.getConnectionsIntact();

                if(analysis.getConnectionsIntact() > 0) {
                    sumAveragePrimaryCostPostFailure += analysis.getAveragePrimaryCostPostFailure();
                    sumAveragePrimaryHopsPostFailure += analysis.getAveragePrimaryHopsPostFailure();
                    numWithConnectionsIntact++;
                }

                updateCachingResults(aggregateCaching, analysis.getCachingResults(), cachingIndices);
            }
        }

        averageCachingResults(aggregateCaching, totalWithResults);

        return AggregateAnalysis.builder()
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
                .runningTime(totalWithResults > 0 ? sumRunningTime / totalWithResults : 0.0)
                .totalCost(totalWithResults > 0 ? sumCost / totalWithResults : 0.0)
                .totalLinksUsed(totalWithResults > 0 ? sumLinksUsed / totalWithResults : 0.0)
                .totalPaths(totalWithResults > 0 ? sumPaths / totalWithResults : 0.0)
                .averagePrimaryHops(totalWithResults > 0 ? sumAveragePrimaryHops / totalWithResults : 0.0)
                .averagePrimaryCost(totalWithResults > 0 ? sumAveragePrimaryCost / totalWithResults : 0.0)
                .averagePathRisk(totalWithResults > 0 ? sumAveragePathRisk / totalWithResults : 0.0)
                .averageMinRiskPerPair(totalWithResults > 0 ? sumAverageMinRiskPerPair / totalWithResults : 0.0)
                .averageMaxRiskPerPair(totalWithResults > 0 ? sumAverageMaxRiskPerPair / totalWithResults : 0.0)
                .averagePrimaryRisk( totalWithResults > 0 ?sumAveragePrimaryRisk / totalWithResults : 0.0)
                .averagePrimaryHopsPostFailure(numWithConnectionsIntact > 0 ? sumAveragePrimaryHopsPostFailure / numWithConnectionsIntact : 0)
                .averagePrimaryCostPostFailure(numWithConnectionsIntact > 0 ?  sumAveragePrimaryCostPostFailure / numWithConnectionsIntact : 0)
                .pathsSevered(totalWithResults > 0 ? sumPathsSevered / totalWithResults : 0.0)
                .pathsIntact(totalWithResults > 0 ? sumPathsIntact / totalWithResults : 0.0)
                .connectionsSevered(totalWithResults > 0 ? sumConnectionsSevered / totalWithResults : 0.0)
                .connectionsIntact(totalWithResults > 0 ? sumConnectionsIntact / totalWithResults : 0.0)
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
                agResult.setAvgAccessibility(agResult.getAvgAccessibility() + result.getAvgAccessibility());
                agResult.setAvgHopCountToContent(agResult.getAvgHopCountToContent() + result.getAvgHopCountToContent());
                agResult.setPairReachThroughBackup(agResult.getPairReachThroughBackup() + result.getPairReachThroughBackup());
                agResult.setReachability(agResult.getReachability() + result.getReachability());
            }
        }
    }

    private void averageCachingResults(List<CachingResult> aggregateCaching, double numToDivideBy){
        for(CachingResult agResult : aggregateCaching){
            agResult.setCachingCost(numToDivideBy > 0 ? agResult.getCachingCost() / numToDivideBy : 0.0);
            agResult.setAvgAccessibility(numToDivideBy > 0 ? agResult.getAvgAccessibility() / numToDivideBy : 0.0);
            agResult.setAvgHopCountToContent(numToDivideBy > 0 ? agResult.getAvgHopCountToContent() / numToDivideBy : 0.0);
            agResult.setPairReachThroughBackup(numToDivideBy > 0 ? agResult.getPairReachThroughBackup() / numToDivideBy : 0.0);
            agResult.setReachability(numToDivideBy > 0 ? agResult.getReachability() / numToDivideBy : 0.0);
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


    public String createAggregationOutput(AggregationParameters agParams, Map<String, AggregateAnalysis> outputMap) {

        List<String> topologyIds = agParams.getTopologyIds();
        List<RoutingType> routingTypes = agParams.getRoutingTypes();
        List<FailureScenario> failureScenarios = agParams.getFailureScenarios();
        List<Integer> nfeValues = agParams.getNfeValues();
        Map<RoutingType, List<Algorithm>> algorithmMap = agParams.getAlgorithmMap();
        Set<Algorithm> algorithmsThatCanIgnoreFailures = agParams.getAlgorithmsThatCanIgnoreFailures();
        Map<RoutingType, List<TrafficCombinationType>> trafficCombinationTypeMap = agParams.getTrafficCombinationTypeMap();
        Map<RoutingType, List<RoutingDescription>> routingDescriptionMap = agParams.getRoutingDescriptionMap();

        List<String> fileNames = new ArrayList<>();
        for(String topoId : topologyIds) {
            for (RoutingType routingType : routingTypes) {
                // CSV per Topology-RoutingType-FailureScenario combo
                List<Algorithm> algorithms = algorithmMap.get(routingType);
                List<TrafficCombinationType> trafficCombinationTypes = trafficCombinationTypeMap.get(routingType);
                List<RoutingDescription> routingDescriptions = routingDescriptionMap.get(routingType);
                for(FailureScenario failureScenario : failureScenarios) {
                    // Make a CSV
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
                                        output.addAll(createLines(aggregateAnalysis));
                                    }
                                }
                            }
                        }
                    }
                    try {
                        String name = "results/aggregated/" + topoId + "_" + routingType + "_" + failureScenario.toString();
                        fileNames.add(name + ".csv");
                        CSVWriter writer = new CSVWriter(new FileWriter(name + ".csv"), ',');
                        writer.writeAll(output);
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return fileNames.toString();
    }

    /*
        private Double totalFeasible;
        private Double percentFeasible;

        private Double runningTime;
        private Double totalCost;
        private Double totalLinksUsed;
        private Double totalPaths;
        private Double averagePrimaryHops;
        private Double averagePrimaryCost;

        private Double averagePathRisk;
        private Double averageMinRiskPerPair;
        private Double averageMaxRiskPerPair;
        private Double averagePrimaryRisk;

        // After failure simulation
        private Double averagePrimaryHopsPostFailure;
        private Double averagePrimaryCostPostFailure;
        private Double pathsSevered;
        private Double pathsIntact;
        private Double connectionsSevered;
        private Double connectionsIntact;

        // Caching
        private List<CachingResult> cachingResults;
     */
    private List<String[]> createLines(AggregateAnalysis agAnalysis) {
        RoutingDescription rd = agAnalysis.getRoutingDescription();
        String[] header = makeArray( "S: " +rd.getNumSources(), "D: " + rd.getNumDestinations(),
                "MinS: " + rd.getUseMinS(), "MaxS: " + rd.getUseMaxS(), "MinD: " + rd.getUseMinD(), "MaxD: " + rd.getUseMaxD());
        String[] metricHeader = makeMetricHeader(agAnalysis.getCachingResults());
        String[] data = makeDataLine(agAnalysis);
        makeArray(agAnalysis.getRunningTime(), agAnalysis.getTotalCost(), agAnalysis.getTotalLinksUsed(),
                agAnalysis.getTotalPaths(), agAnalysis.getAveragePrimaryHops(), agAnalysis.getAveragePrimaryCost(),
                agAnalysis.getAveragePrimaryHopsPostFailure(), agAnalysis.getAveragePrimaryCostPostFailure(),
                agAnalysis.getPathsSevered(), agAnalysis.getPathsIntact(), agAnalysis.getConnectionsSevered(),
                agAnalysis.getConnectionsIntact());
        return Arrays.asList(header, metricHeader, data, new String[]{});
    }

    private String[] makeMetricHeader(List<CachingResult> cachingResults){
        List<String> headers = Arrays.asList("%Feasible", "runningTime", "totalCost", "totalLinksUsed", "totalPaths", "avgPrimaryHops",
                "avgPrimaryCost", "avgPathRisk", "avgMinRisk", "avgMaxRisk", "avgPrimaryRisk",
                "avgPrimaryHopsF", "avgPrimaryCostF", "pathsSevered", "pathsIntact", "connsSevered", "connsIntact");
        List<String> cachingHeaders = new ArrayList<>();
        for(CachingResult cachingResult : cachingResults){
            String cType = cachingResult.getType().getCode();
            cachingHeaders.add(cType + "_" + "cCost");
            cachingHeaders.add(cType + "_" + "cReach");
            //cachingHeaders.add(cType + "_" + "cAccess");
            cachingHeaders.add(cType + "_" + "cHop");
            cachingHeaders.add(cType + "_" + "cBackup");
        }
        List<String> combined = new ArrayList<>(headers);
        combined.addAll(cachingHeaders);
        return combined.toArray(new String[headers.size()]);
    }

    private String[] makeDataLine(AggregateAnalysis ag){
        List<String> dataList = makeList(ag.getPercentFeasible(), ag.getRunningTime(), ag.getTotalCost(), ag.getTotalLinksUsed(),
                ag.getTotalPaths(), ag.getAveragePrimaryHops(), ag.getAveragePrimaryCost(),
                ag.getAveragePathRisk(), ag.getAverageMinRiskPerPair(), ag.getAverageMaxRiskPerPair(), ag.getAveragePrimaryRisk(),
                ag.getAveragePrimaryHopsPostFailure(), ag.getAveragePrimaryCostPostFailure(), ag.getPathsSevered(),
                ag.getPathsIntact(), ag.getConnectionsSevered(), ag.getConnectionsIntact());
        for(CachingResult cachingResult : ag.getCachingResults()){
            dataList.add(String.valueOf(cachingResult.getCachingCost()));
            dataList.add(String.valueOf(cachingResult.getReachability()));
            //dataList.add(String.valueOf(cachingResult.getAvgAccessibility()));
            dataList.add(String.valueOf(cachingResult.getAvgHopCountToContent()));
            dataList.add(String.valueOf(cachingResult.getPairReachThroughBackup()));
        }
        return dataList.toArray(new String[dataList.size()]);
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
