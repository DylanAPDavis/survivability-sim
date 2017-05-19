package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Objective {
    LinksUsed("LinksUsed"),
    Connections("Connections"),
    LinkCost("LinkCost");

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
        if (lookup.containsKey(code)) {
            return Optional.of(lookup.get(code));
        } else {
            return Optional.empty();
        }
    }
}
