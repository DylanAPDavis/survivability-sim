package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HashingService {

    public String hash(String... args){
        return String.join("_", args);
    }

    public String[] unhash(String hashString){
        return hashString.split("_");
    }
}
