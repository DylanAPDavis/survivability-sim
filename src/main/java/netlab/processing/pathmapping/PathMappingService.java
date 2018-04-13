package netlab.processing.pathmapping;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.Connections;
import netlab.submission.request.Details;
import netlab.topology.elements.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PathMappingService {


    public Map<SourceDestPair,Map<String,Path>> formatPathMap(Map<Path, SourceDestPair> potentialPathMap) {
        Map<SourceDestPair, Map<String, Path>> pathMap = new HashMap<>();
        for(Path path : potentialPathMap.keySet()){
            SourceDestPair pair = potentialPathMap.get(path);
            Map<String, Path> mapForPair = pathMap.getOrDefault(pair, new HashMap<>());
            String id = String.valueOf(mapForPair.size() + 1);
            mapForPair.put(id, path);
            pathMap.put(pair, mapForPair);
        }
        return pathMap;
    }

    public List<Path> convertToPaths(List<List<Link>> pathLinks, Map<String, Link> originalLinkIdMap){
        List<Path> paths = new ArrayList<>();
        for(List<Link> links : pathLinks){
            paths.add(convertToPath(links, originalLinkIdMap));
        }
        return paths;
    }

    public Path convertToPath(List<Link> pathLinks, Map<String, Link> originalLinkIdMap){
        pathLinks = pathLinks.stream().map(link -> originalLinkIdMap.getOrDefault(link.getId(), link)).collect(Collectors.toList());
        return new Path(pathLinks);
    }

    public Set<Link> getLinksFromMap(Map<SourceDestPair, Map<String, Path>> pathMap) {
        Set<Link> links = new HashSet<>();
        for(Map<String, Path> pathIdMap : pathMap.values()){
            for(Path path : pathIdMap.values()){
                links.addAll(path.getLinks());
            }
        }
        return links;
    }

    public Map<SourceDestPair,Map<String,Path>> mergeMaps(List<Map<SourceDestPair, Map<String, Path>>> maps) {
        Map<SourceDestPair, Map<String, Path>> allPathsMap = new HashMap<>();
        for(Map<SourceDestPair, Map<String, Path>> pathMap : maps){
            for(SourceDestPair pair : pathMap.keySet()){
                allPathsMap.putIfAbsent(pair, new HashMap<>());
                Map<String, Path> pathIdMap = pathMap.get(pair);
                for(Path path : pathIdMap.values()){
                    String id = String.valueOf(allPathsMap.get(pair).size() + 1);
                    allPathsMap.get(pair).put(id, path);
                }
            }
        }
        return allPathsMap;
    }

    public Map<SourceDestPair, Map<String, Path>> filterMapWithRisk(Map<SourceDestPair, Map<String, Path>> pathMap, Details details,
                                                            Map<Link, Double> riskMap){
        Map<SourceDestPair, Double> pathRiskMap = new HashMap<>();
        for(SourceDestPair pair : pathMap.keySet()){
            Collection<Path> paths = pathMap.get(pair).values();
            Double sum = paths.stream().mapToDouble(path -> path.getLinks().stream().mapToDouble(riskMap::get).sum()).sum();
            pathRiskMap.put(pair, sum);
        }
        List<SourceDestPair> pairsSortedByRisk = pathMap.keySet().stream()
                .sorted(Comparator.comparingDouble(pathRiskMap::get)
                        .thenComparingDouble(p -> pathMap.get(p).values().stream().mapToDouble(Path::getTotalWeight).sum()))
                .collect(Collectors.toList());
        return filterUsingSortedPairs(pathMap, details, pairsSortedByRisk);
    }

    public Map<SourceDestPair, Map<String, Path>> filterMap(Map<SourceDestPair, Map<String, Path>> pathMap, Details details){
        List<SourceDestPair> pairsSortedByTotalWeight = pathMap.keySet().stream()
                .sorted(Comparator.comparingDouble(p -> pathMap.get(p).values().stream().mapToDouble(Path::getTotalWeight).sum()))
                .collect(Collectors.toList());
        return filterUsingSortedPairs(pathMap, details, pairsSortedByTotalWeight);
    }

    public Map<SourceDestPair, Map<String, Path>> filterUsingSortedPairs(Map<SourceDestPair, Map<String, Path>> pathMap, Details details,
                                                                         List<SourceDestPair> pairs){
        // Filter out unneeded pairs
        Set<Node> usedS = new HashSet<>();
        Set<Node> usedD = new HashSet<>();
        Connections connections = details.getConnections();
        int useMinS = connections.getUseMinS();
        int useMinD = connections.getUseMinD();
        int useMaxS = connections.getUseMaxS();
        int useMaxD = connections.getUseMaxD();
        Map<SourceDestPair, Integer> minPerPairMap = connections.getPairMinConnectionsMap();

        boolean remove = false;
        for (SourceDestPair pair : pairs) {
            Node src = pair.getSrc();
            Node dst = pair.getDst();
            boolean added = false;
            // If src or dst has not been used yet
            if (!usedS.contains(src) || !usedD.contains(dst) || minPerPairMap.get(pair) > 0) {
                int newSSize = usedS.contains(src) ? usedS.size() : usedS.size() + 1;
                int newDSize = usedD.contains(dst) ? usedD.size() : usedD.size() + 1;
                if (newSSize <= useMaxS && newDSize <= useMaxD && !(newSSize == usedS.size() && usedD.size() >= useMinD)) {
                    usedS.add(src);
                    usedD.add(dst);
                    added = true;
                }
            }
            if (!added || remove && minPerPairMap.get(pair) == 0) {
                pathMap.put(pair, new HashMap<>());
            }
            // If you have sufficient sources/dests, then remove all paths for future pairs
            if(usedS.size() >= useMinS && usedD.size() >= useMinD){
                remove = true;
            }
        }
        return pathMap;
    }

    public Integer countPaths(Map<SourceDestPair,Map<String,Path>> pathMap){
        int count = 0;
        for(Map<String, Path> idMap : pathMap.values()){
            count += idMap.size();
        }
        return count;
    }

    public List<Path> sortPathsByWeight(Collection<Path> paths){
        return paths.stream()
                .sorted(Comparator.comparingDouble(Path::getTotalWeight).thenComparing(p -> p.getLinks().size()))
                .collect(Collectors.toList());
    }

    public Path getPrimary(Collection<Path> paths){
        if(paths.isEmpty()){
            return null;
        }
        List<Path> sortedPaths = sortPathsByWeight(paths);
        return sortedPaths.get(0);
    }

    public Map<SourceDestPair,Path> buildPrimaryPathMap(Map<SourceDestPair, Map<String, Path>> chosenPathsMap) {
        Map<SourceDestPair, Path> primaryPathMap = new HashMap<>();
        for(SourceDestPair pair : chosenPathsMap.keySet()){
            if(chosenPathsMap.get(pair).values().size() > 0) {
                primaryPathMap.put(pair, getPrimary(chosenPathsMap.get(pair).values()));
            }
        }
        return primaryPathMap;
    }


    public Set<String> findOverlap(Set<Path> paths) {
        Set<String> overlap = new HashSet<>();
        List<Path> pathList = new ArrayList<>(paths);
        for(int i = 0; i < pathList.size(); i++){
            Path path = pathList.get(i);
            for(int j = i+1; j < pathList.size(); j++){
                Set<String> pathNodes = new HashSet<>(path.getNodeIds());
                Path otherPath = pathList.get(j);
                Set<String> otherPathNodes = otherPath.getNodeIds();
                // Keep all nodes in the first path that are also in the second path
                pathNodes.retainAll(otherPathNodes);
                // Add those to the overlap (if any)
                overlap.addAll(pathNodes);
            }
        }
        return overlap;
    }

    public List<Node> getReachableNodes(Path path, Collection<Failure> failures) {
        if(path == null){
            return new ArrayList<>();
        }
        Set<String> failureIds = new HashSet<>();
        for(Failure failure : failures){
            if(failure.getNode() != null){
                failureIds.add(failure.getNode().getId());
            }
            else{
                failureIds.add(failure.getLink().getId());
                failureIds.add(failure.getLink().reverse().getId());
            }
        }
        // Get all the links that do not fail and are not attached to failing nodes
        List<Link> pathLinks = path.getLinks().stream()
                .filter(l -> !failureIds.contains(l.getId()) && !failureIds.contains(l.getOrigin().getId()) && !failureIds.contains(l.getTarget().getId()))
                .collect(Collectors.toList());
        // If the first link does not start at the src, or there are no links, then there are no reachable nodes
        if(pathLinks.isEmpty() || pathLinks.get(0).getOrigin() != path.getNodes().get(0)){
            return new ArrayList<>();
        }
        List<Node> reachableNodes = new ArrayList<>();
        Node prevNode = pathLinks.get(0).getOrigin();
        reachableNodes.add(prevNode);
        for(Link link : pathLinks){
            // If the origin isn't the target of the previous link, then there was a removed link somewhere in the path
            Node origin = link.getOrigin();
            if(origin != prevNode){
                break;
            }
            // Store the target, and designate it as the new prevNode
            Node target = link.getTarget();
            reachableNodes.add(target);
            prevNode = target;
        }
        return reachableNodes;
    }

    public void setOriginalWeights(Map<SourceDestPair, Map<String, Path>> pathMap, Map<String, Link> linkIdMap) {
        for(Map<String, Path> pathIdMap : pathMap.values()){
            for(Path path : pathIdMap.values()){
                List<Link> links = path.getLinks();
                for(Link link : links){
                    link.setWeight(linkIdMap.get(link.getId()).getWeight());
                }
            }
        }
    }


    public Map<Node,Path> getPrimaryPathPerSrc(Map<Node, Set<Path>> pathsPerSrc) {
        Map<Node, Path> primaryPathPerSrc = new HashMap<>();
        for(Node src : pathsPerSrc.keySet()){
            Set<Path> paths = pathsPerSrc.get(src);
            Path minPath = null;
            for(Path path : paths){
                if(minPath == null || minPath.getTotalWeight() > path.getTotalWeight()){
                    minPath = path;
                }
            }
            if(minPath != null){
                primaryPathPerSrc.put(src, minPath);
            }
        }
        return primaryPathPerSrc;
    }

    public Map<Node, Set<Path>> getPathsPerSrc(Map<SourceDestPair, Map<String, Path>> chosenPaths){
        Map<Node, Set<Path>> pathsPerSrc = new HashMap<>();
        // Go through all of the pairs, get all paths per source
        for (SourceDestPair pair : chosenPaths.keySet()) {
            Node src = pair.getSrc();
            Set<Path> paths = chosenPaths.get(pair).values().stream().collect(Collectors.toSet());
            if(paths.size() > 0) {
                // Store all paths for this pair
                pathsPerSrc.putIfAbsent(src, new HashSet<>());
                pathsPerSrc.get(src).addAll(paths);
            }
        }
        return pathsPerSrc;
    }

    public Map<SourceDestPair,Map<String,Path>> filterEmptyPaths(Map<SourceDestPair, Map<String, Path>> chosenPaths) {
        for(SourceDestPair pair : chosenPaths.keySet()){
            Map<String, Path> pathMap = chosenPaths.get(pair).keySet().stream()
                    .filter(id -> chosenPaths.get(pair).get(id).getNodes().size() > 0)
                    .collect(Collectors.toMap(id -> id, id -> chosenPaths.get(pair).get(id)));
            chosenPaths.put(pair, pathMap);
        }
        return chosenPaths;
    }

}
