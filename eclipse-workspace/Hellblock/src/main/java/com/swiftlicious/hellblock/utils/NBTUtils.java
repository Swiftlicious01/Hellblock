package com.swiftlicious.hellblock.utils;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.saicone.rtag.data.ComponentType;
import com.swiftlicious.hellblock.HellblockPlugin;

import lombok.NonNull;

/**
 * Utility class for working with NBT (Named Binary Tag) data.
 */
public class NBTUtils {

	private NBTUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Inner class representing a stack element used during NBT data conversion.
	 */
	public static class StackElement {
		final Map<String, Object> currentMap;
		final RtagItem currentRtagItem;

		StackElement(Map<String, Object> map, RtagItem rtagItem) {
			this.currentMap = map;
			this.currentRtagItem = rtagItem;
		}
	}

	/**
	 * Converts data from a Bukkit YAML configuration to NBT tags.
	 *
	 * @param nbtItem The target NBT compound
	 * @param map     The source map from Bukkit YAML
	 */
	@SuppressWarnings("unchecked")
	public static void setTagsFromBukkitYAML(Player player, Map<String, String> placeholders, RtagItem tag,
			Map<String, Object> map) {

		Deque<StackElement> stack = new ArrayDeque<>();
		stack.push(new StackElement(map, tag));

		while (!stack.isEmpty()) {
			StackElement currentElement = stack.pop();
			Map<String, Object> currentMap = currentElement.currentMap;
			RtagItem currentTag = currentElement.currentRtagItem;

			for (Map.Entry<String, Object> entry : currentMap.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				if (value instanceof MemorySection memorySection) {
					stack.push(new StackElement(memorySection.getValues(false), currentTag.get(key)));
				} else if (value instanceof List<?> list) {
					for (Object o : list) {
						if (o instanceof String stringValue) {
							setListValue(player, placeholders, key, stringValue, currentTag);
						} else if (o instanceof Map<?, ?> mapValue) {
							RtagItem tagCompound = currentTag.get(key);
							stack.push(new StackElement((Map<String, Object>) mapValue, tagCompound));
						}
					}
				} else if (value instanceof String stringValue) {
					setSingleValue(player, placeholders, key, stringValue, currentTag);
				}
			}
		}
	}

	// Private helper method
	private static <T> void setListValue(Player player, Map<String, String> placeholders, String key, String value,
			RtagItem tag) {
		String[] parts = getTypeAndData(value);
		String type = parts[0];
		String data = getParsedData(player, placeholders, parts[1]);
		switch (type) {
		case "String" -> {
			tag.add(data, key);
			tag.load();
			tag.update();
		}
		case "UUID" -> {
			tag.add(UUID.fromString(data), key);
			tag.load();
			tag.update();
		}
		case "Double" -> {
			tag.add(Double.parseDouble(data), key);
			tag.load();
			tag.update();
		}
		case "Long" -> {
			tag.add(Long.parseLong(data), key);
			tag.load();
			tag.update();
		}
		case "Float" -> {
			tag.add(Float.parseFloat(data), key);
			tag.load();
			tag.update();
		}
		case "Int" -> {
			tag.add(Integer.parseInt(data), key);
			tag.load();
			tag.update();
		}
		case "IntArray" -> {
			String[] split = data.replace("[", "").replace("]", "").replaceAll("\\s", "").split(",");
			int[] array = Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
			tag.add(array, key);
			tag.load();
			tag.update();
		}
		default -> throw new IllegalArgumentException("Invalid value type: " + type);
		}
	}

	// Private helper method
	private static <T> void setSingleValue(Player player, Map<String, String> placeholders, String key, String value,
			RtagItem tag) {
		String[] parts = getTypeAndData(value);
		String type = parts[0];
		String data = getParsedData(player, placeholders, parts[1]);
		switch (type) {
		case "Int" -> {
			tag.set(Integer.parseInt(data), key);
			tag.load();
			tag.update();
		}
		case "String" -> {
			tag.set(data, key);
			tag.load();
			tag.update();
		}
		case "Long" -> {
			tag.set(Long.parseLong(data), key);
			tag.load();
			tag.update();
		}
		case "Float" -> {
			tag.set(Float.parseFloat(data), key);
			tag.load();
			tag.update();
		}
		case "Double" -> {
			tag.set(Double.parseDouble(data), key);
			tag.load();
			tag.update();
		}
		case "Short" -> {
			tag.set(Short.parseShort(data), key);
			tag.load();
			tag.update();
		}
		case "Boolean" -> {
			tag.set(Boolean.parseBoolean(data), key);
			tag.load();
			tag.update();
		}
		case "UUID" -> {
			tag.set(UUID.nameUUIDFromBytes(data.getBytes()), key);
			tag.load();
			tag.update();
		}
		case "Byte" -> {
			tag.set(Byte.parseByte(data), key);
			tag.load();
			tag.update();
		}
		case "ByteArray" -> {
			String[] split = splitValue(value);
			byte[] bytes = new byte[split.length];
			for (int i = 0; i < split.length; i++) {
				bytes[i] = Byte.parseByte(split[i]);
			}
			tag.set(bytes, key);
			tag.load();
			tag.update();
		}
		case "IntArray" -> {
			String[] split = splitValue(value);
			int[] array = Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
			tag.set(array, key);
			tag.load();
			tag.update();
		}
		default -> throw new IllegalArgumentException("Invalid value type: " + type);
		}
	}

