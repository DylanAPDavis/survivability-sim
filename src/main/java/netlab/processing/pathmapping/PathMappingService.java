package netlab.processing.pathmapping;

import lombok.extern.slf4j.Slf4j;
import netlab.topology.elements.Link;
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

    public Map<SourceDestPair,Map<String,Path>> mergeMaps(Set<Map<SourceDestPair, Map<String, Path>>> maps) {
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
}
