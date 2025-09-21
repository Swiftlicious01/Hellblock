package com.swiftlicious.hellblock.generation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.io.Files;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

import dev.dejvokep.boostedyaml.YamlDocument;

public class HellblockHandler {

	protected final HellblockPlugin instance;
	private File lastHellblockFile;
	private YamlDocument lastHellblock;

	public HellblockHandler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public CompletableFuture<Void> createHellblock(@NotNull Player player, @NotNull IslandOptions islandChoice,
			@Nullable String schematic, boolean isReset) {
		return CompletableFuture.runAsync(() -> {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			Sender audience = instance.getSenderFactory().wrap(player);
			if (onlineUser.isEmpty()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED.build()));
				return;
			}
			nextHellblockID(player.getUniqueId()).thenAccept((id) -> {
				if (id == 0) {
					instance.getPluginLogger()
							.severe(String.format(
									"Failed to retrieve the next hellblock ID for hellblock creation: %s.",
									onlineUser.get().getName()));
					return;
				}
				HellblockWorld<?> world = instance.getWorldManager().adapter()
						.createWorld(instance.getWorldManager().getHellblockWorldFormat(id));
				Location spawnLocation;
				if (instance.getConfigManager().perPlayerWorlds()) {
					spawnLocation = new Location(world.bukkitWorld(), 0, instance.getConfigManager().height(), 0);
				} else {
					spawnLocation = this.getLastHellblock();
				}
				Location next;
				if (instance.getConfigManager().perPlayerWorlds()) {
					next = spawnLocation;
				} else {
					next = this.nextHellblockLocation(spawnLocation, id);
					this.setLastHellblock(next, id);
				}
				HellblockData hellblockData = onlineUser.get().getHellblockData();
				hellblockData.setDefaultHellblockData(true, next, id);
				hellblockData.clearInvitations();
				hellblockData.setIslandChoice(islandChoice);
				hellblockData.setBiomeCooldown(0L);
				hellblockData.setLockedStatus(false);
				hellblockData.setOwnerUUID(player.getUniqueId());
				hellblockData.setProtectionFlags(new HashMap<>());
				hellblockData.setParty(new HashSet<>());
				hellblockData.setTrusted(new HashSet<>());
				hellblockData.setBanned(new HashSet<>());
				final Location nextHellblock = next;
				CompletableFuture<CompletableFuture<Void>> choice = new CompletableFuture<>();
				instance.getProtectionManager().getIslandProtection()
						.protectHellblock(world.bukkitWorld(), onlineUser.get()).thenRunAsync(() -> {
							if (islandChoice == IslandOptions.SCHEMATIC && schematic != null && !schematic.isEmpty()
									&& instance.getSchematicManager().schematicFiles.keySet().stream()
											.filter(sch -> Files.getNameWithoutExtension(sch)
													.equalsIgnoreCase(Files.getNameWithoutExtension(schematic)))
											.findAny().isPresent()) {
								hellblockData.setUsedSchematic(Files.getNameWithoutExtension(schematic));
								choice.complete(instance.getIslandChoiceConverter()
										.convertIslandChoice(world.bukkitWorld(), player, nextHellblock, schematic));
							} else {
								choice.complete(instance.getIslandChoiceConverter()
										.convertIslandChoice(world.bukkitWorld(), player, nextHellblock));
							}
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_CREATE_PROCESS.build()));
						}).thenRunAsync(() -> {
							choice.thenRunAsync(() -> {
								if (hellblockData.getHomeLocation() == null)
									throw new NullPointerException(
											"Hellblock home location returned null, please report this to the developer.");

								LocationUtils.isSafeLocationAsync(hellblockData.getHomeLocation())
										.thenAcceptAsync((result) -> {
											if (!result.booleanValue()) {
												locateBedrock(player.getUniqueId()).thenAcceptAsync((bedrock) -> {
													hellblockData.setHomeLocation(bedrock);
												});
											}
										}).thenRunAsync(() -> {
											ChunkUtils.teleportAsync(player, hellblockData.getHomeLocation(),
													TeleportCause.PLUGIN).thenRunAsync(() -> {
														if (instance.getConfigManager().creationTitleScreen() != null
																&& instance.getConfigManager().creationTitleScreen()
																		.enabled())
															VersionHelper.getNMSManager().sendTitle(player,
																	AdventureHelper.componentToJson(AdventureHelper
																			.miniMessage(instance.getConfigManager()
																					.creationTitleScreen().title()
																					.replace("{player}",
																							player.getName()))),
																	AdventureHelper.componentToJson(AdventureHelper
																			.miniMessage(instance.getConfigManager()
																					.creationTitleScreen().subtitle()
																					.replace("{player}",
																							player.getName()))),
																	instance.getConfigManager().creationTitleScreen()
																			.fadeIn(),
																	instance.getConfigManager().creationTitleScreen()
																			.stay(),
																	instance.getConfigManager().creationTitleScreen()
																			.fadeOut());
														if (instance.getConfigManager()
																.creatingHellblockSound() != null)
															AdventureHelper.playSound(
																	instance.getSenderFactory().getAudience(player),
																	instance.getConfigManager()
																			.creatingHellblockSound());
														hellblockData.setCreation(System.currentTimeMillis());
														instance.getBiomeHandler().setHellblockBiome(
																world.bukkitWorld(), hellblockData.getBoundingBox(),
																hellblockData.getBiome().getConvertedBiome());
														instance.debug(String.format("Creating new hellblock for %s",
																player.getName()));
													}).thenRunAsync(() -> {
														if (isReset) {
															hellblockData.setResetCooldown(86400L);
														}
													});
										});
							});
						});
			});
		});
	}

	public CompletableFuture<Void> createHellblock(@NotNull Player player, @NotNull IslandOptions islandChoice,
			boolean isReset) {
		return createHellblock(player, islandChoice, null, isReset);
	}

	public CompletableFuture<Void> resetHellblock(@NotNull UUID id, boolean forceReset) {
		return CompletableFuture.runAsync(() -> {
			instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty()) {
							return;
						}
						UserData offlineUser = result.get();
						Optional<HellblockWorld<?>> world = instance.getWorldManager().getWorld(instance
								.getWorldManager().getHellblockWorldFormat(offlineUser.getHellblockData().getID()));
						if (world.isEmpty() || world.get() == null)
							throw new NullPointerException(
									"World returned null, please try to regenerate the world before reporting this issue.");
						World bukkitWorld = world.get().bukkitWorld();
						Location home = offlineUser.getHellblockData().getHomeLocation();
						int hellblockID = offlineUser.getHellblockData().getID();

						Map<Block, Material> blockChanges = new LinkedHashMap<>();

						if (!forceReset && Bukkit.getPlayer(id) != null) {
							Sender audience = instance.getSenderFactory().wrap(Bukkit.getPlayer(id));
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_RESET_PROCESS.build()));
						}
						instance.getScheduler().executeSync(() -> {
							instance.getBiomeHandler().setHellblockBiome(bukkitWorld,
									offlineUser.getHellblockData().getBoundingBox(),
									HellBiome.NETHER_WASTES.getConvertedBiome());
							instance.getProtectionManager().getHellblockBlocks(bukkitWorld, id)
									.thenAcceptAsync((blocks) -> {
										for (Block block : blocks) {
											if (!block.isEmpty()) {
												blockChanges.put(block, Material.AIR);
											}
										}
									}).thenRunAsync(() -> {
										instance.getProtectionManager().getIslandProtection()
												.unprotectHellblock(bukkitWorld, id).thenRunAsync(() -> {
													Set<UUID> party = offlineUser.getHellblockData().getParty();
													if (party != null && !party.isEmpty()) {
														for (UUID uuid : party) {
															Player member = Bukkit.getPlayer(uuid);
															if (member != null && member.isOnline()) {
																Optional<UserData> onlineMember = instance
																		.getStorageManager().getOnlineUser(uuid);
																if (onlineMember.isEmpty())
																	continue;
																onlineMember.get().getHellblockData()
																		.resetHellblockData();
																if (instance.getConfigManager().resetInventory()) {
																	member.getInventory().clear();
																	member.getInventory().setArmorContents(null);
																}
																if (instance.getConfigManager().resetEnderchest()) {
																	member.getEnderChest().clear();
																}
																teleportToSpawn(member, true);
																Sender audience = instance.getSenderFactory()
																		.wrap(member);
																if (!forceReset) {
																	if (offlineUser.isOnline()) {
																		Player player = Bukkit.getPlayer(id);
																		audience.sendMessage(instance
																				.getTranslationManager()
																				.render(MessageConstants.MSG_HELLBLOCK_RESET_PARTY_NOTIFICATION
																						.arguments(AdventureHelper
																								.miniMessage(player
																										.getName()))
																						.build()));
																	}
																} else {
																	audience.sendMessage(
																			instance.getTranslationManager().render(
																					MessageConstants.MSG_HELLBLOCK_RESET_PARTY_FORCED_NOTIFICATION
																							.build()));
																}
															} else {
																instance.getStorageManager()
																		.getOfflineUserData(uuid,
																				instance.getConfigManager().lockData())
																		.thenAccept((memberResult) -> {
																			if (memberResult.isEmpty())
																				return;
																			memberResult.get().getHellblockData()
																					.resetHellblockData();
																			PlayerData data = memberResult.get()
																					.toPlayerData();
																			data.setToClearItems(true);
																			data.setInUnsafeLocation(true);
																		});
															}
														}
													}
													instance.getCoopManager().getVisitors(id).thenAccept(visitors -> {
														if (!visitors.isEmpty()) {
															for (UUID uuid : visitors) {
																Player visitor = Bukkit.getPlayer(uuid);
																if (visitor != null && visitor.isOnline()) {
																	Optional<UserData> onlineVisitor = instance
																			.getStorageManager().getOnlineUser(uuid);
																	if (onlineVisitor.isEmpty())
																		continue;
																	if (onlineVisitor.get().getHellblockData()
																			.hasHellblock()) {
																		if (onlineVisitor.get().getHellblockData()
																				.getOwnerUUID() == null) {
																			throw new NullPointerException(
																					"Owner reference returned null, please report this to the developer.");
																		}
																		instance.getStorageManager().getOfflineUserData(
																				onlineVisitor.get().getHellblockData()
																						.getOwnerUUID(),
																				instance.getConfigManager().lockData())
																				.thenAccept((owner) -> {
																					if (owner.isEmpty())
																						return;
																					UserData visitorOwner = owner.get();
																					instance.getCoopManager()
																							.makeHomeLocationSafe(
																									visitorOwner,
																									onlineVisitor
																											.get());
																				});
																	} else {
																		teleportToSpawn(visitor, true);
																	}
																} else {
																	instance.getStorageManager()
																			.getOfflineUserData(uuid,
																					instance.getConfigManager()
																							.lockData())
																			.thenAccept((visitorResult) -> {
																				if (visitorResult.isEmpty())
																					return;
																				PlayerData data = visitorResult.get()
																						.toPlayerData();
																				data.setInUnsafeLocation(true);
																			});
																}
															}
														}
													}).thenRunAsync(() -> {
														offlineUser.getHellblockData().resetHellblockData();
														if (offlineUser.isOnline()) {
															Player player = Bukkit.getPlayer(offlineUser.getUUID());
															if (!forceReset) {
																Sender audience = instance.getSenderFactory()
																		.wrap(player);
																audience.sendMessage(instance.getTranslationManager()
																		.render(MessageConstants.MSG_HELLBLOCK_RESET_NEW_OPTION
																				.build()));
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
															PlayerData data = offlineUser.toPlayerData();
															data.setToClearItems(true);
															data.setInUnsafeLocation(true);
														}
														instance.getScheduler().executeSync(() -> {
															blockChanges.forEach((change, type) -> {
																if (change
																		.getState() instanceof BlockInventoryHolder holder) {
																	holder.getInventory().clear();
																}
																change.setType(type);
															});
														}, home);
													});
												}).thenRunAsync(() -> {
													if (!forceReset) {
														if (offlineUser.isOnline()) {
															Player player = Bukkit.getPlayer(offlineUser.getUUID());
															instance.getScheduler().sync().runLater(() -> {
																instance.getIslandChoiceGUIManager()
																		.openIslandChoiceGUI(player, true);
															}, 1 * 20, home);
														}
													} else {
														instance.getWorldManager().adapter()
																.deleteWorld(instance.getWorldManager()
																		.getHellblockWorldFormat(hellblockID));
													}
												});
									});
						}, home);
					});
		});
	}

	public CompletableFuture<Integer> nextHellblockID(@NotNull UUID id) {
		CompletableFuture<Integer> idSupplier = new CompletableFuture<>();
		AtomicInteger nextID = new AtomicInteger(1);
		if (instance.getStorageManager().getDataSource().getUniqueUsers().isEmpty())
			idSupplier.complete(nextID.get());
		for (UUID playerData : instance.getStorageManager().getDataSource().getUniqueUsers()) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerData);
			if (onlineUser.isEmpty())
				continue;
			if (playerData.equals(id) && onlineUser.get().getHellblockData().getID() > 0) {
				idSupplier.complete(onlineUser.get().getHellblockData().getID());
				break;
			}
			instance.getStorageManager().getOfflineUserData(playerData, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						int hellblockID = offlineUser.getHellblockData().getID();
						if (hellblockID > 0) {
							do {
								idSupplier.complete(nextID.incrementAndGet());
							} while (hellblockID >= nextID.get());
						}
					});
		}
		return idSupplier;
	}

	public CompletableFuture<Location> locateBedrock(@NotNull UUID id) {
		CompletableFuture<Location> location = new CompletableFuture<>();
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
					Optional<HellblockWorld<?>> world = instance.getWorldManager().getWorld(
							instance.getWorldManager().getHellblockWorldFormat(offlineUser.getHellblockData().getID()));
					if (world.isEmpty() || world.get() == null)
						throw new NullPointerException(
								"World returned null, please try to regenerate the world before reporting this issue.");
					World bukkitWorld = world.get().bukkitWorld();
					instance.getProtectionManager().getHellblockBlocks(bukkitWorld, offlineUser.getUUID())
							.thenAccept((bedrockLocation) -> {
								Block bedrock = bedrockLocation.stream()
										.filter(block -> block.getType() == Material.BEDROCK && block.getLocation()
												.equals(offlineUser.getHellblockData().getHellblockLocation()))
										.findFirst().orElse(null);
								if (bedrock != null) {
									Block highestBlock = bukkitWorld.getHighestBlockAt(bedrock.getLocation());
									highestBlock.getLocation().setY(highestBlock.getY() + 1);
									LocationUtils.isSafeLocationAsync(highestBlock.getLocation()).thenAccept((safe) -> {
										if (safe.booleanValue()) {
											location.complete(
													highestBlock.getLocation().add(new Vector(0.5D, 0D, 0.5D)));
										} else {
											location.complete(offlineUser.getHellblockData().getHellblockLocation());
										}
									});
								} else {
									location.complete(offlineUser.getHellblockData().getHellblockLocation());
								}
							});
				});
		return location;
	}

	private @NotNull Location nextHellblockLocation(@NotNull Location last, int hellblockID) {
		if (hellblockID == getLastHellblockID())
			return last;
		int x = last.getBlockX();
		int z = last.getBlockZ();
		if (x < z) {
			if (-1 * x < z) {
				last.setX(last.getBlockX() + (double) instance.getConfigManager().distance());
				return last;
			} else {
				last.setZ(last.getBlockZ() + (double) instance.getConfigManager().distance());
				return last;
			}
		} else if (x > z) {
			if (-1 * x >= z) {
				last.setX(last.getBlockX() - (double) instance.getConfigManager().distance());
				return last;
			} else {
				last.setZ(last.getBlockZ() - (double) instance.getConfigManager().distance());
				return last;
			}
		} else if (x <= 0) {
			last.setZ(last.getBlockZ() + (double) instance.getConfigManager().distance());
			return last;
		} else {
			last.setZ(last.getBlockZ() - (double) instance.getConfigManager().distance());
			return last;
		}
	}

	private @NotNull YamlDocument getLastHellblockConfig() {
		if (this.lastHellblockFile == null) {
			this.lastHellblockFile = new File(instance.getDataFolder(), "lastHellblock.yml");
		}
		if (this.lastHellblock == null) {
			this.lastHellblock = instance.getConfigManager().loadData(this.lastHellblockFile);
		}

		return this.lastHellblock;
	}

	private int getLastHellblockID() {
		int id = this.getLastHellblockConfig().getInt("last.id");
		return id;
	}

	private @NotNull Location getLastHellblock() {
		Optional<HellblockWorld<?>> world = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(getLastHellblockID()));
		if (world.isEmpty() || world.get() == null)
			throw new NullPointerException(
					"World returned null, please try to regenerate the world before reporting this issue.");
		World bukkitWorld = world.get().bukkitWorld();
		int x = this.getLastHellblockConfig().getInt("last.x");
		int y = instance.getConfigManager().height();
		int z = this.getLastHellblockConfig().getInt("last.z");
		return new Location(bukkitWorld, (double) x, (double) y, (double) z);
	}

	private void setLastHellblock(@NotNull Location location, int hellblockID) {
		this.getLastHellblockConfig().set("last.id", hellblockID);
		this.getLastHellblockConfig().set("last.x", location.getBlockX());
		this.getLastHellblockConfig().set("last.z", location.getBlockZ());

		try {
			this.getLastHellblockConfig().save(this.lastHellblockFile);
		} catch (IOException ex) {
			instance.getPluginLogger().warn("Could not save the last known hellblock data to file.", ex);
		}
	}

	public boolean isInCorrectWorld(@NotNull Player player) {
		if (!instance.getConfigManager().perPlayerWorlds())
			return player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName());

		return player.getWorld().getName().startsWith("hellblock_world_");
	}

	public boolean isInCorrectWorld(@NotNull World world) {
		if (!instance.getConfigManager().perPlayerWorlds())
			return world.getName().equalsIgnoreCase(instance.getConfigManager().worldName());

		return world.getEnvironment() == Environment.NETHER
				&& world.getGenerator() == getDefaultWorldGenerator(world.getName(), world.getUID().toString());
	}

	public @NotNull ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @NotNull String id) {
		return new VoidGenerator();
	}

	public void teleportToSpawn(@NotNull Player player, boolean forced) {
		player.performCommand(instance.getConfigManager().spawnCommand());
		if (!forced)
			instance.getSenderFactory().wrap(player).sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_UNSAFE_CONDITIONS.build()));
	}
}