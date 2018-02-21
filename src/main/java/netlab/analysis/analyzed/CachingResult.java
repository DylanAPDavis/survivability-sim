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
    private Set<Node> cachingLocations;

    private double avgHopCountBefore;
    private double avgHopCountAfter;
    private double reachOnPrimary;
    private double reachOnBackup;
    private double reachOnlyBackup;

    // Total number of caches across all pairs
    private double cachingCost;

    public CachingResult(CachingType type){
        this.type = type;
        avgHopCountBefore = 0.0;
        avgHopCountAfter = 0.0;
        reachOnPrimary = 0.0;
        reachOnBackup = 0.0;
        reachOnlyBackup = 0.0;
        cachingCost = 0.0;
        cachingLocations = new HashSet<>();
    }


    public String toString(){
        String headline = type + " " + "Hop Before: " + avgHopCountBefore + ", Hop After: " + avgHopCountAfter
                + ", Reach on Primary: " + reachOnPrimary + ", Reach on Back: " + reachOnBackup +
                ", Reach Only Back: " + reachOnlyBackup + ", Cost: " + cachingCost;
        return headline  + cacheLocationsToString() +"\n~~~";
    }

    public String cacheLocationsToString(){
        String output = "";
        String dash = "--";
        String line = dash + "Nodes: ";
        List<Node> nodes = new ArrayList<>(cachingLocations);
        for(int i = 0; i < nodes.size(); i++){
            line += nodes.get(i).getId();
            if(i < nodes.size()-1) {
                line += ", ";
            }
        }
        output += "\n" + line;
        return output;
    }
}
