package com.swiftlicious.hellblock.utils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.swiftlicious.hellblock.HellblockPlugin;

public class ConfigUtils {

	private ConfigUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	@SuppressWarnings("unchecked")
	public static void mapToReadableStringList(Map<String, Object> map, List<String> readableList, boolean isMapList) {
		if (map == null || map.isEmpty()) {
			readableList.add("<red>This item has no connected NBT tag data to it from the Hellblock plugin.");
			return;
		}
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (entry.getKey().isEmpty()) {
				readableList.add("<red>This item has no connected NBT tag data to it from the Hellblock plugin.");
				break;
			}
			if (isMapList) {
				readableList.add("<gray>-=- <gold>(NBT Data Tag: <white>" + entry.getKey() + "<gold>) <gray>-=-");
			}
			Object tag = entry.getValue();
			if (tag instanceof List<?> listData) {
				for (Object innerTag : listData) {
					if (innerTag instanceof String stringData) {
						readableList.add("<white>- <red>" + entry.getKey() + ":");
						readableList
								.add("<aqua>(String) <gold>" + (stringData != null && !stringData.isEmpty() ? stringData
										: "Value returned null, please report this."));
					} else if (innerTag instanceof Integer integerData) {
						readableList.add("<white>- <red>" + entry.getKey() + ":");
						readableList.add("<aqua>(Integer) <gold>" + integerData.intValue());
					} else if (innerTag instanceof Float floatData) {
						readableList.add("<white>- <red>" + entry.getKey() + ":");
						readableList.add("<aqua>(Float) <gold>" + floatData.floatValue());
					} else if (innerTag instanceof Double doubleData) {
						readableList.add("<white>- <red>" + entry.getKey() + ":");
						readableList.add("<aqua>(Double) <gold>" + doubleData.doubleValue());
					} else if (innerTag instanceof Short shortData) {
						readableList.add("<white>- <red>" + entry.getKey() + ":");
						readableList.add("<aqua>(Short) <gold>" + shortData.shortValue());
					} else if (innerTag instanceof Long longData) {
						readableList.add("<white>- <red>" + entry.getKey() + ":");
						readableList.add("<aqua>(Long) <gold>" + longData.longValue());
					} else if (innerTag instanceof Byte byteData) {
						readableList.add("<white>- <red>" + entry.getKey() + ":");
						readableList.add("<aqua>(Boolean) <gold>" + (byteData.byteValue() == 1 ? "true" : "false"));
					} else if (innerTag instanceof UUID uuidData) {
						readableList.add("<white>- <red>" + entry.getKey() + ":");
						readableList.add("<aqua>(UUID) <gold>" + (uuidData != null ? uuidData.toString()
								: "Value returned null, please report this."));
					} else if (innerTag instanceof Map<?, ?> mapData) {
						mapToReadableStringList((Map<String, Object>) mapData, readableList, false);
					} else {
						readableList
								.add("<red>This item has no connected NBT tag data to it from the Hellblock plugin.");
						HellblockPlugin.getInstance().getPluginLogger().warn(
								String.format("This class is not recognized as tag data: %s", innerTag.getClass()));
						break;
					}
				}
			} else if (tag instanceof Map<?, ?> mapData) {
				mapToReadableStringList((Map<String, Object>) mapData, readableList, false);
			} else if (tag instanceof String stringData) {
				readableList.add("<white>- <red>" + entry.getKey() + ":");
				readableList.add("<aqua>(String) <gold>" + (stringData != null && !stringData.isEmpty() ? stringData
						: "Value returned null, please report this."));
			} else if (tag instanceof Integer integerData) {
				readableList.add("<white>- <red>" + entry.getKey() + ":");
				readableList.add("<aqua>(Integer) <gold>" + integerData.intValue());
			} else if (tag instanceof Float floatData) {
				readableList.add("<white>- <red>" + entry.getKey() + ":");
				readableList.add("<aqua>(Float) <gold>" + floatData.floatValue());
			} else if (tag instanceof Double doubleData) {
				readableList.add("<white>- <red>" + entry.getKey() + ":");
				readableList.add("<aqua>(Double) <gold>" + doubleData.doubleValue());
			} else if (tag instanceof Short shortData) {
				readableList.add("<white>- <red>" + entry.getKey() + ":");
				readableList.add("<aqua>(Short) <gold>" + shortData.shortValue());
			} else if (tag instanceof Long longData) {
				readableList.add("<white>- <red>" + entry.getKey() + ":");
				readableList.add("<aqua>(Long) <gold>" + longData.longValue());
			} else if (tag instanceof Byte byteData) {
				readableList.add("<white>- <red>" + entry.getKey() + ":");
				readableList.add("<aqua>(Boolean) <gold>" + (byteData.byteValue() == 1 ? "true" : "false"));
			} else if (tag instanceof UUID uuidData) {
				readableList.add("<white>- <red>" + entry.getKey() + ":");
				readableList.add("<aqua>(UUID) <gold>"
						+ (uuidData != null ? uuidData.toString() : "Value returned null, please report this."));
			} else {
				readableList.add("<red>This item has no connected NBT tag data to it from the Hellblock plugin.");
				HellblockPlugin.getInstance().getPluginLogger()
						.warn(String.format("This class is not recognized as tag data: %s", tag.getClass()));
				break;
			}
		}
	}
}