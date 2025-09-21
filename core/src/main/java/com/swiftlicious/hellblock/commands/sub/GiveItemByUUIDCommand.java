package com.swiftlicious.hellblock.commands.sub;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.utils.PlayerUtils;

import net.kyori.adventure.text.Component;

public class GiveItemByUUIDCommand extends BukkitCommandFeature<CommandSender> {

	public GiveItemByUUIDCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.required("uuid", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
			@Override
			public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(
					@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
				return CompletableFuture.completedFuture(Bukkit.getOnlinePlayers().stream()
						.map(it -> Suggestion.suggestion(it.getUniqueId().toString())).toList());
			}
		})).required("id", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
			@Override
			public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(
					@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
				return CompletableFuture.completedFuture(HellblockPlugin.getInstance().getItemManager().getItemIDs()
						.stream().map(Suggestion::suggestion).toList());
			}
		})).optional("amount", IntegerParser.integerParser(1, 6400))
				.flag(manager.flagBuilder("silent").withAliases("s").build())
				.flag(manager.flagBuilder("to-inventory").withAliases("t").build()).handler(context -> {
					final UUID uuid = UUID.fromString(context.get("uuid"));
					final Player player = Bukkit.getPlayer(uuid);
					if (player == null)
						return;
					final int amount = context.getOrDefault("amount", 1);
					final String id = context.get("id");
					boolean toInv = context.flags().hasFlag("to-inventory");
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
						handleFeedback(context, MessageConstants.COMMAND_ITEM_GIVE_SUCCESS,
								Component.text(player.getName()), Component.text(amount), Component.text(id));
					} catch (Exception e) {
						handleFeedback(context, MessageConstants.COMMAND_ITEM_FAILURE_NOT_EXIST, Component.text(id));
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "give_item_by_uuid";
	}
}