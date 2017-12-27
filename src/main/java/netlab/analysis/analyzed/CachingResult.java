package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.analysis.enums.CachingType;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachingResult {

    private CachingType type;
    private Map<SourceDestPair, Set<Node>> cachingMap;
    private double reachability;
    private double avgAccessibility;
    private double avgHopCountToContent;

    public CachingResult(CachingType type){
        this.type = type;
        reachability = 0.0;
        avgAccessibility = 0.0;
        avgHopCountToContent = 0.0;
        cachingMap = new HashMap<>();
    }


    public String toString(){
        return type + "- " + "Reach: " + reachability + " Access: " + " Hop: " + avgHopCountToContent;
    }
}
