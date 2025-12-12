package com.swiftlicious.hellblock.commands.sub;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

public class DebugWorldsCommand extends BukkitCommandFeature<CommandSender> {

	private final Map<String, Integer> cachedCounts = new ConcurrentHashMap<>();
	private SchedulerTask tileTask;

	public DebugWorldsCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	public void startCachingTileEntities(long updateIntervalTicks) {
		tileTask = plugin.getScheduler().sync()
				.runRepeating(() -> plugin.getWorldManager().getLoadedManagedBukkitWorlds().forEach(world -> {
					int count = 0;
					for (Chunk chunk : world.getLoadedChunks()) {
						count += chunk.getTileEntities().length;
					}
					cachedCounts.put(world.getName(), count);
				}), 0L, updateIntervalTicks, LocationUtils.getAnyLocationInstance()); // e.g. every 200L = 10s
	}

	public void stopCachingTileEntities() {
		if (tileTask != null && !tileTask.isCancelled()) {
			tileTask.cancel();
			tileTask = null;
		}
	}

	private int getTileEntitiesCount(World world) {
		return cachedCounts.getOrDefault(world.getName(), -1); // -1 means not yet cached
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.optional("world", StringParser.stringComponent().suggestionProvider((context, input) -> {
			final List<String> suggestions = plugin.getWorldManager().getLoadedManagedBukkitWorlds().stream()
					.map(worldName -> plugin.getWorldManager().getWorld(worldName)).filter(Optional::isPresent)
					.map(Optional::get).map(HellblockWorld::worldName).filter(Objects::nonNull).toList();

			return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
		})).handler(context -> {
			String worldName = context.getOrDefault("world", null);
			List<World> worlds = plugin.getWorldManager().getLoadedManagedBukkitWorlds();

			// If no worlds are loaded at all
			if (worlds.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DEBUG_WORLDS_FAILURE);
				return;
			}

			// No argument — show summary
			if (worldName == null) {
				handleFeedback(context, MessageConstants.COMMAND_DEBUG_WORLDS_LIST_SUMMARY,
						AdventureHelper.miniMessageToComponent(String.valueOf(worlds.size())));
				return;
			}

			// Argument provided — find matching world
			World matchedWorld = null;
			for (World world : worlds) {
				if (world.getName().equalsIgnoreCase(worldName)) {
					matchedWorld = world;
					break;
				}
			}

			if (matchedWorld == null) {
				handleFeedback(context, MessageConstants.COMMAND_DEBUG_WORLDS_NOT_FOUND,
						AdventureHelper.miniMessageToComponent(worldName));
				return;
			}

			Optional<HellblockWorld<?>> optional = plugin.getWorldManager().getWorld(matchedWorld);
			if (optional.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DEBUG_WORLDS_NOT_FOUND,
						AdventureHelper.miniMessageToComponent(worldName));
				return;
			}

			HellblockWorld<?> w = optional.get();
			handleFeedback(context, MessageConstants.COMMAND_DEBUG_WORLDS_SUCCESS,
					AdventureHelper.miniMessageToComponent(matchedWorld.getName()),
					AdventureHelper.miniMessageToComponent(String.valueOf(w.loadedRegions().length)),
					AdventureHelper.miniMessageToComponent(String.valueOf(w.loadedChunks().length)),
					AdventureHelper.miniMessageToComponent(String.valueOf(w.lazyChunks().length)),
					AdventureHelper.miniMessageToComponent(String.valueOf(w.bukkitWorld().getEntities().size())),
					AdventureHelper.miniMessageToComponent(String.valueOf(getTileEntitiesCount(w.bukkitWorld()))),
					AdventureHelper.miniMessageToComponent(String.valueOf(w.bukkitWorld().getPlayers().size())));
		});
	}

	@Override
	public String getFeatureID() {
		return "debug_worlds";
	}
}