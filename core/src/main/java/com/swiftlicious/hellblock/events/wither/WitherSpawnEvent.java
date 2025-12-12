package com.swiftlicious.hellblock.events.wither;

import org.bukkit.entity.Wither;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when an Enhanced Wither successfully spawns on an island.
 */
public class WitherSpawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final int islandId;
    private final Wither wither;
    private final double health;
    private final double strength;

    public WitherSpawnEvent(int islandId, @NotNull Wither wither, double health, double strength) {
        this.islandId = islandId;
        this.wither = wither;
        this.health = health;
        this.strength = strength;
    }

    public int getIslandId() {
        return islandId;
    }

    public @NotNull Wither getWither() {
        return wither;
    }

    public double getHealth() {
        return health;
    }

    public double getStrength() {
        return strength;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}