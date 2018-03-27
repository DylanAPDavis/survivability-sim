package netlab.topology;

import netlab.TestConfiguration;
import netlab.topology.elements.Link;
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
        Double sum = topo.getLinks().stream().mapToDouble(Link::getWeight).sum();
        System.out.println(sum);
    }

    @Test
    public void nsfTest(){
        Topology topo = topologyService.getTopologyById("nsfnet");

        assert(topo.getNodes().size() > 0);
        assert(topo.getLinks().size() > 0);
    }

    @Test
    public void getMetricsTest(){
        Topology nsf = topologyService.getTopologyById("nsfnet");
        Topology tw = topologyService.getTopologyById("tw");

        assert(tw.getNodes().size() > 0);
        assert(tw.getLinks().size() > 0);
        String nsfMetrics = topologyService.getMetrics(nsf);
        System.out.println(nsfMetrics);
        String twMetrics = topologyService.getMetrics(tw);
        System.out.println(twMetrics);
    }

}
