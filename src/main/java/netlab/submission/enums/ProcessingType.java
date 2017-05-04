package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum ProcessingType {
    WDMBatch("WDMBatch"),
    GroomingBatch("GroomingBatch"),
    Solo("Solo");

    private String code;

    ProcessingType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, ProcessingType> lookup = new HashMap<>();

    static {
        for (ProcessingType pc : EnumSet.allOf(ProcessingType.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<ProcessingType> get(String code) {
        if (lookup.containsKey(code)) {
            return Optional.of(lookup.get(code));
        } else {
            return Optional.empty();
        }
    }
}
