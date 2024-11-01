package com.swiftlicious.hellblock.generation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer.HellblockData;
import lombok.NonNull;

public class BiomeHandler {

	private final HellblockPlugin instance;

	public BiomeHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	public @NonNull HellBiome convertBiomeToHellBiome(@NonNull Biome biome) {
		HellBiome hellBiome = HellBiome.NETHER_WASTES;
		switch (biome) {
		case Biome.SOUL_SAND_VALLEY:
			hellBiome = HellBiome.SOUL_SAND_VALLEY;
			break;
		case Biome.NETHER_WASTES:
			hellBiome = HellBiome.NETHER_WASTES;
			break;
		case Biome.CRIMSON_FOREST:
			hellBiome = HellBiome.CRIMSON_FOREST;
			break;
		case Biome.WARPED_FOREST:
			hellBiome = HellBiome.WARPED_FOREST;
			break;
		case Biome.BASALT_DELTAS:
			hellBiome = HellBiome.BASALT_DELTAS;
			break;
		default:
			break;
		}
		return hellBiome;
	}

	public void changeHellblockBiome(@NonNull HellblockPlayer hbPlayer, @NonNull HellBiome biome) {
		Player player = hbPlayer.getPlayer();
		if (player != null) {
			if (!hbPlayer.hasHellblock()) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't have a hellblock island! Create one with /hellblock create");
				return;
			}
			if (hbPlayer.getHellblockOwner() == null) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>An error has occurred. Please report this to the developer.");
				return;
			}
			if (hbPlayer.getHellblockOwner() != null && !hbPlayer.getHellblockOwner().equals(player.getUniqueId())) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't own this hellblock island!");
				return;
			}
			if (hbPlayer.getBiomeCooldown() > 0) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You have recently changed your biome already, you must wait for %s!",
								instance.getFormattedCooldown(hbPlayer.getBiomeCooldown())));
				return;
			}

			if (instance.getHellblockHandler().isWorldguardProtected()) {
				ProtectedRegion region = instance.getWorldGuardHandler().getRegion(player.getUniqueId(),
						hbPlayer.getID());
				if (region == null) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You don't have a hellblock island! Create one with /hellblock create");
					return;
				}
				DefaultDomain owners = region.getOwners();
				if (!owners.contains(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You don't own this hellblock island!");
					return;
				}

				if (!region.contains(player.getLocation().getBlockX(), player.getLocation().getBlockY(),
						player.getLocation().getBlockZ())) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You must be on your hellblock to change the biome!");
					return;
				}

				if (hbPlayer.getHomeLocation().getBlock().getBiome().getKey().getKey()
						.equalsIgnoreCase(biome.toString().toLowerCase())) {
					instance.getAdventureManager().sendMessageWithPrefix(player, String
							.format("<red>Your hellblock biome is already set to <dark_red>%s<red>!", biome.getName()));
					return;
				}

				setHellblockBiome(region, biome);

				hbPlayer.setHellblockBiome(biome);
				hbPlayer.setBiomeCooldown(Duration.ofDays(1).toHours());
				hbPlayer.saveHellblockPlayer();
				instance.getCoopManager().updateParty(hbPlayer.getUUID(), HellblockData.BIOME, biome);
				instance.getCoopManager().updateParty(hbPlayer.getUUID(), HellblockData.BIOME_COOLDOWN,
						Duration.ofDays(1).toHours());
				instance.getAdventureManager().sendMessageWithPrefix(player, String.format(
						"<red>You have changed the biome of your hellblock to <dark_red>%s<red>!", biome.getName()));
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public void setHellblockBiome(@NotNull ProtectedRegion region, @NotNull HellBiome biome) {
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

	public CompletableFuture<Void> setBiome(@NotNull Location start, @NotNull Location end, @NotNull HellBiome biome) {
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
						if (convertBiomeToHellBiome(block.getBiome()) != biome)
							block.setBiome(Biome.valueOf(biome.toString().toUpperCase()));
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
