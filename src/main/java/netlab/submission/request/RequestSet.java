package netlab.submission.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.*;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestSet implements Serializable {

    private Map<String, Request> requests;

    private String id;

    private Long seed;

    private String status;

    private String topologyId;

    private ProcessingType processingType;

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
