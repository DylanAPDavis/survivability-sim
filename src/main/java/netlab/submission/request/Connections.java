package netlab.submission.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Connections {

    private Integer numConnections;

    private Map<SourceDestPair, Integer> pairMinConnectionsMap;
    private Map<SourceDestPair, Integer> pairMaxConnectionsMap;

    private Map<Node, Integer> srcMinConnectionsMap;
    private Map<Node, Integer> dstMinConnectionsMap;
    private Map<Node, Integer> srcMaxConnectionsMap;
    private Map<Node, Integer> dstMaxConnectionsMap;
}
