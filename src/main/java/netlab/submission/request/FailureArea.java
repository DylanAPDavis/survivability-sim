package netlab.submission.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import netlab.topology.elements.Location;

import java.awt.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailureArea {

    private Location center;
    private Integer radius;
    private Boolean mustFail;

    public FailureArea(double latitude, double longitude, int radius, boolean mustFail){
        center = new Location(latitude,longitude);
        this.radius = radius;
        this.mustFail = mustFail;
    }
}
