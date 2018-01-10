package netlab.processing;


import netlab.TestConfiguration;
import netlab.processing.shortestPaths.AStarService;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class AStarServiceTest {

    @Autowired
    AStarService aStarService;

    @Autowired
    TopologyService topologyService;


    @Test
    public void spTest(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        Node src = topo.getNodeById("Seattle");
        Node dst = topo.getNodeById("Atlanta");
        List<Link> path = aStarService.shortestPath(topo, src, dst);
        System.out.println(path.stream().map(Link::getId).collect(Collectors.toList()));
    }

    @Test
    public void allSpTest(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        Map<SourceDestPair, List<Link>> pathMap = aStarService.allShortestPaths(topo);
        for(SourceDestPair pair : pathMap.keySet()){
            List<Link> path = pathMap.get(pair);
            System.out.println(pair.toString() +  ": " + path.stream().map(Link::getId).collect(Collectors.toList()));
        }
    }

}
