package com.swiftlicious.hellblock.handlers.builtin;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.ListUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;

public class ActionBroadcast<T> extends AbstractBuiltInAction<T> {

	private final List<String> messages;

	public ActionBroadcast(HellblockPlugin plugin, Object args, MathValue<T> chance) {
		super(plugin, chance);
		this.messages = ListUtils.toList(args);
	}

	@Override
	protected void triggerAction(Context<T> context) {
		if (context.argOrDefault(ContextKeys.OFFLINE, false)) {
			return;
		}
		OfflinePlayer offlinePlayer = null;
		if (context.holder() instanceof Player player) {
			offlinePlayer = player;
		}
		final List<String> replaced = plugin.getPlaceholderManager().parse(offlinePlayer, messages,
				context.placeholderMap());
		Bukkit.getOnlinePlayers().stream().map((Player player) -> plugin.getSenderFactory().wrap(player))
				.forEach((final Sender audience) -> replaced
						.forEach(text -> audience.sendMessage(AdventureHelper.miniMessage(text))));
	}

	public List<String> messages() {
		return messages;
	}
}