package com.swiftlicious.hellblock.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.DefaultFontInfo;
import com.swiftlicious.hellblock.sender.Sender;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/*	
 * Helper class for handling Adventure components and related functionalities.
 */
public class AdventureHelper {

	private static final int CENTER_PX = 154;

	// We no longer store MiniMessage/etc. directly – only the bridge proxy
	private final LoaderBridge bridge;

	private final Cache<String, String> miniMessageToJsonCache = Caffeine.newBuilder()
			.expireAfterWrite(5, TimeUnit.MINUTES).build();

	public static boolean legacySupport = false;

	private AdventureHelper(@NotNull LoaderBridge bridge) {
		this.bridge = bridge;
	}

	private static class SingletonHolder {
		private static AdventureHelper INSTANCE;
	}

	/**
	 * Retrieves the singleton instance of AdventureHelper.
	 *
	 * @return the singleton instance
	 */
	@NotNull
	public static AdventureHelper getInstance() {
		if (SingletonHolder.INSTANCE == null) {
			HellblockPlugin plugin = HellblockPlugin.getInstance();
			boolean legacyHover = !VersionHelper.isVersionNewerThan1_20_5();
			boolean camelCase = !VersionHelper.isVersionNewerThan1_21_5();

			SingletonHolder.INSTANCE = plugin.getDependencyManager()
					.runWithLoader(AdventureDependencyHelper.ADVENTURE_DEPENDENCIES, () -> {
						try {
							// Directly instantiate LoaderBridge INSIDE the isolated loader
							return new AdventureHelper(LoaderBridge.getBridgeConnection(legacyHover, camelCase));
						} catch (Exception ex) {
							throw new RuntimeException("Failed to initialize AdventureHelper bridge", ex);
						}
					});
		}
		return SingletonHolder.INSTANCE;
	}

	public static void resetInstance() {
		SingletonHolder.INSTANCE = null;
	}

	/**
	 * Converts a MiniMessage string to a Component.
	 *
	 * @param text the MiniMessage string
	 * @return the resulting Component
	 */
	@NotNull
	public static Component miniMessageToComponent(@NotNull String text) {
		return legacySupport ? getInstance().bridge.miniMessageToComponent(legacyToMiniMessage(text))
				: getInstance().bridge.miniMessageToComponent(text);
	}

	/**
	 * Converts a Component to a MiniMessage String.
	 *
	 * @param component the Component
	 * @return the MiniMessage string representation
	 */
	@NotNull
	public static String componentToMiniMessage(@NotNull Component component) {
		return getInstance().bridge.componentToMiniMessage(component);
	}

	/**
	 * Retrieves the MiniMessage instance.
	 *
	 * @return the MiniMessage instance
	 */
	@NotNull
	public static net.kyori.adventure.text.minimessage.MiniMessage getMiniMessage() {
		return getInstance().bridge.miniMessage;
	}

	/**
	 * Retrieves the LegacyComponentSerializer instance.
	 *
	 * @return the LegacyComponentSerializer instance
	 */
	@NotNull
	public static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer getLegacyComponentSerializer() {
		return getInstance().bridge.legacy;
	}

	/**
	 * Retrieves the PlainTextComponentSerializer instance.
	 *
	 * @return the PlainTextComponentSerializer instance
	 */
	@NotNull
	public static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer getPlainTextComponentSerializer() {
		return getInstance().bridge.plain;
	}

	/**
	 * Retrieves the GsonComponentSerializer instance.
	 *
	 * @return the GsonComponentSerializer instance
	 */
	@NotNull
	public static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer getGsonComponentSerializer() {
		return getInstance().bridge.gson;
	}

	/**
	 * Converts a MiniMessage string to a JSON string.
	 *
	 * @param miniMessage the MiniMessage string
	 * @return the JSON string representation
	 */
	@NotNull
	public static String miniMessageToJson(@NotNull String miniMessage) {
		String trimmed = miniMessage.trim();

		// Detect if the user explicitly used italics
		// MiniMessage supports <i>, <italic>, </i>, and </italic>
		if (!trimmed.toLowerCase(Locale.ENGLISH).contains("<i")
				&& !trimmed.toLowerCase(Locale.ENGLISH).contains("<italic")) {
			// Prepend <!italic> to disable italics by default
			miniMessage = "<!italic>" + miniMessage;
		}

		return getInstance().miniMessageToJsonCache.get(miniMessage, t -> componentToJson(miniMessageToComponent(t)));
	}

