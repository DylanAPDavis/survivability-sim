package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum ProblemClass {
    Flex("flex"),
    Endpoint("endpoint"),
    Flow("flow"),
    EndpointSharedF("endpointsharedf"),
    FlowSharedF("flowsharedf"),
    Combined("combined");

    private String code;

    ProblemClass(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, ProblemClass> lookup = new HashMap<>();

    static {
        for (ProblemClass pc : EnumSet.allOf(ProblemClass.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<ProblemClass> get(String code) {
        String lookupCode = code.toLowerCase();
        if (lookup.containsKey(lookupCode)) {
            return Optional.of(lookup.get(lookupCode));
        } else {
            return Optional.empty();
        }
    }
}
