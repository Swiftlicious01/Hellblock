package com.swiftlicious.hellblock.gui.page.item;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.gui.icon.property.item.CMDItem;
import com.swiftlicious.hellblock.gui.icon.property.item.DisplayNameItem;
import com.swiftlicious.hellblock.gui.icon.property.item.DurabilityItem;
import com.swiftlicious.hellblock.gui.icon.property.item.EnchantmentItem;
import com.swiftlicious.hellblock.gui.icon.property.item.Head64Item;
import com.swiftlicious.hellblock.gui.icon.property.item.LoreItem;
import com.swiftlicious.hellblock.gui.icon.property.item.MaterialItem;
import com.swiftlicious.hellblock.gui.icon.property.item.NBTItem;
import com.swiftlicious.hellblock.gui.icon.property.item.RandomDurabilityItem;
import com.swiftlicious.hellblock.gui.icon.property.item.StackableItem;
import com.swiftlicious.hellblock.gui.icon.property.item.StoredEnchantmentItem;
import com.swiftlicious.hellblock.gui.icon.property.item.TagItem;
import com.swiftlicious.hellblock.gui.icon.property.item.UnbreakableItem;

import xyz.xenondevs.invui.item.Item;

public class BaitEditor extends AbstractSectionEditor {

	public BaitEditor(Player player, String key, ItemSelector itemSelector, ConfigurationSection section) {
		super(player, itemSelector, section, key);
	}

	@Override
	public List<Item> getItemList() {
		ArrayList<Item> items = new ArrayList<>();
		items.add(new MaterialItem(this));
		items.add(new DisplayNameItem(this));
		items.add(new LoreItem(this));
		items.add(new CMDItem(this));
		items.add(new TagItem(this));
		items.add(new UnbreakableItem(this));
		items.add(new DurabilityItem(this));
		items.add(new RandomDurabilityItem(this));
		items.add(new StackableItem(this));
		items.add(new Head64Item(this));
		items.add(new NBTItem(this));
		items.add(new EnchantmentItem(this));
		items.add(new StoredEnchantmentItem(this));
		return items;
	}
}