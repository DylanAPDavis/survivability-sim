package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateAverages implements Serializable {

    private Boolean forSource;
    private Boolean forDest;
    private Boolean forPair;

    private Double avgPaths;
    private List<Double> avgPathsConfInterval;

    private Double avgPathLength;
    private List<Double> avgPathLengthConfInterval;

    private Double avgPathCost;
    private List<Double> avgPathCostConfInterval;

    private Double avgDisconnectedPaths;
    private List<Double> avgDisconnectedPathsConfInterval;

    private Double avgPathsPerChosen;
    private List<Double> avgPathsPerChosenConfInterval;

    private Double avgPathLengthPerChosen;
    private List<Double> avgPathLengthPerChosenConfInterval;

    private Double avgPathCostPerChosen;
    private List<Double> avgPathCostPerChosenConfInterval;

    private Double avgDisconnectedPathsPerChosen;
    private List<Double> avgDisconnectedPathsPerChosenConfInterval;
}
