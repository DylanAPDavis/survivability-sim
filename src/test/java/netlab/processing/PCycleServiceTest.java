package netlab.processing;

import netlab.TestConfiguration;
import netlab.processing.cycles.PCycleService;
import netlab.topology.elements.Node;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

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
        System.out.println(path);
    }
}
