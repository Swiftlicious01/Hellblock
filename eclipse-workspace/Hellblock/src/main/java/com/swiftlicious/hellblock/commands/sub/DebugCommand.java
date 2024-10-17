package com.swiftlicious.hellblock.commands.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.effects.FishingEffect;
import com.swiftlicious.hellblock.handlers.AdventureManager;
import com.swiftlicious.hellblock.listeners.fishing.FishingPreparation;
import com.swiftlicious.hellblock.utils.NBTUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.IStringTooltip;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class DebugCommand {

	public static DebugCommand INSTANCE = new DebugCommand();

	public CommandAPICommand getDebugCommand() {
		return new CommandAPICommand("debug").withSubcommands(getLootChanceCommand(), getGroupCommand(), getNBTCommand(), getLocationCommand());
	}

	public CommandAPICommand getLocationCommand() {
		return new CommandAPICommand("location").executesPlayer((player, arg) -> {
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player, player.getLocation().toString());
		});
	}

	public CommandAPICommand getNBTCommand() {
		return new CommandAPICommand("nbt").executesPlayer((player, arg) -> {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR)
				return;
			if (NBTUtils.compoundToMap(new RtagItem(item)).isEmpty())
				return;
			List<String> list = new ArrayList<>();
			HellblockPlugin.getInstance().getConfigUtils().mapToReadableStringList(NBTUtils.compoundToMap(new RtagItem(item)), list, 0, false);
			for (String line : list) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player, line);
			}
		});
	}
	
	public CommandAPICommand getGroupCommand() {
		return new CommandAPICommand("group").withArguments(new StringArgument("group")).executes((sender, arg) -> {
			String group = (String) arg.get("group");
			StringJoiner stringJoiner = new StringJoiner("<white>, </white>");
			List<String> groups = HellblockPlugin.getInstance().getLootManager().getLootGroup(group);
			if (groups != null)
				for (String key : groups) {
					stringJoiner.add(key);
				}
			HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
					"<white>Group<gold>{" + group + "}<yellow>[" + stringJoiner + "]");
		});
	}

	public CommandAPICommand getLootChanceCommand() {
		return new CommandAPICommand("loot-chance")
				.withArguments(
						new BooleanArgument("lava fishing").replaceSuggestions(ArgumentSuggestions.stringsWithTooltips(
								info -> new IStringTooltip[] { StringTooltip.ofString("true", "loots in lava"),
										StringTooltip.ofString("false", "loots in water") })))
				.executesPlayer((player, arg) -> {
					if (player.getInventory().getItemInMainHand().getType() != Material.FISHING_ROD) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Please hold a fishing rod before using this command.");
						return;
					}
					FishingEffect initialEffect = HellblockPlugin.getInstance().getEffectManager().getInitialEffect();
					FishingPreparation fishingPreparation = new FishingPreparation(player,
							HellblockPlugin.getInstance());
					boolean inLava = (boolean) arg.getOrDefault("lava fishing", false);
					fishingPreparation.insertArg("{lava}", String.valueOf(inLava));
					fishingPreparation.mergeEffect(initialEffect);
					var map = HellblockPlugin.getInstance().getLootManager().getPossibleLootKeysWithWeight(initialEffect,
							fishingPreparation);
					List<LootWithWeight> loots = new ArrayList<>();
					double sum = 0;
					for (Map.Entry<String, Double> entry : map.entrySet()) {
						double weight = entry.getValue();
						String loot = entry.getKey();
						if (weight <= 0)
							continue;
						loots.add(new LootWithWeight(loot, weight));
						sum += weight;
					}
					LootWithWeight[] lootArray = loots.toArray(new LootWithWeight[0]);
					quickSort(lootArray, 0, lootArray.length - 1);
					AdventureManager adventureManager = HellblockPlugin.getInstance().getAdventureManager();
					adventureManager.sendMessage(player, "<red>---------- results ---------");
					for (LootWithWeight loot : lootArray) {
						adventureManager.sendMessage(player,
								loot.key() + ": <gold>" + String.format("%.2f", loot.weight() * 100 / sum) + "% <gray>("
										+ String.format("%.2f", loot.weight()) + ")");
					}
					adventureManager.sendMessage(player, "<red>----------- end -----------");
				});
	}

	public record LootWithWeight(String key, double weight) {
	}

	private void quickSort(LootWithWeight[] loot, int low, int high) {
		if (low < high) {
			int pi = partition(loot, low, high);
			quickSort(loot, low, pi - 1);
			quickSort(loot, pi + 1, high);
		}
	}

	private int partition(LootWithWeight[] loot, int low, int high) {
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

	private void swap(LootWithWeight[] loot, int i, int j) {
		LootWithWeight temp = loot[i];
		loot[i] = loot[j];
		loot[j] = temp;
	}
}