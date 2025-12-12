package com.swiftlicious.hellblock.events.wither;

import org.bukkit.entity.Wither;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when an Enhanced Wither despawns naturally or is forcefully removed.
 * This can occur when:
 *  - No island members are online
 *  - The Wither leaves the island boundary too long
 *  - It times out (e.g. after 5 minutes)
 */
public class WitherDespawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final int islandId;
    private final Wither wither;
    private final DespawnReason reason;

    public enum DespawnReason {
        NO_PLAYERS_ONLINE,
        OUT_OF_BOUNDS,
        TIMEOUT,
        MANUAL_REMOVAL
    }

    public WitherDespawnEvent(int islandId, @NotNull Wither wither, @NotNull DespawnReason reason) {
        this.islandId = islandId;
        this.wither = wither;
        this.reason = reason;
    }

    public int getIslandId() {
        return islandId;
    }

    public @NotNull Wither getWither() {
        return wither;
    }

    public @NotNull DespawnReason getReason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}