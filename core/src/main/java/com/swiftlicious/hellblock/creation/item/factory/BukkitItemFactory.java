package com.swiftlicious.hellblock.creation.item.factory;

import java.util.Objects;
import java.util.Optional;

import org.bukkit.inventory.ItemStack;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.creation.item.ItemFactory;
import com.swiftlicious.hellblock.nms.exception.UnsupportedVersionException;

public abstract class BukkitItemFactory extends ItemFactory<HellblockPlugin, RtagItem, ItemStack> {

	protected BukkitItemFactory(HellblockPlugin plugin) {
		super(plugin);
	}

	public static BukkitItemFactory create(HellblockPlugin plugin) {
		Objects.requireNonNull(plugin, "plugin");
		switch (plugin.getVersionManager().getServerVersion()) {
		case "1.17", "1.17.1", "1.18", "1.18.1", "1.18.2", "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4", "1.20",
				"1.20.1", "1.20.2", "1.20.3", "1.20.4" -> {
			return new UniversalItemFactory(plugin);
		}
		case "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4" -> {
			return new ComponentItemFactory(plugin);
		}
		default -> throw new UnsupportedVersionException(
				"Unsupported server version: " + plugin.getVersionManager().getServerVersion());
		}
	}

	public Item<ItemStack> wrap(ItemStack item) {
		Objects.requireNonNull(item, "item");
		return wrap(new RtagItem(item));
	}

	@Override
	protected void setTag(RtagItem item, Object value, Object... path) {
		item.set(value, path);
	}

	@Override
	protected Optional<Object> getTag(RtagItem item, Object... path) {
		return Optional.ofNullable(item.get(path));
	}

	@Override
	protected boolean hasTag(RtagItem item, Object... path) {
		return item.hasTag(path);
	}

	@Override
	protected boolean removeTag(RtagItem item, Object... path) {
		return item.remove(path);
	}

	@Override
	protected void update(RtagItem item) {
		item.update();
	}

	@Override
	protected ItemStack load(RtagItem item) {
		return item.load();
	}

	@Override
	protected ItemStack getItem(RtagItem item) {
		return item.getItem();
	}

	@Override
	protected ItemStack loadCopy(RtagItem item) {
		return item.loadCopy();
	}
}