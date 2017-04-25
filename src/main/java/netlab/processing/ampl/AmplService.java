package netlab.processing.ampl;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.Algorithm;
import netlab.submission.request.Request;
import netlab.topology.elements.SourceDestPair;
import org.springframework.stereotype.Service;

import com.ampl.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AmplService {

    private String modelDirectory = "linear-programs/models";

    public Map<SourceDestPair, List<Path>> solve(Request request, Algorithm algorithm){
        Map<SourceDestPair, List<Path>> paths = new HashMap<>();

        AMPL ampl = new AMPL();
        ampl.setOption("solver", "gurobi");


        return paths;
    }

}
