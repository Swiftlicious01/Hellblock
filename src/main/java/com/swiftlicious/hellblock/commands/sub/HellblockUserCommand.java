package com.swiftlicious.hellblock.commands.sub;

import java.util.UUID;
import java.util.stream.Collectors;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.gui.hellblock.HellblockMenu;
import com.swiftlicious.hellblock.gui.hellblock.IslandChoiceMenu;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.UUIDFetcher;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

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

					HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(player.getUniqueId(), false);
					new IslandChoiceMenu(player);
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

	public CommandAPICommand getVisitCommand() {
		return new CommandAPICommand("visit").withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions
						.stringCollection(collection -> HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayers().values().stream().filter(hbPlayer -> hbPlayer.getPlayer() != null)
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					String user = (String) args.getOrDefault("player", player);
					UUID id = UUIDFetcher.getUUID(user);
					HellblockPlayer ti = null;
					if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().containsKey(id)) {
						ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().get(id);
					} else {
						ti = new HellblockPlayer(id);
					}

					if (ti.hasHellblock()) {
						player.teleportAsync(ti.getHomeLocation());
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You are visiting <dark_red>%s<red>'s hellblock!", user));
						return;
					}

					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>That player does not have a hellblock!");
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
