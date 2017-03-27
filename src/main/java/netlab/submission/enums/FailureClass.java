package netlab.submission.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public enum FailureClass {
    NODE("Node"),
    LINK("Link"),
    BOTH("Both");

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
        if (lookup.containsKey(code)) {
            return Optional.of(lookup.get(code));
        } else {
            return Optional.empty();
        }
    }
}
