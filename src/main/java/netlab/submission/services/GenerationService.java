package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.OverlapType;
import netlab.submission.enums.ProcessingType;
import netlab.submission.request.Request;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyService;
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
                .algorithm(getAlgorithm(params.getAlgorithm()))
                .processingType(getProcessingType(params.getProcessingType()))
                .failureClass(getFailureClass(params.getFailureClass()))
                .overlapType(getOverlapType(params.getSrcDstOverlap()))
                .sdn(params.getSdn())
                .useAws(params.getUseAws())
                .topologyId(params.getTopologyId())
                .build();
    }

    private void assignDefaults(SimulationParameters params) {

        if(params.getNumFailures() == null || params.getNumFailures() < 0){
            params.setNumFailures(0);
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
        if(params.getNumCuts() == null || params.getNumCuts() < 0){
            params.setNumCuts(0);
        }
        if(params.getMinMaxCuts() == null){
            params.setMinMaxCuts(new ArrayList<>());
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
        if(params.getSrcFailuresAllowed() == null){
            params.setSrcFailuresAllowed(false);
        }
        if(params.getDstFailuresAllowed() == null){
            params.setSrcFailuresAllowed(false);
        }
        if(params.getSrcDstOverlap() == null){
            params.setSrcDstOverlap("None");
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
        // Connection params
        Integer numConnections = params.getNumConnections();
        List<Integer> minConnectionsRange = params.getMinConnectionsRange();
        List<Integer> maxConnectionsRange = params.getMaxConnectionsRange();

        // Cut params
        Integer numCuts = params.getNumCuts();
        List<Integer> minMaxCuts = params.getMinMaxCuts();

        // Failure params
        Integer numFailures = params.getNumFailures();
        List<Integer> minMaxFailures = params.getMinMaxFailures();
        FailureClass failureClass = getFailureClass(params.getFailureClass());

        Set<Node> sources = pickSources(topo.getNodes(), params.getNumSources(), rng);
        Set<Node> destinations = pickDestinations(topo.getNodes(), params.getNumDestinations(), rng,
                getOverlapType(params.getSrcDstOverlap()), sources);
        Set<SourceDestPair> pairs = createPairs(sources, destinations);

        List<SourceDestPair> sortedPairs = new ArrayList<>(pairs);
        Comparator<SourceDestPair> bySrc = Comparator.comparing(p -> p.getSrc().getId());
        Comparator<SourceDestPair> byDst = Comparator.comparing(p -> p.getDst().getId());
        sortedPairs.sort(bySrc.thenComparing(byDst));

        // Create failures
        Set<Failure> failures = null;
        Map<SourceDestPair, Set<Failure>> failuresMap = null;
        // If a min and max aren't specified, or they are equal, just create a set of failures
        if(minMaxFailures.size() < 2 || minMaxFailures.get(0).equals(minMaxFailures.get(1)) && numFailures > -1){
            failures = generateFailureSet(topo.getNodes(), topo.getLinks(), sources, destinations,
                    params.getSrcFailuresAllowed(), params.getDstFailuresAllowed(), numFailures, failureClass,
                    params.getFailureProb(), params.getMinMaxFailureProb(), rng);
        }
        // Otherwise create a unique set for each connection pair
        else{
            failuresMap = new HashMap<>();
            for(SourceDestPair pair : pairs){
                Integer randomNumFailures = randomInt(minMaxFailures.get(0), minMaxFailures.get(1), rng);
                Set<Failure> failureSet = generateFailureSet(topo.getNodes(), topo.getLinks(), Collections.singleton(pair.getSrc()),
                        Collections.singleton(pair.getDst()), params.getSrcFailuresAllowed(), params.getDstFailuresAllowed(),
                        randomNumFailures, failureClass, params.getFailureProb(), params.getMinMaxFailureProb(), rng);
                failuresMap.put(pair, failureSet);
            }
        }

        // Determine number of cuts
        Map<SourceDestPair, Integer> numCutsMap = new HashMap<>();
        if(minMaxCuts.size() == 2){
            Integer minCuts = minMaxCuts.get(0);
            Integer maxCuts = minMaxCuts.get(1);
            // Assign random number of cuts between min and max
            // Except: cap out at the number of failures for a pair, so you're not trying to cut more than than the
            // size of the failure set
            for(SourceDestPair pair : pairs){
                Integer numFails = failuresMap != null ? failuresMap.get(pair).size() : failures.size();
                numCutsMap.put(pair, Math.min(numFails, randomInt(minCuts, maxCuts, rng)));
            }
            //Update number of required cuts for request to be equal to the total min
            numCuts = numCutsMap.values().stream().reduce(0, (c1, c2) -> c1 + c2);
        }
        else{
            for(SourceDestPair pair : pairs){
                Integer numFails = failuresMap != null ? failuresMap.get(pair).size() : failures.size();
                numCutsMap.put(pair, Math.min(numFails, params.getNumCuts()));
            }
        }

        // Determine number of connections
        Map<SourceDestPair, Integer> minConnectionsMap;
        Map<SourceDestPair, Integer> maxConnectionsMap;
        if(minConnectionsRange.size() == 2 && maxConnectionsRange.size() == 2){
            // Get the minimum/maximum for generating mins (index 0) and maxes (index 1)
            Integer minForMinConn = minConnectionsRange.get(0);
            Integer maxForMinConn = minConnectionsRange.get(1);
            Integer minForMaxConn = maxConnectionsRange.get(0);
            Integer maxForMaxConn = maxConnectionsRange.get(1);
            // Give random min/max num of connections per pair
            // If src = dst for a pair, both numbers are 0
            minConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                    p -> p.getSrc() == p.getDst() ? 0 : randomInt(minForMinConn, maxForMinConn, rng)));
            maxConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                    p -> p.getSrc() == p.getDst() ? 0 : randomInt(minForMaxConn, maxForMaxConn, rng)));

            //Update number of required connections for request to be equal to the total min
            numConnections = minConnectionsMap.values().stream().reduce(0, (c1, c2) -> c1 + c2);
        }
        else{
            // If no max or mins were set, give every pair a min of 0 and a max of the requested number of conns
            // If src = dst for a pair, both numbers are 0
            minConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
            maxConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                    p -> p.getSrc() == p.getDst() ? 0 : params.getNumConnections()));
        }


        return Request.builder()
                .id(UUID.randomUUID().toString())
                .sources(sources)
                .destinations(destinations)
                .numConnections(numConnections)
                .failures(failures)
                .numCuts(numCuts)
                .pairs(pairs)
                .minConnectionsMap(minConnectionsMap)
                .maxConnectionsMap(maxConnectionsMap)
                .numCutsMap(numCutsMap)
                .failuresMap(failuresMap)
                .build();
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
        return new HashSet<>(chooseRandomSubsetNodes(new ArrayList<>(nodes), numSources, rng));
    }

    private Set<Node> pickDestinations(Set<Node> nodes, Integer numDestinations, Random rng, OverlapType sourceDestOverlap,
                                       Set<Node> sources) {
        Set<Node> remainingNodes = new HashSet<>(nodes);
        if(sourceDestOverlap.equals(OverlapType.None)){
            remainingNodes.removeAll(sources);
        }
        else if(sourceDestOverlap.equals(OverlapType.Total)){
            return sources;
        }
        return new HashSet<>(chooseRandomSubsetNodes(new ArrayList<>(remainingNodes), numDestinations, rng));
    }

    private Set<Failure> generateFailureSet(Set<Node> nodes, Set<Link> links, Set<Node> sources, Set<Node> destinations,
                                            Boolean srcFailuresAllowed, Boolean dstFailuresAllowed, Integer numFailures,
                                            FailureClass failureClass, Double probability, List<Double> minMaxFailureProb, Random rng) {

        if(numFailures == 0){
            return new HashSet<>();
        }
        List<Link> chosenLinks = new ArrayList<>();
        List<Node> chosenNodes = new ArrayList<>();

        List<Link> linkOptions = new ArrayList<>(links);
        if(failureClass.equals(FailureClass.Link)){
            chosenLinks = chooseRandomSubsetLinks(linkOptions, numFailures, rng);
        }

        // remove any nodes, if necessary
        List<Node> nodeOptions = new ArrayList<>(nodes);
        if(!srcFailuresAllowed){
            nodeOptions.removeAll(sources);
        }
        if(!dstFailuresAllowed){
            nodeOptions.removeAll(destinations);
        }
        if(failureClass.equals(FailureClass.Node)){
            chosenNodes = chooseRandomSubsetNodes(nodeOptions, numFailures, rng);
        }

        if(failureClass.equals(FailureClass.Both)){
            Integer numNodeFailures = rng.nextInt(numFailures);
            Integer numLinkFailures = numFailures - numNodeFailures;
            chosenNodes = chooseRandomSubsetNodes(nodeOptions, numNodeFailures, rng);
            chosenLinks = chooseRandomSubsetLinks(linkOptions, numLinkFailures, rng);
        }

        // Determine probabilities
        List<Double> probabilities = generateProbabilities(probability, minMaxFailureProb, numFailures, rng);

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

    private List<Node> chooseRandomSubsetNodes(List<Node> options, Integer numChoices, Random rng) {
        if(numChoices == 0){
            return new ArrayList<>();
        }
        Collections.shuffle(options, rng);
        return options.subList(0, numChoices);
    }

    private List<Link> chooseRandomSubsetLinks(List<Link> options, Integer numChoices, Random rng){
        if(numChoices == 0){
            return new ArrayList<>();
        }
        Collections.shuffle(options, rng);
        return options.subList(0, numChoices);
    }

    private Algorithm getAlgorithm(String alg){
        return Algorithm.get(alg).orElse(Algorithm.FlexibleILP);
    }

    private ProcessingType getProcessingType(String type){
        return ProcessingType.get(type).orElse(ProcessingType.Solo);
    }

    private FailureClass getFailureClass(String fClass){
        return FailureClass.get(fClass).orElse(FailureClass.Both);
    }

    private OverlapType getOverlapType(String oType){
        return OverlapType.get(oType).orElse(OverlapType.None);
    }
}
