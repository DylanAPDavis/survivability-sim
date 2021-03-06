package netlab.visualization;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import netlab.topology.elements.SourceDestPair;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

@Service
@Slf4j
public class VisualizationService {


    public void visualize(Request request){
        try {
            File htmlTemplateFile = new File(System.getProperty("user.dir") + "/src/main/java/netlab/visualization/visualizerTemplate.html");
            Details details = request.getDetails();
            String htmlString = FileUtils.readFileToString(htmlTemplateFile);
            String data = requestSetToJson(details);
            htmlString = htmlString.replace("/* JAVA Put the data here! */", data);
            File newHtmlFile = new File(System.getProperty("user.dir") + "/results/visuals/" + request.getId()+ ".html");
            FileUtils.writeStringToFile(newHtmlFile, htmlString);
        } catch(Exception ex) {
            System.out.println("Error creating result visualization:" + ex.getMessage());
        }
    }

    private String requestSetToJson(Details details) {
        String data = "var data=[";
        Map<SourceDestPair, Map<String, Path>> chosenPaths = details.getChosenPaths();
        for(SourceDestPair pair : details.getPairs()) {
            Map<String, Path> pathMap = chosenPaths.get(pair);
            if(chosenPaths.get(pair).size() > 0) {
                data += "{";
                data += "source: \"" + pair.getSrc().getId() + "\",";
                data += "destination: \"" + pair.getDst().getId() + "\",";
                data += "paths: [";
                for (String pathId : pathMap.keySet()) {
                    Path path = pathMap.get(pathId);
                    data += "{";
                    if (path.getNodes().size() > 0) {
                        data += "javaConnectionId: \"" + pathId + "\",";
                        data += "steps: [";
                        for (Node node : path.getNodes()) {
                            data += "\"" + node.getId() + "\",";
                        }
                        data += "]";
                    }
                    data += "},";
                }
                data += "]},";
            }
        }
        data += "];";
        return data;
    }
}
