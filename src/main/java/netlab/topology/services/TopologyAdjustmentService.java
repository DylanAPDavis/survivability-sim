package netlab.topology.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.TrafficCombinationType;
import netlab.topology.elements.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TopologyAdjustmentService {

    public Topology adjustWeightsUsingTrafficCombination(Topology topo, TrafficCombinationType trafficType, Node src,
                                                         Node dst, Map<Node, Set<Path>> srcPathsMap, Map<Node, Set<Path>> dstPathsMap){

        if(trafficType.equals(TrafficCombinationType.None)){
            return topo;
        }
        // Modify link weights if you're combining traffic
        Set<Path> zeroCostPaths = trafficType.equals(TrafficCombinationType.Source)
                || trafficType.equals(TrafficCombinationType.Both) ?
                srcPathsMap.getOrDefault(src, new HashSet<>()) : new HashSet<>();
        zeroCostPaths.addAll(trafficType.equals(TrafficCombinationType.Destination) || trafficType.equals(TrafficCombinationType.Both) ?
                dstPathsMap.getOrDefault(dst, new HashSet<>()) : new HashSet<>());
        Set<Link> zeroCostLinks = zeroCostPaths.stream().map(Path::getLinks).flatMap(List::stream).collect(Collectors.toSet());

        // Modify weights if you're combining traffic
        Set<Link> modifiedLinks = new HashSet<>();
        for(Link link : topo.getLinks()){
            Long weight = !trafficType.equals(TrafficCombinationType.None) && zeroCostLinks.contains(link) ?
                    0 : link.getWeight();
            Link modifiedLink = Link.builder().id(link.getId()).origin(link.getOrigin()).target(link.getTarget()).weight(weight).build();
            modifiedLinks.add(modifiedLink);
        }

        Topology newTopo = new Topology(topo.getId(), topo.getNodes(), modifiedLinks);
        newTopo.copyPathCosts(topo);
        return newTopo;
    }

    public List<SourceDestPair> sortPairsByPathCost(Collection<SourceDestPair> pairs, Topology topo){
        Map<SourceDestPair, Long> minimumPathCostMap = topo.getMinimumPathCostMap();
        return pairs
                .stream()
                .sorted(Comparator.comparing(minimumPathCostMap::get))
                .sorted(Comparator.comparing(p -> p.getSrc().getId()))
                .sorted(Comparator.comparing(p -> p.getDst().getId()))
                .collect(Collectors.toList());
    }

}
