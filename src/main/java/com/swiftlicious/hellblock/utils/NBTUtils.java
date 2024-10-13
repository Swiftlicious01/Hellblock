package com.swiftlicious.hellblock.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;

import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;

/**
 * Utility class for working with NBT (Named Binary Tag) data.
 */
public class NBTUtils {

	private NBTUtils() {
	}

	/**
	 * Inner class representing a stack element used during NBT data conversion.
	 */
	public static class StackElement {
		final Map<String, Object> currentMap;
		final ReadWriteNBT currentNbtCompound;

		StackElement(Map<String, Object> map, ReadWriteNBT nbtItem) {
			this.currentMap = map;
			this.currentNbtCompound = nbtItem;
		}
	}

	/**
	 * Converts data from a Bukkit YAML configuration to NBT tags.
	 *
	 * @param nbtItem The target NBT compound
	 * @param map         The source map from Bukkit YAML
	 */
	@SuppressWarnings("unchecked")
	public static void setTagsFromBukkitYAML(Player player, Map<String, String> placeholders, ReadWriteNBT nbtItem,
			Map<String, Object> map) {

		Deque<StackElement> stack = new ArrayDeque<>();
		stack.push(new StackElement(map, nbtItem));

		while (!stack.isEmpty()) {
			StackElement currentElement = stack.pop();
			Map<String, Object> currentMap = currentElement.currentMap;
			ReadWriteNBT currentNbtCompound = currentElement.currentNbtCompound;

			for (Map.Entry<String, Object> entry : currentMap.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				if (value instanceof MemorySection memorySection) {
					stack.push(new StackElement(memorySection.getValues(false), currentNbtCompound.resolveOrCreateCompound(key)));
				} else if (value instanceof List<?> list) {
					for (Object o : list) {
						if (o instanceof String stringValue) {
							setListValue(player, placeholders, key, stringValue, currentNbtCompound);
						} else if (o instanceof Map<?, ?> mapValue) {
							ReadWriteNBT nbtListCompound = currentNbtCompound.getCompoundList(key).addCompound();
							stack.push(new StackElement((Map<String, Object>) mapValue, nbtListCompound));
						}
					}
				} else if (value instanceof String stringValue) {
					setSingleValue(player, placeholders, key, stringValue, currentNbtCompound);
				}
			}
		}
	}

	// Private helper method
	private static void setListValue(Player player, Map<String, String> placeholders, String key, String value,
			ReadWriteNBT nbtCompound) {
		String[] parts = getTypeAndData(value);
		String type = parts[0];
		String data = getParsedData(player, placeholders, parts[1]);
		switch (type) {
		case "String" -> nbtCompound.getStringList(key).add(data);
		case "UUID" -> nbtCompound.getUUIDList(key).add(UUID.fromString(data));
		case "Double" -> nbtCompound.getDoubleList(key).add(Double.parseDouble(data));
		case "Long" -> nbtCompound.getLongList(key).add(Long.parseLong(data));
		case "Float" -> nbtCompound.getFloatList(key).add(Float.parseFloat(data));
		case "Int" -> nbtCompound.getIntegerList(key).add(Integer.parseInt(data));
		case "IntArray" -> {
			String[] split = data.replace("[", "").replace("]", "").replaceAll("\\s", "").split(",");
			int[] array = Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
			nbtCompound.getIntArrayList(key).add(array);
		}
		default -> throw new IllegalArgumentException("Invalid value type: " + type);
		}
	}

