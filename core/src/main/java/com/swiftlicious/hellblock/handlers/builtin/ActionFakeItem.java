package com.swiftlicious.hellblock.handlers.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.entity.FakeEntity;
import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.nms.entity.display.FakeItemDisplay;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

import static java.util.Objects.requireNonNull;

public class ActionFakeItem<T> extends AbstractBuiltInAction<T> {

	private final String itemID;
	private final MathValue<T> duration;
	private final boolean other;
	private final MathValue<T> x;
	private final MathValue<T> y;
	private final MathValue<T> z;
	private final MathValue<T> yaw;
	private final int range;
	private final boolean visibleToAll;
	private final boolean useItemDisplay;

	public ActionFakeItem(HellblockPlugin plugin, Section section, MathValue<T> chance) {
		super(plugin, chance);
		String itemID = section.getString("item", "");
		String[] split = itemID.split(":");
		if (split.length >= 2)
			itemID = split[split.length - 1];
		this.itemID = itemID;
		this.duration = MathValue.auto(section.get("duration", 20));
		this.other = section.getString("position", "other").equals("other");
		this.x = MathValue.auto(section.get("x", 0));
		this.y = MathValue.auto(section.get("y", 0));
		this.z = MathValue.auto(section.get("z", 0));
		this.yaw = MathValue.auto(section.get("yaw", 0));
		this.range = section.getInt("range", 32);
		this.visibleToAll = section.getBoolean("visible-to-all", true);
		this.useItemDisplay = section.getBoolean("use-item-display", false);
	}

	@Override
	protected void triggerAction(Context<T> context) {
		if (context.argOrDefault(ContextKeys.OFFLINE, false))
			return;
		Player owner = null;
		if (context.holder() instanceof Player p) {
			owner = p;
		}
		Location location = other ? requireNonNull(context.arg(ContextKeys.LOCATION)).clone()
				: requireNonNull(owner).getLocation().clone();
		location.add(x.evaluate(context), y.evaluate(context), z.evaluate(context));
		location.setPitch(0);
		location.setYaw((float) yaw.evaluate(context));
		FakeEntity fakeEntity;
		if (useItemDisplay && VersionHelper.isVersionNewerThan1_19_4()) {
			location.add(0, 1.5, 0);
			FakeItemDisplay itemDisplay = VersionHelper.getNMSManager().createFakeItemDisplay(location);
			itemDisplay.item(plugin.getItemManager().buildInternal(Context.player(requireNonNull(owner)), itemID));
			fakeEntity = itemDisplay;
		} else {
			FakeArmorStand armorStand = VersionHelper.getNMSManager().createFakeArmorStand(location);
			armorStand.invisible(true);
			armorStand.equipment(EquipmentSlot.HEAD,
					plugin.getItemManager().buildInternal(Context.player(requireNonNull(owner)), itemID));
			fakeEntity = armorStand;
		}
		List<Player> viewers = new ArrayList<>();
		if (range > 0 && visibleToAll) {
			for (Player player : location.getWorld().getPlayers()) {
				if (LocationUtils.getDistance(player.getLocation(), location) <= range) {
					viewers.add(player);
				}
			}
		} else {
			if (owner != null) {
				viewers.add(owner);
			}
		}
		if (viewers.isEmpty())
			return;
		for (Player player : viewers) {
			fakeEntity.spawn(player);
		}
		plugin.getScheduler().asyncLater(() -> {
			for (Player player : viewers) {
				if (player.isOnline() && player.isValid()) {
					fakeEntity.destroy(player);
				}
			}
		}, (long) (duration.evaluate(context) * 50), TimeUnit.MILLISECONDS);
	}

	public String itemID() {
		return itemID;
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

	public MathValue<T> yaw() {
		return yaw;
	}

	public int range() {
		return range;
	}

	public boolean visibleToAll() {
		return visibleToAll;
	}

	public boolean useItemDisplay() {
		return useItemDisplay;
	}
}