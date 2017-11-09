package netlab.processing;

import netlab.TestConfiguration;
import netlab.processing.cycles.PCycleService;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class PCycleServiceTest {

    @Autowired
    PCycleService pCycleService;

    @Autowired
    TopologyService topologyService;

    @Test
    public void NSFCycleTest(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        List<Node> path = pCycleService.findHamiltonianCycle(topo);
        assert(path.get(0).equals(path.get(path.size()-1)));
        System.out.println(path);
    }

    @Test
    public void NSFCyclePathMapTest(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        List<Node> path = pCycleService.findHamiltonianCycle(topo);
        assert(path.get(0).equals(path.get(path.size()-1)));

        Set<SourceDestPair> pairs = new HashSet<>();
        pairs.add(new SourceDestPair(new Node("Palo Alto"), new Node("Atlanta")));
        Details details = Details.builder()
                .pairs(pairs)
                .build();
        Request request = Request.builder()
                .details(details)
                .build();
        Map<SourceDestPair, Map<String, Path>> pathMap = pCycleService.createPathsFromCycle(request, topo, path);
        assert(pathMap.values().stream().allMatch(m -> m.size() == 2));
    }
}
