package com.swiftlicious.hellblock.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.ConfigManager.TitleScreenInfo;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.events.hellblock.HellblockAbandonEvent;
import com.swiftlicious.hellblock.events.hellblock.HellblockCreateEvent;
import com.swiftlicious.hellblock.events.hellblock.HellblockPostCreationEvent;
import com.swiftlicious.hellblock.events.hellblock.HellblockResetEvent;
import com.swiftlicious.hellblock.events.hellblock.HellblockRollbackEvent;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.mechanics.fishing.CustomFishingHook;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.InvasionData;
import com.swiftlicious.hellblock.player.SkysiegeData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.VisitData;
import com.swiftlicious.hellblock.player.WitherData;
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;
import com.swiftlicious.hellblock.player.mailbox.MailboxFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.protection.IslandProtection;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.schematic.EntitySnapshot;
import com.swiftlicious.hellblock.schematic.IslandSnapshot;
import com.swiftlicious.hellblock.schematic.IslandSnapshotBlock;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.world.BlockPos;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.CustomBlock;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.CustomBlockTypes;
import com.swiftlicious.hellblock.world.CustomChunk;
import com.swiftlicious.hellblock.world.CustomSection;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;
import com.swiftlicious.hellblock.world.WorldManager;

import net.kyori.adventure.text.Component;

/**
 * Handles the full lifecycle and core logic for managing Hellblock islands.
 *
 * <p>
 * This class serves as the central handler for all island-related operations,
 * including:
 * <ul>
 * <li>Creation and initialization of new Hellblock islands</li>
 * <li>Island resets (both voluntary and forced) with full rollback support</li>
 * <li>Management of Hellblock IDs, world preparation, and spawn location
 * logic</li>
 * <li>Post-generation and safety tasks such as teleportation and biome
 * setup</li>
 * <li>Visitor handling and ban enforcement on foreign islands</li>
 * <li>Snapshot creation and rollback to restore island states</li>
 * <li>Periodic purging of inactive or abandoned islands</li>
 * <li>Safe teleportation and spawn fallback mechanisms</li>
 * </ul>
 *
 * <p>
 * All major operations are designed to be thread-safe, leveraging asynchronous
 * and scheduled tasks where necessary to ensure consistent world state and
 * player experience.
 * 
 * <p>
 * Debug logging is extensively used throughout to assist with operational
 * diagnostics, rollback audits, and identifying edge-case failures.
 */
public class HellblockHandler implements Reloadable {

	protected final HellblockPlugin instance;

	private final Object idLock = new Object(); // Lock object for thread safety
	private int cachedMaxId = -1;
	private long lastCacheUpdate = 0;
	private volatile long lastInvalidationTime = 0;
	private static final long INVALIDATION_COOLDOWN_MS = 5000; // 5 seconds
	private static final long CACHE_EXPIRY_MS = TimeUnit.SECONDS.toMillis(30);

	private final Set<Integer> reservedIds = ConcurrentHashMap.newKeySet();

	protected final Set<UUID> creationPlayers = ConcurrentHashMap.newKeySet();
	protected final Set<UUID> resetPlayers = ConcurrentHashMap.newKeySet();

	private final Map<GenerationProfile, Long> lastKnownDurations = new ConcurrentHashMap<>();

	public HellblockHandler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void disable() {
		creationPlayers.clear();
		resetPlayers.clear();
	}

