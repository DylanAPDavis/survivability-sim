package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.FailureScenario;
import netlab.submission.enums.MemberFailureType;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.Failures;
import netlab.submission.request.NumFailureEvents;
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
        List<List<Failure>> failureGroups = generateFailureGroups(numFailsAllowed, failures);

        // Failure for pairs
        Map<SourceDestPair, Set<Failure>> pairFailuresMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> new HashSet<>()));
        //makePairFailuresMap(pairs, params.getPairFailureMap(), params.getPairFailureProbabilityMap(), nodeIdMap, linkIdMap);
        Map<SourceDestPair, List<List<Failure>>> pairFailureGroupsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> new ArrayList<>()));
        //pairFailuresMap.keySet().stream().collect(Collectors.toMap(p -> p, p -> generateFailureGroups(pairNumFailsAllowed.get(p), pairFailuresMap.get(p))));

        // Failures for sources
        Map<Node, Set<Failure>> srcFailuresMap = sources.stream().collect(Collectors.toMap(s -> s, s -> new HashSet<>()));
        //makeNodeFailuresMap(sources, params.getSourceFailureMap(), params.getSourceFailureProbabilityMap(), nodeIdMap, linkIdMap);
        Map<Node, List<List<Failure>>> srcFailureGroupsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> new ArrayList<>()));
        //srcFailuresMap.keySet().stream().collect(Collectors.toMap(p -> p, p -> generateFailureGroups(srcNumFailsAllowed.get(p), srcFailuresMap.get(p))));

        // Failures for destinations
        Map<Node, Set<Failure>> dstFailuresMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> new HashSet<>()));
        //makeNodeFailuresMap(destinations, params.getDestFailureMap(), params.getDestFailureProbabilityMap(), nodeIdMap, linkIdMap);
        Map<Node, List<List<Failure>>> dstFailureGroupsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> new ArrayList<>()));
        //dstFailuresMap.keySet().stream().collect(Collectors.toMap(p -> p, p -> generateFailureGroups(dstNumFailsAllowed.get(p), dstFailuresMap.get(p))));

        //long failureSetSize = failures.size() + pairFailuresMap.values().stream().mapToLong(Collection::size).sum();
        //failureSetSize += srcFailuresMap.values().stream().mapToLong(Collection::size).sum();
        //failureSetSize += dstFailureGroupsMap.values().stream().mapToLong(Collection::size).sum();
        return Failures.builder()
                .failureSet(failures)
                .failureSetSize(failures.size())
                .failureGroups(failureGroups)
                .pairFailuresMap(pairFailuresMap)
                .pairFailureGroupsMap(pairFailureGroupsMap)
                .srcFailuresMap(srcFailuresMap)
                .srcFailureGroupsMap(srcFailureGroupsMap)
                .dstFailuresMap(dstFailuresMap)
                .dstFailureGroupsMap(dstFailureGroupsMap)
                .build();
    }

    private Set<Link> filterLinks(Set<Link> links, Map<String, Link> linkIdMap){
        Set<Link> filteredLinks = new HashSet<>();
        for(Link link : links){
            String[] splitId = link.getId().split("-");
            String revId;
            if(splitId.length > 2){
                revId = splitId[1] + "-" + splitId[0] + "-" + splitId[2];
            }
            else{
                revId = splitId[1] + "-" + splitId[0];
            }
            if(linkIdMap.containsKey(revId)){
                Link revLink = linkIdMap.get(revId);
                if(!filteredLinks.contains(revLink)){
                    filteredLinks.add(link);
                }
            }
            else{
                filteredLinks.add(link);
            }
        }
        return filteredLinks;
    }


    private Map<Node,Set<Failure>> makeNodeFailuresMap(Set<Node> members, Map<String, Set<String>> memberFailureMap,
                                                       Map<String, Map<String, Double>> memberFailureProbabilityMap, Map<String, Node> nodeIdMap, Map<String, Link> linkIdMap) {
        Map<Node, Set<Failure>> failureMap = members.stream().collect(Collectors.toMap(p -> p, p -> new HashSet<>()));
        for(String memberString : memberFailureMap.keySet()){
            Node member = topologyService.getNodeById(memberString);
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
                    .src(topologyService.getNodeById(pairList.get(0)))
                    .dst(topologyService.getNodeById(pairList.get(1)))
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

    public NumFailureEvents makeNumFailureEventsFromRequestParams(RequestParameters params, Set<SourceDestPair> pairs,
                                                                  Set<Node> sources, Set<Node> destinations) {
        // Map for pairs
        Map<SourceDestPair, Integer> pairNumFailsMap = selectionService.makePairIntegerMap(pairs, new HashMap<>(), 0);

        // Map for sources
        Map<Node, Integer> srcNumFailsMap = selectionService.makeNodeIntegerMap(sources, new HashMap<>(), 0);

        // Map for destinations
        Map<Node, Integer> dstNumFailsMap = selectionService.makeNodeIntegerMap(destinations, new HashMap<>(), 0);

        return NumFailureEvents.builder()
                .totalNumFailureEvents(params.getNumFailureEvents())
                .pairNumFailureEvents(pairNumFailsMap)
                .srcNumFailureEvents(srcNumFailsMap)
                .dstNumFailureEvents(dstNumFailsMap)
                .build();
    }

    public NumFailureEvents assignNumFails(SimulationParameters params, List<SourceDestPair> pairs, List<Node> sources,
                                           List<Node> destinations, Failures failureCollection, Random rng) {
        ProblemClass problemClass = enumGenerationService.getProblemClass(params.getProblemClass());

        // Cut params
        Integer numFails = params.getNumFailureEvents();
        List<List<Failure>> failureGroups = new ArrayList<>();

        Map<SourceDestPair, Integer> pairNumFailsMap = new HashMap<>();
        Map<SourceDestPair, List<List<Failure>>> pairFailureGroupsMap = new HashMap<>();

        Map<Node, Integer> srcNumFailsMap = new HashMap<>();
        Map<Node, List<List<Failure>>> srcFailureGroupsMap = new HashMap<>();
        Map<Node, Integer> dstNumFailsMap = new HashMap<>();
        Map<Node, List<List<Failure>>> dstFailureGroupsMap = new HashMap<>();


        failureGroups = generateFailureGroups(numFails, failureCollection.getFailureSet());

        // Assign random number of cuts between min and max
        // Except: cap out at the number of failureSet for a pair, so you're not trying to cut more than than the
        // size of the failure set
        /*if(problemClass.equals(ProblemClass.Flex) || problemClass.equals(ProblemClass.FlowSharedF)
                || problemClass.equals(ProblemClass.EndpointSharedF) || problemClass.equals(ProblemClass.Combined)){
            if(minMaxFails.size() == 2) {
                numFails = Math.min(failureCollection.getFailureSet().size(), selectionService.randomInt(minMaxFails.get(0), minMaxFails.get(1), rng));
            }
            failureGroups = generateFailureGroups(numFails, failureCollection.getFailureSet());
        }
        if(problemClass.equals(ProblemClass.Flow)){
            for (SourceDestPair pair : pairs) {
                Set<Failure> thisFailureSet = failureCollection.getPairFailuresMap().getOrDefault(pair, failureCollection.getFailureSet());
                int thisNumFails = minMaxFails.size() == 2 ?
                        Math.min(thisFailureSet.size(), selectionService.randomInt(minMaxFails.get(0), minMaxFails.get(1), rng)) : numFails;
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
        }*/

        failureCollection.setFailureGroups(failureGroups);
        failureCollection.setPairFailureGroupsMap(pairFailureGroupsMap);
        failureCollection.setSrcFailureGroupsMap(srcFailureGroupsMap);
        failureCollection.setDstFailureGroupsMap(dstFailureGroupsMap);

        return NumFailureEvents.builder()
                .totalNumFailureEvents(numFails)
                .pairNumFailureEvents(pairNumFailsMap)
                .srcNumFailureEvents(srcNumFailsMap)
                .dstNumFailureEvents(dstNumFailsMap)
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

        // Changed to just empty lists for now, may revert for later implementation
        List<Integer> minMaxFailures = new ArrayList<>();
        List<Double> minMaxFailureProbs = new ArrayList<>();

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
        MemberFailureType sourceFailureType = enumGenerationService.getMemberFailureType(params.getSourceFailureType());
        Double sourceFailPercentage = percentageFromFailureType(sourceFailureType);
        MemberFailureType destFailureType = enumGenerationService.getMemberFailureType(params.getDestFailureType());
        Double destFailPercentage = percentageFromFailureType(destFailureType);
        Set<Node> srcDstFailures = selectionService.choosePercentageSubsetNodes(new HashSet<>(sources), sourceFailPercentage, rng);
        srcDstFailures.addAll(selectionService.choosePercentageSubsetNodes(new HashSet<>(destinations), destFailPercentage, rng));

        Integer failureSetSize = numFailures != null ? numFailures : 0;

        // Filter out reverse links - prevents user from selecting both (o,t) and (t,o) to fail, when both will
        // fail implicitly
        Set<Link> filteredLinks = filterLinks(topo.getLinks(), topo.getLinkIdMap());

        if(minMaxFailures.size() < 2){
            failureSetSize = numFailures;
            failures = generateFailureSet(topo.getNodes(), filteredLinks, numFailures, failureClass,
                    params.getFailureProb(), params.getFailureScenario(), sources, destinations, srcDstFailures,
                    sourceFailureType, destFailureType, topo, rng);
        }
        else{
            if(problemClass.equals(ProblemClass.Flow)){
                for(SourceDestPair pair : sortedPairs){
                    Integer randomNumFailures = selectionService.randomInt(minMaxFailures.get(0), minMaxFailures.get(1), rng);
                    Set<Failure> failureSet = generateFailureSet(topo.getNodes(), filteredLinks,
                            randomNumFailures, failureClass, params.getFailureProb(), params.getFailureScenario(),
                            sources, destinations, srcDstFailures, sourceFailureType, destFailureType, topo, rng);
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
            else if(problemClass.equals(ProblemClass.Flex) || problemClass.equals(ProblemClass.FlowSharedF)
                    || problemClass.equals(ProblemClass.EndpointSharedF) || problemClass.equals(ProblemClass.Combined)){
                failureSetSize = selectionService.randomInt(minMaxFailures.get(0), minMaxFailures.get(1), rng);
                failures = generateFailureSet(topo.getNodes(), filteredLinks,
                        failureSetSize, failureClass, params.getFailureProb(), params.getFailureScenario(),
                        sources, destinations, srcDstFailures, sourceFailureType, destFailureType, topo, rng);
            }
        }

        return Failures.builder()
                .failureSetSize(failures.size())
                .failureSet(failures)
                .pairFailuresMap(pairFailuresMap)
                .srcFailuresMap(srcFailuresMap)
                .dstFailuresMap(dstFailuresMap)
                .build();
    }

    private Double percentageFromFailureType(MemberFailureType memberFailureType){
        switch(memberFailureType){
            case Allow:
            case Prevent:
                return  0.0;
            case Enforce:
                return 1.0;
            default:
                return 0.0;
        }
    }

    private Integer populateFailureMap(List<Integer> minMaxFailures, Topology topo, Random rng, FailureClass failureClass,
                                       SimulationParameters params, List<Node> sources, List<Node> destinations,
                                       Set<Node> srcDstFailures, Map<Node, Set<Failure>> failuresMap, Integer failureSetSize,
                                       Node member){
        Integer randomNumFailures = selectionService.randomInt(minMaxFailures.get(0), minMaxFailures.get(1), rng);
        // Filter out reverse links - prevents user from selecting both (o,t) and (t,o) to fail, when both will
        // fail implicitly
        Set<Link> filteredLinks = filterLinks(topo.getLinks(), topo.getLinkIdMap());
        Set<Failure> failureSet = generateFailureSet(topo.getNodes(), filteredLinks,
                randomNumFailures, failureClass, params.getFailureProb(), params.getFailureScenario(),
                sources, destinations, srcDstFailures, MemberFailureType.Allow, MemberFailureType.Allow, topo, rng);
        failuresMap.put(member, failureSet);
        failureSetSize += failureSet.size();
        return failureSetSize;
    }

    private Set<Failure> generateFailureSet(Set<Node> nodes, Set<Link> links, Integer numFailures, FailureClass failureClass,
                                            Double probability, String failScenario, List<Node> sources, List<Node> destinations,
                                            Set<Node> prioritySet, MemberFailureType sourceFailureType, MemberFailureType destFailureType, Topology topo, Random rng) {

        List<Link> chosenLinks = new ArrayList<>();
        List<Node> chosenNodes = new ArrayList<>();
        List<Double> probabilities = new ArrayList<>();

        Set<Node> nodeOptions = new HashSet<>(nodes);

        // If you're preventing sources or destinations from failing, remove them from the node options
        if(sourceFailureType.equals(MemberFailureType.Prevent)){
            nodeOptions.removeAll(sources);
        }
        if(destFailureType.equals(MemberFailureType.Prevent)){
            nodeOptions.removeAll(destinations);
        }

        FailureScenario failureScenario = enumGenerationService.getFailureScenario(failScenario);

        if(numFailures == 0 && failureScenario.equals(FailureScenario.Default)){
            return new HashSet<>();
        }

        if(failureScenario.equals(FailureScenario.Default)) {
            if (!prioritySet.isEmpty()) {
                chosenNodes.addAll(selectionService.chooseRandomSubsetNodes(prioritySet, numFailures, rng));

                nodeOptions.removeAll(sources);
                nodeOptions.removeAll(destinations);
            }
            // If we still haven't gotten enough failureSet, make some more
            if (chosenNodes.size() < numFailures) {
                int numLeftToChoose = numFailures - chosenNodes.size();

                Set<Link> linkOptions = new HashSet<>(links);
                if (failureClass.equals(FailureClass.Link)) {
                    chosenLinks.addAll(selectionService.chooseRandomSubsetLinks(linkOptions, numLeftToChoose, rng));
                }


                if (failureClass.equals(FailureClass.Node)) {
                    chosenNodes.addAll(selectionService.chooseRandomSubsetNodes(nodeOptions, numLeftToChoose, rng));
                }

                if (failureClass.equals(FailureClass.Both)) {
                    Set<String> idOptions = nodeOptions.stream().map(Node::getId).collect(Collectors.toSet());
                    idOptions.addAll(linkOptions.stream().map(Link::getId).collect(Collectors.toSet()));
                    Set<String> chosenIds = selectionService.choosenRandomSubsetStrings(idOptions, numLeftToChoose, rng);
                    Map<String, Node> nodeIdMap = topo.getNodeIdMap();
                    Map<String, Link> linkIdMap = topo.getLinkIdMap();
                    for (String chosenId : chosenIds) {
                        if (nodeIdMap.containsKey(chosenId)) {
                            chosenNodes.add(nodeIdMap.get(chosenId));
                        }
                        if (linkIdMap.containsKey(chosenId)) {
                            chosenLinks.add(linkIdMap.get(chosenId));
                        }
                    }
                }
            }

            // Determine probabilities
            probabilities = generateProbabilities(probability, new ArrayList<>(), chosenNodes.size() + chosenLinks.size(), rng);
        }
        else{
            switch(failureScenario){
                case AllLinks:
                    chosenLinks = new ArrayList<>(links);
                    probabilities = generateProbabilities(probability, new ArrayList<>(), chosenNodes.size() + chosenLinks.size(), rng);
                    break;
                case AllNodes:
                    chosenNodes = new ArrayList<>(nodeOptions);
                    probabilities = generateProbabilities(probability, new ArrayList<>(), chosenNodes.size() + chosenLinks.size(), rng);
                    break;
                case Network:
                    chosenLinks = new ArrayList<>(links);
                    chosenNodes = new ArrayList<>(nodeOptions);
                    probabilities = generateProbabilities(probability, new ArrayList<>(), chosenNodes.size() + chosenLinks.size(), rng);
                    break;
                case Earthquake:
                    break;
                case Hurricane:
                    break;
            }
        }


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

    public List<List<Failure>> generateFailureGroups(Integer k, Set<Failure> failureSet){
        List<List<Failure>> failureGroups = new ArrayList<>();
        if(k == 0){
            failureGroups.add(new ArrayList<>());
            return failureGroups;
        }
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
