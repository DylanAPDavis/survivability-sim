package netlab.submission.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Failures {
    private Integer failureSetSize;
    private Set<Failure> failureSet;
    private List<List<Failure>> failureGroups;
    private Map<SourceDestPair, Set<Failure>> pairFailuresMap;
    private Map<SourceDestPair, List<List<Failure>>> pairFailureGroupsMap;
}
