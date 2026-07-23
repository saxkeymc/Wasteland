package dev.r3faced.minecurse.wasteland.api.event;

import dev.r3faced.minecurse.wasteland.api.WastelandChangeReason;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WastelandSkillChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SkillType skill;
    private final int oldLevel;
    private final int newLevel;
    private final long oldXp;
    private final long newXp;
    private final WastelandChangeReason reason;

    public WastelandSkillChangeEvent(Player player, SkillType skill, int oldLevel, int newLevel,
                                     long oldXp, long newXp, WastelandChangeReason reason) {
        this.player = player;
        this.skill = skill;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.oldXp = oldXp;
        this.newXp = newXp;
        this.reason = reason;
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

    public long getOldXp() {
        return oldXp;
    }

    public long getNewXp() {
        return newXp;
    }

    public WastelandChangeReason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
