package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.analysis.enums.CachingType;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;

import java.io.Serializable;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachingResult  implements Serializable {

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
    // Percentage of pairs that can still access content through backup path
    private double pairReachThroughBackup;

    // Total number of caches across all pairs
    private int cachingCost;

    public CachingResult(CachingType type){
        this.type = type;
        reachability = 0.0;
        avgAccessibility = 0.0;
        avgHopCountToContent = 0.0;
        pairReachThroughBackup = 0.0;
        cachingCost = 0;
        cachingMap = new HashMap<>();
    }


    public String toString(){
        String headline = type + " " + "Reach: " + reachability + " Access: " + avgAccessibility
                + " Hop: " + avgHopCountToContent + " Pair_Reach: " + pairReachThroughBackup + " Cost: " + cachingCost;
        return headline  + cachingMapToString() +"\n~~~";
    }

    public String cachingMapToString(){
        String output = "";
        String dash = "--";
        for(SourceDestPair pair : cachingMap.keySet()){
            String line = dash + pair.toString() + ": ";
            List<Node> nodes = new ArrayList<>(cachingMap.get(pair));
            for(int i = 0; i < nodes.size(); i++){
                line += nodes.get(i).getId();
                if(i < nodes.size()-1) {
                    line += ", ";
                }
            }
            output += "\n" + line;
        }
        return output;
    }
}
