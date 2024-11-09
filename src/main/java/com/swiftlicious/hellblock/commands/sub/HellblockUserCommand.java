package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.gui.hellblock.IslandChoiceMenu;
import com.swiftlicious.hellblock.player.OfflineUser;
import com.swiftlicious.hellblock.player.OnlineUser;
import com.swiftlicious.hellblock.player.UUIDFetcher;

import com.swiftlicious.hellblock.scheduler.CancellableTask;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import lombok.NonNull;

public class HellblockUserCommand {

	public static HellblockUserCommand INSTANCE = new HellblockUserCommand();

	private final Map<UUID, Long> visitCache = new HashMap<>();
	private final Map<UUID, Long> confirmCache = new HashMap<>();

	public CommandAPICommand getResetCommand() {
		return new CommandAPICommand("reset").withAliases("restart", "replace")
				.withSubcommand(new CommandAPICommand("confirm").withPermission(CommandPermission.NONE)
						.withPermission("hellblock.user")
						.withRequirement(sender -> confirmCache.containsKey(((Player) sender).getUniqueId()))
						.executesPlayer((player, args) -> {
							OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUser(player.getUniqueId());
							if (onlineUser == null) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
										"<red>Still loading your player data... please try again in a few seconds.");
								return;
							}
							if (!onlineUser.getHellblockData().hasHellblock()) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										HBLocale.MSG_Hellblock_Not_Found);
								return;
							}
							if (onlineUser.getHellblockData().getOwnerUUID() == null) {
								throw new NullPointerException(
										"Owner reference returned null, please report this to the developer.");
							}
							if (onlineUser.getHellblockData().getOwnerUUID() != null
									&& !onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										HBLocale.MSG_Not_Owner_Of_Hellblock);
								return;
							}
							if (onlineUser.getHellblockData().isAbandoned()) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										HBLocale.MSG_Hellblock_Is_Abandoned);
								return;
							}
							if (onlineUser.getHellblockData().getResetCooldown() > 0) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										String.format(
												"<red>You've recently reset your hellblock already, you must wait for %s!",
												HellblockPlugin.getInstance().getFormattedCooldown(
														onlineUser.getHellblockData().getResetCooldown())));
								return;
							}
							if (!confirmCache.containsKey(player.getUniqueId())) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>You're not in the process of restarting your hellblock!");
								return;
							}

							HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(player.getUniqueId(),
									false);
							CommandAPI.updateRequirements(player);
						}))
				.withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.getHellblockData().hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					}
					if (onlineUser.getHellblockData().getOwnerUUID() == null) {
						throw new NullPointerException(
								"Owner reference returned null, please report this to the developer.");
					}
					if (onlineUser.getHellblockData().getOwnerUUID() != null
							&& !onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Not_Owner_Of_Hellblock);
						return;
					}
					if (onlineUser.getHellblockData().isAbandoned()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Is_Abandoned);
						return;
					}
					if (onlineUser.getHellblockData().getResetCooldown() > 0) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format(
										"<red>You've recently reset your hellblock already, you must wait for %s!",
										HellblockPlugin.getInstance().getFormattedCooldown(
												onlineUser.getHellblockData().getResetCooldown())));
						return;
					}
					if (confirmCache.containsKey(player.getUniqueId())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You're already in the process of restarting your hellblock, click confirm!");
						return;
					}

					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>Please click confirm to successfully restart your hellblock. <green><bold><click:run_command:/hellblock reset confirm><hover:show_text:'<yellow>Click here to confirm!'>[CONFIRM]</click>");
					new ConfirmCacher(player.getUniqueId());
					CommandAPI.updateRequirements(player);
				});
	}

	public CommandAPICommand getCreateCommand() {
		return new CommandAPICommand("create").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.getHellblockData().hasHellblock()) {
						if (onlineUser.getHellblockData().getResetCooldown() > 0) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									String.format(
											"<red>You've recently reset your hellblock already, you must wait for %s!",
											HellblockPlugin.getInstance().getFormattedCooldown(
													onlineUser.getHellblockData().getResetCooldown())));
							return;
						}
						new IslandChoiceMenu(player, false);
					} else {
						if (onlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						HellblockPlugin.getInstance().getStorageManager()
								.getOfflineUser(onlineUser.getHellblockData().getOwnerUUID(), HBConfig.lockData)
								.thenAccept((result) -> {
									OfflineUser offlineUser = result.orElseThrow();
									if (offlineUser.getHellblockData().isAbandoned()) {
										HellblockPlugin.getInstance().getAdventureManager()
												.sendMessageWithPrefix(player, HBLocale.MSG_Hellblock_Is_Abandoned);
										return;
									}
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>You already have a hellblock or are in a co-op! Use <dark_red>/hellblock home <red>to teleport to it.");
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>If you wish to leave use <dark_red>/hellcoop leave <red>to leave and start your own.");
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											String.format(
													"<red>Your hellblock is located at x: <dark_red>%s <red>z: <dark_red>%s<red>.",
													offlineUser.getHellblockData().getHellblockLocation().getBlockX(),
													offlineUser.getHellblockData().getHellblockLocation().getBlockZ()));
								});
					}
				});
	}

	public CommandAPICommand getLockCommand() {
		return new CommandAPICommand("lock").withAliases("unlock").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.user").executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (onlineUser.getHellblockData().hasHellblock()) {
						if (onlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.getHellblockData().getOwnerUUID() != null
								&& !onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Not_Owner_Of_Hellblock);
							return;
						}
						if (onlineUser.getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
							return;
						}
						onlineUser.getHellblockData().setLockedStatus(!onlineUser.getHellblockData().isLocked());
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You've just <dark_red>%s <red>your hellblock island!",
										(onlineUser.getHellblockData().isLocked() ? "locked" : "unlocked")));
						if (onlineUser.getHellblockData().isLocked()) {
							HellblockPlugin.getInstance().getCoopManager().kickVisitorsIfLocked(player.getUniqueId());
							HellblockPlugin.getInstance().getCoopManager().changeLockStatus(onlineUser);
						}
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
					}
				});
	}

	public CommandAPICommand getTopCommand() {
		return new CommandAPICommand("top").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!HellblockPlugin.getInstance().getIslandLevelManager().getTopTenHellblocks().entrySet()
							.isEmpty()) {
						int i = 0;
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Top Ten Level Hellblocks:");
						for (Entry<UUID, Float> ten : HellblockPlugin.getInstance().getIslandLevelManager()
								.getTopTenHellblocks().reversed().entrySet()) {

							UUID id = ten.getKey();
							if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
								continue;
							if (Bukkit.getOfflinePlayer(id).getName() == null)
								continue;
							float level = ten.getValue().floatValue();
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									String.format("<dark_red>%s. <red>%s <gray>(Lvl %s)", ++i,
											Bukkit.getOfflinePlayer(id).getName(), level));
							if (i >= 10) {
								break;
							}
						}
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>No hellblocks to list for the top ten!");
						return;
					}
				});
	}

	public CommandAPICommand getBiomeCommand() {
		return new CommandAPICommand("changebiome").withAliases("setbiome", "biome")
				.withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withArguments(new StringArgument("biome").replaceSuggestions(
						ArgumentSuggestions.stringCollection(info -> Arrays.stream(HellBiome.values())
								.map(biome -> biome.toString()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (onlineUser.getHellblockData().hasHellblock()) {
						if (onlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.getHellblockData().getOwnerUUID() != null
								&& !onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Not_Owner_Of_Hellblock);
							return;
						}
						if (onlineUser.getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
							return;
						}
						String newBiome = (String) args.getOrDefault("biome", "NETHER_WASTES");
						if (!(Arrays.asList(HellBiome.values()).stream()
								.filter(biome -> biome.toString().equalsIgnoreCase(newBiome)).findAny().isPresent())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The provided input isn't a valid biome!");
							return;
						}
						HellBiome biome = HellBiome.valueOf(newBiome);
						if (onlineUser.getHellblockData().getBiome() == biome) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									String.format("<red>Your hellblock biome is already set to <dark_red>%s<red>!",
											biome.getName()));
							return;
						}
						HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(onlineUser, biome);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					}
				});
	}

	public CommandAPICommand getVisitCommand() {
		return new CommandAPICommand("visit").withAliases("warp", "goto").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.user").withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
										.getOnlineUser(player.getUniqueId());
								if (onlineUser == null)
									return Collections.emptyList();
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(user -> user != null && user.isOnline()
												&& user.getHellblockData().hasHellblock()
												&& user.getHellblockData().getHomeLocation() != null
												&& !onlineUser.getHellblockData().getParty().contains(user.getUUID())
												&& !user.getName().equalsIgnoreCase(onlineUser.getName()))
										.map(user -> user.getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					String user = (String) args.getOrDefault("player", player);
					UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
							: UUIDFetcher.getUUID(user);
					if (id == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to visit doesn't exist!");
						return;
					}
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to visit doesn't exist!");
						return;
					}
					if (HellblockPlugin.getInstance().getCoopManager().trackBannedPlayer(id, player.getUniqueId())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You're banned from this hellblock island and can't visit it!");
						return;
					}
					HellblockPlugin.getInstance().getStorageManager().getOfflineUser(id, HBConfig.lockData)
							.thenAccept((result) -> {
								OfflineUser offlineUser = result.orElseThrow();
								if (id.equals(player.getUniqueId())
										|| offlineUser.getHellblockData().getParty().contains(player.getUniqueId())) {
									if (!onlineUser.getHellblockData().hasHellblock()) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
												player, "<red>That player doesn't have a hellblock!");
										return;
									}
									if (onlineUser.getHellblockData().getOwnerUUID() == null) {
										throw new NullPointerException(
												"Owner reference returned null, please report this to the developer.");
									}
									HellblockPlugin.getInstance().getStorageManager()
											.getOfflineUser(onlineUser.getHellblockData().getOwnerUUID(),
													HBConfig.lockData)
											.thenAccept((owner) -> {
												OfflineUser ownerUser = owner.orElseThrow();
												if (ownerUser.getHellblockData().isAbandoned()) {
													HellblockPlugin.getInstance().getAdventureManager()
															.sendMessageWithPrefix(player,
																	HBLocale.MSG_Hellblock_Is_Abandoned);
													return;
												}
												if (ownerUser.getHellblockData().getHomeLocation() != null) {
													HellblockPlugin.getInstance().getCoopManager()
															.makeHomeLocationSafe(ownerUser, onlineUser)
															.thenRun(() -> HellblockPlugin.getInstance()
																	.getAdventureManager().sendMessageWithPrefix(player,
																			"<red>Teleporting you to your hellblock!"));
												} else {
													HellblockPlugin.getInstance().getAdventureManager()
															.sendMessageWithPrefix(player,
																	"<red>Error teleporting you to your hellblock!");
													throw new NullPointerException(
															"Hellblock home location returned null, please report this to the developer.");
												}
											});
									return;
								}

								if (offlineUser.getHellblockData().hasHellblock()) {
									if (offlineUser.getHellblockData().getOwnerUUID() == null) {
										throw new NullPointerException(
												"Owner reference returned null, please report this to the developer.");
									}
									HellblockPlugin.getInstance().getStorageManager()
											.getOfflineUser(offlineUser.getHellblockData().getOwnerUUID(),
													HBConfig.lockData)
											.thenAccept((owner) -> {
												OfflineUser ownerUser = owner.orElseThrow();
												if (ownerUser.getHellblockData().isAbandoned()) {
													HellblockPlugin.getInstance().getAdventureManager()
															.sendMessageWithPrefix(player,
																	"<red>This hellblock is abandoned, you can't visit it at this time!");
													return;
												}
												if (!ownerUser.getHellblockData().isLocked()
														&& HellblockPlugin.getInstance().getCoopManager()
																.checkIfVisitorIsWelcome(player, ownerUser.getUUID())) {

													if (ownerUser.getHellblockData().getHomeLocation() != null) {
														LocationUtils
																.isSafeLocationAsync(
																		ownerUser.getHellblockData().getHomeLocation())
																.thenAccept((safe) -> {
																	if (!safe.booleanValue()) {
																		HellblockPlugin.getInstance()
																				.getAdventureManager()
																				.sendMessageWithPrefix(player,
																						"<red>This hellblock is not safe to visit right now!");
																		return;
																	}
																}).thenRunAsync(() -> {
																	ChunkUtils
																			.teleportAsync(player,
																					ownerUser.getHellblockData()
																							.getHomeLocation(),
																					TeleportCause.PLUGIN)
																			.thenRun(() -> {
																				if (!visitCache.containsKey(
																						player.getUniqueId())) {
																					if (ownerUser.getHellblockData()
																							.getOwnerUUID() == null) {
																						throw new NullPointerException(
																								"Owner reference returned null, please report this to the developer.");
																					}
																					if (!(ownerUser.getHellblockData()
																							.getOwnerUUID()
																							.equals(player
																									.getUniqueId())
																							|| ownerUser
																									.getHellblockData()
																									.getParty()
																									.contains(player
																											.getUniqueId())
																							|| ownerUser
																									.getHellblockData()
																									.getTrusted()
																									.contains(player
																											.getUniqueId()))) {
																						ownerUser.getHellblockData()
																								.addTotalVisit();
																						new VisitCacher(
																								player.getUniqueId());
																					}
																				}
																				HellblockPlugin.getInstance()
																						.getAdventureManager()
																						.sendMessageWithPrefix(player,
																								String.format(
																										"<red>You are visiting <dark_red>%s<red>'s hellblock!",
																										ownerUser
																												.getName()));
																			});
																	return;
																});
													} else {
														HellblockPlugin.getInstance().getAdventureManager()
																.sendMessageWithPrefix(player,
																		"<red>Error teleporting you to this hellblock!");
														throw new NullPointerException(
																"Hellblock home location returned null, please report this to the developer.");
													}
												} else {
													HellblockPlugin.getInstance().getAdventureManager()
															.sendMessageWithPrefix(player, String.format(
																	"<red>The player <dark_red>%s<red>'s hellblock is currently locked from having visitors!",
																	user));
													return;
												}
											});
								}

								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>That player doesn't have a hellblock!");
								return;
							});
				});
	}

	public CommandAPICommand getHomeCommand() {
		return new CommandAPICommand("home").withAliases("teleport", "tp").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.user").executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.getHellblockData().hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					} else {
						if (onlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						HellblockPlugin.getInstance().getStorageManager()
								.getOfflineUser(onlineUser.getHellblockData().getOwnerUUID(), HBConfig.lockData)
								.thenAccept((owner) -> {
									OfflineUser ownerUser = owner.orElseThrow();
									if (ownerUser.getHellblockData().isAbandoned()) {
										HellblockPlugin.getInstance().getAdventureManager()
												.sendMessageWithPrefix(player, HBLocale.MSG_Hellblock_Is_Abandoned);
										return;
									}
									if (ownerUser.getHellblockData().getHomeLocation() != null) {
										HellblockPlugin.getInstance().getCoopManager()
												.makeHomeLocationSafe(ownerUser, onlineUser)
												.thenRun(() -> HellblockPlugin.getInstance().getAdventureManager()
														.sendMessageWithPrefix(player,
																"<red>Teleporting you to your hellblock!"));
									} else {
										HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
												player, "<red>Error teleporting you to your hellblock!");
										throw new NullPointerException(
												"Hellblock home location returned null, please report this to the developer.");
									}
								});
					}
				});
	}

	public CommandAPICommand getFixHomeCommand() {
		return new CommandAPICommand("fixhome").withAliases("restorehome", "readjusthome")
				.withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.getHellblockData().hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					} else {
						if (onlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.getHellblockData().getOwnerUUID() != null
								&& !onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Not_Owner_Of_Hellblock);
							return;
						}
						if (onlineUser.getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
							return;
						}
						if (onlineUser.getHellblockData().getHomeLocation() != null
								&& !HellblockPlugin.getInstance().getHellblockHandler()
										.checkIfInSpawn(onlineUser.getHellblockData().getHomeLocation())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Your home location isn't in need of fixing!");
							return;
						}

						HellblockPlugin.getInstance().getHellblockHandler().locateBedrock(player.getUniqueId())
								.thenAccept((result) -> {
									Location bedrock = result.getBedrockLocation();
									bedrock.setY(HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld()
											.getHighestBlockYAt(bedrock));
									onlineUser.getHellblockData().setHomeLocation(bedrock);
									HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
											"<red>Your home location has been readjusted to your bedrock location!");
								});
					}
				});
	}

	public CommandAPICommand getBanCommand() {
		return new CommandAPICommand("ban").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
										.getOnlineUser(player.getUniqueId());
								if (onlineUser == null)
									return Collections.emptyList();
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(user -> user != null && user.isOnline()
												&& !onlineUser.getHellblockData().getTrusted().contains(user.getUUID())
												&& !onlineUser.getHellblockData().getParty().contains(user.getUUID())
												&& !user.getName().equalsIgnoreCase(onlineUser.getName()))
										.map(user -> user.getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.getHellblockData().hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					} else {
						if (onlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.getHellblockData().getOwnerUUID() != null
								&& !onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Not_Owner_Of_Hellblock);
							return;
						}
						if (onlineUser.getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
							return;
						}
						String user = (String) args.getOrDefault("player", player);
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you are trying to ban doesn't exist!");
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you are trying to ban doesn't exist!");
							return;
						}
						if (id.equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						if (onlineUser.getHellblockData().getParty().contains(id)
								|| onlineUser.getHellblockData().getTrusted().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't use this command on a party member or trusted player!");
							return;
						}
						if (onlineUser.getHellblockData().getBanned().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This player is already banned from your island!");
							return;
						}

						onlineUser.getHellblockData().banPlayer(id);
						if (Bukkit.getPlayer(user) != null) {
							if (HellblockPlugin.getInstance().getCoopManager().trackBannedPlayer(player.getUniqueId(),
									id)) {
								OnlineUser bannedPlayer = HellblockPlugin.getInstance().getStorageManager()
										.getOnlineUser(id);
								if (bannedPlayer == null) {
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											String.format(
													"<red>Still loading %s's data... please try again in a few seconds.",
													user));
									return;
								}
								if (bannedPlayer.getHellblockData().hasHellblock()) {
									if (bannedPlayer.getHellblockData().getOwnerUUID() == null) {
										throw new NullPointerException(
												"Owner reference returned null, please report this to the developer.");
									}
									HellblockPlugin.getInstance().getStorageManager()
											.getOfflineUser(bannedPlayer.getHellblockData().getOwnerUUID(),
													HBConfig.lockData)
											.thenAccept((owner) -> {
												OfflineUser bannedOwner = owner.orElseThrow();
												HellblockPlugin.getInstance().getCoopManager()
														.makeHomeLocationSafe(bannedOwner, bannedPlayer);
											});
								} else {
									HellblockPlugin.getInstance().getHellblockHandler()
											.teleportToSpawn(Bukkit.getPlayer(user), true);

								}
							}
						}
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You've banned <dark_red>%s <red>from your hellblock!", user));
					}
				});
	}

	public CommandAPICommand getUnbanCommand() {
		return new CommandAPICommand("unban").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
										.getOnlineUser(player.getUniqueId());
								if (onlineUser == null)
									return Collections.emptyList();
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(user -> user != null && user.isOnline()
												&& !onlineUser.getHellblockData().getTrusted().contains(user.getUUID())
												&& !onlineUser.getHellblockData().getParty().contains(user.getUUID())
												&& !user.getName().equalsIgnoreCase(onlineUser.getName()))
										.map(user -> user.getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.getHellblockData().hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					} else {
						if (onlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.getHellblockData().getOwnerUUID() != null
								&& !onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Not_Owner_Of_Hellblock);
							return;
						}
						if (onlineUser.getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
							return;
						}
						String user = (String) args.getOrDefault("player", player);
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you are trying to unban doesn't exist!");
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you are trying to unban doesn't exist!");
							return;
						}
						if (id.equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						if (onlineUser.getHellblockData().getParty().contains(id)
								|| onlineUser.getHellblockData().getTrusted().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't use this command on a party member or trusted player!");
							return;
						}
						if (!onlineUser.getHellblockData().getBanned().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This player is not banned from your island!");
							return;
						}

						onlineUser.getHellblockData().unbanPlayer(id);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You've unbanned <dark_red>%s <red>from your hellblock!", user));
					}
				});
	}

	public CommandAPICommand getInfoCommand() {
		return new CommandAPICommand("info").withAliases("information", "data").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.user").executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.getHellblockData().hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					} else {
						if (onlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						HellblockPlugin.getInstance().getStorageManager()
								.getOfflineUser(onlineUser.getHellblockData().getOwnerUUID(), HBConfig.lockData)
								.thenAccept((result) -> {
									OfflineUser offlineUser = result.orElseThrow();
									if (offlineUser.getHellblockData().isAbandoned()) {
										HellblockPlugin.getInstance().getAdventureManager()
												.sendMessageWithPrefix(player, HBLocale.MSG_Hellblock_Is_Abandoned);
										return;
									}
									String partyString = "", trustedString = "", bannedString = "";
									for (UUID id : offlineUser.getHellblockData().getParty()) {
										if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
												&& Bukkit.getOfflinePlayer(id).getName() != null) {
											partyString = "<dark_red>" + Bukkit.getOfflinePlayer(id).getName()
													+ "<red>, ";
										}
									}
									for (UUID id : offlineUser.getHellblockData().getTrusted()) {
										if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
												&& Bukkit.getOfflinePlayer(id).getName() != null) {
											trustedString = "<dark_red>" + Bukkit.getOfflinePlayer(id).getName()
													+ "<red>, ";
										}
									}
									for (UUID id : offlineUser.getHellblockData().getBanned()) {
										if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
												&& Bukkit.getOfflinePlayer(id).getName() != null) {
											bannedString = "<dark_red>" + Bukkit.getOfflinePlayer(id).getName()
													+ "<red>, ";
										}
									}
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											String.format("<dark_red>Hellblock Information (ID: <red>%s<dark_red>):",
													offlineUser.getHellblockData().getID()));
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>Owner: <dark_red>" + (offlineUser.getName() != null
													&& Bukkit.getOfflinePlayer(offlineUser.getUUID()).hasPlayedBefore()
															? offlineUser.getName()
															: "Unknown"));
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>Level: <dark_red>" + offlineUser.getHellblockData().getLevel());
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>Creation Date: <dark_red>"
													+ offlineUser.getHellblockData().getCreationTime());
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>Visitor Status: <dark_red>"
													+ (offlineUser.getHellblockData().isLocked() ? "Closed" : "Open"));
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>Total Visits: <dark_red>"
													+ offlineUser.getHellblockData().getTotalVisits());
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>Island Type: <dark_red>" + StringUtils.capitalize(
													offlineUser.getHellblockData().getIslandChoice().getName()));
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>Biome: <dark_red>"
													+ offlineUser.getHellblockData().getBiome().getName());
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>Party Size: <dark_red>"
													+ offlineUser.getHellblockData().getParty().size()
													+ " <red>/<dark_red> " + HellblockPlugin.getInstance()
															.getCoopManager().getPartySizeLimit());
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>Party Members: <dark_red>" + (!partyString.isEmpty()
													? partyString.substring(0, partyString.length() - 2)
													: "None"));
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>Trusted Members: <dark_red>" + (!trustedString.isEmpty()
													? trustedString.substring(0, trustedString.length() - 2)
													: "None"));
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>Banned Players: <dark_red>" + (!bannedString.isEmpty()
													? bannedString.substring(0, bannedString.length() - 2)
													: "None"));
								});

					}
				});
	}

	public CommandAPICommand getSetHomeCommand() {
		return new CommandAPICommand("sethome").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.getHellblockData().hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					} else {
						if (onlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.getHellblockData().getOwnerUUID() != null
								&& !onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Not_Owner_Of_Hellblock);
							return;
						}
						if (onlineUser.getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
							return;
						}
						if (onlineUser.getHellblockData().getHomeLocation() != null) {
							LocationUtils.isSafeLocationAsync(player.getLocation()).thenAccept((result) -> {
								if (!result.booleanValue() || player.isInLava() || player.isInPowderedSnow()) {
									HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
											"<red>The location you're standing at is not safe for a new home!");
									return;
								}
							}).thenRunAsync(() -> {
								if (onlineUser.getHellblockData().getHomeLocation().getWorld() != null
										&& onlineUser.getHellblockData().getHomeLocation().getWorld().getName()
												.equals(player.getWorld().getName())
										&& onlineUser.getHellblockData().getHomeLocation().getX() == player
												.getLocation().getX()
										&& onlineUser.getHellblockData().getHomeLocation().getY() == player
												.getLocation().getY()
										&& onlineUser.getHellblockData().getHomeLocation().getZ() == player
												.getLocation().getZ()
										&& onlineUser.getHellblockData().getHomeLocation().getYaw() == player
												.getLocation().getYaw()) {
									HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
											"<red>The location you're standing at is already set as your home!");
									return;
								}
								onlineUser.getHellblockData().setHomeLocation(player.getLocation());
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										String.format(
												"<red>You've set your new hellblock home location to x:%s, y:%s, z:%s facing %s!",
												player.getLocation().getBlockX(), player.getLocation().getBlockY(),
												player.getLocation().getBlockZ(), LocationUtils.getFacing(player)));
							});
						} else {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Error setting your home location!");
							throw new NullPointerException(String.format(
									"Home location for %s returned null, please report this to the developer.",
									onlineUser.getName()));
						}
					}
				});
	}

	public CommandAPICommand getHelpCommand() {
		return new CommandAPICommand("help").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<dark_red>Hellblock Commands:");
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>/hellblock create: Create your island");
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>/hellblock reset: Reset your island");
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>/hellblock info: See information about your island");
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>/hellblock home: Teleport to your island home");
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>/hellblock fixhome: Fix your home location if it has been set to spawn");
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>/hellblock sethome: Set the new home location of your island");
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>/hellblock lock/unlock: Change whether or not visitors can access your island");
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>/hellblock setbiome <biome>: Change the biome of your island");
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>/hellblock ban/unban <player>: Deny access to this player to your island");
					;
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>/hellblock visit <player>: Visit another player's island");
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>/hellblock top: View the hellblocks with the top levels");
				});
	}

	private class ConfirmCacher implements Runnable {

		private UUID playerUUID;
		private final CancellableTask cancellableTask;

		public ConfirmCacher(@NonNull UUID playerUUID) {
			this.playerUUID = playerUUID;
			confirmCache.putIfAbsent(playerUUID, 15L);
			this.cancellableTask = HellblockPlugin.getInstance().getScheduler().runTaskSyncTimer(this, null, 0, 1 * 20);
		}

		@Override
		public void run() {
			Player player = Bukkit.getPlayer(playerUUID);
			if (player == null || !player.isOnline()) {
				cancelTask();
				return;
			}

			if (!confirmCache.containsKey(playerUUID)) {
				return;
			}

			confirmCache.replace(playerUUID, confirmCache.get(playerUUID).longValue() - 1);
			if (confirmCache.get(playerUUID).longValue() == 0) {
				confirmCache.remove(playerUUID);
				CommandAPI.updateRequirements(player);
				cancelTask();
			}
		}

		public void cancelTask() {
			if (!this.cancellableTask.isCancelled())
				this.cancellableTask.cancel();
		}
	}

	private class VisitCacher implements Runnable {

		private UUID playerUUID;
		private final CancellableTask cancellableTask;

		public VisitCacher(@NonNull UUID playerUUID) {
			this.playerUUID = playerUUID;
			visitCache.putIfAbsent(playerUUID, 1L);
			this.cancellableTask = HellblockPlugin.getInstance().getScheduler().runTaskSyncTimer(this, null, 0,
					60 * 60 * 20L);
		}

		@Override
		public void run() {
			Player player = Bukkit.getPlayer(playerUUID);
			if (player == null || !player.isOnline()) {
				cancelTask();
				return;
			}

			if (!visitCache.containsKey(playerUUID)) {
				return;
			}

			visitCache.replace(playerUUID, visitCache.get(playerUUID).longValue() - 1);
			if (visitCache.get(playerUUID).longValue() == 0) {
				visitCache.remove(playerUUID);
				cancelTask();
			}
		}

		public void cancelTask() {
			if (!this.cancellableTask.isCancelled())
				this.cancellableTask.cancel();
		}
	}
}
