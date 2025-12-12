package com.swiftlicious.hellblock.events.skysiege;

import java.util.UUID;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkysiegeWaveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final int islandId;
    private final UUID ownerUUID;
    private final World world;
    private final int waveNumber;

    public SkysiegeWaveEvent(int islandId, UUID ownerUUID, World world, int waveNumber) {
        this.islandId = islandId;
        this.ownerUUID = ownerUUID;
        this.world = world;
        this.waveNumber = waveNumber;
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

    public int getWaveNumber() {
        return waveNumber;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}