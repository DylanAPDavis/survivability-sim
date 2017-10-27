package netlab.analysis.services;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.*;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.SimulationParameters;
import netlab.topology.elements.Topology;
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

    public AggregateAnalysis aggregateAnalyses(List<Analysis> analyses) {

        if (analyses.isEmpty()) {
            return AggregateAnalysis.builder().build();
        }

        List<String> requestSetIds = new ArrayList<>();
        List<Long> seeds = new ArrayList<>();
        ProblemClass problemClass = analyses.get(0).getProblemClass();
        Algorithm algorithm = analyses.get(0).getAlgorithm();
        Objective objective = analyses.get(0).getObjective();
        FailureClass failureClass = analyses.get(0).getFailureClass();

        Double sumRunningTime = 0.0;
        Double numFeasible = 0.0;
        Double sumLinksUsed = 0.0;
        Double sumCost = 0.0;
        Double sumPaths = 0.0;
        Double sumAveragePrimaryHops = 0.0;
        Double sumAveragePrimaryCost = 0.0;
        Double sumAveragePrimaryHopsPostFailure = 0.0;
        Double sumAveragePrimaryCostPostFailure = 0.0;
        Double sumPathsSevered = 0.0;
        Double sumPathsIntact = 0.0;
        Double sumConnectionsSevered = 0.0;
        Double sumConnectionsIntact = 0.0;


        int numRequests = 0;
        for (Analysis analysis : analyses) {
            if (analysis == null){
                continue;
            }
            numRequests++;
            requestSetIds.add(analysis.getRequestId());
            seeds.add(analysis.getSeed());
            if(analysis.getIsFeasible()){
                numFeasible++;
                sumRunningTime += analysis.getRunningTime();
                sumLinksUsed += analysis.getTotalLinksUsed();
                sumCost += analysis.getTotalCost();
                sumPaths += analysis.getTotalPaths();
                sumAveragePrimaryCost += analysis.getAveragePrimaryCost();
                sumAveragePrimaryHops += analysis.getAveragePrimaryHops();
                sumAveragePrimaryCostPostFailure += analysis.getAveragePrimaryCostPostFailure();
                sumAveragePrimaryHopsPostFailure += analysis.getAveragePrimaryHopsPostFailure();
                sumPathsSevered += analysis.getPathsSevered();
                sumPathsIntact += analysis.getPathsIntact();
                sumConnectionsSevered += analysis.getConnectionsSevered();
                sumConnectionsIntact += analysis.getConnectionsIntact();
            }
        }


        return AggregateAnalysis.builder()
                .requestSetIds(requestSetIds)
                .seeds(seeds)
                .problemClass(problemClass)
                .algorithm(algorithm)
                .objective(objective)
                .failureClass(failureClass)
                .numRequests(numRequests)
                .totalFeasible(numFeasible)
                .percentFeasible(numFeasible / numRequests)
                .runningTime(sumRunningTime /numFeasible)
                .totalCost(sumCost / numFeasible)
                .totalLinksUsed(sumLinksUsed / numFeasible)
                .totalPaths(sumPaths / numFeasible)
                .averagePrimaryHops(sumAveragePrimaryHops / numFeasible)
                .averagePrimaryCost(sumAveragePrimaryCost / numFeasible)
                .averagePrimaryHopsPostFailure(sumAveragePrimaryHopsPostFailure / numFeasible)
                .averagePrimaryCostPostFailure(sumAveragePrimaryCostPostFailure / numFeasible)
                .pathsSevered(sumPathsSevered / numFeasible)
                .pathsIntact(sumPathsIntact / numFeasible)
                .connectionsSevered(sumConnectionsSevered / numFeasible)
                .connectionsIntact(sumConnectionsIntact / numFeasible)
                .build();
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
            String hashString = hashingService.makeAggregationHash(params);
            outputMap.putIfAbsent(hashString, aggSet);
        }
        createAggregationOutput(agParams, outputMap);
        return "";
    }


    private void createAggregationOutput(AggregationParameters agParams, Map<String, AggregateAnalysis> outputMap) {
        List<Topology> topologies = agParams.getTopologies();
        List<Objective> objectives = agParams.getObjectives();
        List<FailureDescription> failureDescriptions = agParams.getFailureDescriptions();
        List<RoutingDescription> routingDescriptions = agParams.getRoutingDescriptions();
        List<Integer> numThreads = agParams.getNumThreads();
        for(Topology topology : topologies) {
            for (Integer threads : numThreads) {
                for (Objective objective : objectives) {
                    // CSV per Topology-Objective-FailureDescription combo
                    for (FailureDescription failureDescription : failureDescriptions) {
                        // Make a CSV
                        // Write each routing description to the CSV
                        List<String[]> output = new ArrayList<>();
                        for (RoutingDescription routingDescription : routingDescriptions) {
                            // Get the matching results from the outputMap
                            String hash = hashingService.makeAggregationHash(topology, threads, objective, failureDescription, routingDescription);
                            AggregateAnalysis aggregateAnalysis = outputMap.get(hash);
                            output.addAll(createLines(routingDescription, aggregateAnalysis));
                        }
                        try {
                            String name = topology.getId() + "_" + threads + "_" + objective + "_" + failureDescription.toString();
                            CSVWriter writer = new CSVWriter(new FileWriter(name + ".csv"), ',');
                            writer.writeAll(output);
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private List<String[]> createLines(RoutingDescription routingDescription, AggregateAnalysis agAnalysis) {
        String[] header = makeArray(routingDescription.getRoutingType(), routingDescription.getProblemClass(),
                routingDescription.getAlgorithm(), "S: " +routingDescription.getNumSources(), "D: " + routingDescription.getNumDestinations(),
                "MinS: " + routingDescription.getUseMinS(), "MaxS: " + routingDescription.getUseMaxS(),
                "MinD: " + routingDescription.getUseMinD(), "MaxD: " + routingDescription.getUseMaxD(),
                "TCombo: " + routingDescription.getTrafficCombinationType(), "S n D: " + routingDescription.getPercentSrcAlsoDest());
        String[] metricHeader = makeArray("runningTime", "totalCost", "totalLinksUsed", "totalPaths", "avgPrimaryHops",
                "avgPrimaryCost", "avgPrimaryHopsF", "avgPrimaryCost", "pathsSevered", "pathsIntact", "connsSevered", "connsIntact");
        String[] data = makeArray(agAnalysis.getRunningTime(), agAnalysis.getTotalCost(), agAnalysis.getTotalLinksUsed(),
                agAnalysis.getTotalPaths(), agAnalysis.getAveragePrimaryHops(), agAnalysis.getAveragePrimaryCost(),
                agAnalysis.getAveragePrimaryHopsPostFailure(), agAnalysis.getAveragePrimaryCostPostFailure(),
                agAnalysis.getPathsSevered(), agAnalysis.getPathsIntact(), agAnalysis.getConnectionsSevered(),
                agAnalysis.getConnectionsIntact());
        return Arrays.asList(header, metricHeader, data, new String[]{});
    }


    private String[] makeArray(Object... args) {
        String[] line = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            line[i] = String.valueOf(args[i]);
        }
        return line;
    }
}
