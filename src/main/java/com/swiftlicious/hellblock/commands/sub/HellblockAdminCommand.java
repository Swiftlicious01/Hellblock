package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.WorldGuardHook;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.OfflineUser;
import com.swiftlicious.hellblock.player.UUIDFetcher;

import com.swiftlicious.hellblock.utils.ChunkUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class HellblockAdminCommand {

	public static HellblockAdminCommand INSTANCE = new HellblockAdminCommand();

	public CommandAPICommand getAdminCommand() {
		return new CommandAPICommand("admin").withPermission(CommandPermission.OP).withPermission("hellblock.admin")
				.withSubcommands(getGenSpawnCommand("genspawn"), getPurgeCommand("purge"), getReloadCommand("reload"),
						getTeleportCommand("goto"), getDeleteCommand("delete"), getHelpCommand());
	}

	private CommandAPICommand getTeleportCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.OP).withPermission("hellblock.admin")
				.withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(onlineUser -> onlineUser != null && onlineUser.isOnline()
												&& onlineUser.getHellblockData().hasHellblock()
												&& !onlineUser.getName().equalsIgnoreCase(player.getName()))
										.map(onlineUser -> onlineUser.getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					String user = (String) args.getOrDefault("player", player);
					UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
							: UUIDFetcher.getUUID(user);
					if (id == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to teleport to doesn't exist!");
						return;
					}
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to teleport to doesn't exist!");
						return;
					}
					HellblockPlugin.getInstance().getStorageManager().getOfflineUser(id, HBConfig.lockData)
							.thenAccept((result) -> {
								OfflineUser offlineUser = result.orElseThrow();
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
												World world = HellblockPlugin.getInstance().getHellblockHandler()
														.getHellblockWorld();
												int x = ownerUser.getHellblockData().getHellblockLocation().getBlockX();
												int z = ownerUser.getHellblockData().getHellblockLocation().getBlockZ();
												Location highest = new Location(world, x,
														world.getHighestBlockYAt(x, z), z);
												ChunkUtils.teleportAsync(player, highest, TeleportCause.PLUGIN)
														.thenRun(() -> HellblockPlugin.getInstance()
																.getAdventureManager()
																.sendMessageWithPrefix(player, String.format(
																		"<red>You've been teleported to <dark_red>%s<red>'s hellblock!",
																		ownerUser.getUUID())));
											});
									return;
								}

								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>That player doesn't have a hellblock!");
								return;
							});
				});
	}

	private CommandAPICommand getPurgeCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.OP).withPermission("hellblock.admin")
				.withArguments(new IntegerArgument("days")).executes((sender, args) -> {
					int purgeDays = (int) args.get("days");
					if (HellblockPlugin.getInstance().getStorageManager().getDataSource().getUniqueUsers(false)
							.size() == 0) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
								"<red>No hellblock player data to purge available!");
						return;
					}
					if (purgeDays <= 0) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
								"<red>Please enter a positive number above 0!");
						return;
					}
					PurgeCounter purgeCount = new PurgeCounter();
					for (UUID id : HellblockPlugin.getInstance().getStorageManager().getDataSource()
							.getUniqueUsers(false)) {
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
							continue;

						OfflinePlayer player = Bukkit.getOfflinePlayer(id);
						if (player.getLastLogin() == 0)
							continue;
						long millisSinceLastLogin = (System.currentTimeMillis() - player.getLastLogin()) -
						// Account for a timezone difference
								TimeUnit.MILLISECONDS.toHours(19);
						if (millisSinceLastLogin > TimeUnit.DAYS.toMillis(purgeDays)) {
							HellblockPlugin.getInstance().getStorageManager().getOfflineUser(id, HBConfig.lockData)
									.thenAccept((result) -> {
										OfflineUser offlineUser = result.orElseThrow();
										if (offlineUser.getHellblockData().hasHellblock()
												&& offlineUser.getHellblockData().getOwnerUUID() != null) {
											if (HellblockPlugin.getInstance().getHellblockHandler().isHellblockOwner(id,
													offlineUser.getHellblockData().getOwnerUUID())) {
												float level = offlineUser.getHellblockData().getLevel();
												if (level == HellblockData.DEFAULT_LEVEL) {

													offlineUser.getHellblockData().setAsAbandoned(true);
													int hellblockID = offlineUser.getHellblockData().getID();
													if (HellblockPlugin.getInstance().getWorldGuardHandler().getRegion(
															offlineUser.getHellblockData().getOwnerUUID(),
															hellblockID) != null) {
														HellblockPlugin.getInstance().getWorldGuardHandler()
																.updateHellblockMessages(
																		offlineUser.getHellblockData().getOwnerUUID(),
																		HellblockPlugin.getInstance()
																				.getWorldGuardHandler().getRegion(
																						offlineUser.getHellblockData()
																								.getOwnerUUID(),
																						hellblockID));
														HellblockPlugin.getInstance().getWorldGuardHandler()
																.abandonIsland(
																		offlineUser.getHellblockData().getOwnerUUID(),
																		HellblockPlugin.getInstance()
																				.getWorldGuardHandler().getRegion(
																						offlineUser.getHellblockData()
																								.getOwnerUUID(),
																						hellblockID));
														purgeCount.setPurgeCount(purgeCount.getPurgeCount() + 1);
													}
												}
											}
										}
									}).join();
						}
					}

					if (purgeCount.getPurgeCount() > 0) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
								String.format("<red>You purged a total of %s hellblocks!", purgeCount));
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
								"<red>No hellblock data was purged with your inputted settings!");
						return;
					}
				});
	}

	private CommandAPICommand getDeleteCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.OP).withPermission("hellblock.admin")
				.withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(onlineUser -> onlineUser != null && onlineUser.isOnline()
												&& onlineUser.getHellblockData().hasHellblock()
												&& !onlineUser.getName().equalsIgnoreCase(player.getName()))
										.map(onlineUser -> onlineUser.getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					String user = (String) args.getOrDefault("player", player);
					UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
							: UUIDFetcher.getUUID(user);
					if (id == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to delete doesn't exist!");
						return;
					}
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to delete doesn't exist!");
						return;
					}
					HellblockPlugin.getInstance().getStorageManager().getOfflineUser(id, HBConfig.lockData)
							.thenAccept((result) -> {
								OfflineUser offlineUser = result.orElseThrow();
								if (offlineUser.getHellblockData().hasHellblock()) {
									HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(id, true)
											.thenRun(() -> {
												HellblockPlugin.getInstance().getAdventureManager()
														.sendMessageWithPrefix(player, String.format(
																"<red>You've forcefully deleted <dark_red>%s<red>'s hellblock!",
																user));
											});
									return;
								}

								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>That player doesn't have a hellblock!");
								return;
							});
				});
	}

	private CommandAPICommand getGenSpawnCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.OP).withPermission("hellblock.admin")
				.executesPlayer((player, args) -> {
					if (!player.getWorld().getName()
							.equalsIgnoreCase(HellblockPlugin.getInstance().getHellblockHandler().getWorldName())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You aren't in the correct world to generate the spawn!");
						return;
					}

					World world = HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld();
					if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
						com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
						if (HellblockPlugin.getInstance().getWorldGuardHandler().getWorldGuardPlatform() != null
								&& HellblockPlugin.getInstance().getWorldGuardHandler().getWorldGuardPlatform()
										.getRegionContainer().get(weWorld).hasRegion(WorldGuardHook.SPAWN_REGION)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The spawn area has already been generated!");
							return;
						}
					} else {
						// TODO: plugin protection
					}
					HellblockPlugin.getInstance().getHellblockHandler().generateSpawn().thenAccept(spawn -> {
						ChunkUtils.teleportAsync(player, spawn, TeleportCause.PLUGIN).thenRun(() -> {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Spawn area has been generated!");
						});
					});
				});
	}

	private CommandAPICommand getReloadCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.OP).withPermission("hellblock.admin")
				.executes((sender, args) -> {
					long time = System.currentTimeMillis();
					HellblockPlugin.getInstance().reload();
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
							HBLocale.MSG_Reload.replace("{time}", String.valueOf(System.currentTimeMillis() - time)));
				});
	}

	private CommandAPICommand getHelpCommand() {
		CommandAPICommand command = new CommandAPICommand("help");
		if (command.getArguments().isEmpty()) {
			command.withPermission(CommandPermission.OP).withPermission("hellblock.admin")
					.executesPlayer((player, args) -> {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<dark_red>Hellblock Admin Commands:");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock admin goto <player>: Forcefully teleports you to this player's island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock admin delete <player>: Forcefully deletes this player's island");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock admin purge <days>: Forcefully abandon the islands that have been inactive for this many days");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock admin genspawn: Generate the spawn area if it was deleted for any reason");
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>/hellblock admin reload: Reload the essential features of the plugin");
					});
		}
		return command;
	}

	public class PurgeCounter {
		int purgeCount;

		public int getPurgeCount() {
			return this.purgeCount;
		}

		public void setPurgeCount(int purgeCount) {
			this.purgeCount = purgeCount;
		}
	}
}