	/**
	 * Sends a centered message to an audience.
	 *
	 * @param audience the player to send the message to
	 * @param message  the message component
	 */
	public static void sendCenteredMessage(@NotNull Sender audience, @NotNull Component message) {
		if (message.children().contains(Component.newline())) {
			final net.kyori.adventure.text.format.Style parentStyle = message.style();
			final List<Component> children = new ArrayList<>(message.children());
			children.add(0, message.children(new ArrayList<>()));
			Component toSend = Component.empty().style(parentStyle);
			for (int i = 0; i < children.size(); i++) {
				final Component child = children.get(i);
				if (child.equals(Component.newline())) {
					sendCenteredMessage(audience, toSend);
					if (i == children.size() - 1) {
						break;
					}
					toSend = children.get(i + 1).applyFallbackStyle(parentStyle);
					i++;
				} else {
					toSend = toSend.append(child);
				}
			}
			sendCenteredMessage(audience, toSend);
			return;
		}
		final String msg = componentToJson(message);
		int messagePxSize = 0;
		boolean previousCode = false;
		boolean isBold = false;
		for (char c : msg.toCharArray()) {
			if (isLegacyColorCode(c)) {
				previousCode = true;
			} else if (previousCode) {
				previousCode = false;
				isBold = c == 'l' || c == 'L';
			} else {
				final DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
				messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
				messagePxSize++;
			}
		}
		final int halvedMessageSize = messagePxSize / 2;
		final int toCompensate = CENTER_PX - halvedMessageSize;
		final int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
		int compensated = 0;
		final StringBuilder sb = new StringBuilder();
		while (compensated < toCompensate) {
			sb.append(" ");
			compensated += spaceLength;
		}
		audience.sendMessage(miniMessageToComponent(sb.toString()).append(message));
	}

	/**
	 * Plays a sound for an audience.
	 *
	 * @param audience the audience to play the sound for
	 * @param sound    the sound to play
	 */
	public static void playSound(@NotNull net.kyori.adventure.audience.Audience audience,
			@NotNull net.kyori.adventure.sound.Sound sound) {
		audience.playSound(sound);
	}

	/**
	 * Plays a sound for an audience at specific coordinates.
	 *
	 * @param audience the audience to play the sound for
	 * @param sound    the sound to play
	 * @param x        the x-coordinate
	 * @param y        the y-coordinate
	 * @param z        the z-coordinate
	 */
	public static void playSound(@NotNull net.kyori.adventure.audience.Audience audience,
			@NotNull net.kyori.adventure.sound.Sound sound, double x, double y, double z) {
		audience.playSound(sound, x, y, z);
	}

	/**
	 * Plays a positional sound in a world at a specific location.
	 *
	 * @param world    the world to play the sound in
	 * @param location the location to play the sound at
	 * @param sound    the sound to play
	 */
	public static void playPositionalSound(@NotNull World world, @NotNull Location loc,
			@NotNull net.kyori.adventure.sound.Sound sound) {
		double half = HellblockPlugin.getInstance().getConfigManager().searchRadius() / 2.0;
		world.getNearbyEntities(loc, half, half, half).stream().filter(Player.class::isInstance).map(Player.class::cast)
				.map(HellblockPlugin.getInstance().getSenderFactory()::getAudience)
				.forEach(audience -> playSound(audience, sound, loc.getX(), loc.getY(), loc.getZ()));
	}

	/**
	 * Surrounds text with a MiniMessage font tag.
	 *
	 * @param text the text to surround
	 * @param font the font as a {@link Key}
	 * @return the text surrounded by the MiniMessage font tag
	 */
	@NotNull
	public static String surroundWithMiniMessageFont(@NotNull String text, @NotNull net.kyori.adventure.key.Key font) {
		return "<font:" + font.asString() + ">" + text + "</font>";
	}

	/**
	 * Surrounds text with a MiniMessage font tag.
	 *
	 * @param text the text to surround
	 * @param font the font as a {@link String}
	 * @return the text surrounded by the MiniMessage font tag
	 */
	@NotNull
	public static String surroundWithMiniMessageFont(@NotNull String text, @NotNull String font) {
		return "<font:" + font + ">" + text + "</font>";
	}

	/**
	 * Converts a JSON string to a MiniMessage string.
	 *
	 * @param json the JSON string
	 * @return the MiniMessage string representation
	 */
	@NotNull
	public static String jsonToMiniMessage(@NotNull String json) {
		return getInstance().bridge.miniMessageStrict.serialize(jsonToComponent(json));
	}

