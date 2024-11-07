package com.swiftlicious.hellblock.commands.sub;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.google.common.io.Files;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.WorldGuardHook;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer.HellblockData;
import com.swiftlicious.hellblock.playerdata.UUIDFetcher;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.LogUtils;

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
						getTeleportCommand("goto"), getDeleteCommand("delete"));
	}

	private CommandAPICommand getTeleportCommand(String namespace) {
		return new CommandAPICommand(namespace).withArguments(
				new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
					if (info.sender() instanceof Player player) {
						return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values().stream()
								.filter(hbPlayer -> hbPlayer.getPlayer() != null && hbPlayer.hasHellblock()
										&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
					} else {
						return Collections.emptyList();
					}
				}))).executesPlayer((player, args) -> {
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
					HellblockPlayer ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(id);
					if (ti.hasHellblock()) {
						LocationUtils.isSafeLocationAsync(ti.getHomeLocation()).thenAccept((result) -> {
							if (!result.booleanValue()) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
								HellblockPlugin.getInstance().getHellblockHandler().locateBedrock(id)
										.thenAccept((bedrock) -> {
											ti.setHome(bedrock.getBedrockLocation());
											HellblockPlugin.getInstance().getCoopManager().updateParty(id,
													HellblockData.HOME, ti.getHomeLocation());
										});
							}
						}).thenRunAsync(() -> {
							ChunkUtils.teleportAsync(player, ti.getHomeLocation(), TeleportCause.PLUGIN).thenRun(() -> {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player, String
										.format("<red>You've been teleported to <dark_red>%s<red>'s hellblock!", user));
							});
						});
						return;
					}

					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>That player does not have a hellblock!");
					return;
				});
	}

	private CommandAPICommand getPurgeCommand(String namespace) {
		return new CommandAPICommand(namespace).withArguments(new IntegerArgument("days")).executes((sender, args) -> {
			int purgeDays = (int) args.get("days");
			if (HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory().listFiles().length == 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
						"<red>No hellblock player data to purge available!");
				return;
			}
			if (purgeDays <= 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
						"<red>Please enter a positive number above 0!");
				return;
			}
			int purgeCount = 0;
			for (File playerData : HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory()
					.listFiles()) {
				if (!playerData.isFile() || !playerData.getName().endsWith(".yml"))
					continue;
				String uuid = Files.getNameWithoutExtension(playerData.getName());
				UUID id = null;
				try {
					id = UUID.fromString(uuid);
				} catch (IllegalArgumentException ignored) {
					// ignored
					continue;
				}
				if (id == null)
					continue;
				if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
					continue;

				OfflinePlayer player = Bukkit.getOfflinePlayer(id);
				if (player.getLastLogin() == 0)
					continue;
				long millisSinceLastLogin = (System.currentTimeMillis() - player.getLastLogin()) -
				// Account for a timezone difference
						TimeUnit.MILLISECONDS.toHours(19);
				if (millisSinceLastLogin > TimeUnit.DAYS.toMillis(purgeDays)) {
					YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerData);
					if (playerConfig.getKeys(true).size() == 0)
						continue;
					String ownerID = playerConfig.getString("player.owner");
					if (ownerID == null)
						continue;
					UUID ownerUUID = null;
					try {
						ownerUUID = UUID.fromString(ownerID);
					} catch (IllegalArgumentException ignored) {
						// ignored
						continue;
					}
					if (ownerUUID == null)
						continue;
					if (HellblockPlugin.getInstance().getHellblockHandler().isHellblockOwner(id, ownerUUID)) {
						float level = (float) playerConfig.getDouble("player.hellblock-level",
								HellblockPlayer.DEFAULT_LEVEL);
						if (level == HellblockPlayer.DEFAULT_LEVEL) {

							playerConfig.set("player.abandoned", true);
							try {
								playerConfig.save(playerData);
							} catch (IOException ex) {
								LogUtils.warn(
										String.format("Could not save the player data file as abandoned for %s", uuid),
										ex);
								continue;
							}
							int hellblockID = playerConfig.getInt("player.hellblock-id");
							if (HellblockPlugin.getInstance().getWorldGuardHandler().getRegion(ownerUUID,
									hellblockID) != null) {
								HellblockPlugin.getInstance().getWorldGuardHandler().updateHellblockMessages(ownerUUID,
										HellblockPlugin.getInstance().getWorldGuardHandler().getRegion(ownerUUID,
												hellblockID));
								HellblockPlugin.getInstance().getWorldGuardHandler().abandonIsland(ownerUUID,
										HellblockPlugin.getInstance().getWorldGuardHandler().getRegion(ownerUUID,
												hellblockID));
								purgeCount++;
							}
						}
					}
				}
			}

			if (purgeCount > 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
						String.format("<red>You purged a total of %s hellblocks!", purgeCount));
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
						"<red>No hellblock data was purged with your inputted settings!");
			}
		});
	}

	private CommandAPICommand getDeleteCommand(String namespace) {
		return new CommandAPICommand(namespace).withArguments(
				new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
					if (info.sender() instanceof Player player) {
						return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values().stream()
								.filter(hbPlayer -> hbPlayer.getPlayer() != null && hbPlayer.hasHellblock()
										&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
					} else {
						return Collections.emptyList();
					}
				}))).executesPlayer((player, args) -> {
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
					HellblockPlayer ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(id);
					if (ti.hasHellblock()) {
						HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(id, true).thenRun(() -> {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player, String
									.format("<red>You've forcefully deleted <dark_red>%s<red>'s hellblock!", user));
						});
						return;
					}

					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>That player does not have a hellblock!");
					return;
				});
	}

	private CommandAPICommand getGenSpawnCommand(String namespace) {
		return new CommandAPICommand(namespace).executesPlayer((player, args) -> {
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
		return new CommandAPICommand(namespace).executes((sender, args) -> {
			long time = System.currentTimeMillis();
			HellblockPlugin.getInstance().reload();
			HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
					HBLocale.MSG_Reload.replace("{time}", String.valueOf(System.currentTimeMillis() - time)));
		});
	}
}
