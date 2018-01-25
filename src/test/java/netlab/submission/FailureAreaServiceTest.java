package netlab.submission;

import netlab.TestConfiguration;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.FailureScenario;
import netlab.submission.services.FailureAreaService;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import netlab.visualization.PrintingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class FailureAreaServiceTest {

    @Autowired
    FailureAreaService failureAreaService;

    @Autowired
    TopologyService topologyService;

    @Autowired
    PrintingService printingService;

    @Test
    public void quake1Test(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        Set<Node> nodes = topo.getNodes();
        Set<Link> links = topo.getLinks();
        FailureClass failureClass = FailureClass.Both;
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Quake_1, nodes, links, failureClass);
        System.out.println(printingService.outputFailures(failures));
    }

    @Test
    public void quake1TestOnlyLinks(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        Set<Node> nodes = topo.getNodes();
        Set<Link> links = topo.getLinks();
        FailureClass failureClass = FailureClass.Link;
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Quake_1, nodes, links, failureClass);
        System.out.println(printingService.outputFailures(failures));
    }

    @Test
    public void quake1TestOnlyNodes(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        Set<Node> nodes = topo.getNodes();
        Set<Link> links = topo.getLinks();
        FailureClass failureClass = FailureClass.Node;
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Quake_1, nodes, links, failureClass);
        System.out.println(printingService.outputFailures(failures));
    }


    @Test
    public void quake2Test(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        Set<Node> nodes = topo.getNodes();
        Set<Link> links = topo.getLinks();
        FailureClass failureClass = FailureClass.Both;
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Quake_2, nodes, links, failureClass);
        System.out.println(printingService.outputFailures(failures));
    }

    @Test
    public void quake3Test(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        Set<Node> nodes = topo.getNodes();
        Set<Link> links = topo.getLinks();
        FailureClass failureClass = FailureClass.Both;
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Quake_3, nodes, links, failureClass);
        System.out.println(printingService.outputFailures(failures));
    }

    @Test
    public void quake1_2Test(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        Set<Node> nodes = topo.getNodes();
        Set<Link> links = topo.getLinks();
        FailureClass failureClass = FailureClass.Both;
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Quake_1_2, nodes, links, failureClass);
        System.out.println(printingService.outputFailures(failures));
    }

    @Test
    public void quake1_2_3Test(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        Set<Node> nodes = topo.getNodes();
        Set<Link> links = topo.getLinks();
        FailureClass failureClass = FailureClass.Both;
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Quake_1_2_3, nodes, links, failureClass);
        System.out.println(printingService.outputFailures(failures));
    }

    @Test
    public void nuke1_2_3Test(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        Set<Node> nodes = topo.getNodes();
        Set<Link> links = topo.getLinks();
        FailureClass failureClass = FailureClass.Both;
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Nuke_1_2_3, nodes, links, failureClass);
        System.out.println(printingService.outputFailures(failures));
    }

    // TW network tests
    @Test
    public void quake1TestTW(){
        Topology topo = topologyService.getTopologyById("tw");
        Set<Node> nodes = topo.getNodes();
        Set<Link> links = topo.getLinks();
        FailureClass failureClass = FailureClass.Both;
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Quake_1, nodes, links, failureClass);
        System.out.println(printingService.outputFailures(failures));
    }

    @Test
    public void quake2TestTW(){
        Topology topo = topologyService.getTopologyById("tw");
        Set<Node> nodes = topo.getNodes();
        Set<Link> links = topo.getLinks();
        FailureClass failureClass = FailureClass.Both;
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Quake_2, nodes, links, failureClass);
        System.out.println(printingService.outputFailures(failures));
    }

    @Test
    public void quake3TestTW(){
        Topology topo = topologyService.getTopologyById("tw");
        Set<Node> nodes = topo.getNodes();
        Set<Link> links = topo.getLinks();
        FailureClass failureClass = FailureClass.Both;
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Quake_3, nodes, links, failureClass);
        System.out.println(printingService.outputFailures(failures));
    }
}
