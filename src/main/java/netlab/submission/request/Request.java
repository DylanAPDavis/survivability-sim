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

    private String status;

    private String topologyId;

    private FailureClass failureClass;

    private Algorithm algorithm;

    private ProblemClass problemClass;

    private Objective objective;

    private Double percentSrcAlsoDest;

    private Double percentSrcFail;

    private Double percentDestFail;

    private boolean sdn;

    private boolean useAws;

    private Integer numThreads;

}
