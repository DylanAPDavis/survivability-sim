package netlab.submission.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public enum FailureClass {
    Node("node"),
    Link("link"),
    Both("both");

    private String code;

    FailureClass(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, FailureClass> lookup = new HashMap<>();

    static {
        for (FailureClass pc : EnumSet.allOf(FailureClass.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<FailureClass> get(String code) {
        String lookupCode = code.toLowerCase();
        if (lookup.containsKey(lookupCode)) {
            return Optional.of(lookup.get(lookupCode));
        } else {
            return Optional.empty();
        }
    }
}
