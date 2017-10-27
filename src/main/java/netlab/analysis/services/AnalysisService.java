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

    private TopologyService topologyService;

    @Autowired
    public AnalysisService( TopologyService topologyService) {
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



}
