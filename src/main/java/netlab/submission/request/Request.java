package netlab.submission.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request {

    private String id;

    private Set<Node> sources;
    private Set<Node> destinations;

    private Integer numConnections;
    private Set<Failure> failures;
    private Integer numCuts;

    // If any of the above three parameters are not set, use parameters from the pairs
    private Set<SourceDestPair> pairs;

    private Map<SourceDestPair, List<Integer>> minMaxConnectionsMap;
    private Map<SourceDestPair, Integer> numCutsMap;
    private Map<SourceDestPair, Set<Failure>> failuresMap;

    private Map<SourceDestPair, List<Path>> chosenPaths;
}
