package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.ampl.AmplService;
import netlab.processing.disjointpaths.BhandariService;
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

    private ShortestPathService shortestPathService;

    private SteinerTreeService steinerTreeService;

    private BhandariService bhandariService;

    private OverlappingTreeService overlappingTreeService;

    private PCycleService pCycleService;

    private TopologyService topoService;

    @Autowired
    public ProcessingService(TopologyService topologyService, AmplService amplService, FlexBhandariService flexBhandariService,
                             ShortestPathService shortestPathService, SteinerTreeService steinerTreeService,
                             BhandariService bhandariService, OverlappingTreeService overlappingTreeService,
                             PCycleService pCycleService) {
        this.topoService = topologyService;
        this.amplService = amplService;
        this.flexBhandariService = flexBhandariService;
        this.shortestPathService = shortestPathService;
        this.steinerTreeService = steinerTreeService;
        this.bhandariService = bhandariService;
        this.overlappingTreeService = overlappingTreeService;
        this.pCycleService = pCycleService;
    }

    public Request processRequest(Request request) {
        Topology topo = topoService.getTopologyById(request.getTopologyId());
        Details details = request.getDetails();
        switch(request.getAlgorithm()){
            case ILP:
                details = amplService.solve(request, topo);
                break;
            case FlexBhandari:
                details = flexBhandariService.solve(request, topo);
                break;
            case ShortestPath:
                details = shortestPathService.solve(request, topo);
                break;
            case MinimumSteinerTree:
                details = steinerTreeService.solve(request, topo);
                break;
            case Bhandari:
                details = bhandariService.solve(request, topo);
                break;
            case OverlappingTrees:
                details = overlappingTreeService.solve(request, topo);
                break;
            case Pcycles:
                details = pCycleService.solve(request, topo);
                break;
        }
        request.setDetails(details);
        return request;
    }

}

