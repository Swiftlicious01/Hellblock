package com.swiftlicious.hellblock.sender;

import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.utils.extras.Tristate;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;

public class BukkitSenderFactory extends SenderFactory<HellblockPlugin, CommandSender> {
	private final BukkitAudiences audiences;

	public BukkitSenderFactory(HellblockPlugin plugin) {
		super(plugin);
		this.audiences = BukkitAudiences.create(plugin);
	}

	@Override
	protected String getName(CommandSender sender) {
		if (sender instanceof Player) {
			return sender.getName();
		}
		return Sender.CONSOLE_NAME;
	}

	@Override
	protected UUID getUniqueId(CommandSender sender) {
		if (sender instanceof Player) {
			return ((Player) sender).getUniqueId();
		}
		return Sender.CONSOLE_UUID;
	}

	@Override
	public Audience getAudience(CommandSender sender) {
		return this.audiences.sender(sender);
	}

	@Override
	protected void sendMessage(CommandSender sender, Component message) {
		// we can safely send async for players and the console - otherwise, send it
		// sync
		if (sender instanceof Player player) {
			VersionHelper.getNMSManager().sendMessage(player, AdventureHelper.componentToJson(message));
		} else if (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender) {
			getAudience(sender).sendMessage(message);
		} else {
			getPlugin().getScheduler().executeSync(() -> getAudience(sender).sendMessage(message));
		}
	}

	@Override
	protected Tristate getPermissionValue(CommandSender sender, String node) {
		if (sender.hasPermission(node)) {
			return Tristate.TRUE;
		} else if (sender.isPermissionSet(node)) {
			return Tristate.FALSE;
		} else {
			return Tristate.UNDEFINED;
		}
	}

	@Override
	protected boolean hasPermission(CommandSender sender, String node) {
		return sender.hasPermission(node);
	}

	@Override
	protected void performCommand(CommandSender sender, String command) {
		getPlugin().getServer().dispatchCommand(sender, command);
	}

	@Override
	protected boolean isConsole(CommandSender sender) {
		return sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender;
	}

	@Override
	public void close() {
		super.close();
		this.audiences.close();
	}
}