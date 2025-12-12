package com.swiftlicious.hellblock.events.invasion;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.listeners.invasion.InvasionProfile;

/**
 * Fired when an invasion is about to generate loot.
 * Plugins can modify, remove, or add items before the loot is finalized.
 */
public class InvasionLootGenerateEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final UUID ownerId;
    private final InvasionProfile profile;
    private final Location chestLocation;
    private List<ItemStack> loot;

    public InvasionLootGenerateEvent(UUID ownerId, InvasionProfile profile, Location chestLocation, List<ItemStack> loot) {
        this.ownerId = ownerId;
        this.profile = profile;
        this.chestLocation = chestLocation;
        this.loot = loot;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public InvasionProfile getProfile() {
        return profile;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public List<ItemStack> getLoot() {
        return loot;
    }

    public void setLoot(List<ItemStack> loot) {
        this.loot = loot;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}