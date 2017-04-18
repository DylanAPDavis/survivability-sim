package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.submission.request.RequestSet;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnalysisService {
    public AnalyzedSet analyzeRequestSet(RequestSet requestSet) {

        return AnalyzedSet.builder().build();
    }
}
