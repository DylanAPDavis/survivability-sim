package netlab.processing;

import lombok.extern.slf4j.Slf4j;
import netlab.processing.ampl.AmplService;
import netlab.processing.cycles.CollapsedRingService;
import netlab.processing.groupcast.CycleForTwoService;
import netlab.processing.cycles.HamiltonianCycleService;
import netlab.processing.disjointpaths.BhandariService;
import netlab.processing.disjointpaths.FlexBhandariService;
import netlab.processing.groupcast.MemberForwardingService;
import netlab.processing.overlappingtrees.OverlappingTreeService;
import netlab.processing.shortestPaths.MinimumCostPathService;
import netlab.processing.shortestPaths.MinimumRiskPathService;
import netlab.processing.shortestPaths.YensService;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.Topology;
import netlab.topology.services.TopologyService;
import netlab.visualization.PrintingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProcessingService {


    private AmplService amplService;

    private FlexBhandariService flexBhandariService;

    private MinimumCostPathService minimumCostPathService;

    private BhandariService bhandariService;

    private OverlappingTreeService overlappingTreeService;

    private HamiltonianCycleService hamiltonianCycleService;

    private MemberForwardingService memberForwardingService;

    private CollapsedRingService collapsedRingService;

    private CycleForTwoService cycleForTwoService;

    private MinimumRiskPathService minimumRiskPathService;

    private YensService yensService;

    private TopologyService topoService;

    private PrintingService printingService;

    @Autowired
    public ProcessingService(TopologyService topologyService, PrintingService printingService,
                             AmplService amplService, FlexBhandariService flexBhandariService,
                             MinimumCostPathService minimumCostPathService, BhandariService bhandariService,
                             OverlappingTreeService overlappingTreeService, HamiltonianCycleService hamiltonianCycleService,
                             MemberForwardingService memberForwardingService,
                             CollapsedRingService collapsedRingService, CycleForTwoService cycleForTwoService,
                             MinimumRiskPathService minimumRiskPathService, YensService yensService) {
        this.topoService = topologyService;
        this.printingService = printingService;
        this.amplService = amplService;
        this.flexBhandariService = flexBhandariService;
        this.minimumCostPathService = minimumCostPathService;
        this.bhandariService = bhandariService;
        this.overlappingTreeService = overlappingTreeService;
        this.hamiltonianCycleService = hamiltonianCycleService;
        this.memberForwardingService = memberForwardingService;
        this.collapsedRingService = collapsedRingService;
        this.cycleForTwoService = cycleForTwoService;
        this.minimumRiskPathService = minimumRiskPathService;
        this.yensService = yensService;
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
            case MinimumCostPath:
                details = minimumCostPathService.solve(request, topo);
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
            case MemberForwarding:
                details = memberForwardingService.solve(request, topo);
                break;
            case CollapsedRing:
                details = collapsedRingService.solve(request, topo);
                break;
            case CycleForTwo:
                details = cycleForTwoService.solve(request, topo);
                break;
            case MinimumRiskPath:
                details = minimumRiskPathService.solve(request, topo);
                break;
            case Yens:
                details = yensService.solve(request, topo);
                break;
        }
        request.setDetails(details);
        return request;
    }

}

