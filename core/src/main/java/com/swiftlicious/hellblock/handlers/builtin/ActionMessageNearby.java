package com.swiftlicious.hellblock.handlers.builtin;

import static java.util.Objects.requireNonNull;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.ListUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ActionMessageNearby<T> extends AbstractBuiltInAction<T> {

	private final List<String> messages;
	private final MathValue<T> range;

	public ActionMessageNearby(HellblockPlugin plugin, Section section, MathValue<T> chance) {
		super(plugin, chance);
		this.messages = ListUtils.toList(section.get("message"));
		this.range = MathValue.auto(section.get("range"));
	}

	@Override
	protected void triggerAction(Context<T> context) {
		if (context.argOrDefault(ContextKeys.OFFLINE, false))
			return;
		double realRange = range.evaluate(context);
		OfflinePlayer owner = null;
		if (context.holder() instanceof Player player) {
			owner = player;
		}
		Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
		for (Player player : location.getWorld().getPlayers()) {
			if (LocationUtils.getDistance(player.getLocation(), location) <= realRange) {
				context.arg(ContextKeys.TEMP_NEAR_PLAYER, player.getName());
				List<String> replaced = plugin.getPlaceholderManager().parse(owner, messages, context.placeholderMap());
                Sender audience = plugin.getSenderFactory().wrap(player);
				for (String text : replaced) {
					audience.sendMessage(AdventureHelper.miniMessage(text));
				}
			}
		}
	}

	public List<String> messages() {
		return messages;
	}

	public MathValue<T> range() {
		return range;
	}
}