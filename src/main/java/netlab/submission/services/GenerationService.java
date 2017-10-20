package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.services.HashingService;
import netlab.submission.enums.*;
import netlab.submission.request.*;
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
        String status = "Processing";
        if(details == null){
            status = "Submission failed. Could not generate details.";
        }
        String setId = params.getRequestId() != null && !params.getRequestId().isEmpty()
                ? params.getRequestId() : hashParams(params);
        params.setRequestId(setId);
        return Request.builder()
                .details(details)
                .status(status)
                .id(setId)
                .seed(params.getSeed())
                .problemClass(enumGenerationService.getProblemClass(params.getProblemClass()))
                .algorithm(enumGenerationService.getAlgorithm(params.getAlgorithm()))
                .objective(enumGenerationService.getObjective(params.getObjective()))
                .failureClass(enumGenerationService.getFailureClass(params.getFailureClass()))
                .percentSrcAlsoDest(params.getPercentSrcAlsoDest())
                .percentSrcFail(params.getPercentSrcFail())
                .percentDestFail(params.getPercentDstFail())
                .sdn(params.getSdn())
                .useAws(params.getUseAws())
                .topologyId(params.getTopologyId())
                .numThreads(params.getNumThreads())
                .build();
    }

    private String hashParams(SimulationParameters params) {
        return hashingService.hash(params.getSeed(), params.getTopologyId(), params.getProblemClass(), params.getObjective(),
                params.getAlgorithm(),
                params.getNumRequests(), params.getNumSources(), params.getNumDestinations(), params.getMinConnections(),
                params.getMinPairConnections(), params.getMaxPairConnections(), params.getMinSrcConnections(),
                params.getMaxSrcConnections(), params.getMinDstConnections(), params.getMaxDstConnections(),
                params.getUseMinS(), params.getUseMaxS(), params.getUseMinD(), params.getUseMaxD(),
                params.getFailureSetSize(), params.getMinMaxFailures(), params.getFailureClass(), params.getFailureProb(),
                params.getMinMaxFailureProb(), params.getNumFailureEvents(), params.getMinMaxFailsAllowed(),
                params.getProcessingType(), params.getPercentSrcAlsoDest(), params.getPercentSrcFail(),
                params.getPercentDstFail(), params.getSdn(), params.getUseAws(), params.getIgnoreFailures(), params.getNumThreads());
    }

    public Request generateFromRequestParams(RequestParameters requestParameters) {
        requestParameters = defaultValueService.assignDefaults(requestParameters);
        Details details = createDetailsFromRequestParameters(requestParameters);
        String status = details == null ? "Submission failed. Could not generate details." : "Processing";
        String requestId = UUID.randomUUID().toString();
        Random rng = new Random();
        Long seed = ((long) (rng.nextDouble() * (1000L)));
        return Request.builder()
                .details(details)
                .status(status)
                .id(requestId)
                .seed(seed)
                .problemClass(enumGenerationService.getProblemClass(requestParameters.getProblemClass()))
                .algorithm(enumGenerationService.getAlgorithm(requestParameters.getAlgorithm()))
                .objective(enumGenerationService.getObjective(requestParameters.getObjective()))
                .failureClass(FailureClass.Both)
                .percentSrcAlsoDest(-1.0)
                .percentSrcFail(-1.0)
                .percentDestFail(-1.0)
                .sdn(requestParameters.getSdn())
                .useAws(requestParameters.getUseAws())
                .topologyId(requestParameters.getTopologyId())
                .numThreads(requestParameters.getNumThreads())
                .build();
    }

    private Details createDetailsFromRequestParameters(RequestParameters params) {
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
        NumFailsAllowed nfa = failureGenerationService.makeNumFailsAllowedFromRequestParams(params, pairs, sources, destinations);
        Failures fails = failureGenerationService.makeFailuresFromRequestParams(params, pairs, sources, destinations, nodeIdMap, linkIdMap,
                nfa.getTotalNumFailsAllowed(), nfa.getPairNumFailsAllowedMap(), nfa.getSrcNumFailsAllowedMap(),
                nfa.getDstNumFailsAllowedMap());

        return Details.builder()
                .sources(sources)
                .destinations(destinations)
                .pairs(pairs)
                .connections(conns)
                .failures(fails)
                .numFailsAllowed(nfa)
                .chosenPaths(null)
                .isFeasible(false)
                .runningTimeSeconds(0L)
                .ignoreFailures(params.getIgnoreFailures())
                .build();
    }

    private Connections makeConnectionsFromRequestParams(RequestParameters params, Set<SourceDestPair> pairs,
                                                         Set<Node> sources, Set<Node> destinations){

        // Map for pairs
        Map<SourceDestPair, Integer> pairMinConnectionsMap = params.getPairNumConnectionsMap().size() > 0 ?
                selectionService.makePairIntegerMap(pairs, params.getPairNumConnectionsMap(), 0) :
                selectionService.makePairIntegerMap(pairs, params.getPairMinNumConnectionsMap(), 0);
        Map<SourceDestPair, Integer> pairMaxConnectionsMap = params.getPairNumConnectionsMap().size() > 0 ?
                selectionService.makePairIntegerMap(pairs, params.getPairNumConnectionsMap(), params.getNumConnections()) :
                selectionService.makePairIntegerMap(pairs, params.getPairMaxNumConnectionsMap(), params.getNumConnections());

        // Map for sources
        Map<Node, Integer> srcMinConnectionsMap = params.getSourceNumConnectionsMap().size() > 0 ?
                selectionService.makeNodeIntegerMap(sources, params.getSourceNumConnectionsMap(), 0) :
                selectionService.makeNodeIntegerMap(sources, params.getSourceMinNumConnectionsMap(), 0);
        Map<Node, Integer> srcMaxConnectionsMap = params.getSourceNumConnectionsMap().size() > 0 ?
                selectionService.makeNodeIntegerMap(sources, params.getSourceNumConnectionsMap(), params.getNumConnections()) :
                selectionService.makeNodeIntegerMap(sources, params.getSourceMaxNumConnectionsMap(), params.getNumConnections());

        // Map for destinations
        Map<Node, Integer> dstMinConnectionsMap = params.getDestNumConnectionsMap().size() > 0 ?
                selectionService.makeNodeIntegerMap(destinations, params.getDestNumConnectionsMap(), 0) :
                selectionService.makeNodeIntegerMap(destinations, params.getDestMinNumConnectionsMap(), 0);
        Map<Node, Integer> dstMaxConnectionsMap = params.getDestNumConnectionsMap().size() > 0 ?
                selectionService.makeNodeIntegerMap(destinations, params.getDestNumConnectionsMap(), params.getNumConnections()) :
                selectionService.makeNodeIntegerMap(destinations, params.getDestMaxNumConnectionsMap(), params.getNumConnections());

        return Connections.builder()
                .numConnections(params.getNumConnections())
                .reachMinS(params.getReachMinSources())
                .reachMaxS(params.getReachMaxSources())
                .reachMinD(params.getReachMinDestinations())
                .reachMaxD(params.getReachMaxDestinations())
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
        Set<Node> destinations = selectionService.pickDestinations(topo.getNodes(), params.getNumDestinations(), rng,
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

        Failures failureCollection = failureGenerationService.assignFailureSets(params, sortedSources, sortedDests, sortedPairs, topo, rng);

        // Determine number of cuts
        NumFailsAllowed numFailsAllowedCollection = failureGenerationService.assignNumFails(params, sortedPairs, sortedSources, sortedDests, failureCollection, rng);


        // Determine number of connections
        Connections connectionsCollection = assignConnections(params, sortedPairs, sortedSources, sortedDests, rng);


        return Details.builder()
                .sources(sources)
                .destinations(destinations)
                .connections(connectionsCollection)
                .failures(failureCollection)
                .numFailsAllowed(numFailsAllowedCollection)
                .pairs(pairs)
                .runningTimeSeconds(0L)
                .isFeasible(false)
                .ignoreFailures(params.getIgnoreFailures())
                .build();
    }

    private Connections assignConnections(SimulationParameters params, List<SourceDestPair> pairs, List<Node> sources,
                                          List<Node> destinations, Random rng){
        // Connection params
        Integer numConnections = params.getMinConnections();
        List<Integer> minConnectionsRange = params.getMinPairConnections();
        List<Integer> maxConnectionsRange = params.getMaxPairConnections();

        List<Integer> minSrcConnectionsRange = params.getMinSrcConnections();
        List<Integer> maxSrcConnectionsRange = params.getMaxSrcConnections();
        List<Integer> minDstConnectionsRange = params.getMinDstConnections();
        List<Integer> maxDstConnectionsRange = params.getMaxDstConnections();

        // GENERATE VALUES FOR PAIRS
        Integer minForMinConn = minConnectionsRange.size() > 0 ? minConnectionsRange.get(0) : 0;
        Integer maxForMinConn = minConnectionsRange.size() > 0 ? minConnectionsRange.get(minConnectionsRange.size()-1): 0;
        Integer minForMaxConn = maxConnectionsRange.size() > 0 ? maxConnectionsRange.get(0) : numConnections;
        Integer maxForMaxConn = maxConnectionsRange.size() > 0 ? maxConnectionsRange.get(maxConnectionsRange.size()-1) : numConnections;
        // Give random min/max num of connections per pair
        // If src = dst for a pair, both numbers are 0
        Map<SourceDestPair, Integer> pairMinConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                p -> p.getSrc() == p.getDst() ? 0 : selectionService.randomInt(minForMinConn, maxForMinConn, rng)));
        Map<SourceDestPair, Integer> pairMaxConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                p -> p.getSrc() == p.getDst() ? 0 : selectionService.randomInt(minForMaxConn, maxForMaxConn, rng)));

        // GENERATE VALUES FOR SOURCES & DESTINATIONS
        int minForSrcMin = minSrcConnectionsRange.size() > 0 ? minSrcConnectionsRange.get(0) : 0;
        int maxForSrcMin = minSrcConnectionsRange.size() > 0 ? minSrcConnectionsRange.get(minSrcConnectionsRange.size() -1) : 0;

        int minForSrcMax = maxSrcConnectionsRange.size() > 0 ? maxSrcConnectionsRange.get(0) : numConnections;
        int maxForSrcMax = maxSrcConnectionsRange.size() > 0 ? maxSrcConnectionsRange.get(maxSrcConnectionsRange.size() - 1) : numConnections;

        int minForDstMin = minDstConnectionsRange.size() > 0 ? minDstConnectionsRange.get(0) : 0;
        int maxForDstMin = minDstConnectionsRange.size() > 0 ? minDstConnectionsRange.get(minDstConnectionsRange.size() -1) : 0;

        int minForDstMax = maxDstConnectionsRange.size() > 0 ? maxDstConnectionsRange.get(0) : numConnections;
        int maxForDstMax = maxDstConnectionsRange.size() > 0 ? maxDstConnectionsRange.get(maxDstConnectionsRange.size() -1) : numConnections;

        Map<Node, Integer> srcMinConnectionsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> selectionService.randomInt(minForSrcMin, maxForSrcMin, rng)));
        Map<Node, Integer> srcMaxConnectionsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> selectionService.randomInt(minForSrcMax, maxForSrcMax, rng)));
        Map<Node, Integer> dstMinConnectionsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> selectionService.randomInt(minForDstMin, maxForDstMin, rng)));
        Map<Node, Integer> dstMaxConnectionsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> selectionService.randomInt(minForDstMax, maxForDstMax, rng)));

        return Connections.builder()
                .numConnections(numConnections)
                .pairMinConnectionsMap(pairMinConnectionsMap)
                .pairMaxConnectionsMap(pairMaxConnectionsMap)
                .srcMinConnectionsMap(srcMinConnectionsMap)
                .srcMaxConnectionsMap(srcMaxConnectionsMap)
                .dstMinConnectionsMap(dstMinConnectionsMap)
                .dstMaxConnectionsMap(dstMaxConnectionsMap)
                .reachMinS(params.getUseMinS())
                .reachMaxS(params.getUseMaxS())
                .reachMinD(params.getUseMinD())
                .reachMaxD(params.getUseMaxD())
                .build();
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


}
