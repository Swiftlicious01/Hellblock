package com.swiftlicious.hellblock.handlers.builtin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

import static java.util.Objects.requireNonNull;

public class ActionDropItem<T> extends AbstractBuiltInAction<T> {

	private final String item;
	private final MathValue<T> min;
	private final MathValue<T> max;
	private final boolean toInv;

	public ActionDropItem(HellblockPlugin plugin, Section section, MathValue<T> chance) {
		super(plugin, chance);
		this.item = section.getString("item");
		this.min = MathValue.auto(section.get("min"));
		this.max = MathValue.auto(section.get("max"));
		this.toInv = section.getBoolean("to-inventory", false);
	}

	@Override
	protected void triggerAction(Context<T> context) {
		Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
		Player player;
		if (context.holder() instanceof Player p) {
			player = p;
		} else {
			player = null;
		}
		int random = RandomUtils.generateRandomInt((int) min.evaluate(context), (int) max.evaluate(context));
		if (random <= 0)
			return;
		ItemStack itemStack = generateItem(player, random);
		plugin.getScheduler().sync().run(() -> {
			if (itemStack != null && itemStack.getType() != Material.AIR && itemStack.getAmount() > 0) {
				if (toInv && player != null) {
					PlayerUtils.giveItem(player, itemStack, itemStack.getAmount());
				} else {
					location.getWorld().dropItemNaturally(location, itemStack);
				}
			}
		}, location);
	}

	@Nullable
	public ItemStack generateItem(@Nullable Player player, int amount) {
		ItemStack itemStack = plugin.getItemManager().buildAny(Context.player(player), item);
		if (itemStack != null) {
			itemStack.setAmount(amount);
		} else {
			plugin.getPluginLogger().warn("Item: " + item + " doesn't exist");
		}
		return itemStack;
	}

	public String itemID() {
		return item;
	}

	public MathValue<T> min() {
		return min;
	}

	public MathValue<T> max() {
		return max;
	}

	public boolean toInventory() {
		return toInv;
	}
}