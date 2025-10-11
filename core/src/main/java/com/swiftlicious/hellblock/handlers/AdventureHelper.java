package com.swiftlicious.hellblock.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.DefaultFontInfo;
import com.swiftlicious.hellblock.sender.Sender;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.kyori.adventure.text.serializer.json.legacyimpl.NBTLegacyHoverEventSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Helper class for handling Adventure components and related functionalities.
 */
public class AdventureHelper {

	private static final int CENTER_PX = 154;

	private final MiniMessage miniMessage;
	private final MiniMessage miniMessageStrict;
	private final GsonComponentSerializer gsonComponentSerializer;
	private final PlainTextComponentSerializer plainTextComponentSerializer;
	private final Cache<String, String> miniMessageToJsonCache = Caffeine.newBuilder()
			.expireAfterWrite(5, TimeUnit.MINUTES).build();
	public static boolean legacySupport = false;

	private static final int MAX_PIXEL_WIDTH = 154; // GUI title line width (in pixels)

	public AdventureHelper() {
		this.miniMessage = MiniMessage.builder().build();
		this.miniMessageStrict = MiniMessage.builder().strict(true).build();
		this.plainTextComponentSerializer = PlainTextComponentSerializer.plainText();
		final GsonComponentSerializer.Builder builder = GsonComponentSerializer.builder();
		if (!VersionHelper.isVersionNewerThan1_20_5()) {
			builder.legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.get());
			builder.editOptions((b) -> b.value(JSONOptions.EMIT_HOVER_SHOW_ENTITY_ID_AS_INT_ARRAY, false));
		}
		if (!VersionHelper.isVersionNewerThan1_21_5()) {
			builder.editOptions((b) -> {
				b.value(JSONOptions.EMIT_CLICK_EVENT_TYPE, JSONOptions.ClickEventValueMode.CAMEL_CASE);
				b.value(JSONOptions.EMIT_HOVER_EVENT_TYPE, JSONOptions.HoverEventValueMode.CAMEL_CASE);
				b.value(JSONOptions.EMIT_HOVER_SHOW_ENTITY_KEY_AS_TYPE_AND_UUID_AS_ID, true);
			});
		}
		this.gsonComponentSerializer = builder.build();
	}

	private static class SingletonHolder {
		private static final AdventureHelper INSTANCE = new AdventureHelper();
	}

	/**
	 * Retrieves the singleton instance of AdventureHelper.
	 *
	 * @return the singleton instance
	 */
	public static AdventureHelper getInstance() {
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Converts a MiniMessage string to a Component.
	 *
	 * @param text the MiniMessage string
	 * @return the resulting Component
	 */
	public static Component miniMessage(String text) {
		if (legacySupport) {
			return getMiniMessage().deserialize(legacyToMiniMessage(text));
		} else {
			return getMiniMessage().deserialize(text);
		}
	}

	/**
	 * Retrieves the MiniMessage instance.
	 *
	 * @return the MiniMessage instance
	 */
	public static MiniMessage getMiniMessage() {
		return getInstance().miniMessage;
	}

	/**
	 * Retrieves the GsonComponentSerializer instance.
	 *
	 * @return the GsonComponentSerializer instance
	 */
	public static GsonComponentSerializer getGson() {
		return getInstance().gsonComponentSerializer;
	}

	/**
	 * Converts a MiniMessage string to a JSON string.
	 *
	 * @param miniMessage the MiniMessage string
	 * @return the JSON string representation
	 */
	public static String miniMessageToJson(String miniMessage) {
		final AdventureHelper instance = getInstance();
		return instance.miniMessageToJsonCache.get(miniMessage,
				(text) -> instance.gsonComponentSerializer.serialize(miniMessage(text)));
	}

	/**
	 * Sends a centered message to an audience.
	 *
	 * @param audience the player to send the message to
	 * @param message  the message component
	 */
	public static void sendCenteredMessage(Sender audience, Component message) {
		if (message.children().contains(Component.newline())) {
			final Style parentStyle = message.style();
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
		final String msg = getGson().serialize(message);
		int messagePxSize = 0;
		boolean previousCode = false;
		boolean isBold = false;
		for (char c : msg.toCharArray()) {
			if (c == '§') {
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
		audience.sendMessage(Component.text(sb.toString()).append(message));
	}

	/**
	 * Plays a sound for an audience.
	 *
	 * @param audience the audience to play the sound for
	 * @param sound    the sound to play
	 */
	public static void playSound(Audience audience, Sound sound) {
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
	public static void playSound(Audience audience, Sound sound, double x, double y, double z) {
		audience.playSound(sound, x, y, z);
	}

	/**
	 * Plays a positional sound in a world at a specific location.
	 *
	 * @param world    the world to play the sound in
	 * @param location the location to play the sound at
	 * @param soundKey the namespaced key of the sound
	 * @param volume   the volume of the sound
	 * @param pitch    the pitch of the sound
	 */
	public static void playPositionalSound(@NotNull World world, @NotNull Location location, @NotNull String soundKey,
			float volume, float pitch) {
		NamespacedKey key = NamespacedKey.fromString(soundKey);
		if (key == null) {
			HellblockPlugin.getInstance().getPluginLogger().warn("Invalid sound key: " + soundKey);
			return;
		}

		org.bukkit.Sound sound = Registry.SOUNDS.get(key);
		if (sound == null) {
			HellblockPlugin.getInstance().getPluginLogger().warn("Unknown sound in registry: " + soundKey);
			return;
		}

		world.playSound(location, sound, SoundCategory.BLOCKS, volume, pitch);
	}

	/**
	 * Surrounds text with a MiniMessage font tag.
	 *
	 * @param text the text to surround
	 * @param font the font as a {@link Key}
	 * @return the text surrounded by the MiniMessage font tag
	 */
	public static String surroundWithMiniMessageFont(String text, Key font) {
		return "<font:" + font.asString() + ">" + text + "</font>";
	}

	/**
	 * Surrounds text with a MiniMessage font tag.
	 *
	 * @param text the text to surround
	 * @param font the font as a {@link String}
	 * @return the text surrounded by the MiniMessage font tag
	 */
	public static String surroundWithMiniMessageFont(String text, String font) {
		return "<font:" + font + ">" + text + "</font>";
	}

	/**
	 * Converts a JSON string to a MiniMessage string.
	 *
	 * @param json the JSON string
	 * @return the MiniMessage string representation
	 */
	public static String jsonToMiniMessage(String json) {
		return getInstance().miniMessageStrict.serialize(getInstance().gsonComponentSerializer.deserialize(json));
	}

	/**
	 * Converts a JSON string to a Component.
	 *
	 * @param json the JSON string
	 * @return the resulting Component
	 */
	public static Component jsonToComponent(String json) {
		return getInstance().gsonComponentSerializer.deserialize(json);
	}

	/**
	 * Converts a Component to a JSON string.
	 *
	 * @param component the Component to convert
	 * @return the JSON string representation
	 */
	public static String componentToJson(Component component) {
		return getGson().serialize(component);
	}

	public static @NotNull Component parseCenteredTitleMultiline(@NotNull String raw) {
		String[] lines = raw.split("\n");

		List<Component> components = new ArrayList<>();

		for (String line : lines) {
			String trimmedLine = line.trim();

			if (trimmedLine.toLowerCase(Locale.ROOT).startsWith("<center>")
					&& trimmedLine.toLowerCase(Locale.ROOT).contains("</center>")) {

				// Extract content between <center>...</center>
				int start = trimmedLine.indexOf("<center>") + "<center>".length();
				int end = trimmedLine.indexOf("</center>");
				String inner = trimmedLine.substring(start, end).trim();

				// Deserialize inner content to preserve formatting
				Component innerComponent = getInstance().miniMessage.deserialize(inner);
				String plain = getInstance().plainTextComponentSerializer.serialize(innerComponent);

				// Compute left-padding in pixels
				int pixelWidth = getStringPixelWidth(plain);
				int spacePixelWidth = getCharPixelWidth(' ');
				int paddingPixels = (MAX_PIXEL_WIDTH - pixelWidth) / 2;
				int spaceCount = Math.max(0, paddingPixels / spacePixelWidth);

				String spaces = " ".repeat(spaceCount);
				Component padded = getInstance().miniMessage.deserialize(spaces + inner);
				components.add(padded);

			} else {
				// Non-centered line, parse as-is
				components.add(getInstance().miniMessage.deserialize(trimmedLine));
			}
		}

		// Combine lines with newline separator
		Component result = Component.empty();
		for (int i = 0; i < components.size(); i++) {
			result = result.append(components.get(i));
			if (i != components.size() - 1) {
				result = result.append(Component.newline());
			}
		}

		return result;
	}

	// Estimate string width in Minecraft pixels
	private static int getStringPixelWidth(String text) {
		int width = 0;
		for (char c : text.toCharArray()) {
			width += getCharPixelWidth(c);
		}
		return width;
	}

	private static int getCharPixelWidth(char c) {
		return switch (c) {
		case 'i', '.', ',', ':', ';', '!', '|', ' ' -> 2;
		case '\'', 'l' -> 3;
		case '`', '(', ')', '[', ']', '{', '}' -> 4;
		case '<', '>', 't', 'f' -> 5;
		case '@', '~', '=', '-' -> 6;
		default -> 6;
		};
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
	public static String legacyToMiniMessage(String legacy) {
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
}