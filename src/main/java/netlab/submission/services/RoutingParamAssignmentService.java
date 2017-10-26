package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.request.RequestParameters;
import netlab.submission.request.SimulationParameters;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class RoutingParamAssignmentService {

    public void provideRoutingValues(RequestParameters params) {
        Set<String> sources = params.getSources();
        Set<String> destinations = params.getDestinations();
        List<List<String>> pairs = makePairs(params.getSources(), params.getDestinations());
        Integer numConnections = 1;
        Map<List<String>, Integer> pairNumConnectionsMap = new HashMap<>();
        Map<List<String>, Integer> pairMinNumConnectionsMap = new HashMap<>();
        Map<List<String>, Integer> pairMaxNumConnectionsMap = new HashMap<>();

        Map<String, Integer> srcNumConnectionsMap = new HashMap<>();
        Map<String, Integer> srcMinNumConnectionsMap = new HashMap<>();
        Map<String, Integer> srcMaxNumConnectionsMap = new HashMap<>();

        Map<String, Integer> destNumConnectionsMap = new HashMap<>();
        Map<String, Integer> destMinNumConnectionsMap = new HashMap<>();
        Map<String, Integer> destMaxNumConnectionsMap = new HashMap<>();

        Integer useMinS = params.getUseMinS();
        Integer useMaxS = params.getUseMaxS();
        Integer useMinD = params.getUseMinD();
        Integer useMaxD = params.getUseMaxD();
        switch(params.getRoutingType().toLowerCase()){
            case "unicast":
                // 1 connection, 1 conns per pair, 1 conns per src, 1conns per dest, 1 srcs connected, 1 dests connected
                pairMinNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
                pairMaxNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
                srcMinNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> 1));
                srcMaxNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> 1));
                destMinNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 1));
                destMaxNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 1));
                useMinS = 1;
                useMaxS = 1;
                useMinD = 1;
                useMaxD = 1;
                break;
            case "anycast":
                // 1 connections, 0/1 conns per pair, 1 conns per src, 0/1 conns per dest, 1 srcs connected, 1 dst connected
                pairMinNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
                pairMaxNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
                srcMinNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> 1));
                srcMaxNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> 1));
                destMinNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 0));
                destMaxNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 1));
                useMinS = 1;
                useMaxS = 1;
                useMinD = 1;
                useMaxD = 1;
                break;
            case "manycast":
                // useMinD connections, 0/1 per pair, minD/maxD per src, 0/1 per dest, 1 src connected, useMinD/useMaxD dsts connected
                pairMinNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
                pairMaxNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
                srcMinNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> 1));
                srcMaxNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> destinations.size()));
                destMinNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 0));
                destMaxNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 1));
                useMinS = 1;
                useMaxS = 1;
                useMinD = useMinD == null || useMinD < 1 ? destinations.size() : useMinD;
                useMaxD = useMaxD == null || useMaxD > destinations.size() ? destinations.size() : useMaxD;
                numConnections = useMinD;
                break;
            case "multicast":
                // |D| connections, 1 per pair, |D| per src, 1 per dst, 1 src connected, |D| dsts connected
                pairMinNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
                pairMaxNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
                srcMinNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> destinations.size()));
                srcMaxNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> destinations.size()));
                destMinNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 1));
                destMaxNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 1));
                useMinS = 1;
                useMaxS = 1;
                useMinD = destinations.size();
                useMaxD = destinations.size();
                numConnections = destinations.size();
                break;
            case "manytoone":
                // useMinS connections, 0/1 per pair, 0/1 per src, useMinS/useMaxS per dst, useMinS/useMaxS src connected, 1 dsts connected
                pairMinNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
                pairMaxNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
                srcMinNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> 0));
                srcMaxNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> 1));
                destMinNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 1));
                destMaxNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> sources.size()));
                useMinS = useMinS == null || useMinS < 1 ? sources.size() : useMinS;
                useMaxS = useMaxS == null || useMaxS > sources.size() ? sources.size() : useMaxS;
                useMinD = 1;
                useMaxD = 1;
                numConnections = sources.size();
                break;
            case "manytomany":
                // max(useMinS, useMinD) connections, 0/1 per pair, 0/1 per src, 0/1 per dst, useMinS/useMaxS src connected, useMinD/useMaxD dsts connected
                pairMinNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 0));
                pairMaxNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
                srcMinNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> 0));
                srcMaxNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> 1));
                destMinNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 0));
                destMaxNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 1));
                useMinS = useMinS == null || useMinS < 1 ? sources.size() : useMinS;
                useMaxS = useMaxS == null || useMaxS > sources.size() ? sources.size() : useMaxS;
                useMinD = useMinD == null || useMinD < 1 ? destinations.size() : useMinD;
                useMaxD = useMaxD == null || useMaxD > destinations.size() ? destinations.size() : useMaxD;
                numConnections = Math.max(useMinS, useMinD);
                break;
            case "broadcast":
                // |pairs| connections, 1/1 per pair, 0/|D| per src, 0/|S| per dst, |S| src connected, |D| dsts connected
                pairMinNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
                pairMaxNumConnectionsMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> 1));
                srcMinNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> 1));
                srcMaxNumConnectionsMap = sources.stream().collect(Collectors.toMap(p -> p, p -> destinations.size()));
                destMinNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> 1));
                destMaxNumConnectionsMap = destinations.stream().collect(Collectors.toMap(p -> p, p -> sources.size()));
                useMinS = sources.size();
                useMaxS = sources.size();
                useMinD = destinations.size();
                useMaxD = destinations.size();
                numConnections = pairs.size();
                break;
        }

        params.setNumConnections(numConnections);
        params.setPairNumConnectionsMap(pairNumConnectionsMap);
        params.setPairMinNumConnectionsMap(pairMinNumConnectionsMap);
        params.setPairMaxNumConnectionsMap(pairMaxNumConnectionsMap);
        params.setSourceNumConnectionsMap(srcNumConnectionsMap);
        params.setSourceMinNumConnectionsMap(srcMinNumConnectionsMap);
        params.setSourceMaxNumConnectionsMap(srcMaxNumConnectionsMap);
        params.setDestNumConnectionsMap(destNumConnectionsMap);
        params.setDestMinNumConnectionsMap(destMinNumConnectionsMap);
        params.setDestMaxNumConnectionsMap(destMaxNumConnectionsMap);
        params.setUseMinS(useMinS);
        params.setUseMaxS(useMaxS);
        params.setUseMinD(useMinD);
        params.setUseMaxD(useMaxD);
    }

    private List<List<String>> makePairs(Set<String> sources, Set<String> destinations) {
        List<List<String>> pairs = new ArrayList<>();
        for(String src : sources){
            for(String dst : destinations){
                if(!src.equals(dst)){
                    List<String> pair = Arrays.asList(src, dst);
                    pairs.add(pair);
                }
            }
        }
        return pairs;
    }


    public void provideDefaultRoutingValues(SimulationParameters params) {
        Integer numS = params.getNumSources();
        Integer numD = params.getNumDestinations();

        // C - total number of connections
        Integer minConnections = 1;
        // Establish min and max connections between each SD pair
        Integer minPairConnections = 0;
        Integer maxPairConnections = 1;
        // Establish min and max connections from each source and to each destination
        Integer minSrcConnections = 0;
        Integer maxSrcConnections = 1;
        Integer minDstConnections = 0;
        Integer maxDstConnections = 1;

        Integer useMinS = params.getUseMinS();
        Integer useMaxS = params.getUseMaxS();
        Integer useMinD = params.getUseMinD();
        Integer useMaxD = params.getUseMaxD();

        switch(params.getRoutingType().toLowerCase()){
            case "unicast":
                // 1 connection, 1 conns per pair, 1 conns per src, 1conns per dest, 1 srcs connected, 1 dests connected
                minPairConnections = 1;
                maxPairConnections = 1;
                minSrcConnections = 1;
                maxSrcConnections = 1;
                minDstConnections = 1;
                maxDstConnections = 1;
                useMinS = 1;
                useMaxS = 1;
                useMinD = 1;
                useMaxD = 1;
                break;
            case "anycast":
                // 1 connections, 0/1 conns per pair, 1 conns per src, 0/1 conns per dest, 1 srcs connected, 1 dst connected
                minPairConnections = 0;
                maxPairConnections = 1;
                minSrcConnections = 1;
                maxSrcConnections = 1;
                minDstConnections = 0;
                maxDstConnections = 1;
                useMinS = 1;
                useMaxS = 1;
                useMinD = 1;
                useMaxD = 1;
                break;
            case "manycast":
                // useMinD connections, 0/1 per pair, minD/maxD per src, 0/1 per dest, 1 src connected, useMinD/useMaxD dsts connected
                minPairConnections = 0;
                maxPairConnections = 1;
                minSrcConnections = 1;
                maxSrcConnections = numD;
                minDstConnections = 0;
                maxDstConnections = 1;
                useMinS = 1;
                useMaxS = 1;
                useMinD = useMinD == null || useMinD < 1 ? numD : useMinD;
                useMaxD = useMaxD == null || useMaxD > numD ? numD : useMaxD;
                minConnections = useMinD;
                break;
            case "multicast":
                // |D| connections, 1 per pair, |D| per src, 1 per dst, 1 src connected, |D| dsts connected
                minPairConnections = 1;
                maxPairConnections = 1;
                minSrcConnections = numD;
                maxSrcConnections = numD;
                minDstConnections = 1;
                maxDstConnections = 1;
                useMinS = 1;
                useMaxS = 1;
                useMinD = numD;
                useMaxD = numD;
                minConnections = useMinD;
                break;
            case "manytoone":
                // useMinS connections, 0/1 per pair, 0/1 per src, useMinS/useMaxS per dst, useMinS/useMaxS src connected, 1 dsts connected
                minPairConnections = 0;
                maxPairConnections = 1;
                minSrcConnections = 0;
                maxSrcConnections = 1;
                minDstConnections = 1;
                maxDstConnections = numS;
                useMinS = useMinS == null || useMinS < 1 ? numS : useMinS;
                useMaxS = useMaxS == null || useMaxS > numS ? numS : useMaxS;
                useMinD = 1;
                useMaxD = 1;
                minConnections = useMinS;
                break;
            case "manytomany":
                // max(useMinS, useMinD) connections, 0/1 per pair, 0/1 per src, 0/1 per dst, useMinS/useMaxS src connected, useMinD/useMaxD dsts connected
                minPairConnections = 0;
                maxPairConnections = 1;
                minSrcConnections = 0;
                maxSrcConnections = 1;
                minDstConnections = 0;
                maxDstConnections = 1;
                useMinS = useMinS == null || useMinS < 1 ? numS : useMinS;
                useMaxS = useMaxS == null || useMaxS > numS ? numS : useMaxS;
                useMinD = useMinD == null || useMinD < 1 ? numD : useMinD;
                useMaxD = useMaxD == null || useMaxD > numD ? numD : useMaxD;
                minConnections = Math.max(useMinS, useMinD);
                break;
            case "broadcast":
                // |pairs| connections, 1/1 per pair, 0/|D| per src, 0/|S| per dst, |S| src connected, |D| dsts connected
                minPairConnections = 1;
                maxPairConnections = 1;
                minSrcConnections = 1;
                maxSrcConnections = numD;
                minDstConnections = 1;
                maxDstConnections = numS;
                useMinS = numS;
                useMaxS = numS;
                useMinD = numD;
                useMaxD = numD;
                Integer numSourcesAlsoDest = (int)Math.ceil(numS * params.getPercentSrcAlsoDest());
                Integer numJustSources = numS - numSourcesAlsoDest;
                // For each source that is also a destination, connect it to all other destinations
                // For each source that is not also a destination, connect it to all destinations
                minConnections = numSourcesAlsoDest * (numD-1) + numJustSources * numD;
                break;
        }

        params.setMinConnections(minConnections);
        params.setMinPairConnections(minPairConnections);
        params.setMaxPairConnections(maxPairConnections);
        params.setMinSrcConnections(minSrcConnections);
        params.setMaxSrcConnections(maxSrcConnections);
        params.setMinDstConnections(minDstConnections);
        params.setMaxDstConnections(maxDstConnections);
        params.setUseMinS(useMinS);
        params.setUseMaxS(useMaxS);
        params.setUseMinD(useMinD);
        params.setUseMaxD(useMaxD);
    }
}
