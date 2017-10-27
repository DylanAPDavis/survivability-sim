package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.FailureClass;
import netlab.submission.enums.FailureScenario;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailureDescription {
    public FailureClass failureClass;
    public FailureScenario failureScenario;
    public Integer numFailureEvents;
    public Integer failureSetSize;
    public Double percentSrcFail;
    public Double percentDstFail;
    public Boolean ignoreFailures;

    public String toString(){
        return failureScenario + "_" + failureClass + "_" + failureSetSize + "_" + numFailureEvents + "_" + percentSrcFail
                + "_" + percentDstFail + "_" + ignoreFailures;
    }
}

