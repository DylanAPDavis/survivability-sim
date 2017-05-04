package netlab.submission.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.topology.elements.SourceDestPair;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NumFails {
    private Integer totalNumFails;
    private Map<SourceDestPair, Integer> pairNumFailsMap;
}
