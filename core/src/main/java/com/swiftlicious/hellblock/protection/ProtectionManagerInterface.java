package com.swiftlicious.hellblock.protection;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.player.HellblockData;

public interface ProtectionManagerInterface {

	void changeProtectionFlag(@NotNull World world, @NotNull UUID id, @NotNull HellblockFlag flag);

	void changeLockStatus(@NotNull World world, @NotNull UUID id);

	void restoreIsland(@NotNull HellblockData data);

	void clearHellblockEntities(@NotNull World world, @NotNull BoundingBox bounds);

	CompletableFuture<List<Block>> getHellblockBlocks(@NotNull World world, @NotNull UUID id);

	CompletableFuture<@Nullable BoundingBox> getHellblockBounds(@NotNull World world, @NotNull UUID ownerId);

	CompletableFuture<Boolean> isInsideIsland(@NotNull UUID ownerId, @NotNull Location location);

	CompletableFuture<Boolean> isInsideIsland2D(@NotNull UUID ownerId, @NotNull Location location);
}
