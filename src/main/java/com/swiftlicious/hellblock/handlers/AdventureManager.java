package com.swiftlicious.hellblock.handlers;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.DefaultFontInfo;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.utils.ReflectionUtils;

import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

/**
 * Helper class for handling Adventure components and related functionalities.
 */
public class AdventureManager implements AdventureManagerInterface {

	private final static int CENTER_PX = 154;

	private final MiniMessage miniMessage;
	private final LegacyComponentSerializer legacyComponentSerializer;
	private final GsonComponentSerializer gsonComponentSerializer;

	public AdventureManager(HellblockPlugin plugin) {
		this.miniMessage = MiniMessage.builder().build();
		this.legacyComponentSerializer = LegacyComponentSerializer.builder().build();
		this.gsonComponentSerializer = GsonComponentSerializer.builder().build();
	}

	/**
	 * Retrieves the MiniMessage instance.
	 *
	 * @return the MiniMessage instance
	 */
	public MiniMessage getMiniMessage() {
		return this.miniMessage;
	}

	@Override
	public @NonNull Component getComponentFromMiniMessage(@Nullable String text) {
		if (text == null) {
			return Component.empty();
		}
		if (HBConfig.legacyColorSupport) {
			return miniMessage.deserialize(legacyToMiniMessage(text));
		} else {
			return miniMessage.deserialize(text);
		}
	}

	@Override
	public void sendMessage(@Nullable CommandSender sender, @Nullable String message) {
		if (message == null)
			return;
		if (sender == null)
			sendGlobalMessage(message);
		else if (sender instanceof Player player)
			sendPlayerMessage(player, message);
		else if (sender instanceof ConsoleCommandSender)
			sendConsoleMessage(message);
	}

	@Override
	public void sendMessageWithPrefix(@Nullable CommandSender sender, @Nullable String message) {
		if (message == null)
			return;
		if (sender == null)
			sendGlobalMessage(HBLocale.MSG_Prefix + message);
		else if (sender instanceof Player player)
			sendPlayerMessage(player, HBLocale.MSG_Prefix + message);
		else if (sender instanceof ConsoleCommandSender)
			sendConsoleMessage(HBLocale.MSG_Prefix + message);
	}

	@Override
	public void sendConsoleMessage(@Nullable String message) {
		if (message == null)
			return;
		Audience au = Audience.audience(Bukkit.getConsoleSender());
		au.sendMessage(getComponentFromMiniMessage(message));
	}

	@Override
	public void sendGlobalMessage(@Nullable String message) {
		if (message == null)
			return;
		Audience au = Audience.audience(Bukkit.getOnlinePlayers());
		au.sendMessage(getComponentFromMiniMessage(message));
	}

	@Override
	public void sendPlayerMessage(@NonNull Player player, @Nullable String message) {
		if (message == null)
			return;
		Audience au = Audience.audience(player);
		au.sendMessage(getComponentFromMiniMessage(message));
	}

	@Override
	public void sendCenteredMessage(@NonNull Player player, @Nullable String message) {
		sendCenteredMessage(player, getComponentFromMiniMessage(message));
	}

