package dev.r3faced.minecurse.wasteland.model;

import java.util.HashMap;
import java.util.Map;

public enum SkillType {

    MINING("mining"),
    WOODCUTTING("woodcutting"),
    FARMING("farming"),
    FISHING("fishing");

    private final String key;

    private static final Map<String, SkillType> ALIASES = new HashMap<>();

    static {
        for (SkillType t : values()) {
            ALIASES.put(t.key.toLowerCase(), t);
        }
        ALIASES.put("chopping", WOODCUTTING);
        ALIASES.put("wood", WOODCUTTING);
        ALIASES.put("axe", WOODCUTTING);
        ALIASES.put("mine", MINING);
        ALIASES.put("pick", MINING);
        ALIASES.put("farm", FARMING);
        ALIASES.put("fish", FISHING);
    }

    SkillType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static SkillType fromString(String input) {
        if (input == null) return null;
        return ALIASES.get(input.toLowerCase());
    }
}
