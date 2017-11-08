package netlab.topology.services;


import netlab.processing.shortestPaths.ShortestPathService;
import netlab.submission.enums.TrafficCombinationType;
import netlab.topology.elements.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TopologyService {

    Map<String, Topology> topologyIdMap;

    ShortestPathService shortestPathService;

    @Autowired
    public TopologyService(ShortestPathService shortestPathService){
        topologyIdMap = new HashMap<>();
        topologyIdMap.put("NSFnet", makeNsfNet());
        this.shortestPathService = shortestPathService;
    }

    public Topology getTopologyById(String id){
        return topologyIdMap.getOrDefault(id, topologyIdMap.get("NSFnet"));
    }

    private Topology makeNsfNet() {
        Set<Node> nodes = new HashSet<>();
        Node seattle = new Node("Seattle");
        Node paloAlto = new Node("Palo Alto");
        Node sanDiego = new Node("San Diego");
        Node saltLakeCity = new Node("Salt Lake City");
        Node boulder = new Node("Boulder");
        Node houston = new Node("Houston");
        Node lincoln = new Node("Lincoln");
        Node champaign = new Node("Champaign");
        Node annArbor = new Node("Ann Arbor");
        Node pittsburgh = new Node("Pittsburgh");
        Node atlanta = new Node("Atlanta");
        Node collegePark = new Node("College Park");
        Node ithaca = new Node("Ithaca");
        Node princeton = new Node("Princeton");
        nodes.add(seattle);
        nodes.add(paloAlto);
        nodes.add(sanDiego);
        nodes.add(saltLakeCity);
        nodes.add(boulder);
        nodes.add(houston);
        nodes.add(lincoln);
        nodes.add(champaign);
        nodes.add(annArbor);
        nodes.add(pittsburgh);
        nodes.add(atlanta);
        nodes.add(collegePark);
        nodes.add(ithaca);
        nodes.add(princeton);
        Set<Link> links = new HashSet<>();
        links.add(new Link(seattle, paloAlto, 1100L));
        links.add(new Link(seattle, sanDiego, 1600L));
        links.add(new Link(seattle, champaign, 2800L));
        links.add(new Link(paloAlto, seattle, 1100L));
        links.add(new Link(paloAlto, sanDiego, 600L));
        links.add(new Link(paloAlto, saltLakeCity, 1000L));
        links.add(new Link(sanDiego, seattle, 1600L));
        links.add(new Link(sanDiego, paloAlto, 600L));
        links.add(new Link(sanDiego, houston, 2000L));
        links.add(new Link(saltLakeCity, paloAlto, 1000L));
        links.add(new Link(saltLakeCity, annArbor, 2400L));
        links.add(new Link(saltLakeCity, boulder, 600L));
        links.add(new Link(boulder, saltLakeCity, 600L));
        links.add(new Link(boulder, houston, 1100L));
        links.add(new Link(boulder, lincoln, 800L));
        links.add(new Link(houston, sanDiego, 2000L));
        links.add(new Link(houston, boulder, 1100L));
        links.add(new Link(houston, collegePark, 2000L));
        links.add(new Link(houston, atlanta, 1200L));
        links.add(new Link(lincoln, boulder, 800L));
        links.add(new Link(lincoln, champaign, 700L));
        links.add(new Link(champaign, seattle, 2800L));
        links.add(new Link(champaign, lincoln, 700L));
        links.add(new Link(champaign, pittsburgh, 700L));
        links.add(new Link(annArbor, saltLakeCity, 2400L));
        links.add(new Link(annArbor, ithaca, 800L));
        links.add(new Link(annArbor, princeton, 800L));
        links.add(new Link(atlanta, houston, 1200L));
        links.add(new Link(atlanta, pittsburgh, 900L));
        links.add(new Link(pittsburgh, champaign, 700L));
        links.add(new Link(pittsburgh, atlanta, 900L));
        links.add(new Link(pittsburgh, princeton, 500L));
        links.add(new Link(pittsburgh, ithaca, 500L));
        links.add(new Link(collegePark, houston, 2000L));
        links.add(new Link(collegePark, princeton, 300L));
        links.add(new Link(collegePark, ithaca, 300L));
        links.add(new Link(princeton, annArbor, 800L));
        links.add(new Link(princeton, pittsburgh, 500L));
        links.add(new Link(princeton, collegePark, 300L));
        links.add(new Link(ithaca, annArbor, 800L));
        links.add(new Link(ithaca, pittsburgh, 500L));
        links.add(new Link(ithaca, collegePark, 300L));
        Topology topo = new Topology("NSFnet", nodes, links);
        return populatePathCosts(topo);
    }

    public Topology populatePathCosts(Topology topo) {
        List<SourceDestPair> pairs = new ArrayList<>();
        Map<SourceDestPair, Long> pairCostMap = new HashMap<>();
        Set<Node> nodes = topo.getNodes();
        for(Node node : nodes){
            for(Node otherNode : nodes){
                if(!otherNode.getId().equals(node.getId())){
                    SourceDestPair pair = new SourceDestPair(node, otherNode);
                    Path shortestPath = shortestPathService.findShortestPath(pair, topo, new HashMap<>(), new HashMap<>(), TrafficCombinationType.None);
                    pairs.add(pair);
                    pairCostMap.put(pair, shortestPath.getTotalWeight());
                }
            }
        }
        pairs.sort(Comparator.comparing(pairCostMap::get));
        topo.setMinimumPathCostMap(pairCostMap);
        return topo;
    }


    public Topology adjustWeightsUsingTrafficCombination(Topology topo, TrafficCombinationType trafficType, Node src,
                                                         Node dst, Map<Node, Set<Path>> srcPathsMap, Map<Node, Set<Path>> dstPathsMap){

        if(trafficType.equals(TrafficCombinationType.None)){
            return topo;
        }
        // Modify link weights if you're combining traffic
        Set<Path> zeroCostPaths = trafficType.equals(TrafficCombinationType.Source)
                || trafficType.equals(TrafficCombinationType.Both) ?
                srcPathsMap.getOrDefault(src, new HashSet<>()) : new HashSet<>();
        zeroCostPaths.addAll(trafficType.equals(TrafficCombinationType.Destination) || trafficType.equals(TrafficCombinationType.Both) ?
                dstPathsMap.getOrDefault(dst, new HashSet<>()) : new HashSet<>());
        Set<Link> zeroCostLinks = zeroCostPaths.stream().map(Path::getLinks).flatMap(List::stream).collect(Collectors.toSet());

        // Modify weights if you're combining traffic
        Set<Link> modifiedLinks = new HashSet<>();
        for(Link link : topo.getLinks()){
            Long weight = !trafficType.equals(TrafficCombinationType.None) && zeroCostLinks.contains(link) ?
                    0 : link.getWeight();
            Link modifiedLink = Link.builder().id(link.getId()).origin(link.getOrigin()).target(link.getTarget()).weight(weight).build();
            modifiedLinks.add(modifiedLink);
        }

        Topology newTopo = new Topology(topo.getId(), topo.getNodes(), modifiedLinks);
        newTopo.copyPathCosts(topo);
        return newTopo;
    }

    public List<SourceDestPair> sortPairsByPathCost(Collection<SourceDestPair> pairs, Topology topo){
        Map<SourceDestPair, Long> minimumPathCostMap = topo.getMinimumPathCostMap();
        return pairs
                .stream()
                .sorted(Comparator.comparing(minimumPathCostMap::get))
                .sorted(Comparator.comparing(p -> p.getSrc().getId()))
                .sorted(Comparator.comparing(p -> p.getDst().getId()))
                .collect(Collectors.toList());
    }

}
