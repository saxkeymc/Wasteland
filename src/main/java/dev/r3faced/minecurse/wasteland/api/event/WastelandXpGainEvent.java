package dev.r3faced.minecurse.wasteland.api.event;

import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before Wasteland XP is applied. Other plugins may cancel this event
 * or change the amount to support boosters, masks, and custom restrictions.
 */
public class WastelandXpGainEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SkillType skill;
    private final long originalAmount;
    private long amount;
    private final WastelandXpCause cause;
    private final String source;
    private boolean cancelled;

    public WastelandXpGainEvent(Player player, SkillType skill, long amount, WastelandXpCause cause, String source) {
        this.player = player;
        this.skill = skill;
        this.originalAmount = amount;
        this.amount = amount;
        this.cause = cause;
        this.source = source;
    }

    public Player getPlayer() {
        return player;
    }

    public SkillType getSkill() {
        return skill;
    }

    public long getOriginalAmount() {
        return originalAmount;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = Math.max(0L, amount);
    }

    public WastelandXpCause getCause() {
        return cause;
    }

    /**
     * Optional source string, such as a block material name or a custom mask id.
     */
    public String getSource() {
        return source;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
