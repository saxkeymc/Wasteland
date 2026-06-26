package dev.r3faced.minecurse.wasteland.model;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Holds all Wasteland data for a single player.
 * <p>
 * Skill levels and XP are tracked per-skill, but the tier system is
 * SHARED across all skills — every skill contributes toward unlocking
 * the same single tier progression. There is no "Mining Tier" or
 * "Fishing Tier"; there is only one tier per player.
 * <p>
 * Playtime is tracked in seconds and persisted across restarts.
 */
public class PlayerData {

    private final UUID uuid;

    /** Skill level per skill type. */
    private final Map<SkillType, Integer> levels = new EnumMap<>(SkillType.class);

    /** Accumulated XP per skill type. */
    private final Map<SkillType, Long> xp = new EnumMap<>(SkillType.class);

    /** Single shared tier (1-based). */
    private int tier = 1;

    /**
     * Set of tier claim keys that have been collected.
     * Key format: "tier_{n}" e.g. "tier_3"
     */
    private final Set<String> claimedTiers = new HashSet<>();

    /** Total time spent in the Wasteland system, in seconds. */
    private long playtimeSeconds = 0L;

    /**
     * Transient (NOT persisted) — epoch-millis when the player most recently
     * entered a Wasteland world, or 0 if they are not currently in one.
     * Used by PlaytimeTask and WorldChangeListener to compute partial
     * sessions on world change / quit / shutdown.
     */
    private transient long inWastelandSince = 0L;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        for (SkillType skill : SkillType.values()) {
            levels.put(skill, 0);
            xp.put(skill, 0L);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    // ── Level ─────────────────────────────────────────────────────────────────

    public int getLevel(SkillType skill) {
        return levels.getOrDefault(skill, 0);
    }

    public void setLevel(SkillType skill, int level) {
        levels.put(skill, level);
    }

    public int getTotalLevel() {
        int total = 0;
        for (SkillType skill : SkillType.values()) {
            total += getLevel(skill);
        }
        return total;
    }

    // ── XP ───────────────────────────────────────────────────────────────────

    public long getXp(SkillType skill) {
        return xp.getOrDefault(skill, 0L);
    }

    public void setXp(SkillType skill, long amount) {
        xp.put(skill, amount);
    }

    public void addXp(SkillType skill, long amount) {
        xp.put(skill, getXp(skill) + amount);
    }

    public long getTotalXp() {
        long total = 0L;
        for (SkillType skill : SkillType.values()) {
            total += getXp(skill);
        }
        return total;
    }

    // ── Tier (single, shared) ─────────────────────────────────────────────────

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    // ── Tier Claim Tracking ───────────────────────────────────────────────────

    public static String tierClaimKey(int tier) {
        return "tier_" + tier;
    }

    public boolean hasClaimedTier(int tier) {
        return claimedTiers.contains(tierClaimKey(tier));
    }

    public void claimTier(int tier) {
        claimedTiers.add(tierClaimKey(tier));
    }

    public Set<String> getClaimedTiers() {
        return claimedTiers;
    }

    public void setClaimedTiers(Set<String> keys) {
        claimedTiers.clear();
        claimedTiers.addAll(keys);
    }

    // ── Playtime ──────────────────────────────────────────────────────────────

    public long getPlaytimeSeconds() {
        return playtimeSeconds;
    }

    public void setPlaytimeSeconds(long seconds) {
        this.playtimeSeconds = seconds;
    }

    public void addPlaytimeSeconds(long seconds) {
        this.playtimeSeconds += seconds;
    }

    /** Returns epoch-millis when the player entered a Wasteland world, or 0 if not in one. */
    public long getInWastelandSince() {
        return inWastelandSince;
    }

    /** Set the epoch-millis when the player entered a Wasteland world (0 = not in one). */
    public void setInWastelandSince(long millis) {
        this.inWastelandSince = millis;
    }

    // ── Bulk access for serialization ─────────────────────────────────────────

    public Map<SkillType, Integer> getLevels() {
        return levels;
    }

    public Map<SkillType, Long> getXpMap() {
        return xp;
    }
}
