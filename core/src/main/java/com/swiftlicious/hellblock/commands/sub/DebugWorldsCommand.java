package com.swiftlicious.hellblock.commands.sub;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
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
							Component.text(w.loadedChunks().length), Component.text(w.lazyChunks().length),
							Component.text(w.bukkitWorld().getEntities().size()),
							Component.text(getTileEntitiesCount(w.bukkitWorld())),
							Component.text(w.bukkitWorld().getPlayers().size()));
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

	private static final Map<String, Integer> cachedCounts = new ConcurrentHashMap<>();
	private static SchedulerTask tileTask;

	public static void startCachingTileEntities(long updateIntervalTicks) {
		tileTask = HellblockPlugin.getInstance().getScheduler()
				.asyncRepeating(() -> Bukkit.getWorlds().forEach(world -> {
					int count = 0;
					for (Chunk chunk : world.getLoadedChunks()) {
						count += chunk.getTileEntities().length;
					}
					cachedCounts.put(world.getName(), count);
				}), 0L, updateIntervalTicks, TimeUnit.MINUTES); // e.g. every 200L = 10s
	}

	public static void stopCachingTileEntities() {
		if (tileTask != null && !tileTask.isCancelled()) {
			tileTask.cancel();
			tileTask = null;
		}
	}

	private int getTileEntitiesCount(World world) {
		return cachedCounts.getOrDefault(world.getName(), -1); // -1 means not yet cached
	}
}