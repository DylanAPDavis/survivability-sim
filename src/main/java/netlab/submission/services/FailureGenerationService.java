package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.Failures;
import netlab.submission.request.NumFailsAllowed;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.SimulationParameters;
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
public class FailureGenerationService {

    private SelectionService selectionService;

    private EnumGenerationService enumGenerationService;

    private TopologyService topologyService;

    @Autowired
    public FailureGenerationService(SelectionService selectionService, EnumGenerationService enumGenerationService, TopologyService topologyService) {
        this.selectionService = selectionService;
        this.enumGenerationService = enumGenerationService;
        this.topologyService = topologyService;
    }

    public Failures makeFailuresFromRequestParams(RequestParameters params, Set<SourceDestPair> pairs,
                                                  Set<Node> sources, Set<Node> destinations,
                                                  Map<String, Node> nodeIdMap, Map<String, Link> linkIdMap,
                                                  Integer numFailsAllowed, Map<SourceDestPair, Integer> pairNumFailsAllowed,
                                                  Map<Node, Integer> srcNumFailsAllowed, Map<Node, Integer> dstNumFailsAllowed){

        // Failures for the whole request
        Set<Failure> failures = makeFailureSet(params.getFailures(), params.getFailureProbabilityMap(), nodeIdMap, linkIdMap);
        List<List<Failure>> failureGroups = generateFailureGroups(numFailsAllowed, filterFailureSet(failures, linkIdMap));

        // Failure for pairs
        Map<SourceDestPair, Set<Failure>> pairFailuresMap = makePairFailuresMap(pairs, params.getPairFailureMap(),
                params.getPairFailureProbabilityMap(), nodeIdMap, linkIdMap);
        Map<SourceDestPair, List<List<Failure>>> pairFailureGroupsMap = pairFailuresMap.keySet().stream()
                .collect(Collectors.toMap(p -> p, p -> generateFailureGroups(pairNumFailsAllowed.get(p), filterFailureSet(pairFailuresMap.get(p), linkIdMap))));

        // Failures for sources
        Map<Node, Set<Failure>> srcFailuresMap = makeNodeFailuresMap(sources, params.getSourceFailureMap(),
                params.getSourceFailureProbabilityMap(), nodeIdMap, linkIdMap);
        Map<Node, List<List<Failure>>> srcFailureGroupsMap = srcFailuresMap.keySet().stream()
                .collect(Collectors.toMap(p -> p, p -> generateFailureGroups(srcNumFailsAllowed.get(p), filterFailureSet(srcFailuresMap.get(p), linkIdMap))));

        // Failures for destinations
        Map<Node, Set<Failure>> dstFailuresMap = makeNodeFailuresMap(destinations, params.getDestFailureMap(),
                params.getDestFailureProbabilityMap(), nodeIdMap, linkIdMap);
        Map<Node, List<List<Failure>>> dstFailureGroupsMap = dstFailuresMap.keySet().stream()
                .collect(Collectors.toMap(p -> p, p -> generateFailureGroups(dstNumFailsAllowed.get(p), filterFailureSet(dstFailuresMap.get(p), linkIdMap))));

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

    private Set<Failure> filterFailureSet(Set<Failure> failures, Map<String, Link> linkIdMap) {
        Set<Failure> filteredFailures = new HashSet<>();
        Map<String, Failure> linkIdToFailMap = failures.stream().filter(f -> f.getLink() != null).collect(Collectors.toMap(f -> f.getLink().getId(), f -> f));
        for(Failure failure: failures){
            if(failure.getLink() != null){
                Link thisLink = failure.getLink();
                String[] splitId = thisLink.getId().split("-");
                String revId = "";
                if(splitId.length > 2){
                    revId = splitId[1] + "-" + splitId[0] + "-" + splitId[2];
                }
                else{
                    revId = splitId[1] + "-" + splitId[0];
                }
                //String revId = thisLink.getTarget().getId() + "-" + thisLink.getOrigin().getId();
                Failure revFailure = linkIdToFailMap.get(revId);
                if(revFailure == null || !filteredFailures.contains(revFailure)){
                    filteredFailures.add(failure);
                }
            }
            else{
                filteredFailures.add(failure);
            }
        }
        return filteredFailures;
    }

    private Map<Node,Set<Failure>> makeNodeFailuresMap(Set<Node> members, Map<String, Set<String>> memberFailureMap,
                                                       Map<String, Map<String, Double>> memberFailureProbabilityMap, Map<String, Node> nodeIdMap, Map<String, Link> linkIdMap) {
        Map<Node, Set<Failure>> failureMap = members.stream().collect(Collectors.toMap(p -> p, p -> new HashSet<>()));
        for(String memberString : memberFailureMap.keySet()){
            Node member = Node.builder().id(memberString).build();
            failureMap.put(member, makeFailureSet(memberFailureMap.get(memberString), memberFailureProbabilityMap.getOrDefault(memberString, new HashMap<>()), nodeIdMap, linkIdMap));
        }
        return failureMap;
    }

    private Map<SourceDestPair,Set<Failure>> makePairFailuresMap(Set<SourceDestPair> pairs, Map<List<String>, Set<String>> pairFailureMap,
                                                                 Map<List<String>, Map<String, Double>> pairFailureProbabilityMap,
                                                                 Map<String, Node> nodeIdMap, Map<String, Link> linkIdMap) {
        Map<SourceDestPair, Set<Failure>> failureMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> new HashSet<>()));
        for(List<String> pairList : pairFailureMap.keySet()){
            SourceDestPair pair = SourceDestPair.builder()
                    .src(Node.builder().id(pairList.get(0)).build())
                    .dst(Node.builder().id(pairList.get(1)).build())
                    .build();
            failureMap.put(pair, makeFailureSet(pairFailureMap.get(pairList), pairFailureProbabilityMap.getOrDefault(pairList, new HashMap<>()), nodeIdMap, linkIdMap));
        }
        return failureMap;
    }

    private Set<Failure> makeFailureSet(Set<String> failureStrings, Map<String, Double> probabilityMap,  Map<String, Node> nodeIdMap,
                                        Map<String, Link> linkIdMap){
        Set<Failure> failures = new HashSet<>();
        for(String failString : failureStrings){
            Double prob = probabilityMap.getOrDefault(failString, 1.0);
            if(nodeIdMap.containsKey(failString)){
                failures.add(Failure.builder().node(nodeIdMap.get(failString)).probability(prob).build());
            }
            else if(linkIdMap.containsKey(failString)){
                failures.add(Failure.builder().link(linkIdMap.get(failString)).probability(prob).build());
            }
        }
        return failures;
    }

    public NumFailsAllowed makeNumFailsAllowedFromRequestParams(RequestParameters params, Set<SourceDestPair> pairs,
                                                                Set<Node> sources, Set<Node> destinations) {
        // Map for pairs
        Map<SourceDestPair, Integer> pairNumFailsMap = selectionService.makePairIntegerMap(pairs, params.getPairNumFailsAllowedMap(), 0);

        // Map for sources
        Map<Node, Integer> srcNumFailsMap = selectionService.makeNodeIntegerMap(sources, params.getSourceNumFailsAllowedMap(), 0);

        // Map for destinations
        Map<Node, Integer> dstNumFailsMap = selectionService.makeNodeIntegerMap(destinations, params.getDestNumFailsAllowedMap(), 0);

        return NumFailsAllowed.builder()
                .totalNumFailsAllowed(params.getNumFailsAllowed())
                .pairNumFailsAllowedMap(pairNumFailsMap)
                .srcNumFailsAllowedMap(srcNumFailsMap)
                .dstNumFailsAllowedMap(dstNumFailsMap)
                .build();
    }

    public NumFailsAllowed assignNumFails(SimulationParameters params, List<SourceDestPair> pairs, List<Node> sources,
                                          List<Node> destinations, Failures failureCollection, Random rng) {
        ProblemClass problemClass = enumGenerationService.getProblemClass(params.getProblemClass());

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

        Topology topo = topologyService.getTopologyById(params.getTopologyId());

        // Assign random number of cuts between min and max
        // Except: cap out at the number of failureSet for a pair, so you're not trying to cut more than than the
        // size of the failure set
        if(problemClass.equals(ProblemClass.Flex) || problemClass.equals(ProblemClass.FlowSharedF) || problemClass.equals(ProblemClass.EndpointSharedF)){
            if(minMaxFails.size() == 2) {
                numFails = Math.min(failureCollection.getFailureSet().size(), selectionService.randomInt(minMaxFails.get(0), minMaxFails.get(1), rng));
            }
            failureGroups = generateFailureGroups(numFails, filterFailureSet(failureCollection.getFailureSet(), topo.getLinkIdMap()));
        }
        if(problemClass.equals(ProblemClass.Flow)){
            for (SourceDestPair pair : pairs) {
                Set<Failure> thisFailureSet = failureCollection.getPairFailuresMap().getOrDefault(pair, failureCollection.getFailureSet());
                int thisNumFails = minMaxFails.size() == 2 ?
                        Math.min(thisFailureSet.size(), selectionService.randomInt(minMaxFails.get(0), minMaxFails.get(1), rng)) : numFails;
                pairNumFailsMap.put(pair, thisNumFails);
                pairFailureGroupsMap.put(pair, generateFailureGroups(thisNumFails, filterFailureSet(thisFailureSet, topo.getLinkIdMap())));
            }
            //Update number of required cuts for request to be equal to the total min
            numFails = pairNumFailsMap.values().stream().reduce(0, (c1, c2) -> c1 + c2);

        }
        if(problemClass.equals(ProblemClass.Endpoint)){
            for(Node source : sources){
                Set<Failure> failureSet = failureCollection.getSrcFailuresMap().getOrDefault(source, failureCollection.getFailureSet());
                populateNumFailsAndFailureGroupMap(filterFailureSet(failureSet, topo.getLinkIdMap()), source, minMaxFails, numFails, srcNumFailsMap, srcFailureGroupsMap, rng);
            }
            for(Node dest : destinations){
                Set<Failure> failureSet = failureCollection.getDstFailuresMap().getOrDefault(dest, failureCollection.getFailureSet());
                populateNumFailsAndFailureGroupMap(filterFailureSet(failureSet, topo.getLinkIdMap()), dest, minMaxFails, numFails, dstNumFailsMap, dstFailureGroupsMap, rng);
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
                Math.min(failureSet.size(), selectionService.randomInt(minMaxFails.get(0), minMaxFails.get(1), rng)) : numFails;
        numFailsMap.put(member, thisNumFails);
        failureGroupsMap.put(member, generateFailureGroups(thisNumFails, failureSet));
    }

    public Failures assignFailureSets(SimulationParameters params, List<Node> sources,
                                      List<Node> destinations, List<SourceDestPair> sortedPairs,
                                      Topology topo, Random rng){

        Integer numFailures = params.getFailureSetSize();
        List<Integer> minMaxFailures = params.getMinMaxFailures();
        FailureClass failureClass = enumGenerationService.getFailureClass(params.getFailureClass());

        ProblemClass problemClass = enumGenerationService.getProblemClass(params.getProblemClass());

        Set<Failure> failures = new HashSet<>();
        Map<SourceDestPair, Set<Failure>> pairFailuresMap = new HashMap<>();
        Map<Node, Set<Failure>> srcFailuresMap = new HashMap<>();
        Map<Node, Set<Failure>> dstFailuresMap = new HashMap<>();

        // Based on ProblemClass and failureSetSize / minMaxFailures input, generate the number of needed failureSet
        // If Flex, use the total number of failureSet
        // Otherwise, use min/max, unless that field isn't set.
        // Create failureSet
        Set<Node> srcDstFailures = selectionService.choosePercentageSubsetNodes(new HashSet<>(sources), params.getPercentSrcFail(), rng);
        srcDstFailures.addAll(selectionService.choosePercentageSubsetNodes(new HashSet<>(destinations), params.getPercentDestFail(), rng));

        Integer failureSetSize = numFailures != null ? numFailures : 0;

        if(minMaxFailures.size() < 2){
            failureSetSize = numFailures;
            failures = generateFailureSet(topo.getNodes(), topo.getLinks(), numFailures, failureClass,
                    params.getFailureProb(), params.getMinMaxFailureProb(), sources, destinations, srcDstFailures, rng);
        }
        else{
            if(problemClass.equals(ProblemClass.Flow)){
                for(SourceDestPair pair : sortedPairs){
                    Integer randomNumFailures = selectionService.randomInt(minMaxFailures.get(0), minMaxFailures.get(1), rng);
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
            else if(problemClass.equals(ProblemClass.Flex) || problemClass.equals(ProblemClass.FlowSharedF) || problemClass.equals(ProblemClass.EndpointSharedF)){
                failureSetSize = selectionService.randomInt(minMaxFailures.get(0), minMaxFailures.get(1), rng);
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
        Integer randomNumFailures = selectionService.randomInt(minMaxFailures.get(0), minMaxFailures.get(1), rng);
        Set<Failure> failureSet = generateFailureSet(topo.getNodes(), topo.getLinks(),
                randomNumFailures, failureClass, params.getFailureProb(), params.getMinMaxFailureProb(),
                sources, destinations, srcDstFailures, rng);
        failuresMap.put(member, failureSet);
        failureSetSize += failureSet.size();
        return failureSetSize;
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
            chosenNodes.addAll(selectionService.chooseRandomSubsetNodes(prioritySet, numFailures, rng));
        }
        // If we still haven't gotten enough failureSet, make some more
        if(chosenNodes.size() < numFailures){
            int numLeftToChoose = numFailures - chosenNodes.size();

            Set<Link> linkOptions = new HashSet<>(links);
            if(failureClass.equals(FailureClass.Link)){
                chosenLinks.addAll(selectionService.chooseRandomSubsetLinks(linkOptions, numLeftToChoose, rng));
            }

            // remove any nodes, if necessary
            nodeOptions.removeAll(sources);
            nodeOptions.removeAll(destinations);

            if(failureClass.equals(FailureClass.Node)){
                chosenNodes.addAll(selectionService.chooseRandomSubsetNodes(nodeOptions, numLeftToChoose, rng));
            }

            if(failureClass.equals(FailureClass.Both)){
                Integer numNodeFailures = rng.nextInt(numLeftToChoose);
                Integer numLinkFailures = numLeftToChoose - numNodeFailures;
                chosenNodes.addAll(selectionService.chooseRandomSubsetNodes(nodeOptions, numNodeFailures, rng));
                chosenLinks.addAll(selectionService.chooseRandomSubsetLinks(linkOptions, numLinkFailures, rng));
            }
        }

        // Determine probabilities
        List<Double> probabilities = generateProbabilities(probability, minMaxFailureProb, chosenNodes.size() + chosenLinks.size(), rng);

        return generateFailuresFromNodeLinks(chosenNodes, chosenLinks, probabilities);
    }

    private List<Double> generateProbabilities(Double probability, List<Double> minMaxFailureProb, Integer numFailures, Random rng) {
        if(probability > 0){
            return DoubleStream.iterate(probability, p -> p).limit(numFailures).boxed().map(p -> Math.min(1.0, p)).collect(Collectors.toList());
        }
        else{
            double minProb = minMaxFailureProb.size() > 1 ? minMaxFailureProb.get(0) : 1.0;
            double maxProb = minMaxFailureProb.size() > 1 ? minMaxFailureProb.get(1) : 1.0;
            return rng.doubles(numFailures, minProb, maxProb + 0.1).boxed().map(p -> Math.min(1.0, p)).collect(Collectors.toList());
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
        // Filter out forward/reverse edges
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

}