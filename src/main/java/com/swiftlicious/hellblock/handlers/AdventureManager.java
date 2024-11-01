package com.swiftlicious.hellblock.handlers;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.api.DefaultFontInfo;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.utils.ReflectionUtils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

public class AdventureManager implements AdventureManagerInterface {

	private final static int CENTER_PX = 154;

	@Override
	public Component getComponentFromMiniMessage(String text) {
		if (text == null) {
			return Component.empty();
		}
		if (HBConfig.legacyColorSupport) {
			return MiniMessage.miniMessage().deserialize(legacyToMiniMessage(text));
		} else {
			return MiniMessage.miniMessage().deserialize(text);
		}
	}

	@Override
	public void sendMessage(CommandSender sender, String message) {
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
	public void sendMessageWithPrefix(CommandSender sender, String message) {
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
	public void sendConsoleMessage(String message) {
		if (message == null)
			return;
		Audience au = Audience.audience(Bukkit.getConsoleSender());
		au.sendMessage(getComponentFromMiniMessage(message));
	}

	@Override
	public void sendGlobalMessage(String message) {
		if (message == null)
			return;
		Audience au = Audience.audience(Bukkit.getOnlinePlayers());
		au.sendMessage(getComponentFromMiniMessage(message));
	}

	@Override
	public void sendPlayerMessage(Player player, String message) {
		if (message == null)
			return;
		Audience au = Audience.audience(player);
		au.sendMessage(getComponentFromMiniMessage(message));
	}

	@Override
	public void sendCenteredMessage(Player player, String message) {
		if (message == null)
			return;

		int messagePxSize = 0;
		boolean isBold = false;

		for (char c : message.toCharArray()) {
			if (getComponentFromMiniMessage(message).hasDecoration(TextDecoration.BOLD)) {
				isBold = true;
				continue;
			} else
				isBold = false;
			DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
			messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
			messagePxSize++;
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
		au.sendMessage(getComponentFromMiniMessage(sb.toString() + message));
	}

	@Override
	public void sendTitle(Player player, String title, String subtitle, int in, int duration, int out) {
		sendTitle(player, getComponentFromMiniMessage(title), getComponentFromMiniMessage(subtitle), in, duration, out);
	}

	@Override
	public void sendTitle(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
		Audience au = Audience.audience(player);
		au.showTitle(Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(fadeIn * 50L),
				Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))));
	}

	@Override
	public void sendActionbar(Player player, String text) {
		if (text == null)
			return;
		Audience au = Audience.audience(player);
		au.sendActionBar(getComponentFromMiniMessage(text));
	}

	@Override
	public void sendSound(Player player, Sound.Source source, net.kyori.adventure.key.Key key, float volume,
			float pitch) {
		Sound sound = Sound.sound(key, source, volume, pitch);
		Audience au = Audience.audience(player);
		au.playSound(sound);
	}

	@Override
	public void sendSound(Location location, Sound.Source source, net.kyori.adventure.key.Key key, float volume,
			float pitch) {
		Sound sound = Sound.sound(key, source, volume, pitch);
		Audience au = Audience.audience(location.getNearbyPlayers(5.0D));
		au.playSound(sound);
	}

	@Override
	public void sendSound(Player player, Sound sound) {
		Audience au = Audience.audience(player);
		au.playSound(sound);
	}

	@Override
	public void sendSound(Location location, Sound sound) {
		Audience au = Audience.audience(location.getNearbyPlayers(5.0D));
		au.playSound(sound);
	}

	@Override
	public String legacyToMiniMessage(String legacy) {
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
	public String componentToLegacy(Component component) {
		return LegacyComponentSerializer.legacySection().serialize(component);
	}

	@Override
	public String componentToJson(Component component) {
		return GsonComponentSerializer.gson().serialize(component);
	}

	@Override
	public Component jsonToComponent(String json) {
		return GsonComponentSerializer.gson().deserialize(json);
	}

	@Override
	public Object shadedComponentToOriginalComponent(Component component) {
		Object cp;
		try {
			cp = ReflectionUtils.gsonDeserializeMethod.invoke(ReflectionUtils.gsonInstance,
					GsonComponentSerializer.gson().serialize(component));
		} catch (InvocationTargetException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
		return cp;
	}
}