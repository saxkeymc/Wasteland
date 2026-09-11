package dev.r3faced.minecurse.wasteland.api.event;

import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class WastelandToolGiveEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SkillType skill;
    private ItemStack item;
    private boolean cancelled;

    public WastelandToolGiveEvent(Player player, SkillType skill, ItemStack item) {
        this.player = player;
        this.skill = skill;
        this.item = item;
    }

    public Player getPlayer() {
        return player;
    }

    public SkillType getSkill() {
        return skill;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        this.item = item;
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
