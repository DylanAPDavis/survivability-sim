package netlab.analysis.services;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.*;
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

    public Analysis analyzeRequest(Request request) {

        Details details = request.getDetails();
        Map<SourceDestPair, Map<String, Path>> chosenPaths = details.getChosenPaths();
        Failures failureColl = details.getFailures();


        List<List<Failure>> failureGroups = failureColl.getFailureGroups();
        Double totalCost = 0.0;
        Double totalPaths = 0.0;
        Double totalLinksUsed = 0.0;
        Double averagePrimaryHops;
        Double averagePrimaryCost;

        // After failure simulation
        Double averagePrimaryHopsPostFailure;
        Double averagePrimaryCostPostFailure;
        Double pathsSevered = 0.0;
        Double pathsIntact = 0.0;
        Double connectionsSevered = 0.0;
        Double connectionsIntact = 0.0;

        // Use to calculate averages
        Double totalPrimaryHops = 0.0;
        Double totalPrimaryCost = 0.0;
        Double totalPrimaryHopsPostFailure = 0.0;
        Double totalPrimaryCostPostFailure = 0.0;
        Double numberOfPrimaryPaths = 0.0;
        Double numberOfPrimaryPathsPostFailure = 0.0;

        List<Failure> chosenFailures = chooseFailuresBasedOnProb(failureGroups);
        for(SourceDestPair pair : chosenPaths.keySet()){
            // Sort in ascending order -> total path weight
            List<Path> pathsForPair = sortPathsByWeight(chosenPaths.get(pair).values());
            if(pathsForPair.size() == 0){
                continue;
            }
            // Get data on primary path
            Path primaryPath = pathsForPair.get(0);
            totalPrimaryHops += primaryPath.getLinks().size();
            totalPrimaryCost += primaryPath.getTotalWeight();
            numberOfPrimaryPaths++;

            double numPathsSevered = 0.0;
            double numPathsIntact = 0.0;
            List<Path> intactPaths = new ArrayList<>();
            for(Path path : pathsForPair){
                totalCost += path.getTotalWeight();
                totalPaths++;
                totalLinksUsed += path.getLinks().size();
                if(path.containsFailures(chosenFailures)){
                    numPathsSevered++;
                }
                else{
                    numPathsIntact++;
                    intactPaths.add(path);
                }
            }
            if(numPathsSevered == pathsForPair.size()){
                connectionsSevered++;
            }
            else{
                connectionsIntact++;
            }
            pathsSevered += numPathsSevered;
            pathsIntact += numPathsIntact;

            // Get new primary for intact paths
            if(intactPaths.size() > 0){
                List<Path> sortedIntactPaths = sortPathsByWeight(intactPaths);
                Path newPrimary = sortedIntactPaths.get(0);
                totalPrimaryCostPostFailure += newPrimary.getTotalWeight();
                totalPrimaryHopsPostFailure += newPrimary.getLinks().size();
                numberOfPrimaryPathsPostFailure++;
            }
        }

        averagePrimaryCost = totalPrimaryCost / numberOfPrimaryPaths;
        averagePrimaryHops = totalPrimaryHops / numberOfPrimaryPaths;
        averagePrimaryCostPostFailure = totalPrimaryCostPostFailure / numberOfPrimaryPathsPostFailure;
        averagePrimaryHopsPostFailure = totalPrimaryHopsPostFailure / numberOfPrimaryPathsPostFailure;


        return Analysis.builder()
                .requestId(request.getId())
                .seed(request.getSeed())
                .problemClass(request.getProblemClass())
                .algorithm(request.getAlgorithm())
                .objective(request.getObjective())
                .failureClass(request.getFailureClass())
                .isFeasible(details.getIsFeasible())
                .runningTime(details.getRunningTimeSeconds())
                .totalCost(totalCost)
                .totalLinksUsed(totalLinksUsed)
                .totalPaths(totalPaths)
                .averagePrimaryHops(averagePrimaryHops)
                .averagePrimaryCost(averagePrimaryCost)
                .averagePrimaryHopsPostFailure(averagePrimaryHopsPostFailure)
                .averagePrimaryCostPostFailure(averagePrimaryCostPostFailure)
                .pathsSevered(pathsSevered)
                .pathsIntact(pathsIntact)
                .connectionsSevered(connectionsSevered)
                .connectionsIntact(connectionsIntact)
                .build();
    }

    private List<Path> sortPathsByWeight(Collection<Path> paths){
        return paths.stream()
                .sorted(Comparator.comparingLong(Path::getTotalWeight))
                .collect(Collectors.toList());
    }

    private List<Failure> chooseFailuresBasedOnProb(List<List<Failure>> failureGroups) {
        if(failureGroups.isEmpty()){
            return new ArrayList<>();
        }
        Map<List<Failure>, Double> likelihoods = new HashMap<>();
        Map<List<Failure>, String> combinedIds = new HashMap<>();
        for(List<Failure> failureGroup : failureGroups){
            Double likelihood = 0.0;
            String combinedId = "";
            for(Failure failure : failureGroup){
                likelihood *= failure.getProbability();
                combinedId += failure.getNode() != null ? failure.getNode().getId() : failure.getLink().getId();
            }
            likelihoods.put(failureGroup, likelihood);
            combinedIds.put(failureGroup, combinedId);
        }
        List<List<Failure>> sortedGroups = failureGroups.stream()
                .sorted(Comparator.comparingDouble(likelihoods::get))
                .sorted(Comparator.comparing(combinedIds::get))
                .collect(Collectors.toList());

        return sortedGroups.get(0);
    }

    private String invertLinkId(Link link) {
        return link.getTarget().getId() + "-" + link.getOrigin().getId();
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
            String hashString = hashParams(params);
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

    private String hashParams(SimulationParameters params) {
        return hashingService.hash(params.getTopologyId(), params.getProblemClass(), params.getObjective(),
                params.getAlgorithm(),params.getNumSources(), params.getNumDestinations(), params.getMinConnections(),
                params.getMinPairConnections(), params.getMaxPairConnections(), params.getMinSrcConnections(),
                params.getMaxSrcConnections(), params.getMinDstConnections(), params.getMaxDstConnections(),
                params.getUseMinS(), params.getUseMaxS(), params.getUseMinD(), params.getUseMaxD(),
                params.getFailureSetSize(),  params.getFailureClass(), params.getFailureProb(),
                params.getNumFailureEvents(), params.getPercentSrcAlsoDest(), params.getPercentSrcFail(),
                params.getPercentDstFail(), params.getIgnoreFailures());
    }

    /*private String makeHash(SimulationParameters p) {
        return hashingService.hash(p.getTopologyId(), p.getAlgorithm(), p.getProblemClass(), p.getObjective(),
                String.valueOf(p.getPercentSrcAlsoDest()), p.getFailureClass(), String.valueOf(p.getFailureSetSize()),
                String.valueOf(p.getNumFailureEvents()), String.valueOf(p.getPercentSrcFail()),
                String.valueOf(p.getPercentDstFail()), String.valueOf(p.getIgnoreFailures()), String.valueOf(p.getMinConnections()),
                String.valueOf(p.getMinPairConnections()), String.valueOf(p.getMaxPairConnections()),
                String.valueOf(p.getNumSources()), String.valueOf(p.getNumDestinations()));
    }*/


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
