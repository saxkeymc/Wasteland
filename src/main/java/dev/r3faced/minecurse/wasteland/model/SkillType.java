package dev.r3faced.minecurse.wasteland.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents one of the four Wasteland skill paths.
 * <p>
 * The internal {@code key} stays as {@code woodcutting} for backwards
 * compatibility with existing player data files; the user-facing display
 * name is "Chopping" (configured in skills.yml). The teleport command
 * and SkillType.fromString() accept both "woodcutting" and "chopping".
 */
public enum SkillType {

    MINING("mining"),
    WOODCUTTING("woodcutting"),
    FARMING("farming"),
    FISHING("fishing");

    private final String key;

    /** Aliases that map to this skill (e.g. "chopping" -> WOODCUTTING). */
    private static final Map<String, SkillType> ALIASES = new HashMap<>();

    static {
        for (SkillType t : values()) {
            ALIASES.put(t.key.toLowerCase(), t);
        }
        // Public-facing aliases (do NOT change internal keys)
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

    /** Lowercase config key used in YAML and commands. */
    public String getKey() {
        return key;
    }

    /**
     * Parse a SkillType from a user-supplied string, case-insensitive.
     * Accepts the canonical key plus common aliases such as "chopping".
     *
     * @param input the string to parse
     * @return the matching SkillType or null if none
     */
    public static SkillType fromString(String input) {
        if (input == null) return null;
        return ALIASES.get(input.toLowerCase());
    }
}
