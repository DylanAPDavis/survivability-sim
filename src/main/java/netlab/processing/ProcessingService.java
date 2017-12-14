package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.ampl.AmplService;
import netlab.processing.cycles.HamiltonianCycleService;
import netlab.processing.disjointpaths.BhandariService;
import netlab.processing.disjointpaths.FlexBhandariService;
import netlab.processing.overlappingtrees.OverlappingTreeService;
import netlab.processing.shortestPaths.ShortestPathService;
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

    private BhandariService bhandariService;

    private OverlappingTreeService overlappingTreeService;

    private HamiltonianCycleService hamiltonianCycleService;

    private TopologyService topoService;

    @Autowired
    public ProcessingService(TopologyService topologyService, AmplService amplService, FlexBhandariService flexBhandariService,
                             ShortestPathService shortestPathService, BhandariService bhandariService, OverlappingTreeService overlappingTreeService,
                             HamiltonianCycleService hamiltonianCycleService) {
        this.topoService = topologyService;
        this.amplService = amplService;
        this.flexBhandariService = flexBhandariService;
        this.shortestPathService = shortestPathService;
        this.bhandariService = bhandariService;
        this.overlappingTreeService = overlappingTreeService;
        this.hamiltonianCycleService = hamiltonianCycleService;
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
            case Bhandari:
                details = bhandariService.solve(request, topo);
                break;
            case OverlappingTrees:
                details = overlappingTreeService.solve(request, topo);
                break;
            case Hamlitonian:
                details = hamiltonianCycleService.solve(request, topo);
                break;
        }
        request.setDetails(details);
        return request;
    }

}

