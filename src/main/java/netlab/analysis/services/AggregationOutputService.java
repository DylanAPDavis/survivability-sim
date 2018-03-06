package netlab.analysis.services;


import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AggregateAnalysis;
import netlab.analysis.analyzed.AggregationParameters;
import netlab.analysis.analyzed.CachingResult;
import netlab.analysis.analyzed.RoutingDescription;
import netlab.analysis.enums.CachingType;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureScenario;
import netlab.submission.enums.RoutingType;
import netlab.submission.enums.TrafficCombinationType;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AggregationOutputService {

    private final String primaryCost = "Primary Cost";
    private final String avgBackupCost = "Avg. Backup Cost";
    private final String totalPaths = "Total paths";
    private final String runningTime = "Running Time";
    private final String destsConnected = "Connected D";
    private final String primaryIntact = "Primary Intact";
    private final String connectionsIntact = "Connection Intact";
    private final String postFailureCost = "Post Failure Cost";
    private final String reachOnPrimary = "Reach on Primary";
    private final String reachOnBackup = "Reach on Backup";
    private final String reachOnlyBackup = "Reach only Backup";
    private final String beforeHopsContent = "Content Hops Before";
    private final String afterHopsContent = "Content Hops After";
    private final String cachingCost = "Caching Cost";

    List<Algorithm> algs = Arrays.asList(Algorithm.ILP, Algorithm.FlexBhandari, Algorithm.Tabu, Algorithm.MinimumCostPath,
            Algorithm.MinimumRiskPath, Algorithm.Yens, Algorithm.Bhandari);


    private HashingService hashingService;


    public AggregationOutputService(HashingService hashingService){
        this.hashingService = hashingService;
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

        List<FailureScenario> failureScenarios = Arrays.asList(FailureScenario.AllLinks, FailureScenario.Quake_2);
        List<Integer> nfeValues = Arrays.asList(1, 2);
        List<Integer> numD = Arrays.asList(1, 2, 3);
        Map<Algorithm, Integer> algOrder = new HashMap<>();
        for(int i = 0; i < algs.size(); i++){
            algOrder.put(algs.get(i), i);
        }
        List<String> topologies = Arrays.asList("tw");


        Map<String, Map<Integer, List<AggregateAnalysis>>> tableMap = buildTableMap(outputMap);

        List<CachingType> cachingTypes = Arrays.asList(CachingType.EntirePath, CachingType.LeaveCopyDown,
                CachingType.SourceAdjacent, CachingType.FailureAware, CachingType.BranchingPoint);

        List<String> beforeMetrics = Arrays.asList(totalPaths, destsConnected, primaryCost, avgBackupCost, runningTime);
        List<String> afterMetrics = Arrays.asList(primaryIntact, connectionsIntact, postFailureCost);
        List<String> cachingMetrics = Arrays.asList(reachOnPrimary, reachOnBackup, reachOnlyBackup, beforeHopsContent,
                afterHopsContent, cachingCost);

        Map<Algorithm, String> algFormatMap = createAlgFormatMap(algs);
        Map<CachingType, String> cacheFormatMap = createCacheFormatMap(cachingTypes);

        Map<Integer, List<String>> metricCategories = new HashMap<>();
        metricCategories.put(0, beforeMetrics);
        metricCategories.put(1, afterMetrics);
        metricCategories.put(2, cachingMetrics);


        DecimalFormat bigFormat = new DecimalFormat("####.##");

        DecimalFormat littleFormat = new DecimalFormat("####.###");

        List<String[]> output = new ArrayList<>();
        List<String[]> altOutput = new ArrayList<>();
        for(String topology : topologies) {
            for (FailureScenario failureScenario : failureScenarios) {
                List<List<String[]>> linesPerNfe = new ArrayList<>();
                for (Integer nfe : nfeValues) {
                    List<String[]> tempOutput = new ArrayList<>();
                    output.add(new String[]{"------"});
                    output.add(new String[]{"------"});
                    tempOutput.add(new String[]{"------"});
                    tempOutput.add(new String[]{"------"});
                    String hash = hashingService.hash(topology, failureScenario.getCode(), nfe);
                    Map<Integer, List<AggregateAnalysis>> mapForTable = tableMap.get(hash);
                    for(int categoryNumber = 0; categoryNumber < metricCategories.size(); categoryNumber++) {
                        // Add header line for this table
                        output.add(makeArray(topology, failureScenario.getCode(), nfe, determineCategory(categoryNumber)));
                        tempOutput.add(makeArray(topology, failureScenario.getCode(), nfe, determineCategory(categoryNumber)));
                        List<String> metrics = metricCategories.get(categoryNumber);
                        if(categoryNumber == 2){
                            // Caching Metrics
                            // Only do this for anycast 1/3
                            for(CachingType cachingType : cachingTypes) {
                                String[] cachingHeader = makeCachingHeader(algs, cachingType, algFormatMap, cacheFormatMap);
                                output.add(cachingHeader);
                                tempOutput.add(cachingHeader); // add alt & output
                                List<AggregateAnalysis> agForD = mapForTable.get(3);
                                // Sort these analyses by which algorithm they're using
                                agForD.sort(Comparator.comparing(ag -> algOrder.get(ag.getAlgorithm())));
                                // Get the lines for the caching metrics
                                List<String[]> metricsLines = makeMetricLines(metrics, agForD, bigFormat, littleFormat, cachingType);
                                for(String[] metricLine : metricsLines){
                                    output.add(metricLine);
                                    tempOutput.add(metricLine); // add alt & output
                                }
                                tempOutput.add(new String[]{"\\hline"});
                            }
                        } else {
                            for (Integer d : numD) {
                                // Sub header for Anycast # and Algorithms
                                String[] anycastHeader = makeAnycastHeader(algs, d, algFormatMap);
                                output.add(anycastHeader);
                                tempOutput.add(anycastHeader); // add alt & output
                                List<AggregateAnalysis> agForD = mapForTable.get(d);
                                // Sort these analyses by which algorithm they're using
                                 agForD.sort(Comparator.comparing(ag -> algOrder.get(ag.getAlgorithm())));
                                // For each line: metric - value for Ag 1 - value for Ag 2 - value for Ag 3 - ...
                                List<String[]> metricsLines = makeMetricLines(metrics, agForD, bigFormat, littleFormat, null);
                                for(String[] metricLine : metricsLines){
                                    output.add(metricLine);
                                    tempOutput.add(metricLine); // add alt & output
                                }
                                tempOutput.add(new String[]{"\\hline"});
                            }
                        }
                        output.add(new String[]{});
                        tempOutput.add(new String[]{});
                        linesPerNfe.add(tempOutput);
                    }
                }
                // We now have metrics and headers for each NFE value for this topo-failure scenario
                // Stored in linesPerNfe
                // Take each of these lines, and merge them together
                // Ignore first column for later nfes
                List<String[]> outputForZero = linesPerNfe.get(0);
                for(int i = 0; i < outputForZero.size(); i++){
                    String[] line = outputForZero.get(i);
                    List<String> combinedLine = new ArrayList<>();
                    for(String component : line){
                        combinedLine.add(component);
                    }
                    for(int nfeI = 1; nfeI < nfeValues.size(); nfeI++){
                        String[] adjacentLine = linesPerNfe.get(nfeI).get(i);
                        for(int adjacentI = 0; adjacentI < adjacentLine.length; adjacentI++){
                            if(i != 0 && adjacentI == 0){
                                continue;
                            }
                            combinedLine.add(adjacentLine[adjacentI]);
                        }
                    }
                    // Now we've got a combined line, have to append with ampersand
                    String[] joined = joinWithAmpersand(combinedLine, true);
                    altOutput.add(joined);
                }

            }
        }
        try {
            CSVWriter writer =  new CSVWriter(new FileWriter(fileName + ".csv"), ',');
            writer.writeAll(output);
            writer.close();
            writer = new CSVWriter(new FileWriter("alt" + fileName + ".csv"), ',');
            writer.writeAll(altOutput);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileName;
    }

    private Map<CachingType,String> createCacheFormatMap(List<CachingType> types) {
        Map<CachingType, String> algFormatMap = new HashMap<>();
        for(CachingType type : types){
            switch(type){
                case EntirePath:
                    algFormatMap.put(type, "EP");
                    break;
                case LeaveCopyDown:
                    algFormatMap.put(type, "LCD");
                    break;
                case SourceAdjacent:
                    algFormatMap.put(type, "CA");
                    break;
                case FailureAware:
                    algFormatMap.put(type, "FA");
                    break;
                case BranchingPoint:
                    algFormatMap.put(type, "BP");
                    break;
            }
        }
        return algFormatMap;
    }

    private Map<Algorithm,String> createAlgFormatMap(List<Algorithm> algs) {
        Map<Algorithm, String> algFormatMap = new HashMap<>();
        for(Algorithm alg : algs){
            switch(alg){
                case ILP:
                    algFormatMap.put(alg, "LP");
                    break;
                case FlexBhandari:
                    algFormatMap.put(alg, "FB");
                    break;
                case Tabu:
                    algFormatMap.put(alg, "Tabu");
                    break;
                case MinimumCostPath:
                    algFormatMap.put(alg, "MinDist");
                    break;
                case MinimumRiskPath:
                    algFormatMap.put(alg, "MinRisk");
                    break;
                case Yens:
                    algFormatMap.put(alg, "Yen");
                    break;
                case Bhandari:
                    algFormatMap.put(alg, "Bhan");
                    break;
            }
        }
        return algFormatMap;
    }

    private String[] joinWithAmpersand(List<String> line, boolean end){
        String output = String.join(" & ", line);
        if(end){
            output += "\\\\ \\hline";
        }
        return new String[]{output};
    }

    private List<String[]> makeMetricLines(List<String> metrics, List<AggregateAnalysis> agForD, DecimalFormat bigFormat, DecimalFormat littleFormat,
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
                if(useBigFormat(metric)){
                    metricValueList.add(bigFormat.format(value));
                }else{
                    metricValueList.add(littleFormat.format(value));
                }
            }
            String[] metricLine = makeArrayFromList(metricValueList);
            output.add(metricLine);
        }
        return output;
    }

    private Boolean useBigFormat(String metric){
        switch(metric){
            case primaryCost:
                return true;
            case avgBackupCost:
                return true;
            case postFailureCost:
                return true;
            default:
                return false;
        }
    }

    private String[] makeCachingHeader(List<Algorithm> algs, CachingType cachingType, Map<Algorithm, String> algFormatMap,
                                       Map<CachingType, String> cacheFormatMap) {
        String cache = cacheFormatMap.get(cachingType);
        List<String> temp = algs.stream().map(algFormatMap::get).collect(Collectors.toList());
        temp.add(0, cache);
        return makeArrayFromList(temp);
    }

    private String[] makeAnycastHeader(List<Algorithm> algs, Integer d, Map<Algorithm, String> algFormatMap){
        String any = "1/" + d;
        List<String> temp = algs.stream().map(algFormatMap::get).collect(Collectors.toList());
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
            if(aggregateAnalysis.getIgnoreFailures() || !algs.contains(aggregateAnalysis.getAlgorithm())){
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
            case beforeHopsContent:
                return cachingResult.getAvgHopCountBefore();
            case afterHopsContent:
                return cachingResult.getAvgHopCountAfter();
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
