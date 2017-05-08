package netlab.topology.elements;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Path {

    private List<Link> links;
    private List<Node> nodes;

    private Set<String> linkIds;
    private Set<String> nodeIds;
}
