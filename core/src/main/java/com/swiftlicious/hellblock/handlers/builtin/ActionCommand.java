package com.swiftlicious.hellblock.handlers.builtin;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;
import com.swiftlicious.hellblock.utils.ListUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;

public class ActionCommand<T> extends AbstractBuiltInAction<T> {

	private final List<String> commands;

	public ActionCommand(HellblockPlugin plugin, Object args, MathValue<T> chance) {
		super(plugin, chance);
		this.commands = ListUtils.toList(args);
	}

	@Override
	protected void triggerAction(Context<T> context) {
		if (context.argOrDefault(ContextKeys.OFFLINE, false))
			return;
		OfflinePlayer owner = null;
		if (context.holder() instanceof Player player) {
			owner = player;
		}
		List<String> replaced = plugin.getPlaceholderManager().parse(owner, commands, context.placeholderMap());
		plugin.getScheduler().sync().run(() -> {
			for (String text : replaced) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), text);
			}
		}, null);
	}

	public List<String> commands() {
		return commands;
	}
}