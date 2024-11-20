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
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.ChunkUtils;

import lombok.NonNull;

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
					public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(
							@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
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
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to teleport to doesn't exist!");
						return;
					}
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player's hellblock you're trying to teleport to doesn't exist!");
						return;
					}
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept((result) -> {
								UserData offlineUser = result.orElseThrow();
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
												World world = HellblockPlugin.getInstance().getHellblockHandler()
														.getHellblockWorld();
												int x = ownerUser.getHellblockData().getHellblockLocation().getBlockX();
												int z = ownerUser.getHellblockData().getHellblockLocation().getBlockZ();
												Location highest = new Location(world, x,
														world.getHighestBlockYAt(x, z), z);
												ChunkUtils.teleportAsync(player, highest, TeleportCause.PLUGIN)
														.thenRun(() -> HellblockPlugin.getInstance()
																.getAdventureManager()
																.sendMessageWithPrefix(player, String.format(
																		"<red>You've been teleported to <dark_red>%s<red>'s hellblock!",
																		ownerUser.getUUID())));
											});
									return;
								}

								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>That player doesn't have a hellblock!");
								return;
							});

				});
	}

	@Override
	public String getFeatureID() {
		return "admin_teleport";
	}
}