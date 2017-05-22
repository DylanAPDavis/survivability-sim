package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.*;
import netlab.submission.request.*;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyService;
import org.apache.commons.math3.util.Combinations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;


@Service
@Slf4j
public class GenerationService {

    private TopologyService topologyService;

    @Autowired
    public GenerationService(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    public RequestSet generateRequests(SimulationParameters params){

        assignDefaults(params);
        Map<String, Request> requests = createRequestsFromParameters(params);
        String status = "Processing";
        if(requests.isEmpty()){
            status = "Submission failed. Could not generate requests.";
        }
        String setId = UUID.randomUUID().toString();
        return RequestSet.builder()
                .requests(requests)
                .status(status)
                .id(setId)
                .seed(params.getSeed())
                .problemClass(getProblemClass(params.getProblemClass()))
                .algorithm(getAlgorithm(params.getAlgorithm()))
                .processingType(getProcessingType(params.getProcessingType()))
                .objective(getObjective(params.getObjective()))
                .failureClass(getFailureClass(params.getFailureClass()))
                .percentSrcAlsoDest(params.getPercentSrcAlsoDest())
                .percentSrcFail(params.getPercentSrcFail())
                .percentDestFail(params.getPercentDestFail())
                .sdn(params.getSdn())
                .useAws(params.getUseAws())
                .topologyId(params.getTopologyId())
                .build();
    }

    public RequestSet generateSetFromRequest(RequestParameters requestParameters) {
        assignDefaults(requestParameters);
        Map<String, Request> requests = new HashMap<>();
        Request request = createRequestFromRequestParameters(requestParameters);
        if(request != null) {
            requests.put(request.getId(), request);
        }
        String status = requests.isEmpty() ? "Submission failed. Could not generate request." : "Processing";
        String setId = UUID.randomUUID().toString();
        return RequestSet.builder()
                .requests(requests)
                .status(status)
                .id(setId)
                .seed(-1L)
                .problemClass(getProblemClass(requestParameters.getProblemClass()))
                .algorithm(getAlgorithm(requestParameters.getAlgorithm()))
                .objective(getObjective(requestParameters.getObjective()))
                .processingType(ProcessingType.Solo)
                .failureClass(FailureClass.Both)
                .percentSrcAlsoDest(-1.0)
                .percentDestFail(-1.0)
                .sdn(requestParameters.getSdn())
                .useAws(requestParameters.getUseAws())
                .topologyId(requestParameters.getTopologyId())
                .build();
    }


    private Request createRequestFromRequestParameters(RequestParameters params) {
        Topology topo = topologyService.getTopologyById(params.getTopologyId());
        if(topo == null){
            return null;
        }
        Map<String, Node> nodeIdMap = topo.getNodeIdMap();
        Map<String, Link> linkIdMap = topo.getLinkIdMap();
        Set<Node> sources = params.getSources().stream().filter(nodeIdMap::containsKey).map(nodeIdMap::get).collect(Collectors.toSet());
        Set<Node> destinations = params.getDestinations().stream().filter(nodeIdMap::containsKey).map(nodeIdMap::get).collect(Collectors.toSet());
        if(sources.size() != params.getSources().size() || destinations.size() != params.getDestinations().size()){
            return null;
        }
        Set<SourceDestPair> pairs = createPairs(sources, destinations);

        Connections conns = makeConnectionsFromRequestParams(params, pairs, sources, destinations);
        NumFailsAllowed nfa = makeNumFailsAllowedFromRequestParams(params, pairs, sources, destinations);
        Failures fails = makeFailuresFromRequestParams(params, pairs, sources, destinations, nodeIdMap, linkIdMap,
                nfa.getTotalNumFailsAllowed(), nfa.getPairNumFailsAllowedMap(), nfa.getSrcNumFailsAllowedMap(),
                nfa.getDstNumFailsAllowedMap());

        return Request.builder()
                .id(UUID.randomUUID().toString())
                .sources(sources)
                .destinations(destinations)
                .pairs(pairs)
                .connections(conns)
                .failures(fails)
                .numFailsAllowed(nfa)
                .chosenPaths(null)
                .isFeasible(false)
                .runningTimeMillis(0L)
                .build();
    }

    private NumFailsAllowed makeNumFailsAllowedFromRequestParams(RequestParameters params, Set<SourceDestPair> pairs,
                                                                 Set<Node> sources, Set<Node> destinations) {
        // Map for pairs
        Map<SourceDestPair, Integer> pairNumFailsMap = makePairIntegerMap(pairs, params.getPairNumFailsAllowedMap(), 0);

        // Map for sources
        Map<Node, Integer> srcNumFailsMap = makeNodeIntegerMap(sources, params.getSourceNumFailsAllowedMap(), 0);

        // Map for destinations
        Map<Node, Integer> dstNumFailsMap = makeNodeIntegerMap(destinations, params.getDestNumFailsAllowedMap(), 0);

        return NumFailsAllowed.builder()
                .totalNumFailsAllowed(params.getNumFailsAllowed())
                .pairNumFailsAllowedMap(pairNumFailsMap)
                .srcNumFailsAllowedMap(srcNumFailsMap)
                .dstNumFailsAllowedMap(dstNumFailsMap)
                .build();
    }

    private Failures makeFailuresFromRequestParams(RequestParameters params, Set<SourceDestPair> pairs,
                                                   Set<Node> sources, Set<Node> destinations,
                                                   Map<String, Node> nodeIdMap, Map<String, Link> linkIdMap,
                                                   Integer numFailsAllowed, Map<SourceDestPair, Integer> pairNumFailsAllowed,
                                                   Map<Node, Integer> srcNumFailsAllowed, Map<Node, Integer> dstNumFailsAllowed){

        // Failures for the whole request
        Set<Failure> failures = makeFailureSet(params.getFailures(), nodeIdMap, linkIdMap);
        List<List<Failure>> failureGroups = generateFailureGroups(numFailsAllowed, failures);

        // Failure for pairs
        Map<SourceDestPair, Set<Failure>> pairFailuresMap = makePairFailuresMap(pairs, params.getPairFailureMap(), nodeIdMap, linkIdMap);
        Map<SourceDestPair, List<List<Failure>>> pairFailureGroupsMap = pairFailuresMap.keySet().stream()
                .collect(Collectors.toMap(p -> p, p -> generateFailureGroups(pairNumFailsAllowed.get(p), pairFailuresMap.get(p))));

        // Failures for sources
        Map<Node, Set<Failure>> srcFailuresMap = makeNodeFailuresMap(sources, params.getSourceFailureMap(), nodeIdMap, linkIdMap);
        Map<Node, List<List<Failure>>> srcFailureGroupsMap = srcFailuresMap.keySet().stream()
                .collect(Collectors.toMap(p -> p, p -> generateFailureGroups(srcNumFailsAllowed.get(p), srcFailuresMap.get(p))));

        // Failures for destinations
        Map<Node, Set<Failure>> dstFailuresMap = makeNodeFailuresMap(destinations, params.getSourceFailureMap(), nodeIdMap, linkIdMap);
        Map<Node, List<List<Failure>>> dstFailureGroupsMap = dstFailuresMap.keySet().stream()
                .collect(Collectors.toMap(p -> p, p -> generateFailureGroups(dstNumFailsAllowed.get(p), dstFailuresMap.get(p))));

        long failureSetSize = failures.size() + pairFailuresMap.values().stream().mapToLong(Collection::size).sum();
        failureSetSize += srcFailuresMap.values().stream().mapToLong(Collection::size).sum();
        failureSetSize += dstFailureGroupsMap.values().stream().mapToLong(Collection::size).sum();
        return Failures.builder()
                .failureSet(failures)
                .failureSetSize((int)failureSetSize)
                .failureGroups(failureGroups)
                .pairFailuresMap(pairFailuresMap)
                .pairFailureGroupsMap(pairFailureGroupsMap)
                .srcFailuresMap(srcFailuresMap)
                .srcFailureGroupsMap(srcFailureGroupsMap)
                .dstFailuresMap(dstFailuresMap)
                .dstFailureGroupsMap(dstFailureGroupsMap)
                .build();
    }

    private Map<Node,Set<Failure>> makeNodeFailuresMap(Set<Node> members, Map<String, Set<String>> memberFailureMap,
                                                       Map<String, Node> nodeIdMap, Map<String, Link> linkIdMap) {
        Map<Node, Set<Failure>> failureMap = members.stream().collect(Collectors.toMap(p -> p, p -> new HashSet<>()));
        for(String memberString : memberFailureMap.keySet()){
            Node member = Node.builder().id(memberString).build();
            failureMap.put(member, makeFailureSet(memberFailureMap.get(memberString), nodeIdMap, linkIdMap));
        }
        return failureMap;
    }

    private Map<SourceDestPair,Set<Failure>> makePairFailuresMap(Set<SourceDestPair> pairs, Map<List<String>, Set<String>> pairFailureMap,
                                                                 Map<String, Node> nodeIdMap, Map<String, Link> linkIdMap) {
        Map<SourceDestPair, Set<Failure>> failureMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> new HashSet<>()));
        for(List<String> pairList : pairFailureMap.keySet()){
            SourceDestPair pair = SourceDestPair.builder()
                    .src(Node.builder().id(pairList.get(0)).build())
                    .dst(Node.builder().id(pairList.get(1)).build())
                    .build();
            failureMap.put(pair, makeFailureSet(pairFailureMap.get(pairList), nodeIdMap, linkIdMap));
        }
        return failureMap;
    }

    private Set<Failure> makeFailureSet(Set<String> failureStrings, Map<String, Node> nodeIdMap,
                                        Map<String, Link> linkIdMap){
        Set<Failure> failures = new HashSet<>();
        for(String failString : failureStrings){
            if(nodeIdMap.containsKey(failString)){
                failures.add(Failure.builder().node(nodeIdMap.get(failString)).build());
            }
            else if(linkIdMap.containsKey(failString)){
                failures.add(Failure.builder().link(linkIdMap.get(failString)).build());
            }
        }
        return failures;
    }

    private Connections makeConnectionsFromRequestParams(RequestParameters params, Set<SourceDestPair> pairs,
                                                         Set<Node> sources, Set<Node> destinations){

        // Map for pairs
        Map<SourceDestPair, Integer> pairMinConnectionsMap = params.getPairNumConnectionsMap().size() > 0 ?
                makePairIntegerMap(pairs, params.getPairNumConnectionsMap(), 0) :
                makePairIntegerMap(pairs, params.getPairMinNumConnectionsMap(), 0);
        Map<SourceDestPair, Integer> pairMaxConnectionsMap = params.getPairNumConnectionsMap().size() > 0 ?
                makePairIntegerMap(pairs, params.getPairNumConnectionsMap(), params.getNumConnections()) :
                makePairIntegerMap(pairs, params.getPairMaxNumConnectionsMap(), params.getNumConnections());

        // Map for sources
        Map<Node, Integer> srcMinConnectionsMap = params.getSourceNumConnectionsMap().size() > 0 ?
                makeNodeIntegerMap(sources, params.getSourceNumConnectionsMap(), 0) :
                makeNodeIntegerMap(sources, params.getSourceMinNumConnectionsMap(), 0);
        Map<Node, Integer> srcMaxConnectionsMap = params.getSourceNumConnectionsMap().size() > 0 ?
                makeNodeIntegerMap(sources, params.getSourceNumConnectionsMap(), params.getNumConnections()) :
                makeNodeIntegerMap(sources, params.getSourceMaxNumConnectionsMap(), params.getNumConnections());

        // Map for destinations
        Map<Node, Integer> dstMinConnectionsMap = params.getDestNumConnectionsMap().size() > 0 ?
                makeNodeIntegerMap(destinations, params.getDestNumConnectionsMap(), 0) :
                makeNodeIntegerMap(destinations, params.getDestMinNumConnectionsMap(), 0);
        Map<Node, Integer> dstMaxConnectionsMap = params.getDestNumConnectionsMap().size() > 0 ?
                makeNodeIntegerMap(destinations, params.getDestNumConnectionsMap(), params.getNumConnections()) :
                makeNodeIntegerMap(destinations, params.getDestMaxNumConnectionsMap(), params.getNumConnections());

        return Connections.builder()
                .numConnections(params.getNumConnections())
                .pairMinConnectionsMap(pairMinConnectionsMap)
                .pairMaxConnectionsMap(pairMaxConnectionsMap)
                .srcMinConnectionsMap(srcMinConnectionsMap)
                .srcMaxConnectionsMap(srcMaxConnectionsMap)
                .dstMinConnectionsMap(dstMinConnectionsMap)
                .dstMaxConnectionsMap(dstMaxConnectionsMap)
                .build();
    }

    private Map<Node, Integer> makeNodeIntegerMap(Set<Node> members, Map<String, Integer> memberConnMap, Integer defaultValue){
        Map<Node, Integer> memberMap = members.stream().collect(Collectors.toMap(m -> m, m -> defaultValue));
        for(String nodeName : memberConnMap.keySet()){
            Node node = Node.builder().id(nodeName).build();
            memberMap.put(node, memberConnMap.get(nodeName));
        }
        return memberMap;
    }

    private Map<SourceDestPair, Integer> makePairIntegerMap(Set<SourceDestPair> pairs, Map<List<String>, Integer> pairConnMap,
                                                            Integer defaultValue){
        Map<SourceDestPair, Integer> pairMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> defaultValue));
        for(List<String> pairList : pairConnMap.keySet()){
            SourceDestPair pair = SourceDestPair.builder()
                    .src(Node.builder().id(pairList.get(0)).build())
                    .dst(Node.builder().id(pairList.get(1)).build())
                    .build();
            pairMap.put(pair, pairConnMap.get(pairList));
        }
        return pairMap;
    }


    private void assignDefaults(RequestParameters params) {
        if(params.getTopologyId() == null){
            params.setTopologyId("NSFnet");
        }
        if(params.getSources() == null){
            params.setSources(new HashSet<>());
        }
        if(params.getDestinations() == null){
            params.setDestinations(new HashSet<>());
        }
        if(params.getProblemClass() == null){
            params.setProblemClass("Flow");
        }
        if(params.getObjective() == null){
            params.setObjective("LinkCost");
        }
        if(params.getAlgorithm() == null){
            params.setAlgorithm("ServiceILP");
        }


        // F - Total size of the failure set (shared by all connections)
        if(params.getFailures() == null){
            params.setFailures(new HashSet<>());
        }
        if(params.getPairFailureMap() == null){
            params.setPairFailureMap(new HashMap<>());
        }
        if(params.getSourceFailureMap() == null){
            params.setSourceFailureMap(new HashMap<>());
        }
        if(params.getDestFailureMap() == null){
            params.setDestFailureMap(new HashMap<>());
        }

        // Failure probability - pick one field
        if(params.getFailureProbabilityMap() == null){
            params.setFailureProbabilityMap(new HashMap<>());
        }
        if(params.getPairFailureProbabilityMap() == null){
            params.setPairFailureProbabilityMap(new HashMap<>());
        }
        if(params.getSourceFailureProbabilityMap() == null){
            params.setSourceFailureProbabilityMap(new HashMap<>());
        }
        if(params.getDestFailureProbabilityMap() == null){
            params.setDestFailureProbabilityMap(new HashMap<>());
        }

        // C - total number of connections
        if(params.getNumConnections() == null || params.getNumConnections() < 0){
            params.setNumConnections(0);
        }

        // Pairs
        if(params.getPairNumConnectionsMap() == null){
            params.setPairNumConnectionsMap(new HashMap<>());
        }
        if(params.getPairMinNumConnectionsMap() == null){
            params.setPairMinNumConnectionsMap(new HashMap<>());
        }
        if(params.getPairMaxNumConnectionsMap() == null){
            params.setPairMaxNumConnectionsMap(new HashMap<>());
        }

        // Source
        if(params.getSourceNumConnectionsMap() == null){
            params.setSourceNumConnectionsMap(new HashMap<>());
        }
        if(params.getSourceMinNumConnectionsMap() == null){
            params.setSourceMinNumConnectionsMap(new HashMap<>());
        }
        if(params.getSourceMaxNumConnectionsMap() == null){
            params.setSourceMaxNumConnectionsMap(new HashMap<>());
        }

        // Dest
        if(params.getDestNumConnectionsMap() == null){
            params.setDestNumConnectionsMap(new HashMap<>());
        }
        if(params.getDestMinNumConnectionsMap() == null){
            params.setDestMinNumConnectionsMap(new HashMap<>());
        }
        if(params.getDestMaxNumConnectionsMap() == null){
            params.setDestMaxNumConnectionsMap(new HashMap<>());
        }

        // Number of failureSet that will occur
        if(params.getNumFailsAllowed() == null || params.getNumFailsAllowed() < 0){
            params.setNumFailsAllowed(0);
        }
        if(params.getPairNumFailsAllowedMap() == null){
            params.setPairNumFailsAllowedMap(new HashMap<>());
        }
        if(params.getSourceNumFailsAllowedMap() == null){
            params.setSourceNumFailsAllowedMap(new HashMap<>());
        }
        if(params.getDestNumFailsAllowedMap() == null){
            params.setDestNumFailsAllowedMap(new HashMap<>());
        }

        if(params.getSdn() == null){
            params.setSdn(false);
        }
        if(params.getUseAws() == null){
            params.setUseAws(false);
        }
    }

    private void assignDefaults(SimulationParameters params) {
        if(params.getSeed() == null){
            Random rng = new Random();
            params.setSeed(rng.nextLong());
        }
        if(params.getTopologyId() == null){
            params.setTopologyId("NSFnet");
        }
        if(params.getNumSources() == null){
            params.setNumSources(0);
        }
        if(params.getNumDestinations() == null){
            params.setNumDestinations(0);
        }
        if(params.getProblemClass() == null){
            params.setProblemClass("Flow");
        }
        if(params.getObjective() == null){
            params.setObjective("LinkCost");
        }
        if(params.getAlgorithm() == null){
            params.setObjective("ServiceILP");
        }
        if(params.getNumRequests() == null || params.getNumRequests() < 0){
            params.setNumRequests(0);
        }

        if(params.getFailureSetSize() == null || params.getFailureSetSize() < 0){
            params.setFailureSetSize(0);
        }
        if(params.getMinMaxFailures() == null){
            params.setMinMaxFailures(new ArrayList<>());
        }
        if(params.getFailureClass() == null){
            params.setFailureClass("Both");
        }
        if(params.getNumConnections() == null || params.getNumConnections() < 0){
            params.setNumConnections(0);
        }
        if(params.getMinConnectionsRange() == null){
            params.setMinConnectionsRange(new ArrayList<>());
        }
        if(params.getMaxConnectionsRange() == null){
            params.setMaxConnectionsRange(new ArrayList<>());
        }
        if(params.getNumFailsAllowed() == null || params.getNumFailsAllowed() < 0){
            params.setNumFailsAllowed(0);
        }
        if(params.getMinMaxFailsAllowed() == null){
            params.setMinMaxFailsAllowed(new ArrayList<>());
        }
        if(params.getFailureProb() == null || params.getFailureProb() < 0 || params.getFailureProb() > 1){
            params.setFailureProb(1.0);
        }
        if(params.getMinMaxFailureProb() == null){
            params.setMinMaxFailureProb(new ArrayList<>());
        }
        if(params.getProcessingType() == null){
            params.setProcessingType("Solo");
        }
        if(params.getSdn() == null){
            params.setSdn(false);
        }
        if(params.getUseAws() == null){
            params.setUseAws(false);
        }

        if(params.getPercentSrcAlsoDest() == null || params.getPercentSrcAlsoDest() < 0.0){
            params.setPercentSrcAlsoDest(0.0);
        }
        else if(params.getPercentSrcAlsoDest() > 1.0){
            params.setPercentSrcAlsoDest(1.0);
        }

        if(params.getPercentSrcFail() == null || params.getPercentSrcFail() < 0.0){
            params.setPercentSrcFail(0.0);
        }
        else if(params.getPercentSrcFail() > 1.0){
            params.setPercentSrcFail(1.0);
        }

        if(params.getPercentDestFail() == null || params.getPercentDestFail() < 0.0){
            params.setPercentDestFail(0.0);
        }
        else if(params.getPercentDestFail() > 1.0){
            params.setPercentDestFail(1.0);
        }
    }

    public Map<String, Request> createRequestsFromParameters(SimulationParameters params) {
        Map<String, Request> requests = new HashMap<>();
        Topology topo = topologyService.getTopologyById(params.getTopologyId());
        if(topo == null){
            return requests;
        }

        Random rng = new Random(params.getSeed());

        for(int i = 0; i < params.getNumRequests(); i++){
            Request r = createRequest(params, topo, rng);
            requests.put(r.getId(), r);
        }
        return requests;
    }


    public Request createRequest(SimulationParameters params, Topology topo, Random rng){

        Set<Node> sources = pickSources(topo.getNodes(), params.getNumSources(), rng);
        Set<Node> destinations = pickDestinations(topo.getNodes(), params.getNumDestinations(), rng,
                params.getPercentSrcAlsoDest(), sources);

        Set<SourceDestPair> pairs = createPairs(sources, destinations);
        List<SourceDestPair> sortedPairs = new ArrayList<>(pairs);
        Comparator<SourceDestPair> bySrc = Comparator.comparing(p -> p.getSrc().getId());
        Comparator<SourceDestPair> byDst = Comparator.comparing(p -> p.getDst().getId());
        sortedPairs.sort(bySrc.thenComparing(byDst));

        List<Node> sortedSources = new ArrayList<>(sources);
        sortedSources.sort(Comparator.comparing(Node::getId));
        List<Node> sortedDests = new ArrayList<>(destinations);
        sortedDests.sort(Comparator.comparing(Node::getId));

        Failures failureCollection = assignFailureSets(params, sortedSources, sortedDests, sortedPairs, topo, rng);

        // Determine number of cuts
        NumFailsAllowed numFailsAllowedCollection = assignNumFails(params, sortedPairs, sortedSources, sortedDests, failureCollection, rng);


        // Determine number of connections
        Connections connectionsCollection = assignConnections(params, sortedPairs, sortedSources, sortedDests, rng);


        return Request.builder()
                .id(UUID.randomUUID().toString())
                .sources(sources)
                .destinations(destinations)
                .connections(connectionsCollection)
                .failures(failureCollection)
                .numFailsAllowed(numFailsAllowedCollection)
                .pairs(pairs)
                .runningTimeMillis(0L)
                .isFeasible(false)
                .build();
    }

    private Connections assignConnections(SimulationParameters params, List<SourceDestPair> pairs, List<Node> sources,
                                          List<Node> destinations, Random rng){
        ProblemClass problemClass = getProblemClass(params.getProblemClass());
        // Connection params
        Integer numConnections = params.getNumConnections();
        List<Integer> minConnectionsRange = params.getMinConnectionsRange();
        List<Integer> maxConnectionsRange = params.getMaxConnectionsRange();

        Map<SourceDestPair, Integer> pairMinConnectionsMap = new HashMap<>();
        Map<SourceDestPair, Integer> pairMaxConnectionsMap = new HashMap<>();

        Map<Node, Integer> srcMinConnectionsMap = new HashMap<>();
        Map<Node, Integer> srcMaxConnectionsMap = new HashMap<>();
        Map<Node, Integer> dstMinConnectionsMap = new HashMap<>();
        Map<Node, Integer> dstMaxConnectionsMap = new HashMap<>();

        if( minConnectionsRange.size() == 2 && maxConnectionsRange.size() == 2) {
            // Get the minimum/maximum for generating mins (index 0) and maxes (index 1)
            Integer minForMinConn = minConnectionsRange.get(0);
            Integer maxForMinConn = minConnectionsRange.get(1);
            Integer minForMaxConn = maxConnectionsRange.get(0);
            Integer maxForMaxConn = maxConnectionsRange.get(1);
            if (problemClass.equals(ProblemClass.Flex)) {
                numConnections = randomInt(minConnectionsRange.get(0), maxConnectionsRange.get(1), rng);
            }
            if (problemClass.equals(ProblemClass.Flow)) {
                // Give random min/max num of connections per pair
                // If src = dst for a pair, both numbers are 0
                pairMinConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                        p -> p.getSrc() == p.getDst() ? 0 : randomInt(minForMinConn, maxForMinConn, rng)));
                pairMaxConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                        p -> p.getSrc() == p.getDst() ? 0 : randomInt(minForMaxConn, maxForMaxConn, rng)));

                //Update number of required connections for request to be equal to the total min
                if (numConnections == 0)
                    numConnections = pairMinConnectionsMap.values().stream().reduce(0, (c1, c2) -> c1 + c2);
            }
            if(problemClass.equals(ProblemClass.Endpoint)){
                srcMinConnectionsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> randomInt(minForMinConn, maxForMinConn, rng)));
                srcMaxConnectionsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> randomInt(minForMaxConn, maxForMaxConn, rng)));
                dstMinConnectionsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> randomInt(minForMinConn, maxForMinConn, rng)));
                dstMaxConnectionsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> randomInt(minForMaxConn, maxForMaxConn, rng)));

                if(numConnections == 0){
                    numConnections = Math.max(
                            srcMinConnectionsMap.values().stream().reduce(0, (c1,c2) -> c1 + c2),
                            dstMinConnectionsMap.values().stream().reduce(0, (c1,c2) -> c1 + c2));
                }
            }
        }
        else{
            if(problemClass.equals(ProblemClass.Flow)){
                pairMinConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
                pairMaxConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                        p -> p.getSrc() == p.getDst() ? 0 : params.getNumConnections()));
            }
            if(problemClass.equals(ProblemClass.Endpoint)){
                srcMinConnectionsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> 0));
                srcMaxConnectionsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> params.getNumConnections()));
                dstMinConnectionsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> 0));
                dstMaxConnectionsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> params.getNumConnections()));
            }
        }

        return Connections.builder()
                .numConnections(numConnections)
                .pairMinConnectionsMap(pairMinConnectionsMap)
                .pairMaxConnectionsMap(pairMaxConnectionsMap)
                .srcMinConnectionsMap(srcMinConnectionsMap)
                .srcMaxConnectionsMap(srcMaxConnectionsMap)
                .dstMinConnectionsMap(dstMinConnectionsMap)
                .dstMaxConnectionsMap(dstMaxConnectionsMap)
                .build();
    }

    private NumFailsAllowed assignNumFails(SimulationParameters params, List<SourceDestPair> pairs, List<Node> sources,
                                           List<Node> destinations, Failures failureCollection, Random rng) {
        ProblemClass problemClass = getProblemClass(params.getProblemClass());

        // Cut params
        Integer numFails = params.getNumFailsAllowed();
        List<List<Failure>> failureGroups = new ArrayList<>();

        List<Integer> minMaxFails = params.getMinMaxFailsAllowed();
        Map<SourceDestPair, Integer> pairNumFailsMap = new HashMap<>();
        Map<SourceDestPair, List<List<Failure>>> pairFailureGroupsMap = new HashMap<>();

        Map<Node, Integer> srcNumFailsMap = new HashMap<>();
        Map<Node, List<List<Failure>>> srcFailureGroupsMap = new HashMap<>();
        Map<Node, Integer> dstNumFailsMap = new HashMap<>();
        Map<Node, List<List<Failure>>> dstFailureGroupsMap = new HashMap<>();

        // Assign random number of cuts between min and max
        // Except: cap out at the number of failureSet for a pair, so you're not trying to cut more than than the
        // size of the failure set
        if(problemClass.equals(ProblemClass.Flex)){
            if(minMaxFails.size() == 2) {
                numFails = Math.min(failureCollection.getFailureSet().size(), randomInt(minMaxFails.get(0), minMaxFails.get(1), rng));
            }
            failureGroups = generateFailureGroups(numFails, failureCollection.getFailureSet());
        }
        if(problemClass.equals(ProblemClass.Flow)){
            for (SourceDestPair pair : pairs) {
                Set<Failure> thisFailureSet = failureCollection.getPairFailuresMap().getOrDefault(pair, failureCollection.getFailureSet());
                int thisNumFails = minMaxFails.size() == 2 ?
                        Math.min(thisFailureSet.size(), randomInt(minMaxFails.get(0), minMaxFails.get(1), rng)) : numFails;
                pairNumFailsMap.put(pair, thisNumFails);
                pairFailureGroupsMap.put(pair, generateFailureGroups(thisNumFails, thisFailureSet));
            }
            //Update number of required cuts for request to be equal to the total min
            numFails = pairNumFailsMap.values().stream().reduce(0, (c1, c2) -> c1 + c2);

        }
        if(problemClass.equals(ProblemClass.Endpoint)){
            for(Node source : sources){
                Set<Failure> failureSet = failureCollection.getSrcFailuresMap().getOrDefault(source, failureCollection.getFailureSet());
                populateNumFailsAndFailureGroupMap(failureSet, source, minMaxFails, numFails, srcNumFailsMap, srcFailureGroupsMap, rng);
            }
            for(Node dest : destinations){
                Set<Failure> failureSet = failureCollection.getDstFailuresMap().getOrDefault(dest, failureCollection.getFailureSet());
                populateNumFailsAndFailureGroupMap(failureSet, dest, minMaxFails, numFails, dstNumFailsMap, dstFailureGroupsMap, rng);
            }
        }

        failureCollection.setFailureGroups(failureGroups);
        failureCollection.setPairFailureGroupsMap(pairFailureGroupsMap);
        failureCollection.setSrcFailureGroupsMap(srcFailureGroupsMap);
        failureCollection.setDstFailureGroupsMap(dstFailureGroupsMap);

        return NumFailsAllowed.builder()
                .totalNumFailsAllowed(numFails)
                .pairNumFailsAllowedMap(pairNumFailsMap)
                .srcNumFailsAllowedMap(srcNumFailsMap)
                .dstNumFailsAllowedMap(dstNumFailsMap)
                .build();
    }

    private void populateNumFailsAndFailureGroupMap(Set<Failure> failureSet, Node member, List<Integer> minMaxFails,
                                                    Integer numFails, Map<Node, Integer> numFailsMap,
                                                    Map<Node, List<List<Failure>>> failureGroupsMap, Random rng){
        int thisNumFails = minMaxFails.size() == 2 ?
                Math.min(failureSet.size(), randomInt(minMaxFails.get(0), minMaxFails.get(1), rng)) : numFails;
        numFailsMap.put(member, thisNumFails);
        failureGroupsMap.put(member, generateFailureGroups(thisNumFails, failureSet));
    }

    private Failures assignFailureSets(SimulationParameters params, List<Node> sources,
                                       List<Node> destinations, List<SourceDestPair> sortedPairs,
                                       Topology topo, Random rng){

        Integer numFailures = params.getFailureSetSize();
        List<Integer> minMaxFailures = params.getMinMaxFailures();
        FailureClass failureClass = getFailureClass(params.getFailureClass());

        ProblemClass problemClass = getProblemClass(params.getProblemClass());

        Set<Failure> failures = new HashSet<>();
        Map<SourceDestPair, Set<Failure>> pairFailuresMap = new HashMap<>();
        Map<Node, Set<Failure>> srcFailuresMap = new HashMap<>();
        Map<Node, Set<Failure>> dstFailuresMap = new HashMap<>();

        // Based on ProblemClass and failureSetSize / minMaxFailures input, generate the number of needed failureSet
        // If Flex, use the total number of failureSet
        // Otherwise, use min/max, unless that field isn't set.
        // Create failureSet
        Set<Node> srcDstFailures = choosePercentageSubsetNodes(new HashSet<>(sources), params.getPercentSrcFail(), rng);
        srcDstFailures.addAll(choosePercentageSubsetNodes(new HashSet<>(destinations), params.getPercentDestFail(), rng));

        Integer failureSetSize = numFailures != null ? numFailures : 0;

        if(minMaxFailures.size() < 2){
            failureSetSize = numFailures;
            failures = generateFailureSet(topo.getNodes(), topo.getLinks(), numFailures, failureClass,
                    params.getFailureProb(), params.getMinMaxFailureProb(), sources, destinations, srcDstFailures, rng);
        }
        else{
            if(problemClass.equals(ProblemClass.Flow)){
                for(SourceDestPair pair : sortedPairs){
                    Integer randomNumFailures = randomInt(minMaxFailures.get(0), minMaxFailures.get(1), rng);
                    Set<Failure> failureSet = generateFailureSet(topo.getNodes(), topo.getLinks(),
                            randomNumFailures, failureClass, params.getFailureProb(), params.getMinMaxFailureProb(),
                            sources, destinations, srcDstFailures, rng);
                    pairFailuresMap.put(pair, failureSet);
                    failureSetSize += failureSet.size();
                }
            }
            else if(problemClass.equals(ProblemClass.Endpoint)){
                for(Node src : sources){
                    populateFailureMap(minMaxFailures, topo, rng, failureClass, params, sources, destinations, srcDstFailures,
                            srcFailuresMap, failureSetSize, src);
                }
                for(Node dst : destinations){
                    populateFailureMap(minMaxFailures, topo, rng, failureClass, params, sources, destinations, srcDstFailures,
                            dstFailuresMap, failureSetSize, dst);
                }
            }
            else if(problemClass.equals(ProblemClass.Flex)){
                failureSetSize = randomInt(minMaxFailures.get(0), minMaxFailures.get(1), rng);
                failures = generateFailureSet(topo.getNodes(), topo.getLinks(),
                        failureSetSize, failureClass, params.getFailureProb(), params.getMinMaxFailureProb(),
                        sources, destinations, srcDstFailures, rng);
            }
        }

        return Failures.builder()
                .failureSetSize(failureSetSize)
                .failureSet(failures)
                .pairFailuresMap(pairFailuresMap)
                .srcFailuresMap(srcFailuresMap)
                .dstFailuresMap(dstFailuresMap)
                .build();
    }

    private Integer populateFailureMap(List<Integer> minMaxFailures, Topology topo, Random rng, FailureClass failureClass,
                                       SimulationParameters params, List<Node> sources, List<Node> destinations,
                                       Set<Node> srcDstFailures, Map<Node, Set<Failure>> failuresMap, Integer failureSetSize,
                                       Node member){
        Integer randomNumFailures = randomInt(minMaxFailures.get(0), minMaxFailures.get(1), rng);
        Set<Failure> failureSet = generateFailureSet(topo.getNodes(), topo.getLinks(),
                randomNumFailures, failureClass, params.getFailureProb(), params.getMinMaxFailureProb(),
                sources, destinations, srcDstFailures, rng);
        failuresMap.put(member, failureSet);
        failureSetSize += failureSet.size();
        return failureSetSize;
    }


    private Integer randomInt(Integer min, Integer max, Random rng){
        return rng.nextInt((max - min) + 1) + min;
    }

    private Set<SourceDestPair> createPairs(Set<Node> sources, Set<Node> destinations) {
        Set<SourceDestPair> pairs = new HashSet<>();
        for(Node source : sources){
            for(Node dest: destinations){
                pairs.add(SourceDestPair.builder().src(source).dst(dest).build());
            }
        }
        return pairs;
    }


    private Set<Node> pickSources(Set<Node> nodes, Integer numSources, Random rng) {
        return new HashSet<>(chooseRandomSubsetNodes(nodes, numSources, rng));
    }

    private Set<Node> pickDestinations(Set<Node> nodes, Integer numDestinations, Random rng, Double percentSrcAlsoDest,
                                       Set<Node> sources) {
        Set<Node> remainingNodes = new HashSet<>(nodes);
        Set<Node> chosenNodes = new HashSet<>();
        // If any sources also must be destinations
        if(percentSrcAlsoDest > 0.0){
            chosenNodes.addAll(choosePercentageSubsetNodes(sources, percentSrcAlsoDest, rng));
        }
        // If you still haven't picked enough yet
        if(chosenNodes.size() < numDestinations){
            remainingNodes.removeAll(sources);
            Set<Node> remainingChoices = chooseRandomSubsetNodes(remainingNodes, numDestinations-chosenNodes.size(), rng);
            chosenNodes.addAll(remainingChoices);
        }
        return chosenNodes;
    }

    private Set<Failure> generateFailureSet(Set<Node> nodes, Set<Link> links, Integer numFailures, FailureClass failureClass,
                                            Double probability, List<Double> minMaxFailureProb, List<Node> sources, List<Node> destinations,
                                            Set<Node> prioritySet, Random rng) {

        List<Link> chosenLinks = new ArrayList<>();
        List<Node> chosenNodes = new ArrayList<>();

        Set<Node> nodeOptions = new HashSet<>(nodes);

        if(numFailures == 0){
            return new HashSet<>();
        }
        if(!prioritySet.isEmpty()){
            chosenNodes.addAll(chooseRandomSubsetNodes(prioritySet, numFailures, rng));
            prioritySet.removeAll(chosenNodes);
        }
        // If we still haven't gotten enough failureSet, make some more
        if(chosenNodes.size() < numFailures){
            int numLeftToChoose = numFailures - chosenNodes.size();

            Set<Link> linkOptions = new HashSet<>(links);
            if(failureClass.equals(FailureClass.Link)){
                chosenLinks.addAll(chooseRandomSubsetLinks(linkOptions, numLeftToChoose, rng));
            }

            // remove any nodes, if necessary
            nodeOptions.removeAll(sources);
            nodeOptions.removeAll(destinations);

            if(failureClass.equals(FailureClass.Node)){
                chosenNodes.addAll(chooseRandomSubsetNodes(nodeOptions, numLeftToChoose, rng));
            }

            if(failureClass.equals(FailureClass.Both)){
                Integer numNodeFailures = rng.nextInt(numLeftToChoose);
                Integer numLinkFailures = numLeftToChoose - numNodeFailures;
                chosenNodes.addAll(chooseRandomSubsetNodes(nodeOptions, numNodeFailures, rng));
                chosenLinks.addAll(chooseRandomSubsetLinks(linkOptions, numLinkFailures, rng));
            }
        }

        // Determine probabilities
        List<Double> probabilities = generateProbabilities(probability, minMaxFailureProb, chosenNodes.size() + chosenLinks.size(), rng);

        return generateFailuresFromNodeLinks(chosenNodes, chosenLinks, probabilities);
    }

    private List<Double> generateProbabilities(Double probability, List<Double> minMaxFailureProb, Integer numFailures, Random rng) {
        if(probability > 0){
            return DoubleStream.iterate(probability, p -> p).limit(numFailures).boxed().collect(Collectors.toList());
        }
        else{
            return rng.doubles(numFailures, minMaxFailureProb.get(0), minMaxFailureProb.get(1) + 0.1).boxed().collect(Collectors.toList());
        }
    }

    private Set<Failure> generateFailuresFromNodeLinks(List<Node> nodes, List<Link> links, List<Double> probabilities){
        Set<Failure> failures = new HashSet<>();
        for(Node node : nodes){
            Double prob = probabilities.remove(0);
            failures.add(Failure.builder().node(node).link(null).probability(prob).build());
        }
        for(Link link : links){
            Double prob = probabilities.remove(0);
            failures.add(Failure.builder().node(null).link(link).probability(prob).build());
        }
        return failures;
    }

    private static List<List<Failure>> generateFailureGroups(Integer k, Set<Failure> failureSet){
        List<List<Failure>> failureGroups = new ArrayList<>();
        List<Failure> failureList = new ArrayList<>(failureSet);
        Collections.shuffle(failureList);

        // Find all k-size subsets of this failure set
        if(failureSet.size() <= k){
            failureGroups.add(failureList);
        }
        else{
            Combinations combos = new Combinations(failureSet.size(), k);
            for(int[] comboIndices : combos) {
                List<Failure> group = new ArrayList<>();
                for(int comboIndice : comboIndices) {
                    group.add(failureList.get(comboIndice));
                }
                failureGroups.add(group);
            }
        }
        return failureGroups;
    }

    private Set<Node> choosePercentageSubsetNodes(Set<Node> options, Double percentage, Random rng){
        int numToChoose = numFromPercentage(options.size(), percentage);
        return chooseRandomSubsetNodes(options, numToChoose, rng);
    }

    private int numFromPercentage(int numOptions, double percentage){
        return (int) Math.round(percentage * numOptions);
    }


    private Set<Node> chooseRandomSubsetNodes(Set<Node> options, Integer numChoices, Random rng) {
        if(numChoices == 0){
            return new HashSet<>();
        }
        if(numChoices > options.size()){
            return options;
        }
        List<Node> choices = new ArrayList<>(options);
        Collections.shuffle(choices, rng);
        return new HashSet<>(choices.subList(0, numChoices));
    }

    private Set<Link> chooseRandomSubsetLinks(Set<Link> options, Integer numChoices, Random rng){
        if(numChoices == 0){
            return new HashSet<>();
        }
        List<Link> choices = new ArrayList<>(options);
        Collections.shuffle(choices, rng);
        return new HashSet<>(choices.subList(0, numChoices));
    }

    private Algorithm getAlgorithm(String alg){
        return Algorithm.get(alg).orElse(Algorithm.ServiceILP);
    }

    private ProcessingType getProcessingType(String type){
        return ProcessingType.get(type).orElse(ProcessingType.Solo);
    }

    private FailureClass getFailureClass(String fClass){
        return FailureClass.get(fClass).orElse(FailureClass.Both);
    }

    private ProblemClass getProblemClass(String problemClass) {
        return ProblemClass.get(problemClass).orElse(ProblemClass.Flex);
    }

    private Objective getObjective(String objective) {
        return Objective.get(objective).orElse(Objective.LinksUsed);
    }

}
