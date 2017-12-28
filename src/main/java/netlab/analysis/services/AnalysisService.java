package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.*;
import netlab.analysis.enums.CachingType;
import netlab.processing.pathmapping.PathMappingService;
import netlab.submission.enums.*;
import netlab.submission.request.*;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalysisService {

    private PathMappingService pathMappingService;
    private CachingService cachingService;

    @Autowired
    public AnalysisService( PathMappingService pathMappingService, CachingService cachingService) {
        this.pathMappingService = pathMappingService;
        this.cachingService = cachingService;
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
        List<CachingResult> cachingResults = Arrays.asList(
                new CachingResult(CachingType.None),
                new CachingResult(CachingType.SourceAdjacent),
                new CachingResult(CachingType.FailureAware),
                new CachingResult(CachingType.BranchingPoint),
                new CachingResult(CachingType.EntirePath)
        );

        cachingService.buildCacheMaps(cachingResults, chosenPaths, failureColl.getFailureSet());


        //Map for storing number of times a source/dest/pair sends a connection over a link
        Map<Link, Set<Node>> sourceLinkMap = new HashMap<>();
        Map<Link, Set<Node>> destLinkMap = new HashMap<>();

        for(SourceDestPair pair : chosenPaths.keySet()){
            // Sort in ascending order -> total path weight
            List<Path> pathsForPair = pathMappingService.sortPathsByWeight(new ArrayList<>(chosenPaths.get(pair).values()));
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
                List<Link> links = path.getLinks();
                for(Link link : links){
                    sourceLinkMap.putIfAbsent(link, new HashSet<>());
                    destLinkMap.putIfAbsent(link, new HashSet<>());

                    sourceLinkMap.get(link).add(pair.getSrc());
                    destLinkMap.get(link).add(pair.getDst());
                }
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
                List<Path> sortedIntactPaths = pathMappingService.sortPathsByWeight(intactPaths);
                Path newPrimary = sortedIntactPaths.get(0);
                totalPrimaryCostPostFailure += newPrimary.getTotalWeight();
                totalPrimaryHopsPostFailure += newPrimary.getLinks().size();
                numberOfPrimaryPathsPostFailure++;
            }
        }

        averagePrimaryCost = totalPrimaryCost / numberOfPrimaryPaths;
        averagePrimaryHops = totalPrimaryHops / numberOfPrimaryPaths;
        averagePrimaryCostPostFailure = numberOfPrimaryPathsPostFailure > 0 ? totalPrimaryCostPostFailure / numberOfPrimaryPathsPostFailure : 0;
        averagePrimaryHopsPostFailure = numberOfPrimaryPathsPostFailure > 0 ? totalPrimaryHopsPostFailure / numberOfPrimaryPathsPostFailure : 0;


        // Content analysis
        cachingService.evaluateContentAccessibility(cachingResults, chosenPaths, chosenFailures,
                details.getConnections().getUseMinD());

        Analysis analysis =  Analysis.builder()
                .requestId(request.getId())
                .seed(request.getSeed())
                .failureScenario(request.getFailureScenario())
                .numFailuresEvents(request.getDetails().getNumFailureEvents().getTotalNumFailureEvents())
                .algorithm(request.getAlgorithm())
                .routingType(request.getRoutingType())
                .trafficCombinationType(request.getTrafficCombinationType())
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
                .chosenFailures(convertFailuresToString(chosenFailures))
                .cachingResults(cachingResults)
                .build();

        if(!request.getTrafficCombinationType().equals(TrafficCombinationType.None)){
            adjustLinksUsedTotalCost(analysis, sourceLinkMap, destLinkMap, request.getTrafficCombinationType());
        }

        return analysis;
    }

    private List<String> convertFailuresToString(List<Failure> chosenFailures) {
        return chosenFailures.stream()
                .map(f -> f.getNode() == null ? f.getLink().getId() : f.getNode().getId())
                .collect(Collectors.toList());
    }

    private void adjustLinksUsedTotalCost(Analysis analysis, Map<Link, Set<Node>> sourceLinkMap,
                                          Map<Link, Set<Node>> destLinkMap,
                                          TrafficCombinationType trafficCombinationType) {
        Double totalCost = 0.0;
        Double totalLinksUsed = 0.0;

        for(Link link : sourceLinkMap.keySet()){
            int count = 0;
            switch(trafficCombinationType){
                case Source:
                    count = sourceLinkMap.get(link).size();
                    break;
                case Destination:
                    count = destLinkMap.get(link).size();
                    break;
                case Both:
                    count = Math.min(sourceLinkMap.get(link).size(), destLinkMap.get(link).size());
                    break;
            }
            totalLinksUsed += count;
            totalCost += link.getWeight() * count;
        }

        analysis.setTotalCost(totalCost);
        analysis.setTotalLinksUsed(totalLinksUsed);
    }

    private List<Failure> chooseFailuresBasedOnProb(List<List<Failure>> failureGroups) {
        if(failureGroups.isEmpty()){
            return new ArrayList<>();
        }
        Map<List<Failure>, Double> likelihoods = new HashMap<>();
        Map<List<Failure>, String> combinedIds = new HashMap<>();
        for(List<Failure> failureGroup : failureGroups){
            Double likelihood = 1.0;
            String combinedId = "";
            failureGroup.sort(Comparator.comparing(f -> f.getNode() == null ? f.getLink().getId() : f.getNode().getId()));
            for(Failure failure : failureGroup){
                likelihood *= failure.getProbability();
                combinedId += failure.getNode() != null ? failure.getNode().getId() : failure.getLink().getId();
            }
            likelihoods.put(failureGroup, likelihood);
            combinedIds.put(failureGroup, combinedId);
        }
        Comparator comparator = Comparator.comparingDouble(likelihoods::get).thenComparing(combinedIds::get);
        failureGroups.sort(comparator);

        return failureGroups.get(0);
    }

    private String invertLinkId(Link link) {
        return link.getTarget().getId() + "-" + link.getOrigin().getId();
    }



}
