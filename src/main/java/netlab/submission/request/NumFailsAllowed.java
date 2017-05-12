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
public class NumFailsAllowed {
    private Integer totalNumFailsAllowed;
    private Map<SourceDestPair, Integer> pairNumFailsAllowedMap;

    private Map<Node, Integer> srcNumFailsAllowedMap;
    private Map<Node, Integer> dstNumFailsAllowedMap;
}
