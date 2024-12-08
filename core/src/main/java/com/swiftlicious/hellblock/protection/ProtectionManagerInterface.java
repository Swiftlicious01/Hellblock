package com.swiftlicious.hellblock.protection;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

public interface ProtectionManagerInterface {

	void changeProtectionFlag(@NotNull World world, @NotNull UUID id, @NotNull HellblockFlag flag);

	void changeLockStatus(@NotNull World world, @NotNull UUID id);

	void clearHellblockEntities(@NotNull World world, @NotNull BoundingBox bounds);

	CompletableFuture<List<Block>> getHellblockBlocks(@NotNull World world, @NotNull UUID id);
}
