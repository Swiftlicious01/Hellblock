package com.swiftlicious.hellblock.commands.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.effects.EffectInterface;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.mechanics.fishing.FishingGears;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class DebugLootCommand extends BukkitCommandFeature<CommandSender> {

	public DebugLootCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("surrounding",
						StringParser.stringComponent()
								.suggestionProvider((context, input) -> CompletableFuture
										.completedFuture(Stream.of("lava").map(Suggestion::suggestion).toList())))
				.optional("page", StringParser.stringParser()).handler(context -> {
					String surrounding = context.get("surrounding");
					if (context.sender().getInventory().getItemInMainHand().getType() != Material.FISHING_ROD) {
						handleFeedback(context, MessageConstants.COMMAND_DEBUG_LOOT_FAILURE_ROD);
						return;
					}
					final Player player = context.sender();

					Context<Player> playerContext = Context.player(player);
					FishingGears gears = new FishingGears();
					gears.init(playerContext);

					Effect effect = EffectInterface.newInstance();
					// The effects impact mechanism at this stage
					gears.effectModifiers().forEach(modifier -> modifier.modifiers()
							.forEach(consumer -> consumer.accept(effect, playerContext, 0)));

					playerContext.arg(ContextKeys.SURROUNDING, surrounding);
					Effect tempEffect = effect.copy();
					gears.effectModifiers().forEach(modifier -> modifier.modifiers()
							.forEach(consumer -> consumer.accept(tempEffect, playerContext, 1)));

					playerContext.arg(ContextKeys.OTHER_LOCATION, player.getLocation());
					playerContext.arg(ContextKeys.OTHER_X, player.getLocation().getBlockX());
					playerContext.arg(ContextKeys.OTHER_Y, player.getLocation().getBlockY());
					playerContext.arg(ContextKeys.OTHER_Z, player.getLocation().getBlockZ());

					Map<String, Double> weightMap = plugin.getLootManager().getWeightedLoots(tempEffect, playerContext);

					if (weightMap.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DEBUG_LOOT_FAILURE_NO_LOOT);
						return;
					}

					List<LootWithWeight> loots = new ArrayList<>();
					double sum = 0;
					for (Map.Entry<String, Double> entry : weightMap.entrySet()) {
						double weight = entry.getValue();
						String loot = entry.getKey();
						if (weight <= 0)
							continue;
						loots.add(new LootWithWeight(loot, weight));
						sum += weight;
					}
					LootWithWeight[] lootArray = loots.toArray(new LootWithWeight[0]);
					int maxPages = (int) Math.ceil((double) lootArray.length / 10) - 1;

					int page;
					// Extract the page argument as a string (if present), or default to "1"
					String pageInput = context.<String>optional("page").orElse("1");
					try {
						page = Integer.parseInt(pageInput) - 1;
					} catch (NumberFormatException e) {
						handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
								AdventureHelper.miniMessageToComponent(pageInput),
								AdventureHelper.miniMessageToComponent(String.valueOf(maxPages)));
						return;
					}

					if (page < 1 || page > maxPages) {
						handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
								AdventureHelper.miniMessageToComponent(String.valueOf(page)),
								AdventureHelper.miniMessageToComponent(String.valueOf(maxPages)));
						return;
					}

					quickSort(lootArray, 0, lootArray.length - 1);
					Component component = Component.empty();
					List<Component> children = new ArrayList<>();
					int i = 0;
					for (LootWithWeight loot : lootArray) {
						if (i >= page * 10 && i < page * 10 + 10) {
							children.add(
									Component
											.newline().append(
													AdventureHelper
															.miniMessageToComponent(loot.key + ": ").color(
																	NamedTextColor.WHITE))
											.append(AdventureHelper
													.miniMessageToComponent(
															String.format("%.4f", loot.weight * 100 / sum) + "% ")
													.color(NamedTextColor.GOLD))
											.append(AdventureHelper.miniMessageToComponent("(" + loot.weight + ")")
													.color(NamedTextColor.GRAY)));
						}
						i++;
					}
					handleFeedback(context, MessageConstants.COMMAND_DEBUG_LOOT_SUCCESS, component.children(children));
					Component previous = AdventureHelper.miniMessageToComponent("( <<< )");
					if (page > 0) {
						previous = previous.color(NamedTextColor.GREEN).clickEvent(ClickEvent
								.runCommand(commandConfig.getUsages().get(0) + " " + surrounding + " " + (page)));
					} else {
						previous = previous.color(NamedTextColor.GRAY);
					}
					Component next = AdventureHelper.miniMessageToComponent("( >>> )");
					if (page < maxPages) {
						next = next.color(NamedTextColor.GREEN).clickEvent(ClickEvent
								.runCommand(commandConfig.getUsages().get(0) + " " + surrounding + " " + (page + 2)));
					} else {
						next = next.color(NamedTextColor.GRAY);
					}
					plugin.getSenderFactory().wrap(player)
							.sendMessage(Component.empty()
									.children(List.of(previous, AdventureHelper.miniMessageToComponent("   "),
											Component.text("[" + (page + 1) + "/" + (maxPages + 1) + "]")
													.color(NamedTextColor.AQUA),
											AdventureHelper.miniMessageToComponent("   "), next)));
				});
	}

	@Override
	public String getFeatureID() {
		return "debug_loot";
	}

	public record LootWithWeight(String key, double weight) {
	}

	private static void quickSort(LootWithWeight[] loot, int low, int high) {
		if (low < high) {
			int pi = partition(loot, low, high);
			quickSort(loot, low, pi - 1);
			quickSort(loot, pi + 1, high);
		}
	}

	private static int partition(LootWithWeight[] loot, int low, int high) {
		double pivot = loot[high].weight();
		int i = low - 1;
		for (int j = low; j <= high - 1; j++) {
			if (loot[j].weight() > pivot) {
				i++;
				swap(loot, i, j);
			}
		}
		swap(loot, i + 1, high);
		return i + 1;
	}

	private static void swap(LootWithWeight[] loot, int i, int j) {
		LootWithWeight temp = loot[i];
		loot[i] = loot[j];
		loot[j] = temp;
	}
}