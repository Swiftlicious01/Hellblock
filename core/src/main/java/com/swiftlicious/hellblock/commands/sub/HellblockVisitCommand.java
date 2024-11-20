package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;

import lombok.NonNull;

public class HellblockVisitCommand extends BukkitCommandFeature<CommandSender> {

	private final Cache<UUID, Boolean> visitCache = Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

	public HellblockVisitCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
					@Override
					public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(
							@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
						if (context.sender() instanceof Player player) {
							Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUser(player.getUniqueId());
							if (onlineUser.isEmpty())
								return CompletableFuture.completedFuture(Collections.emptyList());
							List<String> suggestions = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUsers().stream()
									.filter(user -> user != null && user.isOnline()
											&& user.getHellblockData().hasHellblock()
											&& user.getHellblockData().getHomeLocation() != null
											&& !onlineUser.get().getHellblockData().getParty().contains(user.getUUID())
											&& !user.getName().equalsIgnoreCase(onlineUser.get().getName()))
									.map(user -> user.getName()).collect(Collectors.toList());
							return CompletableFuture
									.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
						}
						return CompletableFuture.completedFuture(Collections.emptyList());
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
					String user = context.get("player");
					UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
							: UUIDFetcher.getUUID(user);
					if (id == null) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to visit doesn't exist!");
						return;
					}
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to visit doesn't exist!");
						return;
					}
					if (HellblockPlugin.getInstance().getCoopManager().trackBannedPlayer(id, player.getUniqueId())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You're banned from this hellblock island and can't visit it!");
						return;
					}
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept((result) -> {
								UserData offlineUser = result.orElseThrow();
								if (id.equals(player.getUniqueId())
										|| offlineUser.getHellblockData().getParty().contains(player.getUniqueId())) {
									if (!onlineUser.get().getHellblockData().hasHellblock()) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
												player, "<red>That player doesn't have a hellblock!");
										return;
									}
									if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
										throw new NullPointerException(
												"Owner reference returned null, please report this to the developer.");
									}
									HellblockPlugin.getInstance().getStorageManager()
											.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(),
													HellblockPlugin.getInstance().getConfigManager().lockData())
											.thenAccept((owner) -> {
												UserData ownerUser = owner.orElseThrow();
												if (ownerUser.getHellblockData().isAbandoned()) {
													HellblockPlugin.getInstance().getAdventureManager()
															.sendMessageWithPrefix(player, HellblockPlugin.getInstance()
																	.getTranslationManager().miniMessageTranslation(
																			MessageConstants.MSG_HELLBLOCK_IS_ABANDONED
																					.build().key()));
													return;
												}
												if (ownerUser.getHellblockData().getHomeLocation() != null) {
													HellblockPlugin.getInstance().getCoopManager()
															.makeHomeLocationSafe(ownerUser, onlineUser.get())
															.thenRun(() -> HellblockPlugin.getInstance()
																	.getAdventureManager().sendMessageWithPrefix(player,
																			"<red>Teleporting you to your hellblock!"));
												} else {
													HellblockPlugin.getInstance().getAdventureManager()
															.sendMessageWithPrefix(player,
																	"<red>Error teleporting you to your hellblock!");
													throw new NullPointerException(
															"Hellblock home location returned null, please report this to the developer.");
												}
											});
									return;
								}

								if (offlineUser.getHellblockData().hasHellblock()) {
									if (offlineUser.getHellblockData().getOwnerUUID() == null) {
										throw new NullPointerException(
												"Owner reference returned null, please report this to the developer.");
									}
									HellblockPlugin.getInstance().getStorageManager()
											.getOfflineUserData(offlineUser.getHellblockData().getOwnerUUID(),
													HellblockPlugin.getInstance().getConfigManager().lockData())
											.thenAccept((owner) -> {
												UserData ownerUser = owner.orElseThrow();
												if (ownerUser.getHellblockData().isAbandoned()) {
													HellblockPlugin.getInstance().getAdventureManager()
															.sendMessageWithPrefix(player,
																	"<red>This hellblock is abandoned, you can't visit it at this time!");
													return;
												}
												if (!ownerUser.getHellblockData().isLocked()
														&& HellblockPlugin.getInstance().getCoopManager()
																.checkIfVisitorIsWelcome(player, ownerUser.getUUID())) {

													if (ownerUser.getHellblockData().getHomeLocation() != null) {
														LocationUtils
																.isSafeLocationAsync(
																		ownerUser.getHellblockData().getHomeLocation())
																.thenAccept((safe) -> {
																	if (!safe.booleanValue()) {
																		HellblockPlugin.getInstance()
																				.getAdventureManager()
																				.sendMessageWithPrefix(player,
																						"<red>This hellblock is not safe to visit right now!");
																		return;
																	}
																}).thenRunAsync(() -> {
																	ChunkUtils
																			.teleportAsync(player,
																					ownerUser.getHellblockData()
																							.getHomeLocation(),
																					TeleportCause.PLUGIN)
																			.thenRun(() -> {
																				if (!visitCache.getIfPresent(
																						player.getUniqueId())) {
																					if (ownerUser.getHellblockData()
																							.getOwnerUUID() == null) {
																						throw new NullPointerException(
																								"Owner reference returned null, please report this to the developer.");
																					}
																					if (!(ownerUser.getHellblockData()
																							.getOwnerUUID()
																							.equals(player
																									.getUniqueId())
																							|| ownerUser
																									.getHellblockData()
																									.getParty()
																									.contains(player
																											.getUniqueId())
																							|| ownerUser
																									.getHellblockData()
																									.getTrusted()
																									.contains(player
																											.getUniqueId()))) {
																						ownerUser.getHellblockData()
																								.addTotalVisit();
																						visitCache.put(
																								player.getUniqueId(),
																								true);
																					}
																				}
																				HellblockPlugin.getInstance()
																						.getAdventureManager()
																						.sendMessageWithPrefix(player,
																								String.format(
																										"<red>You are visiting <dark_red>%s<red>'s hellblock!",
																										ownerUser
																												.getName()));
																			});
																	return;
																});
													} else {
														HellblockPlugin.getInstance().getAdventureManager()
																.sendMessageWithPrefix(player,
																		"<red>Error teleporting you to this hellblock!");
														throw new NullPointerException(
																"Hellblock home location returned null, please report this to the developer.");
													}
												} else {
													HellblockPlugin.getInstance().getAdventureManager()
															.sendMessageWithPrefix(player, String.format(
																	"<red>The player <dark_red>%s<red>'s hellblock is currently locked from having visitors!",
																	user));
													return;
												}
											});
								}

								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>That player doesn't have a hellblock!");
								return;
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_visit";
	}
}
