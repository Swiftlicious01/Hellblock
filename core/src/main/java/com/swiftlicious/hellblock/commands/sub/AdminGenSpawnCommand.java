package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.WorldGuardHook;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.utils.ChunkUtils;

import net.kyori.adventure.text.Component;

public class AdminGenSpawnCommand extends BukkitCommandFeature<CommandSender> {

	public AdminGenSpawnCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			if (!player.getWorld().getName()
					.equalsIgnoreCase(HellblockPlugin.getInstance().getConfigManager().worldName())) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_WRONG_WORLD
						.arguments(Component.text(HellblockPlugin.getInstance().getConfigManager().worldName())));
				return;
			}

			World world = HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld();
			if (HellblockPlugin.getInstance().getConfigManager().worldguardProtect()) {
				com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
				if (HellblockPlugin.getInstance().getWorldGuardHandler().getWorldGuardPlatform() != null
						&& HellblockPlugin.getInstance().getWorldGuardHandler().getWorldGuardPlatform()
								.getRegionContainer().get(weWorld).hasRegion(WorldGuardHook.SPAWN_REGION)) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_SPAWN_ALREADY_EXISTS);
					return;
				}
			} else {
				// TODO: plugin protection
			}
			HellblockPlugin.getInstance().getHellblockHandler().generateSpawn().thenAccept(spawn -> {
				ChunkUtils.teleportAsync(player, spawn, TeleportCause.PLUGIN).thenRun(() -> {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_SPAWN_GENERATED);
				});
			});
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_genspawn";
	}
}