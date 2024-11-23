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
import com.swiftlicious.hellblock.player.UserData;

import lombok.NonNull;

public class BiomeHandler {

	protected final HellblockPlugin instance;

	public BiomeHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void changeHellblockBiome(@NonNull UserData user, @NonNull HellBiome biome) {
		Player player = user.getPlayer();
		if (player != null) {
			if (!user.getHellblockData().hasHellblock()) {
				instance.getAdventureManager().sendMessage(player, instance.getTranslationManager()
						.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
				return;
			}
			if (user.getHellblockData().isAbandoned()) {
				instance.getAdventureManager().sendMessage(player, instance.getTranslationManager()
						.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build().key()));
				return;
			}
			if (user.getHellblockData().getOwnerUUID() == null) {
				throw new NullPointerException("Owner reference returned null, please report this to the developer.");
			}
			if (user.getHellblockData().getOwnerUUID() != null
					&& !user.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
				instance.getAdventureManager().sendMessage(player, instance.getTranslationManager()
						.miniMessageTranslation(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build().key()));
				return;
			}
			if (user.getHellblockData().getBiomeCooldown() > 0) {
				instance.getAdventureManager().sendMessage(player,
						String.format("<red>You've recently changed your biome already, you must wait for %s!",
								instance.getFormattedCooldown(user.getHellblockData().getBiomeCooldown())));
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
					instance.getAdventureManager().sendMessage(player, instance.getTranslationManager()
							.miniMessageTranslation(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build().key()));
					return;
				}

				if (player.getLocation() != null && !region.contains(player.getLocation().getBlockX(),
						player.getLocation().getBlockY(), player.getLocation().getBlockZ())) {
					instance.getAdventureManager().sendMessage(player,
							"<red>You must be on your hellblock to change the biome!");
					return;
				}

				if (user.getHellblockData().getHomeLocation().getBlock().getBiome().getKey().getKey()
						.equalsIgnoreCase(biome.toString().toLowerCase())) {
					instance.getAdventureManager().sendMessage(player, String
							.format("<red>Your hellblock biome is already set to <dark_red>%s<red>!", biome.getName()));
					return;
				}

				setHellblockBiome(region, biome.getConvertedBiome());

				user.getHellblockData().setBiome(biome);
				user.getHellblockData().setBiomeCooldown(86400L);
				instance.getAdventureManager().sendMessage(player, String.format(
						"<red>You've changed the biome of your hellblock to <dark_red>%s<red>!", biome.getName()));
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
