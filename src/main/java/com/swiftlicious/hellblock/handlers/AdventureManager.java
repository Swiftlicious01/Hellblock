package com.swiftlicious.hellblock.handlers;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.ReflectionUtils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class AdventureManager implements AdventureManagerInterface {

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
	public void sendMessage(CommandSender sender, String s) {
		if (s == null)
			return;
		if (sender == null)
			sendGlobalMessage(s);
		else if (sender instanceof Player player)
			sendPlayerMessage(player, s);
		else if (sender instanceof ConsoleCommandSender)
			sendConsoleMessage(s);
	}

	@Override
	public void sendMessageWithPrefix(CommandSender sender, String s) {
		if (s == null)
			return;
		if (sender == null)
			sendGlobalMessage(HBLocale.MSG_Prefix + s);
		else if (sender instanceof Player player)
			sendPlayerMessage(player, HBLocale.MSG_Prefix + s);
		else if (sender instanceof ConsoleCommandSender)
			sendConsoleMessage(HBLocale.MSG_Prefix + s);
	}

	@Override
	public void sendConsoleMessage(String s) {
		if (s == null)
			return;
		Audience au = Audience.audience(Bukkit.getConsoleSender());
		au.sendMessage(getComponentFromMiniMessage(s));
	}

	@Override
	public void sendGlobalMessage(String s) {
		if (s == null)
			return;
		Audience au = Audience.audience(Bukkit.getOnlinePlayers());
		au.sendMessage(getComponentFromMiniMessage(s));
	}

	@Override
	public void sendPlayerMessage(Player player, String s) {
		if (s == null)
			return;
		Audience au = Audience.audience(player);
		au.sendMessage(getComponentFromMiniMessage(s));
	}

	@Override
	public void sendTitle(Player player, String title, String subtitle, int in, int duration, int out) {
		sendTitle(player, getComponentFromMiniMessage(title), getComponentFromMiniMessage(subtitle), in, duration, out);
	}

	@Override
	public void sendTitle(Player player, Component title, Component subtitle, int in, int duration, int out) {
		try {
			PacketContainer titlePacket = new PacketContainer(PacketType.Play.Server.SET_TITLE_TEXT);
			titlePacket.getModifier().write(0, getIChatComponent(componentToJson(title)));
			PacketContainer subTitlePacket = new PacketContainer(PacketType.Play.Server.SET_SUBTITLE_TEXT);
			subTitlePacket.getModifier().write(0, getIChatComponent(componentToJson(subtitle)));
			PacketContainer timePacket = new PacketContainer(PacketType.Play.Server.SET_TITLES_ANIMATION);
			timePacket.getIntegers().write(0, in);
			timePacket.getIntegers().write(1, duration);
			timePacket.getIntegers().write(2, out);
			HellblockPlugin.getInstance().getProtocolManager().sendServerPacket(player, titlePacket);
			HellblockPlugin.getInstance().getProtocolManager().sendServerPacket(player, subTitlePacket);
			HellblockPlugin.getInstance().getProtocolManager().sendServerPacket(player, timePacket);
		} catch (InvocationTargetException | IllegalAccessException e) {
			LogUtils.warn("Error occurred when sending title.");
		}
	}

	@Override
	public void sendActionbar(Player player, String s) {
		try {
			PacketContainer packet = new PacketContainer(PacketType.Play.Server.SET_ACTION_BAR_TEXT);
			packet.getModifier().write(0, getIChatComponent(componentToJson(getComponentFromMiniMessage(s))));
			HellblockPlugin.getInstance().getProtocolManager().sendServerPacket(player, packet);
		} catch (InvocationTargetException | IllegalAccessException e) {
			LogUtils.warn("Error occurred when sending actionbar.");
		}
	}

	@Override
	public void sendSound(Player player, Sound.Source source, net.kyori.adventure.key.Key key, float volume,
			float pitch) {
		Sound sound = Sound.sound(key, source, volume, pitch);
		Audience au = Audience.audience(player);
		au.playSound(sound);
	}

	@Override
	public void sendSound(Player player, Sound sound) {
		Audience au = Audience.audience(player);
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

	public Object getIChatComponent(String json) throws InvocationTargetException, IllegalAccessException {
		return ReflectionUtils.iChatComponentMethod.invoke(null, json);
	}
}