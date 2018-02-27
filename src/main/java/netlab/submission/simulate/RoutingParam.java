package netlab.submission.simulate;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingParam implements Serializable {
    @NonNull
    private String source;
    @NonNull
    private List<String> destinations;

    private Integer neededD;
}
