package netlab.submission.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.ProblemClass;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request implements Serializable {

    private String id;

    private Set<Node> sources;
    private Set<Node> destinations;

    private Failures failures;
    private NumFailsAllowed numFailsAllowed;
    private Connections connections;

    private Set<SourceDestPair> pairs;

    private Map<SourceDestPair, Map<String, Path>> chosenPaths;

    private double runningTimeSeconds;

    private Boolean isFeasible;
}
