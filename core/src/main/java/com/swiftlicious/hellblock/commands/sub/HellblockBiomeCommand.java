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

import lombok.NonNull;

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
					public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(
							@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
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
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (onlineUser.get().getHellblockData().hasHellblock()) {
						if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.get().getHellblockData().getOwnerUUID() != null
								&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
											MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build().key()));
							return;
						}
						if (onlineUser.get().getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
											MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build().key()));
							return;
						}
						String newBiome = context.getOrDefault("biome", "NETHER_WASTES");
						if (!(Arrays.asList(HellBiome.values()).stream()
								.filter(biome -> biome.toString().equalsIgnoreCase(newBiome)).findAny().isPresent())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The provided input isn't a valid biome!");
							return;
						}
						HellBiome biome = HellBiome.valueOf(newBiome);
						if (onlineUser.get().getHellblockData().getBiome() == biome) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									String.format("<red>Your hellblock biome is already set to <dark_red>%s<red>!",
											biome.getName()));
							return;
						}
						HellblockPlugin.getInstance().getBiomeHandler().changeHellblockBiome(onlineUser.get(), biome);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
										MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
						return;
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_biome";
	}
}