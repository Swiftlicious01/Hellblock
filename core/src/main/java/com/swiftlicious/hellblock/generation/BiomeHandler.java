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
	public void changeHellblockBiome(@NotNull UserData ownerData, @NotNull HellBiome biome, boolean performedByGUI,
			boolean forceChange) {
		if (ownerData.isOnline()) {
			changeHellblockBiomeOnline(ownerData, biome, performedByGUI, forceChange);
		} else {
			changeHellblockBiomeOffline(ownerData, biome, forceChange);
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
	private void changeHellblockBiomeOnline(@NotNull UserData ownerData, @NotNull HellBiome biome,
			boolean performedByGUI, boolean forceChange) {
		final Player player = ownerData.getPlayer();
		final Sender audience = instance.getSenderFactory().wrap(player);
		final HellblockData data = ownerData.getHellblockData();

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
			instance.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
					+ player.getUniqueId() + "). This indicates corrupted data.");
			throw new IllegalStateException(
					"Owner reference was null. This should never happen — please report to the developer.");
		}
		if (!forceChange && !ownerId.equals(player.getUniqueId())) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
			return;
		}

		// Cooldown check
		if (!forceChange && data.getBiomeCooldown() > 0) {
			final String formatted = instance.getCooldownManager().getFormattedCooldown(data.getBiomeCooldown());
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_ON_COOLDOWN
							.arguments(AdventureHelper.miniMessageToComponent(formatted)).build()));
			return;
		}

		// Location check
		if (!forceChange) {
			final Location loc = player.getLocation();
			if (loc == null || !data.getBoundingBox().contains(loc.toVector())) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_MUST_BE_ON_ISLAND.build()));
				return;
			}
		}

		// Determine current biome (safe null handling)
		final Location homeLocation = data.getHomeLocation();
		final Biome currentBiome = (homeLocation != null) ? homeLocation.getBlock().getBiome() : null;

		// Prevent same-biome change
		if (currentBiome != null && currentBiome == biome.getConvertedBiome()) {
			audience.sendMessage(instance.getTranslationManager()
					.render(MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
							.arguments(
									AdventureHelper.miniMessageToComponent(StringUtils.toCamelCase(biome.toString())))
							.build()));
			return;
		}

		// GUI requirement check
		if (!forceChange && !performedByGUI) {
			final var requirements = instance.getBiomeGUIManager().getBiomeRequirements(biome);
			if (requirements != null && !RequirementManager.isSatisfied(Context.player(player), requirements)) {
				return; // silently fail
			}
		}

		// Fire pre-change event
		final HellblockBiomeChangeEvent changeEvent = new HellblockBiomeChangeEvent(ownerData, data, currentBiome,
				biome, performedByGUI, forceChange);
		if (EventUtils.fireAndCheckCancel(changeEvent)) {
			return;
		}

		// Apply the biome change
		applyHellblockBiomeChange(data, biome, !forceChange);

		// Success message
		audience.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_CHANGED
				.arguments(AdventureHelper.miniMessageToComponent(StringUtils.toCamelCase(biome.toString()))).build()));

		// Fire post-change event
		final HellblockBiomeChangedEvent changedEvent = new HellblockBiomeChangedEvent(ownerData, data, currentBiome,
				biome);
		EventUtils.fireAndForget(changedEvent);
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
	private void changeHellblockBiomeOffline(@NotNull UserData ownerData, @NotNull HellBiome biome,
			boolean forceChange) {
		final HellblockData data = ownerData.getHellblockData();

		if (!data.hasHellblock() || data.isAbandoned()) {
			return; // silently ignore for offline users
		}

		// Prevent same-biome change
		final Biome currentBiome = data.getHomeLocation().getBlock().getBiome();
		if (currentBiome == biome.getConvertedBiome()) {
			return; // nothing to do
		}

		final HellblockBiomeChangeEvent changeEvent = new HellblockBiomeChangeEvent(ownerData, data, currentBiome,
				biome, false, forceChange);
		if (EventUtils.fireAndCheckCancel(changeEvent)) {
			return;
		}

		// Apply the biome change (no messages, no cooldown unless forceChange is false)
		applyHellblockBiomeChange(data, biome, !forceChange);

		final HellblockBiomeChangedEvent changedEvent = new HellblockBiomeChangedEvent(ownerData, data, currentBiome,
				biome);
		EventUtils.fireAndForget(changedEvent);
	}

	/**
	 * Applies the biome change to the hellblock world and updates the
	 * HellblockData.
	 * 
	 * @param ownerData     The HellblockData representing the user's hellblock.
	 * @param biome         The new biome to set.
	 * @param applyCooldown If true, sets a cooldown on future biome changes.
	 */
	public void applyHellblockBiomeChange(@NotNull HellblockData ownerData, @NotNull HellBiome newBiome,
			boolean applyCooldown) {
		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(ownerData.getIslandId()));
		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
			throw new IllegalStateException("Hellblock world not found. Try regenerating the world.");
		}

		final HellblockWorld<?> world = worldOpt.get();

		BoundingBox bounds = ownerData.getBoundingBox();
		if (bounds == null) {
			throw new IllegalStateException(
					"Hellblock bounding box returned null for islandID=" + ownerData.getIslandId());
		}

		// --- Capture the previous biome BEFORE changing ---
		HellBiome previousBiome = Objects.requireNonNullElse(ownerData.getBiome(), HellBiome.NETHER_WASTES);

		setHellblockBiome(world, bounds, newBiome.getConvertedBiome());

		if (applyCooldown) {
			ownerData.setBiomeCooldown(TimeUnit.DAYS.toSeconds(1)); // 24h cooldown
		}

		instance.getScheduler().executeSync(() -> {
			// fortress logic
			if (newBiome == HellBiome.NETHER_FORTRESS) {
				VersionHelper.getNMSManager().injectFakeFortress(world.bukkitWorld(), bounds);
				instance.debug("Injected fake fortress for islandID=" + ownerData.getIslandId() + " in world "
						+ world.worldName() + " bounds=" + bounds);
			} else if (previousBiome == HellBiome.NETHER_FORTRESS) {
				// Only remove the fake fortress if the *previous* biome had one
				VersionHelper.getNMSManager().removeFakeFortress(world.bukkitWorld(), bounds);
				instance.debug("Removed fake fortress for islandID=" + ownerData.getIslandId() + " in world "
						+ world.worldName() + " bounds=" + bounds);
			}
		});

		// Finally, update the biome in the player data
		ownerData.setBiome(newBiome);
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
	public void setHellblockBiome(@NotNull HellblockWorld<?> world, @NotNull BoundingBox bounds, @NotNull Biome biome) {
		Objects.requireNonNull(biome, () -> "Unsupported biome: " + biome.toString());
		Objects.requireNonNull(world, "Cannot set biome of null world");
		Objects.requireNonNull(bounds, "Cannot set biome of null bounding box");

		getHellblockChunks(world, bounds).thenAccept(chunkPositions -> {
			Pos3 min = Pos3.toMinPos3(bounds, world.bukkitWorld().getMinHeight());
			Pos3 max = Pos3.toMaxPos3(bounds, world.bukkitWorld().getMaxHeight());

			// Refresh chunks in Bukkit world
			setBiome(world, min, max, biome)
					.thenRun(() -> chunkPositions.forEach(pos -> world.bukkitWorld().refreshChunk(pos.x(), pos.z())));
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
	public CompletableFuture<Void> setBiome(@NotNull HellblockWorld<?> world, @NotNull Pos3 start, @NotNull Pos3 end,
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

		return CompletableFuture.runAsync(() -> {
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
		}).exceptionally(ex -> {
			ex.printStackTrace();
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
	public CompletableFuture<Set<ChunkPos>> getHellblockChunks(@NotNull HellblockWorld<?> world,
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