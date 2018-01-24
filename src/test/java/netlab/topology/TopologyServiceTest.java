package netlab.topology;

import netlab.TestConfiguration;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class TopologyServiceTest
{
    @Autowired
    private TopologyService topologyService;


    @Test
    public void twTest(){
        Topology topo = topologyService.getTopologyById("tw");

        assert(topo.getNodes().size() > 0);
        assert(topo.getLinks().size() > 0);
    }

}
