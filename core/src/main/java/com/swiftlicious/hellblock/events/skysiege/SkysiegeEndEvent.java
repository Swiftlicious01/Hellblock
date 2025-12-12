package com.swiftlicious.hellblock.events.skysiege;

import java.util.UUID;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkysiegeEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final int islandId;
    private final UUID ownerUUID;
    private final World world;
    private final boolean success;

    public SkysiegeEndEvent(int islandId, UUID ownerUUID, World world, boolean success) {
        this.islandId = islandId;
        this.ownerUUID = ownerUUID;
        this.world = world;
        this.success = success;
    }

    public int getIslandId() {
        return islandId;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public World getWorld() {
        return world;
    }

    public boolean wasSuccessful() {
        return success;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}