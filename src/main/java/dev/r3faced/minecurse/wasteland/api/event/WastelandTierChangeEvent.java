package dev.r3faced.minecurse.wasteland.api.event;

import dev.r3faced.minecurse.wasteland.api.WastelandChangeReason;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired after a player's shared tier changes.
 */
public class WastelandTierChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int oldTier;
    private final int newTier;
    private final WastelandChangeReason reason;

    public WastelandTierChangeEvent(Player player, int oldTier, int newTier, WastelandChangeReason reason) {
        this.player = player;
        this.oldTier = oldTier;
        this.newTier = newTier;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public int getOldTier() {
        return oldTier;
    }

    public int getNewTier() {
        return newTier;
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
