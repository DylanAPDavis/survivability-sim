package netlab.submission.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public enum MemberFailureType {
    Allow("allow"),
    Enforce("enforce"),
    Half("half"),
    OneThird("onethird"),
    twoThirds("twothirds"),
    Prevent("prevent");

    private String code;

    MemberFailureType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, MemberFailureType> lookup = new HashMap<>();

    static {
        for (MemberFailureType mft : EnumSet.allOf(MemberFailureType.class)) {
            lookup.put(mft.getCode(), mft);
        }
    }

    public static Optional<MemberFailureType> get(String code) {
        String lookupCode = code.toLowerCase();
        if (lookup.containsKey(lookupCode)) {
            return Optional.of(lookup.get(lookupCode));
        } else {
            return Optional.empty();
        }
    }
}