	public void sendCenteredMessage(@NonNull Player player, @Nullable Component component) {
		if (component == null) {
			return;
		}
		if (component.children().contains(Component.newline())) {
			Style parentStyle = component.style();
			List<Component> children = new ArrayList<>(component.children());
			children.add(0, component.children(new ArrayList<>()));
			Component toSend = Component.empty().style(parentStyle);
			for (int i = 0; i < children.size(); i++) {
				Component child = children.get(i);
				if (child.equals(Component.newline())) {
					sendCenteredMessage(player, toSend);
					if (i == children.size() - 1) {
						break;
					}
					toSend = children.get(i + 1).applyFallbackStyle(parentStyle);
					i++;
				} else {
					toSend = toSend.append(child);
				}
			}
			sendCenteredMessage(player, toSend);
			return;
		}
		String message = legacyComponentSerializer.serialize(component);
		int messagePxSize = 0;
		boolean previousCode = false;
		boolean isBold = false;
		for (char c : message.toCharArray()) {
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
		Audience au = Audience.audience(player);
		au.sendMessage(Component.text(sb.toString()).append(component));
	}

	@Override
	public void sendTitle(@NonNull Player player, @NonNull String title, @NonNull String subtitle, int in, int duration,
			int out) {
		sendTitle(player, getComponentFromMiniMessage(title), getComponentFromMiniMessage(subtitle), in, duration, out);
	}

	@Override
	public void sendTitle(@NonNull Player player, @NonNull Component title, @NonNull Component subtitle, int fadeIn,
			int stay, int fadeOut) {
		Audience au = Audience.audience(player);
		au.showTitle(Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(fadeIn * 50L),
				Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))));
	}

	@Override
	public void sendActionbar(@NonNull Player player, @Nullable String text) {
		if (text == null)
			return;
		Audience au = Audience.audience(player);
		au.sendActionBar(getComponentFromMiniMessage(text));
	}

	@Override
	public void sendSound(@NonNull Player player, @NonNull Sound.Source source,
			@NonNull net.kyori.adventure.key.Key key, float volume, float pitch) {
		Sound sound = Sound.sound(key, source, volume, pitch);
		Audience au = Audience.audience(player);
		au.playSound(sound);
	}

	@Override
	public void sendSound(@NonNull Location location, @NonNull Sound.Source source,
			@NonNull net.kyori.adventure.key.Key key, float volume, float pitch) {
		Sound sound = Sound.sound(key, source, volume, pitch);
		Audience au = Audience.audience(location.getNearbyPlayers(5.0D));
		au.playSound(sound);
	}

	@Override
	public void sendSound(@NonNull Player player, @NonNull Sound sound) {
		Audience au = Audience.audience(player);
		au.playSound(sound);
	}

	@Override
	public void sendSound(@NonNull Location location, @NonNull Sound sound) {
		Audience au = Audience.audience(location.getNearbyPlayers(5.0D));
		au.playSound(sound);
	}

	@Override
	public @NonNull String legacyToMiniMessage(@NonNull String legacy) {
		StringBuilder stringBuilder = new StringBuilder();
		char[] chars = legacy.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (!isColorCode(chars[i])) {
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
			case 'r' -> stringBuilder.append("<r><!i>");
			case 'l' -> stringBuilder.append("<b>");
			case 'm' -> stringBuilder.append("<st>");
			case 'o' -> stringBuilder.append("<i>");
			case 'n' -> stringBuilder.append("<u>");
			case 'k' -> stringBuilder.append("<obf>");
			case 'x' -> {
				if (i + 13 >= chars.length || !isColorCode(chars[i + 2]) || !isColorCode(chars[i + 4])
						|| !isColorCode(chars[i + 6]) || !isColorCode(chars[i + 8]) || !isColorCode(chars[i + 10])
						|| !isColorCode(chars[i + 12])) {
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

	@Override
	public boolean isColorCode(char c) {
		return c == '§' || c == '&';
	}

	@Override
	public @NonNull String componentToLegacy(@NonNull Component component) {
		return legacyComponentSerializer.serialize(component);
	}

	@Override
	public @NonNull String componentToJson(@NonNull Component component) {
		return gsonComponentSerializer.serialize(component);
	}

	@Override
	public @NonNull Component jsonToComponent(@NonNull String json) {
		return gsonComponentSerializer.deserialize(json);
	}

	@Override
	public @NonNull Object shadedComponentToOriginalComponent(@NonNull Component component) {
		Object cp;
		try {
			cp = ReflectionUtils.gsonDeserializeMethod.invoke(ReflectionUtils.gsonInstance,
					gsonComponentSerializer.serialize(component));
		} catch (InvocationTargetException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
		return cp;
	}

	/**
	 * Surrounds text with a MiniMessage font tag.
	 *
	 * @param text the text to surround
	 * @param font the font as a {@link Key}
	 * @return the text surrounded by the MiniMessage font tag
	 */
	public @NonNull String surroundWithMiniMessageFont(@NonNull String text, @NonNull Key font) {
		return "<font:" + font.asString() + ">" + text + "</font>";
	}

	/**
	 * Surrounds text with a MiniMessage font tag.
	 *
	 * @param text the text to surround
	 * @param font the font as a {@link String}
	 * @return the text surrounded by the MiniMessage font tag
	 */
	public @NonNull String surroundWithMiniMessageFont(@NonNull String text, @NonNull String font) {
		return "<font:" + font + ">" + text + "</font>";
	}

}