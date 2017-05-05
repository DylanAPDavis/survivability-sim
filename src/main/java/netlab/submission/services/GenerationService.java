package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.ProblemClass;
import netlab.submission.enums.ProcessingType;
import netlab.submission.request.*;
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
                .problemClass(getProblemClass(params.getProblemClass()))
                .algorithm(getAlgorithm(params.getAlgorithm()))
                .processingType(getProcessingType(params.getProcessingType()))
                .failureClass(getFailureClass(params.getFailureClass()))
                .percentSrcAlsoDest(params.getPercentSrcAlsoDest())
                .percentSrcFail(params.getPercentSrcFail())
                .percentDestFail(params.getPercentDestFail())
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
        if(params.getNumFails() == null || params.getNumFails() < 0){
            params.setNumFails(0);
        }
        if(params.getMinMaxFails() == null){
            params.setMinMaxFails(new ArrayList<>());
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
        if(params.getPercentSrcFail() == null || params.getPercentSrcFail() < 0.0){
            params.setPercentSrcFail(0.0);
        }
        if(params.getPercentDestFail() == null || params.getPercentDestFail() < 0.0){
            params.setPercentDestFail(0.0);
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

        List<Node> sortedSources = sortedPairs.stream().map(SourceDestPair::getSrc).collect(Collectors.toList());
        List<Node> sortedDests = sortedPairs.stream().map(SourceDestPair::getDst).collect(Collectors.toList());

        Failures failureCollection = assignFailureSets(params, sortedSources, sortedDests, sortedPairs, topo, rng);

        // Determine number of cuts
        NumFails numFailsCollection = assignNumFails(params, sortedPairs, failureCollection, rng);


        // Determine number of connections
        Connections connectionsCollection = assignConnections(params, sortedPairs, rng);


        return Request.builder()
                .id(UUID.randomUUID().toString())
                .sources(sources)
                .destinations(destinations)
                .connections(connectionsCollection)
                .failures(failureCollection)
                .numFails(numFailsCollection)
                .pairs(pairs)
                .build();
    }

    private Connections assignConnections(SimulationParameters params, List<SourceDestPair> pairs, Random rng){
        ProblemClass problemClass = getProblemClass(params.getProblemClass());
        // Connection params
        Integer numConnections = params.getNumConnections();
        List<Integer> minConnectionsRange = params.getMinConnectionsRange();
        List<Integer> maxConnectionsRange = params.getMaxConnectionsRange();

        Map<SourceDestPair, Integer> pairMinConnectionsMap = new HashMap<>();
        Map<SourceDestPair, Integer> pairMaxConnectionsMap = new HashMap<>();

        if(problemClass.equals(ProblemClass.Flex) && minConnectionsRange.size() == 2 && maxConnectionsRange.size() == 2){
            numConnections = randomInt(minConnectionsRange.get(0), maxConnectionsRange.get(1), rng);
        }
        else {
            if(minConnectionsRange.size() == 2 && maxConnectionsRange.size() == 2){
                // Get the minimum/maximum for generating mins (index 0) and maxes (index 1)
                Integer minForMinConn = minConnectionsRange.get(0);
                Integer maxForMinConn = minConnectionsRange.get(1);
                Integer minForMaxConn = maxConnectionsRange.get(0);
                Integer maxForMaxConn = maxConnectionsRange.get(1);
                if(problemClass.equals(ProblemClass.Flow)){
                    // Give random min/max num of connections per pair
                    // If src = dst for a pair, both numbers are 0
                    pairMinConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                            p -> p.getSrc() == p.getDst() ? 0 : randomInt(minForMinConn, maxForMinConn, rng)));
                    pairMaxConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                            p -> p.getSrc() == p.getDst() ? 0 : randomInt(minForMaxConn, maxForMaxConn, rng)));

                    //Update number of required connections for request to be equal to the total min
                    if(numConnections == 0)
                        numConnections = pairMinConnectionsMap.values().stream().reduce(0, (c1, c2) -> c1 + c2);
                }
            }
            else{
                if(problemClass.equals(ProblemClass.Flow)){
                    pairMinConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
                    pairMaxConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p,
                            p -> p.getSrc() == p.getDst() ? 0 : params.getNumConnections()));
                }
            }
        }

        return Connections.builder()
                .numConnections(numConnections)
                .pairMinConnectionsMap(pairMinConnectionsMap)
                .pairMaxConnectionsMap(pairMaxConnectionsMap)
                .build();
    }

    private NumFails assignNumFails(SimulationParameters params, List<SourceDestPair> pairs, Failures failureCollection,
                                    Random rng) {
        ProblemClass problemClass = getProblemClass(params.getProblemClass());

        // Cut params
        Integer numFails = params.getNumFails();
        List<Integer> minMaxFails = params.getMinMaxFails();

        Map<SourceDestPair, Integer> pairNumFailsMap = new HashMap<>();

        // Assign random number of cuts between min and max
        // Except: cap out at the number of failures for a pair, so you're not trying to cut more than than the
        // size of the failure set
        if(problemClass.equals(ProblemClass.Flex) && minMaxFails.size() == 2){
            numFails = Math.min(failureCollection.getFailures().size(), randomInt(minMaxFails.get(0), minMaxFails.get(1), rng));
        }
        if(problemClass.equals(ProblemClass.Flow)){
            for(SourceDestPair pair : pairs){
                int failSetSize = failureCollection.getPairFailuresMap().getOrDefault(pair, failureCollection.getFailures()).size();
                int thisNumFails = minMaxFails.size() == 2 ?
                        Math.min(failSetSize, randomInt(minMaxFails.get(0), minMaxFails.get(1), rng)) : numFails;
                pairNumFailsMap.put(pair, thisNumFails);
            }
            //Update number of required cuts for request to be equal to the total min
            numFails = pairNumFailsMap.values().stream().reduce(0, (c1, c2) -> c1 + c2);
        }

        return NumFails.builder()
                .totalNumFails(numFails)
                .pairNumFailsMap(pairNumFailsMap)
                .build();
    }

    private Failures assignFailureSets(SimulationParameters params, List<Node> sources,
                                       List<Node> destinations, List<SourceDestPair> sortedPairs,
                                       Topology topo, Random rng){

        Integer numFailures = params.getNumFailures();
        List<Integer> minMaxFailures = params.getMinMaxFailures();
        FailureClass failureClass = getFailureClass(params.getFailureClass());

        ProblemClass problemClass = getProblemClass(params.getProblemClass());

        Set<Failure> failures = new HashSet<>();
        Map<SourceDestPair, Set<Failure>> pairFailuresMap = new HashMap<>();

        // Based on ProblemClass and numFailures / minMaxFailures input, generate the number of needed failures
        // If Flex, use the total number of failures
        // Otherwise, use min/max, unless that field isn't set.
        // Create failures
        Set<Node> srcDstFailures = choosePercentageSubsetNodes(new HashSet<>(sources), params.getPercentSrcFail(), rng);
        srcDstFailures.addAll(choosePercentageSubsetNodes(new HashSet<>(destinations), params.getPercentDestFail(), rng));

        Integer failureSetSize = numFailures != null ? numFailures : 0;

        if(minMaxFailures.size() < 2 || minMaxFailures.get(0).equals(minMaxFailures.get(1))){
            numFailures = minMaxFailures.size() > 1 ? minMaxFailures.get(1) : numFailures;
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
            else if(problemClass.equals(ProblemClass.Flex)){
                failureSetSize = randomInt(minMaxFailures.get(0), minMaxFailures.get(1), rng);
                failures = generateFailureSet(topo.getNodes(), topo.getLinks(),
                        failureSetSize, failureClass, params.getFailureProb(), params.getMinMaxFailureProb(),
                        sources, destinations, srcDstFailures, rng);
            }
        }

        return Failures.builder()
                .failureSetSize(failureSetSize)
                .failures(failures)
                .pairFailuresMap(pairFailuresMap)
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
            remainingNodes.removeAll(chosenNodes);
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
        // If we still haven't gotten enough failures, make some more
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
}
