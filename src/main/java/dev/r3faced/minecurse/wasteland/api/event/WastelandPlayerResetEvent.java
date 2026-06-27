package dev.r3faced.minecurse.wasteland.api.event;

import dev.r3faced.minecurse.wasteland.api.WastelandChangeReason;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired before Wasteland resets a player's stored progress.
 */
public class WastelandPlayerResetEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID uuid;
    private final WastelandChangeReason reason;
    private boolean cancelled;

    public WastelandPlayerResetEvent(UUID uuid, WastelandChangeReason reason) {
        this.uuid = uuid;
        this.reason = reason;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public WastelandChangeReason getReason() {
        return reason;
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
