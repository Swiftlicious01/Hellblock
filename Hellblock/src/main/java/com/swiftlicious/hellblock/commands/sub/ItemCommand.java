package com.swiftlicious.hellblock.commands.sub;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.BuildableItem;
import com.swiftlicious.hellblock.utils.ItemUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.NBTUtils;
import com.swiftlicious.hellblock.utils.extras.Condition;
import com.swiftlicious.hellblock.utils.extras.Key;

import de.tr7zw.changeme.nbtapi.NBT;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class ItemCommand {

	public static ItemCommand INSTANCE = new ItemCommand();

	private final Map<String, String[]> completionMap = new HashMap<>();

	public CommandAPICommand getItemCommand() {
		return new CommandAPICommand("items").withSubcommands(getSubCommand("item"), getSubCommand("util"),
				getSubCommand("bait"), getSubCommand("rod"), getSubCommand("hook"));
	}

	private CommandAPICommand getSubCommand(String namespace) {
		completionMap.put(namespace, HellblockPlugin.getInstance().getItemManager().getAllItemsKey().stream()
				.filter(it -> it.namespace().equals(namespace)).map(Key::value).toList().toArray(new String[0]));
		return new CommandAPICommand(namespace).withSubcommands(getCommand(namespace), giveCommand(namespace),
				importCommand(namespace));
	}

	private CommandAPICommand importCommand(String namespace) {
		return new CommandAPICommand("import").withArguments(new StringArgument("key"))
				.withOptionalArguments(new StringArgument("file")).executesPlayer((player, args) -> {
					String key = (String) args.get("key");
					String fileName = args.getOrDefault("file", "import") + ".yml";
					ItemStack itemStack = player.getInventory().getItemInMainHand();
					if (itemStack.getType() == Material.AIR)
						return;
					File file = new File(HellblockPlugin.getInstance().getDataFolder(),
							"contents" + File.separator + namespace + File.separator + fileName);
					try {
						if (!file.exists()) {
							file.createNewFile();
						}
						YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
						config.set(key + ".material", itemStack.getType().toString());
						config.set(key + ".amount", itemStack.getAmount());
						Map<String, Object> nbtMap = NBTUtils.compoundToMap(NBT.itemStackToNBT(itemStack));
						if (nbtMap.size() != 0) {
							config.createSection(key + ".nbt", nbtMap);
						}
						try {
							config.save(file);
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"Imported! Saved to " + file.getAbsolutePath());
						} catch (IOException e) {
							e.printStackTrace();
						}
					} catch (IOException e) {
						LogUtils.warn("Failed to create imported file.", e);
					}
				});
	}

	private CommandAPICommand getCommand(String namespace) {
		return new CommandAPICommand("get")
				.withArguments(new StringArgument("id")
						.replaceSuggestions(ArgumentSuggestions.strings(info -> completionMap.get(namespace))))
				.withOptionalArguments(new IntegerArgument("amount", 1)).executesPlayer((player, args) -> {
					String id = (String) args.get("id");
					assert id != null;
					int amount = (int) args.getOrDefault("amount", 1);
					ItemStack item = HellblockPlugin.getInstance().getItemManager().build(player, namespace, id,
							new Condition(player).getArgs());
					if (item != null) {
						int actual = ItemUtils.giveItem(player, item, amount);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"Successfully got {amount}x {item}.".replace("{item}", id).replace("{amount}",
										String.valueOf(actual)));
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Item not found.");
					}
				});
	}

	private CommandAPICommand giveCommand(String namespace) {
		return new CommandAPICommand("give").withArguments(new EntitySelectorArgument.ManyPlayers("player"))
				.withArguments(new StringArgument("id")
						.replaceSuggestions(ArgumentSuggestions.strings(info -> completionMap.get(namespace))))
				.withOptionalArguments(new IntegerArgument("amount", 1)).withOptionalArguments(new StringArgument("-s"))
				.executes((sender, args) -> {
					if (args.get("player") instanceof Collection<?>) {
						Collection<?> players = (Collection<?>) args.get("player");
						String id = (String) args.get("id");
						boolean silence = args.getOrDefault("-s", "").equals("-s");
						int amount = (int) args.getOrDefault("amount", 1);
						BuildableItem buildableItem = HellblockPlugin.getInstance().getItemManager()
								.getBuildableItem(namespace, id);
						if (buildableItem != null) {
							assert players != null;
							for (Object object : players) {
								if (object instanceof Player) {
									Player player = (Player) object;
									ItemStack item = HellblockPlugin.getInstance().getItemManager().build(player,
											namespace, id, new Condition(player).getArgs());
									int actual = ItemUtils.giveItem(player, item, amount);
									if (!silence)
										HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
												sender,
												"Successfully given player {player} {amount}x {item}."
														.replace("{item}", id)
														.replace("{amount}", String.valueOf(actual))
														.replace("{player}", player.getName()));
								}
							}
						} else {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
									"<red>Item not found.");
						}
					}
				});
	}
}