	public static String getParsedData(Player player, Map<String, String> placeholders, String data) {
		if (data.length() >= 3)
			switch (data.substring(0, 3)) {
			case "-P:" -> data = HellblockPlugin.getInstance().getPlaceholderManager().parse(player, data.substring(3),
					placeholders);
			case "-E:" -> {
				double value = HellblockPlugin.getInstance().getConfigUtils().getExpressionValue(player,
						data.substring(3), placeholders);
				if (value % 1 == 0) {
					data = Long.toString((long) value);
				} else {
					data = Double.toString(value);
				}
			}
			}
		return data;
	}

	/**
	 * Converts an NBT compound to a map of key-value pairs.
	 *
	 * @param nbtCompound The source NBT compound
	 * @return A map representing the NBT data
	 */
	// TODO: Present in a more readable fashion
	public static Map<String, Object> compoundToMap(RtagItem tag) {
		if (tag.get() == null)
			return new HashMap<>();
		Map<String, Object> map = new HashMap<>(tag.get());
		return map;
	}

	/**
	 * Splits a value into type and data components.
	 * 
	 * @param <T>
	 *
	 * @param value The input value string
	 * @return An array containing type and data strings
	 */
	public static String[] getTypeAndData(String value) {
		String[] parts = value.split("\\s+", 2);
		if (parts.length == 1) {
			return new String[] { "String", value };
		}
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid value format: " + value);
		}
		String type = parts[0].substring(1, parts[0].length() - 1);
		String data = parts[1];
		return new String[] { type, data };
	}

	/**
	 * Splits a value containing arrays into individual elements.
	 * 
	 * @param <T>
	 *
	 * @param value The input value containing arrays
	 * @return An array of individual elements
	 */
	public static String[] splitValue(String value) {
		return value.substring(value.indexOf('[') + 1, value.lastIndexOf(']')).replaceAll("\\s", "").split(",");
	}

	/**
	 * Retrieves the item's nbt data
	 * 
	 * @param <T>         The type of data it is
	 * @param bukkitStack The item to get data from
	 * @param key         The path to the data
	 * @return the data in the specific form used
	 */
	public static @Nullable <T> Object getNBTItemComponentData(@NonNull ItemStack bukkitStack, Object... key) {
		RtagItem tag = new RtagItem(bukkitStack);
		Object component = tag.getComponent("minecraft:" + key);
		return ComponentType.encodeJava("minecraft:" + key, component).orElse(null);
	}

	/**
	 * Updates the item's nbt data
	 * 
	 * @param <T>         The type of data it is
	 * @param bukkitStack The item to update
	 * @param value       The data type you want to set
	 * @param key         The path to the data
	 */
	public static <T> void setNBTItemComponentData(@NonNull ItemStack bukkitStack, T value, Object... key) {
		RtagItem tag = new RtagItem(bukkitStack);
		tag.setComponent("minecraft:" + key, value);
		tag.load();
		tag.update();
	}

	/**
	 * Removes data from the item
	 * 
	 * @param bukkitStack The item to update
	 * @param key         The path to the data
	 */
	public static void removeNBTItemComponentData(@NonNull ItemStack bukkitStack, Object... key) {
		RtagItem tag = new RtagItem(bukkitStack);
		tag.removeComponent("minecraft:" + key);
		tag.load();
		tag.update();
	}

	/**
	 * Checks for data on the item
	 * 
	 * @param bukkitStack The item to check
	 * @param key         The path to the data
	 * @return Whether or not there was data on the item
	 */
	public static boolean hasNBTItemComponentData(@NonNull ItemStack bukkitStack, Object... key) {
		RtagItem tag = new RtagItem(bukkitStack);
		return tag.hasComponent("minecraft:" + key) && tag.getComponent("minecraft:" + key) != null;
	}
}
