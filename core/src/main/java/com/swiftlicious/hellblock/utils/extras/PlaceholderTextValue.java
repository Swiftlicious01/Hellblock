package com.swiftlicious.hellblock.utils.extras;

import java.util.Map;

import org.bukkit.OfflinePlayer;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.Context;

public class PlaceholderTextValue<T> implements TextValue<T> {

	private final String raw;

	public PlaceholderTextValue(String raw) {
		this.raw = raw;
	}

	@Override
	public String render(Context<T> context) {
		Map<String, String> replacements = context.placeholderMap();
		String text;
		if (context.holder() instanceof OfflinePlayer player)
			text = HellblockPlugin.getInstance().getPlaceholderManager().parse(player, raw, replacements);
		else
			text = HellblockPlugin.getInstance().getPlaceholderManager().parse(null, raw, replacements);
		return text;
	}
}