package netlab.submission.simulate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Survivability implements Serializable{
    private List<String> failures;
    private String failureScenario;
    private Integer numFailureEvents;
}
