package com.swiftlicious.hellblock.creation.item.factory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.inventory.ItemFlag;

import com.saicone.rtag.RtagItem;
import com.saicone.rtag.tag.TagBase;
import com.saicone.rtag.tag.TagCompound;
import com.saicone.rtag.tag.TagList;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.SkullUtils;
import com.swiftlicious.hellblock.utils.extras.Key;

public class UniversalItemFactory extends BukkitItemFactory {

	public UniversalItemFactory(HellblockPlugin plugin) {
		super(plugin);
	}

	@Override
	protected void displayName(RtagItem item, String json) {
		if (json != null) {
			item.set(json, "display", "Name");
		} else {
			item.remove("display", "Name");
		}
	}

	@Override
	protected Optional<String> displayName(RtagItem item) {
		if (!item.hasTag("display", "Name"))
			return Optional.empty();
		return Optional.of(item.get("display", "Name"));
	}

	@Override
	protected void customModelData(RtagItem item, Integer data) {
		if (data == null) {
			item.remove("CustomModelData");
		} else {
			item.set(data, "CustomModelData");
		}
	}

	@Override
	protected Optional<Integer> customModelData(RtagItem item) {
		if (!item.hasTag("CustomModelData"))
			return Optional.empty();
		return Optional.of(item.get("CustomModelData"));
	}

	@Override
	protected void skull(RtagItem item, String skullData) {
		if (skullData == null) {
			item.remove("SkullOwner");
		} else {
			item.set(
					UUID.nameUUIDFromBytes(SkullUtils.identifierFromBase64(skullData).getBytes(StandardCharsets.UTF_8)),
					"SkullOwner", "Id");
			item.set(List.of(Map.of("Value", skullData)), "SkullOwner", "Properties", "textures");
		}
	}

	@Override
	protected void potionEffect(RtagItem item, String effect) {
		if (effect == null) {
			item.remove("Potion");
		} else {
			item.set(effect, "Potion");
		}
	}

	@Override
	protected void potionColor(RtagItem item, int color) {
		item.set(color, "CustomPotionColor");
	}

	@Override
	protected Optional<List<String>> lore(RtagItem item) {
		if (!item.hasTag("display", "Lore"))
			return Optional.empty();
		return Optional.of(item.get("display", "Lore"));
	}

	@Override
	protected void lore(RtagItem item, List<String> lore) {
		if (lore == null || lore.isEmpty()) {
			item.remove("display", "Lore");
		} else {
			item.set(lore, "display", "Lore");
		}
	}

	@Override
	protected boolean unbreakable(RtagItem item) {
		return item.isUnbreakable();
	}

	@Override
	protected void unbreakable(RtagItem item, boolean unbreakable) {
		item.setUnbreakable(unbreakable);
	}

	@Override
	protected Optional<Boolean> glint(RtagItem item) {
		return Optional.of(false);
	}

	@Override
	protected void glint(RtagItem item, Boolean glint) {
		throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
	}

	@Override
	protected Optional<Integer> damage(RtagItem item) {
		if (!item.hasTag("Damage"))
			return Optional.empty();
		return Optional.of(item.get("Damage"));
	}

	@Override
	protected void damage(RtagItem item, Integer damage) {
		item.set(damage, "Damage");
	}

	@Override
	protected Optional<Integer> maxDamage(RtagItem item) {
//        if (!item.hasTag("HellFishing", "max_dur")) return Optional.empty();
//        return Optional.of(item.get("HellFishing", "max_dur"));
		return Optional.of((int) item.getItem().getType().getMaxDurability());
	}

	@Override
	protected void maxDamage(RtagItem item, Integer damage) {
//        if (damage == null) {
//            item.remove("HellFishing", "max_dur");
//        } else {
//            item.set(damage, "HellFishing", "max_dur");
//        }
		throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
	}

	@Override
	protected void enchantments(RtagItem item, Map<Key, Short> enchantments) {
		List<Object> tags = new ArrayList<>();
		for (Map.Entry<Key, Short> entry : enchantments.entrySet()) {
			tags.add((Map.of("id", entry.getKey().toString(), "lvl", entry.getValue())));
		}
		item.set(tags, "Enchantments");
	}

	@Override
	protected void storedEnchantments(RtagItem item, Map<Key, Short> enchantments) {
		List<Object> tags = new ArrayList<>();
		for (Map.Entry<Key, Short> entry : enchantments.entrySet()) {
			tags.add((Map.of("id", entry.getKey().toString(), "lvl", entry.getValue())));
		}
		item.set(tags, "StoredEnchantments");
	}

	@Override
	protected void addEnchantment(RtagItem item, Key enchantment, int level) {
		Object enchantments = item.getExact("Enchantments");
		if (enchantments != null) {
			for (Object enchant : TagList.getValue(enchantments)) {
				if (TagBase.getValue(TagCompound.get(enchant, "id")).equals(enchant.toString())) {
					TagCompound.set(enchant, "lvl", TagBase.newTag(level));
					return;
				}
			}
			item.add(Map.of("id", enchantment.toString(), "lvl", (short) level), "Enchantments");
		} else {
			item.set(List.of(Map.of("id", enchantment.toString(), "lvl", (short) level)), "Enchantments");
		}
	}

	@Override
	protected void addStoredEnchantment(RtagItem item, Key enchantment, int level) {
		Object enchantments = item.getExact("StoredEnchantments");
		if (enchantments != null) {
			for (Object enchant : TagList.getValue(enchantments)) {
				if (TagBase.getValue(TagCompound.get(enchant, "id")).equals(enchant.toString())) {
					TagCompound.set(enchant, "lvl", TagBase.newTag(level));
					return;
				}
			}
			item.add(Map.of("id", enchantment.toString(), "lvl", (short) level), "StoredEnchantments");
		} else {
			item.set(List.of(Map.of("id", enchantment.toString(), "lvl", (short) level)), "StoredEnchantments");
		}
	}

	@Override
	protected void itemFlags(RtagItem item, List<String> flags) {
		if (flags == null || flags.isEmpty()) {
			item.remove("HideFlags");
			return;
		}
		int f = 0;
		for (String flag : flags) {
			ItemFlag itemFlag = ItemFlag.valueOf(flag);
			f = f | 1 << itemFlag.ordinal();
		}
		item.set(f, "HideFlags");
	}
}