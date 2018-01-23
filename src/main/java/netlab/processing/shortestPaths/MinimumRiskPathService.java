package netlab.processing.shortestPaths;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.pathmapping.PathMappingService;
import netlab.submission.enums.RoutingType;
import netlab.submission.enums.TrafficCombinationType;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MinimumRiskPathService {

    private MinimumCostPathService minimumCostPathService;
    private TopologyAdjustmentService topologyAdjustmentService;
    private PathMappingService pathMappingService;

    @Autowired
    public MinimumRiskPathService(MinimumCostPathService minimumCostPathService, TopologyAdjustmentService topologyAdjustmentService,
                                  PathMappingService pathMappingService){
        this.minimumCostPathService = minimumCostPathService;
        this.topologyAdjustmentService = topologyAdjustmentService;
        this.pathMappingService = pathMappingService;
    }

    public Details solve(Request request, Topology topo){
        Details details = request.getDetails();

        List<SourceDestPair> pairs = topologyAdjustmentService.sortPairsByPathCost(details.getPairs(), topo);
        Set<Failure> failures = request.getDetails().getFailures().getFailureSet();

        //Topology adjustedTopo = topologyAdjustmentService.adjustWeightsWithFailureProbs(topo, failures);
        long startTime = System.nanoTime();
        Map<Link, Double> riskWeightMap = pathMappingService.createRiskMap(topo.getLinks(), failures);
        Map<SourceDestPair, Map<String, Path>> pathMap = minimumCostPathService.findPaths(request.getDetails(),
                request.getRoutingType(), pairs, topo, TrafficCombinationType.None, false, riskWeightMap);
        pathMappingService.filterMapWithRisk(pathMap, details, riskWeightMap);
        //pathMappingService.setOriginalWeights(pathMap, topo.getLinkIdMap());
        long endTime = System.nanoTime();

        double duration = (endTime - startTime)/1e9;
        details.setChosenPaths(pathMap);
        details.setRunningTimeSeconds(duration);
        details.setIsFeasible(true);
        return details;
    }

}
