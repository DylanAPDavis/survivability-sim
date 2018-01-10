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


        Set<Link> modifiedLinks = modifyLinks(topo.getLinks(), !trafficType.equals(TrafficCombinationType.None), zeroCostLinks, 0L);

        return createTopologyWithLinkSubset(topo, modifiedLinks);
    }

    public Topology adjustWeightsToMax(Topology topo, Collection<Path> paths){
        Set<Link> pathLinks = paths.size() == 0 ? new HashSet<>() : paths.stream().map(Path::getLinks).flatMap(List::stream).collect(Collectors.toSet());
        return adjustWeightsToMaxWithLinks(topo, pathLinks);
    }

    public Topology adjustWeightsToMaxWithLinks(Topology topo, Set<Link> pathLinks){
        Set<Link> modifiedLinks = new HashSet<>(topo.getLinks());
        if(pathLinks.size() > 0) {
            Map<String, Link> linkIdMap = topo.getLinkIdMap();
            Set<Link> inverseLinks = pathLinks.stream()
                    .filter(l -> linkIdMap.containsKey(l.getTarget().getId() + "-" + l.getOrigin().getId()))
                    .map(l -> linkIdMap.get(l.getTarget().getId() + "-" + l.getOrigin().getId()))
                    .collect(Collectors.toSet());
            pathLinks.addAll(inverseLinks);
            modifiedLinks = modifyLinks(topo.getLinks(), true, pathLinks, Long.MAX_VALUE);
        }
        return createTopologyWithLinkSubset(topo, modifiedLinks);
    }

    public Topology adjustWeightsWithFailureProbs(Topology topo, Set<Failure> failures){
        Map<String, Long> weightMap = new HashMap<>();
        Map<String, Failure> failureIdMap = createFailureIdMap(failures);
        for(Link link : topo.getLinks()){
            String linkId = link.getId();
            String origin = link.getOrigin().getId();
            String target = link.getTarget().getId();
            double originProb = failureIdMap.containsKey(origin) ? failureIdMap.get(origin).getProbability() : 0;
            double targetProb = failureIdMap.containsKey(target) ? failureIdMap.get(target).getProbability() : 0;
            double linkProb = failureIdMap.containsKey(linkId) ? failureIdMap.get(linkId).getProbability() : 0;
            long newWeight = link.getWeight() + Math.round((link.getWeight() * originProb) + (link.getWeight() * linkProb) + (link.getWeight() * targetProb));
            weightMap.put(link.getId(), newWeight);
        }
        Set<Link> modifiedLinks = modifyLinks(topo.getLinks(), weightMap);
        return createTopologyWithLinkSubset(topo, modifiedLinks);
    }

    public Map<String, Failure> createFailureIdMap(Set<Failure> failures){
        return failures.stream().collect(Collectors.toMap(Failure::getId, f -> f));
    }

    public void readjustLinkWeights(Map<SourceDestPair, Map<String, Path>> chosenPathsMap, Topology sourceTopo) {
        Map<String, Link> sourceLinkIdMap = sourceTopo.getLinkIdMap();
        for(Map<String, Path> pathMap : chosenPathsMap.values()){
            for(String pathId : pathMap.keySet()){
                List<Link> readjustedLinks = new ArrayList<>(pathMap.get(pathId).getLinks());
                for(Link link : readjustedLinks){
                    Link sourceLink = sourceLinkIdMap.get(link.getId());
                    if(!Objects.equals(sourceLink.getWeight(), link.getWeight())){
                        link.setWeight(sourceLink.getWeight());
                    }
                }
                pathMap.put(pathId, new Path(readjustedLinks));
            }
        }
    }

    public List<List<Link>> readjustLinkWeights(List<List<Link>> pathLinks, Topology sourceTopo) {
        Map<String, Link> sourceLinkIdMap = sourceTopo.getLinkIdMap();
        List<List<Link>> adjustedPaths = new ArrayList<>();
        for(List<Link> path : pathLinks){
                List<Link> adjustedPath = new ArrayList<>(path);
                for(Link link : adjustedPath){
                    Link sourceLink = sourceLinkIdMap.get(link.getId());
                    if(!Objects.equals(sourceLink.getWeight(), link.getWeight())){
                        link.setWeight(sourceLink.getWeight());
                    }
                }
            adjustedPaths.add(adjustedPath);
        }
        return adjustedPaths;
    }

    public Set<Link> modifyLinks(Set<Link> links, boolean shouldModify, Set<Link> setToBeModified, Long newWeight){
        Set<Link> modifiedLinks = new HashSet<>();
        for(Link link : links){
            Long weight = shouldModify && setToBeModified.contains(link) ?
                    newWeight : link.getWeight();
            modifiedLinks.add(modifyLink(link, weight));
        }
        return modifiedLinks;
    }

    public Set<Link> modifyLinks(Set<Link> links, Map<String, Long> weightMap){
        Set<Link> modifiedLinks = new HashSet<>();
        for(Link link : links){
            Long weight = weightMap.get(link.getId());
            modifiedLinks.add(modifyLink(link, weight));
        }
        return modifiedLinks;
    }

    public Link modifyLink(Link link, Long weight){
        return new Link(link.getOrigin(), link.getTarget(), weight, link.getPoints());
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

    public Topology removeLinksFromTopology(Topology topo, Collection<Link> linksToRemove){
        Set<Link> newLinkSet = new HashSet<>();
        for(Link link : topo.getLinks()){
            if(!linksToRemove.contains(link)){
                newLinkSet.add(link);
            }
        }

        return createTopologyWithLinkSubset(topo, newLinkSet);
    }

    public Topology createTopologyWithLinkSubset(Topology topo, Set<Link> linkSubset){
        Topology newTopo = new Topology(topo.getId(), topo.getNodes(), linkSubset);
        newTopo.copyPathCosts(topo);
        return newTopo;
    }

}
