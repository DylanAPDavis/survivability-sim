package netlab.submission.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.Algorithm;
import netlab.submission.enums.BatchType;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestSet {

    Set<Request> requests;

    private Long id;

    private Long seed;

    private String topologyId;

    private BatchType batchType;

    private Algorithm algorithm;

    private boolean sdn;

    private boolean useAws;

}
