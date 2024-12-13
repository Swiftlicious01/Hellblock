package com.swiftlicious.hellblock.creation.item.tag;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import com.swiftlicious.hellblock.context.Context;

import java.util.Map;

@ApiStatus.Internal
public interface TagMapInterface {

	Map<String, Object> apply(Context<Player> context);

	static TagMapInterface of(Map<String, Object> inputMap) {
		return new TagMap(inputMap);
	}
}