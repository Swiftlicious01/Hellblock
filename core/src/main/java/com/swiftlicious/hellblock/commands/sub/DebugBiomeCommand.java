package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.handlers.VersionHelper;

import net.kyori.adventure.text.Component;

public class DebugBiomeCommand extends BukkitCommandFeature<CommandSender> {

	public DebugBiomeCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			HellblockPlugin.getInstance().getSenderFactory().wrap(context.sender()).sendMessage(Component
					.text(VersionHelper.getNMSManager().getBiomeResourceLocation(context.sender().getLocation())));
		});
	}

	@Override
	public String getFeatureID() {
		return "debug_biome";
	}
}