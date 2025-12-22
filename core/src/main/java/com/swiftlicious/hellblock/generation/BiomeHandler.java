package com.swiftlicious.hellblock.generation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
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
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

/**
 * Handles all biome-related operations within the Hellblock system, including
 * both online and offline biome changes, validation, and world updates.
 * <p>
 * This class manages biome transformations for a player's hellblock island,
 * ensuring proper checks such as ownership, cooldowns, location validation, and
 * event triggering. It also provides asynchronous biome updates across chunks
 * and supports safe biome modification even when the player is offline.
 * </p>
 * 
 * <h2>Responsibilities:</h2>
 * <ul>
 * <li>Initiate biome changes for online and offline players.</li>
 * <li>Validate ownership, cooldowns, and biome change conditions.</li>
 * <li>Apply biome transformations safely across multiple chunks.</li>
 * <li>Trigger pre- and post-biome change events for plugin extensibility.</li>
 * </ul>
 * 
 * <h2>Threading Notes:</h2>
 * <p>
 * Biome updates are performed asynchronously to prevent blocking the main
 * server thread. Unloaded chunks are loaded as needed, and chunk refreshes are
 * triggered post-update to ensure visual consistency for players.
 * </p>
 */
public class BiomeHandler {

	protected final HellblockPlugin instance;

	public BiomeHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	/**
	 * Initiates a biome change for the user's hellblock, handling both online and
	 * offline scenarios.
	 *
	 * @param ownerData      The island owner whose hellblock biome is to be
	 *                       changed.
	 * @param biome          The new biome to set.
	 * @param performedByGUI Whether the change was initiated via GUI.
	 * @param forceChange    If true, bypasses ownership, location, and cooldown
	 *                       checks.
	 */
	public CompletableFuture<Boolean> changeHellblockBiome(@NotNull UserData ownerData, @NotNull HellBiome biome,
			boolean performedByGUI, boolean forceChange) {
		if (ownerData.isOnline()) {
			return changeHellblockBiomeOnline(ownerData, biome, performedByGUI, forceChange);
		} else {
			return changeHellblockBiomeOffline(ownerData, biome, forceChange);
		}
	}

	/**
	 * Handles biome changes for online users, including all necessary checks and
	 * messaging.
	 * 
	 * @param ownerData      The island owner whose hellblock biome is to be
	 *                       changed.
	 * @param biome          The new biome to set.
	 * @param performedByGUI Whether the change was initiated via GUI.
	 * @param forceChange    If true, bypasses ownership, location, and cooldown
	 *                       checks.
	 */
	private CompletableFuture<Boolean> changeHellblockBiomeOnline(@NotNull UserData ownerData, @NotNull HellBiome biome,
			boolean performedByGUI, boolean forceChange) {
		final Player player = ownerData.getPlayer();
		final Sender audience = instance.getSenderFactory().wrap(player);
		final HellblockData data = ownerData.getHellblockData();

		if (!data.hasHellblock()) {
			if (!forceChange)
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
			return CompletableFuture.completedFuture(false);
		}

		if (data.isAbandoned()) {
			if (!forceChange)
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
			return CompletableFuture.completedFuture(false);
		}

		final UUID ownerId = data.getOwnerUUID();
		if (ownerId == null) {
			instance.getPluginLogger().severe("Hellblock owner UUID was null for player " + ownerData.getName() + " ("
					+ ownerData.getUUID() + "). This indicates corrupted data.");
			return CompletableFuture.failedFuture(new IllegalStateException(
					"Owner reference was null. This should never happen — please report to the developer."));
		}

		if (!forceChange && !ownerId.equals(ownerData.getUUID())) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
			return CompletableFuture.completedFuture(false);
		}

