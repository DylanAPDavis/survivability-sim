package netlab.processing.ampl;

import com.ampl.*;
import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.Algorithm;
import netlab.submission.request.Request;
import netlab.topology.elements.Failure;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;
import netlab.topology.elements.Topology;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.springframework.stereotype.Service;

import org.apache.commons.math3.util.Combinations;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AmplService {

    private String modelDirectory = "linear-programs/models";

    public Map<SourceDestPair, List<Path>> solve(Request request, Topology topology){
        Map<SourceDestPair, List<Path>> paths = new HashMap<>();
        AMPL ampl = new AMPL();
        try {
            ampl = assignValues(request, topology);
            ampl.solve();
            Variable numConn = ampl.getVariable("Num_Conn");
            Map<String, Double> numConnections = numConn.getInstances().stream()
                    .filter(nc -> nc.value() > 0)
                    .collect(Collectors.toMap(VariableInstance::name, VariableInstance::value));
            System.out.println(numConnections);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            ampl.close();
        }
        return paths;
    }

    private AMPL assignValues(Request request, Topology topology) throws IOException{
        AMPL ampl = new AMPL();
        ampl.setOption("solver", "gurobi");
        ampl.read(modelDirectory + "/minMax.mod");

        // Assign nodes, links, sources, and destinations
        assignTopoValues(ampl, topology);
        Object[] sources = request.getSources().stream().map(Node::getId).toArray();
        Object[] destinations = request.getDestinations().stream().map(Node::getId).toArray();
        com.ampl.Set s = ampl.getSet("S");
        s.setValues(sources);
        com.ampl.Set d = ampl.getSet("D");
        d.setValues(destinations);

        // Assign the maximum number of connections possible between a pair
        Parameter iMax = ampl.getParameter("I_max");
        iMax.set(10);

        // Assign the number of connections from S to D
        Parameter cTotal = ampl.getParameter("c_total");
        cTotal.set(request.getNumConnections());

        assignPairParamsAndSets(ampl, request);

        return ampl;
     }

    private void assignTopoValues(AMPL ampl, Topology topology) {
        com.ampl.Set v = ampl.getSet("V");
        Parameter a = ampl.getParameter("A");

        Object[] nodes = topology.getNodeIdMap().keySet().toArray();
        v.setValues(nodes);

        // Generate the adjacency matrix
        Object[] links = topology.getLinks()
                .stream()
                .map(l -> new Tuple(l.getOrigin().getId(), l.getTarget().getId()))
                .toArray();
        double[] linkExists = new double[links.length];
        Arrays.fill(linkExists, 1);
        a.setValues(links, linkExists);
    }


    private void assignPairParamsAndSets(AMPL ampl, Request request){

        // Assign SD pairs and parameters indexed on those pairs
        DataFrame pairParams = new DataFrame(1, "SD", "c_min_sd", "c_max_sd", "NumGroups");

        ArrayList<SourceDestPair> pairList = new ArrayList<>(request.getPairs());
        Object[] pairs = pairList.stream().map(pair -> new Tuple(pair.getSrc().getId(), pair.getDst().getId())).toArray();

        double[] c_min_sd = new double[pairs.length];
        double[] c_max_sd = new double[pairs.length];
        double[] numGroups = new double[pairs.length];

        Map<SourceDestPair, Integer> minConnMap = request.getMinConnectionsMap();
        Map<SourceDestPair, Integer> maxConnMap = request.getMaxConnectionsMap();
        Map<SourceDestPair, Integer> numCutsMap = request.getNumCutsMap();
        Map<SourceDestPair, Set<Failure>> failureSetMap = request.getFailuresMap();
        for(int index = 0; index < pairList.size(); index++){
            SourceDestPair pair = pairList.get(index);

            c_min_sd[index] = minConnMap.get(pair);
            c_max_sd[index] = maxConnMap.get(pair);

            Set<Failure> failures = failureSetMap != null ? failureSetMap.get(pair) : request.getFailures();
            Integer numCuts = numCutsMap.get(pair);
            // Calculate the number of numCuts-sized combinations of the failure set
            double divisor = ArithmeticUtils.factorial(numCuts) * ArithmeticUtils.factorial(failures.size() - numCuts);
            numGroups[index] = ArithmeticUtils.factorial(failures.size()) / divisor;
        }
        pairParams.setColumn("SD", pairs);
        pairParams.setColumn("c_min_sd", c_min_sd);
        pairParams.setColumn("c_max_sd", c_max_sd);
        pairParams.setColumn("NumGroups", numGroups);
        ampl.setData(pairParams, "SD");

        assignFailureSetsAndGroups(ampl, pairList, pairs, request);
    }

    // Must be done after the numGroups has been assigned to the model
    private void assignFailureSetsAndGroups(AMPL ampl, List<SourceDestPair> pairList, Object[] pairs, Request request){
        Map<SourceDestPair, Integer> numCutsMap = request.getNumCutsMap();
        Map<SourceDestPair, Set<Failure>> failureSetMap = request.getFailuresMap();

        // Failure set
        com.ampl.Set f = ampl.getSet("F");

        // Failure groups
        com.ampl.Set fg = ampl.getSet("FG");

        for(int index = 0; index < pairList.size(); index++){
            SourceDestPair pair = pairList.get(index);
            Set<Failure> failures = failureSetMap != null ? failureSetMap.get(pair) : request.getFailures();
            Integer numCuts = numCutsMap.get(pair);

            Object[] failureSet = createFailureSetArray(failures);
            f.get(pairs[index]).setValues(failureSet);
            // Find all k-size subsets of this failure set
            Map<Tuple, Object[]> failureGroups = generateFailureGroups(numCuts, failureSet, pair);
            for(Tuple fgTriplet : failureGroups.keySet()){
                fg.get(fgTriplet).setValues(failureGroups.get(fgTriplet));
            }
        }
    }

    private Object[] createFailureSetArray(Set<Failure> failures) {
        Object[] failureSet = new Object[failures.size()];

        int index = 0;
        for(Failure failure: failures){
            Tuple id = failure.getLink() != null ?
                    new Tuple(failure.getLink().getOrigin().getId(), failure.getLink().getTarget().getId()):
                    new Tuple(failure.getNode().getId(), failure.getNode().getId());
            failureSet[index] = id;
            index++;
        }
        return failureSet;
    }

    private static Map<Tuple,Object[]> generateFailureGroups(Integer k, Object[] failureSet, SourceDestPair pair) {
        Map<Tuple, Object[]> groups = new HashMap<>();
        Object src = pair.getSrc().getId();
        Object dst = pair.getDst().getId();
        // Find all k-size subsets of this failure set
        Integer groupCounter = 1;
        if(failureSet.length <= k){
            groups.put(new Tuple(src, dst, groupCounter), failureSet);
        }
        else{
            Combinations combos = new Combinations(failureSet.length, k);
            for (int[] comboIndices : combos) {
                Object[] group = new Object[k];
                for (int index = 0; index < comboIndices.length; index++) {
                    group[index] = failureSet[comboIndices[index]];
                }
                groups.put(new Tuple(src, dst, groupCounter), group);
                groupCounter++;
            }
        }
        return groups;
    }


}
