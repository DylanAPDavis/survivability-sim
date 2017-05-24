package netlab.processing.ampl;


import com.ampl.*;
import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AmplService {

    private String modelDirectory = "linear-programs/models";

    public Request solve(Request request, ProblemClass problemClass, Objective objective, Topology topology){
        Map<SourceDestPair, Map<String, Path>> paths = new HashMap<>();
        Environment env = new Environment(System.getProperty("user.dir") + "/linear-programs/ampl/");
        AMPL ampl = new AMPL(env);
        double duration = 0.0;
        try {
            ampl = assignValues(request, problemClass, objective, topology, ampl);
            long startTime = System.nanoTime();
            ampl.solve();
            long endTime = System.nanoTime();
            duration = (endTime - startTime)/1e6;
            System.out.println("Solution took: " + duration + " milliseconds");
            com.ampl.Objective obj = ampl.getObjective(objective.getCode());
            String result = obj.result();
            if(result.toLowerCase().contains("solved")){
                request.setIsFeasible(true);
            }
            DataFrame flows = ampl.getData("L");
            paths = translateFlowsIntoPaths(flows, request.getPairs(), topology);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            ampl.close();
        }
        request.setChosenPaths(paths);
        request.setRunningTimeMillis(duration);
        return request;
    }


    private void assignTopoValues(AMPL ampl, Topology topology) {
        com.ampl.Set v = ampl.getSet("V");
        Parameter a = ampl.getParameter("A");
        Parameter weight = ampl.getParameter("Weight");

        Object[] nodes = topology.getNodeIdMap().keySet().toArray();
        v.setValues(nodes);

        Set<Link> linkSet = topology.getLinks();
        // Generate the adjacency matrix
        Object[] links = new Object[linkSet.size()];
        double[] linkExists = new double[linkSet.size()];
        double[] linkWeights = new double[linkSet.size()];
        int index = 0;
        for(Link link : topology.getLinks()){
            links[index] = new Tuple(link.getOrigin().getId(), link.getTarget().getId());
            linkExists[index] = 1;
            linkWeights[index] = link.getWeight();
            index++;
        }
        a.setValues(links, linkExists);
        weight.setValues(links, linkWeights);
    }


    private AMPL assignValues(Request request, ProblemClass problemClass, Objective objective, Topology topology, AMPL ampl) throws IOException{
        if(problemClass.equals(ProblemClass.Flex)){
            ampl.read(modelDirectory + "/flex.mod");
        }
        if(problemClass.equals(ProblemClass.Endpoint)){
            ampl.read(modelDirectory + "/endpoint.mod");
        }
        if(problemClass.equals(ProblemClass.Flow)){
            ampl.read(modelDirectory + "/flow.mod");
        }

        ampl.eval("objective " + objective.getCode()  + ";");
        ampl.setIntOption("omit_zero_rows", 1);
        ampl.setOption("solver", "gurobi");

        List<String> dataLines = createDataLines(request, topology, problemClass);
        java.nio.file.Path file = Paths.get(request.getId() + ".dat");
        try {
            // Create the empty file with default permissions, etc.
            Files.createFile(file);
        } catch (FileAlreadyExistsException x) {
            System.err.format("file named %s" +
                    " already exists%n", file);
        } catch (IOException x) {
            // Some other sort of failure, such as permissions.
            System.err.format("createFile error: %s%n", x);
        }
        Files.write(file, dataLines);
        ampl.readData(request.getId() + ".dat");

        Files.delete(file);
        ampl.setIntOption("times", 1);
        ampl.setIntOption("gentimes", 1);
        ampl.setIntOption("show_stats", 1);

        return ampl;
    }

    private List<String> createDataLines(Request request, Topology topology, ProblemClass problemClass) {
        List<String> dataLines = new ArrayList<>();
        // Topology
        String v = "set V := ";
        List<Node> nodes = new ArrayList<>(topology.getNodes());
        for(int vIndex = 0; vIndex < nodes.size(); vIndex++){
            Node node = nodes.get(vIndex);
            v += vIndex < nodes.size() - 1 ?  " '" + node.getId() + "'," : " '" + node.getId() + "';";
        }
        dataLines.add(v);
        String a = "param A: ";
        String weight = "param Weight: ";
        Map<Node, Set<Link>> nodeLinkMap = topology.getNodeLinkMap();
        String linkIndex = nodes.stream().map(n -> "'" + n.getId() + "'").reduce(" ", (n1, n2) -> n1 + " " + n2);
        a += linkIndex + " :=";
        weight += linkIndex + " :=";
        for(Node node : nodes){
            String linkLine = " '" + node.getId() + "'";
            String weightLine = " '" + node.getId() + "'";
            Set<Link> linksForNode = nodeLinkMap.get(node);
            Map<Node, Link> linkExists = new HashMap<>();
            for(Link link : linksForNode){
                linkExists.put(link.getTarget(), link);
            }
            for(Node otherNode : nodes){
                if(linkExists.containsKey(otherNode)){
                    Link connectingLink = linkExists.get(otherNode);
                    linkLine += " 1";
                    weightLine += " " + connectingLink.getWeight();
                }
                else{
                    linkLine += " 0";
                    weightLine += " 0";
                }
            }
            a += linkLine;
            weight += weightLine;
        }
        a += ";";
        weight += ";";
        dataLines.add(a);
        dataLines.add(weight);


        //S
        String s = "set S := ";
        for(Node source : request.getSources()){
            s += " '" + source.getId() + "'";
        }
        s += ";";
        dataLines.add(s);

        //D
        String d = "set D := ";
        for(Node dest : request.getSources()){
            d += " '" + dest.getId() + "'";
        }
        d += ";";
        dataLines.add(d);

        String iMax = "param I_max := 10;";
        dataLines.add(iMax);

        String cTotal = "param c_total := " + request.getConnections().getNumConnections() + ";";
        dataLines.add(cTotal);

        // Pair params
        String cMin = "param c_min_sd := ";
        String cMax = "param c_max_sd := ";
        String numGroups = "param NumGroups := ";
        Map<SourceDestPair, Integer> pairMinMap = request.getConnections().getPairMinConnectionsMap();
        Map<SourceDestPair, Integer> pairMaxMap = request.getConnections().getPairMaxConnectionsMap();
        Map<SourceDestPair, List<List<Failure>>> pairFailGroupsMap = request.getFailures().getPairFailureGroupsMap();
        List<String> fgLines = new ArrayList<>();
        for(SourceDestPair pair : pairMinMap.keySet()){
            if(!pair.getSrc().equals(pair.getDst())) {
                Integer min = pairMinMap.get(pair);
                Integer max = pairMaxMap.get(pair);
                List<List<Failure>> failureGroups = pairFailGroupsMap.get(pair);
                cMin += " '" + pair.getSrc().getId() + "' '" + pair.getDst().getId() + "' " + min;
                cMax += " '" + pair.getSrc().getId() + "' '" + pair.getDst().getId() + "' " + max;
                numGroups += " '" + pair.getSrc().getId() + "' '" + pair.getDst().getId() + "' " + failureGroups.size();
                for (int groupIndex = 0; groupIndex < failureGroups.size(); groupIndex++) {
                    String fg = "set FG['" + pair.getSrc().getId() + "','" + pair.getDst().getId() + "'," + (groupIndex + 1) + "] :=";
                    List<Failure> group = failureGroups.get(groupIndex);
                    for (Failure fail : group) {
                        String failString = fail.getLink() != null ? "('" + fail.getLink().getOrigin().getId() + "','" + fail.getLink().getTarget() + "')"
                                : "('" + fail.getNode().getId() + "','" + fail.getNode().getId() + "')";
                        fg += " " + failString;
                    }
                    fg += ";";
                    fgLines.add(fg);
                }
            }
        }
        cMin += ";";
        cMax += ";";
        numGroups += ";";
        dataLines.add(cMin);
        dataLines.add(cMax);
        dataLines.add(numGroups);
        dataLines.addAll(fgLines);



        return dataLines;
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
        if(problemClass.equals(ProblemClass.Flow)){
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

            assignFailureGroups(ampl, pairList, request);
        }
        if(problemClass.equals(ProblemClass.Endpoint)){
            List<Node> sourceList = new ArrayList<>(request.getSources());
            List<Node> destinationList = new ArrayList<>(request.getDestinations());
            assignMemberParams(ampl, request, sourceList, true);
            assignMemberParams(ampl, request, destinationList, false);
            assignFailureGroups(ampl, sourceList, request, true);
            assignFailureGroups(ampl, destinationList, request, false);
        }

    }

    private void assignMemberParams(AMPL ampl, Request request, List<Node> memberNodes, boolean isSourceSet){
        String setName = isSourceSet ? "S" : "D";
        String cMinName = isSourceSet ? "c_min_s" : "c_min_d";
        String cMaxName = isSourceSet ? "c_max_s" : "c_max_d";
        String numGroupsName = isSourceSet ? "NumGroups_s" : "NumGroups_d";
        DataFrame params = isSourceSet ?
                new DataFrame(1, setName, "c_min_s", "c_max_s", "NumGroups_s") :
                new DataFrame(1, setName, "c_min_d", "c_max_d", "NumGroups_d");
        Object[] members = memberNodes.stream().map(Node::getId).toArray();

        double[] cMin = new double[members.length];
        double[] cMax = new double[members.length];
        double[] numGroups = new double[members.length];

        Map<Node, Integer> minConnMap = isSourceSet ?
                request.getConnections().getSrcMinConnectionsMap() :
                request.getConnections().getDstMinConnectionsMap();
        Map<Node, Integer> maxConnMap = isSourceSet ?
                request.getConnections().getSrcMaxConnectionsMap() :
                request.getConnections().getDstMaxConnectionsMap();
        Map<Node, Set<Failure>> failureSetMap = isSourceSet ?
                request.getFailures().getSrcFailuresMap() :
                request.getFailures().getDstFailuresMap();
        Map<Node, Integer> numFailsMap = isSourceSet ?
                request.getNumFailsAllowed().getSrcNumFailsAllowedMap() :
                request.getNumFailsAllowed().getDstNumFailsAllowedMap();

        for(int index = 0; index < memberNodes.size(); index++){
            Node node = memberNodes.get(index);
            cMin[index] = minConnMap.get(node);
            cMax[index] = maxConnMap.get(node);

            Set<Failure> failures = failureSetMap.getOrDefault(node, request.getFailures().getFailureSet());
            Integer numFails = numFailsMap.getOrDefault(node, request.getNumFailsAllowed().getTotalNumFailsAllowed());
            // Calculate the number of totalNumFailsAllowed-sized combinations of the failure set
            double divisor = CombinatoricsUtils.factorial(numFails) * CombinatoricsUtils.factorial(failures.size() - numFails);
            numGroups[index] = CombinatoricsUtils.factorial(failures.size()) / divisor;
        }

        params.setColumn(setName, members);
        params.setColumn(cMinName, cMin);
        params.setColumn(cMaxName, cMax);
        params.setColumn(numGroupsName, numGroups);
        ampl.setData(params, setName);
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
        Map<Object[], Object[]>  failureGroups = convertFailureGroups(request.getFailures().getFailureGroups(), new ArrayList<>());
        for(Object[] fgTuple : failureGroups.keySet()){
            fg.get(fgTuple).setValues(failureGroups.get(fgTuple));
        }
    }

    // Must be done after the numGroups has been assigned to the model
    private void assignFailureGroups(AMPL ampl, List<SourceDestPair> pairList, Request request){
        Map<SourceDestPair, List<List<Failure>>> failureGroupsMap = request.getFailures().getPairFailureGroupsMap();

        // Failure groups
        com.ampl.Set fg = ampl.getSet("FG");
        //Collection<SetInstance> fgSets = fg.getInstances();
        //ampl.eval("data;");
        //DataFrame fg = new DataFrame(3, "S", "D", "GroupIndices", "FG");

        // set FG[1,11,1] := (5,5);
        // set FG['Boulder','Pittsburgh',3.0] := ('Champaign','Champaign') ;
        for(SourceDestPair pair : pairList){
            // Find all k-size subsets of this failure set
            List<Object> tupleArgs = new ArrayList<>();
            tupleArgs.add(pair.getSrc().getId());
            tupleArgs.add(pair.getDst().getId());
            List<List<Failure>> failureGroupList = failureGroupsMap.get(pair);
            Map<Object[], Object[]>  failureGroups = convertFailureGroups(failureGroupList, tupleArgs);
            for(Object[] tuple : failureGroups.keySet()){
                Object[]group = failureGroups.get(tuple);
                //fg.addRow(tuple[0], tuple[1], tuple[2], groupTuples[0]);
                fg.get(tuple).setValues(group);
                //System.out.println(fgFrame);
                //fg.get(fgTriplet.get(0), fgTriplet.get(1), fgTriplet.get(2)).setValues(failureGroups.get(fgTriplet));
            }
        }
        //ampl.setData(fg);
    }

    private void assignFailureGroups(AMPL ampl, List<Node> members, Request request, boolean isSourceSet){
        Map<Node, List<List<Failure>>> failureGroupsMap = isSourceSet ?
                request.getFailures().getSrcFailureGroupsMap() : request.getFailures().getDstFailureGroupsMap();

        String fgSetName = isSourceSet ? "FG_s" : "FG_d";
        com.ampl.Set fg = ampl.getSet(fgSetName);
        for(Node member : members){
            List<List<Failure>> failureGroupList = failureGroupsMap.getOrDefault(member, request.getFailures().getFailureGroups());
            List<Object> tupleArgs = new ArrayList<>();
            tupleArgs.add(member.getId());
            Map<Object[], Object[]>  failureGroups = convertFailureGroups(failureGroupList, tupleArgs);
            for(Object[] fgTuple : failureGroups.keySet()){
                Object[] group = failureGroups.get(fgTuple);
                fg.get(fgTuple).setValues(group);
            }
        }
    }

    private Object[] createFailureSetArray(Collection<Failure> failures) {
        Object[] failureSet = new Object[failures.size() * 2];

        int index = 0;
        for(Failure failure: failures){
            Object[] failureTuple = failure.getLink() != null ?
                    new Object[]{failure.getLink().getOrigin().getId(), failure.getLink().getTarget().getId()}:
                    new Object[]{failure.getNode().getId(), failure.getNode().getId()};
            failureSet[index] = failureTuple[0];
            index++;
            failureSet[index] = failureTuple[1];
            index++;
        }
        return failureSet;
    }

    private Map<Object[], Object[]> convertFailureGroups(List<List<Failure>> failureGroups, List<Object> tupleArgs){
        Map<Object[], Object[]> failureGroupMap = new HashMap<>();
        for(int index = 0; index < failureGroups.size(); index++){
            List<Failure> failureGroup = failureGroups.get(index);
            Object[] failureSet = createFailureSetArray(failureGroup);
            tupleArgs.add(index+1);
            failureGroupMap.put(tupleArgs.toArray(), failureSet);
            tupleArgs.remove(tupleArgs.size()-1);
        }
        return failureGroupMap;
    }


    // AMPL output -> Java translation

    private Map<SourceDestPair,Map<String,Path>> translateFlowsIntoPaths(DataFrame linkFlows, Set<SourceDestPair> pairs, Topology topo) {
        Map<SourceDestPair, Map<String, Path>> pathMap = pairs.stream()
                .collect(Collectors.toMap(p -> p, p -> new HashMap<>()));
        Map<String, Link> linkIdMap = topo.getLinkIdMap();
        Map<String, Node> nodeIdMap = topo.getNodeIdMap();
        String[] sources = linkFlows.getColumnAsStrings("index0");
        String[] destinations = linkFlows.getColumnAsStrings("index1");
        double[] pathIds = linkFlows.getColumnAsDoubles("index2");
        String[] origins = linkFlows.getColumnAsStrings("index3");
        String[] targets = linkFlows.getColumnAsStrings("index4");
        for(int index = 0; index < sources.length; index++){
            String src = sources[index];
            String dst = destinations[index];
            String pathId = Double.toString(pathIds[index]);
            String origin = origins[index];
            String target = targets[index];

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
        sortedNodes.add(currLink.getOrigin());
        nodeIds.add(currLink.getOrigin().getId());

        // While the next node has an outgoing link
        while(outgoingLinks.containsKey(currLink.getTarget())){
            sortedLinks.add(currLink);
            linkIds.add(currLink.getId());

            currLink = outgoingLinks.get(currLink.getTarget());

            sortedNodes.add(currLink.getOrigin());
            nodeIds.add(currLink.getOrigin().getId());

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


}
