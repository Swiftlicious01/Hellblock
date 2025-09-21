package com.swiftlicious.hellblock.handlers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.api.DefaultFontInfo;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.kyori.adventure.text.serializer.json.legacyimpl.NBTLegacyHoverEventSerializer;
import net.kyori.adventure.title.Title;

/**
 * Helper class for handling Adventure components and related functionalities.
 */
public class AdventureHelper {

	private final static int CENTER_PX = 154;

	private final MiniMessage miniMessage;
	private final MiniMessage miniMessageStrict;
	private final GsonComponentSerializer gsonComponentSerializer;
	private final Cache<String, String> miniMessageToJsonCache = Caffeine.newBuilder()
			.expireAfterWrite(5, TimeUnit.MINUTES).build();
	public static boolean legacySupport = false;

	public AdventureHelper() {
		this.miniMessage = MiniMessage.builder().build();
		this.miniMessageStrict = MiniMessage.builder().strict(true).build();
		GsonComponentSerializer.Builder builder = GsonComponentSerializer.builder();
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
		AdventureHelper instance = getInstance();
		return instance.miniMessageToJsonCache.get(miniMessage,
				(text) -> instance.gsonComponentSerializer.serialize(miniMessage(text)));
	}

	/**
	 * Sends a title to an audience.
	 *
	 * @param audience the audience to send the title to
	 * @param title    the title component
	 * @param subtitle the subtitle component
	 * @param fadeIn   the fade-in duration in ticks
	 * @param stay     the stay duration in ticks
	 * @param fadeOut  the fade-out duration in ticks
	 */
	public static void sendTitle(Audience audience, Component title, Component subtitle, int fadeIn, int stay,
			int fadeOut) {
		audience.showTitle(Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(fadeIn * 50L),
				Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))));
	}

	/**
	 * Sends an action bar message to an audience.
	 *
	 * @param audience  the audience to send the action bar message to
	 * @param actionBar the action bar component
	 */
	public static void sendActionBar(Audience audience, Component actionBar) {
		audience.sendActionBar(actionBar);
	}

	/**
	 * Sends a message to an audience.
	 *
	 * @param audience the audience to send the message to
	 * @param message  the message component
	 */
	public static void sendMessage(Audience audience, Component message) {
		audience.sendMessage(message);
	}

	/**
	 * Sends a centered message to an audience.
	 *
	 * @param audience the audience to send the message to
	 * @param message  the message component
	 */
	public static void sendCenteredMessage(Audience audience, Component message) {
		if (message.children().contains(Component.newline())) {
			Style parentStyle = message.style();
			List<Component> children = new ArrayList<>(message.children());
			children.add(0, message.children(new ArrayList<>()));
			Component toSend = Component.empty().style(parentStyle);
			for (int i = 0; i < children.size(); i++) {
				Component child = children.get(i);
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
		String msg = getGson().serialize(message);
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
				DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
				messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
				messagePxSize++;
			}
		}
		int halvedMessageSize = messagePxSize / 2;
		int toCompensate = CENTER_PX - halvedMessageSize;
		int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
		int compensated = 0;
		StringBuilder sb = new StringBuilder();
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
		StringBuilder stringBuilder = new StringBuilder();
		char[] chars = legacy.toCharArray();
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