	/**
	 * Converts a JSON string to a Component.
	 *
	 * @param json the JSON string
	 * @return the resulting Component
	 */
	@NotNull
	public static Component jsonToComponent(@NotNull String json) {
		return getInstance().bridge.jsonToComponent(json);
	}

	/**
	 * Converts a Component to a JSON string.
	 *
	 * @param component the Component to convert
	 * @return the JSON string representation
	 */
	@NotNull
	public static String componentToJson(@NotNull Component component) {
		return getInstance().bridge.componentToJson(component);
	}

	/**
	 * Converts plain text to a Component.
	 *
	 * @param plainText the plain text
	 * @return the resulting Component
	 */
	@NotNull
	public static Component plainTextToComponent(@NotNull String plainText) {
		return getInstance().bridge.plainTextToComponent(plainText);
	}

	/**
	 * Converts a Component to a plain text.
	 *
	 * @param component the Component to convert
	 * @return the plain text representation
	 */
	@NotNull
	public static String componentToPlainText(@NotNull Component component) {
		return getInstance().bridge.componentToPlainText(component);
	}

	/**
	 * Converts a Legacy string to a Component.
	 *
	 * @param legacy The Legacy string
	 * @return the resulting Component
	 */
	@NotNull
	public static Component legacyToComponent(@NotNull String legacy) {
		return getInstance().bridge.legacyToComponent(legacy);
	}

	/**
	 * Converts a Component to a Legacy string.
	 *
	 * @param component the Component to convert
	 * @return the Legacy string representation
	 */
	@NotNull
	public static String componentToLegacy(@NotNull Component component) {
		return getInstance().bridge.componentToLegacy(component);
	}

	/**
	 * Checks if a component is empty.
	 *
	 * @param component the Component to check
	 * @return if empty or not
	 */
	public static boolean isEmpty(@NotNull Component component) {
		String plain = componentToPlainText(component);
		return plain.trim().isEmpty();
	}

	/**
	 * Parses a multiline MiniMessage string (with optional &lt;center&gt; tags) into a
	 * visually centered Adventure Component suitable for GUI titles or inventory
	 * menus.
	 *
	 * <p>
	 * This implementation estimates text centering using character-domain
	 * heuristics (not pixel width) because Minecraft GUI title centering is
	 * font-approximate rather than strictly pixel-perfect. It dynamically adjusts
	 * padding based on text length, color, gradient, bold, and italic effects to
	 * produce consistent centering across short, medium, and long titles.
	 * </p>
	 *
	 * <p>
	 * Supports:
	 * <ul>
	 * <li>{@code <center>} — Automatically centers text line.</li>
	 * <li>{@code <center:offset=±N>} — Adds manual fine-tuning offset in
	 * spaces.</li>
	 * <li>Nested MiniMessage tags like {@code <gradient>}, {@code <b>},
	 * {@code <i>}, and colors.</li>
	 * </ul>
	 * </p>
	 *
	 * @param raw the raw MiniMessage input text (may contain multiple lines)
	 * @return a fully formatted, approximately centered Adventure Component
	 */
	@NotNull
	public static Component parseCenteredTitleMultiline(@NotNull String raw) {
		String[] lines = raw.split("\n");
		List<Component> components = new ArrayList<>();

		for (String line : lines) {
			String trimmed = line.trim();
			boolean isCentered = trimmed.toLowerCase(Locale.ROOT).contains("<center");
			int manualOffset = 0;

			if (isCentered) {
				Matcher matcher = Pattern.compile("(?i)<center:offset=(-?\\d+)>").matcher(trimmed);
				if (matcher.find()) {
					manualOffset = Integer.parseInt(matcher.group(1));
				}
			}

			String inner = trimmed.replaceAll("(?i)</?center(:offset=-?\\d+)?>", "").trim();
			Component component = miniMessageToComponent(inner);

			if (isCentered) {
				String plain = stripFormattingTags(inner);
				int charCount = plain.length();

				// Detect formatting that affects visual balance
				boolean hasGradient = inner.toLowerCase(Locale.ROOT).contains("<gradient");
				boolean hasBold = inner.toLowerCase(Locale.ROOT).contains("<b")
						|| inner.toLowerCase(Locale.ROOT).contains("<bold");
				boolean hasItalic = inner.toLowerCase(Locale.ROOT).contains("<i")
						|| inner.toLowerCase(Locale.ROOT).contains("<italic");
				boolean hasColor = inner.toLowerCase(Locale.ROOT).matches(".*<#[a-f0-9]{6}>.*|.*<color:.*>.*");

				// --- Base width estimation ---
				double visualWidth = charCount;
				if (hasGradient)
					visualWidth *= 1.05;
				if (hasColor)
					visualWidth *= 1.02;
				if (hasBold)
					visualWidth *= 1.15;
				if (hasItalic)
					visualWidth *= 0.97;

				// --- Adaptive normalization ---
				// GUI width domain grows slightly with title length (Minecraft font quirk)
				double normalizedDomain = 34.5 + (visualWidth * 0.06);

				// --- Bias correction ---
				// Prevents short titles from leaning left and long ones from overflowing right
				double bias = (Math.pow(visualWidth / 18.0, 1.3) - 1.0) * -2.5;

				// --- Final padding ---
				int padSpaces = (int) Math.max(0,
						Math.round(((normalizedDomain - visualWidth) / 2.0) + bias + manualOffset));

				Component padding = Component.text(" ".repeat(padSpaces));
				components.add(padding.append(component));
			} else {
				components.add(component);
			}
		}

		Component result = Component.empty();
		for (int i = 0; i < components.size(); i++) {
			result = result.append(components.get(i));
			if (i != components.size() - 1)
				result = result.append(Component.newline());
		}

		return result;
	}

