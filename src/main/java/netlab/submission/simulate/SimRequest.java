package netlab.submission.simulate;

import lombok.*;
import org.springframework.beans.factory.annotation.Required;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimRequest implements Serializable {

    @NonNull
    private List<RoutingParam> routingParams;
    @NonNull
    private Network network;
    @NonNull
    private Survivability survivability;
}
