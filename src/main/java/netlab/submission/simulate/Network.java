package netlab.submission.simulate;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Network implements Serializable{
    @NonNull
    private List<String> nodes;
    @NonNull
    private List<String> links;
}
