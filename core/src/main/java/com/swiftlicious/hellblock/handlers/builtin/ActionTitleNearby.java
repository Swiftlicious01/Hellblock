package com.swiftlicious.hellblock.handlers.builtin;

import static java.util.Objects.requireNonNull;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ActionTitleNearby<T> extends AbstractBuiltInAction<T> {

	private final TextValue<T> title;
	private final TextValue<T> subtitle;
	private final int fadeIn;
	private final int stay;
	private final int fadeOut;
	private final int range;

	public ActionTitleNearby(HellblockPlugin plugin, Section section, MathValue<T> chance) {
		super(plugin, chance);
		this.title = TextValue.auto(section.getString("title"));
		this.subtitle = TextValue.auto(section.getString("subtitle"));
		this.fadeIn = section.getInt("fade-in", 20);
		this.stay = section.getInt("stay", 30);
		this.fadeOut = section.getInt("fade-out", 10);
		this.range = section.getInt("range", 0);
	}

	@Override
	protected void triggerAction(Context<T> context) {
		if (context.argOrDefault(ContextKeys.OFFLINE, false))
			return;
		Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
		for (Player player : location.getWorld().getPlayers()) {
			if (LocationUtils.getDistance(player.getLocation(), location) <= range) {
				context.arg(ContextKeys.TEMP_NEAR_PLAYER, player.getName());
				VersionHelper.getNMSManager().sendTitle(player,
						AdventureHelper.componentToJson(AdventureHelper.miniMessage(title.render(context))),
						AdventureHelper.componentToJson(AdventureHelper.miniMessage(subtitle.render(context))), fadeIn,
						stay, fadeOut);
			}
		}
	}

	public TextValue<T> title() {
		return title;
	}

	public TextValue<T> subtitle() {
		return subtitle;
	}

	public int fadeIn() {
		return fadeIn;
	}

	public int stay() {
		return stay;
	}

	public int fadeOut() {
		return fadeOut;
	}

	public int range() {
		return range;
	}
}