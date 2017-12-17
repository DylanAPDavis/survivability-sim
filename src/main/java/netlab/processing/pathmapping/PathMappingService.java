package netlab.processing.pathmapping;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.Connections;
import netlab.submission.request.Details;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
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

    public Map<SourceDestPair, Map<String, Path>> filterMap(Map<SourceDestPair, Map<String, Path>> pathMap, Details details){
        List<SourceDestPair> pairsSortedByTotalWeight = pathMap.keySet().stream()
                .sorted(Comparator.comparingLong(p -> pathMap.get(p).values().stream().mapToLong(Path::getTotalWeight).sum()))
                .collect(Collectors.toList());
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
        for (SourceDestPair pair : pairsSortedByTotalWeight) {
            Node src = pair.getSrc();
            Node dst = pair.getDst();
            boolean added = false;
            // If src or dst has not been used yet
            if (!usedS.contains(src) || !usedD.contains(dst) || minPerPairMap.get(pair) > 0) {
                int newSSize = usedS.contains(src) ? usedS.size() : usedS.size() + 1;
                int newDSize = usedD.contains(dst) ? usedD.size() : usedD.size() + 1;
                if (newSSize <= useMaxS && newDSize <= useMaxD) {
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
}
