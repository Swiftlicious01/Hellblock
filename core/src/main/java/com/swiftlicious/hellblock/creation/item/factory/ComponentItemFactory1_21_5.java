package com.swiftlicious.hellblock.creation.item.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.saicone.rtag.RtagItem;
import com.saicone.rtag.data.ComponentType;
import com.saicone.rtag.tag.TagList;
import com.saicone.rtag.util.ChatComponent;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;

public class ComponentItemFactory1_21_5 extends ComponentItemFactory {

	public ComponentItemFactory1_21_5(HellblockPlugin plugin) {
		super(plugin);
	}

	@Override
	protected void displayName(RtagItem item, String json) {
		if (json == null) {
			item.removeComponent(ComponentKeys.CUSTOM_NAME);
		} else {
			item.setComponent(ComponentKeys.CUSTOM_NAME,
					ChatComponent.toTag(VersionHelper.getNMSManager().getMinecraftComponent(json)));
		}
	}

	@Override
	protected Optional<String> displayName(RtagItem item) {
		if (!item.hasComponent(ComponentKeys.CUSTOM_NAME))
			return Optional.empty();
		return ComponentType.encodeJson(ComponentKeys.CUSTOM_NAME, item.getComponent(ComponentKeys.CUSTOM_NAME))
				.map(jsonElement -> AdventureHelper.getGson().serializer().toJson(jsonElement));
	}

	@Override
	protected Optional<List<String>> lore(RtagItem item) {
		if (!item.hasComponent(ComponentKeys.LORE))
			return Optional.empty();
		return ComponentType.encodeJson(ComponentKeys.LORE, item.getComponent(ComponentKeys.LORE)).map(list -> {
			List<String> lore = new ArrayList<>();
			for (JsonElement jsonElement : (JsonArray) list) {
				lore.add(AdventureHelper.getGson().serializer().toJson(jsonElement));
			}
			return lore;
		});
	}

	@Override
	protected void lore(RtagItem item, List<String> lore) {
		if (lore == null || lore.isEmpty()) {
			item.removeComponent(ComponentKeys.LORE);
		} else {
			List<Object> loreTags = new ArrayList<>();
			for (String json : lore) {
				loreTags.add(ChatComponent.toTag(VersionHelper.getNMSManager().getMinecraftComponent(json)));
			}
			item.setComponent(ComponentKeys.LORE, TagList.newTag(loreTags));
		}
	}
}
