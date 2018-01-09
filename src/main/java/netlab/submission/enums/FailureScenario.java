package netlab.submission.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum FailureScenario {
    Default("default"),
    AllLinks("alllinks"),
    AllNodes("allnodes"),
    Sources("sources"),
    Destinations("destinations"),
    Members("members"),
    Network("network"),
    Quake_1("quake1"),
    Quake_2("quake2"),
    Quake_3("quake3"),
    Quake_1_2("quake12"),
    Quake_1_3("quake13"),
    Quake_2_3("quake23"),
    Quake_1_2_3("quake123"),
    Nuke_1("nuke1"),
    Nuke_2("nuke2"),
    Nuke_3("nuke3"),
    Nuke_1_2("nuke12"),
    Nuke_1_3("nuke13"),
    Nuke_2_3("nuke23"),
    Nuke_1_2_3("nuke123"),
    ;

    private String code;

    FailureScenario(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<String, FailureScenario> lookup = new HashMap<>();

    static {
        for (FailureScenario fs : EnumSet.allOf(FailureScenario.class)) {
            lookup.put(fs.getCode(), fs);
        }
    }

    public static Optional<FailureScenario> get(String code) {
        String lookupCode = code.toLowerCase();
        if (lookup.containsKey(lookupCode)) {
            return Optional.of(lookup.get(lookupCode));
        } else {
            return Optional.empty();
        }
    }
}
