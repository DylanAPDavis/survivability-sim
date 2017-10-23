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
public class NumFailureEvents implements Serializable {
    private Integer totalNumFailureEvents;
    private Map<SourceDestPair, Integer> pairNumFailureEvents;

    private Map<Node, Integer> srcNumFailureEvents;
    private Map<Node, Integer> dstNumFailureEvents;
}
