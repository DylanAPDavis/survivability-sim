package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum BatchType {
    WDM("WDM"),
    Grooming("Grooming");

    private String code;

    BatchType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, BatchType> lookup = new HashMap<>();

    static {
        for (BatchType pc : EnumSet.allOf(BatchType.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<BatchType> get(String code) {
        if (lookup.containsKey(code)) {
            return Optional.of(lookup.get(code));
        } else {
            return Optional.empty();
        }
    }
}
