package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MassAnalysisParameters {

    private List<Long> seeds;
    private String topology;
    private String routingType;
}
