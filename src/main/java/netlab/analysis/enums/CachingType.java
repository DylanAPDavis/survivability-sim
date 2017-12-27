package netlab.analysis.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum CachingType {
    EntirePath("entirepath"),
    SourceAdjacent("sourceadjacent"),
    FailureAware("failureaware"),
    BranchingPoint("branchingpoint"),
    None("none")
    ;


    private String code;

    CachingType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, CachingType> lookup = new HashMap<>();

    static {
        for (CachingType pc : EnumSet.allOf(CachingType.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<CachingType> get(String code) {
        if (lookup.containsKey(code)) {
            return Optional.of(lookup.get(code));
        } else {
            return Optional.empty();
        }
    }
}
