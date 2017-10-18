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
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analysis implements Serializable {

    private String requestSetId;
    private Long seed;
    private ProblemClass problemClass;
    private Algorithm algorithm;
    private Objective objective;
    private FailureClass failureClass;

    private RequestMetrics requestMetrics;
}
