package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.*;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EnumGenerationService {

    public Algorithm getAlgorithm(String alg){
        return Algorithm.get(alg.toLowerCase()).orElse(Algorithm.MinimumCostPath);
    }

    public TrafficCombinationType getTrafficCombinationType(String type){
        return TrafficCombinationType.get(type.toLowerCase()).orElse(TrafficCombinationType.None);
    }

    public RoutingType getRoutingType(String type){
        return RoutingType.get(type).orElse(RoutingType.Default);
    }

    public FailureScenario getFailureScenario(String scenario){
        return FailureScenario.get(scenario.toLowerCase()).orElse(FailureScenario.Default);
    }

    public FailureClass getFailureClass(String fClass){
        return FailureClass.get(fClass.toLowerCase()).orElse(FailureClass.Both);
    }

    public ProblemClass getProblemClass(String problemClass) {
        return ProblemClass.get(problemClass.toLowerCase()).orElse(ProblemClass.Combined);
    }

    public Objective getObjective(String objective) {
        return Objective.get(objective.toLowerCase()).orElse(Objective.TotalCost);
    }

    public MemberFailureType getMemberFailureType(String memberFailureType){
        return MemberFailureType.get(memberFailureType.toLowerCase()).orElse(MemberFailureType.Prevent);
    }

    public SourceSubsetDestType getSourceSubsetDestType(String sourceSubsetDestType){
        return SourceSubsetDestType.get(sourceSubsetDestType.toLowerCase()).orElse(SourceSubsetDestType.None);
    }
}
