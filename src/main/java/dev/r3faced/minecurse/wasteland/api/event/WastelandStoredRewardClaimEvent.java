package dev.r3faced.minecurse.wasteland.api.event;

import dev.r3faced.minecurse.wasteland.model.StoredReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WastelandStoredRewardClaimEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final StoredReward reward;
    private boolean cancelled;

    public WastelandStoredRewardClaimEvent(Player player, StoredReward reward) {
        this.player = player;
        this.reward = reward;
    }

    public Player getPlayer() {
        return player;
    }

    public StoredReward getReward() {
        return reward;
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
