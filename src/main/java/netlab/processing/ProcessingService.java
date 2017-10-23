package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.ampl.AmplService;
import netlab.processing.disjointpaths.FlexBhandariService;
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

    private FlexBhandariService flexBhandariService;

    private TopologyService topoService;

    @Autowired
    public ProcessingService(TopologyService topologyService, AmplService amplService, FlexBhandariService flexBhandariService) {
        this.topoService = topologyService;
        this.amplService = amplService;
        this.flexBhandariService = flexBhandariService;
    }

    public Request processRequest(Request request) {
        Topology topo = topoService.getTopologyById(request.getTopologyId());
        Details details = request.getDetails();
        switch(request.getAlgorithm()){
            case ServiceILP:
                details = amplService.solve(request, topo);
            case FlexBhandari:
                details = flexBhandariService.solve(request, topo);
        }
        request.setDetails(details);
        return request;
    }

}
