package netlab.analysis.analyzed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.submission.enums.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingDescription {

    public RoutingType routingType;
    public Integer numSources;
    public Integer numDestinations;
    public Algorithm algorithm;
    public ProblemClass problemClass;
    public Integer minConnections;
    public Integer useMinS;
    public Integer useMaxS;
    public Integer useMinD;
    public Integer useMaxD;
    public TrafficCombinationType trafficCombinationType;
    public Double percentSrcAlsoDest;
}
