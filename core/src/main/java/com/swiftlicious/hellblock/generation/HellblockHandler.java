package com.swiftlicious.hellblock.generation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.io.Files;
import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.WorldGuardHook;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.audience.Audience;

public class HellblockHandler {

	protected final HellblockPlugin instance;
	public File schematicsDirectory;
	private File lastHellblockFile;
	private YamlDocument lastHellblock;

	private MVWorldManager mvWorldManager;
	private SlimePlugin slimeWorldManager;

	public HellblockHandler(HellblockPlugin plugin) {
		this.instance = plugin;
		this.schematicsDirectory = new File(instance.getDataFolder() + File.separator + "schematics");
		if (!this.schematicsDirectory.exists())
			this.schematicsDirectory.mkdirs();
		instance.getScheduler().asyncLater(() -> startCountdowns(), 5, TimeUnit.SECONDS);
	}

	public File getSchematicsDirectory() {
		return this.schematicsDirectory;
	}

	public @Nullable MVWorldManager getMVWorldManager() {
		return this.mvWorldManager;
	}

	public @Nullable SlimePlugin getSlimeWorldManager() {
		return this.slimeWorldManager;
	}

	public void setMVWorldManager(@NotNull MVWorldManager mvWorldManager) {
		this.mvWorldManager = mvWorldManager;
	}

	public void setSlimeWorldManager(@NotNull SlimePlugin slimeWorldManager) {
		this.slimeWorldManager = slimeWorldManager;
	}

	public void startCountdowns() {
		instance.getScheduler().asyncRepeating(() -> {
			for (UUID playerData : instance.getStorageManager().getDataSource().getUniqueUsers()) {
				instance.getStorageManager().getOfflineUserData(playerData, instance.getConfigManager().lockData())
						.thenAccept((result) -> {
							if (result.isEmpty())
								return;
							UserData offlineUser = result.get();
							UUID ownerUUID = offlineUser.getHellblockData().getOwnerUUID();
							if (ownerUUID != null && playerData.equals(ownerUUID)) {
								if (offlineUser.getHellblockData().getResetCooldown() > 0) {
									offlineUser.getHellblockData()
											.setResetCooldown(offlineUser.getHellblockData().getResetCooldown() - 1);
								}
								if (offlineUser.getHellblockData().getBiomeCooldown() > 0) {
									offlineUser.getHellblockData()
											.setBiomeCooldown(offlineUser.getHellblockData().getBiomeCooldown() - 1);
								}
								if (offlineUser.getHellblockData().getTransferCooldown() > 0) {
									offlineUser.getHellblockData().setTransferCooldown(
											offlineUser.getHellblockData().getTransferCooldown() - 1);
								}
							}
							if (!offlineUser.getHellblockData().hasHellblock()
									&& offlineUser.getHellblockData().getInvitations() != null) {
								for (Map.Entry<UUID, Long> invites : offlineUser.getHellblockData().getInvitations()
										.entrySet()) {
									if (invites.getValue() > 0) {
										offlineUser.getHellblockData().getInvitations().replace(invites.getKey(),
												invites.getValue() - 1);
									} else {
										offlineUser.getHellblockData().getInvitations().remove(invites.getKey());
									}
								}
							}
						});
			}
		}, 0, 1, TimeUnit.SECONDS);
	}

