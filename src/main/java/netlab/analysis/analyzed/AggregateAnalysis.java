package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateAnalysis implements Serializable {

    private List<String> requestSetIds;
    private List<Long> seeds;
    private ProblemClass problemClass;
    private Algorithm algorithm;
    private Objective objective;
    private FailureClass failureClass;

    private Integer numRequests;

    private Double runningTime;
    private Double totalCost;
    private Double totalPaths;
    private Double averagePrimaryHops;

    // After failure simulation
    private Double averagePrimaryHopsPostFailure;
    private Double pathsSevered;
    private Double pathsRemaining;
    private Double connectionsDisrupted;

}
