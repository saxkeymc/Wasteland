package dev.r3faced.minecurse.wasteland.api.event;

import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WastelandSkillLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SkillType skill;
    private final int oldLevel;
    private final int newLevel;
    private final WastelandXpCause cause;
    private final String source;

    public WastelandSkillLevelUpEvent(Player player, SkillType skill, int oldLevel, int newLevel,
                                      WastelandXpCause cause, String source) {
        this.player = player;
        this.skill = skill;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.cause = cause;
        this.source = source;
    }

    public Player getPlayer() {
        return player;
    }

    public SkillType getSkill() {
        return skill;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public WastelandXpCause getCause() {
        return cause;
    }

    public String getSource() {
        return source;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
