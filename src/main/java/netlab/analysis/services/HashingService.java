package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class HashingService {

    public String hash(Object... args){
        List<String> strings = new ArrayList<>();
        for(Object arg : args){
            strings.add(String.valueOf(arg).replace(" ", "").toLowerCase());
        }
        return String.join("_", strings);
    }

    public String[] unhash(String hashString){
        return hashString.split("_");
    }
}
