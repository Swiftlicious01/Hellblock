package com.swiftlicious.hellblock.commands;

import java.util.Collection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.sub.DataCommand;
import com.swiftlicious.hellblock.commands.sub.DebugCommand;
import com.swiftlicious.hellblock.commands.sub.GUIEditorCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockAdminCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockCoopCommand;
import com.swiftlicious.hellblock.commands.sub.HellblockUserCommand;
import com.swiftlicious.hellblock.commands.sub.ItemCommand;
import com.swiftlicious.hellblock.config.HBLocale;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.UUIDArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class CommandManager implements CommandManagerInterface {

	private final HellblockPlugin instance;

	public CommandManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		new CommandAPICommand("hellblock").withAliases("hellisland")
				.withSubcommands(getReloadCommand(), getOpenCommand(), HellblockAdminCommand.INSTANCE.getAdminCommand(),
						GUIEditorCommand.INSTANCE.getEditorCommand(), DataCommand.INSTANCE.getDataCommand(),
						HellblockUserCommand.INSTANCE.getMenuCommand(), HellblockUserCommand.INSTANCE.getResetCommand(),
						HellblockUserCommand.INSTANCE.getCreateCommand(),
						HellblockUserCommand.INSTANCE.getHomeCommand(), HellblockUserCommand.INSTANCE.getBiomeCommand(),
						HellblockUserCommand.INSTANCE.getLockCommand(),
						HellblockUserCommand.INSTANCE.getSetHomeCommand(),
						HellblockUserCommand.INSTANCE.getInfoCommand(), HellblockUserCommand.INSTANCE.getHelpCommand(),
						HellblockUserCommand.INSTANCE.getBanCommand(), HellblockUserCommand.INSTANCE.getUnbanCommand(),
						HellblockUserCommand.INSTANCE.getTopCommand(), HellblockUserCommand.INSTANCE.getVisitCommand(),
						getAboutCommand(), ItemCommand.INSTANCE.getItemCommand(),
						DebugCommand.INSTANCE.getDebugCommand(), HellblockCoopCommand.INSTANCE.getCoopCommand())
				.register();
		if (instance.getMarketManager().isEnable()) {
			new CommandAPICommand("sellfish").withPermission("hellblock.sellfish").executesPlayer((player, args) -> {
				if (instance.getMarketManager().isEnable())
					instance.getMarketManager().openMarketGUI(player);
			}).register();
		}
	}

	@Override
	public void unload() {
	}

	private CommandAPICommand getReloadCommand() {
		return new CommandAPICommand("reload").executes((sender, args) -> {
			long time = System.currentTimeMillis();
			instance.reload();
			instance.getAdventureManager().sendMessageWithPrefix(sender,
					HBLocale.MSG_Reload.replace("{time}", String.valueOf(System.currentTimeMillis() - time)));
		});
	}

	private CommandAPICommand getOpenCommand() {
		CommandAPICommand command = new CommandAPICommand("open");
		if (instance.getMarketManager().isEnable()) {
			command.withSubcommands(
					new CommandAPICommand("market").withArguments(new EntitySelectorArgument.ManyPlayers("player"))
							.withOptionalArguments(new StringArgument("-s")).executes((sender, args) -> {
								if (args.get("player") instanceof Collection<?>) {
									Collection<?> players = (Collection<?>) args.get("player");
									assert players != null;
									boolean silence = args.getOrDefault("-s", "").equals("-s");
									for (Object object : players) {
										if (object instanceof Player) {
											Player player = (Player) object;
											instance.getMarketManager().openMarketGUI(player);
											if (!silence)
												instance.getAdventureManager().sendMessageWithPrefix(sender,
														HBLocale.MSG_Market_GUI_Open.replace("{player}",
																player.getName()));
										}
									}
								}
							}),
					new CommandAPICommand("market-uuid").withArguments(new UUIDArgument("uuid"))
							.withOptionalArguments(new StringArgument("-s")).executes((sender, args) -> {
								UUID uuid = (UUID) args.get("uuid");
								Player player = Bukkit.getPlayer(uuid);
								boolean silence = args.getOrDefault("-s", "").equals("-s");
								if (player == null)
									return;
								instance.getMarketManager().openMarketGUI(player);
								if (!silence)
									instance.getAdventureManager().sendMessageWithPrefix(sender,
											HBLocale.MSG_Market_GUI_Open.replace("{player}", player.getName()));
							}));
		}
		return command;
	}

	private CommandAPICommand getAboutCommand() {
		return new CommandAPICommand("about").executes((sender, args) -> {
			instance.getAdventureManager().sendMessage(sender, "<#00BFFF>\uD83C\uDFDD Hellblock <gray>- <#87CEEB>"
					+ instance.getVersionManager().getPluginVersion());
			instance.getAdventureManager().sendMessage(sender, "<#B0C4DE>" + instance.getPluginMeta().getDescription());
			instance.getAdventureManager().sendMessage(sender,
					"<#DA70D6>\uD83E\uDDEA Author: <#FFC0CB>" + instance.getPluginMeta().getAuthors().getFirst());
			instance.getAdventureManager().sendMessage(sender,
					"<#FAFAD2>⛏ <click:open_url:https://github.com/Swiftlicious01/Hellblock>Github</click>");
		});
	}
}