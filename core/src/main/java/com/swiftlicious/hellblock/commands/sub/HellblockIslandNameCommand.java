package com.swiftlicious.hellblock.commands.sub;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class HellblockIslandNameCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockIslandNameCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).optional("name", StringParser.greedyStringParser()).handler(context -> {
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

			// Must be the owner
			final UUID ownerUUID = hellblockData.getOwnerUUID();
			if (ownerUUID == null) {
				HellblockPlugin.getInstance().getPluginLogger()
						.severe("Hellblock owner UUID was null for player " + player.getName() + " ("
								+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			if (!hellblockData.isOwner(ownerUUID)) {
				handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
				return;
			}

			if (hellblockData.isAbandoned()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
				return;
			}

			final Optional<String> nameArgOpt = context.optional("name");

			// Show current name
			if (nameArgOpt.isEmpty() || "view".equalsIgnoreCase(nameArgOpt.get())) {
				final String currentName = hellblockData.getDisplaySettings().getIslandName();
				if (currentName.isBlank()) {
					handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_NAME_EMPTY);
				} else {
					handleFeedback(context,
							MessageConstants.COMMAND_HELLBLOCK_NAME_SHOW.arguments(Component.text(currentName)));
				}
				return;
			}

			final String rawName = nameArgOpt.get().trim();

			// Reset to default
			if ("reset".equalsIgnoreCase(rawName) || "clear".equalsIgnoreCase(rawName)) {
				hellblockData.getDisplaySettings().setIslandName(hellblockData.getDefaultIslandName());
				hellblockData.getDisplaySettings().setAsDefaultIslandName();
				handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_NAME_RESET_DEFAULT
						.arguments(hellblockData.displayIslandNameWithContext()));
				return;
			}

			// Validation
			final int maxLength = HellblockPlugin.getInstance().getConfigManager().maxNameCharLength();
			final List<String> bannedWords = HellblockPlugin.getInstance().getConfigManager().bannedWords();

			String formatted = AdventureHelper.getMiniMessage()
					.serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(rawName));

			String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
					.serialize(AdventureHelper.getMiniMessage().deserialize(rawName));

			// Detect meaningless names
			boolean empty = plain.trim().isEmpty() || plain.replaceAll("\\s+", "").isEmpty()
					|| !plain.matches(".*[A-Za-z0-9].*");

			if (empty) {
				handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_NAME_EMPTY);
				return;
			}

			if (plain.length() > maxLength) {
				handleFeedback(context,
						MessageConstants.COMMAND_HELLBLOCK_NAME_TOO_LONG.arguments(Component.text(maxLength)));
				return;
			}

			final Set<String> detected = HellblockIslandBioCommand.findBannedWords(plain, bannedWords);
			if (!detected.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_NAME_BANNED_WORDS
						.arguments(Component.text(String.join(", ", detected))));
				return;
			}

			// No changes?
			String current = hellblockData.getDisplaySettings().getIslandName();
			if (current.equals(formatted)) {
				handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_NAME_UNCHANGED);
				return;
			}

			// Save
			hellblockData.getDisplaySettings().setIslandName(formatted);
			hellblockData.getDisplaySettings().isNotDefaultIslandName();

			handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_NAME_SET_SUCCESS
					.arguments(hellblockData.displayIslandNameWithContext()));
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_name";
	}
}