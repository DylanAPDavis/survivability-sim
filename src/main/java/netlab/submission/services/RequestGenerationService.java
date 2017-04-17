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
public class RequestGenerationService {

    private TopologyService topologyService;

    @Autowired
    public RequestGenerationService(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    public RequestSet generateRequests(SimulationParameters params){

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

    private Map<String, Request> createRequestsFromParameters(SimulationParameters params) {
        Map<String, Request> requests = new HashMap<>();
        Topology topo = topologyService.getTopologyById(params.getTopologyId());
        if(topo == null || !checkValidParams(params, topo)){
            return requests;
        }

        Random rng = new Random(params.getSeed());

        // Connection params
        Integer numConnections = params.getNumConnections() >= 0 ? params.getNumConnections() : null;
        List<List<Integer>> minMaxConnections = params.getMinMaxConnections();

        // Cut params
        Integer numCuts = params.getNumCuts() >= 0 ? params.getNumCuts() : null;
        List<Integer> minMaxCuts = params.getMinMaxCuts();

        // Failure params
        Integer numFailures = params.getNumFailures();
        List<Integer> minMaxFailures = params.getMinMaxFailures();
        FailureClass failureClass = getFailureClass(params.getFailureClass());

        for(int i = 0; i < params.getNumRequests(); i++){

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
            Map<SourceDestPair, Integer> numCutsMap = null;
            if(minMaxCuts.size() == 2){
                Integer minCuts = minMaxCuts.get(0);
                Integer maxCuts = minMaxCuts.get(1);
                numCutsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> randomInt(minCuts, maxCuts, rng)));
            }

            // Determine number of connections
            Map<SourceDestPair, Integer> minConnectionsMap = null;
            Map<SourceDestPair, Integer> maxConnectionsMap = null;
            if(minMaxConnections.size() == 2){
                // Get the minimum/maximum for generating mins (index 0) and maxes (index 1)
                List<Integer> minMaxForMin = minMaxConnections.get(0);
                List<Integer> minMaxForMax = minMaxConnections.get(1);
                Integer minMin = minMaxForMin.get(0);
                Integer maxMin = minMaxForMin.get(1);
                Integer minMax = minMaxForMax.get(0);
                Integer maxMax = minMaxForMax.get(1);
                minConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> randomInt(minMin, maxMin, rng)));
                maxConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> randomInt(minMax, maxMax, rng)));
            }

            Request request = Request.builder()
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
            requests.put(request.getId(), request);
        }
        return requests;
    }



    private Integer randomInt(Integer min, Integer max, Random rng){
        return rng.nextInt((max - min) + 1) + min;
    }

    private Set<SourceDestPair> createPairs(Set<Node> sources, Set<Node> destinations) {
        Set<SourceDestPair> pairs = new HashSet<>();
        for(Node source : sources){
            for(Node dest: destinations){
                if(!source.equals(dest)){
                    pairs.add(SourceDestPair.builder().src(source).dst(dest).build());
                }
            }
        }
        return pairs;
    }

    private boolean checkValidParams(SimulationParameters params, Topology topo) {
        Integer numNodes = topo.getNodes().size();
        Integer numLinks = topo.getLinks().size();
        Integer numSources = params.getNumSources();
        Integer numDestinations = params.getNumDestinations();
        Integer numFailures = params.getMinMaxFailures().size() == 2 ?
                params.getMinMaxFailures().get(1) : params.getNumFailures();
        OverlapType srcDestOverlap = OverlapType.get(params.getSrcDstOverlap()).orElse(OverlapType.None);
        boolean srcFailures = params.getSrcFailuresAllowed();
        boolean dstFailures = params.getDstFailuresAllowed();
        String failureClass = params.getFailureClass();

        // Check if there are enough nodes for:
        // sources, destinations, failures, sources + destinations (when they can't overlap),
        // failures + sources + destinations (when no overlap)
        // failures + sources (failures and destinations can overlap)
        // failures + destinations (failures and sources can overlap)
        // failures + max(sources, destinations) (sources and destinations can overlap, but failures can't with either)
        boolean enoughForSources = numNodes >= numSources;
        boolean enoughForDestinations = numNodes >= numDestinations;
        boolean enoughForFailures = numNodes >= numFailures && failureClass.equals("Node")
                || numLinks >= numFailures && failureClass.equals("Link")
                || numLinks + numNodes >= numFailures && failureClass.equals("Both");
        boolean enoughForNonOverlapSrcDest = numNodes >= numSources + numDestinations && srcDestOverlap.equals(OverlapType.None)
                && failureClass.equals("Node");
        boolean enoughForNonOverlapSrcDestFailsSrcDestOverlap = !srcFailures && !dstFailures && !srcDestOverlap.equals(OverlapType.None)
                && numNodes >= Math.max(numSources, numDestinations) && failureClass.equals("Node");
        boolean enoughForNonOverlapSrcDestFails = !srcFailures && !dstFailures
                && numNodes >= numFailures + numSources + numDestinations && failureClass.equals("Node");
        boolean enoughForNonOverlapSrcFails = !srcFailures && numNodes >= numFailures + numSources
                && failureClass.equals("Node");
        boolean enoughForNonOverlapDstFails = !dstFailures && numNodes >= numFailures + numDestinations
                && failureClass.equals("Node");
        boolean enoughForNonOverlapSrcDestBoth = numNodes + numLinks >= numSources + numDestinations && srcDestOverlap.equals(OverlapType.None)
                && failureClass.equals("Both");
        boolean enoughForNonOverlapSrcDestFailsSrcDestOverlapBoth = !srcFailures && !dstFailures && !srcDestOverlap.equals(OverlapType.None)
                && numNodes + numLinks >= Math.max(numSources, numDestinations) && failureClass.equals("Both");
        boolean enoughForNonOverlapSrcDestFailsBoth = !srcFailures && !dstFailures
                && numNodes + numLinks >= numFailures + numSources + numDestinations && failureClass.equals("Both");
        boolean enoughForNonOverlapSrcFailsBoth = !srcFailures && numNodes + numLinks >= numFailures + numSources
                && failureClass.equals("Both");
        boolean enoughForNonOverlapDstFailsBoth = !dstFailures && numNodes + numLinks >= numFailures + numDestinations
                && failureClass.equals("Both");


        return enoughForSources && enoughForDestinations && enoughForFailures && enoughForNonOverlapSrcDest
                && enoughForNonOverlapSrcDestFails && enoughForNonOverlapSrcDestFailsSrcDestOverlap
                && enoughForNonOverlapSrcFails && enoughForNonOverlapDstFails && enoughForNonOverlapSrcDestBoth
                && enoughForNonOverlapSrcDestFailsSrcDestOverlapBoth && enoughForNonOverlapSrcDestFailsBoth
                && enoughForNonOverlapSrcFailsBoth && enoughForNonOverlapDstFailsBoth;

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
