package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum FailureScenario {
    Default("default"),
    AllLinks("alllinks"),
    AllNodes("allnodes"),
    Network("network"),
    Earthquake("earthquake"),
    Hurricane("hurricane");

    private String code;

    FailureScenario(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, FailureScenario> lookup = new HashMap<>();

    static {
        for (FailureScenario fs : EnumSet.allOf(FailureScenario.class)) {
            lookup.put(fs.getCode(), fs);
        }
    }

    public static Optional<FailureScenario> get(String code) {
        String lookupCode = code.toLowerCase();
        if (lookup.containsKey(lookupCode)) {
            return Optional.of(lookup.get(lookupCode));
        } else {
            return Optional.empty();
        }
    }
}
