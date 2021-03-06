package netlab.processing.tabu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Solution {

    private Set<String> pathIds;
    private Double cost;
    private Double fitness;
    private Map<SourceDestPair, Set<String>> pairPathMap;
    private Map<Node, Set<String>> srcPathMap;
    private Map<Node, Set<String>> dstPathMap;


    Solution copy(){
        return new Solution(new HashSet<>(pathIds), cost, fitness, new HashMap<>(pairPathMap), new HashMap<>(srcPathMap), new HashMap<>(dstPathMap));
    }
}
