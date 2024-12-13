package com.swiftlicious.hellblock.handlers.builtin;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.audience.Audience;

import static java.util.Objects.requireNonNull;

public class ActionActionbarNearby<T> extends AbstractBuiltInAction<T> {

	private final String actionbar;
	private final MathValue<T> range;

	public ActionActionbarNearby(HellblockPlugin plugin, Section section, MathValue<T> chance) {
		super(plugin, chance);
		this.actionbar = section.getString("actionbar");
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
		Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
		double realRange = range.evaluate(context);
		for (Player player : location.getWorld().getPlayers()) {
			if (LocationUtils.getDistance(player.getLocation(), location) <= realRange) {
				context.arg(ContextKeys.TEMP_NEAR_PLAYER, player.getName());
				String replaced = plugin.getPlaceholderManager().parse(owner, actionbar, context.placeholderMap());
				Audience audience = plugin.getSenderFactory().getAudience(player);
				audience.sendActionBar(AdventureHelper.miniMessage(replaced));
			}
		}
	}

	public String actionbar() {
		return actionbar;
	}

	public MathValue<T> range() {
		return range;
	}
}