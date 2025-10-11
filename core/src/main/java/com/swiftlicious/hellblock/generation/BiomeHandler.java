package com.swiftlicious.hellblock.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
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
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.events.hellblock.HellblockBiomeChangeEvent;
import com.swiftlicious.hellblock.events.hellblock.HellblockBiomeChangedEvent;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.world.HellblockWorld;

public class BiomeHandler {

	protected final HellblockPlugin instance;

	public BiomeHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	/**
	 * Initiates a biome change for the user's hellblock, handling both online and
	 * offline scenarios.
	 *
	 * @param user           The user whose hellblock biome is to be changed.
	 * @param biome          The new biome to set.
	 * @param performedByGUI Whether the change was initiated via GUI.
	 * @param forceChange    If true, bypasses ownership, location, and cooldown
	 *                       checks.
	 */
	public void changeHellblockBiome(@NotNull UserData user, @NotNull HellBiome biome, boolean performedByGUI,
			boolean forceChange) {
		if (user.isOnline()) {
			changeHellblockBiomeOnline(user, biome, performedByGUI, forceChange);
		} else {
			changeHellblockBiomeOffline(user, biome, forceChange);
		}
	}

	/**
	 * Handles biome changes for online users, including all necessary checks and
	 * messaging.
	 * 
	 * @param user
	 * @param biome
	 * @param performedByGUI
	 * @param forceChange
	 */
	private void changeHellblockBiomeOnline(@NotNull UserData user, @NotNull HellBiome biome, boolean performedByGUI,
			boolean forceChange) {
		final Player player = user.getPlayer();
		final Sender audience = instance.getSenderFactory().wrap(player);
		final HellblockData data = user.getHellblockData();

		// Validate island existence
		if (!data.hasHellblock()) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
			return;
		}

