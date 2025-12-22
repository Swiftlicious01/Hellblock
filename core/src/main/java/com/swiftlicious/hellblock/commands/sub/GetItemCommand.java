package com.swiftlicious.hellblock.commands.sub;

import java.util.concurrent.CompletableFuture;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.utils.PlayerUtils;

import net.kyori.adventure.text.Component;

public class GetItemCommand extends BukkitCommandFeature<CommandSender> {

	public GetItemCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("id",
						StringParser.stringComponent(StringParser.StringMode.QUOTED)
								.suggestionProvider((context,
										input) -> CompletableFuture.completedFuture(plugin.getItemManager().getItemIDs()
												.stream().map(it -> Suggestion.suggestion("\"" + it + "\"")).toList())))
				.optional("amount", IntegerParser.integerParser(1, 6400))
				.flag(manager.flagBuilder("silent").withAliases("s").build())
				.flag(manager.flagBuilder("to-inventory").withAliases("t").build()).handler(context -> {
					final int amount = context.getOrDefault("amount", 1);
					boolean toInv = context.flags().hasFlag("to-inventory");
					final String id = context.get("id");
					final Player player = context.sender();
					try {
						ItemStack itemStack = plugin.getItemManager()
								.buildInternal(Context.player(player).arg(ContextKeys.ID, id), id);
						if (itemStack == null) {
							handleFeedback(context, MessageConstants.COMMAND_ITEM_FAILURE_NOT_EXIST,
									Component.text(id));
							return;
						}
						int amountToGive = amount;
						int maxStack = itemStack.getMaxStackSize();
						while (amountToGive > 0) {
							int perStackSize = Math.min(maxStack, amountToGive);
							amountToGive -= perStackSize;
							ItemStack more = itemStack.clone();
							more.setAmount(perStackSize);
							if (VersionHelper.isFolia()) {
								plugin.runFoliaPlayerTask(player, plugin, p -> {
									if (toInv || p.getGameMode() == GameMode.SPECTATOR) {
										PlayerUtils.putItemsToInventory(p.getInventory(), more, more.getAmount());
									} else {
										PlayerUtils.dropItem(p, more, false, true, false);
									}
								});
							} else {
								if (toInv || player.getGameMode() == GameMode.SPECTATOR) {
									PlayerUtils.putItemsToInventory(player.getInventory(), more, more.getAmount());
								} else {
									PlayerUtils.dropItem(player, more, false, true, false);
								}
							}
						}
						handleFeedback(context, MessageConstants.COMMAND_ITEM_GET_SUCCESS,
								AdventureHelper.miniMessageToComponent(String.valueOf(amount)),
								AdventureHelper.miniMessageToComponent(id));
					} catch (Exception e) {
						handleFeedback(context, MessageConstants.COMMAND_ITEM_FAILURE_NOT_EXIST,
								AdventureHelper.miniMessageToComponent(id));
						plugin.getPluginLogger().warn("Failed to get item:" + id, e);
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "get_item";
	}
}