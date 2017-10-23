package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Objective {
    LinksUsed("linksused"),
    Connections("connections"),
    TotalCost("totalcost");

    private String code;

    Objective(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, Objective> lookup = new HashMap<>();

    static {
        for (Objective pc : EnumSet.allOf(Objective.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<Objective> get(String code) {
        String lookupCode = code.toLowerCase();
        if (lookup.containsKey(lookupCode)) {
            return Optional.of(lookup.get(lookupCode));
        } else {
            return Optional.empty();
        }
    }
}
