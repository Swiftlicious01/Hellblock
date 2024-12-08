package com.swiftlicious.hellblock.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.RequirementManagerInterface;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.audience.Audience;

public class BiomeHandler {

	protected final HellblockPlugin instance;

	public BiomeHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void changeHellblockBiome(@NotNull UserData user, @NotNull HellBiome biome, boolean performedByGUI) {
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

			if (player.getLocation() != null
					&& !user.getHellblockData().getBoundingBox().contains(player.getLocation().getBlockX(),
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

			if (!performedByGUI && instance.getBiomeGUIManager().getBiomeRequirements(biome) != null) {
				if (!RequirementManagerInterface.isSatisfied(Context.player(player),
						instance.getBiomeGUIManager().getBiomeRequirements(biome))) {
					return;
				}
			}

			Optional<HellblockWorld<?>> world = instance.getWorldManager()
					.getWorld(instance.getWorldManager().getHellblockWorldFormat(user.getHellblockData().getID()));
			if (world.isEmpty() || world.get() == null)
				throw new NullPointerException(
						"World returned null, please try to regenerate the world before reporting this issue.");
			World bukkitWorld = world.get().bukkitWorld();

			setHellblockBiome(bukkitWorld, user.getHellblockData().getBoundingBox(), biome.getConvertedBiome());

			user.getHellblockData().setBiome(biome);
			user.getHellblockData().setBiomeCooldown(86400L);
			audience.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_CHANGED
					.arguments(AdventureHelper.miniMessage(biome.getName())).build()));
		}
	}

	public void setHellblockBiome(@NotNull World world, @NotNull BoundingBox bounds, @NotNull Biome biome) {
		Objects.requireNonNull(biome, () -> "Unsupported biome: " + biome.name());
		Objects.requireNonNull(world, "Cannot set biome of null world");
		Objects.requireNonNull(bounds, "Cannot set biome of null bounding box");

		getHellblockChunks(world, bounds).thenAccept(chunks -> {
			Location min = new Location(world, bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
			Location max = new Location(world, bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
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

	/**
	 * Change the biome in the selected region. Unloaded chunks will be ignored.
	 * Note that this doesn't send any update packets to the nearby clients.
	 *
	 * @param start the start position.
	 * @param end   the end position.
	 **/
	@NotNull
	public CompletableFuture<Void> setBiome(@NotNull Location start, @NotNull Location end, @NotNull Biome biome) {
		Objects.requireNonNull(start, "Start location cannot be null");
		Objects.requireNonNull(end, "End location cannot be null");
		Objects.requireNonNull(biome, () -> "Unsupported biome: " + biome.name());
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
							block.setBiome(biome);
					}
				}
			}
		}).exceptionally((result) -> {
			result.printStackTrace();
			return null;
		});
	}

	@NotNull
	public CompletableFuture<List<Chunk>> getHellblockChunks(@NotNull World world, @NotNull BoundingBox bounds) {
		Objects.requireNonNull(bounds, "Cannot get chunks of null bounding box");
		CompletableFuture<List<Chunk>> chunkData = new CompletableFuture<>();
		List<Chunk> chunks = new ArrayList<>();

		int minX = (int) bounds.getMinX() >> 4;
		int minZ = (int) bounds.getMinZ() >> 4;
		int maxX = (int) bounds.getMaxX() >> 4;
		int maxZ = (int) bounds.getMaxZ() >> 4;

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				chunks.add(world.getChunkAt(x, z));
			}
		}
		chunkData.complete(chunks.stream().collect(Collectors.toList()));
		return chunkData;
	}
}