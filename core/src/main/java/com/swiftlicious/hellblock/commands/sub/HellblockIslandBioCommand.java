package com.swiftlicious.hellblock.commands.sub;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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

public class HellblockIslandBioCommand extends BukkitCommandFeature<CommandSender> {

	private static final Map<String, Pattern> BANNED_WORD_PATTERN_CACHE = new ConcurrentHashMap<>();

	public HellblockIslandBioCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).optional("bio", StringParser.greedyStringParser()).handler(context -> {
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

			// Bio input
			final Optional<String> bioArgOpt = context.optional("bio");

			// If no argument or "view" -> show current bio
			if (bioArgOpt.isEmpty() || "view".equalsIgnoreCase(bioArgOpt.get())) {
				final String currentBio = hellblockData.getDisplaySettings().getIslandBio();
				if (currentBio.isBlank()) {
					handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_BIO_EMPTY);
				} else {
					handleFeedback(context,
							MessageConstants.COMMAND_HELLBLOCK_BIO_SHOW.arguments(Component.text(currentBio)));
				}
				return;
			}

			final String rawBio = bioArgOpt.get().trim();

			// Clear bio
			if ("clear".equalsIgnoreCase(rawBio) || "reset".equalsIgnoreCase(rawBio)) {
				hellblockData.getDisplaySettings().setIslandBio(hellblockData.getDefaultIslandBio());
				hellblockData.getDisplaySettings().setAsDefaultIslandBio();
				handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_BIO_RESET_DEFAULT
						.arguments(hellblockData.displayIslandBioWithContext()));
				return;
			}

			// Validation
			final int maxLength = HellblockPlugin.getInstance().getConfigManager().maxBioCharLength();
			final List<String> bannedWords = HellblockPlugin.getInstance().getConfigManager().bannedWords();

			// Convert literal "\n" into real newlines before formatting
			String preProcessed = rawBio.replace("\\n", "\n").trim();

			// Convert legacy color codes (&) to MiniMessage safely
			String formattedBio = AdventureHelper.getMiniMessage()
					.serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(preProcessed));

			// Plain text version for validation
			final String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
					.serialize(AdventureHelper.getMiniMessage().deserialize(preProcessed));

			int maxLines = HellblockPlugin.getInstance().getConfigManager().maxNewLines();
			if (preProcessed.split("\n").length > maxLines) {
				handleFeedback(context,
						MessageConstants.COMMAND_HELLBLOCK_BIO_TOO_MANY_LINES.arguments(Component.text(maxLines)));
				return;
			}

			// Detect meaningless bios
			final boolean emptyAfterTrim = plain.trim().isEmpty();
			final boolean hasNoVisibleChars = plain.replaceAll("\\s+", "").isEmpty();
			final boolean hasNoAlphanumeric = !plain.matches(".*[A-Za-z0-9].*");

			// Detect repetitive color codes (e.g., "&a&a&a&l&k&l")
			// We'll normalize for MiniMessage tags too
			String colorStripped = rawBio.replaceAll("(?i)&[0-9A-FK-ORX]", "") // legacy Bukkit color codes
					.replaceAll("(?i)<(/?\\w+)>", "") // MiniMessage tags like <red>, <bold>
					.replaceAll("[<>]", "") // leftover brackets
					.replaceAll("\\s+", ""); // remove spaces

			final boolean onlyFormatting = colorStripped.isEmpty();

			// Detect color code spam (e.g. more than 10 color codes in a short string)
			int colorCodeCount = countColorCodes(rawBio);
			int maxColorCode = HellblockPlugin.getInstance().getConfigManager().maxColorCodes();
			boolean excessiveColorFormatting = colorCodeCount > maxColorCode && plain.length() < maxColorCode;

			if (emptyAfterTrim || hasNoVisibleChars || hasNoAlphanumeric || onlyFormatting
					|| excessiveColorFormatting) {
				handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_BIO_EMPTY);
				return;
			}

			if (plain.length() > maxLength) {
				handleFeedback(context,
						MessageConstants.COMMAND_HELLBLOCK_BIO_TOO_LONG.arguments(Component.text(maxLength)));
				return;
			}

			// Detect banned words (advanced)
			final Set<String> detected = findBannedWords(plain, bannedWords);
			if (!detected.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_BIO_BANNED_WORDS
						.arguments(Component.text(String.join(", ", detected))));
				return;
			}

			// Detect exact same bio (including color codes and formatting)
			String currentStored = hellblockData.getDisplaySettings().getIslandBio();
			if (currentStored.equals(formattedBio)) {
				handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_BIO_UNCHANGED);
				return;
			}

			// Save the formatted bio
			hellblockData.getDisplaySettings().setIslandBio(formattedBio);
			hellblockData.getDisplaySettings().isNotDefaultIslandBio();

			handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_BIO_SET_SUCCESS
					.arguments(hellblockData.displayIslandBioWithContext()));
		});
	}

	/**
	 * Advanced banned word detection — handles leetspeak, symbols, and spacing.
	 */
	public static Set<String> findBannedWords(String input, List<String> bannedWords) {
		Set<String> found = new LinkedHashSet<>();

		if (bannedWords == null || bannedWords.isEmpty() || input == null) {
			return found;
		}

		// Step 1: Normalize input (remove accents, weird Unicode)
		String normalized = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

		// Step 2: Strip color codes (&a, <red>, etc.) and spaces
		normalized = normalized.replaceAll("(?i)&[0-9A-FK-ORX]", "") // Bukkit legacy
				.replaceAll("(?i)<[^>]+>", "") // MiniMessage tags
				.replaceAll("\\s+", ""); // Remove all spaces

		// Step 3: Replace leet/obfuscation characters
		normalized = normalized.replaceAll("(?i)@", "a").replaceAll("(?i)4", "a").replaceAll("(?i)3", "e")
				.replaceAll("(?i)1", "i").replaceAll("(?i)!+", "i").replaceAll("(?i)\\|", "i").replaceAll("(?i)0", "o")
				.replaceAll("(?i)\\$", "s").replaceAll("(?i)5", "s").replaceAll("(?i)7", "t").replaceAll("(?i)\\+", "t")
				.replaceAll("(?i)9", "g").replaceAll("(?i)8", "b").replaceAll("(?i)\\(\\)", "o")
				.replaceAll("[^a-zA-Z]", ""); // Remove everything not a letter

		// Step 4: Lowercase
		normalized = normalized.toLowerCase(Locale.ROOT);

		// Step 5: Fuzzy match banned words with cached regex patterns
		for (String banned : bannedWords) {
			if (banned == null || banned.isEmpty())
				continue;

			String cleanedBanned = banned.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", ""); // Clean banned word

			// Fetch from cache or compile and store
			Pattern pattern = BANNED_WORD_PATTERN_CACHE.computeIfAbsent(cleanedBanned, key -> {
				String fuzzyRegex = String.join(".{0,2}", key.split(""));
				return Pattern.compile("\\b" + fuzzyRegex + "\\b", Pattern.CASE_INSENSITIVE);
			});

			if (pattern.matcher(normalized).find()) {
				found.add(banned);
			}
		}

		return found;
	}

	/**
	 * Precompiles all banned word regex patterns at startup for performance.
	 * Invalid or empty entries are skipped, but errors are logged clearly.
	 */
	public static void precompileBannedWordPatterns(List<String> bannedWords) {
		if (bannedWords == null || bannedWords.isEmpty()) {
			HellblockPlugin.getInstance().getPluginLogger().info("No banned words defined to precompile.");
			return;
		}

		BANNED_WORD_PATTERN_CACHE.clear();
		int compiled = 0;
		int failed = 0;

		for (String banned : bannedWords) {
			if (banned == null || banned.isEmpty()) {
				continue;
			}

			try {
				String cleanedBanned = banned.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", ""); // Strip symbols and
																									// spaces

				if (BANNED_WORD_PATTERN_CACHE.containsKey(cleanedBanned))
					continue;

				if (cleanedBanned.isEmpty()) {
					HellblockPlugin.getInstance().getPluginLogger()
							.warn("Skipped banned word entry '" + banned + "' (no valid letters after cleanup).");
					failed++;
					continue;
				}

				// Create a fuzzy regex allowing up to 2 filler chars between letters (handles
				// obfuscation)
				String fuzzyRegex = String.join(".{0,2}", cleanedBanned.split(""));
				Pattern pattern = Pattern.compile("\\b" + fuzzyRegex + "\\b", Pattern.CASE_INSENSITIVE);

				BANNED_WORD_PATTERN_CACHE.put(cleanedBanned, pattern);
				compiled++;

			} catch (Exception e) {
				failed++;
				HellblockPlugin.getInstance().getPluginLogger()
						.severe("Failed to compile banned word '" + banned + "': " + e.getMessage());
			}
		}

		HellblockPlugin.getInstance().getPluginLogger()
				.info("Precompiled " + compiled + " banned word patterns (" + failed + " skipped or invalid).");
	}

	/**
	 * Counts the total number of color or format codes in the given input. Supports
	 * both legacy '&' codes and MiniMessage tags.
	 */
	private int countColorCodes(String input) {
		if (input == null || input.isEmpty())
			return 0;

		// Count & codes
		int legacyCount = 0;
		for (int i = 0; i < input.length() - 1; i++) {
			char c = input.charAt(i);
			if (c == '&' && i + 1 < input.length()) {
				char next = Character.toLowerCase(input.charAt(i + 1));
				if ("0123456789abcdefklmnorx".indexOf(next) != -1) {
					legacyCount++;
				}
			}
		}

		// Count MiniMessage tags like <red>, <bold>, <#FF0000>, etc.
		int miniCount = (int) input.chars().filter(ch -> ch == '<').count();

		return legacyCount + miniCount;
	}

	@Override
	public String getFeatureID() {
		return "hellblock_bio";
	}
}