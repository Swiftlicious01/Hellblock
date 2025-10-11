package com.swiftlicious.hellblock.commands.sub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.TextWrapUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class HellblockInfoCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockInfoCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();

			final Optional<UserData> onlineUserOpt = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());

			if (onlineUserOpt.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}

			final UserData onlineUser = onlineUserOpt.get();
			final HellblockData hellblockData = onlineUser.getHellblockData();

			if (!hellblockData.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			final UUID ownerUUID = hellblockData.getOwnerUUID();
			if (ownerUUID == null) {
				HellblockPlugin.getInstance().getPluginLogger()
						.severe("Hellblock owner UUID was null for player " + player.getName() + " ("
								+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			final HellblockPlugin plugin = HellblockPlugin.getInstance();

			plugin.getStorageManager().getOfflineUserData(ownerUUID, plugin.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							final String username = Optional.ofNullable(Bukkit.getOfflinePlayer(ownerUUID).getName())
									.orElse("???");
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
									.arguments(Component.text(username)));
							return;
						}

						final UserData offlineUser = result.get();
						final HellblockData offlineData = offlineUser.getHellblockData();

						if (offlineData.isAbandoned()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
							return;
						}

						// Build member lists
						final String partyString = formatPlayerList(offlineData.getParty());
						final String trustedString = formatPlayerList(offlineData.getTrusted());
						final String bannedString = formatPlayerList(offlineData.getBanned());

						Component bioComponent = hellblockData.displayIslandBioWithContext();
						String plainBio = PlainTextComponentSerializer.plainText().serialize(bioComponent);
						String[] bioLines = plainBio.split("\\r?\\n");

						// Extract the prefix pattern from your message template dynamically
						String bioLineTemplate = plugin.getTranslationManager()
								.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_INFORMATION.build().key());
						String bioPrefix = extractBioPrefix(bioLineTemplate);

						// Fallback if extraction fails
						if (bioPrefix == null || bioPrefix.isBlank()) {
							bioPrefix = "<gray>-</gray><reset>";
						}

						List<Component> formattedBioLines = new ArrayList<>();

						for (String line : bioLines) {
							if (line.isBlank())
								continue;

							// Wrap line text with indent support
							List<String> wrapped = TextWrapUtils.wrapLineWithIndentAdaptive(player, line.trim(), 2);

							for (int i = 0; i < wrapped.size(); i++) {
								String wrappedLine = wrapped.get(i);

								// Use dynamic prefix for first line, indent for wrapped ones
								if (i == 0) {
									formattedBioLines.add(AdventureHelper.getMiniMessage()
											.deserialize(bioPrefix + " " + wrappedLine.trim()));
								} else {
									// Indent follows same color scheme but replaces symbol with spaces
									formattedBioLines.add(AdventureHelper.getMiniMessage()
											.deserialize(bioPrefix.replaceAll("(?i)<[^>]+>|[^\\w]", " ") + " "
													+ wrappedLine.trim()));
								}
							}
						}

						// Join wrapped lines
						Component formattedBio = Component.join(JoinConfiguration.newlines(), formattedBioLines);

						// Send main info message
						handleFeedback(context,
								MessageConstants.MSG_HELLBLOCK_INFORMATION.arguments(
										Component.text(offlineData.getID()),
										Component
												.text(Bukkit.getOfflinePlayer(offlineUser.getUUID()).hasPlayedBefore()
														? offlineUser.getName()
														: plugin.getTranslationManager().miniMessageTranslation(
																MessageConstants.FORMAT_UNKNOWN.build().key())),
										Component.text(offlineData.getLevel()),
										Component.text(offlineData.getCreationTime()),
										offlineData.displayIslandNameWithContext(), formattedBio,
										Component.text(offlineData.getMaxProtectionRange()),
										Component.text(offlineData.isLocked()
												? plugin.getTranslationManager().miniMessageTranslation(
														MessageConstants.FORMAT_CLOSED.build().key())
												: plugin.getTranslationManager().miniMessageTranslation(
														MessageConstants.FORMAT_OPEN.build().key())),
										Component.text(offlineData.getVisitData().getDailyVisits()),
										Component.text(offlineData.getVisitData().getWeeklyVisits()),
										Component.text(offlineData.getVisitData().getMonthlyVisits()),
										Component.text(offlineData.getVisitData().getTotalVisits()),
										Component.text(plugin.getHopperHandler().getHopperCount(offlineUser.getUUID())),
										Component.text(offlineData.getMaxHopperLimit()),
										Component.text(
												StringUtils.toProperCase(offlineData.getIslandChoice().toString())),
										Component.text(offlineData.getBiome().getName()),
										Component.text(offlineData.getParty().size()),
										Component.text(plugin.getCoopManager().getMaxPartySize(offlineUser)),
										Component.text(partyString), Component.text(trustedString),
										Component.text(bannedString)));
					}).exceptionally(ex -> {
						HellblockPlugin.getInstance().getPluginLogger()
								.warn("getOfflineUserData failed for hellblock information check of " + player.getName()
										+ ": " + ex.getMessage());
						return null;
					});
		});
	}

	/**
	 * Utility method to format a list of UUIDs into a translated, comma-separated
	 * string.
	 */
	private String formatPlayerList(Collection<UUID> uuids) {
		if (uuids.isEmpty()) {
			return HellblockPlugin.getInstance().getTranslationManager()
					.miniMessageTranslation(MessageConstants.FORMAT_NONE.build().key());
		}

		return uuids.stream().map(Bukkit::getOfflinePlayer).filter(p -> p.hasPlayedBefore() && p.getName() != null)
				.map(p -> HellblockPlugin.getInstance().getTranslationManager()
						.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_INFO_LIST_STYLE
								.arguments(Component.text(p.getName())).build().key()))
				.collect(Collectors.joining(", ")); // cleanly handles commas
	}

	/**
	 * Extracts the prefix (e.g. "<gray>-</gray>") from the YAML line that contains
	 * <arg:5>. Returns null if not found.
	 */
	private @Nullable String extractBioPrefix(String rawTemplateList) {
		if (rawTemplateList == null || rawTemplateList.isBlank()) {
			return null;
		}

		// Find the line that contains <arg:4>
		for (String line : rawTemplateList.split("\n")) {
			if (line.contains("<arg:5>")) {
				// Strip <arg:4> and trailing spaces
				return line.replace("<arg:5>", "").trim();
			}
		}
		return null;
	}

	@Override
	public String getFeatureID() {
		return "hellblock_info";
	}
}