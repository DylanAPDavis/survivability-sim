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

import java.util.*;

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

    /*
    payload={"routingParams":[{"source":"1","destinations":[14]}],

        "network":{"nodes":["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13","14"],
        "links":["1-7", "1-11", "1-12", "2-6", "2-10", "3-6", "3-8", "3-12", "4-8", "4- 10", "5-6", "5-7","5-11","6-13","7-10","9-12","9-13","10-11","4-14","14-9","14-13"]
        },

        "survivability":{"failureScenario":"AllLinks" ,"numFailureEvents" : "1"}
}
     */
    @Test
    public void errorTest() {
        List<RoutingParam> routingParams = new ArrayList<>();
        RoutingParam unicast1 = RoutingParam.builder()
                .source("1")
                .destinations(Arrays.asList("14"))
                .build();
        routingParams.add(unicast1);

        SimRequest simRequest = SimRequest.builder()
                .routingParams(routingParams)
                .network(buildNSFnet())
                .survivability(buildAllLinksFailuresNfe1())
                .build();

        test(simRequest, true);
    }

    @Test
    public void errorEmptyPathTest() {
        List<RoutingParam> routingParams = new ArrayList<>();
        RoutingParam unicast1 = RoutingParam.builder()
                .source("1")
                .destinations(Arrays.asList("14"))
                .build();
        routingParams.add(unicast1);
        RoutingParam unicast2 = RoutingParam.builder()
                .source("2")
                .destinations(Arrays.asList("8"))
                .build();
        routingParams.add(unicast2);

        SimRequest simRequest = SimRequest.builder()
                .routingParams(routingParams)
                .network(buildNSFnet())
                .survivability(buildAllLinksFailuresNfe2())
                .build();

        test(simRequest, true);
    }

    @Test
    public void errorNotDisjointTest() {
        List<RoutingParam> routingParams = new ArrayList<>();
        RoutingParam unicast1 = RoutingParam.builder()
                .source("5")
                .destinations(Arrays.asList("14"))
                .build();
        routingParams.add(unicast1);

        SimRequest simRequest = SimRequest.builder()
                .routingParams(routingParams)
                .network(buildNSFnet())
                .survivability(buildNodesAndLinks())
                .build();

        test(simRequest, true);
    }

    @Test
    public void noResponseTest(){
        List<RoutingParam> routingParams = new ArrayList<>();
        List<String> sources = Arrays.asList("1", "13", "12", "13", "1", "14", "12", "14", "5");
        List<String> destinations = Arrays.asList("3", "5", "6", "1", "9", "11", "7", "8", "10");
        for(int i = 0; i < sources.size(); i++){
            String src = sources.get(i);
            String dst = destinations.get(i);
            RoutingParam param = RoutingParam.builder()
                    .source(src)
                    .destinations(Collections.singletonList(dst))
                    .build();
            routingParams.add(param);
        }

        SimRequest simRequest = SimRequest.builder()
                .routingParams(routingParams)
                .network(buildNSFnet())
                .survivability(buildAllLinksFailuresNfe1())
                .build();

        test(simRequest, true);
        /*
        payload={"routingParams":[{"source":"1","destinations":[3]},
                            {"source":"13","destinations":[5]},
                            {"source":"12","destinations":[6]},
                            {"source":"13","destinations":[1]},
                            {"source":"1","destinations":[9]},
                            {"source":"14","destinations":[11]},
                            {"source":"12","destinations":[7]},
                            {"source":"14","destinations":[8]},
                            {"source":"5","destinations":[10]}],

        "network":{"nodes":["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13","14"],
        "links":["1-7", "1-11", "1-12", "2-6", "2-10", "3-6", "3-8", "3-12", "4-8", "4-10", "5-6", "5-7","5-11","6-13","7-10","9-12","9-13","10-11","4-14","14-9","14-13"]
        },

        "survivability":{"failureScenario":"AllLinks" ,"numFailureEvents" : "1"}

}
         */
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

    private Survivability buildNodesAndLinks(){
        return Survivability.builder()
                .failures(Arrays.asList("1-7", "6"))
                .numFailureEvents(1)
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

    private Network buildNSFnet() {

         List<String> nodes = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13","14");
         List<String> links = Arrays.asList("1-7", "1-11", "1-12", "2-6", "2-10", "3-6", "3-8", "3-12", "4-8", "4- 10",
                 "5-6", "5-7","5-11","6-13","7-10","9-12","9-13","10-11","4-14","14-9","14-13");
         return Network.builder()
                 .nodes(nodes)
                 .links(links)
                 .build();

    }
}
