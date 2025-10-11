package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class AdminForceBiomeCommand extends BukkitCommandFeature<CommandSender> {

	public AdminForceBiomeCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).required("player", StringParser.stringComponent())
				.required("biome",
						StringParser.stringComponent()
								.suggestionProvider((context,
										input) -> CompletableFuture.completedFuture(Arrays.stream(HellBiome.values())
												.map(Enum::toString).map(Suggestion::suggestion).toList())))
				.handler(context -> {
					final String userArg = context.get("player");
					final String biomeInput = context.getOrDefault("biome", "NETHER_WASTES").toUpperCase(Locale.ROOT);

					// Resolve UUID
					final UUID id = Bukkit.getPlayer(userArg) != null ? Bukkit.getPlayer(userArg).getUniqueId()
							: UUIDFetcher.getUUID(userArg);

					if (id == null || !Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(userArg)));
									return;
								}

								final UserData targetUser = result.get();
								final HellblockData data = targetUser.getHellblockData();

								if (!data.hasHellblock()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
									return;
								}

								final UUID ownerUUID = data.getOwnerUUID();
								if (ownerUUID == null) {
									HellblockPlugin.getInstance().getPluginLogger()
											.severe("Hellblock owner UUID was null for player " + targetUser.getName()
													+ " (" + targetUser.getUUID()
													+ "). This indicates corrupted data.");
									throw new IllegalStateException(
											"Owner reference was null. This should never happen — please report to the developer.");
								}

								HellblockPlugin.getInstance().getStorageManager()
										.getOfflineUserData(ownerUUID,
												HellblockPlugin.getInstance().getConfigManager().lockData())
										.thenAccept(ownerOpt -> {
											if (ownerOpt.isEmpty()) {
												final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
												handleFeedback(context,
														MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
																.arguments(Component
																		.text(username != null ? username : "???")));
												return;
											}

											final UserData ownerData = ownerOpt.get();

											if (ownerData.getHellblockData().isAbandoned()) {
												handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
												return;
											}

											final Optional<HellBiome> biomeOpt = Arrays.stream(HellBiome.values())
													.filter(b -> b.name().equalsIgnoreCase(biomeInput)).findFirst();

											if (biomeOpt.isEmpty()) {
												handleFeedback(context, MessageConstants.MSG_HELLBLOCK_INVALID_BIOME
														.arguments(Component.text(biomeInput)));
												return;
											}

											final HellBiome biome = biomeOpt.get();

											if (ownerData.getHellblockData().getBiome() == biome) {
												handleFeedback(context, MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
														.arguments(Component.text(biome.getName())));
												return;
											}

											HellblockPlugin.getInstance().getBiomeHandler()
													.changeHellblockBiome(ownerData, biome, false, true);
											handleFeedback(context,
													MessageConstants.MSG_HELLBLOCK_ADMIN_FORCE_BIOME.arguments(
															Component.text(targetUser.getName()),
															Component.text(biome.getName())));
										});
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_forcebiome";
	}
}
