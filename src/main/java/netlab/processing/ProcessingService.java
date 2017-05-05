package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.ampl.AmplService;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.Request;
import netlab.submission.request.RequestSet;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ProcessingService {


    private AmplService amplService;

    private TopologyService topoService;

    @Autowired
    public ProcessingService(TopologyService topologyService, AmplService amplService) {
        this.topoService = topologyService;
        this.amplService = amplService;
    }

    public RequestSet processRequestSet(RequestSet requestSet) {

        Topology topo = topoService.getTopologyById(requestSet.getTopologyId());
        for(Request request : requestSet.getRequests().values()){
            Map<SourceDestPair, Map<String, Path>> paths =  processRequest(request, requestSet.getAlgorithm(), requestSet.getProblemClass(),
                    topo, requestSet.isUseAws(), requestSet.isSdn());
            request.setChosenPaths(paths);
        }

        return requestSet;
    }

    public Map<SourceDestPair, Map<String, Path>> processRequest(Request request, Algorithm algorithm, ProblemClass problemClass,
                                                                 Topology topology, Boolean useAws, Boolean sdn){
        Map<SourceDestPair, Map<String, Path>> paths = new HashMap<>();

        switch(algorithm){
            case ServiceILP:
                paths = amplService.solve(request, problemClass, topology);
        }
        return paths;
    }
}