	/**
	 * Creates a new Hellblock island for the specified player with the given
	 * options.
	 *
	 * @param ownerData    The player for whom the island is being created
	 * @param islandChoice The type of island to create (e.g., SCHEMATIC, DEFAULT)
	 * @param schematic    Optional schematic name if using a schematic island
	 * @param isReset      Whether this creation is part of a reset operation
	 * @return A CompletableFuture that completes when the island creation process
	 *         is finished
	 */
	public CompletableFuture<Void> createHellblock(@NotNull UserData ownerData, @NotNull IslandOptions islandChoice,
			@Nullable String schematic, boolean isReset) {
		return instance.getPlacementDetector().checkSpiralPlacementCompletion().thenCompose(ignored -> {
			final String playerName = ownerData.getName();
			final UUID playerUUID = ownerData.getUUID();

			GenerationProfile profile = new GenerationProfile(islandChoice,
					!instance.getConfigManager().disableGenerationAnimation(), isReset,
					instance.getConfigManager().perPlayerWorlds());

			Long predictedTime = lastKnownDurations.get(profile);

			Player player = ownerData.getPlayer();
			if (player != null && player.isOnline()) {
				instance.getSenderFactory().wrap(player)
						.sendMessage(
								instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_CREATE_PROCESS
												.arguments(
														AdventureHelper.miniMessageToComponent(
																String.valueOf((predictedTime != null ? predictedTime
																		: estimateCreationTimeSeconds(islandChoice,
																				schematic, isReset,
																				instance.getConfigManager()
																						.perPlayerWorlds())))))
												.build()));
			}

			long startTime = System.nanoTime();

			instance.debug("createHellblock: Starting island creation for " + playerName + " (UUID=" + playerUUID
					+ "), choice=" + islandChoice.name() + ", schematic=" + (schematic != null ? schematic : "none")
					+ ", isReset=" + isReset);

			AtomicInteger trackedIslandId = new AtomicInteger(-1);

			// Step 1: Generate the hellblock ID
			return resolveHellblockID(ownerData).thenCompose(islandId -> {
				if (islandId <= 0) {
					instance.getPluginLogger()
							.severe("createHellblock: Failed to retrieve valid island ID for " + playerName);
					return CompletableFuture.completedFuture(null);
				}

				instance.debug("createHellblock: Resolved island ID " + islandId + " for " + playerName);
				trackedIslandId.set(islandId);

				final HellblockCreateEvent createEvent = new HellblockCreateEvent(ownerData, islandChoice, islandId);
				if (EventUtils.fireAndCheckCancel(createEvent)) {
					instance.getPluginLogger()
							.warn("createHellblock: Island creation cancelled by event for " + playerName);
					reservedIds.remove(islandId);
					return CompletableFuture.completedFuture(null);
				}

				// Step 2: Prepare the world
				return prepareWorld(islandId).thenCompose(world -> {
					if (world == null) {
						instance.getPluginLogger().severe(
								"createHellblock: World couldn't be prepared and returned null for " + playerName);
						reservedIds.remove(islandId);
						return CompletableFuture.completedFuture(null);
					}

					startCreationProcess(playerUUID);

					instance.debug(
							"createHellblock: World '" + world.worldName() + "' prepared for island ID = " + islandId);

					// Step 3: Resolve island location
					return resolveIslandLocation(world, ownerData, isReset).thenCompose(islandLoc -> {
						instance.debug("createHellblock: Island location resolved for " + playerName + " at [world="
								+ islandLoc.getWorld().getName() + ", x=" + islandLoc.getBlockX() + ", y="
								+ islandLoc.getBlockY() + ", z=" + islandLoc.getBlockZ() + "]");

						// Step 4: Initialize Hellblock data
						return initializeHellblockData(ownerData, islandChoice, islandId, islandLoc).thenCompose(v -> {
							instance.debug("createHellblock: HellblockData initialized for " + playerName
									+ " (Island ID=" + islandId + ")");

							// Step 5: Protect and generate the island
							return protectAndGenerateIsland(world, ownerData, islandChoice, schematic, islandLoc,
									isReset).thenCompose(vv -> {

										// Step 6: Save initialized user data before proceeding
										return instance.getStorageManager().saveUserData(ownerData, false)
												.thenRun(() -> instance.getStorageManager()
														.invalidateCachedUserData(ownerData.getUUID()))
												.thenCompose(vvv -> {
													instance.debug("createHellblock: Saved initialized user data for "
															+ playerName);

													// Step 7: Run post-generation tasks
													return postGenerationTasks(ownerData, world, isReset)
															.thenCompose(vvvv -> {
																instance.debug(
																		"createHellblock: Post-generation tasks completed for "
																				+ playerName);

																long duration = TimeUnit.NANOSECONDS
																		.toSeconds(System.nanoTime() - startTime);
																lastKnownDurations.put(profile, duration);

																instance.debug(
																		"createHellblock: Generation for profile "
																				+ profile + " took " + duration
																				+ " seconds.");

																// Cleanup
																reservedIds.remove(islandId);
																invalidateHellblockIDCache();
																endCreationProcess(playerUUID);

																// Recalculate level
																instance.debug(
																		"createHellblock: Scheduling level recalculation for ID "
																				+ islandId);

																return instance.getScheduler()
																		.callSync(() -> instance.getIslandLevelManager()
																				.recalculateIslandLevel(islandId))
																		.thenAccept(level -> {
																			instance.debug(
																					"createHellblock: Recalculated island level for ID "
																							+ islandId + " to "
																							+ level);
																		}).exceptionally(ex -> {
																			instance.getPluginLogger().warn(
																					"Failed to recalculate level: "
																							+ ex.getMessage());
																			return null;
																		});
															});
												});
									});
						});
					});
				});
			}).exceptionally(ex -> {
				int reserved = trackedIslandId.get();
				if (reserved > 0) {
					reservedIds.remove(reserved);
				}
				endCreationProcess(playerUUID);

				if (ex instanceof CompletionException && ex.getCause() instanceof IllegalStateException) {
					instance.getPluginLogger()
							.warn("createHellblock: Aborted due to unready spiral placement: " + ex.getMessage());
				} else {
					instance.getPluginLogger().severe("createHellblock: Unhandled exception: " + ex.getMessage(), ex);
				}

				return null;
			});
		}).exceptionally(ex -> {
			instance.getPluginLogger().severe("Spiral placement not ready. Aborting island creation.", ex);
			return null; // fail gracefully
		});
	}

	/**
	 * Overloaded method to create a Hellblock island without a schematic.
	 *
	 * @param ownerData    The player for whom the island is being created
	 * @param islandChoice The type of island to create (e.g., SCHEMATIC, DEFAULT)
	 * @param isReset      Whether this creation is part of a reset operation
	 * @return A CompletableFuture that completes when the island creation process
	 *         is finished
	 */
	public CompletableFuture<Void> createHellblock(@NotNull UserData ownerData, @NotNull IslandOptions islandChoice,
			boolean isReset) {
		return createHellblock(ownerData, islandChoice, null, isReset);
	}

	/**
	 * Resolves the Hellblock island ID for the given user. If the user already has
	 * an assigned island ID, it is returned. Otherwise, a new unique island ID is
	 * generated.
	 *
	 * @param ownerData The user data for whom to resolve the island ID
	 * @return A CompletableFuture that resolves to the island ID
	 */
	@NotNull
	private CompletableFuture<Integer> resolveHellblockID(@NotNull UserData ownerData) {
		int existingId = ownerData.getHellblockData().getIslandId();

		if (existingId > 0) {
			instance.debug("resolveHellblockID: Found existing island ID = " + existingId + " for user "
					+ ownerData.getUUID());
			return CompletableFuture.completedFuture(existingId);
		} else {
			instance.debug("resolveHellblockID: No existing island ID found for user " + ownerData.getUUID()
					+ ", generating new one...");
			return nextHellblockID().thenApply(newId -> {
				instance.debug(
						"resolveHellblockID: Generated new island ID = " + newId + " for user " + ownerData.getUUID());
				return newId;
			});
		}
	}

	/**
	 * Prepares the world for the hellblock island creation.
	 *
	 * @param islandId the ID of the hellblock island.
	 * @return A CompletableFuture that resolves the world.
	 */
	@NotNull
	private CompletableFuture<? extends HellblockWorld<?>> prepareWorld(int islandId) {
		String worldName = instance.getWorldManager().getHellblockWorldFormat(islandId);
		instance.debug("prepareWorld: Preparing world for island ID = " + islandId + ", world name = " + worldName);

		return instance.getWorldManager().adapter().createWorld(worldName).handle((world, ex) -> {
			if (ex != null) {
				throw new CompletionException(new IllegalStateException(
						"Failed to create/load world '" + worldName + "' for island ID " + islandId, ex));
			}
			if (world == null) {
				throw new CompletionException(new IllegalStateException(
						"World creation returned null for island ID " + islandId + " (" + worldName + ")"));
			}
			instance.debug(
					"prepareWorld: Successfully created/loaded world '" + worldName + "' for island ID = " + islandId);
			return world;
		});
	}

	/**
	 * Determines the spawn location for a Hellblock island based on the world type
	 * and user data.
	 *
	 * @param world    The Hellblock world where the island is located
	 * @param userData The user data associated with the island
	 * @return The calculated spawn Location for the island
	 */
	@NotNull
	public Location determineSpawnLocation(@NotNull HellblockWorld<?> world, @NotNull UserData userData) {
		World bukkitWorld = world.bukkitWorld();
		if (bukkitWorld == null) {
			throw new IllegalStateException(
					"Cannot determine spawn location: Bukkit world is null for " + world.worldName());
		}

		// Per-player world → default (0, height, 0)
		if (instance.getConfigManager().perPlayerWorlds()) {
			instance.getWorldManager().markWorldAccess(world.worldName());
			return new Location(bukkitWorld, 0.5, instance.getConfigManager().height(), 0.5);
		}

		// Shared world → compute center of bounding box
		HellblockData data = userData.getHellblockData();
		BoundingBox box = data.getBoundingBox();

		if (box == null) {
			instance.getPluginLogger()
					.severe("BoundingBox is null for user " + userData.getName() + " (" + userData.getUUID() + ")");
			return new Location(bukkitWorld, 0.5, instance.getConfigManager().height(), 0.5); // fallback spawn
		}

		double centerX = (box.getMinX() + box.getMaxX()) / 2.0 + 0.5;
		double centerZ = (box.getMinZ() + box.getMaxZ()) / 2.0 + 0.5;
		double height = instance.getConfigManager().height();

		return new Location(bukkitWorld, centerX, height, centerZ);
	}

	/**
	 * Resolves the island location for a Hellblock island based on the world type,
	 * user data, and reset status.
	 *
	 * @param world     The Hellblock world where the island is located
	 * @param ownerData The user data associated with the island
	 * @param isReset   Whether this resolution is part of a reset operation
	 * @return A CompletableFuture that resolves to the calculated island Location
	 */
	@NotNull
	public CompletableFuture<Location> resolveIslandLocation(@NotNull HellblockWorld<?> world,
			@NotNull UserData ownerData, boolean isReset) {
		World bukkitWorld = world.bukkitWorld();
		if (bukkitWorld == null) {
			throw new IllegalStateException(
					"Cannot determine island location: Bukkit world is null for " + world.worldName());
		}

		String userName = ownerData.getName();
		UUID userId = ownerData.getUUID();

		if (instance.getConfigManager().perPlayerWorlds()) {
			instance.debug("resolveIslandLocation: Per-player world enabled for " + userName
					+ ". Using fixed spawn at Y=" + instance.getConfigManager().height());
			return CompletableFuture
					.completedFuture(new Location(bukkitWorld, 0, instance.getConfigManager().height(), 0));
		}

		if (isReset) {
			BoundingBox preserved = ownerData.getHellblockData().getPreservedBoundingBox();
			if (preserved != null) {
				double centerX = (preserved.getMinX() + preserved.getMaxX()) / 2.0 + 0.5;
				double centerZ = (preserved.getMinZ() + preserved.getMaxZ()) / 2.0 + 0.5;
				double height = instance.getConfigManager().height();
				instance.debug("resolveIslandLocation: Reset requested for " + userName
						+ ". Using preserved location at (" + centerX + ", " + height + ", " + centerZ + ")");
				return CompletableFuture.completedFuture(new Location(bukkitWorld, centerX, height, centerZ));
			} else {
				instance.getPluginLogger()
						.warn("resolveIslandLocation: Preserved bounding box is null during reset for user " + userName
								+ " (" + userId + ")");
			}
		}

		instance.debug("resolveIslandLocation: No preserved location for " + userName
				+ ". Finding next available island location using spiral placement...");
		return instance.getPlacementDetector().findNextIslandLocation().thenApply(loc -> {
			instance.debug("resolveIslandLocation: Assigned new island location for " + userName + " at [world="
					+ loc.getWorld().getName() + ", x=" + loc.getBlockX() + ", y=" + loc.getBlockY() + ", z="
					+ loc.getBlockZ() + "]");
			return loc;
		}).orTimeout(10, TimeUnit.SECONDS).thenApply(loc -> {
			if (loc == null)
				throw new IllegalStateException("Placement detector returned null location");
			return loc;
		});
	}

	/**
	 * Initializes the HellblockData for a user when creating a new island.
	 *
	 * @param ownerData The user data to initialize
	 * @param choice    The island option selected by the user
	 * @param islandId  The unique island ID assigned to the user
	 * @param location  The spawn location of the new island
	 */
	private CompletableFuture<Void> initializeHellblockData(@NotNull UserData ownerData, @NotNull IslandOptions choice,
			int islandId, @NotNull Location location) {
		return CompletableFuture.runAsync(() -> {
			instance.debug("initializeHellblockData: Initializing HellblockData for " + ownerData.getName() + " (UUID="
					+ ownerData.getUUID() + "), Island ID=" + islandId + " at location [world="
					+ location.getWorld().getName() + ", x=" + location.getBlockX() + ", y=" + location.getBlockY()
					+ ", z=" + location.getBlockZ() + "], choice=" + choice.name());

			final HellblockData hellblockData = ownerData.getHellblockData();
			hellblockData.setDefaultHellblockData(true, location, islandId);
			hellblockData.setIslandChoice(choice);
			hellblockData.setBiomeCooldown(0L);
			hellblockData.setLockedStatus(false);
			hellblockData.setOwnerUUID(ownerData.getUUID());
			hellblockData.setProtectionFlags(new EnumMap<>(FlagType.class));
			hellblockData.setIslandUpgrades(new EnumMap<>(IslandUpgradeType.class));
			hellblockData.setDefaultUpgradeTiers();
			hellblockData.setPartyMembers(new HashSet<>());
			hellblockData.setTrustedMembers(new HashSet<>());
			hellblockData.setBannedMembers(new HashSet<>());
			hellblockData.setInvasionData(new InvasionData());
			hellblockData.setWitherData(new WitherData());
			hellblockData.setSkysiegeData(new SkysiegeData());

			VisitData visitData = new VisitData();
			visitData.reset();
			hellblockData.setVisitData(visitData);
			hellblockData.setRecentVisitors(new ArrayList<>());

			instance.debug("initializeHellblockData: Initialization complete for " + ownerData.getName()
					+ " (Island ID=" + islandId + ")");
		});
	}

	/**
	 * Protects the Hellblock island area and generates the island based on the
	 * selected options.
	 *
	 * @param world     The Hellblock world where the island is located
	 * @param ownerData The user data associated with the island
	 * @param choice    The island option selected by the user
	 * @param schematic Optional schematic name if using a schematic island
	 * @param location  The spawn location of the new island
	 * @param isReset   Whether this generation is part of a reset operation
	 * @return A CompletableFuture that completes when protection and generation are
	 *         finished
	 */
	private CompletableFuture<Void> protectAndGenerateIsland(@NotNull HellblockWorld<?> world,
			@NotNull UserData ownerData, @NotNull IslandOptions choice, @Nullable String schematic,
			@NotNull Location location, boolean isReset) {
		final HellblockData hellblockData = ownerData.getHellblockData();

		instance.debug("protectAndGenerateIsland: Starting protection and island generation for player "
				+ ownerData.getName() + " (UUID=" + ownerData.getUUID() + "), World=" + world.worldName() + ", Choice="
				+ choice.name() + ", Schematic=" + (schematic != null ? schematic : "none") + ", Reset=" + isReset
				+ ", Location=[" + location.getWorld().getName() + ", " + location.getBlockX() + ", "
				+ location.getBlockY() + ", " + location.getBlockZ() + "]");

		return instance.getProtectionManager().getIslandProtection().protectHellblock(world, ownerData)
				.thenCompose(v -> {
					instance.debug("protectAndGenerateIsland: Protection setup complete for " + ownerData.getName()
							+ " in world '" + world.worldName() + "'.");

					CompletableFuture<Void> genTask;
					boolean hasSchematic = choice == IslandOptions.SCHEMATIC && schematic != null
							&& !schematic.isEmpty() && instance.getSchematicManager().schematicFiles.keySet().stream()
									.anyMatch(sch -> sch.equalsIgnoreCase(schematic));

					if (hasSchematic) {
						String matchedSchematic = instance.getSchematicManager().schematicFiles.keySet().stream()
								.filter(sch -> sch.equalsIgnoreCase(schematic)).findFirst().orElse(schematic);
						instance.debug("protectAndGenerateIsland: Valid schematic detected: " + matchedSchematic
								+ ". Generating island from schematic...");

						hellblockData.setUsedSchematic(matchedSchematic);
						genTask = instance.getIslandChoiceConverter().convertIslandChoice(world, ownerData, location,
								matchedSchematic);
					} else {
						if (choice == IslandOptions.SCHEMATIC) {
							instance.getPluginLogger()
									.warn("protectAndGenerateIsland: Requested schematic '" + schematic
											+ "' not found or invalid. Falling back to default generation for player "
											+ ownerData.getName());
						} else {
							instance.debug("protectAndGenerateIsland: Using default generation for "
									+ ownerData.getName() + " (choice=" + choice.name() + ")");
						}

						genTask = instance.getIslandChoiceConverter().convertIslandChoice(world, ownerData, location);
					}

					return genTask.thenRun(() -> instance
							.debug("protectAndGenerateIsland: Island generation successfully completed for "
									+ ownerData.getName() + " (Schematic=" + (hasSchematic ? schematic : "none")
									+ ")"));
				}).exceptionally(ex -> {
					instance.getPluginLogger()
							.severe("protectAndGenerateIsland: Failed to generate island for player "
									+ ownerData.getName() + " (UUID=" + ownerData.getUUID() + "): " + ex.getMessage(),
									ex);
					return null;
				});
	}

	/**
	 * Estimates the time (in seconds) it will take to create a new Hellblock island
	 * based on the selected island type, schematic name and reset flag.
	 * 
	 * @param option         The island type (SCHEMATIC, DEFAULT, etc.)
	 * @param schematic      The schematic name, or null if not used
	 * @param isReset        Whether this is a reset operation
	 * @param perPlayerWorld Whether per-player world generation is enabled
	 * 
	 * @return Estimated time in seconds for island creation
	 */
	private long estimateCreationTimeSeconds(@NotNull IslandOptions option, @Nullable String schematic, boolean isReset,
			boolean perPlayerWorld) {
		// Minimal setup (loading config, ID, etc.)
		long baseTime = 10;

		// Additional time if per-player worlds are used (world creation is async and
		// slower)
		if (perPlayerWorld) {
			baseTime += 5;
		}

		// Estimate based on island type
		switch (option) {
		case SCHEMATIC -> {
			baseTime += 5;
			if (schematic != null && !schematic.isEmpty()) {
				String lower = schematic.toLowerCase();
				if (lower.contains("mega")) {
					baseTime += 8;
				} else if (lower.contains("huge") || lower.contains("large")) {
					baseTime += 5;
				} else {
					baseTime += 2; // default schematic size
				}
			}
		}
		case DEFAULT, CLASSIC -> baseTime += 3;
		default -> baseTime += 4;
		}

		// Extra time for post-generation tasks on reset (teleport, recalculation, biome
		// setup, etc.)
		if (isReset) {
			baseTime += 4;
		}

		return baseTime;
	}

	/**
	 * Performs post-generation tasks after the Hellblock island has been created,
	 * including safety checks, teleportation, and final setup.
	 *
	 * @param ownerData The user data associated with the island
	 * @param world     The Hellblock world where the island is located
	 * @param isReset   Whether this is part of a reset operation
	 * @return A CompletableFuture that completes when all post-generation tasks are
	 *         finished
	 */
	private CompletableFuture<Void> postGenerationTasks(@NotNull UserData ownerData, @NotNull HellblockWorld<?> world,
			boolean isReset) {
		final HellblockData hellblockData = ownerData.getHellblockData();
		Location homeLocation = hellblockData.getHomeLocation();

		if (homeLocation == null) {
			throw new NullPointerException("postGenerationTasks: Hellblock home location is null for "
					+ ownerData.getName() + ". Please report this to the developer.");
		}

		instance.debug("postGenerationTasks: Starting for " + ownerData.getName() + " (UUID=" + ownerData.getUUID()
				+ "), " + "World=" + world.worldName() + ", isReset=" + isReset);

		// Step 1: Ensure the home location is initially safe; fallback if not
		return LocationUtils.isSafeLocationAsync(homeLocation).thenCompose(isSafe -> {
			if (!isSafe) {
				instance.debug("postGenerationTasks: Unsafe home location for " + ownerData.getName()
						+ ". Searching for fallback bedrock.");
				return locateNearestBedrock(ownerData).thenAccept(newSafeLocation -> {
					if (newSafeLocation == null) {
						throw new IllegalStateException("Bedrock search came back null.");
					}
					instance.debug(
							"postGenerationTasks: Fallback bedrock location found at [" + newSafeLocation.getBlockX()
									+ ", " + newSafeLocation.getBlockY() + ", " + newSafeLocation.getBlockZ() + "]");
					hellblockData.setHomeLocation(newSafeLocation); // temporarily update before safe check
				});
			}
			return CompletableFuture.completedFuture(null);
		})

				// Step 2: Recheck for solid ground and fallback if necessary
				.thenCompose(v -> waitForSafeSpawn(ownerData, 10))

				.thenCompose(checkedSafeLocation -> {
					instance.debug("postGenerationTasks: Final safe spawn resolved for " + ownerData.getName() + " at ["
							+ checkedSafeLocation.getBlockX() + ", " + checkedSafeLocation.getBlockY() + ", "
							+ checkedSafeLocation.getBlockZ() + "]");

					// Set final home location here, once
					hellblockData.setHomeLocation(checkedSafeLocation);

					if (instance.getConfigManager().perPlayerWorlds()) {
						world.bukkitWorld().setSpawnLocation(checkedSafeLocation);
					}

					// Step 3: Teleport player
					return teleportPlayerToHome(ownerData);
				})

				// Step 4: Post-teleport tasks
				.thenCompose(vv -> {
					long creationTime = System.currentTimeMillis();
					hellblockData.setCreationTime(creationTime);

					instance.debug(
							"postGenerationTasks: Running final setup for " + ownerData.getName() + ". Island ID="
									+ hellblockData.getIslandId() + ", Biome=" + hellblockData.getBiome().name());

					// Start farming, borders, biome, etc.
					instance.getIslandManager().handleIslandCreation(ownerData.getUUID(), hellblockData.getIslandId());
					if (ownerData.getPlayer() != null && ownerData.getPlayer().isOnline())
						instance.getBorderHandler().startBorderTask(ownerData.getUUID());
					instance.getBiomeHandler().applyHellblockBiomeChange(hellblockData, hellblockData.getBiome(),
							false);
					instance.getCoopManager().validateCachedOwnerData(ownerData.getUUID());

					instance.debug("postGenerationTasks: Biome set for " + ownerData.getName() + "'s island in world '"
							+ world.worldName() + "' to " + hellblockData.getBiome().toString() + ".");

					// Reset logic
					if (isReset) {
						instance.debug(
								"postGenerationTasks: Applying reset cooldown and clearing preserved bounding box for "
										+ ownerData.getName());
						hellblockData.setResetCooldown(TimeUnit.DAYS.toSeconds(1)); // 1 day
						hellblockData.setPreservedBoundingBox(null);
					}

					// Cleanup ghost liquids if any
					return cleanupGhostLiquids(world, ownerData.getHellblockData().getIslandId()).thenCompose(vvv -> {

						// Fire event for plugins
						final HellblockPostCreationEvent postCreationEvent = new HellblockPostCreationEvent(ownerData);
						EventUtils.fireAndForget(postCreationEvent);

						instance.debug("postGenerationTasks: HellblockPostCreationEvent dispatched for "
								+ ownerData.getName());

						instance.getCoopManager().removeNonOwnerUUID(ownerData.getUUID());

						instance.debug("postGenerationTasks: Added " + ownerData.getName()
								+ " as an owner in future cache checks (Island ID="
								+ ownerData.getHellblockData().getIslandId() + ")");

						return instance.getStorageManager().getDataSource()
								.updatePlayerData(ownerData.getUUID(), ownerData.toPlayerData(), true).thenRun(() -> {
									instance.getStorageManager().invalidateCachedUserData(ownerData.getUUID());
									if (instance.getConfigManager().perPlayerWorlds()) {
										World bukkitWorld = world.bukkitWorld();
										if (bukkitWorld != null && bukkitWorld.getPlayers().isEmpty()) {
											instance.debug(
													"postGenerationTasks: Scheduling idle unload for newly created per-player world: "
															+ world.worldName());
											instance.getScheduler().sync().runLater(() -> {
												if (bukkitWorld.getPlayers().isEmpty()) {
													instance.getWorldManager().unloadWorld(bukkitWorld, false);
													Bukkit.unloadWorld(bukkitWorld, true);
													instance.debug(
															"postGenerationTasks: Unloaded inactive newly created world: "
																	+ world.worldName());
												}
											}, 20L * 120L, homeLocation); // check again after 2 minutes
										}
									}
								});
					});
				});
	}

	/**
	 * Waits asynchronously until the specified home location is considered safe for
	 * spawning, checking the ground block below the location.
	 *
	 * @param ownerData The owner of the hellblock island
	 * @param maxTries  The maximum number of attempts to check for safety
	 * @return A CompletableFuture that completes when the location is safe or max
	 *         tries are reached and returns said safe Location
	 */
	@NotNull
	private CompletableFuture<Location> waitForSafeSpawn(@NotNull UserData ownerData, int maxTries) {
		CompletableFuture<Location> future = new CompletableFuture<>();
		AtomicInteger attempts = new AtomicInteger(0);
		HellblockData hellblockData = ownerData.getHellblockData();
		Location safeCheckLoc = hellblockData.getHomeLocation().clone();
		World world = safeCheckLoc.getWorld();

		instance.debug("waitForSafeSpawn: Starting ground check at [world=" + (world != null ? world.getName() : "null")
				+ ", x=" + safeCheckLoc.getBlockX() + ", y=" + safeCheckLoc.getBlockY() + ", z="
				+ safeCheckLoc.getBlockZ() + "] for maxTries=" + maxTries);

		AtomicReference<SchedulerTask> scheduledRef = new AtomicReference<>();

		SchedulerTask scheduledTask = instance.getScheduler().sync().runRepeating(() -> {
			if (safeCheckLoc.getWorld() == null) {
				instance.getPluginLogger().warn("waitForSafeSpawn: Home world is null, cannot verify ground safety.");
				future.completeExceptionally(new IllegalStateException("Home world is null."));
				SchedulerTask scheduled = scheduledRef.get();
				if (scheduled != null && !scheduled.isCancelled()) {
					scheduled.cancel();
				}
				return;
			}

			Block blockBelow = safeCheckLoc.getWorld().getBlockAt(safeCheckLoc.getBlockX(),
					safeCheckLoc.getBlockY() - 1, safeCheckLoc.getBlockZ());

			if (blockBelow.getType().isSolid()) {
				instance.debug("waitForSafeSpawn: Solid ground detected on attempt #" + (attempts.get() + 1));
				future.complete(safeCheckLoc);
				SchedulerTask scheduled = scheduledRef.get();
				if (scheduled != null && !scheduled.isCancelled()) {
					scheduled.cancel();
				}
				return;
			}

			if (attempts.incrementAndGet() >= maxTries) {
				instance.getPluginLogger().warn("waitForSafeSpawn: Failed to detect solid ground after " + maxTries
						+ " attempts at location [x=" + safeCheckLoc.getBlockX() + ", y=" + safeCheckLoc.getBlockY()
						+ ", z=" + safeCheckLoc.getBlockZ() + "]. Falling back to bedrock search.");

				locateNearestBedrock(ownerData).thenAccept(fallbackLoc -> {
					if (fallbackLoc != null) {
						instance.getPluginLogger()
								.info("waitForSafeSpawn: Fallback safe location found at [x=" + fallbackLoc.getBlockX()
										+ ", y=" + fallbackLoc.getBlockY() + ", z=" + fallbackLoc.getBlockZ() + "]");
						hellblockData.setHomeLocation(fallbackLoc);
						future.complete(fallbackLoc);
					} else {
						future.completeExceptionally(new IllegalStateException("Bedrock search came back null."));
					}
				});
				SchedulerTask scheduled = scheduledRef.get();
				if (scheduled != null && !scheduled.isCancelled()) {
					scheduled.cancel();
				}
			}
		}, 1L, 2L, safeCheckLoc); // Start after 1 tick, check every 2 ticks

		scheduledRef.set(scheduledTask);

		return future;
	}

	/**
	 * Teleports the specified player to their Hellblock home location
	 * asynchronously.
	 *
	 * @param ownerData The UserData containing the home location
	 * @return A CompletableFuture that resolves to true if teleportation was
	 *         successful, false otherwise
	 */
	@NotNull
	public CompletableFuture<Boolean> teleportPlayerToHome(@NotNull UserData ownerData) {
		Player player = ownerData.getPlayer();
		HellblockData data = ownerData.getHellblockData();
		Location homeLocation = data.getHomeLocation();

		if (homeLocation.getWorld() == null) {
			instance.getPluginLogger().warn("teleportPlayerToHome: Home location world is null for "
					+ ownerData.getName() + ". Teleport skipped.");
			return CompletableFuture.completedFuture(false);
		}

		if (player == null || !player.isOnline()) {
			instance.debug("teleportPlayerToHome: Player " + ownerData.getName() + " is offline. Queueing teleport.");
			instance.getMailboxManager().queueMailbox(ownerData.getUUID(), MailboxFlag.QUEUE_TELEPORT_HOME);
			return CompletableFuture.completedFuture(false);
		}

		instance.debug("teleportPlayerToHome: Teleporting player " + ownerData.getName() + " to home at [world="
				+ homeLocation.getWorld().getName() + ", x=" + homeLocation.getBlockX() + ", y="
				+ homeLocation.getBlockY() + ", z=" + homeLocation.getBlockZ() + "]");

		return ChunkUtils.teleportAsync(player, homeLocation, TeleportCause.PLUGIN).thenApply(success -> {
			if (success) {
				World world = homeLocation.getWorld();
				if (world != null) {
					instance.getWorldManager().markWorldAccess(world.getName());
					instance.debug("teleportPlayerToHome: Teleport successful for " + ownerData.getName()
							+ ". World access marked: " + world.getName());
				}
			} else {
				instance.getPluginLogger().warn("teleportPlayerToHome: Teleport failed for " + ownerData.getName());
			}
			return success;
		});
	}

	/**
	 * Displays a creation title and plays a sound effect to the player upon
	 * successful Hellblock island creation.
	 *
	 * @param ownerData The user data of the player to notify
	 */
	public void showCreationTitleAndSound(@NotNull UserData ownerData) {
		String playerName = ownerData.getName();
		Player player = ownerData.getPlayer();

		if (player == null || !player.isOnline()) {
			instance.debug("showCreationTitleAndSound: Player " + playerName + " is offline — skipping title/sound.");
			instance.getMailboxManager().queueMailbox(ownerData.getUUID(), MailboxFlag.SHOW_TITLE,
					MailboxFlag.PLAY_SOUND);
			return;
		}

		TitleScreenInfo titleScreen = instance.getConfigManager().creationTitleScreen();
		if (titleScreen != null && titleScreen.enabled()) {
			String titleRaw = titleScreen.title();
			String subtitleRaw = titleScreen.subtitle();

			String title = titleRaw.replace("{player}", playerName);
			String subtitle = subtitleRaw.replace("{player}", playerName);

			instance.debug("showCreationTitleAndSound: Showing title to " + playerName + " → Title: '" + title
					+ "', Subtitle: '" + subtitle + "'");

			VersionHelper.getNMSManager().sendTitle(player,
					AdventureHelper.componentToJson(AdventureHelper.miniMessageToComponent(title)),
					AdventureHelper.componentToJson(AdventureHelper.miniMessageToComponent(subtitle)),
					titleScreen.fadeIn(), titleScreen.stay(), titleScreen.fadeOut());
		}

		if (instance.getConfigManager().creatingHellblockSound() != null) {
			instance.debug("showCreationTitleAndSound: Playing creation sound for " + playerName + " → Sound: "
					+ instance.getConfigManager().creatingHellblockSound().name().asString());
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					instance.getConfigManager().creatingHellblockSound());
		}
	}

	/**
	 * Performs a cleanup pass that removes any "ghost" fluid blocks (WATER or LAVA)
	 * from a Hellblock island's stored chunk data. This prevents phantom liquids
	 * that can persist in memory due to async world deserialization or copy
	 * operations.
	 *
	 * <p>
	 * The cleanup runs in two stages:
	 * <ol>
	 * <li><b>Async stage:</b> Iterates through all island chunks and removes
	 * WATER/LAVA from the custom chunk block maps off the main thread.</li>
	 * <li><b>Sync stage:</b> Refreshes the affected chunks in the live Bukkit world
	 * to visually update clients after modification.</li>
	 * </ol>
	 *
	 * <p>
	 * This operation is completely safe — it never touches live Bukkit blocks
	 * asynchronously. Only the in-memory {@link CustomChunk} data is modified
	 * off-thread.
	 * </p>
	 *
	 * @param world    The Hellblock world to clean up
	 * @param islandId The ID of the island whose chunks should be processed
	 * @return A {@link CompletableFuture} that completes when cleanup and visual
	 *         refresh are finished
	 */
	public CompletableFuture<Void> cleanupGhostLiquids(@NotNull HellblockWorld<?> world, int islandId) {
		instance.debug("cleanupGhostLiquids: Starting cleanup for island ID=" + islandId);

		// Stage 1: collect all chunks belonging to the island
		return instance.getProtectionManager().getHellblockChunks(world, islandId).thenCompose(islandChunks -> {
			// Stage 2: perform async cleanup
			return CompletableFuture.runAsync(() -> {
				int removed = 0;

				for (ChunkPos chunkPos : islandChunks) {
					Optional<CustomChunk> optChunk = world.getChunk(chunkPos);
					if (optChunk.isEmpty())
						continue;

					CustomChunk chunk = optChunk.get();

					// Iterate through each section and remove fluid entries
					for (CustomSection section : chunk.sections()) {
						Map<BlockPos, CustomBlockState> map = section.blockMap();
						Iterator<Map.Entry<BlockPos, CustomBlockState>> iterator = map.entrySet().iterator();

						while (iterator.hasNext()) {
							Map.Entry<BlockPos, CustomBlockState> entry = iterator.next();
							String typeId = entry.getValue().type().type().value().toUpperCase(Locale.ROOT);
							Material mat = Material.matchMaterial(typeId);
							if (mat == null)
								continue;

							if (mat == Material.WATER || mat == Material.LAVA) {
								iterator.remove();
								removed++;
							}
						}
					}
				}

				instance.debug("cleanupGhostLiquids: Removed " + removed + " liquid block" + (removed == 1 ? "" : "s")
						+ " for island ID=" + islandId);
			}).thenCompose(v -> instance.getScheduler().supplySync(() -> {
				// Stage 3: visually refresh affected chunks on the main thread
				instance.debug("cleanupGhostLiquids: Performing visual refresh for island ID=" + islandId);
				try {
					for (ChunkPos pos : islandChunks) {
						if (world.bukkitWorld().isChunkLoaded(pos.x(), pos.z())) {
							world.bukkitWorld().refreshChunk(pos.x(), pos.z());
						}
					}
				} catch (Exception ex) {
					instance.getPluginLogger().warn("cleanupGhostLiquids: Error refreshing chunks — " + ex.getMessage(),
							ex);
				}

				instance.debug("cleanupGhostLiquids: Completed visual refresh after liquid cleanup.");
				return null;
			}));
		});
	}

	/**
	 * Resets the Hellblock island for the specified owner UUID, with options for
	 * forced reset and executor name.
	 *
	 * @param ownerId              The UUID of the island owner
	 * @param forceReset           Whether to force the reset regardless of
	 *                             conditions
	 * @param executorNameForReset Optional name of the executor initiating the
	 *                             reset (for logging purposes)
	 * @return A CompletableFuture that completes when the reset process is finished
	 */
	public CompletableFuture<Void> resetHellblock(@NotNull UUID ownerId, boolean forceReset,
			@Nullable String executorNameForReset) {
		instance.debug("resetHellblock: Starting reset for UUID=" + ownerId + ", force=" + forceReset);

		return instance.getStorageManager()
				.getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenComposeAsync(result -> {
					if (result.isEmpty()) {
						instance.debug("resetHellblock: No UserData found for UUID=" + ownerId + ". Aborting reset.");
						return CompletableFuture.completedFuture(null);
					}

					final UserData ownerData = result.get();
					final HellblockData data = ownerData.getHellblockData();
					final String playerName = ownerData.getName();
					final Location home = data.getHomeLocation();
					final int islandId = data.getIslandId();

					instance.debug("resetHellblock: Resolved world and home location for " + playerName + " (Island ID="
							+ islandId + ")");

					startResetProcess(ownerId);

					// Step 1: create snapshot before reset
					instance.debug("resetHellblock: Creating snapshot before reset for " + playerName);
					return instance.getIslandBackupManager().createPreResetSnapshot(ownerId)
							.thenComposeAsync(snapshotTs -> {

								final HellblockResetEvent resetEvent = new HellblockResetEvent(ownerId, ownerData,
										forceReset);
								EventUtils.fireAndForget(resetEvent);
								instance.debug("resetHellblock: HellblockResetEvent called for " + playerName);

								final Map<Pos3, CustomBlock> blockChanges = new LinkedHashMap<>();

								// Notify owner (must run sync)
								Player online = Bukkit.getPlayer(ownerId);
								if (!forceReset && online != null) {
									instance.debug("resetHellblock: Notifying owner " + ownerData.getName());
									instance.getScheduler().executeSync(() -> notifyOwnerStartReset(ownerData), home);
								}

								// Step 2: perform reset (returns CompletableFuture<Void>)
								instance.debug("resetHellblock: Performing reset for " + playerName);
								return performReset(ownerData, home, blockChanges, forceReset, islandId,
										executorNameForReset);
							});
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe("resetHellblock: Exception during reset of UUID=" + ownerId, ex);
					endResetProcess(ownerId);
					return null;
				});
	}

	/**
	 * Notifies the owner player that their Hellblock island reset process has
	 * started, including an estimated time for completion.
	 *
	 * @param ownerData The player who owns the Hellblock island
	 */
	private void notifyOwnerStartReset(@NotNull UserData ownerData) {
		instance.debug("notifyOwnerStartReset: Preparing reset notification for " + ownerData.getName());

		final Sender audience = instance.getSenderFactory().wrap(ownerData.getPlayer());
		instance.getStorageManager().getOnlineUser(ownerData.getUUID()).ifPresent(userData -> {
			long estimatedTime = estimateResetTimeSeconds(userData, false);
			instance.debug("notifyOwnerStartReset: Sending estimated reset time (" + estimatedTime + "s) to "
					+ ownerData.getName());
			audience.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_RESET_PROCESS
					.arguments(AdventureHelper.miniMessageToComponent(String.valueOf(estimatedTime))).build()));
		});
	}

	/**
	 * Estimates the time (in seconds) it will take to reset a Hellblock island,
	 * based on the user's island data and whether the reset is forced.
	 *
	 * @param ownerData  The user data associated with the island
	 * @param forceReset Whether the reset is forced
	 * @return Estimated time in seconds for island reset
	 */
	private long estimateResetTimeSeconds(@NotNull UserData ownerData, boolean forceReset) {
		long baseTime = 6;

		// More expensive if not forced (player sync messaging, GUI, etc.)
		if (!forceReset) {
			baseTime += 4;
		}

		// Consider bounding box size as a proxy for block volume
		BoundingBox box = ownerData.getHellblockData().getBoundingBox();
		if (box != null) {
			int volume = (int) Math.ceil(box.getMaxX() - box.getMinX() + 1)
					* (int) Math.ceil(box.getMaxY() - box.getMinY() + 1)
					* (int) Math.ceil(box.getMaxZ() - box.getMinZ() + 1);

			if (volume > 50000) {
				baseTime += 8;
			} else if (volume > 20000) {
				baseTime += 5;
			} else if (volume > 10000) {
				baseTime += 3;
			}
		} else {
			// Fallback estimate
			baseTime += 4;
		}

		return baseTime;
	}

	/**
	 * Performs the reset of a Hellblock island by gathering protected blocks,
	 * unprotecting the island, and resetting its state.
	 *
	 * @param ownerData            The user data of the island owner
	 * @param home                 The home location of the island
	 * @param blockChanges         A map of blocks to be changed during reset
	 * @param forceReset           Whether the reset is forced
	 * @param islandId             The unique island ID
	 * @param executorNameForReset The name of the executor if forced, null
	 *                             otherwise
	 * @return A CompletableFuture that completes when the reset process is finished
	 */
	private CompletableFuture<Void> performReset(@NotNull UserData ownerData, @NotNull Location home,
			@NotNull Map<Pos3, CustomBlock> blockChanges, boolean forceReset, int islandId,
			@Nullable String executorNameForReset) {
		String ownerName = ownerData.getName();
		UUID ownerUUID = ownerData.getUUID();
		instance.debug("performReset: Starting island reset for " + ownerName + " (ID=" + islandId + ", force="
				+ forceReset + ")");
		return instance.getWorldManager().ensureHellblockWorldLoaded(islandId).thenCompose(hellblockWorld -> {
			// Sync biome change
			instance.debug("performReset: Applying biome override to Nether Wastes...");
			instance.getScheduler().executeSync(() -> instance.getBiomeHandler()
					.applyHellblockBiomeChange(ownerData.getHellblockData(), HellBiome.NETHER_WASTES, false));

			instance.debug("performReset: Cancelling block scan for " + ownerName);
			instance.getProtectionManager().cancelBlockScan(ownerUUID);

			instance.debug("performReset: Triggering island deletion handlers");
			instance.getIslandManager().handleIslandDeletion(islandId);
			instance.getBorderHandler().stopBorderTask(ownerUUID);
			instance.getUpgradeManager().revalidateUpgradeCache(islandId, null);
			instance.getCoopManager().invalidateCachedOwnerData(ownerUUID);
			instance.getHopperHandler().invalidateHopperWarningCache(islandId);

			// Async block gathering
			instance.debug("performReset: Fetching all protected blocks to reset...");

			return instance.getProtectionManager().getHellblockBlocks(hellblockWorld, ownerUUID)
					.thenCompose(positions -> {
						AtomicInteger count = new AtomicInteger(0);
						List<CompletableFuture<Void>> futures = new ArrayList<>();

						for (Pos3 pos : positions) {
							CompletableFuture<Void> future = hellblockWorld.getBlockState(pos)
									.thenAccept(optionalState -> {
										optionalState.ifPresent(state -> {
											if (!state.isAir()) {
												blockChanges.put(pos, CustomBlockTypes.AIR);
												count.incrementAndGet();
											}
										});
									});
							futures.add(future);
						}

						instance.debug("performReset: Queued " + count.get() + " block changes to AIR for reset.");
						return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
								.thenCompose(v -> unprotectAndResetIsland(ownerData, hellblockWorld, home, blockChanges,
										forceReset, islandId, executorNameForReset));
					});
		});
	}

	/**
	 * Unprotects the Hellblock island and resets it by handling party members,
	 * visitors, and finalizing the reset process.
	 *
	 * @param ownerData            The user data of the island owner
	 * @param world                The Hellblock world where the island is located
	 * @param home                 The home location of the island
	 * @param blockChanges         A map of blocks to be changed during reset
	 * @param forceReset           Whether the reset is forced
	 * @param islandId             The unique island ID
	 * @param executorNameForReset The name of the executor if forced, null
	 *                             otherwise
	 * @return A CompletableFuture that completes when the unprotection and reset
	 *         are finished
	 */
	private CompletableFuture<Void> unprotectAndResetIsland(@NotNull UserData ownerData,
			@NotNull HellblockWorld<?> world, @NotNull Location home, @NotNull Map<Pos3, CustomBlock> blockChanges,
			boolean forceReset, int islandId, @Nullable String executorNameForReset) {
		String ownerName = ownerData.getName();
		UUID ownerUUID = ownerData.getUUID();

		instance.debug("unprotectAndResetIsland: Starting unprotection phase for " + ownerName);

		return instance.getProtectionManager().getIslandProtection().unprotectHellblock(world, ownerUUID)
				.thenCompose(v -> {
					instance.debug("unprotectAndResetIsland: Hellblock unprotected for " + ownerName);
					return handlePartyMembers(ownerData, forceReset, executorNameForReset);
				}).thenCompose(v -> {
					instance.debug("unprotectAndResetIsland: Party members handled for " + ownerName);
					return handleVisitors(ownerUUID);
				}).thenRun(() -> {
					instance.debug("unprotectAndResetIsland: Finalizing reset and updating world...");

					instance.getScheduler().executeSync(() -> {
						instance.debug("unprotectAndResetIsland: Resetting owner data for " + ownerName);
						resetOwnerData(ownerData, forceReset, world, blockChanges, home, executorNameForReset);
					}, home);

					instance.getScheduler().executeSync(() -> {
						instance.debug("unprotectAndResetIsland: Finalizing reset tasks (GUI or world deletion)");
						finalizeReset(ownerData, forceReset, islandId, home);
					}, home);
				});
	}

	/**
	 * Handles resetting Hellblock data for all party members of the island owner.
	 *
	 * @param ownerData            The user data of the island owner
	 * @param forceReset           Whether the reset is forced
	 * @param executorNameForReset The name of the executor if forced, null
	 *                             otherwise
	 * @return A CompletableFuture that completes when all party members have been
	 *         handled
	 */
	private CompletableFuture<Void> handlePartyMembers(@NotNull UserData ownerData, boolean forceReset,
			@Nullable String executorNameForReset) {
		final Set<UUID> party = ownerData.getHellblockData().getPartyMembers();
		String ownerName = ownerData.getName();

		if (party.isEmpty()) {
			instance.debug("handlePartyMembers: No party members found for " + ownerName);
			return CompletableFuture.completedFuture(null);
		}

		instance.debug("handlePartyMembers: Processing " + party.size() + " party members for " + ownerName
				+ " (forceReset=" + forceReset + ")");

		final List<CompletableFuture<Void>> futures = new ArrayList<>();

		party.forEach(uuid -> {
			final Player member = Bukkit.getPlayer(uuid);
			if (member != null && member.isOnline()) {
				instance.debug("handlePartyMembers: Handling **online** party member " + member.getName());
				futures.add(handleOnlinePartyMember(member, ownerData, forceReset, executorNameForReset));
			} else {
				instance.debug("handlePartyMembers: Handling **offline** party member UUID=" + uuid);
				futures.add(handleOfflinePartyMember(uuid, forceReset, executorNameForReset));
			}
		});

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
	}

	/**
	 * Handles an online party member by resetting their Hellblock data and
	 * notifying them.
	 *
	 * @param member               The online party member player
	 * @param ownerData            The user data of the island owner
	 * @param forceReset           Whether the reset is forced
	 * @param executorNameForReset The name of the executor if forced, null
	 *                             otherwise
	 * @return A CompletableFuture that completes when the online member has been
	 *         handled
	 */
	private CompletableFuture<Void> handleOnlinePartyMember(@NotNull Player member, @NotNull UserData ownerData,
			boolean forceReset, @Nullable String executorNameForReset) {
		final Optional<UserData> onlineMemberOpt = instance.getStorageManager().getOnlineUser(member.getUniqueId());
		String memberName = member.getName();

		if (onlineMemberOpt.isEmpty()) {
			instance.debug(
					"handleOnlinePartyMember: UserData not found for online member " + memberName + ". Skipping.");
			return CompletableFuture.completedFuture(null);
		}

		final UserData data = onlineMemberOpt.get();
		instance.debug("handleOnlinePartyMember: Resetting hellblock data for online member " + memberName);

		data.getHellblockData().resetHellblockData();
		instance.getBorderHandler().stopBorderTask(member.getUniqueId());
		instance.getFishingManager().getFishHook(member.getUniqueId()).ifPresent(CustomFishingHook::destroy);

		member.closeInventory();
		instance.debug("handleOnlinePartyMember: Closed inventory and stopped border task for " + memberName);

		if (instance.getConfigManager().resetInventory()) {
			member.getInventory().clear();
			member.getInventory().setArmorContents(null);
			instance.debug("handleOnlinePartyMember: Cleared inventory and armor for " + memberName);
		}

		if (instance.getConfigManager().resetEnderchest()) {
			member.getEnderChest().clear();
			instance.debug("handleOnlinePartyMember: Cleared ender chest for " + memberName);
		}

		teleportToSpawn(member, true);
		instance.debug("handleOnlinePartyMember: Teleported " + memberName + " to spawn");

		final Sender audience = instance.getSenderFactory().wrap(member);
		if (!forceReset && ownerData.isOnline()) {
			String ownerName = ownerData.getName();
			instance.debug("handleOnlinePartyMember: Sending standard reset message to " + memberName + " from owner "
					+ ownerName);
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_RESET_PARTY_NOTIFICATION
							.arguments(AdventureHelper.miniMessageToComponent(ownerName)).build()));
		} else if (forceReset) {
			instance.debug("handleOnlinePartyMember: Sending forced reset message to " + memberName + " from executor "
					+ executorNameForReset);
			audience.sendMessage(instance.getTranslationManager()
					.render(MessageConstants.MSG_HELLBLOCK_RESET_PARTY_FORCED_NOTIFICATION
							.arguments(AdventureHelper.miniMessageToComponent(executorNameForReset)).build()));
		}

		return CompletableFuture.completedFuture(null);
	}

	/**
	 * Handles an offline party member by resetting their Hellblock data and queuing
	 * a mailbox message.
	 *
	 * @param memberId             The UUID of the offline party member
	 * @param forceReset           Whether the reset is forced
	 * @param executorNameForReset The name of the executor if forced, null
	 *                             otherwise
	 * @return A CompletableFuture that completes when the offline member has been
	 *         handled
	 */
	private CompletableFuture<Void> handleOfflinePartyMember(@NotNull UUID memberId, boolean forceReset,
			@Nullable String executorNameForReset) {
		return instance.getStorageManager()
				.getCachedUserDataWithFallback(memberId, instance.getConfigManager().lockData()).thenAccept(result -> {
					if (result.isEmpty()) {
						instance.debug("handleOfflinePartyMember: No UserData found for offline party member UUID="
								+ memberId);
						return;
					}

					final UserData offlineMember = result.get();
					final UUID ownerUUID = offlineMember.getHellblockData().getOwnerUUID();
					String memberName = offlineMember.getName();
					String ownerName = instance.getTranslationManager()
							.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key());

					// Resolve owner's name if possible
					if (ownerUUID != null) {
						OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
						if (owner.hasPlayedBefore() && owner.getName() != null) {
							ownerName = owner.getName();
						}
					}

					instance.debug("handleOfflinePartyMember: Resetting hellblock data for offline member " + memberName
							+ " (UUID=" + memberId + "), owner=" + ownerName + ", forceReset=" + forceReset);

					offlineMember.getHellblockData().resetHellblockData();

					String mailboxMessageKey = forceReset ? "message.hellblock.coop.deleted.offline"
							: "message.hellblock.coop.reset.offline";

					List<Component> args = forceReset
							? List.of(AdventureHelper.miniMessageToComponent(executorNameForReset))
							: List.of(AdventureHelper.miniMessageToComponent(ownerName));

					Set<MailboxFlag> flags = Set.of(MailboxFlag.RESET_INVENTORY, MailboxFlag.RESET_ENDERCHEST,
							MailboxFlag.UNSAFE_LOCATION, MailboxFlag.NOTIFY_PARTY);

					instance.debug("handleOfflinePartyMember: Queueing mailbox message '" + mailboxMessageKey + "' for "
							+ memberName + " with flags " + flags);

					instance.getMailboxManager().queue(offlineMember.getUUID(),
							new MailboxEntry(mailboxMessageKey, args, flags));
				});
	}

	/**
	 * Handles all active visitors on the Hellblock island being reset, teleporting
	 * online visitors to spawn and queuing mailbox flags for offline visitors.
	 *
	 * @param ownerId The UUID of the island owner
	 * @return A CompletableFuture that completes when all visitors have been
	 *         handled
	 */
	private CompletableFuture<Void> handleVisitors(@NotNull UUID ownerId) {
		instance.debug("handleVisitors: Fetching active visitors for island ID=" + ownerId);

		return instance.getCoopManager().getVisitors(ownerId).thenCompose(visitors -> {
			if (visitors.isEmpty()) {
				instance.debug("handleVisitors: No visitors found for island ID=" + ownerId);
				return CompletableFuture.completedFuture(null);
			}

			instance.debug("handleVisitors: Found " + visitors.size() + " visitors for island ID=" + ownerId);

			final List<CompletableFuture<Void>> futures = new ArrayList<>();

			visitors.forEach(visitorUUID -> {
				Player visitor = Bukkit.getPlayer(visitorUUID);
				if (visitor != null && visitor.isOnline()) {
					instance.debug("handleVisitors: Handling **online** visitor " + visitor.getName());
					futures.add(handleOnlineVisitor(visitor, visitorUUID));
				} else {
					instance.debug("handleVisitors: Handling **offline** visitor UUID=" + visitorUUID);
					futures.add(handleOfflineVisitor(visitorUUID));
				}
			});

			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		});
	}

	/**
	 * Handles an online visitor by checking their Hellblock data and teleporting
	 * them to spawn if necessary.
	 *
	 * @param visitor   The online visitor player
	 * @param visitorId The UUID of the visitor
	 * @return A CompletableFuture that completes when the visitor has been handled
	 */
	private CompletableFuture<Void> handleOnlineVisitor(@NotNull Player visitor, @NotNull UUID visitorId) {
		final Optional<UserData> visitorOpt = instance.getStorageManager().getOnlineUser(visitorId);
		String visitorName = visitor.getName();

		if (visitorOpt.isEmpty()) {
			instance.debug("handleOnlineVisitor: No UserData found for online visitor " + visitorName + " (UUID="
					+ visitorId + ")");
			return CompletableFuture.completedFuture(null);
		}

		final UserData visitorData = visitorOpt.get();
		instance.debug("handleOnlineVisitor: Found UserData for " + visitorName + ", checking island ownership...");
		instance.getFishingManager().getFishHook(visitor.getUniqueId()).ifPresent(CustomFishingHook::destroy);

		if (visitorData.getHellblockData().hasHellblock()) {
			final UUID ownerUUID = visitorData.getHellblockData().getOwnerUUID();
			if (ownerUUID == null) {
				instance.getPluginLogger().warn("handleOnlineVisitor: Null owner UUID for visitor " + visitorName);
				throw new NullPointerException("Owner reference returned null, please report this to the developer.");
			}

			instance.debug(
					"handleOnlineVisitor: Visitor " + visitorName + " is part of island owned by UUID=" + ownerUUID);

			return instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(ownerOpt -> {
						if (ownerOpt.isEmpty()) {
							instance.debug("handleOnlineVisitor: Owner data not found. Teleporting " + visitorName
									+ " to spawn.");
							teleportToSpawn(visitor, true);
							return;
						}

						UserData owner = ownerOpt.get();
						instance.debug("handleOnlineVisitor: Making home location safe for visitor " + visitorName
								+ " (owner=" + owner.getName() + ")");
						instance.getCoopManager().makeHomeLocationSafe(owner, visitorData);
					});
		} else {
			instance.debug("handleOnlineVisitor: " + visitorName
					+ " is not part of a Hellblock island. Teleporting to spawn.");
			teleportToSpawn(visitor, true);
		}

		return CompletableFuture.completedFuture(null);
	}

	/**
	 * Handles an offline visitor by queuing a mailbox flag for unsafe location.
	 *
	 * @param visitorId The UUID of the offline visitor
	 * @return A CompletableFuture that completes when the visitor has been handled
	 */
	private CompletableFuture<Void> handleOfflineVisitor(@NotNull UUID visitorId) {
		instance.debug("handleOfflineVisitor: Attempting to resolve UserData for offline visitor UUID=" + visitorId);

		return instance.getStorageManager()
				.getCachedUserDataWithFallback(visitorId, instance.getConfigManager().lockData())
				.thenAccept(visitorResult -> {
					if (visitorResult.isEmpty()) {
						instance.debug("handleOfflineVisitor: No UserData found for offline visitor UUID=" + visitorId);
						return;
					}

					final UserData visitorData = visitorResult.get();
					final String visitorName = visitorData.getName();

					instance.debug("handleOfflineVisitor: Queuing UNSAFE_LOCATION mailbox flag for offline visitor "
							+ visitorName);

					instance.getMailboxManager().queueMailbox(visitorData.getUUID(), MailboxFlag.UNSAFE_LOCATION);
				});
	}

	/**
	 * Resets the Hellblock data for the owner player, clears inventories if
	 * configured, notifies the player, and restores original blocks.
	 *
	 * @param ownerData            The user data of the player whose Hellblock is
	 *                             being reset
	 * @param forceReset           Whether the reset is forced
	 * @param world                The Hellblock world where the island is located
	 * @param blockChanges         A map of blocks to restore after reset
	 * @param home                 The home location of the Hellblock island
	 * @param executorNameForReset The name of the executor if forced, null
	 *                             otherwise
	 */
	private void resetOwnerData(@NotNull UserData ownerData, boolean forceReset, @NotNull HellblockWorld<?> world,
			@NotNull Map<Pos3, CustomBlock> blockChanges, @NotNull Location home,
			@Nullable String executorNameForReset) {
		String playerName = ownerData.getName();
		instance.debug("resetOwnerData: Resetting data for " + playerName + " (forceReset=" + forceReset + ")");

		ownerData.getHellblockData().resetHellblockData();
		ownerData.getHellblockData().setDefaultUpgradeTiers();

		ownerData.getLocationCacheData().clearBlockData();

		if (!forceReset && !ownerData.isOnline()) {
			instance.debug("resetOwnerData: Queuing mailbox for offline reset GUI for " + playerName);
			instance.getMailboxManager().queueMailbox(ownerData.getUUID(), MailboxFlag.SHOW_RESET_GUI);
		}

		if (ownerData.isOnline()) {
			Player owner = Bukkit.getPlayer(ownerData.getUUID());
			Sender audience = instance.getSenderFactory().wrap(owner);
			instance.getFishingManager().getFishHook(owner.getUniqueId()).ifPresent(CustomFishingHook::destroy);

			if (!forceReset) {
				instance.debug("resetOwnerData: Notifying player " + playerName + " of standard reset");
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_RESET_NEW_OPTION.build()));
			} else {
				instance.debug("resetOwnerData: Notifying player " + playerName + " of forced reset by "
						+ executorNameForReset);
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_RESET_OWNER_FORCED_NOTIFICATION
								.arguments(AdventureHelper.miniMessageToComponent(executorNameForReset)).build()));
			}

			if (instance.getConfigManager().resetInventory()) {
				instance.debug("resetOwnerData: Clearing inventory and armor for " + playerName);
				owner.getInventory().clear();
				owner.getInventory().setArmorContents(null);
			}
			if (instance.getConfigManager().resetEnderchest()) {
				instance.debug("resetOwnerData: Clearing ender chest for " + playerName);
				owner.getEnderChest().clear();
			}

			instance.debug("resetOwnerData: Teleporting " + playerName + " to spawn after reset.");
			teleportToSpawn(owner, true);
		} else {
			instance.debug("resetOwnerData: Player " + playerName + " is offline. Queueing mailbox entry.");
			instance.getMailboxManager().queue(ownerData.getUUID(),
					new MailboxEntry(
							forceReset ? "message.hellblock.coop.deleted.offline"
									: "message.hellblock.coop.reset.offline",
							forceReset ? List.of(AdventureHelper.miniMessageToComponent(executorNameForReset)) : null,
							Set.of(MailboxFlag.RESET_INVENTORY, MailboxFlag.RESET_ENDERCHEST,
									MailboxFlag.UNSAFE_LOCATION, MailboxFlag.NOTIFY_OWNER)));
		}

		instance.debug("resetOwnerData: Restoring original blocks after reset for " + playerName);
		instance.getScheduler().executeAsync(() -> {
			List<CompletableFuture<Void>> futures = new ArrayList<>();

			for (Map.Entry<Pos3, CustomBlock> entry : blockChanges.entrySet()) {
				Pos3 pos = entry.getKey();
				CustomBlock blockType = entry.getValue();

				CompletableFuture<Void> future = world.getBlockState(pos).thenAccept(optionalState -> {
					optionalState.ifPresent(state -> {
						if (state.hasInventory()) {
							state.clearInventory();
						}
					});

					world.updateBlockState(pos, blockType.createBlockState())
							.thenRun(() -> world.removeBlockState(pos));
				});

				futures.add(future);
			}

			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
		});
	}

	/**
	 * Finalizes the Hellblock reset process by opening the island choice GUI for
	 * online players or deleting personal worlds if forced.
	 *
	 * @param ownerData  The user data of the player whose Hellblock was reset
	 * @param forceReset Whether the reset was forced
	 * @param islandId   The ID of the Hellblock island
	 * @param home       The home location of the Hellblock island
	 */
	private void finalizeReset(@NotNull UserData ownerData, boolean forceReset, int islandId, @NotNull Location home) {
		String playerName = ownerData.getName();

		instance.debug("finalizeReset: Finalizing reset for " + playerName + " (forceReset=" + forceReset + ", ID="
				+ islandId + ")");

		if (!forceReset && ownerData.isOnline()) {
			Player player = Bukkit.getPlayer(ownerData.getUUID());
			instance.debug("finalizeReset: Opening island choice GUI for " + playerName);
			instance.getScheduler().sync()
					.runLater(() -> instance.getIslandChoiceGUIManager().openIslandChoiceGUI(player, true), 20L, home);
		} else if (forceReset) {
			ownerData.getHellblockData().setPreservedBoundingBox(null);
			ownerData.getHellblockData().setIslandId(0);
			ownerData.getHellblockData().setResetCooldown(0L);
			instance.getCoopManager().invalidateCachedOwnerData(ownerData.getUUID());
			instance.getIslandBackupManager().deleteAllSnapshots(ownerData.getUUID());
			if (instance.getConfigManager().perPlayerWorlds()) {
				String worldName = instance.getWorldManager().getHellblockWorldFormat(islandId);
				instance.debug("finalizeReset: Deleting personal world '" + worldName + "' for " + playerName);
				instance.getWorldManager().adapter().deleteWorld(worldName);
			}
		}

		// Invalidate caches
		instance.debug("finalizeReset: Invalidating caches for island ID " + islandId);
		invalidateHellblockIDCache();
		endResetProcess(ownerData.getUUID());
		instance.getStorageManager().getDataSource().invalidateIslandCache(islandId);
		instance.getProtectionManager().invalidateIslandChunkCache(islandId);
	}

	/**
	 * Ensures the player's safety by checking their current location. If unsafe,
	 * attempts to teleport them to their Hellblock home or spawn if necessary.
	 *
	 * @param userData The user data of the player to check
	 */
	public void ensureSafety(@NotNull UserData userData) {
		Player player = userData.getPlayer();
		if (player == null || !player.isOnline() || player.getLocation() == null) {
			instance.debug("ensureSafety: Skipping safety check – player not online or location is null.");
			return;
		}

		if (!isInCorrectWorld(player)) {
			instance.debug(
					"ensureSafety: Skipping safety check – player not in hellblock world, safety is irrevelant.");
			return;
		}

		instance.debug("ensureSafety: Checking if " + player.getName() + " is in a safe location...");

		LocationUtils.isSafeLocationAsync(player.getLocation(), player).thenAccept(isSafe -> {
			if (isSafe) {
				instance.debug("ensureSafety: Location is safe for " + player.getName());
				return;
			}

			instance.debug("ensureSafety: Unsafe location detected for " + player.getName());

			if (userData.getHellblockData().hasHellblock()) {
				final UUID ownerUUID = userData.getHellblockData().getOwnerUUID();
				if (ownerUUID == null) {
					throw new NullPointerException("ensureSafety: Owner UUID is null for " + player.getName());
				}

				instance.debug("ensureSafety: Resolving owner UserData for UUID=" + ownerUUID);
				instance.getStorageManager()
						.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
						.thenAccept(optionalOwner -> {
							if (optionalOwner.isEmpty()) {
								instance.debug("ensureSafety: Owner data not found for " + player.getName()
										+ ", teleporting to spawn.");
								instance.getScheduler().executeSync(() -> teleportToSpawn(player, false));
								return;
							}

							final UserData ownerUser = optionalOwner.get();
							if (ownerUser.getHellblockData().getHomeLocation() != null) {
								instance.debug("ensureSafety: Making home location safe for " + player.getName());
								instance.getScheduler().executeSync(
										() -> instance.getCoopManager().makeHomeLocationSafe(ownerUser, userData));
							} else {
								instance.debug("ensureSafety: Owner has no home location. Teleporting "
										+ player.getName() + " to spawn.");
								instance.getScheduler().executeSync(() -> teleportToSpawn(player, false));
							}
						}).exceptionally(ex -> {
							instance.getPluginLogger()
									.severe("ensureSafety: Error retrieving owner data for " + player.getName(), ex);
							instance.getScheduler().executeSync(() -> teleportToSpawn(player, false));
							return null;
						});
			} else {
				instance.debug("ensureSafety: Player " + player.getName() + " has no hellblock. Teleporting to spawn.");
				instance.getScheduler().executeSync(() -> teleportToSpawn(player, false));
			}
		});
	}

	/**
	 * Handles logic for players visiting other Hellblock islands, including ban
	 * enforcement and safe teleportation if necessary.
	 *
	 * <p>
	 * This method checks if the player is currently on another player's island,
	 * verifies if they are banned, and handles teleportation either back to their
	 * home island or to spawn if needed.
	 *
	 * @param player     The player visiting the island.
	 * @param onlineUser The UserData of the visiting player.
	 */
	public void handleVisitingIsland(@NotNull Player player, @NotNull UserData onlineUser) {
		final UUID playerId = player.getUniqueId();

		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				instance.debug("Player " + player.getName() + " is not on a Hellblock island. Skipping visit check.");
				return CompletableFuture.completedFuture(null);
			}

			instance.debug("Player " + player.getName() + " is visiting island owned by " + ownerUUID);

			return instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenApply(ownerOpt -> {
						if (ownerOpt.isEmpty()) {
							instance.debug("No UserData found for island owner " + ownerUUID);
							return null;
						}

						final UserData ownerUser = ownerOpt.get();
						final HellblockData ownerData = ownerUser.getHellblockData();
						final BoundingBox bounds = ownerData.getBoundingBox();
						if (bounds == null) {
							instance.debug("Bounding box not found for owner " + ownerUUID);
							return null;
						}

						boolean isBypassing = player.isOp() || player.hasPermission("hellblock.admin")
								|| player.hasPermission("hellblock.bypass.interact");

						Set<UUID> banned = ownerData.getBannedMembers();

						if (!isBypassing && bounds.contains(player.getLocation().toVector())
								&& banned.contains(playerId)) {
							instance.debug("Player " + player.getName() + " is banned from island " + ownerUUID);
							return ownerUser;
						}

						instance.debug("Player " + player.getName() + " is not banned from island " + ownerUUID);
						return null;
					});

		}).thenAccept(bannedOwnerUser -> {
			if (bannedOwnerUser == null) {
				// Player is allowed to stay
				return;
			}

			UUID hellblockOwner = onlineUser.getHellblockData().getOwnerUUID();

			if (onlineUser.getHellblockData().hasHellblock() && hellblockOwner != null) {
				instance.debug("Kicking banned player " + player.getName() + " back to their home island.");

				instance.getCoopManager().kickVisitorsIfLocked(bannedOwnerUser.getUUID());

				instance.getStorageManager()
						.getCachedUserDataWithFallback(hellblockOwner, instance.getConfigManager().lockData())
						.thenCompose(homeOwnerOpt -> {
							if (homeOwnerOpt.isEmpty()) {
								instance.debug("No home island data found for " + hellblockOwner);
								return CompletableFuture.completedFuture(null);
							}

							final UserData homeOwner = homeOwnerOpt.get();

							return instance.getCoopManager().makeHomeLocationSafe(homeOwner, onlineUser)
									.thenRun(() -> instance.getScheduler().executeSync(() -> {
										World world = player.getWorld();
										instance.getWorldManager().markWorldAccess(world.getName());
										instance.debug("Teleported " + player.getName()
												+ " to safe location on their island.");
									}));
						}).exceptionally(ex -> {
							instance.getPluginLogger().severe("Error fetching offline owner data for " + playerId, ex);
							return null;
						});

			} else {
				// No home island → teleport to spawn
				instance.debug("Player " + player.getName() + " has no Hellblock island. Teleporting to spawn.");
				instance.getScheduler().executeSync(() -> {
					World world = player.getWorld();
					instance.getWorldManager().markWorldAccess(world.getName());
					teleportToSpawn(player, true);
				});
			}
		}).exceptionally(ex -> {
			instance.getPluginLogger().severe("Error handling visiting island logic for player " + playerId, ex);
			return null;
		});
	}

	/**
	 * Captures a snapshot of the island owned by the specified player.
	 * <p>
	 * Captures non-air blocks and non-player entities within the island's bounding
	 * box. The snapshot is captured asynchronously and returned via a
	 * {@link CompletableFuture}.
	 *
	 * @param ownerId The UUID of the island owner.
	 * @return A CompletableFuture that resolves to the {@link IslandSnapshot}.
	 */
	@NotNull
	public CompletableFuture<IslandSnapshot> captureIslandSnapshot(@NotNull UUID ownerId) {
		final CompletableFuture<IslandSnapshot> future = new CompletableFuture<>();

		instance.debug("Starting snapshot capture for island owned by " + ownerId);

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(optionalUserData -> {
					if (optionalUserData.isEmpty()) {
						instance.debug("No UserData found for " + ownerId + ". Returning empty snapshot.");
						future.complete(new IslandSnapshot(List.of(), List.of()));
						return;
					}

					final HellblockData hb = optionalUserData.get().getHellblockData();
					final Optional<HellblockWorld<?>> optWorld = instance.getWorldManager()
							.getWorld(instance.getWorldManager().getHellblockWorldFormat(hb.getIslandId()));

					if (optWorld.isEmpty() || optWorld.get().bukkitWorld() == null) {
						instance.debug("World not loaded or missing for island " + hb.getIslandId()
								+ ". Returning empty snapshot.");
						future.complete(new IslandSnapshot(List.of(), List.of()));
						return;
					}

					if (hb.getBoundingBox() == null) {
						instance.debug(
								"BoundingBox not found for snapshot for " + ownerId + ". Returning empty snapshot.");
						future.complete(new IslandSnapshot(List.of(), List.of()));
						return;
					}

					final World world = optWorld.get().bukkitWorld();
					final BoundingBox box = hb.getBoundingBox();

					instance.debug("Preparing block snapshot in bounding box: " + box);
					final List<IslandSnapshotBlock> blocks = Collections.synchronizedList(new ArrayList<>());
					final List<EntitySnapshot> entities = Collections.synchronizedList(new ArrayList<>());

					// Pre-build all block locations in the bounding box
					final List<Location> locations = new ArrayList<>();
					for (int x = (int) box.getMinX(); x <= (int) box.getMaxX(); x++) {
						for (int y = (int) box.getMinY(); y <= (int) box.getMaxY(); y++) {
							for (int z = (int) box.getMinZ(); z <= (int) box.getMaxZ(); z++) {
								locations.add(new Location(world, x, y, z));
							}
						}
					}

					instance.debug("Total block locations to process: " + locations.size());

					final Iterator<Location> it = locations.iterator();
					final int BATCH_SIZE = 500;

					// Use atomic reference so we can cancel the task later
					final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

					final SchedulerTask task = instance.getScheduler().sync().runRepeating(() -> {
						int processed = 0;
						while (processed < BATCH_SIZE && it.hasNext()) {
							final Location loc = it.next();
							final Block block = loc.getBlock();
							if (!block.getType().isAir()) {
								blocks.add(IslandSnapshotBlock.fromBlockState(block.getState(), List.of()));
							}
							processed++;
						}

						instance.debug("Processed " + processed + " blocks this tick (remaining: "
								+ (locations.size() - blocks.size()) + ")");

						if (!it.hasNext()) {
							instance.debug("Block snapshot complete. Starting entity capture...");

							world.getNearbyEntities(box).forEach(entity -> {
								if (!(entity instanceof Player)) {
									entities.add(EntitySnapshot.fromEntity(entity));
								}
							});

							instance.debug("Entity capture complete. Total: " + entities.size());

							// Cancel the task safely
							final SchedulerTask scheduled = taskRef.get();
							if (scheduled != null && !scheduled.isCancelled()) {
								scheduled.cancel();
							}

							// Complete the snapshot
							future.complete(new IslandSnapshot(blocks, entities));
							instance.debug("Island snapshot complete for owner " + ownerId);
						}
					}, 1L, 1L, LocationUtils.getAnyLocationInstance());

					taskRef.set(task); // store reference after scheduling
				});

		return future;
	}

	/**
	 * Rolls back an island to a specific snapshot timestamp.
	 *
	 * @param ownerId   The UUID of the island owner.
	 * @param timestamp The snapshot timestamp to roll back to.
	 * @return A future completing when rollback is done.
	 */
	public CompletableFuture<Void> rollbackIsland(@NotNull UUID ownerId, long timestamp) {
		instance.debug("Starting rollback for " + ownerId + " using snapshot " + timestamp);

		return instance.getStorageManager()
				.getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenCompose(optionalUserData -> {
					if (optionalUserData.isEmpty()) {
						instance.getPluginLogger().warn("No userdata found for rollback of " + ownerId);
						return CompletableFuture.completedFuture(null);
					}

					final HellblockData data = optionalUserData.get().getHellblockData();
					final String worldName = instance.getWorldManager().getHellblockWorldFormat(data.getIslandId());
					final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(worldName);

					if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
						instance.getPluginLogger().warn("World not found for rollback: " + worldName);
						return CompletableFuture.completedFuture(null);
					}

					if (data.getBoundingBox() == null) {
						instance.getPluginLogger().warn("BoundingBox not found for rollback of " + ownerId);
						return CompletableFuture.completedFuture(null);
					}

					final HellblockWorld<?> world = worldOpt.get();
					final BoundingBox box = data.getBoundingBox();

					instance.debug("Loading snapshot " + timestamp + " for island " + data.getIslandId());

					// Load snapshot async
					return CompletableFuture
							.supplyAsync(() -> instance.getIslandBackupManager().loadSnapshot(ownerId, timestamp))
							.thenCompose(snapshot -> {
								if (snapshot == null) {
									instance.getPluginLogger().warn("No snapshot found for rollback of " + ownerId);
									return CompletableFuture.completedFuture(null);
								}

								final HellblockRollbackEvent rollbackEvent = new HellblockRollbackEvent(ownerId,
										timestamp);
								EventUtils.fireAndForget(rollbackEvent);

								// Ensure chunks are loaded before restoring
								return preloadRollbackChunks(world, box).thenRun(() -> {
									instance.debug(
											"Chunks loaded. Starting batched restore for island " + data.getIslandId());

									// Restore snapshot in safe batches
									snapshot.restoreIntoWorldBatched(world.bukkitWorld(), box, instance, () -> {
										instance.getPluginLogger().info("Rollback complete for " + ownerId);
										instance.getScheduler().executeSync(() -> {
											instance.debug(
													"Triggering island level recalculation for " + data.getIslandId());
											instance.getIslandLevelManager().recalculateIslandLevel(data.getIslandId());
										});
									}, progress -> instance.getPluginLogger().info("Rollback progress for " + ownerId
											+ ": " + "%.1f%%".formatted(progress * 100)));
								});
							});
				});
	}

	/**
	 * Asynchronously preloads all chunks within the given bounding box in the
	 * specified Hellblock world to prepare for operations like rollback or
	 * restoration.
	 * <p>
	 * This method avoids blocking the main server thread by using
	 * {@code CustomChunk#load(true)} asynchronously, and waits for all chunks to
	 * complete loading via {@code CompletableFuture.allOf(...)}.
	 * </p>
	 *
	 * @param world the Hellblock world where the chunks should be loaded
	 * @param box   the bounding box defining the area of chunks to preload
	 * @return a {@code CompletableFuture<Void>} that completes when all relevant
	 *         chunks have finished loading
	 */
	private CompletableFuture<Void> preloadRollbackChunks(@NotNull HellblockWorld<?> world, @NotNull BoundingBox box) {
		List<CompletableFuture<Boolean>> loadFutures = new ArrayList<>();

		for (int x = (int) box.getMinX() >> 4; x <= (int) box.getMaxX() >> 4; x++) {
			for (int z = (int) box.getMinZ() >> 4; z <= (int) box.getMaxZ() >> 4; z++) {
				ChunkPos chunkPos = ChunkPos.of(x, z);

				Optional<CustomChunk> customChunk = world.getChunk(chunkPos);
				if (customChunk.isPresent()) {
					CompletableFuture<Boolean> future = customChunk.get().load(true).thenApply(success -> {
						if (!success) {
							instance.getPluginLogger()
									.warn("Failed to load chunk " + chunkPos + " during rollback preload.");
						}
						return success;
					});

					loadFutures.add(future);
				}
			}
		}

		return CompletableFuture.allOf(loadFutures.toArray(CompletableFuture[]::new));
	}

	/**
	 * Attempts to roll back an island for the given owner to the most recent
	 * snapshot taken within the last {@code minutes}.
	 *
	 * @param ownerId The UUID of the island owner.
	 * @param minutes The number of minutes to look back for a snapshot.
	 * @return A future completing when the rollback is either completed or skipped.
	 */
	public CompletableFuture<Void> rollbackLastMinutes(@NotNull UUID ownerId, int minutes) {
		final long cutoff = System.currentTimeMillis() - (minutes * 60L * 1000L);
		final List<Long> snapshots = instance.getIslandBackupManager().listSnapshots(ownerId);

		instance.debug("Found " + snapshots.size() + " snapshots for owner " + ownerId);
		instance.debug("Looking for snapshot newer than " + cutoff + " (" + minutes + " minutes ago)");

		final Optional<Long> chosen = snapshots.stream().filter(ts -> ts >= cutoff).findFirst();

		if (chosen.isPresent()) {
			instance.debug("Using snapshot timestamp " + chosen.get() + " for rollback of " + ownerId);
			return rollbackIsland(ownerId, chosen.get());
		}

		instance.getPluginLogger().warn("No snapshot within " + minutes + " minutes for " + ownerId);
		return CompletableFuture.completedFuture(null);
	}

	/**
	 * Purges inactive Hellblocks by marking them as abandoned.
	 * <p>
	 * A Hellblock is considered inactive if:
	 * <ul>
	 * <li>The player is the island owner</li>
	 * <li>The island is still at the default level (unprogressed)</li>
	 * <li>The player's last activity (join, quit, or island entry/exit) exceeds the
	 * configured inactivity threshold</li>
	 * </ul>
	 * <p>
	 * This method runs asynchronously where needed and uses the
	 * {@code getLastActivity()} value from {@link UserData} for a more accurate
	 * indication of inactivity.
	 * <p>
	 * A {@link HellblockAbandonEvent} is fired when an island is marked abandoned.
	 */
	public CompletableFuture<Void> purgeInactiveHellblocks() {
		final int purgeDays = instance.getConfigManager().abandonAfterDays();
		if (purgeDays <= 0) {
			instance.debug("Skipping purge: Configured abandonAfterDays is <= 0.");
			return CompletableFuture.completedFuture(null);
		}

		final AtomicInteger purgeCount = new AtomicInteger(0);
		final List<CompletableFuture<Void>> futures = new ArrayList<>();
		final IslandProtection<?> protection = instance.getProtectionManager().getIslandProtection();

		instance.getPluginLogger()
				.info("Starting purge of inactive Hellblocks for players inactive for " + purgeDays + " days...");

		for (UUID id : instance.getStorageManager().getDataSource().getUniqueUsers()) {
			final OfflinePlayer player = Bukkit.getOfflinePlayer(id);

			if (!player.hasPlayedBefore()) {
				instance.debug("Skipping player " + id + ": has never played before.");
				continue;
			}

			// Track this future
			CompletableFuture<Void> future = instance.getStorageManager().getCachedUserDataWithFallback(id, false)
					.thenAccept(result -> {
						if (result.isEmpty()) {
							instance.debug("No user data found for player " + id + ". Skipping.");
							return;
						}

						final UserData user = result.get();
						final HellblockData data = user.getHellblockData();

						if (!data.hasHellblock()) {
							instance.debug("Player " + id + " doesn't own a Hellblock. Skipping.");
							return;
						}

						final boolean isOwner = data.hasHellblock() && data.getOwnerUUID() != null
								&& id.equals(data.getOwnerUUID());

						if (!isOwner) {
							instance.debug("Player " + id + " is not the owner of their Hellblock. Skipping.");
							return;
						}

						if (data.getIslandLevel() <= HellblockData.DEFAULT_LEVEL) {
							instance.debug("Player " + id + " has progressed beyond default level. Skipping.");
							return;
						}

						final long lastActivity = data.getLastIslandActivity();
						final long millisSinceLastSeen = System.currentTimeMillis() - lastActivity;

						if (millisSinceLastSeen <= TimeUnit.DAYS.toMillis(purgeDays)) {
							instance.debug("Player " + id + " was active " + (millisSinceLastSeen / 1000 / 60 / 60 / 24)
									+ " days ago. Skipping.");
							return;
						}

						final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
								.getWorld(instance.getWorldManager().getHellblockWorldFormat(data.getIslandId()));

						if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
							instance.getPluginLogger().warn("Failed to purge Hellblock %s: world could not be loaded."
									.formatted(data.getIslandId()));
							return;
						}

						final HellblockWorld<?> world = worldOpt.get();
						data.setAsAbandoned(true);

						protection.updateHellblockMessages(world, data.getOwnerUUID());
						protection.abandonIsland(world, data.getOwnerUUID());

						final HellblockAbandonEvent abandonEvent = new HellblockAbandonEvent(id, data);
						EventUtils.fireAndForget(abandonEvent);

						final int current = purgeCount.incrementAndGet();
						instance.getPluginLogger()
								.info("Hellblock %s marked as abandoned due to inactivity (total so far: %s)"
										.formatted(data.getIslandId(), current));
					}).exceptionally(ex -> {
						instance.getPluginLogger().warn("Error during purge for player " + id + ": " + ex.getMessage(),
								ex);
						return null;
					});

			futures.add(future);
		}

		// Wait for all async purge operations to complete
		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
			final int total = purgeCount.get();
			if (total > 0) {
				instance.getPluginLogger().info("Purge complete: " + total + " Hellblock" + (total == 1 ? "" : "s")
						+ " have been set as abandoned.");
			} else {
				instance.getPluginLogger().info("Purge complete: No Hellblocks qualified for abandonment.");
			}
		});
	}

	/**
	 * Generates the next available Hellblock island ID.
	 * <p>
	 * This method attempts to use a cached maximum ID if the cache is still valid
	 * (based on {@code CACHE_EXPIRY_MS}). If not, it fetches all currently used
	 * island IDs and finds the next available integer not already assigned or
	 * reserved.
	 * <p>
	 * Reserved IDs (e.g., in-progress generations) are tracked to avoid collisions.
	 * The selected ID is cached and reserved before returning.
	 *
	 * <p>
	 * <b>Thread Safety:</b> The internal state (cache, reservation set) is
	 * protected by synchronization on {@code idLock}.
	 *
	 * @return a {@link CompletableFuture} containing the next safe Hellblock island
	 *         ID
	 */
	@NotNull
	public CompletableFuture<Integer> nextHellblockID() {
		long now = System.currentTimeMillis();

		// Use cache if valid
		synchronized (idLock) {
			if (cachedMaxId > 0 && (now - lastCacheUpdate) < CACHE_EXPIRY_MS) {
				int candidate = cachedMaxId + 1;
				while (reservedIds.contains(candidate)) {
					instance.debug("nextHellblockID: Skipping reserved ID (cache): " + candidate);
					candidate++;
				}
				reservedIds.add(candidate);
				cachedMaxId = candidate; // Update cache
				lastCacheUpdate = now;

				instance.debug("nextHellblockID: Using cached max ID. Reserved ID = " + candidate);
				return CompletableFuture.completedFuture(candidate);
			}
		}

		// Refresh ID cache asynchronously
		instance.debug("nextHellblockID: Cache expired or empty. Refreshing used IDs...");

		return instance.getCoopManager().getCachedIslandOwnerData().thenApply(ownerUsers -> {
			synchronized (idLock) {
				Set<Integer> usedIds = ownerUsers.stream().map(UserData::getHellblockData)
						.filter(data -> data.hasHellblock() && data.getOwnerUUID() != null)
						.map(HellblockData::getIslandId).filter(id -> id > 0).collect(Collectors.toSet());

				usedIds.addAll(reservedIds); // Avoid collisions

				int newId = 1;
				while (usedIds.contains(newId)) {
					instance.debug("nextHellblockID: Skipping used/reserved ID = " + newId);
					newId++;
				}

				// Reserve + update cache
				reservedIds.add(newId);
				cachedMaxId = newId;
				lastCacheUpdate = System.currentTimeMillis();

				instance.debug("nextHellblockID: Assigned new Hellblock ID = " + newId);
				return newId;
			}
		});
	}

	/**
	 * Invalidates the cached maximum Hellblock ID and resets the last cache update
	 * timestamp.
	 * <p>
	 * This forces the system to recalculate the maximum ID from persistent storage
	 * on the next access. Should be called when islands are added or removed
	 * externally.
	 */
	public void invalidateHellblockIDCache() {
		long now = System.currentTimeMillis();
		synchronized (idLock) {
			if ((now - lastInvalidationTime) < INVALIDATION_COOLDOWN_MS) {
				instance.debug("invalidateHellblockIDCache: Skipping redundant invalidation (too recent).");
				return;
			}
			cachedMaxId = -1;
			lastCacheUpdate = 0;
			lastInvalidationTime = now;
			instance.debug("invalidateHellblockIDCache: Cache invalidated.");
		}
	}

	/**
	 * Asynchronously locates a safe bedrock position for the player's hellblock. If
	 * no bedrock is found, or if the bedrock position is unsafe, it falls back to
	 * the hellblock's home location.
	 *
	 * @param ownerData The data of the player whose hellblock is to be located.
	 * @return A CompletableFuture that resolves to the safe Location.
	 */
	@Nullable
	public CompletableFuture<Location> locateNearestBedrock(@NotNull UserData ownerData) {
		CompletableFuture<Location> locationFuture = new CompletableFuture<>();

		instance.debug("Starting bedrock location search for UUID: " + ownerData.getUUID());

		HellblockData hellblockData = ownerData.getHellblockData();
		int hellblockId = hellblockData.getIslandId();
		Location hellblockLoc = hellblockData.getHellblockLocation();

		if (hellblockLoc == null) {
			instance.getPluginLogger().severe("Hellblock location not found for hellblock ID: " + hellblockId);
			locationFuture.completeExceptionally(
					new IllegalStateException("Hellblock location not set before locateNearestBedrock"));
			return locationFuture;
		}

		instance.debug("Hellblock location found for ID " + hellblockId + ": " + hellblockLoc);

		Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(hellblockId));

		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
			instance.getPluginLogger().severe("World not found for hellblock ID: " + hellblockId);
			instance.debug("Falling back to hellblock location: " + hellblockLoc);
			locationFuture.complete(hellblockLoc);
			return locationFuture;
		}

		HellblockWorld<?> hellWorld = worldOpt.get();
		World world = hellWorld.bukkitWorld();
		instance.debug("World found for hellblock ID " + hellblockId + ": " + world.getName());

		instance.getProtectionManager().getHellblockBlocks(hellWorld, ownerData.getUUID()).thenAccept(positions -> {
			instance.debug("Retrieved " + positions.size() + " blocks for UUID: " + ownerData.getUUID());

			List<CompletableFuture<Pos3>> bedrockCandidates = new ArrayList<>();

			for (Pos3 pos : positions) {
				CompletableFuture<Pos3> future = hellWorld.getBlockState(pos).thenApply(optionalState -> {
					if (optionalState.isPresent()
							&& optionalState.get().type() == CustomBlockTypes.fromMaterial(Material.BEDROCK)) {
						return pos;
					}
					return null;
				});
				bedrockCandidates.add(future);
			}

			CompletableFuture.allOf(bedrockCandidates.toArray(CompletableFuture[]::new))
					.thenApply(v -> bedrockCandidates.stream().map(CompletableFuture::join).filter(Objects::nonNull)
							.min(Comparator.comparingDouble(pos -> pos.toLocation(world).distanceSquared(hellblockLoc)))
							.orElse(null))
					.thenAccept(nearestBedrockPosition -> {
						if (nearestBedrockPosition == null) {
							instance.debug("No bedrock blocks found. Using hellblock location: " + hellblockLoc);
							locationFuture.complete(hellblockLoc);
							return;
						}

						instance.debug("Nearest bedrock block found at: " + nearestBedrockPosition.toLocation(world));

						Location candidate = world.getHighestBlockAt(nearestBedrockPosition.toLocation(world))
								.getLocation().clone().add(0.5, 1, 0.5);

						instance.debug("Candidate safe location above bedrock: " + candidate);

						LocationUtils.isSafeLocationAsync(candidate).thenAccept(safe -> {
							if (safe) {
								instance.debug("Location is safe. Returning: " + candidate);
								locationFuture.complete(candidate);
							} else {
								instance.debug("Location is NOT safe. Falling back to: " + hellblockLoc);
								locationFuture.complete(hellblockLoc);
							}
						});
					});
		});

		return locationFuture;
	}

	/**
	 * Asynchronously retrieves the HellblockData for the hellblock located at the
	 * given world and location.
	 *
	 * @param world    The world to check.
	 * @param location The location to check.
	 * @return A CompletableFuture that resolves to the HellblockData if found,
	 *         otherwise null.
	 */
	@Nullable
	public CompletableFuture<HellblockData> getHellblockByWorld(@NotNull World world, @NotNull Location location) {
		return instance.getCoopManager().getCachedIslandOwnerData().thenApply(users -> {
			if (users == null || users.isEmpty()) {
				return null;
			}

			for (UserData user : users) {
				HellblockData data = user.getHellblockData();
				if (data.hasHellblock() && data.getOwnerUUID() != null) {
					BoundingBox box = data.getBoundingBox();
					if (box != null && box.contains(location.toVector())) {
						return data; // Found a matching island
					}
				}
			}
			return null; // No island matched
		});
	}

	/**
	 * Checks if the player is in the correct world based on the plugin's
	 * configuration.
	 *
	 * @param player The player to check.
	 * @return True if the player is in the correct world, false otherwise.
	 */
	public boolean isInCorrectWorld(@NotNull Player player) {
		if (!instance.getConfigManager().perPlayerWorlds()) {
			return player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName());
		}

		return player.getWorld().getEnvironment() == Environment.NETHER && player.getWorld().getGenerator() != null
				&& player.getWorld().getGenerator().equals(
						getDefaultWorldGenerator(player.getWorld().getName(), player.getWorld().getUID().toString()))
				&& player.getWorld().getName().startsWith(WorldManager.WORLD_PREFIX);
	}

	/**
	 * Checks if the given world is a valid hellblock world based on the plugin's
	 * configuration.
	 *
	 * @param world The world to check.
	 * @return True if the world is a valid hellblock world, false otherwise.
	 */
	public boolean isInCorrectWorld(@NotNull World world) {
		if (!instance.getConfigManager().perPlayerWorlds()) {
			return world.getName().equalsIgnoreCase(instance.getConfigManager().worldName());
		}

		return world.getEnvironment() == Environment.NETHER && world.getGenerator() != null
				&& world.getGenerator().equals(getDefaultWorldGenerator(world.getName(), world.getUID().toString()))
				&& world.getName().startsWith(WorldManager.WORLD_PREFIX);
	}

	/**
	 * Provides the default chunk generator for hellblock worlds. This is a void
	 * generator to ensure a blank canvas for island generation.
	 *
	 * @param worldName The name of the world.
	 * @param id        The unique identifier of the world.
	 * @return A ChunkGenerator that generates void chunks.
	 */
	@NotNull
	public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @NotNull String id) {
		return new VoidGenerator();
	}

	/**
	 * Retrieves a safe spawn location for the user based on their Hellblock home.
	 * <p>
	 * Uses the user's saved home location. If unsafe (e.g., air or liquid),
	 * attempts to find a nearby solid block above. Falls back to the configured
	 * default height if no safe spot is found. If the home or world is unavailable,
	 * logs a warning and returns {@code null}.
	 *
	 * @param userData the user whose home location should be used
	 * @return a safe {@link Location} near the user's home, or {@code null} if it
	 *         cannot be determined
	 */
	@Nullable
	public Location getSafeSpawnLocation(@NotNull UserData userData) {
		try {
			final HellblockData hellblockData = userData.getHellblockData();
			Location home = hellblockData.getHomeLocation();

			if (home == null || home.getWorld() == null) {
				instance.getPluginLogger().warn("Home location or world is null for user " + userData.getUUID());
				return null;
			}

			Location safe = home.clone();
			Block block = safe.getBlock();
			int attempts = 0;

			// Try moving up to find a safe block (not in air or liquid)
			while ((block.isEmpty() || block.isLiquid()) && attempts < 10) {
				safe.clone().add(0, 1, 0);
				block = safe.getBlock();
				attempts++;
			}

			// Fallback to configured Y height if still unsafe
			if (block.isEmpty() || block.isLiquid()) {
				safe.clone().setY(instance.getConfigManager().height());
			}

			// Center player slightly above block
			safe.clone().add(0.5, 0.1, 0.5);

			return safe;
		} catch (Exception ex) {
			instance.getPluginLogger().warn("Failed to resolve safe home spawn for user " + userData.getUUID(), ex);
			return null;
		}
	}

	/**
	 * Teleports the player to spawn using the configured spawn command. If not
	 * forced, also sends a warning message about unsafe conditions.
	 *
	 * @param player The player to teleport.
	 * @param forced If true, no warning message is sent.
	 */
	public void teleportToSpawn(@NotNull Player player, boolean forced) {
		if (!forced) {
			instance.getSenderFactory().wrap(player).sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_UNSAFE_CONDITIONS.build()));
		}
		instance.getSenderFactory().wrap(player).performCommand(instance.getConfigManager().spawnCommand());
	}

	/**
	 * Marks a player as currently undergoing an island or schematic creation.
	 * <p>
	 * This flag is used to temporarily restrict player ownership checks and
	 * interactions during creation of their island.
	 * </p>
	 *
	 * @param playerId The unique ID of the player to mark as creating.
	 */
	public void startCreationProcess(@NotNull UUID playerId) {
		creationPlayers.add(playerId);
	}

	/**
	 * Removes a player's creation state, restoring their normal interaction
	 * ability.
	 *
	 * @param playerId The unique ID of the player to unmark.
	 */
	public boolean endCreationProcess(@NotNull UUID playerId) {
		return creationPlayers.remove(playerId);
	}

	/**
	 * Checks if a player is currently creating an island.
	 *
	 * @param playerId The unique ID of the player to check.
	 * @return True if the player is creating, false otherwise.
	 */
	public boolean creationProcessing(@NotNull UUID playerId) {
		return creationPlayers.contains(playerId);
	}

	/**
	 * Marks a player as currently undergoing an island reset.
	 * <p>
	 * This flag is used to temporarily restrict player ownership checks and
	 * interactions during reset of their island.
	 * </p>
	 *
	 * @param playerId The unique ID of the player to mark as resetting.
	 */
	public void startResetProcess(@NotNull UUID playerId) {
		resetPlayers.add(playerId);
	}

	/**
	 * Removes a player's reset state, restoring their normal interaction ability.
	 *
	 * @param playerId The unique ID of the player to unmark.
	 */
	public boolean endResetProcess(@NotNull UUID playerId) {
		return resetPlayers.remove(playerId);
	}

	/**
	 * Checks if a player is currently resetting an island.
	 *
	 * @param playerId The unique ID of the player to check.
	 * @return True if the player is resetting, false otherwise.
	 */
	public boolean resetProcessing(@NotNull UUID playerId) {
		return resetPlayers.contains(playerId);
	}

	public record GenerationProfile(@NotNull IslandOptions option, boolean animated, boolean reset,
			boolean perPlayerWorld) {
	}
}