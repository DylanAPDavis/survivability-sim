package netlab.analysis.enums;


import netlab.submission.enums.Algorithm;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum MemberType {
    Source("Source"),
    Destination("Destination");

    private String code;

    MemberType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, MemberType> lookup = new HashMap<>();

    static {
        for (MemberType pc : EnumSet.allOf(MemberType.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<MemberType> get(String code) {
        if (lookup.containsKey(code)) {
            return Optional.of(lookup.get(code));
        } else {
            return Optional.empty();
        }
    }
}
