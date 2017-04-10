package netlab.submission.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum OverlapType {
    None("None"),
    Partial("Partial"),
    Total("Total");

    private String code;

    OverlapType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, OverlapType> lookup = new HashMap<>();

    static {
        for (OverlapType pc : EnumSet.allOf(OverlapType.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<OverlapType> get(String code) {
        if (lookup.containsKey(code)) {
            return Optional.of(lookup.get(code));
        } else {
            return Optional.empty();
        }
    }
}
