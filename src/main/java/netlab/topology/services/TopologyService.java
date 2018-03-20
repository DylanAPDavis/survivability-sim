package netlab.topology.services;


import lombok.extern.slf4j.Slf4j;
import netlab.processing.shortestPaths.MinimumCostPathService;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.FailureScenario;
import netlab.submission.services.FailureAreaService;
import netlab.submission.simulate.Network;
import netlab.topology.elements.*;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceFactory;
import org.graphstream.stream.file.FileSourceGML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TopologyService {

    private Map<String, Topology> topologyIdMap;

    private Map<String, Node> nodeIdMap;

    private Map<String, Link> linkIdMap;

    private MinimumCostPathService minimumCostPathService;

    private TopologyAdjustmentService topologyAdjustmentService;


    @Autowired
    public TopologyService(MinimumCostPathService minimumCostPathService,
                           TopologyAdjustmentService topologyAdjustmentService){
        this.minimumCostPathService = minimumCostPathService;
        this.topologyAdjustmentService = topologyAdjustmentService;
        topologyIdMap = new HashMap<>();
        topologyIdMap.put("nsfnet", makeNsfNet());
        topologyIdMap.put("tw", makeTWTelecom());

        nodeIdMap = new HashMap<>();
        linkIdMap = new HashMap<>();
        for(Topology topo : topologyIdMap.values()){
            for(Node node : topo.getNodes()){
                nodeIdMap.put(node.getId(), node);
            }
            for(Link link : topo.getLinks()){
                linkIdMap.put(link.getId(), link);
            }
        }
    }

    public Topology getTopologyById(String id){
        return topologyIdMap.getOrDefault(id.toLowerCase(), topologyIdMap.get("nsfnet"));
    }

    /*
        Seattle, WA	47.60621, -122.332071
        Palo Alto, CA	37.441883, -122.143019
        San Diego, CA	32.715738, -117.161084
        Salt Lake City, UT	40.760779, -111.891047
        Boulder, CO	40.014986, -105.270546
        Houston, TX	29.760427, -95.369803
        Lincoln, NE	40.825763, -96.685198
        Champaign, IL	40.11642, -88.243383
        Ann Arbor, MI	42.280826, -83.743038
        Pittsburgh, PA	40.440625, -79.995886
        Atlanta, GA	33.748995, -84.387982
        College Park, MD	38.989697, -76.93776
        Ithaca, NY	42.443961, -76.501881
        Princeton, NJ	40.357298, -74.667223
     */
    private Topology makeNsfNet() {
        Set<Node> nodes = new HashSet<>();
        Node seattle = new Node("Seattle", 47.60621, -122.332071);
        Node paloAlto = new Node("Palo Alto", 37.441883, -122.143019);
        Node sanDiego = new Node("San Diego", 32.715738, -117.161084);
        Node saltLakeCity = new Node("Salt Lake City", 40.760779, -111.891047);
        Node boulder = new Node("Boulder", 40.014986, -105.270546);
        Node houston = new Node("Houston", 29.760427, -95.369803);
        Node lincoln = new Node("Lincoln", 40.825763, -96.685198);
        Node champaign = new Node("Champaign", 40.11642, -88.243383);
        Node annArbor = new Node("Ann Arbor", 42.280826, -83.743038);
        Node pittsburgh = new Node("Pittsburgh", 40.440625, -79.995886);
        Node atlanta = new Node("Atlanta", 33.748995, -84.387982);
        Node collegePark = new Node("College Park", 38.989697, -76.93776);
        Node ithaca = new Node("Ithaca", 42.443961, -76.501881);
        Node princeton = new Node("Princeton", 40.357298, -74.667223);
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
        links.add(new Link(seattle, paloAlto, 1100.0));
        links.add(new Link(seattle, sanDiego, 1600.0));
        links.add(new Link(seattle, champaign, 2800.0));
        links.add(new Link(paloAlto, seattle, 1100.0));
        links.add(new Link(paloAlto, sanDiego, 600.0));
        links.add(new Link(paloAlto, saltLakeCity, 1000.0));
        links.add(new Link(sanDiego, seattle, 1600.0));
        links.add(new Link(sanDiego, paloAlto, 600.0));
        links.add(new Link(sanDiego, houston, 2000.0));
        links.add(new Link(saltLakeCity, paloAlto, 1000.0));
        links.add(new Link(saltLakeCity, annArbor, 2400.0));
        links.add(new Link(saltLakeCity, boulder, 600.0));
        links.add(new Link(boulder, saltLakeCity, 600.0));
        links.add(new Link(boulder, houston, 1100.0));
        links.add(new Link(boulder, lincoln, 800.0));
        links.add(new Link(houston, sanDiego, 2000.0));
        links.add(new Link(houston, boulder, 1100.0));
        links.add(new Link(houston, collegePark, 2000.0));
        links.add(new Link(houston, atlanta, 1200.0));
        links.add(new Link(lincoln, boulder, 800.0));
        links.add(new Link(lincoln, champaign, 700.0));
        links.add(new Link(champaign, seattle, 2800.0));
        links.add(new Link(champaign, lincoln, 700.0));
        links.add(new Link(champaign, pittsburgh, 700.0));
        links.add(new Link(annArbor, saltLakeCity, 2400.0));
        links.add(new Link(annArbor, ithaca, 800.0));
        links.add(new Link(annArbor, princeton, 800.0));
        links.add(new Link(atlanta, houston, 1200.0));
        links.add(new Link(atlanta, pittsburgh, 900.0));
        links.add(new Link(pittsburgh, champaign, 700.0));
        links.add(new Link(pittsburgh, atlanta, 900.0));
        links.add(new Link(pittsburgh, princeton, 500.0));
        links.add(new Link(pittsburgh, ithaca, 500.0));
        links.add(new Link(collegePark, houston, 2000.0));
        links.add(new Link(collegePark, princeton, 300.0));
        links.add(new Link(collegePark, ithaca, 300.0));
        links.add(new Link(princeton, annArbor, 800.0));
        links.add(new Link(princeton, pittsburgh, 500.0));
        links.add(new Link(princeton, collegePark, 300.0));
        links.add(new Link(ithaca, annArbor, 800.0));
        links.add(new Link(ithaca, pittsburgh, 500.0));
        links.add(new Link(ithaca, collegePark, 300.0));
        Topology topo = new Topology("nsfnet", nodes, links);
        return populatePathCosts(topo);
    }

    public Topology makeTWTelecom(){
        String graphName = "tw";
        Graph g = readGraphModel(graphName);

        return populatePathCosts(makeTopologyFromGraph(g));
    }

    private Topology makeTopologyFromGraph(Graph g) {
        Set<Node> nodes = new HashSet<>();
        Map<String, Node> nodeIdMap = new HashMap<>();
        Map<String, Set<String>> nodeIdNeighborsMap = new HashMap<>();
        Set<Link> links = new HashSet<>();

        List<String> simpleNodes = new ArrayList<>();
        List<List<String>> simpleLinks = new ArrayList<>();


        Set<Set<String>> usedPairs = new HashSet<>();

        Map<String, String> idToLabelMap = new HashMap<>();

        for(org.graphstream.graph.Node node : g.getNodeSet()){
            simpleNodes.add(node.getId());
            Iterator<org.graphstream.graph.Node> neighbors = node.getNeighborNodeIterator();
            // Skip nodes with no neighbors
            if(!neighbors.hasNext()){
                continue;
            }
            String label = node.getAttribute("ui.label");
            idToLabelMap.put(node.getId(), label);
            Double longitude = node.getAttribute("Longitude");
            Double latitude = node.getAttribute("Latitude");
            Location nodePoint = new Location(latitude, longitude);
            Node thisNode = new Node(label, nodePoint);
            nodes.add(thisNode);
            nodeIdMap.put(label, thisNode);
            nodeIdNeighborsMap.putIfAbsent(label, new HashSet<>());
            while(neighbors.hasNext()){
                org.graphstream.graph.Node neighbor = neighbors.next();
                Set<String> pair = new HashSet<>();
                pair.add(node.getId());
                pair.add(neighbor.getId());
                if(!usedPairs.contains(pair)) {
                    List<String> link = Arrays.asList(node.getId(), neighbor.getId());
                    simpleLinks.add(link);
                    usedPairs.add(pair);
                }
                String neighborLabel = neighbor.getAttribute("ui.label");
                nodeIdNeighborsMap.get(label).add(neighborLabel);
            }
        }

        for(String nodeId : nodeIdNeighborsMap.keySet()){
            Node origin = nodeIdMap.get(nodeId);
            for(String neighborId : nodeIdNeighborsMap.get(nodeId)){
                Node target = nodeIdMap.get(neighborId);
                Link otLink = new Link(origin, target, origin.getPoint().distanceTo(target.getPoint()));
                links.add(otLink);
            }
        }

        //System.out.println(simpleNodes);
        //System.out.println(simpleLinks);
        /*
        Set<Failure> failures = failureAreaService.generateFailures(FailureScenario.Quake_2, nodes, links, FailureClass.Link);
        Set<Link> failureLinks = failures.stream().map(Failure::getLink).collect(Collectors.toSet());
        Set<List<String>> simpleFailureLinks = failureLinks.stream().map(l -> Arrays.asList(l.getOrigin().getId(), l.getTarget().getId())).collect(Collectors.toSet());

        List<List<String>> actualFailureLinks = new ArrayList<>();

        for(List<String> simpleLink : simpleLinks){
            // Check the failure links
            String originId = simpleLink.get(0);
            String targetId = simpleLink.get(1);
            List<String> modSimpleLink = Arrays.asList(idToLabelMap.get(originId), idToLabelMap.get(targetId));
            List<String> modRevLink = Arrays.asList(idToLabelMap.get(targetId), idToLabelMap.get(originId));
            if(simpleFailureLinks.contains(modSimpleLink) || simpleFailureLinks.contains(modRevLink)){
                if(!actualFailureLinks.contains(simpleLink)) {
                    actualFailureLinks.add(simpleLink);
                }
            }
        }

        System.out.println(actualFailureLinks);
        */

        return new Topology(g.getId(), nodes, links);
    }


    public Graph readGraphModel(String graphName){
        String filePath = System.getProperty("user.dir") + "/config/topologies/" + graphName + "/" + graphName + ".gml";
        Graph g = new DefaultGraph(graphName);
        FileSource fs = new FileSourceGML();

        try {
            fs.addSink(g);
            fs.readAll(filePath);
        } catch( IOException e) {
            e.printStackTrace();
        } finally {
            fs.removeSink(g);
        }

        return g;
    }

    public Topology populatePathCosts(Topology topo) {
        Map<SourceDestPair, Path> allPairsPathMap = minimumCostPathService.findAllShortestPaths(topo);
        Map<SourceDestPair, Double> allPairsWeightMap = allPairsPathMap.keySet().stream()
                .collect(Collectors.toMap(p -> p, p -> allPairsPathMap.get(p).getTotalWeight()));
        topo.setMinimumPathCostMap(allPairsWeightMap);
        return topo;
    }

    public Node getNodeById(String id){
        return nodeIdMap.get(id);
    }

    public Link getLinkById(String id){
        return linkIdMap.get(id);
    }

    public String getMetrics(Topology topo){
        // Min Degree
        // Max Degree
        // Avg Degree
        // Longest min-hop path (Diameter)
        // Maximum min-cost path
        Topology equalCostLinksTopo = topologyAdjustmentService.adjustWeightsToOneWithLinks(topo, topo.getLinks());
        int minDegree = Integer.MAX_VALUE;
        int maxDegree = 0;
        double totalDegree = 0.0;
        int numNodes = topo.getNodes().size();
        double longestMinHop = 0;
        double maxMinCost = 0;
        for(Node node : topo.getNodeLinkMap().keySet()){
            Set<Link> links = topo.getNodeLinkMap().get(node);
            int degree = links.size();
            if(degree < minDegree){
                minDegree = degree;
            }
            if(degree > maxDegree){
                maxDegree = degree;
            }
            totalDegree += degree;
            for(Node node2 : topo.getNodes()){
                if(!node.getId().equals(node2.getId())){
                    Path minCostPath = minimumCostPathService.findShortestPath(node, node2, topo);
                    Path minHopPath = minimumCostPathService.findShortestPath(node, node2, equalCostLinksTopo);
                    if(minCostPath.getTotalWeight() > maxMinCost){
                        maxMinCost = minCostPath.getTotalWeight();
                    }
                    if(minHopPath.getTotalWeight() > longestMinHop){
                        longestMinHop = minHopPath.getTotalWeight();
                    }
                }
            }
        }
        double avgDegree =totalDegree / numNodes;

        String metrics = "Topology metrics: " + topo.getId() + "\n";
        metrics += "--------------\n";
        metrics += "Min Degree: " + minDegree + "" + "\n";
        metrics += "Max Degree: " + maxDegree + "" + "\n";
        metrics += "Avg Degree: " + avgDegree + "" + "\n";
        metrics += "Max Min Hop: " + longestMinHop + "" + "\n";
        metrics += "Max Min Cost: " + maxMinCost + "" + "\n";
        return metrics;
    }

    public Topology convert(Network network) {

        List<String> nodeStrings = network.getNodes();
        List<String> linkStrings = network.getLinks();

        Set<Node> nodes = new HashSet<>();
        Set<Link> links = new HashSet<>();

        //TODO: Convert!
        double count = 0.0;
        Map<String, Node> nodeIdMap = new HashMap<>();
        for(String nodeString : nodeStrings){
            Node newNode = new Node(nodeString, count, count);
            nodes.add(newNode);
            nodeIdMap.put(nodeString, newNode);
            count += 1.0;
        }

        for(String linkString : linkStrings){
            String[] linkComponents = linkString.split("-");
            if(linkComponents.length != 2){
                continue;
            }
            String origin = linkComponents[0];
            String target = linkComponents[1];
            Link newLink = new Link(nodeIdMap.get(origin), nodeIdMap.get(target));
            Link revLink = newLink.reverse();
            links.add(newLink);
            links.add(revLink);
        }

        return new Topology("generated", nodes, links);
    }
}
