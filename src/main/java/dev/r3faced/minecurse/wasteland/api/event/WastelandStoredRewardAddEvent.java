package dev.r3faced.minecurse.wasteland.api.event;

import dev.r3faced.minecurse.wasteland.api.WastelandChangeReason;
import dev.r3faced.minecurse.wasteland.model.StoredReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a stored reward is added to a player's virtual reward backpack.
 */
public class WastelandStoredRewardAddEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private StoredReward reward;
    private final int tier;
    private final WastelandChangeReason reason;
    private boolean cancelled;

    public WastelandStoredRewardAddEvent(Player player, StoredReward reward, int tier, WastelandChangeReason reason) {
        this.player = player;
        this.reward = reward;
        this.tier = tier;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public StoredReward getReward() {
        return reward;
    }

    public void setReward(StoredReward reward) {
        if (reward == null) {
            throw new IllegalArgumentException("reward cannot be null");
        }
        this.reward = reward;
    }

    /**
     * Returns the tier that produced this reward, or -1 for API/custom rewards.
     */
    public int getTier() {
        return tier;
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
