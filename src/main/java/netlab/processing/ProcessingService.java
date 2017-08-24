package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.ampl.AmplService;
import netlab.processing.disjointpaths.PartialBhandariService;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;
import netlab.submission.enums.ProcessingType;
import netlab.submission.request.Request;
import netlab.submission.request.RequestSet;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProcessingService {


    private AmplService amplService;

    private PartialBhandariService partialBhandariService;

    private TopologyService topoService;

    @Autowired
    public ProcessingService(TopologyService topologyService, AmplService amplService, PartialBhandariService partialBhandariService) {
        this.topoService = topologyService;
        this.amplService = amplService;
        this.partialBhandariService = partialBhandariService;
    }

    public RequestSet processRequestSet(RequestSet requestSet) {
        Topology topo = topoService.getTopologyById(requestSet.getTopologyId());
        if(requestSet.getProcessingType().equals(ProcessingType.Solo)){
           for(Request r : requestSet.getRequests().values()){
               r = processRequest(r, requestSet.getAlgorithm(), requestSet.getProblemClass(), requestSet.getObjective(),
                       topo, requestSet.getId(), requestSet.isUseAws(), requestSet.isSdn(), requestSet.getNumThreads());
               requestSet.getRequests().put(r.getId(), r);
           }
        }

        return requestSet;
    }

    private Request processRequest(Request request, Algorithm algorithm, ProblemClass problemClass, Objective objective,
                                Topology topology, String requestSetId, Boolean useAws, Boolean sdn, Integer numThreads){
        switch(algorithm){
            case ServiceILP:
                return amplService.solve(request, problemClass, objective, topology, requestSetId, numThreads);
            case PartialBhandari:
                return partialBhandariService.solve(request, problemClass, objective, topology, requestSetId);
        }
        return request;
    }
}
