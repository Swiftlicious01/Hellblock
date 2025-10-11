package com.swiftlicious.hellblock.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.io.Files;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.events.hellblock.HellblockAbandonEvent;
import com.swiftlicious.hellblock.events.hellblock.HellblockCreateEvent;
import com.swiftlicious.hellblock.events.hellblock.HellblockPostCreationEvent;
import com.swiftlicious.hellblock.events.hellblock.HellblockResetEvent;
import com.swiftlicious.hellblock.events.hellblock.HellblockRollbackEvent;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;
import com.swiftlicious.hellblock.player.mailbox.MailboxFlag;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.schematic.EntitySnapshot;
import com.swiftlicious.hellblock.schematic.IslandSnapshot;
import com.swiftlicious.hellblock.schematic.IslandSnapshotBlock;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.text.Component;

public class HellblockHandler {

	protected final HellblockPlugin instance;

	public HellblockHandler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public CompletableFuture<Void> createHellblock(@NotNull Player player, @NotNull IslandOptions islandChoice,
			@Nullable String schematic, boolean isReset) {

		final Optional<UserData> userOpt = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		final Sender audience = instance.getSenderFactory().wrap(player);

		if (userOpt.isEmpty()) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED.build()));
			return CompletableFuture.completedFuture(null);
		}

		final UserData user = userOpt.get();

		return nextHellblockID(player.getUniqueId()).thenCompose(id -> {
			if (id == 0) {
				instance.getPluginLogger()
						.severe("Failed to retrieve next hellblock ID for player: %s".formatted(user.getName()));
				return CompletableFuture.completedFuture(null);
			}

			final HellblockCreateEvent createEvent = new HellblockCreateEvent(player, user, islandChoice, id);
			Bukkit.getPluginManager().callEvent(createEvent);
			if (createEvent.isCancelled()) {
				return CompletableFuture.completedFuture(null);
			}

			// Step 1: create and prepare world (async if slime world)
			// Step 2: resolve actual island location (async)
			// Step 3: initialize hellblock data
			// Step 4: protect world and generate island (async)
			// Step 5: run post generation tasks (async)
			return prepareWorld(id).thenCompose(world -> resolveIslandLocation(world, id).thenCompose(islandLoc -> {
				initializeHellblockData(user, player, islandChoice, id, islandLoc);
				return protectAndGenerateIsland(world, user, player, islandChoice, schematic, islandLoc)
						.thenCompose(v -> postGenerationTasks(user, player, world, isReset));
			}));
		});
	}

	public CompletableFuture<Void> createHellblock(@NotNull Player player, @NotNull IslandOptions islandChoice,
			boolean isReset) {
		return createHellblock(player, islandChoice, null, isReset);
	}

	private CompletableFuture<? extends HellblockWorld<?>> prepareWorld(int id) {
		String worldName = instance.getWorldManager().getHellblockWorldFormat(id);
		return instance.getWorldManager().adapter().createWorld(worldName);
	}

	public Location determineSpawnLocation(HellblockWorld<?> world, UserData userData) {
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

	public CompletableFuture<Location> resolveIslandLocation(HellblockWorld<?> world, int id) {
		if (instance.getConfigManager().perPlayerWorlds()) {
			return CompletableFuture
					.completedFuture(new Location(world.bukkitWorld(), 0, instance.getConfigManager().height(), 0));
		}

		// Shared world → find next available location using spiral algorithm
		return instance.getPlacementDetector().findNextIslandLocation();
	}

	private void initializeHellblockData(UserData user, Player player, IslandOptions choice, int id,
			Location location) {
		if (!player.isOnline()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + " disconnected before initializing HellblockData.");
			return;
		}
		final HellblockData hellblockData = user.getHellblockData();
		hellblockData.setDefaultHellblockData(true, location, id);
		hellblockData.setIslandChoice(choice);
		hellblockData.setBiomeCooldown(0L);
		hellblockData.setLockedStatus(false);
		hellblockData.setOwnerUUID(player.getUniqueId());
		hellblockData.setProtectionFlags(new HashMap<>());
		hellblockData.setIslandUpgrades(new EnumMap<>(IslandUpgradeType.class));
		hellblockData.setParty(new HashSet<>());
		hellblockData.setTrusted(new HashSet<>());
		hellblockData.setBanned(new HashSet<>());
	}

	private CompletableFuture<Void> protectAndGenerateIsland(HellblockWorld<?> world, UserData user, Player player,
			IslandOptions choice, @Nullable String schematic, Location location) {
		if (!player.isOnline()) {
			instance.getPluginLogger().warn("Player " + player.getName() + " disconnected before generation.");
			return CompletableFuture.completedFuture(null);
		}
		final HellblockData hellblockData = user.getHellblockData();
		final Sender audience = instance.getSenderFactory().wrap(player);

		return instance.getProtectionManager().getIslandProtection().protectHellblock(world.bukkitWorld(), user)
				.thenCompose(v -> {
					final CompletableFuture<Void> genTask;
					if (choice == IslandOptions.SCHEMATIC && schematic != null && !schematic.isEmpty()
							&& instance.getSchematicManager().schematicFiles.keySet().stream()
									.anyMatch(sch -> Files.getNameWithoutExtension(sch)
											.equalsIgnoreCase(Files.getNameWithoutExtension(schematic)))) {
						hellblockData.setUsedSchematic(Files.getNameWithoutExtension(schematic));
						genTask = instance.getIslandChoiceConverter().convertIslandChoice(world.bukkitWorld(), player,
								location, schematic);
					} else {
						genTask = instance.getIslandChoiceConverter().convertIslandChoice(world.bukkitWorld(), player,
								location);
					}
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_CREATE_PROCESS.build()));
					return genTask;
				});
	}

	private CompletableFuture<Void> postGenerationTasks(UserData user, Player player, HellblockWorld<?> world,
			boolean isReset) {
		if (!player.isOnline()) {
			instance.getPluginLogger().warn("Player " + user.getName() + " disconnected before post-generation tasks.");
			instance.getMailboxManager().queueMailbox(player.getUniqueId(), MailboxFlag.RESET_ANIMATION);
			return CompletableFuture.completedFuture(null);
		}
		final HellblockData hellblockData = user.getHellblockData();

		if (hellblockData.getHomeLocation() == null) {
			throw new NullPointerException(
					"Hellblock home location returned null, please report this to the developer.");
		}

		return LocationUtils.isSafeLocationAsync(hellblockData.getHomeLocation()).thenCompose(isSafe -> {
			if (!isSafe) {
				return locateBedrock(player.getUniqueId()).thenAccept(hellblockData::setHomeLocation);
			}
			return CompletableFuture.completedFuture(null);
		}).thenCompose(v -> waitForSafeSpawn(hellblockData.getHomeLocation(), 10))
				.thenCompose(v -> teleportPlayerToHome(player, hellblockData)).thenRun(() -> {
					hellblockData.setCreation(System.currentTimeMillis());
					user.startSpawningAnimals();
					user.startSpawningFortressMobs();
					instance.getBorderHandler().startBorderTask(player);
					instance.getBiomeHandler().setHellblockBiome(world.bukkitWorld(), hellblockData.getBoundingBox(),
							hellblockData.getBiome().getConvertedBiome());
					instance.debug("Creating new hellblock for %s".formatted(player.getName()));
					if (isReset) {
						hellblockData.setResetCooldown(TimeUnit.SECONDS.toDays(86400));
					}
					final HellblockPostCreationEvent postCreationEvent = new HellblockPostCreationEvent(player, user,
							hellblockData);
					Bukkit.getPluginManager().callEvent(postCreationEvent);
				});
	}

	private CompletableFuture<Void> waitForSafeSpawn(@NotNull Location home, int maxTries) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		AtomicInteger attempts = new AtomicInteger(0);
		Location safeCheckLoc = home.clone();

		instance.getScheduler().sync().runRepeating(new BukkitRunnable() {
			@Override
			public void run() {
				World world = safeCheckLoc.getWorld();
				if (world == null) {
					future.completeExceptionally(new IllegalStateException("Home world is null."));
					cancel();
					return;
				}

				Block blockBelow = world.getBlockAt(safeCheckLoc.getBlockX(), safeCheckLoc.getBlockY() - 1,
						safeCheckLoc.getBlockZ());

				if (blockBelow.getType().isSolid()) {
					future.complete(null);
					cancel();
					return;
				}

				if (attempts.incrementAndGet() >= maxTries) {
					instance.getPluginLogger().warn("Failed to detect solid ground at home location after " + maxTries
							+ " attempts. Proceeding anyway.");
					future.complete(null); // Fail-safe
					cancel();
				}
			}
		}, 1L, 2L, safeCheckLoc); // Start after 1 tick, check every 2 ticks

		return future;
	}

	private CompletableFuture<Boolean> teleportPlayerToHome(Player player, HellblockData hellblockData) {
		Location homeLocation = hellblockData.getHomeLocation();

		if (homeLocation == null) {
			return CompletableFuture.completedFuture(false); // no home = can't teleport
		}

		return ChunkUtils.teleportAsync(player, homeLocation, TeleportCause.PLUGIN).thenApply(success -> {
			if (success) {
				World world = homeLocation.getWorld();
				if (world != null) {
					instance.getWorldManager().markWorldAccess(world.getName());
				}
			}
			return success;
		});
	}

	public void showCreationTitleAndSound(Player player) {
		if (instance.getConfigManager().creationTitleScreen() != null
				&& instance.getConfigManager().creationTitleScreen().enabled()) {
			VersionHelper.getNMSManager().sendTitle(player,
					AdventureHelper.componentToJson(AdventureHelper.miniMessage(instance.getConfigManager()
							.creationTitleScreen().title().replace("{player}", player.getName()))),
					AdventureHelper.componentToJson(AdventureHelper.miniMessage(instance.getConfigManager()
							.creationTitleScreen().subtitle().replace("{player}", player.getName()))),
					instance.getConfigManager().creationTitleScreen().fadeIn(),
					instance.getConfigManager().creationTitleScreen().stay(),
					instance.getConfigManager().creationTitleScreen().fadeOut());
		}

		if (instance.getConfigManager().creatingHellblockSound() != null) {
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					instance.getConfigManager().creatingHellblockSound());
		}
	}

	public CompletableFuture<Void> resetHellblock(@NotNull UUID id, boolean forceReset,
			@Nullable String executorNameForReset) {
		return instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenComposeAsync(result -> {
					if (result.isEmpty()) {
						return CompletableFuture.completedFuture(null);
					}

					final UserData offlineUser = result.get();
					final HellblockWorld<?> world = getHellblockWorld(offlineUser);
					final World bukkitWorld = world.bukkitWorld();
					final Location home = offlineUser.getHellblockData().getHomeLocation();
					final int hellblockID = offlineUser.getHellblockData().getID();

					// Step 1: create snapshot before reset
					return instance.getIslandBackupManager().createPreResetSnapshot(id).thenComposeAsync(snapshotTs -> {

						final HellblockResetEvent resetEvent = new HellblockResetEvent(id, offlineUser, forceReset);
						Bukkit.getPluginManager().callEvent(resetEvent);

						final Map<Block, Material> blockChanges = new LinkedHashMap<>();

						// Notify owner (must run sync)
						if (!forceReset && Bukkit.getPlayer(id) != null) {
							instance.getScheduler().executeSync(() -> notifyOwnerStartReset(Bukkit.getPlayer(id)),
									home);
						}

						// Step 2: perform reset (returns CompletableFuture<Void>)
						return performReset(offlineUser, id, bukkitWorld, home, blockChanges, forceReset, hellblockID,
								executorNameForReset);
					});
				});
	}

	private HellblockWorld<?> getHellblockWorld(UserData offlineUser) {
		return instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(offlineUser.getHellblockData().getID()))
				.orElseThrow(() -> new NullPointerException(
						"World returned null, please try to regenerate the world before reporting this issue."));
	}

	private void notifyOwnerStartReset(Player owner) {
		final Sender audience = instance.getSenderFactory().wrap(owner);
		audience.sendMessage(
				instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_RESET_PROCESS.build()));
	}

	private CompletableFuture<Void> performReset(UserData offlineUser, UUID id, World bukkitWorld, Location home,
			Map<Block, Material> blockChanges, boolean forceReset, int hellblockID,
			@Nullable String executorNameForReset) {
		// Run biome change sync
		instance.getScheduler()
				.executeSync(() -> instance.getBiomeHandler().setHellblockBiome(bukkitWorld,
						offlineUser.getHellblockData().getBoundingBox(), HellBiome.NETHER_WASTES.getConvertedBiome()),
						home);

		instance.getProtectionManager().cancelBlockScan(id);
		offlineUser.stopSpawningAnimals();
		offlineUser.stopSpawningFortressMobs();
		instance.getBorderHandler().stopBorderTask(id);

		// Gather blocks async, but world edits queued later as sync
		return instance.getProtectionManager().getHellblockBlocks(bukkitWorld, id).thenCompose(blocks -> {
			blocks.forEach(block -> {
				if (!block.isEmpty()) {
					blockChanges.put(block, Material.AIR);
				}
			});
			return unprotectAndResetIsland(offlineUser, id, bukkitWorld, home, blockChanges, forceReset, hellblockID,
					executorNameForReset);
		});
	}

	private CompletableFuture<Void> unprotectAndResetIsland(UserData offlineUser, UUID id, World bukkitWorld,
			Location home, Map<Block, Material> blockChanges, boolean forceReset, int hellblockID,
			@Nullable String executorNameForReset) {
		return instance.getProtectionManager().getIslandProtection().unprotectHellblock(bukkitWorld, id)
				.thenCompose(v -> handlePartyMembers(offlineUser, forceReset, executorNameForReset))
				.thenCompose(v -> handleVisitors(id)).thenRun(() -> {
					// Reset owner must run sync
					instance.getScheduler().executeSync(
							() -> resetOwnerData(offlineUser, forceReset, blockChanges, home, executorNameForReset),
							home);

					// GUI opening or world deletion must run sync
					instance.getScheduler().executeSync(() -> finalizeReset(offlineUser, forceReset, hellblockID, home),
							home);
				});
	}

	private CompletableFuture<Void> handlePartyMembers(UserData offlineUser, boolean forceReset,
			@Nullable String executorNameForReset) {
		final Set<UUID> party = offlineUser.getHellblockData().getParty();
		if (party.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		final List<CompletableFuture<Void>> futures = new ArrayList<>();

		party.forEach(uuid -> {
			final Player member = Bukkit.getPlayer(uuid);
			if (member != null && member.isOnline()) {
				futures.add(handleOnlinePartyMember(member, offlineUser, forceReset, executorNameForReset));
			} else {
				futures.add(handleOfflinePartyMember(uuid, forceReset, executorNameForReset));
			}
		});

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
	}

	private CompletableFuture<Void> handleOnlinePartyMember(Player member, UserData offlineUser, boolean forceReset,
			@Nullable String executorNameForReset) {
		final Optional<UserData> onlineMember = instance.getStorageManager().getOnlineUser(member.getUniqueId());
		if (onlineMember.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		final UserData data = onlineMember.get();
		data.getHellblockData().resetHellblockData();
		data.stopSpawningAnimals();
		data.stopSpawningFortressMobs();
		instance.getBorderHandler().stopBorderTask(member.getUniqueId());

		if (instance.getConfigManager().resetInventory()) {
			member.getInventory().clear();
			member.getInventory().setArmorContents(null);
		}
		if (instance.getConfigManager().resetEnderchest()) {
			member.getEnderChest().clear();
		}

		teleportToSpawn(member, true);

		final Sender audience = instance.getSenderFactory().wrap(member);
		if (!forceReset && offlineUser.isOnline()) {
			final Player owner = Bukkit.getPlayer(offlineUser.getUUID());
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_RESET_PARTY_NOTIFICATION
							.arguments(AdventureHelper.miniMessage(owner.getName())).build()));
		} else if (forceReset) {
			audience.sendMessage(instance.getTranslationManager()
					.render(MessageConstants.MSG_HELLBLOCK_RESET_PARTY_FORCED_NOTIFICATION
							.arguments(AdventureHelper.miniMessage(executorNameForReset)).build()));
		}

		return CompletableFuture.completedFuture(null);
	}

	private CompletableFuture<Void> handleOfflinePartyMember(UUID uuid, boolean forceReset,
			@Nullable String executorNameForReset) {
		return instance.getStorageManager().getOfflineUserData(uuid, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						return;
					}
					UUID ownerUUID = result.get().getHellblockData().getOwnerUUID();
					String ownerName = "???";
					if (ownerUUID != null) {
						ownerName = Bukkit.getOfflinePlayer(ownerUUID).hasPlayedBefore()
								&& Bukkit.getOfflinePlayer(ownerUUID).getName() != null
										? Bukkit.getOfflinePlayer(ownerUUID).getName()
										: "???";
					}
					result.get().getHellblockData().resetHellblockData();
					final PlayerData data = result.get().toPlayerData();
					instance.getMailboxManager().queue(data.getUUID(),
							new MailboxEntry(
									forceReset ? "message.hellblock.coop.deleted.offline"
											: "message.hellblock.coop.reset.offline",
									forceReset ? List.of(Component.text(executorNameForReset))
											: List.of(Component.text(ownerName)),
									Set.of(MailboxFlag.RESET_INVENTORY, MailboxFlag.RESET_ENDERCHEST,
											MailboxFlag.UNSAFE_LOCATION, MailboxFlag.NOTIFY_PARTY)));
				});
	}

	private CompletableFuture<Void> handleVisitors(UUID id) {
		return instance.getCoopManager().getVisitors(id).thenCompose(visitors -> {
			if (visitors.isEmpty()) {
				return CompletableFuture.completedFuture(null);
			}

			final List<CompletableFuture<Void>> futures = new ArrayList<>();

			visitors.forEach(uuid -> {
				final Player visitor = Bukkit.getPlayer(uuid);
				if (visitor != null && visitor.isOnline()) {
					futures.add(handleOnlineVisitor(visitor, uuid));
				} else {
					futures.add(handleOfflineVisitor(uuid));
				}
			});

			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		});
	}

	private CompletableFuture<Void> handleOnlineVisitor(Player visitor, UUID uuid) {
		final Optional<UserData> onlineVisitor = instance.getStorageManager().getOnlineUser(uuid);
		if (onlineVisitor.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		final UserData visitorData = onlineVisitor.get();
		if (visitorData.getHellblockData().hasHellblock()) {
			final UUID ownerUUID = visitorData.getHellblockData().getOwnerUUID();
			if (ownerUUID == null) {
				throw new NullPointerException("Owner reference returned null, please report this to the developer.");
			}

			return instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(owner -> {
						if (owner.isEmpty()) {
							return;
						}
						instance.getCoopManager().makeHomeLocationSafe(owner.get(), visitorData);
					});
		} else {
			teleportToSpawn(visitor, true);
		}

		return CompletableFuture.completedFuture(null);
	}

	private CompletableFuture<Void> handleOfflineVisitor(UUID uuid) {
		return instance.getStorageManager().getOfflineUserData(uuid, instance.getConfigManager().lockData())
				.thenAccept(visitorResult -> {
					if (visitorResult.isEmpty()) {
						return;
					}
					final PlayerData data = visitorResult.get().toPlayerData();
					instance.getMailboxManager().queueMailbox(data.getUUID(), MailboxFlag.UNSAFE_LOCATION);
				});
	}

	private void resetOwnerData(UserData offlineUser, boolean forceReset, Map<Block, Material> blockChanges,
			Location home, @Nullable String executorNameForReset) {
		offlineUser.getHellblockData().resetHellblockData();
		final PlayerData data = offlineUser.toPlayerData();
		if (!forceReset && !offlineUser.isOnline()) {
			instance.getMailboxManager().queueMailbox(data.getUUID(), MailboxFlag.SHOW_RESET_GUI);
		}

		if (offlineUser.isOnline()) {
			final Player player = Bukkit.getPlayer(offlineUser.getUUID());
			final Sender audience = instance.getSenderFactory().wrap(player);
			if (!forceReset) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_RESET_NEW_OPTION.build()));
			} else {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_RESET_OWNER_FORCED_NOTIFICATION
								.arguments(AdventureHelper.miniMessage(executorNameForReset)).build()));
			}

			if (instance.getConfigManager().resetInventory()) {
				player.getInventory().clear();
				player.getInventory().setArmorContents(null);
			}
			if (instance.getConfigManager().resetEnderchest()) {
				player.getEnderChest().clear();
			}

			teleportToSpawn(player, true);
		} else {
			instance.getMailboxManager().queue(data.getUUID(),
					new MailboxEntry(
							forceReset ? "message.hellblock.coop.deleted.offline"
									: "message.hellblock.coop.reset.offline",
							forceReset ? List.of(Component.text(executorNameForReset)) : null,
							Set.of(MailboxFlag.RESET_INVENTORY, MailboxFlag.RESET_ENDERCHEST,
									MailboxFlag.UNSAFE_LOCATION, MailboxFlag.NOTIFY_OWNER)));
		}

		instance.getScheduler().executeSync(() -> blockChanges.forEach((change, type) -> {
			if (change.getState() instanceof BlockInventoryHolder holder) {
				holder.getInventory().clear();
			}
			change.setType(type);
		}), home);
	}

	private void finalizeReset(UserData offlineUser, boolean forceReset, int hellblockID, Location home) {
		if (!forceReset && offlineUser.isOnline()) {
			final Player player = Bukkit.getPlayer(offlineUser.getUUID());
			instance.getScheduler().sync()
					.runLater(() -> instance.getIslandChoiceGUIManager().openIslandChoiceGUI(player, true), 20L, home);
		} else if (forceReset) {
			if (instance.getConfigManager().perPlayerWorlds()) {
				instance.getWorldManager().adapter()
						.deleteWorld(instance.getWorldManager().getHellblockWorldFormat(hellblockID));
			}
		}
	}

	public void ensureSafety(@NotNull Player player, @NotNull UserData onlineUser) {
		if (player.getLocation() == null) {
			return;
		}

		LocationUtils.isSafeLocationAsync(player.getLocation()).thenAccept(isSafe -> {
			if (isSafe) {
				return;
			}

			if (onlineUser.getHellblockData().hasHellblock()) {
				final UUID ownerUUID = onlineUser.getHellblockData().getOwnerUUID();
				if (ownerUUID == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}

				instance.getStorageManager().getOfflineUserData(ownerUUID, false).thenAccept(optionalOwner -> {
					if (optionalOwner.isEmpty()) {
						// No owner data → fallback to spawn
						instance.getScheduler().executeSync(() -> teleportToSpawn(player, false));
						return;
					}

					final UserData ownerUser = optionalOwner.get();
					if (ownerUser.getHellblockData().getHomeLocation() != null) {
						instance.getScheduler().executeSync(
								() -> instance.getCoopManager().makeHomeLocationSafe(ownerUser, onlineUser));
					} else {
						instance.getScheduler().executeSync(() -> teleportToSpawn(player, false));
					}
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe("Error retrieving owner data for player " + player.getName(), ex);
					instance.getScheduler().executeSync(() -> teleportToSpawn(player, false));
					return null;
				});
			} else {
				instance.getScheduler().executeSync(() -> teleportToSpawn(player, false));
			}
		});
	}

	public void handleVisitingIsland(@NotNull Player player, @NotNull UserData onlineUser) {
		final UUID playerId = player.getUniqueId();

		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				return CompletableFuture.completedFuture(null);
			}
			return instance.getCoopManager().trackBannedPlayer(ownerUUID, playerId)
					.thenApply(isBanned -> isBanned ? ownerUUID : null);
		}).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}

			instance.getCoopManager().kickVisitorsIfLocked(ownerUUID);

			if (onlineUser.getHellblockData().hasHellblock()) {
				final UUID hellblockOwner = onlineUser.getHellblockData().getOwnerUUID();
				if (hellblockOwner == null) {
					instance.getPluginLogger().severe("Owner reference returned null for user " + playerId
							+ " (please report this to the developer).");
					return;
				}

				instance.getStorageManager().getOfflineUserData(hellblockOwner, instance.getConfigManager().lockData())
						.thenCompose(optionalOwner -> {
							if (optionalOwner.isEmpty()) {
								return CompletableFuture.completedFuture(null);
							}

							final UserData bannedOwner = optionalOwner.get();

							return instance.getCoopManager().makeHomeLocationSafe(bannedOwner, onlineUser)
									// Ensure sync marking and safe teleport only after async home preparation
									.thenRun(() -> instance.getScheduler().executeSync(() -> {
										World world = player.getWorld();
										if (world != null) {
											instance.getWorldManager().markWorldAccess(world.getName());
										}
									}));
						}).exceptionally(ex -> {
							instance.getPluginLogger().severe("Error fetching offline owner data for " + playerId, ex);
							return null;
						});

			} else {
				// Fallback: no hellblock data → teleport to spawn safely
				instance.getScheduler().executeSync(() -> {
					World world = player.getWorld();
					if (world != null) {
						instance.getWorldManager().markWorldAccess(world.getName());
					}
					teleportToSpawn(player, true);
				});
			}
		}).exceptionally(ex -> {
			instance.getPluginLogger().severe("Error handling visiting island logic for player " + playerId, ex);
			return null;
		});
	}

	public CompletableFuture<IslandSnapshot> captureIslandSnapshot(UUID ownerId) {
		final CompletableFuture<IslandSnapshot> future = new CompletableFuture<>();

		instance.getStorageManager().getOfflineUserData(ownerId, instance.getConfigManager().lockData())
				.thenAccept(optionalUserData -> {
					if (optionalUserData.isEmpty()) {
						future.complete(new IslandSnapshot(List.of(), List.of()));
						return;
					}

					final HellblockData hb = optionalUserData.get().getHellblockData();
					final Optional<HellblockWorld<?>> optWorld = instance.getWorldManager()
							.getWorld(instance.getWorldManager().getHellblockWorldFormat(hb.getID()));

					if (optWorld.isEmpty() || optWorld.get().bukkitWorld() == null) {
						future.complete(new IslandSnapshot(List.of(), List.of()));
						return;
					}

					final World world = optWorld.get().bukkitWorld();
					final BoundingBox box = hb.getBoundingBox();

					final List<IslandSnapshotBlock> blocks = Collections.synchronizedList(new ArrayList<>());
					final List<EntitySnapshot> entities = Collections.synchronizedList(new ArrayList<>());

					// Pre-build all block locations
					final List<Location> locations = new ArrayList<>();
					for (int x = (int) box.getMinX(); x <= (int) box.getMaxX(); x++) {
						for (int y = (int) box.getMinY(); y <= (int) box.getMaxY(); y++) {
							for (int z = (int) box.getMinZ(); z <= (int) box.getMaxZ(); z++) {
								locations.add(new Location(world, x, y, z));
							}
						}
					}

					final Iterator<Location> it = locations.iterator();
					final int BATCH_SIZE = 500;

					// use atomic reference so we can cancel later
					final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

					final SchedulerTask task = instance.getScheduler().sync().runRepeating(() -> {
						int processed = 0;
						while (processed < BATCH_SIZE && it.hasNext()) {
							final Location loc = it.next();
							final Block block = loc.getBlock();
							if (!block.getType().isAir()) {
								// snapshot block properly
								blocks.add(IslandSnapshotBlock.fromBlockState(block.getState(), List.of()));
							}
							processed++;
						}

						if (!it.hasNext()) {
							// Capture entities once blocks are done
							world.getNearbyEntities(box).forEach(entity -> {
								if (!(entity instanceof Player)) { // never snapshot players
									entities.add(EntitySnapshot.fromEntity(entity));
								}
							});

							// cancel task safely
							final SchedulerTask t = taskRef.get();
							if (t != null) {
								t.cancel();
							}

							// construct IslandSnapshot with correct record type
							future.complete(new IslandSnapshot(blocks, entities));
						}
					}, 1L, 1L, LocationUtils.getAnyLocationInstance());

					taskRef.set(task); // store reference after scheduling
				});

		return future;
	}

	public CompletableFuture<Void> rollbackIsland(UUID ownerId, long timestamp) {
		return instance.getStorageManager().getOfflineUserData(ownerId, instance.getConfigManager().lockData())
				.thenCompose(optionalUserData -> {
					if (optionalUserData.isEmpty()) {
						instance.getPluginLogger().warn("No userdata found for rollback of " + ownerId);
						return CompletableFuture.completedFuture(null);
					}

					final HellblockData data = optionalUserData.get().getHellblockData();
					final String worldName = instance.getWorldManager().getHellblockWorldFormat(data.getID());
					final Optional<HellblockWorld<?>> world = instance.getWorldManager().getWorld(worldName);
					if (world.isEmpty() || world.get().bukkitWorld() == null) {
						instance.getPluginLogger().warn("World not found for rollback: " + worldName);
						return CompletableFuture.completedFuture(null);
					}

					final World bukkitWorld = world.get().bukkitWorld();
					final BoundingBox box = data.getBoundingBox();

					// Load snapshot async
					return CompletableFuture
							.supplyAsync(() -> instance.getIslandBackupManager().loadSnapshot(ownerId, timestamp))
							.thenAccept(snapshot -> {
								if (snapshot == null) {
									instance.getPluginLogger().warn("No snapshot found for rollback of " + ownerId);
									return;
								}

								final HellblockRollbackEvent rollbackEvent = new HellblockRollbackEvent(ownerId,
										timestamp);
								Bukkit.getPluginManager().callEvent(rollbackEvent);

								// Ensure chunks loaded sync
								instance.getScheduler().executeSync(() -> {
									for (int x = (int) box.getMinX() >> 4; x <= (int) box.getMaxX() >> 4; x++) {
										for (int z = (int) box.getMinZ() >> 4; z <= (int) box.getMaxZ() >> 4; z++) {
											final Chunk chunk = bukkitWorld.getChunkAt(x, z);
											if (!chunk.isLoaded()) {
												chunk.load();
											}
										}
									}
								}, LocationUtils.getAnyLocationInstance());

								// Restore snapshot in batches (safe)
								snapshot.restoreIntoWorldBatched(bukkitWorld, box, instance,
										() -> instance.getPluginLogger().info("Rollback complete for " + ownerId),
										progress -> instance.getPluginLogger().info("Rollback progress for " + ownerId
												+ ": " + "%.1f%%".formatted(progress * 100)));
							});
				});
	}

	public CompletableFuture<Void> rollbackLastMinutes(UUID ownerId, int minutes) {
		final long cutoff = System.currentTimeMillis() - (minutes * 60L * 1000L);
		final List<Long> snapshots = instance.getIslandBackupManager().listSnapshots(ownerId);

		final Optional<Long> chosen = snapshots.stream().filter(ts -> ts >= cutoff).findFirst();

		if (chosen.isPresent()) {
			return rollbackIsland(ownerId, chosen.get());
		}

		instance.getPluginLogger().warn("No snapshot within " + minutes + " minutes for " + ownerId);
		return CompletableFuture.completedFuture(null);
	}

	@SuppressWarnings("deprecation")
	public void purgeInactiveHellblocks() {
		final int purgeDays = instance.getConfigManager().abandonAfterDays();
		if (purgeDays <= 0) {
			return;
		}

		final AtomicInteger purgeCount = new AtomicInteger(0);

		for (UUID id : instance.getStorageManager().getDataSource().getUniqueUsers()) {
			final OfflinePlayer player = Bukkit.getOfflinePlayer(id);

			if (!player.hasPlayedBefore() || player.getLastPlayed() == 0) {
				continue;
			}

			// Time since last online, adjusted by 19 hours
			final long millisSinceLastSeen = System.currentTimeMillis() - player.getLastPlayed()
					- TimeUnit.HOURS.toMillis(19);

			if (millisSinceLastSeen > TimeUnit.DAYS.toMillis(purgeDays)) {
				instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
						.thenAccept(result -> {
							if (result.isEmpty()) {
								return;
							}

							final UserData offlineUser = result.get();
							final HellblockData data = offlineUser.getHellblockData();

							final boolean isOwner = data.hasHellblock() && data.getOwnerUUID() != null
									&& id.equals(data.getOwnerUUID());

							if (isOwner && data.getLevel() == HellblockData.DEFAULT_LEVEL) {
								final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
										.getWorld(instance.getWorldManager().getHellblockWorldFormat(data.getID()));

								if (worldOpt.isEmpty()) {
									instance.getPluginLogger()
											.warn("Failed to purge hellblock %s because its world could not be loaded."
													.formatted(data.getID()));
									return;
								}

								final World bukkitWorld = worldOpt.get().bukkitWorld();
								data.setAsAbandoned(true);

								instance.getProtectionManager().getIslandProtection()
										.updateHellblockMessages(bukkitWorld, data.getOwnerUUID());
								instance.getProtectionManager().getIslandProtection().abandonIsland(bukkitWorld,
										data.getOwnerUUID());

								final HellblockAbandonEvent abandonEvent = new HellblockAbandonEvent(id, data);
								Bukkit.getPluginManager().callEvent(abandonEvent);

								final int current = purgeCount.incrementAndGet();
								instance.getPluginLogger()
										.info("Hellblock %s has been marked abandoned (total so far: %s)"
												.formatted(data.getID(), current));
							}
						});
			}
		}

		// Delay summary log until async tasks finish
		instance.getScheduler().sync().runLater(() -> {
			final int total = purgeCount.get();
			if (total > 0) {
				instance.getPluginLogger()
						.info("A total of %s hellblocks have been set as abandoned.".formatted(total));
			}
		}, 100L, LocationUtils.getAnyLocationInstance()); // ~5 seconds later, tweak if needed
	}

	public CompletableFuture<Integer> nextHellblockID(@NotNull UUID id) {
		final CompletableFuture<Integer> resultFuture = new CompletableFuture<>();

		// Collect all current IDs
		final List<CompletableFuture<Integer>> idFutures = new ArrayList<>();

		instance.getStorageManager().getDataSource().getUniqueUsers().forEach(playerData -> {
			final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerData);
			onlineUser.ifPresentOrElse(
					value -> idFutures.add(CompletableFuture.completedFuture(value.getHellblockData().getID())),
					() -> idFutures.add(instance.getStorageManager()
							.getOfflineUserData(playerData, instance.getConfigManager().lockData())
							.thenApply(opt -> opt.map(u -> u.getHellblockData().getID()).orElse(0))));
		});

		// Combine all futures
		CompletableFuture.allOf(idFutures.toArray(CompletableFuture[]::new)).thenRun(() -> {
			final Set<Integer> usedIds = idFutures.stream().map(CompletableFuture::join).filter(idValue -> idValue > 0)
					.collect(Collectors.toSet());

			int newId = 1;
			while (usedIds.contains(newId)) {
				newId++;
			}

			resultFuture.complete(newId);
		});

		return resultFuture;
	}

	/**
	 * Asynchronously locates a safe bedrock position for the player's hellblock. If
	 * no bedrock is found, or if the bedrock position is unsafe, it falls back to
	 * the hellblock's home location.
	 *
	 * @param id The UUID of the player whose hellblock is to be located.
	 * @return A CompletableFuture that resolves to the safe Location.
	 */
	public CompletableFuture<Location> locateBedrock(@NotNull UUID id) {
		final CompletableFuture<Location> locationFuture = new CompletableFuture<>();

		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept(optUser -> {
					if (optUser.isEmpty()) {
						return;
					}

					final UserData offlineUser = optUser.get();
					final int hellblockId = offlineUser.getHellblockData().getID();

					final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
							.getWorld(instance.getWorldManager().getHellblockWorldFormat(hellblockId));

					if (worldOpt.isEmpty()) {
						throw new NullPointerException("World not found for hellblock " + hellblockId);
					}

					final World bukkitWorld = worldOpt.get().bukkitWorld();
					final Location hellblockLoc = offlineUser.getHellblockData().getHellblockLocation();

					instance.getProtectionManager().getHellblockBlocks(bukkitWorld, offlineUser.getUUID())
							.thenAccept(blocks -> {
								final Block bedrock = blocks.stream()
										.filter(block -> block.getType() == Material.BEDROCK
												&& block.getLocation().equals(hellblockLoc))
										.findFirst().orElse(null);

								if (bedrock == null) {
									locationFuture.complete(hellblockLoc);
									return;
								}

								final Block highestBlock = bukkitWorld.getHighestBlockAt(bedrock.getLocation());
								final Location candidate = highestBlock.getLocation().add(0.5, 1, 0.5);

								LocationUtils.isSafeLocationAsync(candidate).thenAccept(safe -> {
									if (safe) {
										locationFuture.complete(candidate);
									} else {
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
	public CompletableFuture<@Nullable HellblockData> getHellblockByWorld(@NotNull World world,
			@NotNull Location location) {
		// Collect all the futures for offline users
		List<CompletableFuture<Optional<UserData>>> futures = instance.getStorageManager().getDataSource()
				.getUniqueUsers().stream().map(uuid -> instance.getStorageManager().getOfflineUserData(uuid,
						instance.getConfigManager().lockData()))
				.toList();

		// Process them asynchronously
		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
			for (CompletableFuture<Optional<UserData>> future : futures) {
				Optional<UserData> userOpt = future.join(); // safe because allOf is complete
				if (userOpt.isEmpty())
					continue;

				HellblockData data = userOpt.get().getHellblockData();
				if (data.hasHellblock() && !data.isAbandoned()) {
					BoundingBox box = data.getBoundingBox();
					if (box != null && box.contains(location.toVector())) {
						return data;
					}
				}
			}
			return null;
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

		return player.getWorld().getName().startsWith("hellblock_world_");
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

		return world.getEnvironment() == Environment.NETHER
				&& world.getGenerator() == getDefaultWorldGenerator(world.getName(), world.getUID().toString());
	}

	/**
	 * Provides the default chunk generator for hellblock worlds. This is a void
	 * generator to ensure a blank canvas for island generation.
	 *
	 * @param worldName The name of the world.
	 * @param id        The unique identifier of the world.
	 * @return A ChunkGenerator that generates void chunks.
	 */
	public @NotNull ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @NotNull String id) {
		return new VoidGenerator();
	}

	/**
	 * Teleports the player to spawn using the configured spawn command. If not
	 * forced, also sends a warning message about unsafe conditions.
	 *
	 * @param player The player to teleport.
	 * @param forced If true, no warning message is sent.
	 */
	public void teleportToSpawn(@NotNull Player player, boolean forced) {
		player.performCommand(instance.getConfigManager().spawnCommand());
		if (!forced) {
			instance.getSenderFactory().wrap(player).sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_UNSAFE_CONDITIONS.build()));
		}
	}
}