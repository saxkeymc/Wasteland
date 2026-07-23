package dev.r3faced.minecurse.wasteland.api.event;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WastelandTierLockedOreBreakEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Block block;
    private final Material oreType;
    private int requiredTier;
    private final int playerTier;
    private boolean cancelled;

    public WastelandTierLockedOreBreakEvent(Player player, Block block, Material oreType, int requiredTier, int playerTier) {
        this.player = player;
        this.block = block;
        this.oreType = oreType;
        this.requiredTier = requiredTier;
        this.playerTier = playerTier;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getBlock() {
        return block;
    }

    public Material getOreType() {
        return oreType;
    }

    public int getRequiredTier() {
        return requiredTier;
    }

    public void setRequiredTier(int requiredTier) {
        this.requiredTier = Math.max(1, requiredTier);
    }

    public int getPlayerTier() {
        return playerTier;
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
