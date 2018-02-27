package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.Request;
import netlab.submission.request.RequestParameters;
import netlab.submission.simulate.*;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SimulateService {

    @Autowired
    public SimulateService(GenerationService generationService){
        this.generationService = generationService;
    }

    private GenerationService generationService;

    public Request generateRequest(SimRequest simRequest) {
        assignDefaults(simRequest);
        RequestParameters requestParameters = makeRequestParameters(simRequest);

        return generationService.generateFromRequestParams(requestParameters);
    }

    private void assignDefaults(SimRequest simRequest) {
        List<RoutingParam> routingParams = simRequest.getRoutingParams();
        for(RoutingParam rp : routingParams){
            if(rp.getNeededD() == null){
                rp.setNeededD(rp.getDestinations().size());
            }
        }
        Survivability survivability = simRequest.getSurvivability();
        if(survivability.getFailures() == null){
            survivability.setFailures(new ArrayList<>());
        }
        if(survivability.getFailureScenario() == null){
            survivability.setFailureScenario("default");
        }
        if(survivability.getNumFailureEvents() == null){
            survivability.setNumFailureEvents(0);
        }
    }

    private RequestParameters makeRequestParameters(SimRequest simRequest) {
        List<RoutingParam> routingParams = simRequest.getRoutingParams();
        Survivability survivability = simRequest.getSurvivability();
        Set<String> failures = new HashSet<>(survivability.getFailures());
        String failureScenario = survivability.getFailureScenario();

        Set<String> sources = new HashSet<>();
        Set<String> destinations = new HashSet<>();
        Map<String, Integer> srcConnectionsMap = new HashMap<>();
        Map<List<String>, Integer> pairMinConnectionsMap = new HashMap<>();
        Map<List<String>, Integer> pairMaxConnectionsMap = new HashMap<>();
        Map<String, Set<String>> srcDestsMap = new HashMap<>();
        Map<String, Integer> srcNeededDMap = new HashMap<>();
        int numConnections = 0;
        for(RoutingParam rp : routingParams){
            String src = rp.getSource();
            List<String> dsts = rp.getDestinations();
            Integer neededD = rp.getNeededD();

            sources.add(src);
            destinations.addAll(dsts);
            numConnections += neededD;
            srcConnectionsMap.putIfAbsent(src, 0);
            srcConnectionsMap.put(src, srcConnectionsMap.get(src) + neededD);
            for(String dst : dsts){

            }
        }

        Map<String, Double> failureProbabiltyMap = failures.stream().collect(Collectors.toMap(f -> f, f -> 1.0));

        return RequestParameters.builder()
                .topologyId("generated")
                .sources(sources)
                .destinations(destinations)
                .problemClass("combined")
                .objective("totalcost")
                .algorithm("flexbhandari")
                .routingType("default")
                .failures(failures)
                .failureScenario(failureScenario)
                .failureProbabilityMap(failureProbabiltyMap)
                .numFailureEvents(survivability.getNumFailureEvents())
                .numConnections(numConnections)
                /*.pairNumConnectionsMap()
                .sourceNumConnectionsMap()
                .destNumConnectionsMap()
                .useMinS()
                .useMaxS()
                .useMinD()
                .useMaxD()
                .trafficCombinationType("none")
                .ignoreFailures(false)
                .numThreads(8)*/
                .build();
    }
    /*

    public String trafficCombinationType;

    private Boolean ignoreFailures;
    private Integer numThreads;
     */

    public SimResponse formatResponse(Request request) {
        String status = "success";
        List<Connection> connectionList = new ArrayList<>();
        if(request == null){
            status = "failed";
        } else{
            Map<SourceDestPair, Map<String, Path>> pathMap = request.getDetails().getChosenPaths();
            if(pathMap.values().stream().allMatch(map -> map.size() == 0)){
                status = "failed";
            } else{
                connectionList = convertPathMap(pathMap);
            }
        }

        return SimResponse.builder()
                .connections(connectionList)
                .status(status)
                .build();
    }

    private List<Connection> convertPathMap(Map<SourceDestPair, Map<String, Path>> pathMap) {
        List<Connection> connections = new ArrayList<>();
        for(SourceDestPair pair : pathMap.keySet()){
            Node src = pair.getSrc();
            Node dst = pair.getDst();
            Map<String, Path> paths = pathMap.get(pair);
            if(paths.size() == 0){
                continue;
            }
            List<Path> sortedPaths = paths.values().stream()
                    .sorted(Comparator.comparing(Path::getTotalWeight))
                    .collect(Collectors.toList());
            List<List<String>> routes = sortedPaths.stream()
                    .map(p -> p.getNodes().stream().map(Node::getId).collect(Collectors.toList()))
                    .collect(Collectors.toList());

            List<String> pairList = Arrays.asList(src.getId(), dst.getId());
            Connection conn = Connection.builder()
                    .pair(pairList)
                    .routes(routes)
                    .build();
            connections.add(conn);
        }
        return connections;
    }


}
