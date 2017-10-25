package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum RoutingType {
    Unicast("unicast"),
    Anycast("anycast"),
    Manycast("manycast"),
    Multicast("multicast"),
    ManyToOne("manytoone"),
    ManyToMany("manytomany"),
    Broadcast("broadcast"),
    Default("default");

    private String code;

    RoutingType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, RoutingType> lookup = new HashMap<>();

    static {
        for (RoutingType pc : EnumSet.allOf(RoutingType.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<RoutingType> get(String code) {
        String lookupCode = code.toLowerCase();
        if (lookup.containsKey(lookupCode)) {
            return Optional.of(lookup.get(lookupCode));
        } else {
            return Optional.empty();
        }
    }
}
