package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.SourceSubsetDestType;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.SourceDestPair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SelectionService {

    public Set<Node> pickSources(Set<Node> nodes, Integer numSources, Random rng) {
        return new HashSet<>(chooseRandomSubsetNodes(nodes, numSources, rng));
    }

    public Set<Node> pickDestinations(Set<Node> nodes, Integer numDestinations, Random rng, SourceSubsetDestType sourceSubsetDestType,
                                      Set<Node> sources) {
        Set<Node> remainingNodes = new HashSet<>(nodes);
        Set<Node> chosenNodes = new HashSet<>();


        // If any sources also must be destinations
        if(!sourceSubsetDestType.equals(SourceSubsetDestType.None)){
            Double percentSrcAlsoDest = translateSubsetTypeToPercentage(sourceSubsetDestType);
            chosenNodes.addAll(choosePercentageSubsetNodes(sources, percentSrcAlsoDest, rng));
        }
        // If you still haven't picked enough yet
        if(chosenNodes.size() < numDestinations){
            remainingNodes.removeAll(sources);
            Set<Node> remainingChoices = chooseRandomSubsetNodes(remainingNodes, numDestinations-chosenNodes.size(), rng);
            chosenNodes.addAll(remainingChoices);
        }
        return chosenNodes;
    }

    public Set<Node> choosePercentageSubsetNodes(Set<Node> options, Double percentage, Random rng){
        int numToChoose = numFromPercentage(options.size(), percentage);
        return chooseRandomSubsetNodes(options, numToChoose, rng);
    }

    public double translateSubsetTypeToPercentage(SourceSubsetDestType sourceSubsetDestType){
        switch(sourceSubsetDestType){
            case All:
                return 1.0;
            case Half:
                return 0.5;
            case None:
            default:
                return 0.0;
        }
    }

    private int numFromPercentage(int numOptions, double percentage){
        return (int) Math.ceil(percentage * numOptions);
    }

    public Set<String> choosenRandomSubsetStrings(Set<String> options, Integer numChoices, Random rng){
        if(numChoices == 0){
            return new HashSet<>();
        }
        if(numChoices > options.size()){
            return options;
        }
        List<String> choices = new ArrayList<>(options);
        Collections.shuffle(choices, rng);
        return new HashSet<>(choices.subList(0, numChoices));
    }

    public Set<Node> chooseRandomSubsetNodes(Set<Node> options, Integer numChoices, Random rng) {
        if(numChoices == 0){
            return new HashSet<>();
        }
        if(numChoices > options.size()){
            return options;
        }
        List<Node> choices = new ArrayList<>(options);
        Collections.shuffle(choices, rng);
        return new HashSet<>(choices.subList(0, numChoices));
    }

    public Set<Link> chooseRandomSubsetLinks(Set<Link> options, Integer numChoices, Random rng){
        if(numChoices == 0){
            return new HashSet<>();
        }
        List<Link> choices = new ArrayList<>(options);
        Collections.shuffle(choices, rng);
        return new HashSet<>(choices.subList(0, numChoices));
    }

    public Integer randomInt(Integer min, Integer max, Random rng){
        return rng.nextInt((max - min) + 1) + min;
    }

    public Map<Node, Integer> makeNodeIntegerMap(Set<Node> members, Map<String, Integer> memberConnMap, Integer defaultValue){
        Map<Node, Integer> memberMap = members.stream().collect(Collectors.toMap(m -> m, m -> defaultValue));
        for(String nodeName : memberConnMap.keySet()){
            Node node = Node.builder().id(nodeName).build();
            memberMap.put(node, memberConnMap.get(nodeName));
        }
        return memberMap;
    }

    public Map<SourceDestPair, Integer> makePairIntegerMap(Set<SourceDestPair> pairs, Map<List<String>, Integer> pairConnMap,
                                                           Integer defaultValue){
        Map<SourceDestPair, Integer> pairMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> defaultValue));
        for(List<String> pairList : pairConnMap.keySet()){
            SourceDestPair pair = SourceDestPair.builder()
                    .src(Node.builder().id(pairList.get(0)).build())
                    .dst(Node.builder().id(pairList.get(1)).build())
                    .build();
            pairMap.put(pair, pairConnMap.get(pairList));
        }
        return pairMap;
    }
}
