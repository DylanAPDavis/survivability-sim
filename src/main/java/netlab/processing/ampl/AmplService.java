package netlab.processing.ampl;

import com.ampl.*;
import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.springframework.stereotype.Service;

import org.apache.commons.math3.util.Combinations;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AmplService {

    private String modelDirectory = "linear-programs/models";

    public Map<SourceDestPair, Map<String, Path>> solve(Request request, ProblemClass problemClass, Topology topology){
        Map<SourceDestPair, Map<String, Path>> paths = new HashMap<>();
        AMPL ampl = new AMPL();
        Instant start = Instant.now();
        try {
            ampl = assignValues(request, problemClass, topology);
            ampl.solve();
            paths = translateFlowsIntoPaths(ampl.getVariable("L"), request.getPairs(), topology);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            ampl.close();
        }
        Long duration = Instant.now().minusMillis(start.toEpochMilli()).toEpochMilli();
        System.out.println("Solution took: " + duration + " milliseconds");
        return paths;
    }

    private Map<SourceDestPair,Map<String,Path>> translateFlowsIntoPaths(Variable linkFlows, Set<SourceDestPair> pairs, Topology topo) {
        List<String> flows = linkFlows.getInstances()
                .stream()
                .filter(f -> f.value() > 0)
                .map(VariableInstance::name)
                .collect(Collectors.toList());
        Map<SourceDestPair, Map<String, Path>> pathMap = pairs.stream()
                .collect(Collectors.toMap(p -> p, p -> new HashMap<>()));
        Map<String, Link> linkIdMap = topo.getLinkIdMap();
        Map<String, Node> nodeIdMap = topo.getNodeIdMap();
        for(String flow : flows){
            String[] components = flow.substring(2, flow.length()-1).split(",");
            System.out.println(Arrays.toString(components));
            String src = components[0].replace("'", "");
            String dst = components[1].replace("'", "");
            String pathId = components[2].replace("'", "");
            String origin = components[3].replace("'", "");
            String target = components[4].replace("'", "");

            SourceDestPair thisPair = SourceDestPair.builder()
                    .src(nodeIdMap.get(src))
                    .dst(nodeIdMap.get(dst))
                    .build();
            Link link = linkIdMap.get(origin + "-" + target);
            Map<String, Path> pairMap = pathMap.get(thisPair);
            // Path already exists, add to it
            if(pairMap.containsKey(pathId)){
                Path thisPath = pairMap.get(pathId);
                thisPath.getLinks().add(link);
            }
            // New path
            else{
                List<Link> links = new ArrayList<>();
                links.add(link);
                Path newPath = Path.builder()
                        .links(links)
                        .build();
                pairMap.put(pathId, newPath);
            }
        }
        //printPaths(pathMap);
        pathMap = sortPaths(pathMap);
        //System.out.println("-----------------------------");
        //printPaths(pathMap);
        return pathMap;
    }

    private void printPaths(Map<SourceDestPair, Map<String, Path>> pairPathMap){
        for(SourceDestPair pair : pairPathMap.keySet()){
            System.out.println("Pair: (" + pair.getSrc().getId() + ", " + pair.getDst().getId() + ")");
            System.out.println("---");
            Map<String, Path> pathMap = pairPathMap.get(pair);
            for(String pathId : pathMap.keySet()){
                String pathString = pathId + ": ";
                for(Link link : pathMap.get(pathId).getLinks()){
                    pathString += "(" + link.getOrigin().getId() + ", " + link.getTarget().getId() + ") ";
                }
                System.out.println(pathString);
            }
            System.out.println("~~~~~~~");
        }
    }

    private Map<SourceDestPair, Map<String, Path>> sortPaths(Map<SourceDestPair, Map<String, Path>> pathMap) {
        for(SourceDestPair pair : pathMap.keySet()){
            Map<String, Path> mapForPair = pathMap.get(pair);
            for(Path path : mapForPair.values()){
                sortPath(path, pair);
            }
        }
        return pathMap;
    }

    private void sortPath(Path path, SourceDestPair pair) {
        List<Link> links = path.getLinks();

        List<Link> sortedLinks = new ArrayList<>();
        List<Node> sortedNodes = new ArrayList<>();
        Set<String> linkIds = new HashSet<>();
        Set<String> nodeIds = new HashSet<>();

        Map<Node, Link> outgoingLinks = new HashMap<>();
        for(Link link : links){
            outgoingLinks.put(link.getOrigin(), link);
        }
        Link currLink = outgoingLinks.get(pair.getSrc());

        // While the next node has an outgoing link
        while(outgoingLinks.containsKey(currLink.getTarget())){
            sortedLinks.add(currLink);
            linkIds.add(currLink.getId());

            sortedNodes.add(currLink.getOrigin());
            nodeIds.add(currLink.getOrigin().getId());

            currLink = outgoingLinks.get(currLink.getTarget());
        }
        sortedLinks.add(currLink);
        linkIds.add(currLink.getId());
        sortedNodes.add(currLink.getTarget());
        nodeIds.add(currLink.getTarget().getId());

        path.setLinks(sortedLinks);
        path.setLinkIds(linkIds);
        path.setNodes(sortedNodes);
        path.setNodeIds(nodeIds);
    }


    private AMPL assignValues(Request request, ProblemClass problemClass, Topology topology) throws IOException{
        AMPL ampl = new AMPL();
        ampl.setOption("solver", "gurobi");
        if(problemClass.equals(ProblemClass.Flex)){
            ampl.read(modelDirectory + "/flex.mod");
        }
        else{
            ampl.read(modelDirectory + "/flow.mod");
        }

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
        cTotal.set(request.getConnections().getNumConnections());

        assignPairParamsAndSets(ampl, request, problemClass);

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


    private void assignPairParamsAndSets(AMPL ampl, Request request, ProblemClass problemClass){

        // Assign SD pairs and parameters indexed on those pairs
        if(problemClass.equals(ProblemClass.Flex)){
            Set<Failure> failures = request.getFailures().getFailureSet();
            int numFails = request.getNumFailsAllowed().getTotalNumFailsAllowed();
            double divisor = CombinatoricsUtils.factorial(numFails) * CombinatoricsUtils.factorial(failures.size() - numFails);
            Parameter numGroups = ampl.getParameter("NumGroups");
            numGroups.set(CombinatoricsUtils.factorial(failures.size()) / divisor);
            assignFailureSetAndGroups(ampl, request);
        }
        else{
            DataFrame pairParams = new DataFrame(1, "SD", "c_min_sd", "c_max_sd", "NumGroups");

            ArrayList<SourceDestPair> pairList = new ArrayList<>(request.getPairs());
            Object[] pairs = pairList.stream().map(pair -> new Tuple(pair.getSrc().getId(), pair.getDst().getId())).toArray();

            double[] c_min_sd = new double[pairs.length];
            double[] c_max_sd = new double[pairs.length];
            double[] numGroups = new double[pairs.length];

            Map<SourceDestPair, Integer> minConnMap = request.getConnections().getPairMinConnectionsMap();
            Map<SourceDestPair, Integer> maxConnMap = request.getConnections().getPairMaxConnectionsMap();
            Map<SourceDestPair, Integer> numFailsMap = request.getNumFailsAllowed().getPairNumFailsAllowedMap();
            Map<SourceDestPair, Set<Failure>> failureSetMap = request.getFailures().getPairFailuresMap();
            for(int index = 0; index < pairList.size(); index++){
                SourceDestPair pair = pairList.get(index);

                c_min_sd[index] = minConnMap.get(pair);
                c_max_sd[index] = maxConnMap.get(pair);

                Set<Failure> failures = failureSetMap.getOrDefault(pair, request.getFailures().getFailureSet());
                Integer numFails = numFailsMap.get(pair);
                // Calculate the number of totalNumFailsAllowed-sized combinations of the failure set
                double divisor = CombinatoricsUtils.factorial(numFails) * CombinatoricsUtils.factorial(failures.size() - numFails);
                numGroups[index] = CombinatoricsUtils.factorial(failures.size()) / divisor;
            }
            pairParams.setColumn("SD", pairs);
            pairParams.setColumn("c_min_sd", c_min_sd);
            pairParams.setColumn("c_max_sd", c_max_sd);
            pairParams.setColumn("NumGroups", numGroups);
            ampl.setData(pairParams, "SD");

            assignFailureSetsAndGroups(ampl, pairList, pairs, request);
        }

    }

    private void assignFailureSetAndGroups(AMPL ampl, Request request){
        // Failure set
        com.ampl.Set f = ampl.getSet("F");

        // Failure groups
        com.ampl.Set fg = ampl.getSet("FG");
        Set<Failure> failures = request.getFailures().getFailureSet();

        Object[] failureSet = createFailureSetArray(failures);
        f.setValues(failureSet);
        // Find all k-size subsets of this failure set
        Map<Tuple, Object[]> failureGroups = convertFailureGroups(request.getFailures().getFailureGroups(), new ArrayList<>());
        for(Tuple fgTuple : failureGroups.keySet()){
            fg.get(fgTuple.get(0)).setValues(failureGroups.get(fgTuple));
        }
    }


    // Must be done after the numGroups has been assigned to the model
    private void assignFailureSetsAndGroups(AMPL ampl, List<SourceDestPair> pairList, Object[] pairs, Request request){
        Map<SourceDestPair, Integer> numFailsMap = request.getNumFailsAllowed().getPairNumFailsAllowedMap();
        Map<SourceDestPair, Set<Failure>> failureSetMap = request.getFailures().getPairFailuresMap();
        Map<SourceDestPair, List<List<Failure>>> failureGroupsMap = request.getFailures().getPairFailureGroupsMap();

        // Failure set
        com.ampl.Set f = ampl.getSet("F");

        // Failure groups
        com.ampl.Set fg = ampl.getSet("FG");

        for(int index = 0; index < pairList.size(); index++){
            SourceDestPair pair = pairList.get(index);
            Set<Failure> failures = failureSetMap.getOrDefault(pair, request.getFailures().getFailureSet());

            Object[] failureSet = createFailureSetArray(failures);
            f.get(pairs[index]).setValues(failureSet);
            // Find all k-size subsets of this failure set
            List<Object> tupleArgs = new ArrayList<>();
            tupleArgs.add(pair.getSrc().getId());
            tupleArgs.add(pair.getDst().getId());
            List<List<Failure>> failureGroupList = failureGroupsMap.get(pair);
            Map<Tuple, Object[]> failureGroups = convertFailureGroups(failureGroupList, tupleArgs);
            for(Tuple fgTriplet : failureGroups.keySet()){
                fg.get(fgTriplet).setValues(failureGroups.get(fgTriplet));
            }
        }
    }

    private Object[] createFailureSetArray(Collection<Failure> failures) {
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

    private Map<Tuple, Object[]> convertFailureGroups(List<List<Failure>> failureGroups, List<Object> tupleArgs){
        Map<Tuple, Object[]> failureGroupMap = new HashMap<>();
        for(int index = 0; index < failureGroups.size(); index++){
            List<Failure> failureGroup = failureGroups.get(index);
            Object[] failureSet = createFailureSetArray(failureGroup);
            if(tupleArgs.size() == 2){
                failureGroupMap.put(new Tuple(tupleArgs.get(0), tupleArgs.get(1), index+1), failureSet);
            }
            else{
                failureGroupMap.put(new Tuple(index+1), failureSet);
            }
        }
        return failureGroupMap;
    }

    private static Map<Tuple,Object[]> generateFailureGroups(Integer k, Object[] failureSet, List<Object> tupleArgs) {
        Map<Tuple, Object[]> groups = new HashMap<>();
        // Find all k-size subsets of this failure set
        Integer groupCounter = 1;
        if(failureSet.length <= k){
            if(tupleArgs.size() > 1) {
                groups.put(new Tuple(tupleArgs.get(0), tupleArgs.get(1), groupCounter), failureSet);
            }
            else{
                groups.put(new Tuple(groupCounter), failureSet);
            }
        }
        else{
            Combinations combos = new Combinations(failureSet.length, k);
            for (int[] comboIndices : combos) {
                Object[] group = new Object[k];
                for (int index = 0; index < comboIndices.length; index++) {
                    group[index] = failureSet[comboIndices[index]];
                }
                if(tupleArgs.size() > 1) {
                    groups.put(new Tuple(tupleArgs.get(0), tupleArgs.get(1), groupCounter), group);
                }
                else{
                    groups.put(new Tuple(groupCounter), group);
                }
                groupCounter++;
            }
        }
        return groups;
    }


}
