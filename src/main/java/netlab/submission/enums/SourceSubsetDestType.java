package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum SourceSubsetDestType {
    All("all"),
    Half("half"),
    None("none");

    private String code;

    SourceSubsetDestType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, SourceSubsetDestType> lookup = new HashMap<>();

    static {
        for (SourceSubsetDestType mot : EnumSet.allOf(SourceSubsetDestType.class)) {
            lookup.put(mot.getCode(), mot);
        }
    }

    public static Optional<SourceSubsetDestType> get(String code) {
        String lookupCode = code.toLowerCase();
        if (lookup.containsKey(lookupCode)) {
            return Optional.of(lookup.get(lookupCode));
        } else {
            return Optional.empty();
        }
    }
}
