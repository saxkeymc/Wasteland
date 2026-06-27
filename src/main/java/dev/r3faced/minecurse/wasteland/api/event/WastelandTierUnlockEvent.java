package dev.r3faced.minecurse.wasteland.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a player unlocks a new shared Wasteland tier.
 */
public class WastelandTierUnlockEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int previousTier;
    private final int tier;
    private final int requiredLevel;
    private boolean cancelled;

    public WastelandTierUnlockEvent(Player player, int previousTier, int tier, int requiredLevel) {
        this.player = player;
        this.previousTier = previousTier;
        this.tier = tier;
        this.requiredLevel = requiredLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public int getPreviousTier() {
        return previousTier;
    }

    public int getTier() {
        return tier;
    }

    public int getRequiredLevel() {
        return requiredLevel;
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