	public CompletableFuture<Void> createHellblock(@NotNull Player player, @NotNull IslandOptions islandChoice,
			@Nullable String schematic, boolean isReset) {
		return CompletableFuture.runAsync(() -> {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (onlineUser.isEmpty()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED.build()));
				return;
			}
			HellblockWorld<?> worldData;
			if (instance.getConfigManager().perPlayerWorlds()) {
				if (getSlimeWorldManager() != null && instance.getStorageManager().getSlimeLoader() != null) {
					try {
						if (!instance.getStorageManager().getSlimeLoader().worldExists(player.getName())) {
							SlimePropertyMap properties = new SlimePropertyMap();
							properties.setInt(SlimeProperties.SPAWN_X, 0);
							properties.setInt(SlimeProperties.SPAWN_Y, instance.getConfigManager().height());
							properties.setInt(SlimeProperties.SPAWN_Z, 0);
							properties.setBoolean(SlimeProperties.ALLOW_ANIMALS,
									HellblockFlag.FlagType.MOB_SPAWNING.getDefaultValue());
							properties.setBoolean(SlimeProperties.ALLOW_MONSTERS,
									HellblockFlag.FlagType.MOB_SPAWNING.getDefaultValue());
							properties.setBoolean(SlimeProperties.PVP, HellblockFlag.FlagType.PVP.getDefaultValue());
							properties.setString(SlimeProperties.DIFFICULTY, "normal");
							properties.setString(SlimeProperties.ENVIRONMENT, "nether");
							SlimeWorld world = getSlimeWorldManager().createEmptyWorld(
									instance.getStorageManager().getSlimeLoader(), player.getUniqueId().toString(),
									false, properties);
							getSlimeWorldManager().generateWorld(world);
							Location spawn = new Location(Bukkit.getWorld(world.getName()),
									properties.getInt(SlimeProperties.SPAWN_X),
									properties.getInt(SlimeProperties.SPAWN_Y),
									properties.getInt(SlimeProperties.SPAWN_Z));
							worldData = new HellblockWorld<SlimeWorld>(world, spawn);
						} else {
							SlimePropertyMap properties = new SlimePropertyMap();
							properties.setInt(SlimeProperties.SPAWN_X, 0);
							properties.setInt(SlimeProperties.SPAWN_Y, instance.getConfigManager().height());
							properties.setInt(SlimeProperties.SPAWN_Z, 0);
							properties.setBoolean(SlimeProperties.ALLOW_ANIMALS,
									HellblockFlag.FlagType.MOB_SPAWNING.getDefaultValue());
							properties.setBoolean(SlimeProperties.ALLOW_MONSTERS,
									HellblockFlag.FlagType.MOB_SPAWNING.getDefaultValue());
							properties.setBoolean(SlimeProperties.PVP, HellblockFlag.FlagType.PVP.getDefaultValue());
							properties.setString(SlimeProperties.DIFFICULTY, "normal");
							properties.setString(SlimeProperties.ENVIRONMENT, "nether");
							SlimeWorld world = getSlimeWorldManager().loadWorld(
									instance.getStorageManager().getSlimeLoader(), player.getUniqueId().toString(),
									false, properties);
							Location spawn = new Location(Bukkit.getWorld(world.getName()),
									properties.getInt(SlimeProperties.SPAWN_X),
									properties.getInt(SlimeProperties.SPAWN_Y),
									properties.getInt(SlimeProperties.SPAWN_Z));
							worldData = new HellblockWorld<SlimeWorld>(world, spawn);
						}
					} catch (WorldAlreadyExistsException | IOException | UnknownWorldException | CorruptedWorldException
							| NewerFormatException | WorldInUseException ex) {
						instance.getPluginLogger()
								.severe(String.format(
										"Failed to create new player world for the player %s using SlimeWorldManager.",
										onlineUser.get().getName()));
						return;
					}
				} else {
					throw new NullPointerException(
							"Failed to retrieve SlimeWorldManager instance for creating new player world.");
				}
			} else {
				worldData = new HellblockWorld<World>(getHellblockWorld(), this.getLastHellblock());
			}
			nextHellblockID(player.getUniqueId()).thenAccept((nextID) -> {
				int id = nextID.getNextID();
				if (id == 0) {
					instance.getPluginLogger()
							.severe(String.format(
									"Failed to retrieve the next hellblock ID for hellblock creation: %s.",
									onlineUser.get().getName()));
					return;
				}
				Location next;
				if (worldData.getWorld() instanceof World) {
					next = this.nextHellblockLocation(worldData.getSpawnLocation(), id);
					do {
						next = this.nextHellblockLocation(next, id);
					} while (this.hellblockInSpawn(next));
					this.setLastHellblock(next, id);
				} else {
					next = worldData.getSpawnLocation();
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
				ProtectionTask protection = new ProtectionTask();
				if (instance.getConfigManager().worldguardProtect()) {
					protection.setProtectionTask(instance.getWorldGuardHandler().protectHellblock(onlineUser.get()));
				} else {
					// TODO: using plugin protection
				}

				final Location nextHellblock = next;
				IslandChoiceTask choice = new IslandChoiceTask();
				protection.getProtectionTask().thenRunAsync(() -> {
					if (islandChoice == IslandOptions.SCHEMATIC && schematic != null && !schematic.isEmpty()
							&& instance.getSchematicManager().schematicFiles.keySet().stream()
									.filter(sch -> Files.getNameWithoutExtension(sch)
											.equalsIgnoreCase(Files.getNameWithoutExtension(schematic)))
									.findAny().isPresent()) {
						hellblockData.setUsedSchematic(Files.getNameWithoutExtension(schematic));
						choice.setIslandChoiceTask(instance.getIslandChoiceConverter().convertIslandChoice(player,
								nextHellblock, schematic));
					} else {
						choice.setIslandChoiceTask(
								instance.getIslandChoiceConverter().convertIslandChoice(player, nextHellblock));
					}
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_CREATE_PROCESS.build()));
				}).thenRunAsync(() -> {
					choice.getIslandChoiceTask().thenRunAsync(() -> {
						if (hellblockData.getHomeLocation() == null)
							throw new NullPointerException(
									"Hellblock home location returned null, please report this to the developer.");

						LocationUtils.isSafeLocationAsync(hellblockData.getHomeLocation()).thenAcceptAsync((result) -> {
							if (!result.booleanValue()) {
								locateBedrock(player.getUniqueId()).thenAcceptAsync((bedrock) -> {
									hellblockData.setHomeLocation(bedrock.getBedrockLocation());
								});
							}
						}).thenRunAsync(() -> {
							ChunkUtils.teleportAsync(player, hellblockData.getHomeLocation(), TeleportCause.PLUGIN)
									.thenRunAsync(() -> {
										if (instance.getConfigManager().creationTitleScreen() != null
												&& instance.getConfigManager().creationTitleScreen().enabled())
											AdventureHelper.sendTitle(audience,
													AdventureHelper.miniMessage(
															instance.getConfigManager().creationTitleScreen().title()
																	.replace("{player}", player.getName())),
													AdventureHelper.miniMessage(
															instance.getConfigManager().creationTitleScreen().subtitle()
																	.replace("{player}", player.getName())),
													instance.getConfigManager().creationTitleScreen().fadeIn(),
													instance.getConfigManager().creationTitleScreen().stay(),
													instance.getConfigManager().creationTitleScreen().fadeOut());
										if (instance.getConfigManager().creatingHellblockSound() != null)
											audience.playSound(instance.getConfigManager().creatingHellblockSound());
										hellblockData.setCreation(System.currentTimeMillis());
										if (instance.getConfigManager().worldguardProtect()) {
											ProtectedRegion region = instance.getWorldGuardHandler()
													.getRegion(onlineUser.get().getUUID(), hellblockData.getID());
											if (region != null)
												instance.getBiomeHandler().setHellblockBiome(region,
														hellblockData.getBiome().getConvertedBiome());
										} else {
											// TODO: using plugin protection
										}
										instance.debug(
												String.format("Creating new hellblock for %s", player.getName()));
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
						Location home = offlineUser.getHellblockData().getHomeLocation();

						Map<Block, Material> blockChanges = new LinkedHashMap<>();

						if (!forceReset && Bukkit.getPlayer(id) != null) {
							Audience audience = instance.getSenderFactory().getAudience(Bukkit.getPlayer(id));
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_RESET_PROCESS.build()));
						}
						UnprotectionTask unprotection = new UnprotectionTask();
						ClearBlocksTask clearBlocks = new ClearBlocksTask();
						instance.getScheduler().executeSync(() -> {
							if (instance.getConfigManager().worldguardProtect()) {
								clearBlocks.setClearBlocksTask(instance.getWorldGuardHandler().getRegionBlocks(id));
								unprotection.setUnprotectionTask(
										instance.getWorldGuardHandler().unprotectHellblock(id, forceReset));
								ProtectedRegion region = instance.getWorldGuardHandler().getRegion(id,
										offlineUser.getHellblockData().getID());
								instance.getBiomeHandler().setHellblockBiome(region,
										HellBiome.NETHER_WASTES.getConvertedBiome());
							} else {
								// TODO: using plugin protection
							}
							clearBlocks.getClearBlocksTask().thenAcceptAsync((blocks) -> {
								for (Block block : blocks) {
									if (!block.isEmpty()) {
										blockChanges.put(block, Material.AIR);
									}
								}
							}).thenRunAsync(() -> {
								unprotection.getUnprotectionTask().thenRunAsync(() -> {
									Set<UUID> party = offlineUser.getHellblockData().getParty();
									if (party != null && !party.isEmpty()) {
										for (UUID uuid : party) {
											Player member = Bukkit.getPlayer(uuid);
											if (member != null && member.isOnline()) {
												Optional<UserData> onlineMember = instance.getStorageManager()
														.getOnlineUser(uuid);
												if (onlineMember.isEmpty())
													continue;
												onlineMember.get().getHellblockData().resetHellblockData();
												teleportToSpawn(member, true);
												Audience audience = instance.getSenderFactory().getAudience(member);
												if (!forceReset) {
													if (offlineUser.isOnline()) {
														Player player = Bukkit.getPlayer(id);
														audience.sendMessage(instance.getTranslationManager().render(
																MessageConstants.MSG_HELLBLOCK_RESET_PARTY_NOTIFICATION
																		.arguments(AdventureHelper
																				.miniMessage(player.getName()))
																		.build()));
													}
												} else {
													audience.sendMessage(instance.getTranslationManager().render(
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
															UserData offlineMember = memberResult.get();
															offlineMember.getHellblockData().resetHellblockData();
															offlineMember.setInUnsafeLocation(true);
														});
											}
										}
									}
									Set<UUID> visitors = instance.getCoopManager().getVisitors(id);
									if (!visitors.isEmpty()) {
										for (UUID uuid : visitors) {
											Player visitor = Bukkit.getPlayer(uuid);
											if (visitor != null && visitor.isOnline()) {
												Optional<UserData> onlineVisitor = instance.getStorageManager()
														.getOnlineUser(uuid);
												if (onlineVisitor.isEmpty())
													continue;
												if (onlineVisitor.get().getHellblockData().hasHellblock()) {
													if (onlineVisitor.get().getHellblockData().getOwnerUUID() == null) {
														throw new NullPointerException(
																"Owner reference returned null, please report this to the developer.");
													}
													instance.getStorageManager()
															.getOfflineUserData(
																	onlineVisitor.get().getHellblockData()
																			.getOwnerUUID(),
																	instance.getConfigManager().lockData())
															.thenAccept((owner) -> {
																if (owner.isEmpty())
																	return;
																UserData visitorOwner = owner.get();
																instance.getCoopManager().makeHomeLocationSafe(
																		visitorOwner, onlineVisitor.get());
															});
												} else {
													instance.getHellblockHandler().teleportToSpawn(visitor, true);
												}
											} else {
												instance.getStorageManager()
														.getOfflineUserData(uuid,
																instance.getConfigManager().lockData())
														.thenAccept((visitorResult) -> {
															if (visitorResult.isEmpty())
																return;
															UserData offlineVisitor = visitorResult.get();
															offlineVisitor.setInUnsafeLocation(true);
														});
											}
										}
									}
									offlineUser.getHellblockData().resetHellblockData();
									if (offlineUser.isOnline()) {
										Player player = Bukkit.getPlayer(offlineUser.getUUID());
										if (!forceReset) {
											Audience audience = instance.getSenderFactory().getAudience(player);
											audience.sendMessage(instance.getTranslationManager()
													.render(MessageConstants.MSG_HELLBLOCK_RESET_NEW_OPTION.build()));
										}
										if (instance.getConfigManager().resetInventory()) {
											player.getInventory().clear();
											player.getInventory().setArmorContents(null);
										}
										if (instance.getConfigManager().resetEnderchest()) {
											player.getEnderChest().clear();
										}
										teleportToSpawn(player, true);
									}
									instance.getScheduler().executeSync(() -> {
										blockChanges.forEach((change, type) -> {
											if (change.getState() instanceof BlockInventoryHolder holder) {
												holder.getInventory().clear();
											}
											change.setType(type);
										});
									}, home);
								}).thenRunAsync(() -> {
									if (!forceReset) {
										if (offlineUser.isOnline()) {
											Player player = Bukkit.getPlayer(offlineUser.getUUID());
											instance.getScheduler().sync().runLater(() -> {
												instance.getIslandChoiceGUIManager().openIslandChoiceGUI(player, true);
											}, 1 * 20, home);
										}
									}
								});
							});
						}, home);
					});
		});
	}

	public boolean hellblockInSpawn(@Nullable Location location) {
		if (location == null) {
			return true;
		} else {
			return location.getX() > (double) (0 - instance.getConfigManager().spawnSize())
					&& location.getX() < (double) instance.getConfigManager().spawnSize()
					&& location.getZ() > (double) (0 - instance.getConfigManager().spawnSize())
					&& location.getZ() < (double) instance.getConfigManager().spawnSize();
		}
	}

	public CompletableFuture<IDSupplier> nextHellblockID(@NotNull UUID id) {
		CompletableFuture<IDSupplier> nextID = CompletableFuture.supplyAsync(() -> {
			IDSupplier idSupplier = new IDSupplier(0);
			if (instance.getStorageManager().getDataSource().getUniqueUsers().isEmpty())
				idSupplier.setNextID(1);
			for (UUID playerData : instance.getStorageManager().getDataSource().getUniqueUsers()) {
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerData);
				if (onlineUser.isEmpty())
					continue;
				if (playerData.equals(id) && onlineUser.get().getHellblockData().getID() > 0) {
					idSupplier.setNextID(onlineUser.get().getHellblockData().getID());
					break;
				}
				instance.getStorageManager().getOfflineUserData(playerData, instance.getConfigManager().lockData())
						.thenAccept((result) -> {
							if (result.isEmpty())
								return;
							UserData offlineUser = result.get();
							int hellblockID = offlineUser.getHellblockData().getID();
							do {
								idSupplier.setNextID(idSupplier.getNextID() + 1);
							} while (hellblockID >= idSupplier.getNextID());
						}).join();
			}
			return idSupplier;
		});
		return nextID;
	}

	public CompletableFuture<BedrockLocator> locateBedrock(@NotNull UUID id) {
		CompletableFuture<BedrockLocator> location = CompletableFuture.supplyAsync(() -> {
			if (instance.getConfigManager().worldguardProtect()) {
				Location spawn = instance.getWorldGuardHandler().getSpawnCenter().toLocation(getHellblockWorld());
				spawn.setY(getHellblockWorld().getHighestBlockYAt(spawn) + 1);
				BedrockLocator bedrockLocator = new BedrockLocator(spawn);
				CompletableFuture<Void> regionBlockIterator = instance.getWorldGuardHandler().getRegionBlocks(id)
						.thenAccept((bedrockLocation) -> {
							Block bedrock = bedrockLocation.stream()
									.filter(block -> block.getType() == Material.BEDROCK).findFirst().orElse(null);
							if (bedrock != null) {
								bedrockLocator.setBedrockLocation(bedrock.getLocation());
								Block highestBlock = getHellblockWorld().getHighestBlockAt(bedrock.getLocation());
								highestBlock.getLocation().setY(highestBlock.getY() + 1);
								CompletableFuture<Void> safe = LocationUtils
										.isSafeLocationAsync(highestBlock.getLocation()).thenAccept((result) -> {
											if (result.booleanValue()) {
												bedrockLocator.setBedrockLocation(
														highestBlock.getLocation().add(new Vector(0.5D, 0D, 0.5D)));
											} else {
												bedrockLocator.setBedrockLocation(spawn);
											}
										});
								safe.join(); // Wait for the asynchronous operation to complete
							}
						});
				regionBlockIterator.join(); // Wait for the asynchronous operation to complete
				return bedrockLocator;
			} else {
				return new BedrockLocator(
						new Location(getHellblockWorld(), 0.0D, (instance.getConfigManager().height() + 1), 0.0D));
				// TODO: using plugin protection
			}
		});
		return location;

	}

	public boolean isHellblockOwner(@NotNull UUID playerID, @NotNull UUID ownerID) {
		return playerID.equals(ownerID);
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
		World world = getHellblockWorld();
		int x = this.getLastHellblockConfig().getInt("last.x");
		int y = instance.getConfigManager().height();
		int z = this.getLastHellblockConfig().getInt("last.z");
		return new Location(world, (double) x, (double) y, (double) z);
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

	public @NotNull File[] getSchematics() {
		return this.schematicsDirectory.listFiles();
	}

	public @NotNull ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @NotNull String id) {
		return new VoidGenerator();
	}

	public boolean isInCorrectWorld(@NotNull Player player) {
		if (!instance.getConfigManager().perPlayerWorlds())
			return player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName());

		return player.getWorld().getName().equalsIgnoreCase(player.getUniqueId().toString());
	}

	public boolean isInCorrectWorld(@NotNull World world) {
		if (!instance.getConfigManager().perPlayerWorlds())
			return world.getName().equalsIgnoreCase(instance.getConfigManager().worldName());

		return world.getEnvironment() == Environment.NETHER
				&& world.getGenerator() == getDefaultWorldGenerator(world.getName(), world.getUID().toString());
	}

	@SuppressWarnings("deprecation")
	public @NotNull World getHellblockWorld() {
		World hellblockWorld = Bukkit.getWorld(instance.getConfigManager().worldName());
		if (hellblockWorld == null) {
			VoidGenerator voidGen = new VoidGenerator();
			hellblockWorld = WorldCreator.name(instance.getConfigManager().worldName()).type(WorldType.FLAT)
					.generateStructures(false).environment(Environment.NETHER).generator(voidGen)
					.biomeProvider(voidGen.new VoidBiomeProvider()).createWorld();
			instance.debug(String.format("Created the new world to be used for Hellblock islands: %s",
					instance.getConfigManager().worldName()));
			final World spawnWorld = hellblockWorld;
			final HellblockSpawn spawnLocation = new HellblockSpawn();
			generateSpawn().thenAccept(spawn -> {
				CompletableFuture<Void> spawnProtection = new CompletableFuture<Void>();
				spawnLocation.setSpawnLocation(spawn);
				if (instance.getConfigManager().worldguardProtect()) {
					spawnProtection = instance.getWorldGuardHandler().protectSpawn();
				} else {
					// TODO: using plugin protection
				}
				spawnProtection.thenRun(() -> {
					if (instance.getWorldGuardHandler().getSpawnRegion() != null)
						instance.getBiomeHandler().setHellblockBiome(instance.getWorldGuardHandler().getSpawnRegion(),
								HellBiome.SOUL_SAND_VALLEY.getConvertedBiome());
					instance.debug("Spawn area protected with WorldGuard!");
					spawnLocation
							.setSpawnLocation(instance.getWorldGuardHandler().getSpawnCenter().toLocation(spawnWorld));
					spawnLocation.getSpawnLocation().setY(instance.getConfigManager().height() + 1);
					spawnWorld.setSpawnLocation(spawnLocation.getSpawnLocation());
					instance.debug(
							String.format("Generated the spawn for the new Hellblock world %s at x:%s, y:%s, z:%s",
									instance.getConfigManager().worldName(), spawnLocation.getSpawnLocation().getX(),
									spawnLocation.getSpawnLocation().getY(), spawnLocation.getSpawnLocation().getZ()));
				}).thenRun(() -> {
					if (getMVWorldManager() != null) {
						instance.getScheduler().executeSync(() -> {
							getMVWorldManager().addWorld(spawnWorld.getName(), Environment.NETHER,
									String.valueOf(spawnWorld.getSeed()), WorldType.FLAT, false,
									instance.getDescription().getName(), true);
							getMVWorldManager().getMVWorld(spawnWorld).setSpawnLocation(spawnWorld.getSpawnLocation());
							instance.debug(String.format("Imported the world to Multiverse-Core: %s",
									instance.getConfigManager().worldName()));
						});
					}
				});
			});
		}
		return hellblockWorld;
	}

	public void teleportToSpawn(@NotNull Player player, boolean forced) {
		if (instance.getConfigManager().worldguardProtect()) {
			Location spawn = instance.getWorldGuardHandler().getSpawnCenter().toLocation(getHellblockWorld());
			spawn.setY(getHellblockWorld().getHighestBlockYAt(spawn) + 1);
			ChunkUtils.teleportAsync(player, spawn).thenRun(() -> {
				if (!forced)
					instance.getSenderFactory().getAudience(player).sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_UNSAFE_CONDITIONS.build()));
			});
		} else {
			// TODO: using plugin protection
		}
	}

