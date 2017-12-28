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

    //Content Reachability: The percentage of sources that can still reach all of their desired content.
    private double reachability;
    // Average Content Accessibility: The average percentage of content that can still be accessed per source.
    // For example, if a source wants to access content from three destinations, and can only access content from two
    // of them (either from the destination itself, or from a cached location), then it has an accessibility percentage of 66%.
    private double avgAccessibility;
    // Average Hop Count to Content: The average hop count that will be traversed after failure to access content, per source.
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
