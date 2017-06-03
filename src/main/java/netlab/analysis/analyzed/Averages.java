package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Averages implements Serializable {
    private Boolean forSource;
    private Boolean forDest;
    private Boolean forPair;

    private Double avgPaths;
    private Double avgPathLength;
    private Double avgPathCost;
    private Double avgDisconnectedPaths;
    private Double avgPathsPerChosen;
    private Double avgPathLengthPerChosen;
    private Double avgPathCostPerChosen;
    private Double avgDisconnectedPathsPerChosen;
}
