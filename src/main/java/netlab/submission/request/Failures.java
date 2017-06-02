package netlab.submission.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Failures implements Serializable {
    private Integer failureSetSize;

    private Set<Failure> failureSet;
    private List<List<Failure>> failureGroups;

    private Map<SourceDestPair, Set<Failure>> pairFailuresMap;
    private Map<SourceDestPair, List<List<Failure>>> pairFailureGroupsMap;

    private Map<Node, Set<Failure>> srcFailuresMap;
    private Map<Node, Set<Failure>> dstFailuresMap;
    private Map<Node, List<List<Failure>>> srcFailureGroupsMap;
    private Map<Node, List<List<Failure>>> dstFailureGroupsMap;
}
