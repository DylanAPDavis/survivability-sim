package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Algorithm {
    ServiceILP("ServiceILP"),
    PartialBhandari("PartialBhandari");

    private String code;

    Algorithm(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, Algorithm> lookup = new HashMap<>();

    static {
        for (Algorithm pc : EnumSet.allOf(Algorithm.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<Algorithm> get(String code) {
        if (lookup.containsKey(code)) {
            return Optional.of(lookup.get(code));
        } else {
            return Optional.empty();
        }
    }
}
