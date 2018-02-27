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
public class Connection implements Serializable {
    private List<String> pair;
    private List<List<String>> routes;
}
