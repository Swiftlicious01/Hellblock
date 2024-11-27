package com.swiftlicious.hellblock.commands.sub;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
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
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.ChunkUtils;

import net.kyori.adventure.text.Component;

import org.jetbrains.annotations.NotNull;

public class AdminTeleportCommand extends BukkitCommandFeature<CommandSender> {

	public AdminTeleportCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
					@Override
					public @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestionsFuture(
							@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
						List<String> suggestions = HellblockPlugin.getInstance().getStorageManager().getOnlineUsers()
								.stream()
								.filter(onlineUser -> onlineUser.isOnline()
										&& onlineUser.getHellblockData().hasHellblock())
								.map(onlineUser -> onlineUser.getName()).collect(Collectors.toList());
						return CompletableFuture
								.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
					}
				})).handler(context -> {
					final Player player = context.sender();
					String user = context.get("player");
					UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
							: UUIDFetcher.getUUID(user);
					if (id == null) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept((result) -> {
								if (result.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(user)));
									return;
								}
								UserData offlineUser = result.get();
								if (offlineUser.getHellblockData().hasHellblock()) {
									if (offlineUser.getHellblockData().getOwnerUUID() == null) {
										throw new NullPointerException(
												"Owner reference returned null, please report this to the developer.");
									}
									HellblockPlugin.getInstance().getStorageManager()
											.getOfflineUserData(offlineUser.getHellblockData().getOwnerUUID(),
													HellblockPlugin.getInstance().getConfigManager().lockData())
											.thenAccept((owner) -> {
												if (owner.isEmpty()) {
													String username = Bukkit
															.getOfflinePlayer(
																	offlineUser.getHellblockData().getOwnerUUID())
															.getName() != null
																	? Bukkit.getOfflinePlayer(offlineUser
																			.getHellblockData().getOwnerUUID())
																			.getName()
																	: "???";
													handleFeedback(context,
															MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
																	.arguments(Component.text(username)));
													return;
												}
												UserData ownerUser = owner.get();
												World world = HellblockPlugin.getInstance().getHellblockHandler()
														.getHellblockWorld();
												int x = ownerUser.getHellblockData().getHellblockLocation().getBlockX();
												int z = ownerUser.getHellblockData().getHellblockLocation().getBlockZ();
												Location highest = new Location(world, x,
														world.getHighestBlockYAt(x, z), z);
												ChunkUtils.teleportAsync(player, highest, TeleportCause.PLUGIN)
														.thenRun(() -> handleFeedback(context,
																MessageConstants.MSG_HELLBLOCK_ADMIN_FORCE_TELEPORT
																		.arguments(Component
																				.text(offlineUser.getName()))));
											});
									return;
								}

								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
								return;
							});

				});
	}

	@Override
	public String getFeatureID() {
		return "admin_teleport";
	}
}