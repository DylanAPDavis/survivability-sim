package netlab.processing;

import netlab.TestConfiguration;
import netlab.processing.cycles.HamiltonianCycleService;
import netlab.submission.enums.FailureClass;
import netlab.submission.request.Details;
import netlab.submission.request.Failures;
import netlab.submission.request.NumFailureEvents;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
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
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class HamiltonianCycleServiceTest {

    @Autowired
    HamiltonianCycleService hamiltonianCycleService;

    @Autowired
    TopologyService topologyService;

    @Test
    public void NSFCycleTest(){
        Topology topo = topologyService.getTopologyById("NSFnet");
        List<Node> path = hamiltonianCycleService.findHamiltonianCycle(topo);
        assert(path.get(0).equals(path.get(path.size()-1)));
        System.out.println(path);
    }

    @Test
    public void NSFCyclePathMapTest(){
        Topology topo = topologyService.getTopologyById("NSFnet");

        Set<SourceDestPair> pairs = new HashSet<>();
        pairs.add(new SourceDestPair(new Node("Palo Alto"), new Node("Atlanta")));

        NumFailureEvents nfe = NumFailureEvents.builder()
                .totalNumFailureEvents(2)
                .build();
        Set<Failure> failureSet = topo.getLinks().stream().map(l -> Failure.builder().link(l).build()).collect(Collectors.toSet());
        Failures failures = Failures.builder()
                .failureSet(failureSet)
                .build();
        Details details = Details.builder()
                .failures(failures)
                .numFailureEvents(nfe)
                .pairs(pairs)
                .build();
        Request request = Request.builder()
                .failureClass(FailureClass.Link)
                .details(details)
                .build();
        details = hamiltonianCycleService.solve(request, topo);
        Map<SourceDestPair, Map<String, Path>> pathMap = details.getChosenPaths();
        assert(pathMap.values().stream().allMatch(m -> m.size() == 3));
    }
}
