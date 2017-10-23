package netlab.submission.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Connections implements Serializable {

    private Integer numConnections;
    private Integer useMinS;
    private Integer useMaxS;
    private Integer useMinD;
    private Integer useMaxD;

    private Map<SourceDestPair, Integer> pairMinConnectionsMap;
    private Map<SourceDestPair, Integer> pairMaxConnectionsMap;

    private Map<Node, Integer> srcMinConnectionsMap;
    private Map<Node, Integer> srcMaxConnectionsMap;

    private Map<Node, Integer> dstMinConnectionsMap;
    private Map<Node, Integer> dstMaxConnectionsMap;
}
