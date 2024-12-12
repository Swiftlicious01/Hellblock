package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.text.Component;

public class DebugWorldsCommand extends BukkitCommandFeature<CommandSender> {

	public DebugWorldsCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.handler(context -> {
			int worldCount = 0;
			for (World world : Bukkit.getWorlds()) {
				Optional<HellblockWorld<?>> optional = HellblockPlugin.getInstance().getWorldManager().getWorld(world);
				if (optional.isPresent()) {
					worldCount++;
					HellblockWorld<?> w = optional.get();
					handleFeedback(context, MessageConstants.COMMAND_DEBUG_WORLDS_SUCCESS,
							Component.text(world.getName()), Component.text(w.loadedRegions().length),
							Component.text(w.loadedChunks().length), Component.text(w.lazyChunks().length));
				}
			}
			if (worldCount == 0) {
				handleFeedback(context, MessageConstants.COMMAND_DEBUG_WORLDS_FAILURE);
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "debug_worlds";
	}
}