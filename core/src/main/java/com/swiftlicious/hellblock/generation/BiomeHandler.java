package com.swiftlicious.hellblock.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.audience.Audience;

public class BiomeHandler {

	protected final HellblockPlugin instance;

	public BiomeHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void changeHellblockBiome(@NotNull UserData user, @NotNull HellBiome biome) {
		Player player = user.getPlayer();
		if (player != null) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (!user.getHellblockData().hasHellblock()) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
				return;
			}
			if (user.getHellblockData().isAbandoned()) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
				return;
			}
			if (user.getHellblockData().getOwnerUUID() == null) {
				throw new NullPointerException("Owner reference returned null, please report this to the developer.");
			}
			if (user.getHellblockData().getOwnerUUID() != null
					&& !user.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
				return;
			}
			if (user.getHellblockData().getBiomeCooldown() > 0) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_BIOME_ON_COOLDOWN
								.arguments(AdventureHelper.miniMessage(
										instance.getFormattedCooldown(user.getHellblockData().getBiomeCooldown())))
								.build()));
				return;
			}

			if (instance.getConfigManager().worldguardProtect()) {
				ProtectedRegion region = instance.getWorldGuardHandler().getRegion(player.getUniqueId(),
						user.getHellblockData().getID());
				if (region == null) {
					throw new NullPointerException("Region returned null, please report this to the developer.");
				}
				Set<UUID> owners = region.getOwners().getUniqueIds();
				if (!owners.contains(player.getUniqueId())) {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
					return;
				}

				if (player.getLocation() != null && !region.contains(player.getLocation().getBlockX(),
						player.getLocation().getBlockY(), player.getLocation().getBlockZ())) {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_MUST_BE_ON_ISLAND.build()));
					return;
				}

				if (user.getHellblockData().getHomeLocation().getBlock().getBiome().getKey().getKey()
						.equalsIgnoreCase(biome.toString().toLowerCase())) {
					audience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
									.arguments(AdventureHelper.miniMessage(biome.getName())).build()));
					return;
				}

				setHellblockBiome(region, biome.getConvertedBiome());

				user.getHellblockData().setBiome(biome);
				user.getHellblockData().setBiomeCooldown(86400L);
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_CHANGED
								.arguments(AdventureHelper.miniMessage(biome.getName())).build()));
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public void setHellblockBiome(@NotNull ProtectedRegion region, @NotNull Biome biome) {
		World world = instance.getHellblockHandler().getHellblockWorld();
		getHellblockChunks(region).thenAccept(chunks -> {
			Location min = BukkitAdapter.adapt(world, region.getMinimumPoint());
			Location max = BukkitAdapter.adapt(world, region.getMaximumPoint());
			setBiome(min, max, biome).thenRun(() -> {
				for (Chunk chunk : chunks) {
					chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
				}
			});
		}).exceptionally(throwable -> {
			throwable.printStackTrace();
			return null;
		});
	}

	public CompletableFuture<Void> setBiome(@NotNull Location start, @NotNull Location end, @NotNull Biome biome) {
		World world = start.getWorld(); // Avoid getting from weak reference in a loop.
		if (!world.getUID().equals(end.getWorld().getUID()))
			throw new IllegalArgumentException("Location worlds mismatch");
		int heightMax = world.getMaxHeight();
		int heightMin = world.getMinHeight();

		// Apparently setBiome is thread-safe.
		return CompletableFuture.runAsync(() -> {
			for (int x = start.getBlockX(); x < end.getBlockX(); x++) {
				// As of now increasing it by 4 seems to work.
				// This should be the minimal size of the vertical biomes.
				for (int y = heightMin; y < heightMax; y += 4) {
					for (int z = start.getBlockZ(); z < end.getBlockZ(); z++) {
						Block block = new Location(world, x, y, z).getBlock();
						if (block.getBiome() != biome)
							if (block.getBiome() != biome)
								block.setBiome(biome);
					}
				}
			}
		}).exceptionally((result) -> {
			result.printStackTrace();
			return null;
		});
	}

	public CompletableFuture<List<Chunk>> getHellblockChunks(@NotNull ProtectedRegion region) {
		return CompletableFuture.supplyAsync(() -> {
			List<Chunk> chunks = new ArrayList<>();

			World world = instance.getHellblockHandler().getHellblockWorld();
			BlockVector3 pos1 = region.getMinimumPoint();
			BlockVector3 pos2 = region.getMaximumPoint();

			int minX = pos1.x() >> 4;
			int minZ = pos1.z() >> 4;
			int maxX = pos2.x() >> 4;
			int maxZ = pos2.z() >> 4;

			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					chunks.add(world.getChunkAt(x, z));
				}
			}
			return chunks.stream().collect(Collectors.toList());
		}).exceptionally(throwable -> {
			throwable.printStackTrace();
			return Collections.emptyList();
		});
	}
}
