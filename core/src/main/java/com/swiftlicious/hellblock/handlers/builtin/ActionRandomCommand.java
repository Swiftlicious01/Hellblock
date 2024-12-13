package com.swiftlicious.hellblock.handlers.builtin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.utils.ListUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;

public class ActionRandomCommand<T> extends AbstractBuiltInAction<T> {

	private final List<String> commands;

	public ActionRandomCommand(HellblockPlugin plugin, Object args, MathValue<T> chance) {
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
		String random = commands.get(ThreadLocalRandom.current().nextInt(commands.size()));
		random = plugin.getPlaceholderManager().parse(owner, random, context.placeholderMap());
		String finalRandom = random;
		plugin.getScheduler().sync().run(() -> {
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalRandom);
		}, null);
	}

	public List<String> commands() {
		return commands;
	}
}