package com.swiftlicious.hellblock.events.skysiege;

import java.util.UUID;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkysiegeStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final int islandId;
    private final UUID ownerUUID;
    private final World world;

    public SkysiegeStartEvent(int islandId, UUID ownerUUID, World world) {
        this.islandId = islandId;
        this.ownerUUID = ownerUUID;
        this.world = world;
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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}