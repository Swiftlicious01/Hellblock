package com.swiftlicious.hellblock.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.saicone.rtag.item.ItemTagStream;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.ItemEditor;
import com.swiftlicious.hellblock.creation.item.tag.TagMapInterface;
import com.swiftlicious.hellblock.creation.item.tag.TagValueType;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import static com.swiftlicious.hellblock.utils.TagUtils.toTypeAndData;
import static com.swiftlicious.hellblock.utils.ArrayUtils.splitValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ItemStackUtils {

	private ItemStackUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	public static ItemStack deserialize(String base64) {
		if (base64 == null || base64.isEmpty())
			return new ItemStack(Material.AIR);
		ByteArrayInputStream inputStream;
		try {
			inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
		} catch (IllegalArgumentException e) {
			return new ItemStack(Material.AIR);
		}
		ItemStack stack = null;
		try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
			stack = (ItemStack) dataInput.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return stack;
	}

	public static String serialize(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return "";
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
			dataOutput.writeObject(itemStack);
			byte[] byteArr = outputStream.toByteArray();
			dataOutput.close();
			outputStream.close();
			return Base64Coder.encodeLines(byteArr);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static Map<String, Object> itemStackToMap(ItemStack itemStack) {
		Map<String, Object> map = ItemTagStream.INSTANCE.toMap(itemStack);
		map.remove("rtagDataVersion");
		map.remove("count");
		map.remove("id");
		map.put("material", itemStack.getType().name().toLowerCase(Locale.ENGLISH));
		map.put("amount", itemStack.getAmount());
		Object tag = map.remove("tag");
		if (tag != null) {
			map.put("nbt", tag);
		}
		return map;
	}

	private static void sectionToMap(Section section, Map<String, Object> outPut) {
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (entry.getValue() instanceof Section inner) {
				Map<String, Object> map = new HashMap<>();
				outPut.put(entry.getKey(), map);
				sectionToMap(inner, map);
			} else {
				outPut.put(entry.getKey(), entry.getValue());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void sectionToComponentEditor(Section section, List<ItemEditor> itemEditors) {
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			String component = entry.getKey();
			if (VersionHelper.isVersionNewerThan1_21_5() && component.equals("minecraft:hide_tooltip")) {
				itemEditors.add((item, context) -> {
					item.setComponent("minecraft:tooltip_display", Map.of("hide_tooltip", true));
				});
				continue;
			}
			Object value = entry.getValue();
			if (value instanceof Section inner) {
				Map<String, Object> innerMap = new HashMap<>();
				sectionToMap(inner, innerMap);
				TagMapInterface tagMap = TagMapInterface.of(innerMap);
				itemEditors.add(((item, context) -> item.setComponent(component, tagMap.apply(context))));
			} else if (value instanceof List<?> list) {
				Object first = list.get(0);
				if (first instanceof Map<?, ?>) {
					List<TagMapInterface> output = new ArrayList<>();
					for (Object o : list) {
						Map<String, Object> innerMap = (Map<String, Object>) o;
						TagMapInterface tagMap = TagMapInterface.of(innerMap);
						output.add(tagMap);
					}
					itemEditors.add(((item, context) -> {
						List<Map<String, Object>> maps = output.stream().map(unparsed -> unparsed.apply(context))
								.toList();
						item.setComponent(component, maps);
					}));
				} else if (first instanceof String str) {
					Pair<TagValueType, String> pair = toTypeAndData(str);
					switch (pair.left()) {
					case INT -> {
						List<MathValue<Player>> values = new ArrayList<>();
						for (Object o : list) {
							values.add(MathValue.auto(toTypeAndData((String) o).right()));
						}
						itemEditors.add(((item, context) -> {
							List<Integer> integers = values.stream().map(unparsed -> (int) unparsed.evaluate(context))
									.toList();
							item.setComponent(component, integers);
						}));
					}
					case SHORT -> {
						List<MathValue<Player>> values = new ArrayList<>();
						for (Object o : list) {
							values.add(MathValue.auto(toTypeAndData((String) o).right()));
						}
						itemEditors.add(((item, context) -> {
							List<Short> shorts = values.stream().map(unparsed -> (short) unparsed.evaluate(context))
									.toList();
							item.setComponent(component, shorts);
						}));
					}
					case BYTE -> {
						List<MathValue<Player>> values = new ArrayList<>();
						for (Object o : list) {
							values.add(MathValue.auto(toTypeAndData((String) o).right()));
						}
						itemEditors.add(((item, context) -> {
							List<Byte> bytes = values.stream().map(unparsed -> (byte) unparsed.evaluate(context))
									.toList();
							item.setComponent(component, bytes);
						}));
					}
					case LONG -> {
						List<MathValue<Player>> values = new ArrayList<>();
						for (Object o : list) {
							values.add(MathValue.auto(toTypeAndData((String) o).right()));
						}
						itemEditors.add(((item, context) -> {
							List<Long> longs = values.stream().map(unparsed -> (long) unparsed.evaluate(context))
									.toList();
							item.setComponent(component, longs);
						}));
					}
					case FLOAT -> {
						List<MathValue<Player>> values = new ArrayList<>();
						for (Object o : list) {
							values.add(MathValue.auto(toTypeAndData((String) o).right()));
						}
						itemEditors.add(((item, context) -> {
							List<Float> floats = values.stream().map(unparsed -> (float) unparsed.evaluate(context))
									.toList();
							item.setComponent(component, floats);
						}));
					}
					case DOUBLE -> {
						List<MathValue<Player>> values = new ArrayList<>();
						for (Object o : list) {
							values.add(MathValue.auto(toTypeAndData((String) o).right()));
						}
						itemEditors.add(((item, context) -> {
							List<Double> doubles = values.stream().map(unparsed -> (double) unparsed.evaluate(context))
									.toList();
							item.setComponent(component, doubles);
						}));
					}
					case STRING -> {
						List<TextValue<Player>> values = new ArrayList<>();
						for (Object o : list) {
							values.add(TextValue.auto(toTypeAndData((String) o).right()));
						}
						itemEditors.add(((item, context) -> {
							List<String> texts = values.stream().map(unparsed -> unparsed.render(context)).toList();
							item.setComponent(component, texts);
						}));
					}
					default -> throw new IllegalArgumentException("Unexpected value: " + pair.left());
					}

				} else {
					itemEditors.add(((item, context) -> {
						item.setComponent(component, list);
					}));
				}
			} else if (value instanceof String str) {
				Pair<TagValueType, String> pair = toTypeAndData(str);
				switch (pair.left()) {
				case INT -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.setComponent(component, (int) mathValue.evaluate(context));
					}));
				}
				case BYTE -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.setComponent(component, (byte) mathValue.evaluate(context));
					}));
				}
				case FLOAT -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.setComponent(component, (float) mathValue.evaluate(context));
					}));
				}
				case LONG -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.setComponent(component, (long) mathValue.evaluate(context));
					}));
				}
				case SHORT -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.setComponent(component, (short) mathValue.evaluate(context));
					}));
				}
				case DOUBLE -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.setComponent(component, (double) mathValue.evaluate(context));
					}));
				}
				case STRING -> {
					TextValue<Player> textValue = TextValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.setComponent(component, textValue.render(context));
					}));
				}
				case INTARRAY -> {
					String[] split = splitValue(str);
					int[] array = Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
					itemEditors.add(((item, context) -> {
						item.setComponent(component, array);
					}));
				}
				case BYTEARRAY -> {
					String[] split = splitValue(str);
					byte[] bytes = new byte[split.length];
					for (int i = 0; i < split.length; i++) {
						bytes[i] = Byte.parseByte(split[i]);
					}
					itemEditors.add(((item, context) -> {
						item.setComponent(component, bytes);
					}));
				}
				}
			} else {
				itemEditors.add(((item, context) -> {
					item.setComponent(component, value);
				}));
			}
		}
	}

	// ugly codes, remaining improvements
	@SuppressWarnings("unchecked")
	public static void sectionToTagEditor(Section section, List<ItemEditor> itemEditors, String... route) {
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			Object value = entry.getValue();
			String key = entry.getKey();
			String[] currentRoute = ArrayUtils.appendElementToArray(route, key);
			if (value instanceof Section inner) {
				sectionToTagEditor(inner, itemEditors, currentRoute);
			} else if (value instanceof List<?> list) {
				Object first = list.get(0);
				if (first instanceof Map<?, ?>) {
					List<TagMapInterface> maps = new ArrayList<>();
					for (Object o : list) {
						Map<String, Object> map = (Map<String, Object>) o;
						maps.add(TagMapInterface.of(map));
					}
					itemEditors.add(((item, context) -> {
						List<Map<String, Object>> parsed = maps.stream().map(render -> render.apply(context)).toList();
						item.set(parsed, (Object[]) currentRoute);
					}));
				} else {
					if (first instanceof String str) {
						Pair<TagValueType, String> pair = toTypeAndData(str);
						switch (pair.left()) {
						case INT -> {
							List<MathValue<Player>> values = new ArrayList<>();
							for (Object o : list) {
								values.add(MathValue.auto(toTypeAndData((String) o).right()));
							}
							itemEditors.add(((item, context) -> {
								List<Integer> integers = values.stream()
										.map(unparsed -> (int) unparsed.evaluate(context)).toList();
								item.set(integers, (Object[]) currentRoute);
							}));
						}
						case SHORT -> {
							List<MathValue<Player>> values = new ArrayList<>();
							for (Object o : list) {
								values.add(MathValue.auto(toTypeAndData((String) o).right()));
							}
							itemEditors.add(((item, context) -> {
								List<Short> shorts = values.stream().map(unparsed -> (short) unparsed.evaluate(context))
										.toList();
								item.set(shorts, (Object[]) currentRoute);
							}));
						}
						case BYTE -> {
							List<MathValue<Player>> values = new ArrayList<>();
							for (Object o : list) {
								values.add(MathValue.auto(toTypeAndData((String) o).right()));
							}
							itemEditors.add(((item, context) -> {
								List<Byte> bytes = values.stream().map(unparsed -> (byte) unparsed.evaluate(context))
										.toList();
								item.set(bytes, (Object[]) currentRoute);
							}));
						}
						case LONG -> {
							List<MathValue<Player>> values = new ArrayList<>();
							for (Object o : list) {
								values.add(MathValue.auto(toTypeAndData((String) o).right()));
							}
							itemEditors.add(((item, context) -> {
								List<Long> longs = values.stream().map(unparsed -> (long) unparsed.evaluate(context))
										.toList();
								item.set(longs, (Object[]) currentRoute);
							}));
						}
						case FLOAT -> {
							List<MathValue<Player>> values = new ArrayList<>();
							for (Object o : list) {
								values.add(MathValue.auto(toTypeAndData((String) o).right()));
							}
							itemEditors.add(((item, context) -> {
								List<Float> floats = values.stream().map(unparsed -> (float) unparsed.evaluate(context))
										.toList();
								item.set(floats, (Object[]) currentRoute);
							}));
						}
						case DOUBLE -> {
							List<MathValue<Player>> values = new ArrayList<>();
							for (Object o : list) {
								values.add(MathValue.auto(toTypeAndData((String) o).right()));
							}
							itemEditors.add(((item, context) -> {
								List<Double> doubles = values.stream().map(unparsed -> unparsed.evaluate(context))
										.toList();
								item.set(doubles, (Object[]) currentRoute);
							}));
						}
						case STRING -> {
							List<TextValue<Player>> values = new ArrayList<>();
							for (Object o : list) {
								values.add(TextValue.auto(toTypeAndData((String) o).right()));
							}
							itemEditors.add(((item, context) -> {
								List<String> texts = values.stream().map(unparsed -> unparsed.render(context)).toList();
								item.set(texts, (Object[]) currentRoute);
							}));
						}
						default -> throw new IllegalArgumentException("Unexpected value: " + pair.left());
						}
					} else {
						itemEditors.add(((item, context) -> {
							item.set(list, (Object[]) currentRoute);
						}));
					}
				}
			} else if (value instanceof String str) {
				Pair<TagValueType, String> pair = toTypeAndData(str);
				switch (pair.left()) {
				case INT -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.set((int) mathValue.evaluate(context), (Object[]) currentRoute);
					}));
				}
				case BYTE -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.set((byte) mathValue.evaluate(context), (Object[]) currentRoute);
					}));
				}
				case LONG -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.set((long) mathValue.evaluate(context), (Object[]) currentRoute);
					}));
				}
				case SHORT -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.set((short) mathValue.evaluate(context), (Object[]) currentRoute);
					}));
				}
				case DOUBLE -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.set(mathValue.evaluate(context), (Object[]) currentRoute);
					}));
				}
				case FLOAT -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.set((float) mathValue.evaluate(context), (Object[]) currentRoute);
					}));
				}
				case STRING -> {
					TextValue<Player> textValue = TextValue.auto(pair.right());
					itemEditors.add(((item, context) -> {
						item.set(textValue.render(context), (Object[]) currentRoute);
					}));
				}
				case INTARRAY -> {
					String[] split = splitValue(str);
					int[] array = Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
					itemEditors.add(((item, context) -> {
						item.set(array, (Object[]) currentRoute);
					}));
				}
				case BYTEARRAY -> {
					String[] split = splitValue(str);
					byte[] bytes = new byte[split.length];
					for (int i = 0; i < split.length; i++) {
						bytes[i] = Byte.parseByte(split[i]);
					}
					itemEditors.add(((item, context) -> {
						item.set(bytes, (Object[]) currentRoute);
					}));
				}
				}
			} else {
				itemEditors.add(((item, context) -> {
					item.set(value, (Object[]) currentRoute);
				}));
			}
		}
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