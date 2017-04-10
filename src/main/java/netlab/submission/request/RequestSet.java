package netlab.submission.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.OverlapType;
import netlab.submission.enums.ProcessingType;

import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestSet {

    Map<String, Request> requests;

    private String id;

    private Long seed;

    private String status;

    private String topologyId;

    private ProcessingType processingType;

    private FailureClass failureClass;

    private Algorithm algorithm;

    private OverlapType overlapType;

    private boolean sdn;

    private boolean useAws;

}
