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

        return generationService.generateFromRequestParams(requestParameters, simRequest.getNetwork());
    }

    private void assignDefaults(SimRequest simRequest) {
        System.out.println(simRequest);
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
            List<String> dests = rp.getDestinations();
            Integer neededD = rp.getNeededD();

            sources.add(src);
            destinations.addAll(dests);
            numConnections += neededD;
            srcConnectionsMap.putIfAbsent(src, 0);
            srcConnectionsMap.put(src, srcConnectionsMap.get(src) + neededD);
            srcDestsMap.putIfAbsent(src, new HashSet<>());
            srcDestsMap.get(src).addAll(dests);
            srcNeededDMap.putIfAbsent(src, 0);
            srcNeededDMap.put(src, srcNeededDMap.get(src) + neededD);
        }

        Integer maxNeededD = 0;
        for(String src : srcDestsMap.keySet()){
            Set<String> srcDests = srcDestsMap.get(src);
            Integer neededD = srcNeededDMap.get(src);
            if(neededD > maxNeededD){
                maxNeededD = neededD;
            }
            int min = neededD == srcDests.size() ? 1 : 0;
            for(String dest : destinations){
                List<String> pair = Arrays.asList(src, dest);
                // If this dest has been specified for this src, let it establish connections
                if(srcDests.contains(dest)){
                    pairMinConnectionsMap.put(pair, min);
                    pairMaxConnectionsMap.put(pair, Integer.MAX_VALUE);
                }
                // Otherwise, don't let src connect to this dest
                else{
                    pairMinConnectionsMap.put(pair, 0);
                    pairMaxConnectionsMap.put(pair, 0);
                }
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
                .pairMinNumConnectionsMap(pairMinConnectionsMap)
                .pairMaxNumConnectionsMap(pairMaxConnectionsMap)
                .sourceNumConnectionsMap(srcConnectionsMap)
                .useMinS(sources.size())
                .useMaxS(sources.size())
                .useMinD(Math.min(maxNeededD, destinations.size()))
                .useMaxD(destinations.size())
                .trafficCombinationType("none")
                .ignoreFailures(false)
                .numThreads(8)
                .build();
    }
    /*

    public String trafficCombinationType;

    private Boolean ignoreFailures;
    private Integer numThreads;
     */

    public SimResponse formatResponse(Request request) {
        Boolean status = true;
        List<Connection> connectionList = new ArrayList<>();
        if(request == null){
            status = false;
        } else{
            Map<SourceDestPair, Map<String, Path>> pathMap = request.getDetails().getChosenPaths();
            if(pathMap.values().stream().allMatch(map -> map.size() == 0)){
                status = false;
            } else{
                connectionList = convertPathMap(pathMap);
            }
        }

        return SimResponse.builder()
                .connections(connectionList)
                .succeeded(status)
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
                    .filter(p -> p.getLinks().size() > 0)
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
