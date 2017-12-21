package netlab.processing.cycles;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.shortestPaths.ShortestPathService;
import netlab.topology.elements.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
Minimum Cost Collapsed Ring (MC-CR)
Multicast Protection
From "Protection of Multicast Sessions in WDM Mesh Optical Networks" by Tanvir Rahman, Georgios Ellinas - 2005
 */
@Service
@Slf4j
public class CollapsedRingService {

    private ShortestPathService shortestPathService;

    @Autowired
    public CollapsedRingService(ShortestPathService shortestPathService){
        this.shortestPathService = shortestPathService;
    }

    public List<Path> findCollapsedRing(Node src, List<Node> destinations, Topology topo){
        // Find a path from src to each destination in order.
        // If a destination ends up as an intermediate node, remove it from the list
        Set<Node> reachedDestinations = new HashSet<>();
        Path firstSrcPath = null;
        List<Path> destToDestPaths = new ArrayList<>();
        Node pathOrigin = src;
        for(Node dest : destinations){
            // If the dest is not the source, and it hasn't been reached already
            if(dest != src && !reachedDestinations.contains(dest)){
                SourceDestPair pair = SourceDestPair.builder().src(pathOrigin).dst(dest).build();
                // Get the Shortest Path
                Path sp = shortestPathService.findShortestPath(pair, topo);
                // Add all reached destinations to set
                for(Node pathNode : sp.getNodes()){
                    if(destinations.contains(pathNode)){
                        reachedDestinations.add(pathNode);
                    }
                }
                if(firstSrcPath == null){
                    firstSrcPath = sp;
                }
                else{
                    destToDestPaths.add(sp);
                }
                pathOrigin = dest;
            }
        }

        // Now we have src -> d1 -> d2 -> ... -> dn paths
        // Need to find a src -> dn path, then store all of the reverse paths
    }
}
