package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.ProblemClass;
import netlab.submission.enums.ProcessingType;
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
                srcMaxConnectionsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> randomInt(minForMinConn, maxForMinConn, rng)));
                dstMinConnectionsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> randomInt(minForMinConn, maxForMinConn, rng)));
                dstMaxConnectionsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> randomInt(minForMinConn, maxForMinConn, rng)));

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
            failureGroups = generateFailureGroups(numFails, failureCollection.getFailureSet(), rng);
        }
        if(problemClass.equals(ProblemClass.Flow)){
            for(SourceDestPair pair : pairs){
                Set<Failure> thisFailureSet = failureCollection.getPairFailuresMap().getOrDefault(pair, failureCollection.getFailureSet());
                int thisNumFails = minMaxFails.size() == 2 ?
                        Math.min(thisFailureSet.size(), randomInt(minMaxFails.get(0), minMaxFails.get(1), rng)) : numFails;
                pairNumFailsMap.put(pair, thisNumFails);
                pairFailureGroupsMap.put(pair, generateFailureGroups(thisNumFails, thisFailureSet, rng));
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
        failureGroupsMap.put(member, generateFailureGroups(thisNumFails, failureSet, rng));
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

    private static List<List<Failure>> generateFailureGroups(Integer k, Set<Failure> failureSet, Random rng){
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
}