	// Private helper method
	private static void setSingleValue(Player player, Map<String, String> placeholders, String key, String value,
			ReadWriteNBT nbtCompound) {
		String[] parts = getTypeAndData(value);
		String type = parts[0];
		String data = getParsedData(player, placeholders, parts[1]);
		switch (type) {
		case "Int" -> nbtCompound.setInteger(key, Integer.parseInt(data));
		case "String" -> nbtCompound.setString(key, data);
		case "Long" -> nbtCompound.setLong(key, Long.parseLong(data));
		case "Float" -> nbtCompound.setFloat(key, Float.parseFloat(data));
		case "Double" -> nbtCompound.setDouble(key, Double.parseDouble(data));
		case "Short" -> nbtCompound.setShort(key, Short.parseShort(data));
		case "Boolean" -> nbtCompound.setBoolean(key, Boolean.parseBoolean(data));
		case "UUID" -> nbtCompound.setUUID(key, UUID.nameUUIDFromBytes(data.getBytes()));
		case "Byte" -> nbtCompound.setByte(key, Byte.parseByte(data));
		case "ByteArray" -> {
			String[] split = splitValue(value);
			byte[] bytes = new byte[split.length];
			for (int i = 0; i < split.length; i++) {
				bytes[i] = Byte.parseByte(split[i]);
			}
			nbtCompound.setByteArray(key, bytes);
		}
		case "IntArray" -> {
			String[] split = splitValue(value);
			int[] array = Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
			nbtCompound.setIntArray(key, array);
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
	public static Map<String, Object> compoundToMap(ReadWriteNBT nbtCompound) {
		Map<String, Object> map = new HashMap<>();
		for (String key : nbtCompound.getKeys()) {
			switch (nbtCompound.getType(key)) {
			case NBTTagByte -> map.put(key, "(Byte) " + nbtCompound.getByte(key));
			case NBTTagInt -> map.put(key, "(Int) " + nbtCompound.getInteger(key));
			case NBTTagDouble -> map.put(key, "(Double) " + nbtCompound.getDouble(key));
			case NBTTagLong -> map.put(key, "(Long) " + nbtCompound.getLong(key));
			case NBTTagFloat -> map.put(key, "(Float) " + nbtCompound.getFloat(key));
			case NBTTagShort -> map.put(key, "(Short) " + nbtCompound.getShort(key));
			case NBTTagString -> map.put(key, "(String) " + nbtCompound.getString(key));
			case NBTTagByteArray -> map.put(key, "(ByteArray) " + Arrays.toString(nbtCompound.getByteArray(key)));
			case NBTTagIntArray -> map.put(key, "(IntArray) " + Arrays.toString(nbtCompound.getIntArray(key)));
			case NBTTagCompound -> {
				Map<String, Object> map1 = compoundToMap(Objects.requireNonNull(nbtCompound.getCompound(key)));
				if (map1.size() != 0)
					map.put(key, map1);
			}
			case NBTTagList -> {
				List<Object> list = new ArrayList<>();
				switch (Objects.requireNonNull(nbtCompound.getListType(key))) {
				case NBTTagCompound -> nbtCompound.getCompoundList(key).forEach(a -> list.add(compoundToMap(a)));
				case NBTTagInt -> nbtCompound.getIntegerList(key).forEach(a -> list.add("(Int) " + a));
				case NBTTagDouble -> nbtCompound.getDoubleList(key).forEach(a -> list.add("(Double) " + a));
				case NBTTagString -> nbtCompound.getStringList(key).forEach(a -> list.add("(String) " + a));
				case NBTTagFloat -> nbtCompound.getFloatList(key).forEach(a -> list.add("(Float) " + a));
				case NBTTagLong -> nbtCompound.getLongList(key).forEach(a -> list.add("(Long) " + a));
				case NBTTagIntArray ->
					nbtCompound.getIntArrayList(key).forEach(a -> list.add("(IntArray) " + Arrays.toString(a)));
				default -> throw new IllegalArgumentException(
						"Unexpected value: " + Objects.requireNonNull(nbtCompound.getListType(key)));
				}
				if (list.size() != 0)
					map.put(key, list);
			}
			default -> throw new IllegalArgumentException("Unexpected value: " + nbtCompound.getType(key));
			}
		}
		return map;
	}

	/**
	 * Splits a value into type and data components.
	 *
	 * @param str The input value string
	 * @return An array containing type and data strings
	 */
	public static String[] getTypeAndData(String str) {
		String[] parts = str.split("\\s+", 2);
		if (parts.length == 1) {
			return new String[] { "String", str };
		}
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid value format: " + str);
		}
		String type = parts[0].substring(1, parts[0].length() - 1);
		String data = parts[1];
		return new String[] { type, data };
	}

	/**
	 * Splits a value containing arrays into individual elements.
	 *
	 * @param value The input value containing arrays
	 * @return An array of individual elements
	 */
	public static String[] splitValue(String value) {
		return value.substring(value.indexOf('[') + 1, value.lastIndexOf(']')).replaceAll("\\s", "").split(",");
	}
}