		if (data.isAbandoned()) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
			return;
		}

		// Ownership check
		final UUID ownerId = data.getOwnerUUID();
		if (ownerId == null) {
			throw new IllegalStateException("Hellblock owner UUID is missing.");
		}
		if (!forceChange && !ownerId.equals(player.getUniqueId())) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
			return;
		}

		// Cooldown check
		if (!forceChange && data.getBiomeCooldown() > 0) {
			final String formatted = instance.getFormattedCooldown(data.getBiomeCooldown());
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_ON_COOLDOWN
							.arguments(AdventureHelper.miniMessage(formatted)).build()));
			return;
		}

		// Location check
		if (!forceChange) {
			final Location loc = player.getLocation();
			if (loc == null || !data.getBoundingBox().contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_MUST_BE_ON_ISLAND.build()));
				return;
			}
		}

		// Prevent same-biome change
		final Biome currentBiome = data.getHomeLocation().getBlock().getBiome();
		if (currentBiome == biome.getConvertedBiome()) {
			audience.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
					.arguments(AdventureHelper.miniMessage(biome.getName())).build()));
			return;
		}

		// GUI requirement check
		if (!forceChange && !performedByGUI) {
			final var requirements = instance.getBiomeGUIManager().getBiomeRequirements(biome);
			if (requirements != null && !RequirementManager.isSatisfied(Context.player(player), requirements)) {
				return; // silently fail
			}
		}

		final HellblockBiomeChangeEvent changeEvent = new HellblockBiomeChangeEvent(user, data, currentBiome, biome,
				performedByGUI, forceChange);
		Bukkit.getPluginManager().callEvent(changeEvent);
		if (changeEvent.isCancelled()) {
			return;
		}

		// Apply the biome change
		applyHellblockBiomeChange(data, biome, !forceChange);

		// Success message
		audience.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_CHANGED
				.arguments(AdventureHelper.miniMessage(biome.getName())).build()));

		final HellblockBiomeChangedEvent changedEvent = new HellblockBiomeChangedEvent(user, data, currentBiome, biome);
		Bukkit.getPluginManager().callEvent(changedEvent);
	}

	/**
	 * Handles biome changes for offline users, applying the change directly if
	 * valid.
	 * 
	 * @param user
	 * @param biome
	 * @param forceChange
	 */
	private void changeHellblockBiomeOffline(@NotNull UserData user, @NotNull HellBiome biome, boolean forceChange) {
		final HellblockData data = user.getHellblockData();

		if (!data.hasHellblock() || data.isAbandoned()) {
			return; // silently ignore for offline users
		}

		// Prevent same-biome change
		final Biome currentBiome = data.getHomeLocation().getBlock().getBiome();
		if (currentBiome == biome.getConvertedBiome()) {
			return; // nothing to do
		}

		final HellblockBiomeChangeEvent changeEvent = new HellblockBiomeChangeEvent(user, data, currentBiome, biome,
				false, forceChange);
		Bukkit.getPluginManager().callEvent(changeEvent);
		if (changeEvent.isCancelled()) {
			return;
		}

		// Apply the biome change (no messages, no cooldown unless forceChange is false)
		applyHellblockBiomeChange(data, biome, !forceChange);

		final HellblockBiomeChangedEvent changedEvent = new HellblockBiomeChangedEvent(user, data, currentBiome, biome);
		Bukkit.getPluginManager().callEvent(changedEvent);
	}

	/**
	 * Applies the biome change to the hellblock world and updates the
	 * HellblockData.
	 * 
	 * @param data          The HellblockData representing the user's hellblock.
	 * @param biome         The new biome to set.
	 * @param applyCooldown If true, sets a cooldown on future biome changes.
	 */
	private void applyHellblockBiomeChange(@NotNull HellblockData data, @NotNull HellBiome biome,
			boolean applyCooldown) {
		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(data.getID()));
		if (worldOpt.isEmpty()) {
			throw new IllegalStateException("Hellblock world not found. Try regenerating the world.");
		}

		final World bukkitWorld = worldOpt.get().bukkitWorld();

		setHellblockBiome(bukkitWorld, data.getBoundingBox(), biome.getConvertedBiome());
		data.setBiome(biome);

		if (applyCooldown) {
			data.setBiomeCooldown(TimeUnit.SECONDS.toDays(86400)); // 24h cooldown
		}
	}

	/**
	 * Sets the biome for all blocks within the specified bounding box in the given
	 * world. Unloaded chunks will be loaded as needed. Note that this doesn't send
	 * any update packets to the nearby clients.
	 *
	 * @param world  the world where the biome change will occur.
	 * @param bounds the bounding box defining the area to change.
	 * @param biome  the new biome to set.
	 **/
	public void setHellblockBiome(@NotNull World world, @NotNull BoundingBox bounds, @NotNull Biome biome) {
		Objects.requireNonNull(biome, () -> "Unsupported biome: " + biome.toString());
		Objects.requireNonNull(world, "Cannot set biome of null world");
		Objects.requireNonNull(bounds, "Cannot set biome of null bounding box");

		getHellblockChunks(world, bounds).thenAccept(chunks -> {
			final Location min = new Location(world, bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
			final Location max = new Location(world, bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
			setBiome(min, max, biome)
					.thenRun(() -> chunks.forEach(chunk -> chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ())));
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
		Objects.requireNonNull(biome, () -> "Unsupported biome: " + biome.toString());
		final World world = start.getWorld(); // Avoid getting from weak reference in a loop.
		if (!world.getUID().equals(end.getWorld().getUID())) {
			throw new IllegalArgumentException("Location worlds mismatch");
		}
		final int heightMax = world.getMaxHeight();
		final int heightMin = world.getMinHeight();

		// Apparently setBiome is thread-safe.
		return CompletableFuture.runAsync(() -> {
			for (int x = start.getBlockX(); x < end.getBlockX(); x++) {
				// As of now increasing it by 4 seems to work.
				// This should be the minimal size of the vertical biomes.
				for (int y = heightMin; y < heightMax; y += 4) {
					for (int z = start.getBlockZ(); z < end.getBlockZ(); z++) {
						final Block block = new Location(world, x, y, z).getBlock();
						if (block.getBiome() != biome) {
							block.setBiome(biome);
						}
					}
				}
			}
		}).exceptionally((result) -> {
			result.printStackTrace();
			return null;
		});
	}

	/**
	 * Retrieves all chunks that intersect with the given bounding box in the
	 * specified world.
	 * 
	 * @param world  the world to get chunks from.
	 * 
	 * @param bounds the bounding box defining the area of interest.
	 * 
	 * @return a CompletableFuture that will complete with a list of chunks within
	 *         the bounding box.
	 */
	@NotNull
	public CompletableFuture<List<Chunk>> getHellblockChunks(@NotNull World world, @NotNull BoundingBox bounds) {
		Objects.requireNonNull(bounds, "Cannot get chunks of null bounding box");
		final CompletableFuture<List<Chunk>> chunkData = new CompletableFuture<>();
		final List<Chunk> chunks = new ArrayList<>();

		final int minX = (int) bounds.getMinX() >> 4;
		final int minZ = (int) bounds.getMinZ() >> 4;
		final int maxX = (int) bounds.getMaxX() >> 4;
		final int maxZ = (int) bounds.getMaxZ() >> 4;

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				chunks.add(world.getChunkAt(x, z));
			}
		}
		chunkData.complete(chunks.stream().collect(Collectors.toList()));
		return chunkData;
	}
}