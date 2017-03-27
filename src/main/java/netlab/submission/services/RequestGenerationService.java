package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.FailureClass;
import netlab.submission.request.Request;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.topology.elements.*;
import netlab.topology.services.TopologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@Slf4j
public class RequestGenerationService {

    private TopologyService topologyService;

    @Autowired
    public RequestGenerationService(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    public RequestSet generateRequests(SimulationParameters simulationParameters){

        Map<String, Request> requests = createRequestsFromParameters(simulationParameters);
        String status = "Processing";
        if(requests.isEmpty()){
            status = "Submission failed. Could not generate requests.";
        }
        String setId = UUID.randomUUID().toString();
        return RequestSet.builder()
                .requests(requests)
                .status(status)
                .id(setId)
                .seed()
                .algorithm()
                .batchType()
                .failureClass()
                .sdn()
                .useAws()
                .topologyId()
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
        Integer numConnections = params.getNumConnections();
        List<Integer> minMaxConnections = params.getMinMaxConnections();

        // Failure params
        Integer numFailures = params.getNumFailures();
        List<Integer> minMaxFailures = params.getMinMaxFailures();
        FailureClass failureClass = FailureClass.get(params.getFailureClass()).orElse(FailureClass.BOTH);

        for(int i = 0; i < params.getNumRequests(); i++){

            Set<Node> sources = pickSources(topo.getNodes(), params.getNumSources(), rng);
            Set<Node> destinations = pickDestinations(topo.getNodes(), params.getNumDestinations(), rng,
                    params.getSourceDestCanOverlap(), sources);
            Set<SourceDestPair> pairs = createPairs(sources, destinations);

            Set<Failure> failures = null;
            Map<SourceDestPair, Set<Failure>> failuresMap = null;
            Double failureProbability = params.getFailureProb() != -1 ?
                    params.getFailureProb() :
                    rng.doubles(params.getMinMaxFailureProb().get(0), params.getMinMaxFailureProb().get(1) + .01).;
            // If a min and max aren't specified, or they are equal, just create a set of failures
            if(minMaxFailures.size() < 2 || minMaxFailures.get(0).equals(minMaxFailures.get(1)) && numFailures > -1){
                failures = generateFailureSet(topo.getNodes(), topo.getLinks(), sources, destinations,
                        params.getSrcFailuresAllowed(), params.getDstFailuresAllowed(), numFailures, failureClass,
                        failureProbability, rng);
            }
            // Otherwise create a unique set for each connection pair
            else{

            }

            Map<SourceDestPair, Integer> numConnectionsMap;
            Map<SourceDestPair, Integer> numCutsMap;

            Request request = Request.builder()
                    .sources(sources)
                    .destinations(destinations)
                    .numConnections(numConnections)
                    .failures(failures)
                    .numCuts(numCuts)
                    .pairs(pairs)
                    .numConnectionsMap(numConnectionsMap)
                    .numCutsMap(numCutsMap)
                    .failuresMap(failuresMap)
                    .build();
            requests.add(request);
        }
        return requests;
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
        boolean srcDestOverlap = params.getSourceDestCanOverlap();
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
        boolean enoughForNonOverlapSrcDest = numNodes >= numSources + numDestinations && !srcDestOverlap
                && failureClass.equals("Node");
        boolean enoughForNonOverlapSrcDestFailsSrcDestOverlap = !srcFailures && !dstFailures && srcDestOverlap
                && numNodes >= Math.max(numSources, numDestinations) && failureClass.equals("Node");
        boolean enoughForNonOverlapSrcDestFails = !srcFailures && !dstFailures
                && numNodes >= numFailures + numSources + numDestinations && failureClass.equals("Node");
        boolean enoughForNonOverlapSrcFails = !srcFailures && numNodes >= numFailures + numSources
                && failureClass.equals("Node");
        boolean enoughForNonOverlapDstFails = !dstFailures && numNodes >= numFailures + numDestinations
                && failureClass.equals("Node");
        boolean enoughForNonOverlapSrcDestBoth = numNodes + numLinks >= numSources + numDestinations && !srcDestOverlap
                && failureClass.equals("Both");
        boolean enoughForNonOverlapSrcDestFailsSrcDestOverlapBoth = !srcFailures && !dstFailures && srcDestOverlap
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
        List<Node> options = new ArrayList<>(nodes);
        return chooseRandomSubsetNodes(options, numSources, rng);
    }

    private Set<Node> pickDestinations(Set<Node> nodes, Integer numDestinations, Random rng, Boolean sourceDestOverlap,
                                       Set<Node> sources) {
        Set<Node> remainingNodes = new HashSet<>(nodes);
        if(!sourceDestOverlap){
            remainingNodes.removeAll(sources);
        }
        List<Node> options = new ArrayList<>(remainingNodes);
        return chooseRandomSubsetNodes(options, numDestinations, rng);
    }

    private Set<Failure> generateFailureSet(Set<Node> nodes, Set<Link> links, Set<Node> sources, Set<Node> destinations,
                                            Boolean srcFailuresAllowed, Boolean dstFailuresAllowed, Integer numFailures,
                                            FailureClass failureClass, Double probability, Random rng) {

        Set<Link> chosenLinks = new HashSet<>();
        Set<Node> chosenNodes = new HashSet<>();

        List<Link> linkOptions = new ArrayList<>(links);
        if(failureClass.equals(FailureClass.LINK)){
            chosenLinks = chooseRandomSubsetLinks(linkOptions, numFailures, rng);
        }
        // remove any nodes, if necessary
        Set<Node> remainingNodes = new HashSet<>(nodes);
        if(!srcFailuresAllowed){
            remainingNodes.removeAll(sources);
        }
        if(!dstFailuresAllowed){
            remainingNodes.removeAll(destinations);
        }
        List<Node> nodeOptions = new ArrayList<>(remainingNodes);

        if(failureClass.equals(FailureClass.NODE)){
            chosenNodes = chooseRandomSubsetNodes(nodeOptions, numFailures, rng);
        }
        if(failureClass.equals(FailureClass.BOTH)){
            Integer numNodeFailures = rng.nextInt(numFailures);
            Integer numLinkFailures = numFailures - numNodeFailures;
            chosenNodes = chooseRandomSubsetNodes(nodeOptions, numNodeFailures, rng);
            chosenLinks = chooseRandomSubsetLinks(linkOptions, numLinkFailures, rng);
        }
        return generateFailuresFromNodeLinks(chosenNodes, chosenLinks);
    }

    private Set<Failure> generateFailuresFromNodeLinks(Set<Node> nodes, Set<Link> links){
        Set<Failure> failures = new HashSet<>();
        for(Node node : nodes){
            failures.add(Failure.builder().node(node).link(null).probability().build());
        }
        for(Link link : links){
            failures.add(Failure.builder().node(null).link(link).probability().build());
        }
        return failures;
    }

    private Set<Node> chooseRandomSubsetNodes(List<Node> options, Integer numChoices, Random rng) {
        if(numChoices == 0){
            return new HashSet<>();
        }
        Collections.shuffle(options, rng);
        return new HashSet<>(options.subList(0, numChoices));
    }

    private Set<Link> chooseRandomSubsetLinks(List<Link> options, Integer numChoices, Random rng){
        if(numChoices == 0){
            return new HashSet<>();
        }
        Collections.shuffle(options, rng);
        return new HashSet<>(options.subList(0, numChoices));
    }
}
