package netlab.submission.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request implements Serializable {

    private Details details;

    private String id;

    private Long seed;

    private boolean completed;

    private String topologyId;

    private FailureClass failureClass;

    private FailureScenario failureScenario;

    private Algorithm algorithm;

    private ProblemClass problemClass;

    private Objective objective;

    private TrafficCombinationType trafficCombinationType;

    private Double percentSrcAlsoDest;

    private Double percentSrcFail;

    private Double percentDestFail;

    private boolean useAws;

    private boolean ignoreFailures;

    private Integer numThreads;

}
