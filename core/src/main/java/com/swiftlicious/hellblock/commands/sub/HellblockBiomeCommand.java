package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

import org.jetbrains.annotations.NotNull;

public class HellblockBiomeCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockBiomeCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("biome", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
					@Override
					public @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestionsFuture(
							@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
						List<String> suggestions = Arrays.asList(HellBiome.values()).stream().map(HellBiome::toString)
								.collect(Collectors.toList());
						return CompletableFuture
								.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
					}
				})).handler(context -> {
					final Player player = context.sender();
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}
					if (onlineUser.get().getHellblockData().hasHellblock()) {
						if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.get().getHellblockData().getOwnerUUID() != null
								&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
							return;
						}
						if (onlineUser.get().getHellblockData().isAbandoned()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
							return;
						}
						String newBiome = context.getOrDefault("biome", "NETHER_WASTES");
						if (!(Arrays.asList(HellBiome.values()).stream()
								.filter(biome -> biome.toString().equalsIgnoreCase(newBiome)).findAny().isPresent())) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_INVALID_BIOME);
							return;
						}
						HellBiome biome = HellBiome.valueOf(newBiome);
						if (onlineUser.get().getHellblockData().getBiome() == biome) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
									.arguments(Component.text(biome.getName())));
							return;
						}
						if (onlineUser.get().getHellblockData().getBiomeCooldown() > 0) {
							handleFeedback(context,
									MessageConstants.MSG_HELLBLOCK_BIOME_ON_COOLDOWN.arguments(
											Component.text(HellblockPlugin.getInstance().getFormattedCooldown(
													onlineUser.get().getHellblockData().getBiomeCooldown()))));
							return;
						}
						HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(onlineUser.get(), biome,
								false);
					} else {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_biome";
	}
}