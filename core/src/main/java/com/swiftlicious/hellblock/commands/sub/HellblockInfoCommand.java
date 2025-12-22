package com.swiftlicious.hellblock.commands.sub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

public class HellblockInfoCommand extends BukkitCommandFeature<CommandSender> {

	private static final int DEFAULT_LIST_CUTOFF = 4;

	public HellblockInfoCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();

			final Optional<UserData> onlineUserOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

			if (onlineUserOpt.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}

			final UserData userData = onlineUserOpt.get();
			final HellblockData data = userData.getHellblockData();

			if (!data.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			final UUID ownerUUID = data.getOwnerUUID();
			if (ownerUUID == null) {
				plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
						+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false).thenAccept(optOwnerData -> {
				if (optOwnerData.isEmpty()) {
					final String username = Optional.ofNullable(Bukkit.getOfflinePlayer(ownerUUID).getName())
							.orElse(plugin.getTranslationManager()
									.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key()));
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
							AdventureHelper.miniMessageToComponent(username));
					return;
				}

				final UserData ownerData = optOwnerData.get();
				final HellblockData hellblockData = ownerData.getHellblockData();

				if (hellblockData.isAbandoned()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
					return;
				}

				// Build member lists
				final String partyString = formatPlayerList(hellblockData.getPartyMembers(), DEFAULT_LIST_CUTOFF);
				final String trustedString = formatPlayerList(hellblockData.getTrustedMembers(), DEFAULT_LIST_CUTOFF);
				final String bannedString = formatPlayerList(hellblockData.getBannedMembers(), DEFAULT_LIST_CUTOFF);

				Component bioComponent = hellblockData.displayIslandBioWithContext();
				String plainBio = AdventureHelper.componentToPlainText(bioComponent);
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
							formattedBioLines
									.add(AdventureHelper.miniMessageToComponent(bioPrefix + " " + wrappedLine.trim()));
						} else {
							// Indent follows same color scheme but replaces symbol with spaces
							formattedBioLines.add(AdventureHelper.miniMessageToComponent(
									bioPrefix.replaceAll("(?i)<[^>]+>|[^\\w]", " ") + " " + wrappedLine.trim()));
						}
					}
				}

				// Join wrapped lines
				Component formattedBio = Component.join(JoinConfiguration.newlines(), formattedBioLines);

				// Send main info message
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_INFORMATION,
						AdventureHelper.miniMessageToComponent(String.valueOf(hellblockData.getIslandId())),
						AdventureHelper.miniMessageToComponent(
								Bukkit.getOfflinePlayer(ownerData.getUUID()).hasPlayedBefore() ? ownerData.getName()
										: plugin.getTranslationManager()
												.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key())),
						AdventureHelper.miniMessageToComponent(String.valueOf(hellblockData.getIslandLevel())),
						AdventureHelper.miniMessageToComponent(hellblockData.getCreationTimeFormatted()),
						hellblockData.displayIslandNameWithContext(), formattedBio,
						AdventureHelper.miniMessageToComponent(String.valueOf(hellblockData.getMaxProtectionRange())),
						AdventureHelper.miniMessageToComponent(hellblockData.isLocked()
								? plugin.getTranslationManager()
										.miniMessageTranslation(MessageConstants.FORMAT_CLOSED.build().key())
								: plugin.getTranslationManager()
										.miniMessageTranslation(MessageConstants.FORMAT_OPEN.build().key())),
						AdventureHelper
								.miniMessageToComponent(String.valueOf(hellblockData.getVisitData().getDailyVisits())),
						AdventureHelper
								.miniMessageToComponent(String.valueOf(hellblockData.getVisitData().getWeeklyVisits())),
						AdventureHelper.miniMessageToComponent(
								String.valueOf(hellblockData.getVisitData().getMonthlyVisits())),
						AdventureHelper
								.miniMessageToComponent(String.valueOf(hellblockData.getVisitData().getTotalVisits())),
						AdventureHelper.miniMessageToComponent(hellblockData.getBoundingBox() != null
								? String.valueOf(plugin.getHopperHandler().countHoppers(hellblockData.getBoundingBox()))
								: String.valueOf("0")),
						AdventureHelper.miniMessageToComponent(String.valueOf(hellblockData.getMaxHopperLimit())),
						AdventureHelper.miniMessageToComponent(hellblockData.getIslandChoice() != null
								? StringUtils.toCamelCase(hellblockData.getIslandChoice().toString())
								: plugin.getTranslationManager()
										.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key())),
						AdventureHelper.miniMessageToComponent(hellblockData.getBiome() != null
								? StringUtils.toCamelCase(hellblockData.getBiome().toString())
								: plugin.getTranslationManager()
										.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key())),
						AdventureHelper.miniMessageToComponent(String.valueOf(hellblockData.getPartyMembers().size())),
						AdventureHelper.miniMessageToComponent(
								String.valueOf(plugin.getCoopManager().getMaxPartySize(ownerData))),
						AdventureHelper.miniMessageToComponent(partyString),
						AdventureHelper.miniMessageToComponent(trustedString),
						AdventureHelper.miniMessageToComponent(bannedString));
			}).exceptionally(ex -> {
				plugin.getPluginLogger().warn("getCachedUserDataWithFallback failed for hellblock information check of "
						+ player.getName() + ": " + ex.getMessage());
				return null;
			});
		});
	}

	/**
	 * Utility method to format a list of UUIDs into a translated, comma-separated
	 * string.
	 */
	@NotNull
	private String formatPlayerList(@NotNull Collection<UUID> uuids, int cutoff) {
		if (uuids.isEmpty()) {
			return plugin.getTranslationManager().miniMessageTranslation(MessageConstants.FORMAT_NONE.build().key());
		}

		List<OfflinePlayer> players = uuids.stream().map(Bukkit::getOfflinePlayer)
				.filter(p -> p.hasPlayedBefore() && p.getName() != null).toList();

		List<String> output = new ArrayList<>();
		int displayedCount = Math.min(players.size(), cutoff);

		for (int i = 0; i < displayedCount; i++) {
			String styled = plugin.getTranslationManager()
					.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_INFO_LIST_STYLE
							.arguments(AdventureHelper.miniMessageToComponent(players.get(i).getName())).build().key());
			output.add(styled);
		}

		// If cutoff applies, append the "...and more" at the end
		if (players.size() > cutoff) {
			int remaining = players.size() - cutoff;
			String cutoffMessage = plugin.getTranslationManager()
					.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_INFO_LIST_CUTOFF
							.arguments(AdventureHelper.miniMessageToComponent(String.valueOf(remaining))).build()
							.key());
			output.add(cutoffMessage);
		}

		String separator = plugin.getTranslationManager()
				.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_INFO_LIST_SEPARATOR.build().key());

		// Do NOT join with ", " — just concatenate MiniMessage strings
		return String.join(separator, output); // Already contains commas from the style key
	}

	/**
	 * Extracts the prefix (e.g. "<gray>-</gray>") from the YAML line that contains
	 * <arg:5>. Returns null if not found.
	 */
	@Nullable
	private String extractBioPrefix(@Nullable String rawTemplateList) {
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