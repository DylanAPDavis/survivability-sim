package netlab.processing.ampl;


import com.ampl.*;
import com.fasterxml.jackson.databind.deser.impl.CreatorCandidate;
import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;
import netlab.submission.enums.RoutingType;
import netlab.submission.request.Details;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
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

    public Details solve(Request request, Topology topology){
        Map<SourceDestPair, Map<String, Path>> paths = new HashMap<>();
        Environment env = new Environment(System.getProperty("user.dir") + "/linear-programs/ampl/");
        AMPL ampl = new AMPL(env);
        double duration = 0.0;
        Details details = request.getDetails();
        try {
            ampl = assignValues(request, topology, ampl);
            long startTime = System.nanoTime();
            ampl.solve();
            long endTime = System.nanoTime();
            duration = (endTime - startTime)/1e9;
            log.info("Solution took: " + duration + " seconds");
            com.ampl.Objective obj = ampl.getObjective(request.getObjective().getCode());
            String result = obj.result();
            String message = obj.message();

            if(result.toLowerCase().contains("solved") || message.toLowerCase().contains("objective")){
                details.setIsFeasible(true);
                DataFrame flows = ampl.getData("L");
                paths = translateFlowsIntoPaths(flows, details.getPairs(), topology);
            }
            else{
                paths = details.getPairs().stream().collect(Collectors.toMap(p -> p, p -> new HashMap<>()));
            }
            if(result.contains("limit")){
                request.setTimedOut(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            ampl.close();
        }
        details.setChosenPaths(paths);
        details.setRunningTimeSeconds(duration);
        return details;
    }

    private AMPL assignValues(Request request, Topology topology, AMPL ampl) throws IOException{
        /*if(problemClass.equals(ProblemClass.Flex)){
            ampl.read(modelDirectory + "/flex.mod");
        }
        if(problemClass.equals(ProblemClass.Endpoint)){
            ampl.read(modelDirectory + "/endpoint.mod");
        }
        if(problemClass.equals(ProblemClass.Flow)){
            ampl.read(modelDirectory + "/flow.mod");
        }
        if(problemClass.equals(ProblemClass.EndpointSharedF)){
            ampl.read(modelDirectory + "/endpointSharedF.mod");
        }
        if(problemClass.equals(ProblemClass.FlowSharedF)){
            ampl.read(modelDirectory + "/flowSharedF.mod");
        }
        if(problemClass.equals(ProblemClass.Combined)){
            ampl.read(modelDirectory + "/combined.mod");
        }*/
        switch(request.getRoutingType()){
            case Unicast:
                ampl.read(modelDirectory + "/unicast.mod");
                break;
            case Anycast:
            case Multicast:
            case Manycast:
                ampl.read(modelDirectory + "/manycast.mod");
                break;
            case ManyToOne:
                ampl.read(modelDirectory + "/manyToOne.mod");
                break;
            default:
                ampl.read(modelDirectory + "/combined.mod");
        }

        ampl.eval("objective " + request.getObjective().getCode()  + ";");
        ampl.setIntOption("omit_zero_rows", 1);
        ampl.setOption("solver", "gurobi");
        ampl.eval("option gurobi_options \'threads " + request.getNumThreads() + "\';");
        ampl.eval("option gurobi_options $gurobi_options\'timelim " + request.getCutoffTimeSeconds() + "\';");

        List<String> dataLines = createDataLines(request, topology);
        java.nio.file.Path file = Paths.get(request.getId() + ".dat");
        try {
            // Create the empty file with default permissions, etc.
            Files.deleteIfExists(file);
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

        //ampl.setIntOption("times", 1);
        //ampl.setIntOption("gentimes", 1);
        //ampl.setIntOption("show_stats", 1);

        return ampl;
    }

    private List<String> createDataLines(Request request, Topology topology) {
        Details details = request.getDetails();
        ProblemClass problemClass = request.getProblemClass();
        boolean ignoreF = request.isIgnoreFailures();
        List<String> dataLines = new ArrayList<>();
        // Topology
        dataLines.addAll(createTopologyLines(topology));

        RoutingType routingType = request.getRoutingType();

        switch(routingType){
            case Unicast:
                dataLines.add(createSingleNodeParamLine(details.getSources(), "s"));
                dataLines.add(createSingleNodeParamLine(details.getDestinations(), "d"));
                break;
            case Anycast:
            case Multicast:
            case Manycast:
                dataLines.add(createSingleNodeParamLine(details.getSources(), "s"));
                break;
            case ManyToOne:
                dataLines.add(createSingleNodeParamLine(details.getDestinations(), "d"));
                break;
        }

        dataLines.add(createNodeSetLine(details.getSources(), "S"));
        dataLines.add(createNodeSetLine(details.getDestinations(), "D"));

        // I_max
        Integer minFailures = Math.min(details.getNumFailureEvents().getTotalNumFailureEvents(), details.getFailures().getFailureSetSize());
        int iMaxNum = details.getConnections().getNumConnections() * (minFailures + 1);
        String iMax = String.format("param I_max := %d;", iMaxNum);
        dataLines.add(iMax);

        // C_total
        String cTotal = "param c_total := " + details.getConnections().getNumConnections() + ";";
        dataLines.add(cTotal);

        // Reach min/max src/dest if not doing Unicast
        switch(routingType){
            case Unicast:
                break;
            case Anycast:
            case Multicast:
            case Manycast:
                dataLines.add("param useMinD := " + details.getConnections().getUseMinD() + ";");
                dataLines.add("param useMaxD := " + details.getConnections().getUseMaxD() + ";");
                break;
            case ManyToOne:
                dataLines.add("param useMinS := " + details.getConnections().getUseMinS() + ";");
                dataLines.add("param useMaxS := " + details.getConnections().getUseMaxS() + ";");
                break;
            default:
                dataLines.add("param useMinS := " + details.getConnections().getUseMinS() + ";");
                dataLines.add("param useMaxS := " + details.getConnections().getUseMaxS() + ";");
                dataLines.add("param useMinD := " + details.getConnections().getUseMinD() + ";");
                dataLines.add("param useMaxD := " + details.getConnections().getUseMaxD() + ";");
        }

        // Traffic Combination
        String combineSourceTraffic = "param combineSourceTraffic := ";
        String combineDestTraffic = "param combineDestTraffic := ";
        switch(request.getTrafficCombinationType()){
            case None:
                combineSourceTraffic += "0;";
                combineDestTraffic += "0;";
                break;
            case Source:
                combineSourceTraffic += "1;";
                combineDestTraffic += "0;";
                break;
            case Destination:
                combineSourceTraffic += "0;";
                combineDestTraffic += "1;";
                break;
            case Both:
                combineSourceTraffic += "1;";
                combineDestTraffic += "1;";
                break;
        }
        dataLines.add(combineSourceTraffic);
        dataLines.add(combineDestTraffic);

        // Flex/Endpoint/Flow level params
        if(problemClass.equals(ProblemClass.Flex)){
            dataLines.addAll(createFlexParamsLines(details, ignoreF));
        }
        if(problemClass.equals(ProblemClass.Endpoint) || problemClass.equals(ProblemClass.EndpointSharedF)){
            dataLines.addAll(createEndpointParamsLines(details, problemClass, ignoreF));
        }
        if(problemClass.equals(ProblemClass.Flow) || problemClass.equals(ProblemClass.FlowSharedF)){
            dataLines.addAll(createPairParamsLines(details, problemClass, ignoreF));
        }
        if(problemClass.equals(ProblemClass.Combined)){
            dataLines.addAll(createCombinedParamsLines(details, problemClass, routingType, ignoreF));
        }

        return dataLines;
    }


    private List<String> createFlexParamsLines(Details details, boolean ignoreFailures){
        List<String> flexLines = new ArrayList<>();
        List<List<Failure>> failureGroups = ignoreFailures ? Collections.singletonList(new ArrayList<>()) : details.getFailures().getFailureGroups();
        if(failureGroups.isEmpty()){
            failureGroups.add(new ArrayList<>());
        }
        String numGroups = "param NumGroups := " + failureGroups.size() + ";";
        flexLines.add(numGroups);
        flexLines.addAll(createFailureGroupLines(failureGroups, ProblemClass.Flex, null, null, false));
        if(!ignoreFailures) {
            String nfe = "param nfe := " + details.getNumFailureEvents().getTotalNumFailureEvents() + ";";
            flexLines.add(nfe);
            flexLines.add(createFailureSetLine(details.getFailures().getFailureSet()));
        }
        return flexLines;
    }


    private List<String> createEndpointParamsLines(Details details, ProblemClass problemClass, boolean ignoreFailures){
        List<String> endpointLines = new ArrayList<>();
        Map<Node, Integer> srcMinMap = details.getConnections().getSrcMinConnectionsMap();
        Map<Node, Integer> srcMaxMap = details.getConnections().getSrcMaxConnectionsMap();
        Map<Node, Integer> dstMinMap = details.getConnections().getDstMinConnectionsMap();
        Map<Node, Integer> dstMaxMap = details.getConnections().getDstMaxConnectionsMap();

        Map<Node, List<List<Failure>>> srcFailGroupsMap = details.getFailures().getSrcFailureGroupsMap();
        Map<Node, List<List<Failure>>> dstFailGroupsMap = details.getFailures().getDstFailureGroupsMap();
        List<List<Failure>> requestFailureGroups = ignoreFailures ? Collections.singletonList(new ArrayList<>()) : details.getFailures().getFailureGroups();

        Set<Node> sources = details.getSources();
        Set<Node> destinations = details.getDestinations();

        boolean printFailsGroupPerMember = problemClass.equals(ProblemClass.Endpoint);
        endpointLines.addAll(createParamsForMemberGroup(sources, srcMinMap, srcMaxMap, srcFailGroupsMap, true, printFailsGroupPerMember, ignoreFailures));
        endpointLines.addAll(createParamsForMemberGroup(destinations, dstMinMap, dstMaxMap, dstFailGroupsMap, false, printFailsGroupPerMember, ignoreFailures));
        // If you're solving the EndpointSharedF problem, just print one FG set and one NumGroups param
        if(problemClass.equals(ProblemClass.EndpointSharedF)){
            String numGroups = "param NumGroups := " + requestFailureGroups.size() + ";";
            List<String> fgLines = createFailureGroupLines(requestFailureGroups, ProblemClass.EndpointSharedF, null, null, false);
            endpointLines.add(numGroups);
            endpointLines.addAll(fgLines);
        }
        return endpointLines;
    }

    private Collection<? extends String> createParamsForMemberGroup(Set<Node> members, Map<Node, Integer> memberMinMap,
                                                                    Map<Node, Integer> memberMaxMap,
                                                                    Map<Node, List<List<Failure>>> memberFailGroupsMap,
                                                                    boolean areSources, boolean printFailsGroupPerMember,
                                                                    Boolean ignoreFailures) {
        List<String> memberLines = new ArrayList<>();
        String cMin = "param c_min_" + (areSources ? "s" : "d") + " := ";
        String cMax = "param c_max_" + (areSources ? "s" : "d") + " := ";
        String numGroups = "param NumGroups_" + (areSources ? "s" : "d") + " := ";
        List<String> fgLines = new ArrayList<>();
        for(Node member : members){
            Integer min = memberMinMap.get(member);
            Integer max = memberMaxMap.get(member);
            cMin += " '" + member.getId() + "' " + min;
            cMax += " '" + member.getId() + "' " + max;
            if(printFailsGroupPerMember) {
                List<List<Failure>> failureGroups = ignoreFailures ? Collections.singletonList(new ArrayList<>())
                        : memberFailGroupsMap.getOrDefault(member, new ArrayList<>());
                if(failureGroups.isEmpty()){
                    failureGroups.add(new ArrayList<>());
                }               numGroups += " '" + member.getId() + "' " + failureGroups.size();
                fgLines.addAll(createFailureGroupLines(failureGroups, ProblemClass.Endpoint, null, member, areSources));
            }
        }
        cMin += ";";
        cMax += ";";
        numGroups += ";";
        memberLines.add(cMin);
        memberLines.add(cMax);
        // Only add NumGroups param and FG[member, i] if you're solving the regular Endpoint problem
        if(printFailsGroupPerMember) {
            memberLines.add(numGroups);
            memberLines.addAll(fgLines);
        }
        return memberLines;
    }

    private List<String> createPairParamsLines(Details details, ProblemClass problemClass, boolean ignoreFailures){
        List<String> pairLines = new ArrayList<>();
        String cMin = "param c_min_sd := ";
        String cMax = "param c_max_sd := ";
        String numGroups = "param NumGroups := ";
        Map<SourceDestPair, Integer> pairMinMap = details.getConnections().getPairMinConnectionsMap();
        Map<SourceDestPair, Integer> pairMaxMap = details.getConnections().getPairMaxConnectionsMap();
        Map<SourceDestPair, List<List<Failure>>> pairFailGroupsMap = details.getFailures().getPairFailureGroupsMap();
        List<String> fgLines = new ArrayList<>();
        if(problemClass.equals(ProblemClass.FlowSharedF)){
            List<List<Failure>> requestFailureGroups = ignoreFailures ? Collections.singletonList(new ArrayList<>())
                    : details.getFailures().getFailureGroups();
            if(requestFailureGroups.isEmpty()){
                requestFailureGroups.add(new ArrayList<>());
            }
            numGroups += requestFailureGroups.size();
            fgLines = createFailureGroupLines(requestFailureGroups, ProblemClass.FlowSharedF, null, null, false);
        }
        for(SourceDestPair pair : pairMinMap.keySet()){
            if(!pair.getSrc().equals(pair.getDst())) {
                Integer min = pairMinMap.get(pair);
                Integer max = pairMaxMap.get(pair);
                List<List<Failure>> failureGroups = ignoreFailures ? Collections.singletonList(new ArrayList<>())
                        : pairFailGroupsMap.getOrDefault(pair, new ArrayList<>());
                if(failureGroups.isEmpty()){
                    failureGroups.add(new ArrayList<>());
                }
                cMin += " '" + pair.getSrc().getId() + "' '" + pair.getDst().getId() + "' " + min;
                cMax += " '" + pair.getSrc().getId() + "' '" + pair.getDst().getId() + "' " + max;
                if(problemClass.equals(ProblemClass.Flow)) {
                    numGroups += " '" + pair.getSrc().getId() + "' '" + pair.getDst().getId() + "' " + failureGroups.size();
                    fgLines.addAll(createFailureGroupLines(failureGroups, ProblemClass.Flow, pair, null, false));
                }
            }
        }
        cMin += ";";
        cMax += ";";
        numGroups += ";";
        pairLines.add(cMin);
        pairLines.add(cMax);
        if(problemClass.equals(ProblemClass.FlowSharedF) || problemClass.equals(ProblemClass.Flow)) {
            pairLines.add(numGroups);
            pairLines.addAll(fgLines);
        }
        return pairLines;
    }

    /**
     * Create print lines for Combined model - takes C and F/FG params for Flex, adds in cMin and cMax params for both
     * endpoint and flow models.
     * @param details
     * @param problemClass
     * @return
     */
    private List<String> createCombinedParamsLines(Details details, ProblemClass problemClass, RoutingType routingType, boolean ignoreFailures) {
        List<String> lines = createFlexParamsLines(details, ignoreFailures);
        boolean combinedRouting = routingType.equals(RoutingType.ManyToMany) || routingType.equals(RoutingType.Broadcast) || routingType.equals(RoutingType.Default);
        if(combinedRouting) {
            lines.addAll(createEndpointParamsLines(details, problemClass, ignoreFailures));
            lines.addAll(createPairParamsLines(details, problemClass, ignoreFailures));
        }
        return lines;
    }

    private List<String> createFailureGroupLines(List<List<Failure>> failureGroups, ProblemClass problemClass, SourceDestPair pair, Node node, Boolean isSource) {
        List<String> fgLines = new ArrayList<>();
        for (int groupIndex = 0; groupIndex < failureGroups.size(); groupIndex++) {
            String fg = "";
            if(problemClass.equals(ProblemClass.Flex) || problemClass.equals(ProblemClass.EndpointSharedF) || problemClass.equals(ProblemClass.FlowSharedF)){
                fg = "set FG[" + (groupIndex + 1) + "] :=";
            }
            if(problemClass.equals(ProblemClass.Endpoint)){
                if(isSource){
                    fg = "set FG_s['" + node.getId() + "'," + (groupIndex+1) + "] :=";
                }
                else{
                    fg = "set FG_d['" + node.getId() + "'," + (groupIndex+1) + "] :=";
                }
            }
            if(problemClass.equals(ProblemClass.Flow)){
                fg = "set FG['" + pair.getSrc().getId() + "','" + pair.getDst().getId() + "'," + (groupIndex + 1) + "] :=";
            }
            List<Failure> group = failureGroups.get(groupIndex);
            for (Failure fail : group) {
                String failString = createFailureString(fail);
                fg += " " + failString;
            }
            fg += ";";
            fgLines.add(fg);
        }
        return fgLines;
    }

    private String createFailureString(Failure fail){
        return fail.getLink() != null ? "('" + fail.getLink().getOrigin().getId() + "','" + fail.getLink().getTarget().getId() + "')"
                : "('" + fail.getNode().getId() + "','" + fail.getNode().getId() + "')";
    }

    private String createFailureSetLine(Set<Failure> failureSet) {
        String set = "set F := ";
        int count = 0;
        for(Failure fail : failureSet){
            String failString = createFailureString(fail);
            set += failString;
            if(count < failureSet.size()-1) {
                set += ", ";
            }
            count++;
        }
        set += ";";
        return set;
    }

    private String createSingleNodeParamLine(Collection<Node> nodes, String paramName){
        return "param " + paramName + " := '" + nodes.iterator().next().getId() +"';";
    }

    private String createNodeSetLine(Collection<Node> nodes, String setName){
        String set = "set " + setName + " := ";
        for(Node node : nodes){
            set += " '" + node.getId() + "'";
        }
        set += ";";
        return set;
    }

    private List<String> createTopologyLines(Topology topology){
        List<String> topoLines = new ArrayList<>();
        topoLines.add(createNodeSetLine(topology.getNodes(), "V"));
        topoLines.addAll(createEdgeLines(topology, new ArrayList<>(topology.getNodes())));
        return topoLines;
    }

    private List<String> createEdgeLines(Topology topology, List<Node> nodes){
        List<String> topoLines = new ArrayList<>();
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
        topoLines.add(a);
        topoLines.add(weight);
        return topoLines;
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
        if(sources == null || destinations == null || pathIds == null || origins == null || targets == null){
            return pathMap;
        }
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
                thisPath.appendLink(link);
            }
            // New path
            else{
                List<Link> links = new ArrayList<>();
                links.add(link);
                Path newPath = new Path(links);
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
