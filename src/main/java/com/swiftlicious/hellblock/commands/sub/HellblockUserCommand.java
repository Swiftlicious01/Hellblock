package com.swiftlicious.hellblock.commands.sub;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.gui.hellblock.HellblockMenu;
import com.swiftlicious.hellblock.gui.hellblock.IslandChoiceMenu;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class HellblockUserCommand {

	public static HellblockUserCommand INSTANCE = new HellblockUserCommand();

	public CommandAPICommand getMenuCommand() {
		return new CommandAPICommand("menu").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					new HellblockMenu(player);
				});
	}

	public CommandAPICommand getResetCommand() {
		return new CommandAPICommand("reset").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
						return;
					}
					if (!pi.getHellblockOwner().equals(player.getUniqueId())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't own this hellblock!");
						return;
					}
					if (pi.getResetCooldown() > 0) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format(
										"<red>You have recently rest your hellblock already, you must wait for %s!",
										HellblockPlugin.getInstance().getFormattedCooldown(pi.getResetCooldown())));
						return;
					}
					int z_operate;
					Location loc = pi.getHellblockLocation();
					double y = loc.getY();
					z_operate = (int) loc.getX() - HellblockPlugin.getInstance().getHellblockHandler().getDistance();

					while (true) {
						if (z_operate > (int) loc.getX()
								+ HellblockPlugin.getInstance().getHellblockHandler().getDistance()) {
							HellblockPlugin.getInstance().getWorldGuardHandler().unprotectHellblock(player);
							pi.setHellblock(false, null);
							pi.setHellblockOwner(null);
							pi.setHellblockBiome(null);
							pi.setBiomeCooldown(0L);
							pi.setUsedSchematic(null);
							pi.setIslandChoice(null);
							pi.setResetCooldown(Duration.ofDays(1).toHours());
							List<UUID> party = pi.getHellblockParty();
							if (party != null && !party.isEmpty()) {
								for (UUID id : party) {
									Player member = Bukkit.getPlayer(id);
									if (member != null && member.isOnline()) {
										HellblockPlayer hbMember = HellblockPlugin.getInstance().getHellblockHandler()
												.getActivePlayer(member);
										hbMember.setHellblock(false, null);
										hbMember.setHellblockOwner(null);
										hbMember.setHellblockBiome(null);
										hbMember.setBiomeCooldown(0L);
										hbMember.setResetCooldown(0L);
										hbMember.setIslandChoice(null);
										hbMember.setUsedSchematic(null);
										hbMember.setHellblockParty(new ArrayList<>());
										hbMember.saveHellblockPlayer();
										member.performCommand(
												HellblockPlugin.getInstance().getHellblockHandler().getNetherCMD());
										HellblockPlugin.getInstance().getAdventureManager()
												.sendMessageWithPrefix(member, String.format(
														"<red>Your hellblock owner <dark_red>%s <red>has reset the island, so you have been removed.",
														player.getName()));
									} else {
										File memberFile = new File(HellblockPlugin.getInstance().getHellblockHandler()
												.getPlayersDirectory() + File.separator + id + ".yml");
										YamlConfiguration memberConfig = YamlConfiguration
												.loadConfiguration(memberFile);
										memberConfig.set("player.hasHellblock", false);
										memberConfig.set("player.hellblock", null);
										memberConfig.set("player.home", null);
										memberConfig.set("player.owner", null);
										memberConfig.set("player.biome", null);
										memberConfig.set("player.party", null);
										memberConfig.set("player.reset-cooldown", null);
										memberConfig.set("player.biome-cooldown", null);
										memberConfig.set("player.island-choice", null);
										memberConfig.set("player.island-choice.schematic", null);
										try {
											memberConfig.save(memberFile);
										} catch (IOException ex) {
											LogUtils.severe(String.format("Unable to save member file for %s!", id),
													ex);
										}
									}
								}
							}
							pi.setHellblockParty(new ArrayList<>());
							new IslandChoiceMenu(player);
							break;
						}

						for (int x_operate = (int) loc.getZ() - HellblockPlugin.getInstance().getHellblockHandler()
								.getDistance(); x_operate <= (int) loc.getZ()
										+ HellblockPlugin.getInstance().getHellblockHandler()
												.getDistance(); ++x_operate) {
							Block block = loc.getWorld().getBlockAt(x_operate, (int) y, z_operate);
							if (block.getType() != Material.AIR) {
								block.setType(Material.AIR);
								block.getState().update();
							}
						}

						++z_operate;
					}
				});
	}

	public CommandAPICommand getCreateCommand() {
		return new CommandAPICommand("create").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						new IslandChoiceMenu(player);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You already have a hellblock!");
					}
				});
	}

	public CommandAPICommand getHomeCommand() {
		return new CommandAPICommand("home").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					} else {
						if (pi.getHomeLocation() != null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Teleporting you to your hellblock!");
							player.teleportAsync(pi.getHomeLocation());
							// if raining give player a bit of protection
							if (HellblockPlugin.getInstance().getLavaRain().getLavaRainTask() != null
									&& HellblockPlugin.getInstance().getLavaRain().getLavaRainTask().isLavaRaining()
									&& HellblockPlugin.getInstance().getLavaRain()
											.getHighestBlock(player.getLocation()) != null
									&& !HellblockPlugin.getInstance().getLavaRain()
											.getHighestBlock(player.getLocation()).isEmpty()) {
								player.setNoDamageTicks(5 * 20);
							}
						} else {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Error teleporting you to your hellblock!");
						}
					}
				});
	}
}
