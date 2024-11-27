package com.swiftlicious.hellblock.creation.item.damage;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ScoreComponent;

public class CustomDurabilityItem implements DurabilityItem {

	private final Item<ItemStack> item;

	public CustomDurabilityItem(Item<ItemStack> item) {
		this.item = item;
	}

	@Override
	public void damage(int value) {
		int customMaxDamage = (int) item.getTag("HellblockItem", "max_dur").get();
		int maxDamage = item.maxDamage().get();
		double ratio = (double) maxDamage / (double) customMaxDamage;
		int fakeDamage = (int) (value * ratio);
		item.damage(fakeDamage);
		item.setTag(customMaxDamage - value, "HellblockItem", "cur_dur");
		List<String> durabilityLore = HellblockPlugin.getInstance().getConfigManager().durabilityLore();
		List<String> previousLore = item.lore().orElse(new ArrayList<>());
		List<String> newLore = new ArrayList<>();
		for (String previous : previousLore) {
			Component component = AdventureHelper.jsonToComponent(previous);
			if (component instanceof ScoreComponent scoreComponent && scoreComponent.name().equals("hb")) {
				if (scoreComponent.objective().equals("durability")) {
					continue;
				}
			}
			newLore.add(previous);
		}
		for (String lore : durabilityLore) {
			ScoreComponent.Builder builder = Component.score().name("hb").objective("durability");
			builder.append(AdventureHelper.miniMessage(lore.replace("{dur}", String.valueOf(customMaxDamage - value))
					.replace("{max}", String.valueOf(customMaxDamage))));
			newLore.add(AdventureHelper.componentToJson(builder.build()));
		}
		item.lore(newLore);
	}

	@Override
	public int damage() {
		int customMaxDamage = (int) item.getTag("HellblockItem", "max_dur").get();
		return customMaxDamage - (int) item.getTag("HellblockItem", "cur_dur").orElse(0);
	}

	@Override
	public int maxDamage() {
		return (int) item.getTag("HellblockItem", "max_dur").get();
	}
}