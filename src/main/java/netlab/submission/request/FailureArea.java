package netlab.submission.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailureArea {

    private Point center;
    private Integer radius;
    private Boolean mustFail;

    public FailureArea(int x, int y, int radius, boolean mustFail){
        center = new Point(x,y);
        this.radius = radius;
        this.mustFail = mustFail;
    }
}