		if (!forceChange && data.getBiomeCooldown() > 0) {
			final String formatted = instance.getCooldownManager().getFormattedCooldown(data.getBiomeCooldown());
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_ON_COOLDOWN
							.arguments(AdventureHelper.miniMessageToComponent(formatted)).build()));
			return CompletableFuture.completedFuture(false);
		}

		if (!forceChange) {
			final BoundingBox bounds = data.getBoundingBox();
			if (bounds == null) {
				instance.getPluginLogger().severe("Hellblock bounds was null for owner " + ownerData.getName() + " ("
						+ ownerData.getUUID() + "). This indicates corrupted data or a serious bug.");
				return CompletableFuture.failedFuture(new IllegalStateException(
						"Hellblock bounds location returned null. This should never happen — please report to the developer."));
			}
			final Location loc = player.getLocation();
			if (loc == null || !bounds.contains(loc.toVector())) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_MUST_BE_ON_ISLAND.build()));
				return CompletableFuture.completedFuture(false);
			}
		}

		final Location homeLocation = data.getHomeLocation();
		if (homeLocation == null) {
			instance.getPluginLogger().severe("Hellblock home location was null for owner " + ownerData.getName() + " ("
					+ ownerData.getUUID() + "). This indicates corrupted data or a serious bug.");
			return CompletableFuture.failedFuture(new IllegalStateException(
					"Hellblock home location returned null. This should never happen — please report to the developer."));
		}

		// Get the current biome on the main thread
		return instance.getScheduler().supplySync(() -> homeLocation.getBlock().getBiome())
				.thenCompose(currentBiome -> {
					if (currentBiome.equals(biome.getConvertedBiome())) {
						if (!forceChange)
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
											.arguments(AdventureHelper
													.miniMessageToComponent(StringUtils.toCamelCase(biome.toString())))
											.build()));
						return CompletableFuture.completedFuture(false);
					}

					if (!forceChange && !performedByGUI) {
						final var requirements = instance.getBiomeGUIManager().getBiomeRequirements(biome);
						if (requirements != null
								&& !RequirementManager.isSatisfied(Context.player(player), requirements)) {
							return CompletableFuture.completedFuture(false); // silently fail
						}
					}

					HellblockBiomeChangeEvent changeEvent = new HellblockBiomeChangeEvent(ownerData, data, currentBiome,
							biome, performedByGUI, forceChange);
					if (EventUtils.fireAndCheckCancel(changeEvent)) {
						return CompletableFuture.completedFuture(false);
					}

					return applyHellblockBiomeChange(ownerData, biome, !forceChange).thenRun(() -> {
						// Send success message
						if (!forceChange)
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_BIOME_CHANGED
											.arguments(AdventureHelper
													.miniMessageToComponent(StringUtils.toCamelCase(biome.toString())))
											.build()));
					}).thenApply(v -> true);
				});
	}

	/**
	 * Handles biome changes for offline users, applying the change directly if
	 * valid.
	 * 
	 * @param ownerData   The island owner whose hellblock biome is to be changed.
	 * @param biome       The new biome to set.
	 * @param forceChange If true, bypasses ownership, location, and cooldown
	 *                    checks.
	 */
	private CompletableFuture<Boolean> changeHellblockBiomeOffline(@NotNull UserData ownerData,
			@NotNull HellBiome biome, boolean forceChange) {
		final HellblockData data = ownerData.getHellblockData();

		if (!data.hasHellblock() || data.isAbandoned()) {
			return CompletableFuture.completedFuture(false);
		}

		// Defensive: ensure home location is not null
		Location homeLocation = data.getHomeLocation();
		if (homeLocation == null) {
			instance.getPluginLogger().severe("Hellblock home location was null for owner " + ownerData.getName() + " ("
					+ ownerData.getUUID() + "). This indicates corrupted data or a serious bug.");
			return CompletableFuture.failedFuture(new IllegalStateException(
					"Hellblock home location returned null. This should never happen — please report to the developer."));
		}

		// Get current biome asynchronously
		return instance.getScheduler().supplySync(() -> homeLocation.getBlock().getBiome())
				.thenCompose(currentBiome -> {
					if (currentBiome.equals(biome.getConvertedBiome())) {
						return CompletableFuture.completedFuture(false);
					}

					HellblockBiomeChangeEvent changeEvent = new HellblockBiomeChangeEvent(ownerData, data, currentBiome,
							biome, false, forceChange);
					if (EventUtils.fireAndCheckCancel(changeEvent)) {
						return CompletableFuture.completedFuture(false);
					}

					return applyHellblockBiomeChange(ownerData, biome, !forceChange).thenApply(v -> true);
				});
	}

	/**
	 * Applies the biome change to the hellblock world and updates the
	 * HellblockData.
	 * 
	 * @param ownerData     The HellblockData representing the user's hellblock.
	 * @param biome         The new biome to set.
	 * @param applyCooldown If true, sets a cooldown on future biome changes.
	 */
	public CompletableFuture<Void> applyHellblockBiomeChange(@NotNull UserData ownerData, @NotNull HellBiome newBiome,
			boolean applyCooldown) {
		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(
				instance.getWorldManager().getHellblockWorldFormat(ownerData.getHellblockData().getIslandId()));

		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
			return CompletableFuture
					.failedFuture(new IllegalStateException("Hellblock world not found. Try regenerating the world."));
		}

		final HellblockWorld<?> world = worldOpt.get();
		final BoundingBox bounds = ownerData.getHellblockData().getBoundingBox();

		if (bounds == null) {
			return CompletableFuture.failedFuture(new IllegalStateException(
					"Hellblock bounding box returned null for islandID=" + ownerData.getHellblockData().getIslandId()));
		}

		// Capture previous biome
		HellBiome previousBiome = Objects.requireNonNullElse(ownerData.getHellblockData().getBiome(),
				HellBiome.NETHER_WASTES);

		// Async biome setting
		return setHellblockBiome(world, bounds, newBiome.getConvertedBiome()).thenRun(() -> {

			// Apply cooldown if needed
			if (applyCooldown) {
				ownerData.getHellblockData().setBiomeCooldown(TimeUnit.DAYS.toSeconds(1)); // 24h cooldown
			}

			// Update biome in data
			ownerData.getHellblockData().setBiome(newBiome);

			final HellblockBiomeChangedEvent changedEvent = new HellblockBiomeChangedEvent(ownerData,
					ownerData.getHellblockData(), previousBiome.getConvertedBiome(), newBiome);
			EventUtils.fireAndForget(changedEvent);

			// Run fortress logic on sync thread
			instance.getScheduler().executeSync(() -> {
				if (newBiome == HellBiome.NETHER_FORTRESS) {
					VersionHelper.getNMSManager().injectFakeFortress(world.bukkitWorld(), bounds);
					instance.debug("Injected fake fortress for islandID=" + ownerData.getHellblockData().getIslandId()
							+ " in world " + world.worldName() + " bounds=" + bounds);
				} else if (previousBiome == HellBiome.NETHER_FORTRESS) {
					VersionHelper.getNMSManager().removeFakeFortress(world.bukkitWorld(), bounds);
					instance.debug("Removed fake fortress for islandID=" + ownerData.getHellblockData().getIslandId()
							+ " in world " + world.worldName() + " bounds=" + bounds);
				}
			});
		});
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
	@NotNull
	private CompletableFuture<Void> setHellblockBiome(@NotNull HellblockWorld<?> world, @NotNull BoundingBox bounds,
			@NotNull Biome biome) {
		Objects.requireNonNull(biome, () -> "Unsupported biome: " + biome.toString());
		Objects.requireNonNull(world, "Cannot set biome of null world");
		Objects.requireNonNull(bounds, "Cannot set biome of null bounding box");

		Pos3 min = Pos3.toMinPos3(bounds, world.bukkitWorld().getMinHeight());
		Pos3 max = Pos3.toMaxPos3(bounds, world.bukkitWorld().getMaxHeight());

		return getHellblockChunks(world, bounds).thenCompose(chunkPositions -> setBiome(world, min, max, biome)
				.thenCompose(v -> instance.getScheduler().runSync(() -> {
					chunkPositions.forEach(pos -> world.bukkitWorld().refreshChunk(pos.x(), pos.z()));
				}))).exceptionally(ex -> {
					instance.getPluginLogger().warn("Failed to set Hellblock biome", ex);
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
	private CompletableFuture<Void> setBiome(@NotNull HellblockWorld<?> world, @NotNull Pos3 start, @NotNull Pos3 end,
			@NotNull Biome biome) {
		Objects.requireNonNull(start, "Start position cannot be null");
		Objects.requireNonNull(end, "End position cannot be null");
		Objects.requireNonNull(biome, () -> "Unsupported biome: " + biome.toString());
		Objects.requireNonNull(world, "Hellblock world cannot be null");

		final World bukkitWorld = world.bukkitWorld();
		final int minX = Math.min(start.x(), end.x());
		final int maxX = Math.max(start.x(), end.x());
		final int minZ = Math.min(start.z(), end.z());
		final int maxZ = Math.max(start.z(), end.z());
		final int sampleY = Math.max(bukkitWorld.getMinHeight(), bukkitWorld.getSeaLevel()); // use mid height

		// Sync biome changes safely
		return instance.getScheduler().supplySync(() -> {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					// Sample one Y-level per column to check current biome
					Biome current = bukkitWorld.getBiome(x, sampleY, z);
					if (current != biome) {
						// Set the entire column biome — Paper applies internally to all Y-levels
						for (int y = bukkitWorld.getMinHeight(); y < bukkitWorld.getMaxHeight(); y += 4) {
							bukkitWorld.setBiome(x, y, z, biome);
						}
					}
				}
			}
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
	private CompletableFuture<Set<ChunkPos>> getHellblockChunks(@NotNull HellblockWorld<?> world,
			@NotNull BoundingBox bounds) {
		Objects.requireNonNull(bounds, "Cannot get chunks of null bounding box");

		final CompletableFuture<Set<ChunkPos>> chunkData = new CompletableFuture<>();
		final Set<ChunkPos> chunkPositions = new HashSet<>();

		final int minChunkX = (int) bounds.getMinX() >> 4;
		final int maxChunkX = (int) bounds.getMaxX() >> 4;
		final int minChunkZ = (int) bounds.getMinZ() >> 4;
		final int maxChunkZ = (int) bounds.getMaxZ() >> 4;

		for (int x = minChunkX; x <= maxChunkX; x++) {
			for (int z = minChunkZ; z <= maxChunkZ; z++) {
				chunkPositions.add(ChunkPos.of(x, z));
			}
		}

		chunkData.complete(chunkPositions);
		return chunkData;
	}
}