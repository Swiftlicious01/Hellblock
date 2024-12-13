package com.swiftlicious.hellblock.commands.sub;

import java.util.concurrent.CompletableFuture;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
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
				.required("id", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
					@Override
					public @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestionsFuture(
							@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
						return CompletableFuture.completedFuture(HellblockPlugin.getInstance().getItemManager()
								.getItemIDs().stream().map(Suggestion::suggestion).toList());
					}
				})).optional("amount", IntegerParser.integerParser(1, 6400))
				.flag(manager.flagBuilder("silent").withAliases("s").build())
				.flag(manager.flagBuilder("to-inventory").withAliases("t").build()).handler(context -> {
					final int amount = context.getOrDefault("amount", 1);
					boolean toInv = context.flags().hasFlag("to-inventory");
					final String id = context.get("id");
					final Player player = context.sender();
					try {
						ItemStack itemStack = HellblockPlugin.getInstance().getItemManager()
								.buildInternal(Context.player(player).arg(ContextKeys.ID, id), id);
						if (itemStack == null) {
							throw new RuntimeException("Unrecognized item id: " + id);
						}
						int amountToGive = amount;
						int maxStack = itemStack.getMaxStackSize();
						while (amountToGive > 0) {
							int perStackSize = Math.min(maxStack, amountToGive);
							amountToGive -= perStackSize;
							ItemStack more = itemStack.clone();
							more.setAmount(perStackSize);
							if (toInv || player.getGameMode() == GameMode.SPECTATOR) {
								PlayerUtils.putItemsToInventory(player.getInventory(), more, more.getAmount());
							} else {
								PlayerUtils.dropItem(player, more, false, true, false);
							}
						}
						handleFeedback(context, MessageConstants.COMMAND_ITEM_GET_SUCCESS, Component.text(amount),
								Component.text(id));
					} catch (Exception e) {
						handleFeedback(context, MessageConstants.COMMAND_ITEM_FAILURE_NOT_EXIST, Component.text(id));
						HellblockPlugin.getInstance().getPluginLogger().warn("Failed to get item:" + id, e);
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "get_item";
	}
}