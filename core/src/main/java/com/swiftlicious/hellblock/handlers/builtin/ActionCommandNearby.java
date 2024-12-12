package com.swiftlicious.hellblock.handlers.builtin;

import static java.util.Objects.requireNonNull;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;
import com.swiftlicious.hellblock.utils.ListUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ActionCommandNearby<T> extends AbstractBuiltInAction<T> {

	private final List<String> cmd;
	private final MathValue<T> range;

	public ActionCommandNearby(HellblockPlugin plugin, Section section, MathValue<T> chance) {
		super(plugin, chance);
		this.cmd = ListUtils.toList(section.get("command"));
		this.range = MathValue.auto(section.get("range"));
	}

	@Override
	protected void triggerAction(Context<T> context) {
		if (context.argOrDefault(ContextKeys.OFFLINE, false))
			return;
		OfflinePlayer owner = null;
		if (context.holder() instanceof Player player) {
			owner = player;
		}
		double realRange = range.evaluate(context);
		Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
		for (Player player : location.getWorld().getPlayers()) {
			if (LocationUtils.getDistance(player.getLocation(), location) <= realRange) {
				context.arg(ContextKeys.TEMP_NEAR_PLAYER, player.getName());
				List<String> replaced = plugin.getPlaceholderManager().parse(owner, cmd, context.placeholderMap());
				for (String text : replaced) {
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), text);
				}
			}
		}
	}

	public List<String> commands() {
		return cmd;
	}

	public MathValue<T> range() {
		return range;
	}
}