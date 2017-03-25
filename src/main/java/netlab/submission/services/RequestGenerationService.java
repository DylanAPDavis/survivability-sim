package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.Request;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


@Service
@Slf4j
public class RequestGenerationService {

    public RequestSet generateRequests(SimulationParameters simulationParameters){

        Set<Request> requests = createSetFromParameters(simulationParameters);
        return RequestSet.builder()
                .requests()
                .id()
                .seed()
                .algorithm()
                .batchType()
                .sdn()
                .useAws()
                .topologyId()
                .build();
    }

    private Set<Request> createSetFromParameters(SimulationParameters params) {
        Random rng = new Random(params.getSeed());
        Set<Request> requests = new HashSet<>();
        for(int i = 0; i < params.getNumRequests(); i++){

            Set<Node> sources = new HashSet<>();
            Set<Node> destinations = new HashSet<>();

            Integer numConnections;
            Set<Failure> failures;
            Integer numCuts;

            Set<SourceDestPair> pairs;
            Map<SourceDestPair, Integer> numConnectionsMap;
            Map<SourceDestPair, Integer> numCutsMap;
            Map<SourceDestPair, Set<Failure>> failuresMap;

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
}
