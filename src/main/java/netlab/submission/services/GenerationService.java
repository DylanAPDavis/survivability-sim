package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.services.HashingService;
import netlab.submission.enums.*;
import netlab.submission.request.*;
import netlab.submission.simulate.Network;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class GenerationService {

    private TopologyService topologyService;

    private DefaultValueService defaultValueService;

    private EnumGenerationService enumGenerationService;

    private FailureGenerationService failureGenerationService;

    private SelectionService selectionService;

    private HashingService hashingService;

    @Autowired
    public GenerationService(TopologyService topologyService, DefaultValueService defaultValueService,
                             EnumGenerationService enumGenerationService, FailureGenerationService failureGenerationService,
                             SelectionService selectionService, HashingService hashingService) {
        this.topologyService = topologyService;
        this.defaultValueService = defaultValueService;
        this.enumGenerationService = enumGenerationService;
        this.failureGenerationService = failureGenerationService;
        this.selectionService = selectionService;
        this.hashingService = hashingService;
    }

    public Request generateFromSimParams(SimulationParameters params){

        params = defaultValueService.assignDefaults(params);
        Details details = createDetailsFromParameters(params);
        String setId = params.getRequestId() != null && !params.getRequestId().isEmpty()
                ? params.getRequestId().toLowerCase() : hashingService.createRequestId(params);
        params.setRequestId(setId);
        return Request.builder()
                .details(details)
                .completed(params.getCompleted())
                .id(setId)
                .seed(params.getSeed())
                .problemClass(enumGenerationService.getProblemClass(params.getProblemClass()))
                .algorithm(enumGenerationService.getAlgorithm(params.getAlgorithm()))
                .objective(enumGenerationService.getObjective(params.getObjective()))
                .failureClass(enumGenerationService.getFailureClass(params.getFailureClass()))
                .failureScenario(enumGenerationService.getFailureScenario(params.getFailureScenario()))
                .trafficCombinationType(enumGenerationService.getTrafficCombinationType(params.getTrafficCombinationType()))
                .routingType(enumGenerationService.getRoutingType(params.getRoutingType()))
                .sourceSubsetDestType(enumGenerationService.getSourceSubsetDestType(params.getSourceSubsetDestType()))
                .sourceFailureType(enumGenerationService.getMemberFailureType(params.getSourceFailureType()))
                .destFailureType(enumGenerationService.getMemberFailureType(params.getDestFailureType()))
                .useAws(params.getUseAws())
                .ignoreFailures(params.getIgnoreFailures())
                .topologyId(params.getTopologyId())
                .numThreads(params.getNumThreads())
                .cutoffTimeSeconds(params.getCutoffTimeSeconds())
                .timedOut(params.getTimedOut())
                .build();
    }


    public Request generateFromRequestParams(RequestParameters params, Network network) {
        params = defaultValueService.assignDefaults(params);
        Details details = createDetailsFromRequestParameters(params, network);
        String requestId = UUID.randomUUID().toString();
        Random rng = new Random();
        Long seed = ((long) (rng.nextDouble() * (1000L)));
        return Request.builder()
                .details(details)
                .completed(false)
                .id(requestId)
                .seed(seed)
                .problemClass(enumGenerationService.getProblemClass(params.getProblemClass()))
                .algorithm(enumGenerationService.getAlgorithm(params.getAlgorithm()))
                .objective(enumGenerationService.getObjective(params.getObjective()))
                .failureClass(FailureClass.Both)
                .failureScenario(FailureScenario.Default)
                .trafficCombinationType(enumGenerationService.getTrafficCombinationType(params.getTrafficCombinationType()))
                .routingType(enumGenerationService.getRoutingType(params.getRoutingType()))
                .sourceSubsetDestType(SourceSubsetDestType.None)
                .sourceFailureType(MemberFailureType.Allow)
                .destFailureType(MemberFailureType.Allow)
                .useAws(false)
                .topologyId(params.getTopologyId())
                .numThreads(params.getNumThreads())
                .cutoffTimeSeconds(3600)
                .timedOut(false)
                .build();
    }

    private Details createDetailsFromRequestParameters(RequestParameters params, Network network) {
        Topology topo = null;
        if(params.getTopologyId().equals("generated") && network != null){
            topo = topologyService.convert(network);
        } else{
            topo = topologyService.getTopologyById(params.getTopologyId());
        }
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

        Connections conns = makeConnectionsFromRequestParams(params, pairs, sources, destinations, topo.getNodeIdMap());
        NumFailureEvents nfe = failureGenerationService.makeNumFailureEventsFromRequestParams(params, pairs, sources, destinations, topo.getNodeIdMap());
        Failures fails = failureGenerationService.makeFailuresFromRequestParams(params, pairs, sources, destinations, nodeIdMap, linkIdMap,
                nfe.getTotalNumFailureEvents());

        return Details.builder()
                .sources(sources)
                .destinations(destinations)
                .pairs(pairs)
                .connections(conns)
                .failures(fails)
                .numFailureEvents(nfe)
                .chosenPaths(null)
                .isFeasible(false)
                .runningTimeSeconds(0L)
                .build();
    }

    private Connections makeConnectionsFromRequestParams(RequestParameters params, Set<SourceDestPair> pairs,
                                                         Set<Node> sources, Set<Node> destinations, Map<String, Node> nodeIdMap){

        RoutingType routingType = enumGenerationService.getRoutingType(params.getRoutingType());
        /*(if(routingType != RoutingType.Default){
            return makeConnectionsFromRoutingType(pairs, sources, destinations, params.getUseMinS(), params.getUseMaxS(),
                    params.getUseMinD(), params.getUseMaxD(), routingType);
        }*/
        // Map for pairs
        Map<SourceDestPair, Integer> pairMinConnectionsMap = params.getPairNumConnectionsMap().size() > 0 ?
                selectionService.makePairIntegerMap(pairs, params.getPairNumConnectionsMap(), 0, nodeIdMap) :
                selectionService.makePairIntegerMap(pairs, params.getPairMinNumConnectionsMap(), 0, nodeIdMap);
        Map<SourceDestPair, Integer> pairMaxConnectionsMap = params.getPairNumConnectionsMap().size() > 0 ?
                selectionService.makePairIntegerMap(pairs, params.getPairNumConnectionsMap(), params.getNumConnections(), nodeIdMap) :
                selectionService.makePairIntegerMap(pairs, params.getPairMaxNumConnectionsMap(), params.getNumConnections(), nodeIdMap);

        // Map for sources
        Map<Node, Integer> srcMinConnectionsMap = params.getSourceNumConnectionsMap().size() > 0 ?
                selectionService.makeNodeIntegerMap(sources, params.getSourceNumConnectionsMap(), 0, nodeIdMap) :
                selectionService.makeNodeIntegerMap(sources, params.getSourceMinNumConnectionsMap(), 0, nodeIdMap);
        Map<Node, Integer> srcMaxConnectionsMap = params.getSourceNumConnectionsMap().size() > 0 ?
                selectionService.makeNodeIntegerMap(sources, params.getSourceNumConnectionsMap(), params.getNumConnections(), nodeIdMap) :
                selectionService.makeNodeIntegerMap(sources, params.getSourceMaxNumConnectionsMap(), params.getNumConnections(), nodeIdMap);

        // Map for destinations
        Map<Node, Integer> dstMinConnectionsMap = params.getDestNumConnectionsMap().size() > 0 ?
                selectionService.makeNodeIntegerMap(destinations, params.getDestNumConnectionsMap(), 0, nodeIdMap) :
                selectionService.makeNodeIntegerMap(destinations, params.getDestMinNumConnectionsMap(), 0, nodeIdMap);
        Map<Node, Integer> dstMaxConnectionsMap = params.getDestNumConnectionsMap().size() > 0 ?
                selectionService.makeNodeIntegerMap(destinations, params.getDestNumConnectionsMap(), params.getNumConnections(), nodeIdMap) :
                selectionService.makeNodeIntegerMap(destinations, params.getDestMaxNumConnectionsMap(), params.getNumConnections(), nodeIdMap);

        return Connections.builder()
                .numConnections(params.getNumConnections())
                .useMinS(params.getUseMinS())
                .useMaxS(params.getUseMaxS())
                .useMinD(params.getUseMinD())
                .useMaxD(params.getUseMaxD())
                .pairMinConnectionsMap(pairMinConnectionsMap)
                .pairMaxConnectionsMap(pairMaxConnectionsMap)
                .srcMinConnectionsMap(srcMinConnectionsMap)
                .srcMaxConnectionsMap(srcMaxConnectionsMap)
                .dstMinConnectionsMap(dstMinConnectionsMap)
                .dstMaxConnectionsMap(dstMaxConnectionsMap)
                .build();
    }

    private Connections makeConnectionsFromRoutingType(Collection<SourceDestPair> pairs, Collection<Node> sources,
                                                       Collection<Node> destinations, Integer useMinS, Integer useMaxS,
                                                       Integer useMinD, Integer useMaxD, RoutingType routingType) {
        // Using the Routing Type and the other parameters, build a Connections object to store min/maxes
        switch(routingType){
            case Unicast:
                // 1 connection, 1 conns per pair, 1 conns per src, 1conns per dest, 1 srcs connected, 1 dests connected
                return buildConnections(pairs, sources, destinations, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1);
            case Anycast:
                // 1 connections, 0/1 conns per pair, 1 conns per src, 0/1 conns per dest, 1 srcs connected, 1 dst connected
                return buildConnections(pairs, sources, destinations, 1, 0, 1, 1, 1,
                        0, 1, 1, 1, 1, 1);
            case Manycast:
                // useMinD connections, 0/1 per pair, minD/maxD per src, 0/1 per dest, 1 src connected, useMinD/useMaxD dsts connected
                return buildConnections(pairs, sources, destinations, useMinD, 0, 1, useMinD, useMaxD,
                        0, 1, 1, 1, useMinD, useMaxD);
            case Multicast:
                // |D| connections, 1 per pair, |D| per src, 1 per dst, 1 src connected, |D| dsts connected
                return buildConnections(pairs, sources, destinations, destinations.size(), 1, 1, destinations.size(), destinations.size(),
                        1, 1, 1, 1, destinations.size(), destinations.size());
            case ManyToOne:
                // useMinS connections, 0/1 per pair, 0/1 per src, useMinS/useMaxS per dst, useMinS/useMaxS src connected, 1 dsts connected
                return buildConnections(pairs, sources, destinations, useMinS, 0, 1, 0, 1,
                        useMinS, useMaxS, useMinS, useMaxS, 1, 1);
            case ManyToMany:
                // max(useMinS, useMinD) connections, 0/1 per pair, 0/1 per src, 0/1 per dst, useMinS/useMaxS src connected, useMinD/useMaxD dsts connected
                return buildConnections(pairs, sources, destinations, Math.max(useMinS, useMinD), 0, 1, 0, destinations.size(),
                        0, sources.size(), useMinS, useMaxS, useMinD, useMaxD);
            case Broadcast:
                // |pairs| connections, 1/1 per pair, 0/|D| per src, 0/|S| per dst, |S| src connected, |D| dsts connected
                return buildConnections(pairs, sources, destinations, pairs.size(), 1, 1, 1, destinations.size(),
                        1, sources.size(), sources.size(), sources.size(), destinations.size(), destinations.size());
            default:
                return null;
        }

    }

    public Connections buildConnections(Collection<SourceDestPair> pairs, Collection<Node> sources,
                                        Collection<Node> destinations, int numC, int minPerPair, int maxPerPair,
                                        int minPerSource, int maxPerSource, int minPerDest, int maxPerDest,
                                        int useMinS, int useMaxS, int useMinD, int useMaxD){
        Integer numConnections = numC;
        Map<SourceDestPair, Integer> pairMinConnectionsMap = new HashMap<>();
        Map<SourceDestPair, Integer> pairMaxConnectionsMap = new HashMap<>();
        for(SourceDestPair pair : pairs){
            pairMinConnectionsMap.put(pair, minPerPair);
            pairMaxConnectionsMap.put(pair, maxPerPair);
        }
        Map<Node, Integer> srcMinConnectionsMap = new HashMap<>();
        Map<Node, Integer> srcMaxConnectionsMap = new HashMap<>();
        for(Node source : sources){
            srcMinConnectionsMap.put(source, minPerSource);
            srcMaxConnectionsMap.put(source, maxPerSource);
        }
        Map<Node, Integer> dstMinConnectionsMap = new HashMap<>();
        Map<Node, Integer> dstMaxConnectionsMap = new HashMap<>();
        for(Node dest : destinations){
            dstMinConnectionsMap.put(dest, minPerDest);
            dstMaxConnectionsMap.put(dest, maxPerDest);
        }


        return Connections.builder()
                .numConnections(numConnections)
                .useMinS(useMinS)
                .useMaxS(useMaxS)
                .useMinD(useMinD)
                .useMaxD(useMaxD)
                .pairMinConnectionsMap(pairMinConnectionsMap)
                .pairMaxConnectionsMap(pairMaxConnectionsMap)
                .srcMinConnectionsMap(srcMinConnectionsMap)
                .srcMaxConnectionsMap(srcMaxConnectionsMap)
                .dstMinConnectionsMap(dstMinConnectionsMap)
                .dstMaxConnectionsMap(dstMaxConnectionsMap)
                .build();
    }

    public Details createDetailsFromParameters(SimulationParameters params) {
        Topology topo = topologyService.getTopologyById(params.getTopologyId());
        if(topo == null){
            return null;
        }

        Random rng = new Random(params.getSeed());
        return createDetails(params, topo, rng);

    }

    public Details createDetails(SimulationParameters params, Topology topo, Random rng){

        Set<Node> sources = selectionService.pickSources(topo.getNodes(), params.getNumSources(), rng);
        SourceSubsetDestType sourceSubsetDestType = enumGenerationService.getSourceSubsetDestType(params.getSourceSubsetDestType());
        Set<Node> destinations = selectionService.pickDestinations(topo.getNodes(), params.getNumDestinations(), rng,
                sourceSubsetDestType, sources);

        Set<SourceDestPair> pairs = createPairs(sources, destinations);
        List<SourceDestPair> sortedPairs = new ArrayList<>(pairs);
        Comparator<SourceDestPair> bySrc = Comparator.comparing(p -> p.getSrc().getId());
        Comparator<SourceDestPair> byDst = Comparator.comparing(p -> p.getDst().getId());
        sortedPairs.sort(bySrc.thenComparing(byDst));

        List<Node> sortedSources = new ArrayList<>(sources);
        sortedSources.sort(Comparator.comparing(Node::getId));
        List<Node> sortedDests = new ArrayList<>(destinations);
        sortedDests.sort(Comparator.comparing(Node::getId));

        Failures failureCollection = failureGenerationService.assignFailureSets(params, sortedSources, sortedDests, sortedPairs, topo, rng);
        params.setFailureSetSize(failureCollection.getFailureSet().size());

        // Determine number of cuts
        NumFailureEvents numFailureEventsCollection = failureGenerationService.assignNumFails(params, sortedPairs, sortedSources, sortedDests, failureCollection, rng);


        // Determine number of connections
        Connections connectionsCollection = assignConnections(params, sortedPairs, sortedSources, sortedDests);


        return Details.builder()
                .sources(sources)
                .destinations(destinations)
                .connections(connectionsCollection)
                .failures(failureCollection)
                .numFailureEvents(numFailureEventsCollection)
                .pairs(pairs)
                .runningTimeSeconds(0L)
                .isFeasible(false)
                .build();
    }

    private Connections assignConnections(SimulationParameters params, Collection<SourceDestPair> pairs, Collection<Node> sources,
                                          Collection<Node> destinations){
        RoutingType routingType = enumGenerationService.getRoutingType(params.getRoutingType());
        /*if(routingType != RoutingType.Default){
            return makeConnectionsFromRoutingType(pairs, sources, destinations, params.getUseMinS(), params.getUseMaxS(),
                    params.getUseMinD(), params.getUseMaxD(), routingType);
        }*/
        // Connection params
        Integer numConnections = params.getMinConnections();
        Integer minPairConnections = params.getMinPairConnections();
        Integer maxPairConnections = params.getMaxPairConnections();
        Integer minSrcConnections = params.getMinSrcConnections();
        Integer maxSrcConnections = params.getMaxSrcConnections();
        Integer minDstConnections = params.getMinDstConnections();
        Integer maxDstConnections = params.getMaxDstConnections();

        Map<SourceDestPair, Integer> pairMinConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                p -> p.getSrc() == p.getDst() ? 0 : minPairConnections));
        Map<SourceDestPair, Integer> pairMaxConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                p -> p.getSrc() == p.getDst() ? 0 : maxPairConnections));

        Map<Node, Integer> srcMinConnectionsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> minSrcConnections));
        Map<Node, Integer> srcMaxConnectionsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> maxSrcConnections));
        Map<Node, Integer> dstMinConnectionsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> minDstConnections));
        Map<Node, Integer> dstMaxConnectionsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> maxDstConnections));

        return Connections.builder()
                .numConnections(numConnections)
                .pairMinConnectionsMap(pairMinConnectionsMap)
                .pairMaxConnectionsMap(pairMaxConnectionsMap)
                .srcMinConnectionsMap(srcMinConnectionsMap)
                .srcMaxConnectionsMap(srcMaxConnectionsMap)
                .dstMinConnectionsMap(dstMinConnectionsMap)
                .dstMaxConnectionsMap(dstMaxConnectionsMap)
                .useMinS(params.getUseMinS())
                .useMaxS(params.getUseMaxS())
                .useMinD(params.getUseMinD())
                .useMaxD(params.getUseMaxD())
                .build();
    }

    private Set<SourceDestPair> createPairs(Set<Node> sources, Set<Node> destinations) {
        Set<SourceDestPair> pairs = new HashSet<>();
        for(Node source : sources){
            for(Node dest: destinations){
                if(!source.getId().equals(dest.getId())) {
                    pairs.add(SourceDestPair.builder().src(source).dst(dest).build());
                }
            }
        }
        return pairs;
    }


}
