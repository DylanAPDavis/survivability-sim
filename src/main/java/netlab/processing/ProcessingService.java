package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.ampl.AmplService;
import netlab.processing.disjointpaths.PartialBhandariService;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;
import netlab.submission.enums.ProcessingType;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Request processRequest(Request request) {
        Topology topo = topoService.getTopologyById(request.getTopologyId());
        Details details = request.getDetails();
        switch(request.getAlgorithm()){
            case ServiceILP:
                details = amplService.solve(details, request.getProblemClass(), request.getObjective(),
                        topo, request.getId(), request.getNumThreads());
            case PartialBhandari:
                details = partialBhandariService.solve(details, request.getProblemClass(), request.getObjective(),
                        topo, request.getId());
        }
        request.setDetails(details);
        return request;
    }

}
