package netlab.submission.services;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.*;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EnumGenerationService {

    public Algorithm getAlgorithm(String alg){
        return Algorithm.get(alg).orElse(Algorithm.ServiceILP);
    }

    public ProcessingType getProcessingType(String type){
        return ProcessingType.get(type).orElse(ProcessingType.Solo);
    }

    public FailureClass getFailureClass(String fClass){
        return FailureClass.get(fClass).orElse(FailureClass.Both);
    }

    public ProblemClass getProblemClass(String problemClass) {
        return ProblemClass.get(problemClass).orElse(ProblemClass.Flex);
    }

    public Objective getObjective(String objective) {
        return Objective.get(objective).orElse(Objective.LinksUsed);
    }
}
