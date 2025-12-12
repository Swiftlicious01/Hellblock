package com.swiftlicious.hellblock.events.wither;

import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Called when an Enhanced Wither is defeated (killed) on an island.
 * This includes the killer (if known) and island context.
 */
public class WitherDefeatEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final int islandId;
    private final Wither wither;
    private final @Nullable Player killer;
    private final long timeAlive;

    public WitherDefeatEvent(int islandId, @NotNull Wither wither, @Nullable Player killer, long timeAlive) {
        this.islandId = islandId;
        this.wither = wither;
        this.killer = killer;
        this.timeAlive = timeAlive;
    }

    public int getIslandId() {
        return islandId;
    }

    public @NotNull Wither getWither() {
        return wither;
    }

    public @Nullable Player getKiller() {
        return killer;
    }

    /**
     * @return The time (in milliseconds) the Wither survived before death.
     */
    public long getTimeAlive() {
        return timeAlive;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}