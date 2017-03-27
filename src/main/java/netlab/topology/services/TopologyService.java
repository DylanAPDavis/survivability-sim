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
        links.add(new Link(seattle, paloAlto));
        links.add(new Link(seattle, sanDiego));
        links.add(new Link(seattle, champaign));
        links.add(new Link(paloAlto, seattle));
        links.add(new Link(paloAlto, sanDiego));
        links.add(new Link(paloAlto, saltLakeCity));
        links.add(new Link(sanDiego, seattle));
        links.add(new Link(sanDiego, paloAlto));
        links.add(new Link(sanDiego, houston));
        links.add(new Link(saltLakeCity, paloAlto));
        links.add(new Link(saltLakeCity, annArbor));
        links.add(new Link(saltLakeCity, boulder));
        links.add(new Link(boulder, saltLakeCity));
        links.add(new Link(boulder, houston));
        links.add(new Link(boulder, lincoln));
        links.add(new Link(houston, sanDiego));
        links.add(new Link(houston, boulder));
        links.add(new Link(houston, collegePark));
        links.add(new Link(houston, atlanta));
        links.add(new Link(lincoln, boulder));
        links.add(new Link(lincoln, champaign));
        links.add(new Link(champaign, seattle));
        links.add(new Link(champaign, lincoln));
        links.add(new Link(champaign, pittsburgh));
        links.add(new Link(annArbor, saltLakeCity));
        links.add(new Link(annArbor, ithaca));
        links.add(new Link(annArbor, princeton));
        links.add(new Link(atlanta, houston));
        links.add(new Link(atlanta, pittsburgh));
        links.add(new Link(pittsburgh, champaign));
        links.add(new Link(pittsburgh, atlanta));
        links.add(new Link(pittsburgh, princeton));
        links.add(new Link(pittsburgh, ithaca));
        links.add(new Link(collegePark, houston));
        links.add(new Link(collegePark, princeton));
        links.add(new Link(collegePark, ithaca));
        links.add(new Link(princeton, annArbor));
        links.add(new Link(princeton, pittsburgh));
        links.add(new Link(princeton, collegePark));
        links.add(new Link(ithaca, annArbor));
        links.add(new Link(ithaca, pittsburgh));
        links.add(new Link(ithaca, collegePark));
        return new Topology("NSFnet", nodes, links);
    }
}
