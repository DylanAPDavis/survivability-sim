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

        Map<String, Failure> failureIdMap = failureColl.getFailureSet().stream()
                .collect(Collectors.toMap(Failure::getId, f -> f));

        List<List<Failure>> failureGroups = failureColl.getFailureGroups();

        Map<Node, Set<Path>> pathsPerSrc = pathMappingService.getPathsPerSrc(chosenPaths);
        Map<Node, Path> primaryPathPerSrc = pathMappingService.getPrimaryPathPerSrc(pathsPerSrc);
        int numSrcs = primaryPathPerSrc.size();


        List<Failure> chosenFailures = chooseFailuresBasedOnProb(failureGroups);
        List<CachingResult> cachingResults = Arrays.asList(
                new CachingResult(CachingType.None),
                new CachingResult(CachingType.SourceAdjacent),
                new CachingResult(CachingType.FailureAware),
                new CachingResult(CachingType.BranchingPoint),
                new CachingResult(CachingType.EntirePath),
                new CachingResult(CachingType.LeaveCopyDown)
        );

        cachingService.buildCacheMaps(cachingResults, chosenPaths, failureColl.getFailureSet());


        Double totalCost = 0.0;
        Double totalLinksUsed = 0.0;
        Double totalPaths = 0.0;
        Double totalPrimaryPaths = 0.0;
        Double totalBackupPaths = 0.0;
        Double connectionsSevered = 0.0;
        Double connectionsIntact = 0.0;
        Double pathsSevered = 0.0;
        Double pathsIntact = 0.0;
        Double primaryPathsSevered = 0.0;
        Double primaryPathsIntact = 0.0;
        Double backupPathsSevered = 0.0;
        Double backupPathsIntact = 0.0;

        Double totalPrimaryHops = 0.0;
        Double totalPrimaryCost = 0.0;
        Double totalBackupHops = 0.0;
        Double totalBackupCost = 0.0;
        Double totalPrimaryHopsPostFailure = 0.0;
        Double totalPrimaryCostPostFailure = 0.0;
        Double totalPrimaryRisk = 0.0;
        Double totalBackupRisk = 0.0;

        //Map for storing number of times a source/dest/pair sends a connection over a link
        Map<Link, Set<Node>> sourceLinkMap = new HashMap<>();
        Map<Link, Set<Node>> destLinkMap = new HashMap<>();

        Set<Node> connectedDests = new HashSet<>();
        for(Node src : primaryPathPerSrc.keySet()){
            List<Path> allPaths = pathsPerSrc.get(src).stream()
                    .sorted(Comparator.comparing(Path::getTotalWeight))
                    .collect(Collectors.toList());
            boolean primarySevered = false;
            boolean foundIntactBackup = false;
            Double postFailureHops = 0.0;
            Double postFailureCost = 0.0;
            for(int i = 0; i < allPaths.size(); i++){
                 Path path = allPaths.get(i);
                 List<Link> pathLinks = path.getLinks();
                 Node dst = pathLinks.get(pathLinks.size()-1).getTarget();
                 connectedDests.add(dst);
                 for(Link link : pathLinks){
                     sourceLinkMap.putIfAbsent(link, new HashSet<>());
                     sourceLinkMap.get(link).add(src);
                     destLinkMap.putIfAbsent(link, new HashSet<>());
                     destLinkMap.get(link).add(dst);
                 }

                 Double cost = path.getTotalWeight();
                 totalCost += cost;
                 totalLinksUsed += pathLinks.size();
                 totalPaths++;
                 Double risk = calculatePathRisk(path, failureIdMap);

                // Evaluate how the path stands up to failure
                 boolean severed = path.containsFailures(chosenFailures);
                 if(severed){
                     pathsSevered++;
                 } else{
                     pathsIntact++;
                 }


                 // First path is the primary path
                 if(i == 0){
                     totalPrimaryPaths++;
                     totalPrimaryRisk += risk;
                     totalPrimaryHops += pathLinks.size();
                     totalPrimaryCost += cost;
                     if(severed){
                         primaryPathsSevered++;
                         primarySevered = true;
                     } else{
                         primaryPathsIntact++;
                         postFailureHops = 1.0 * path.getLinks().size();
                         postFailureCost = path.getTotalWeight();
                     }
                 }
                 else{
                     totalBackupPaths++;
                     totalBackupRisk += risk;
                     totalBackupHops += pathLinks.size();
                     totalBackupCost += cost;
                     if(severed){
                         backupPathsSevered++;
                     }
                     // If this backup path is not severed, and you haven't already picked a primary backup
                     // Take this as the backup path
                     else{
                         backupPathsIntact++;
                         if(!foundIntactBackup && primarySevered) {
                             foundIntactBackup = true;
                             postFailureHops = 1.0 * path.getLinks().size();
                             postFailureCost = path.getTotalWeight();
                         }
                     }
                 }
            }
            // If the primary path was severed, and you never found an intact backup path, then the connection is lost
            if(primarySevered && !foundIntactBackup){
                connectionsSevered++;
            }
            else{
                connectionsIntact++;
            }

            totalPrimaryHopsPostFailure += postFailureHops;
            totalPrimaryCostPostFailure += postFailureCost;

        }

        Double averagePrimaryHops = numSrcs > 0 ? totalPrimaryHops / numSrcs : 0.0;
        Double averagePrimaryCost = numSrcs > 0 ? totalPrimaryCost / numSrcs : 0.0;
        Double averagePrimaryRisk = numSrcs > 0 ? totalPrimaryRisk / numSrcs : 0.0;
        Double averageBackupHops = totalBackupPaths > 0 ? totalBackupHops / totalBackupPaths : 0.0;
        Double averageBackupCost = totalBackupPaths > 0 ? totalBackupCost / totalBackupPaths : 0.0;
        Double averageBackupRisk = totalBackupPaths > 0 ? totalBackupRisk / totalBackupPaths : 0.0;
        Double averageBackupPaths = numSrcs > 0 ? totalBackupPaths / numSrcs : 0.0;
        Double averagePrimaryHopsPostFailure = connectionsIntact > 0 ? totalPrimaryHopsPostFailure / connectionsIntact : 0.0;
        Double averagePrimaryCostPostFailure = connectionsIntact > 0 ? totalPrimaryCostPostFailure / connectionsIntact : 0.0;
        Double averageBackupPathsIntact = numSrcs > 0 ? backupPathsIntact / numSrcs : 0.0;
        Double averageBackupPathsSevered = numSrcs > 0 ? backupPathsSevered / numSrcs : 0.0;


        // Content analysis
        cachingService.evaluateContentAccessibility(cachingResults, chosenPaths, chosenFailures, details.getDestinations());

        Connections conns = details.getConnections();
        RoutingDescription routingDescription = new RoutingDescription(details.getSources().size(),
                details.getDestinations().size(), conns.getUseMinS(), conns.getUseMaxS(), conns.getUseMinD(),
                conns.getUseMaxD());

        Analysis analysis =  Analysis.builder()
                .requestId(request.getId())
                .seed(request.getSeed())
                .topologyId(request.getTopologyId())
                .failureScenario(request.getFailureScenario())
                .numFailuresEvents(request.getDetails().getNumFailureEvents().getTotalNumFailureEvents())
                .algorithm(request.getAlgorithm())
                .routingType(request.getRoutingType())
                .trafficCombinationType(request.getTrafficCombinationType())
                .ignoreFailures(request.isIgnoreFailures())
                .routingDescription(routingDescription)
                .isFeasible(details.getIsFeasible())
                .runningTime(details.getRunningTimeSeconds())
                .totalCost(totalCost)
                .totalLinksUsed(totalLinksUsed)
                .totalPaths(totalPaths)
                .totalPrimaryPaths(totalPrimaryPaths)
                .totalBackupPaths(totalBackupPaths)
                .connectionsSevered(connectionsSevered)
                .connectionsIntact(connectionsIntact)
                .primaryPathsSevered(primaryPathsSevered)
                .primaryPathsIntact(primaryPathsIntact)
                .pathsIntact(pathsIntact)
                .pathsSevered(pathsSevered)
                .destsConnected(1.0 * connectedDests.size())
                .averagePrimaryHops(averagePrimaryHops)
                .averagePrimaryCost(averagePrimaryCost)
                .averagePrimaryRisk(averagePrimaryRisk)
                .averageBackupHops(averageBackupHops)
                .averageBackupCost(averageBackupCost)
                .averageBackupRisk(averageBackupRisk)
                .averageBackupPaths(averageBackupPaths)
                .averagePrimaryHopsPostFailure(averagePrimaryHopsPostFailure)
                .averagePrimaryCostPostFailure(averagePrimaryCostPostFailure)
                .averageBackupPathsIntact(averageBackupPathsIntact)
                .averageBackupPathsSevered(averageBackupPathsSevered)
                .chosenFailures(convertFailuresToString(chosenFailures))
                .cachingResults(cachingResults)
                .build();

        if(!request.getTrafficCombinationType().equals(TrafficCombinationType.None)){
            adjustLinksUsedTotalCost(analysis, sourceLinkMap, destLinkMap, request.getTrafficCombinationType());
        }

        return analysis;

    }

    private double calculatePathRisk(Path path, Map<String, Failure> failureIdMap) {
        /*Set<String> pathIds = new HashSet<>(path.getNodeIds());
        pathIds.addAll(path.getLinkIds());
        pathIds.addAll(path.getLinks().stream().map(Link::reverse).map(Link::getId).collect(Collectors.toList()));*/
        // Compound risk probability -> risk at least one item in the path will fail
        double runningProb = 1.0;
        for(Node node : path.getNodes()){
            String id = node.getId();
            double weight = failureIdMap.containsKey(id) ? failureIdMap.get(id).getProbability() : 0.0;
            runningProb *= (1 - weight);
        }
        for(Link link : path.getLinks()){
            Link reverse = link.reverse();
            double weight = 0.0;
            if(failureIdMap.containsKey(link.getId())){
                weight = failureIdMap.get(link.getId()).getProbability();
            } else if(failureIdMap.containsKey(reverse.getId())){
                weight = failureIdMap.get(reverse.getId()).getProbability();
            }
            runningProb *= (1 - weight);
        }
        return 1.0 - runningProb;
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

        List<List<Failure>> sortedGroups = failureGroups.stream()
                .sorted(Comparator.comparingDouble(likelihoods::get).reversed().thenComparing(combinedIds::get))
                .collect(Collectors.toList());


        return sortedGroups.get(0);
    }

    private String invertLinkId(Link link) {
        return link.getTarget().getId() + "-" + link.getOrigin().getId();
    }



}
