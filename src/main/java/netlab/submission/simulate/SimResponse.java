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
public class SimResponse implements Serializable {

    private List<Connection> connections;
    private Boolean succeeded;

    public String toString(){
        StringBuilder builder = new StringBuilder();
        for(Connection connection : connections){
            List<String> pair = connection.getPair();
            List<List<String>> routes = connection.getRoutes();
            builder.append(pair).append(": ");
            for(List<String> route : routes){
                builder.append(route).append("\t");
            }
            builder.append("\n");
        }
        return builder.toString();
    }
}
