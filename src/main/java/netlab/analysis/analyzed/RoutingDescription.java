package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingDescription implements Serializable {

    private Integer numSources;
    private Integer numDestinations;
    private Integer useMinS;
    private Integer useMaxS;
    private Integer useMinD;
    private Integer useMaxD;


    public String toString(){
        return "|S|: " + numSources + ", |D|: " + numDestinations + ", minS: " + useMinS + ", maxS: " + useMaxS
                + ", minD: " + useMinD + ", maxD: " + useMaxD;
    }
}
