package netlab.submission;

import netlab.TestConfiguration;
import netlab.submission.controller.SubmissionController;
import netlab.submission.services.SimulateService;
import netlab.submission.simulate.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class SimulateEndpointTest {

    @Autowired
    private SubmissionController submissionController;

    @Autowired
    private SimulateService simulateService;

    @Test
    public void unicastTest(){
        List<RoutingParam> routingParams = new ArrayList<>();
        RoutingParam unicast = RoutingParam.builder()
                .source("1")
                .destinations(Arrays.asList("4"))
                .build();
        routingParams.add(unicast);

        SimRequest simRequest = SimRequest.builder()
                .routingParams(routingParams)
                .network(buildRevLinkNetwork())
                .survivability(buildNoFailures())
                .build();

        test(simRequest, true);
    }

    @Test
    public void unicastAllLinksNfe1Test(){
        List<RoutingParam> routingParams = new ArrayList<>();
        RoutingParam unicast = RoutingParam.builder()
                .source("1")
                .destinations(Arrays.asList("4"))
                .build();
        routingParams.add(unicast);

        SimRequest simRequest = SimRequest.builder()
                .routingParams(routingParams)
                .network(buildRevLinkNetwork())
                .survivability(buildAllLinksFailuresNfe1())
                .build();

        test(simRequest, true);
    }

    @Test
    public void multicastTest(){
        List<RoutingParam> routingParams = new ArrayList<>();
        RoutingParam unicast = RoutingParam.builder()
                .source("1")
                .destinations(Arrays.asList("4", "3"))
                .build();
        routingParams.add(unicast);

        SimRequest simRequest = SimRequest.builder()
                .routingParams(routingParams)
                .network(buildOnlyForwardLinkNetwork())
                .survivability(buildNoFailures())
                .build();

        test(simRequest, true);
    }

    @Test
    public void manyToOne(){
        List<RoutingParam> routingParams = new ArrayList<>();
        RoutingParam unicast1 = RoutingParam.builder()
                .source("1")
                .destinations(Arrays.asList("4"))
                .build();
        RoutingParam unicast2 = RoutingParam.builder()
                .source("2")
                .destinations(Arrays.asList("4"))
                .build();
        routingParams.add(unicast1);
        routingParams.add(unicast2);

        SimRequest simRequest = SimRequest.builder()
                .routingParams(routingParams)
                .network(buildOnlyForwardLinkNetwork())
                .survivability(buildNoFailures())
                .build();

        test(simRequest, true);
    }

    @Test
    public void manyToManyAllLinksFailNfe2(){
        List<RoutingParam> routingParams = new ArrayList<>();
        RoutingParam unicast1 = RoutingParam.builder()
                .source("1")
                .destinations(Arrays.asList("4"))
                .build();
        RoutingParam unicast2 = RoutingParam.builder()
                .source("2")
                .destinations(Arrays.asList("3"))
                .build();
        routingParams.add(unicast1);
        routingParams.add(unicast2);

        SimRequest simRequest = SimRequest.builder()
                .routingParams(routingParams)
                .network(buildOnlyForwardLinkNetwork())
                .survivability(buildAllLinksFailuresNfe2())
                .build();

        test(simRequest, true);
    }

    @Test
    public void manyToManyAllNodesFailNfe1(){
        //TODO: Double check that connecting to all dests is actually desired behavior
        List<RoutingParam> routingParams = new ArrayList<>();
        RoutingParam unicast1 = RoutingParam.builder()
                .source("1")
                .destinations(Arrays.asList("4", "3"))
                .neededD(1)
                .build();
        RoutingParam unicast2 = RoutingParam.builder()
                .source("2")
                .destinations(Arrays.asList("3", "6"))
                .build();
        routingParams.add(unicast1);
        routingParams.add(unicast2);

        SimRequest simRequest = SimRequest.builder()
                .routingParams(routingParams)
                .network(buildOnlyForwardLinkNetwork())
                .survivability(buildAllLinksFailuresNfe1())
                .build();

        test(simRequest, true);
    }


    private void test(SimRequest simRequest, Boolean succeed){
        SimResponse response = submissionController.simulateRequest(simRequest);
        System.out.println(response);
        assert(response.getSucceeded() == succeed);
    }

    private Survivability buildNoFailures(){

        return Survivability.builder()
                .failures(new ArrayList<>())
                .failureScenario("default")
                .numFailureEvents(0)
                .build();
    }

    private Survivability buildSomeLinksFailuresNfe1(){

        return Survivability.builder()
                .failures(Arrays.asList("1-2", "3-4"))
                .numFailureEvents(1)
                .build();
    }

    private Survivability buildSomeLinksFailuresNfe2(){

        return Survivability.builder()
                .failures(Arrays.asList("1-2", "3-4"))
                .numFailureEvents(2)
                .build();
    }

    private Survivability buildAllLinksFailuresNfe1(){

        return Survivability.builder()
                .failureScenario("AllLinks")
                .numFailureEvents(1)
                .build();
    }

    private Survivability buildAllLinksFailuresNfe2(){

        return Survivability.builder()
                .failureScenario("AllLinks")
                .numFailureEvents(2)
                .build();
    }

    private Survivability buildSomeNodeFailuresNfe1(){

        return Survivability.builder()
                .failures(Arrays.asList("2", "4"))
                .numFailureEvents(1)
                .build();
    }

    private Survivability buildSomeNodeFailuresNfe2(){

        return Survivability.builder()
                .failures(Arrays.asList("2", "3"))
                .numFailureEvents(2)
                .build();
    }

    private Survivability buildAllNodesFailuresNfe1(){

        return Survivability.builder()
                .failureScenario("AllNodes")
                .numFailureEvents(1)
                .build();
    }

    private Survivability buildAllNodesFailuresNfe2(){

        return Survivability.builder()
                .failureScenario("AllNodes")
                .numFailureEvents(2)
                .build();
    }

    private Network buildRevLinkNetwork(){
        List<String> nodes = Arrays.asList("1", "2", "3", "4", "5", "6");
        List<String> forLinks = Arrays.asList("1-2", "2-3", "3-4", "4-6", "6-5", "5-1", "2-5", "3-6");
        List<String> revLinks = Arrays.asList("2-1", "3-2", "4-3", "6-4", "5-6", "1-5", "5-2", "6-3");

        List<String> links = new ArrayList<>(forLinks);
        links.addAll(revLinks);
        return Network.builder()
                .nodes(nodes)
                .links(links)
                .build();
    }

    private Network buildOnlyForwardLinkNetwork(){
        List<String> nodes = Arrays.asList("1", "2", "3", "4", "5", "6");
        List<String> forLinks = Arrays.asList("1-2", "2-3", "3-4", "4-6", "6-5", "5-1", "2-5", "3-6");
        return Network.builder()
                .nodes(nodes)
                .links(forLinks)
                .build();
    }
}
