package com.swiftlicious.hellblock.handlers.builtin;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.world.HellblockWorld;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ActionHologram<T> extends AbstractBuiltInAction<T> {

	private final TextValue<T> text;
	private final MathValue<T> duration;
	private final boolean other;
	private final MathValue<T> x;
	private final MathValue<T> y;
	private final MathValue<T> z;
	private final boolean onlyShowToOne;
	private final int range;

	public ActionHologram(HellblockPlugin plugin, Section section, MathValue<T> chance) {
		super(plugin, chance);
		this.text = TextValue.auto(section.getString("text", ""));
		this.duration = MathValue.auto(section.get("duration", 20));
		this.other = "other".equals(section.getString("position", "other"));
		this.x = MathValue.auto(section.get("x", 0));
		this.y = MathValue.auto(section.get("y", 0));
		this.z = MathValue.auto(section.get("z", 0));
		this.onlyShowToOne = !section.getBoolean("visible-to-all", false);
		this.range = section.getInt("range", 32);
	}

	@Override
	protected void triggerAction(Context<T> context) {
		if (context.argOrDefault(ContextKeys.OFFLINE, false)) {
			return;
		}
		Player owner = null;
		if (context.holder() instanceof Player p) {
			owner = p;
		}
		final Location location = other ? requireNonNull(context.arg(ContextKeys.LOCATION)).clone()
				: owner.getLocation().clone();
		// Pos3 pos3 = Pos3.from(location).add(0,1,0);
		location.add(x.evaluate(context), y.evaluate(context), z.evaluate(context));
		final Optional<HellblockWorld<?>> optionalWorld = plugin.getWorldManager().getWorld(location.getWorld());
		if (optionalWorld.isEmpty()) {
			return;
		}
		final List<Player> viewers = new ArrayList<>();
		if (onlyShowToOne) {
			if (owner == null) {
				return;
			}
			viewers.add(owner);
		} else {
			location.getWorld().getPlayers().stream()
					.filter(player -> LocationUtils.getDistance(player.getLocation(), location) <= range)
					.forEach(viewers::add);
		}
		if (viewers.isEmpty()) {
			return;
		}
		final String json = AdventureHelper.componentToJson(AdventureHelper.miniMessage(text.render(context)));
		final int durationInMillis = (int) (duration.evaluate(context) * 50);
		viewers.forEach(viewer -> plugin.getHologramManager().showHologram(viewer, location, json, durationInMillis));
	}

	public TextValue<T> text() {
		return text;
	}

	public MathValue<T> duration() {
		return duration;
	}

	public boolean otherPosition() {
		return other;
	}

	public MathValue<T> x() {
		return x;
	}

	public MathValue<T> y() {
		return y;
	}

	public MathValue<T> z() {
		return z;
	}

	public boolean showToOne() {
		return onlyShowToOne;
	}

	public int range() {
		return range;
	}
}