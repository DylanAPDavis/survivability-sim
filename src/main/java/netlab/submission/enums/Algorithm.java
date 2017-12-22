package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Algorithm {
    ILP("ilp"),
    FlexBhandari("flexbhandari"),
    ShortestPath("shortestpath"),
    Bhandari("bhandari"),
    OverlappingTrees("overlappingtrees"),
    Hamlitonian("hamiltonian"),
    DestinationForwarding("destinationforwarding"),
    CollapsedRing("collapsedring");

    private String code;

    Algorithm(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, Algorithm> lookup = new HashMap<>();

    static {
        for (Algorithm pc : EnumSet.allOf(Algorithm.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<Algorithm> get(String code) {
        String lookupCode = code.toLowerCase();
        if (lookup.containsKey(lookupCode)) {
            return Optional.of(lookup.get(lookupCode));
        } else {
            return Optional.empty();
        }
    }
}