	/**
	 * Strips MiniMessage formatting tags from a string for width approximation.
	 * This allows centering calculations to consider only visible characters while
	 * ignoring visual formatting markup (like gradients, colors, or style tags).
	 *
	 * <p>
	 * This method removes:
	 * <ul>
	 * <li>Gradient and rainbow tags ({@code <gradient>, <rainbow>})</li>
	 * <li>Hex color codes ({@code <#RRGGBB>}) and named colors
	 * ({@code <color:red>})</li>
	 * <li>Bold/italic tags ({@code <b>, <bold>, <i>, <italic>})</li>
	 * <li>Center tags themselves ({@code <center>})</li>
	 * </ul>
	 * </p>
	 *
	 * @param input raw MiniMessage-style string
	 * @return plain-text version of the input, safe for approximate width
	 *         computation
	 */
	@NotNull
	private static String stripFormattingTags(@NotNull String input) {
		return input.replaceAll("(?i)</?gradient(:[^>]+)?>", "").replaceAll("(?i)</?rainbow(:[^>]+)?>", "")
				.replaceAll("(?i)<#[A-F0-9]{6}>", "").replaceAll("(?i)<color:[^>]+>", "")
				.replaceAll("(?i)</?b(>|$)|</?bold(>|$)|</?i(>|$)|</?italic(>|$)", "")
				.replaceAll("(?i)</?center(:offset=-?\\d+)?>", "").replaceAll("\\s+", " ").trim();
	}

	/**
	 * Checks if a character is a legacy color code.
	 *
	 * @param c the character to check
	 * @return true if the character is a color code, false otherwise
	 */
	public static boolean isLegacyColorCode(char c) {
		return c == '§' || c == '&';
	}

