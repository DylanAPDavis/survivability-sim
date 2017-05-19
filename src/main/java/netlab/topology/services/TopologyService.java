package netlab.topology.services;


import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.Topology;
import org.joda.time.Hours;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TopologyService {

    Map<String, Topology> topologyIdMap;

    public Topology getTopologyById(String id){
        return topologyIdMap.getOrDefault(id, null);
    }

    public TopologyService(){
        topologyIdMap = new HashMap<>();
        topologyIdMap.put("NSFnet", makeNsfNet());
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
        return new Topology("NSFnet", nodes, links);
    }
}
