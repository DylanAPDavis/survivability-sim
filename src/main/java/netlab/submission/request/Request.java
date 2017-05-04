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

    private Failures failures;
    private NumFails numFails;
    private Connections connections;

    private Set<SourceDestPair> pairs;

    private Map<SourceDestPair, List<Path>> chosenPaths;
}