	/**
	 * Converts a legacy color code string to a MiniMessage string.
	 *
	 * @param legacy the legacy color code string
	 * @return the MiniMessage string representation
	 */
	@NotNull
	public static String legacyToMiniMessage(@NotNull String legacy) {
		final StringBuilder stringBuilder = new StringBuilder();
		final char[] chars = legacy.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (!isLegacyColorCode(chars[i])) {
				stringBuilder.append(chars[i]);
				continue;
			}
			if (i + 1 >= chars.length) {
				stringBuilder.append(chars[i]);
				continue;
			}
			switch (chars[i + 1]) {
			case '0' -> stringBuilder.append("<black>");
			case '1' -> stringBuilder.append("<dark_blue>");
			case '2' -> stringBuilder.append("<dark_green>");
			case '3' -> stringBuilder.append("<dark_aqua>");
			case '4' -> stringBuilder.append("<dark_red>");
			case '5' -> stringBuilder.append("<dark_purple>");
			case '6' -> stringBuilder.append("<gold>");
			case '7' -> stringBuilder.append("<gray>");
			case '8' -> stringBuilder.append("<dark_gray>");
			case '9' -> stringBuilder.append("<blue>");
			case 'a' -> stringBuilder.append("<green>");
			case 'b' -> stringBuilder.append("<aqua>");
			case 'c' -> stringBuilder.append("<red>");
			case 'd' -> stringBuilder.append("<light_purple>");
			case 'e' -> stringBuilder.append("<yellow>");
			case 'f' -> stringBuilder.append("<white>");
			case 'r' -> stringBuilder.append("<reset><!i>");
			case 'l' -> stringBuilder.append("<b>");
			case 'm' -> stringBuilder.append("<st>");
			case 'o' -> stringBuilder.append("<i>");
			case 'n' -> stringBuilder.append("<u>");
			case 'k' -> stringBuilder.append("<obf>");
			case 'x' -> {
				if (i + 13 >= chars.length || !isLegacyColorCode(chars[i + 2]) || !isLegacyColorCode(chars[i + 4])
						|| !isLegacyColorCode(chars[i + 6]) || !isLegacyColorCode(chars[i + 8])
						|| !isLegacyColorCode(chars[i + 10]) || !isLegacyColorCode(chars[i + 12])) {
					stringBuilder.append(chars[i]);
					continue;
				}
				stringBuilder.append("<#").append(chars[i + 3]).append(chars[i + 5]).append(chars[i + 7])
						.append(chars[i + 9]).append(chars[i + 11]).append(chars[i + 13]).append(">");
				i += 12;
			}
			default -> {
				stringBuilder.append(chars[i]);
				continue;
			}
			}
			i++;
		}
		return stringBuilder.toString();
	}

	public static class LoaderBridge {
		private static LoaderBridge INSTANCE;

		@NotNull
		public static LoaderBridge getBridgeConnection(boolean legacyHover, boolean camelCase) {
			if (INSTANCE == null)
				INSTANCE = new LoaderBridge(legacyHover, camelCase);
			return INSTANCE;
		}

		private final net.kyori.adventure.text.minimessage.MiniMessage miniMessage;
		private final net.kyori.adventure.text.minimessage.MiniMessage miniMessageStrict;
		private final net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer plain;
		private final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer legacy;
		private final net.kyori.adventure.text.serializer.gson.GsonComponentSerializer gson;

		public LoaderBridge(boolean legacyHover, boolean camelCase) {
			net.kyori.adventure.text.minimessage.MiniMessage.Builder mini = net.kyori.adventure.text.minimessage.MiniMessage
					.builder();
			this.miniMessage = mini.build();
			this.miniMessageStrict = mini.strict(true).build();

			this.plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();
			this.legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand();

			net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.Builder gsonBuilder = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
					.builder();

			if (legacyHover) {
				gsonBuilder.legacyHoverEventSerializer(
						net.kyori.adventure.text.serializer.json.legacyimpl.NBTLegacyHoverEventSerializer.get());
				gsonBuilder.editOptions(b -> b.value(
						net.kyori.adventure.text.serializer.json.JSONOptions.EMIT_HOVER_SHOW_ENTITY_ID_AS_INT_ARRAY,
						false));
			}

			if (camelCase) {
				gsonBuilder.editOptions(b -> {
					b.value(net.kyori.adventure.text.serializer.json.JSONOptions.EMIT_CLICK_EVENT_TYPE,
							net.kyori.adventure.text.serializer.json.JSONOptions.ClickEventValueMode.CAMEL_CASE);
					b.value(net.kyori.adventure.text.serializer.json.JSONOptions.EMIT_HOVER_EVENT_TYPE,
							net.kyori.adventure.text.serializer.json.JSONOptions.HoverEventValueMode.CAMEL_CASE);
					b.value(net.kyori.adventure.text.serializer.json.JSONOptions.EMIT_HOVER_SHOW_ENTITY_KEY_AS_TYPE_AND_UUID_AS_ID,
							true);
				});
			}

			this.gson = gsonBuilder.build();
		}

		@NotNull
		public Component miniMessageToComponent(@NotNull String text) {
			return this.miniMessage.deserializeOr(text, Component.empty());
		}

		@NotNull
		public String componentToMiniMessage(@NotNull Component component) {
			return this.miniMessage.serialize(component);
		}

		@NotNull
		public Component jsonToComponent(@NotNull String json) {
			return this.gson.deserializeOr(json, Component.empty());
		}

		@NotNull
		public String componentToJson(@NotNull Component component) {
			return this.gson.serialize(component);
		}

		@NotNull
		public Component plainTextToComponent(@NotNull String plainText) {
			return this.plain.deserializeOr(plainText, Component.empty());
		}

		@NotNull
		public String componentToPlainText(@NotNull Component component) {
			return this.plain.serialize(component);
		}

		@NotNull
		public Component legacyToComponent(@NotNull String legacy) {
			return this.legacy.deserializeOr(legacy, Component.empty());
		}

		@NotNull
		public String componentToLegacy(@NotNull Component component) {
			return this.legacy.serialize(component);
		}
	}
}