	public boolean checkIfInSpawn(@NotNull Location location) {
		if (instance.getConfigManager().worldguardProtect()) {
			if (instance.getWorldGuardHandler().getWorldGuardPlatform() == null) {
				throw new NullPointerException("Could not retrieve WorldGuard platform.");
			}
			RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer();
			com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(getHellblockWorld());
			RegionManager regions = container.get(world);
			if (regions == null) {
				instance.getPluginLogger().severe(
						String.format("Could not load WorldGuard regions for hellblock world: %s", world.getName()));
				return false;
			}
			ProtectedRegion region = regions.getRegion(WorldGuardHook.SPAWN_REGION);
			if (region == null) {
				return false;
			}

			return region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public CompletableFuture<Location> generateSpawn() {
		CompletableFuture<Location> location = CompletableFuture.supplyAsync(() -> {
			Map<Block, Material> blockChanges = new LinkedHashMap<>();
			World world = getHellblockWorld();
			int y = instance.getConfigManager().height();
			int spawnSize = instance.getConfigManager().spawnSize();

			Location spawn = new Location(world, 0, y, 0);
			int i = 0;
			for (int operateX = 0 - spawnSize; operateX <= spawnSize; ++operateX) {
				for (int operateZ = 0 - spawnSize; operateZ <= spawnSize; ++operateZ) {
					Block block = world.getBlockAt(operateX, y, operateZ);
					blockChanges.put(block, Material.RED_NETHER_BRICKS);
					if (i == 0) {
						spawn = block.getLocation(
								new Location(block.getWorld(), block.getX(), block.getY() + 1, block.getZ()));
						i++;
					}
				}
			}

			instance.getScheduler().executeSync(() -> blockChanges.forEach((change, type) -> change.setType(type)));

			return spawn;
		});

		return location;
	}

	protected class ProtectionTask {
		private CompletableFuture<Void> protectionTask;

		public @NotNull CompletableFuture<Void> getProtectionTask() {
			return this.protectionTask;
		}

		public void setProtectionTask(@NotNull CompletableFuture<Void> protectionTask) {
			this.protectionTask = protectionTask;
		}
	}

	protected class UnprotectionTask {
		private CompletableFuture<Void> unprotectionTask;

		public @NotNull CompletableFuture<Void> getUnprotectionTask() {
			return this.unprotectionTask;
		}

		public void setUnprotectionTask(@NotNull CompletableFuture<Void> unprotectionTask) {
			this.unprotectionTask = unprotectionTask;
		}
	}

	protected class IslandChoiceTask {
		private CompletableFuture<Void> islandChoiceTask;

		public @NotNull CompletableFuture<Void> getIslandChoiceTask() {
			return this.islandChoiceTask;
		}

		public void setIslandChoiceTask(@NotNull CompletableFuture<Void> islandChoiceTask) {
			this.islandChoiceTask = islandChoiceTask;
		}
	}

	protected class ClearBlocksTask {
		private CompletableFuture<List<Block>> clearBlocksTask;

		public @NotNull CompletableFuture<List<Block>> getClearBlocksTask() {
			return this.clearBlocksTask;
		}

		public void setClearBlocksTask(@NotNull CompletableFuture<List<Block>> clearBlocksTask) {
			this.clearBlocksTask = clearBlocksTask;
		}
	}

	protected class IDSupplier {
		private int id;

		public IDSupplier(int id) {
			this.id = id;
		}

		public int getNextID() {
			return this.id;
		}

		public void setNextID(int id) {
			this.id = id;
		}
	}

	protected class HellblockSpawn {
		private Location spawnLocation;

		public @NotNull Location getSpawnLocation() {
			return this.spawnLocation;
		}

		public void setSpawnLocation(@NotNull Location spawnLocation) {
			this.spawnLocation = spawnLocation;
		}
	}

	protected class HellblockWorld<T> {
		private T world;
		private Location spawnLocation;

		public HellblockWorld(T world, Location spawnLocation) {
			this.world = world;
			this.spawnLocation = spawnLocation;
		}

		public T getWorld() {
			return this.world;
		}

		public Location getSpawnLocation() {
			return this.spawnLocation;
		}
	}

	public class BedrockLocator {
		private Location bedrockLocation;

		public BedrockLocator(Location bedrockLocation) {
			this.bedrockLocation = bedrockLocation;
		}

		public @NotNull Location getBedrockLocation() {
			return this.bedrockLocation;
		}

		public void setBedrockLocation(@NotNull Location bedrockLocation) {
			this.bedrockLocation = bedrockLocation;
		}
	}
}
