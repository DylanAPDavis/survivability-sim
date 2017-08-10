package netlab.processing.disjointpaths;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.Connections;
import netlab.submission.request.Failures;
import netlab.submission.request.NumFailsAllowed;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.transform.Source;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PartialBhandariService {

    private BhandariService bhandariService;

    private BellmanFordService bellmanFordService;

    @Autowired
    public PartialBhandariService(BhandariService bhandariService, BellmanFordService bellmanFordService){
        this.bhandariService = bhandariService;
        this.bellmanFordService = bellmanFordService;
    }


    public Request solve(Request request, ProblemClass problemClass, Objective objective, Topology topology, String requestSetId) {
        Map<SourceDestPair, Map<String, Path>> paths = new HashMap<>();
        Set<SourceDestPair> pairs = request.getPairs();
        Failures failCollection = request.getFailures();
        NumFailsAllowed nfaCollection = request.getNumFailsAllowed();
        Connections connCollection = request.getConnections();
        long startTime = System.nanoTime();
        switch(problemClass){
            case Flex:
                paths = pathsForFlex(pairs, failCollection.getFailureSet(),
                        nfaCollection.getTotalNumFailsAllowed(), connCollection.getNumConnections(), topology);
            //TODO: Implement Flow
            //TODO: Implement Endpoint
            //TODO: Implement FlowSharedF
            //TODO: Implement EndpointSharedF
        }
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        log.info("Solution took: " + duration + " seconds");
        request.setChosenPaths(paths);
        request.setRunningTimeSeconds(duration);
        return request;
    }

    private Map<SourceDestPair,Map<String,Path>> pathsForFlex(Set<SourceDestPair> pairs, Set<Failure> failureSet,
                                                              Integer totalNumFailsAllowed, Integer numConnections, Topology topology) {

        Map<SourceDestPair, Map<String, Path>> pathMap = new HashMap<>();
        List<SourceDestPair> sortedPairs = sortPairsByPathCost(pairs, topology);
        boolean nodesCanFail = failureSet.stream().anyMatch(f -> f.getNode() != null);
        for(SourceDestPair pair : sortedPairs){
            List<List<Link>> pathLinks = bhandariService.computeDisjointPaths(topology, pair.getSrc(), pair.getDst(), numConnections, totalNumFailsAllowed, nodesCanFail, failureSet);
            System.out.println(pathLinks);
            //TODO: Add these to a path map
            List<Path> paths = convertToPaths(pathLinks);
            paths = sortPathsByWeight(paths);
            //TODO: Determine if sufficient number of connections have been established. If not, continue looping through pairs.
        }
        return new HashMap<>();
    }

    private List<List<Link>> sortPathsByWeight(List<Path> paths) {
    }

    private List<Path> convertToPaths(List<List<Link>> pathLinks){
        return pathLinks.stream().map(Path::new).collect(Collectors.toList());
    }

    private List<SourceDestPair> sortPairsByPathCost(Set<SourceDestPair> pairs, Topology topology) {
        Map<SourceDestPair, Long> leastCostMap = new HashMap<>();
        for(SourceDestPair pair : pairs){
            List<Link> leastCostPath = bellmanFordService.shortestPath(topology, pair.getSrc(), pair.getDst());
            Long weight = leastCostPath.stream().map(Link::getWeight).reduce(0L, (li1, li2) -> li1 + li2);
            leastCostMap.put(pair, weight);
        }
        return pairs.stream().sorted(Comparator.comparing((leastCostMap::get))).collect(Collectors.toList());
    }


}
