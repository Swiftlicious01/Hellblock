package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.utils.StringUtils;

public class DebugBiomeCommand extends BukkitCommandFeature<CommandSender> {

	public DebugBiomeCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			final Location location = player.getLocation();
			final String biomeResult = VersionHelper.getNMSManager().getBiomeResourceLocation(location);
			String biome = biomeResult;
			if (biomeResult.contains(":")) {
				String[] biomeArray = biomeResult.split(":");
				if ("minecraft".equalsIgnoreCase(biomeArray[0])) {
					biome = biomeArray[1];
				}
			}
			handleFeedback(context, MessageConstants.COMMAND_DEBUG_BIOME_RESULT,
					AdventureHelper.miniMessageToComponent(StringUtils.toCamelCase(biome)));
		});
	}

	@Override
	public String getFeatureID() {
		return "debug_biome";
	}
}