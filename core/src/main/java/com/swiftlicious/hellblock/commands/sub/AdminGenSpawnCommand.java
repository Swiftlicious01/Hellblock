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
import com.swiftlicious.hellblock.utils.ChunkUtils;

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
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>You aren't in the correct world to generate the spawn!");
				return;
			}

			World world = HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld();
			if (HellblockPlugin.getInstance().getConfigManager().worldguardProtect()) {
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

	@Override
	public String getFeatureID() {
		return "admin_genspawn";
	}
}