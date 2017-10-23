package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum TrafficCombinationType {
    Source("source"),
    Destination("dest"),
    Both("both"),
    None("none");

    private String code;

    TrafficCombinationType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, TrafficCombinationType> lookup = new HashMap<>();

    static {
        for (TrafficCombinationType pc : EnumSet.allOf(TrafficCombinationType.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<TrafficCombinationType> get(String code) {
        String lookupCode = code.toLowerCase();
        if (lookup.containsKey(lookupCode)) {
            return Optional.of(lookup.get(lookupCode));
        } else {
            return Optional.empty();
        }
    }
}
