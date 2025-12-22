package com.swiftlicious.hellblock.utils;

import static com.swiftlicious.hellblock.utils.ArrayUtils.splitValue;
import static com.swiftlicious.hellblock.utils.TagUtils.toTypeAndData;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.saicone.rtag.item.ItemTagStream;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.ItemEditor;
import com.swiftlicious.hellblock.creation.item.tag.TagMapInterface;
import com.swiftlicious.hellblock.creation.item.tag.TagValueType;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ItemStackUtils {

	private ItemStackUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	// Deserializes a YAML string into an ItemStack
	public static ItemStack deserialize(String yamlString) {
		if (yamlString == null || yamlString.isEmpty()) {
			return new ItemStack(Material.AIR);
		}

		final YamlConfiguration config = new YamlConfiguration();
		try {
			config.loadFromString(yamlString);
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
			return new ItemStack(Material.AIR);
		}

		final Map<String, Object> itemMap = config.getConfigurationSection("item").getValues(false);
		return ItemStack.deserialize(itemMap);
	}

	// Serializes an ItemStack into a YAML string
	public static String serialize(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return "";
		}

		final Map<String, Object> itemMap = itemStack.serialize();

		final YamlConfiguration config = new YamlConfiguration();
		config.set("item", itemMap);
		return config.saveToString();
	}

	// Serializes an ItemStack and encodes it into a Base64 string
	public static String serializeToBase64(ItemStack itemStack) {
		final String yaml = serialize(itemStack);
		return Base64.getEncoder().encodeToString(yaml.getBytes(StandardCharsets.UTF_8));
	}

	// Decodes a Base64 string and deserializes it into an ItemStack
	public static ItemStack deserializeFromBase64(String base64) {
		if (base64 == null || base64.isEmpty()) {
			return new ItemStack(Material.AIR);
		}
		final String yaml = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
		return deserialize(yaml);
	}

	public static Map<String, Object> itemStackToMap(ItemStack itemStack) {
		final Map<String, Object> map = ItemTagStream.INSTANCE.toMap(itemStack);
		map.remove("rtagDataVersion");
		map.remove("count");
		map.remove("id");
		map.put("material", itemStack.getType().name().toLowerCase(Locale.ENGLISH));
		map.put("amount", itemStack.getAmount());
		final Object tag = map.remove("tag");
		if (tag != null) {
			map.put("nbt", tag);
		}
		return map;
	}

	// Converts a Section to a Map recursively
	private static void sectionToMap(Section section, Map<String, Object> outPut) {
		section.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
			if (entry.getValue() instanceof Section inner) {
				final Map<String, Object> map = new HashMap<>();
				outPut.put(entry.getKey(), map);
				sectionToMap(inner, map);
			} else {
				outPut.put(entry.getKey(), entry.getValue());
			}
		});
	}

	@SuppressWarnings("unchecked")
	// Converts a Section to ItemEditors recursively
	public static void sectionToComponentEditor(Section section, List<ItemEditor> itemEditors) {
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			final String component = entry.getKey();
			if (VersionHelper.isVersionNewerThan1_21_5() && "minecraft:hide_tooltip".equals(component)) {
				itemEditors.add((item, context) -> item.setComponent("minecraft:tooltip_display",
						Map.of("hide_tooltip", true)));
				continue;
			}
			if (VersionHelper.isVersionNewerThan1_21_5() && "minecraft:hide_additional_tooltip".equals(component)) {
				itemEditors.add((item, context) -> {
					// never hide the following: minecraft:item_name, minecraft:lore,
					// minecraft:item_model, minecraft:custom_data, minecraft:custom_name,
					// minecraft:custom_model_data, minecraft:tooltip_style,
					// minecraft:tooltip_display, minecraft:profile, minecraft:potion_contents
					List<String> hiddenComponents = new ArrayList<>(List.of("minecraft:attribute_modifiers",
							"minecraft:banner_patterns", "minecraft:base_color", "minecraft:bees",
							"minecraft:block_entity_data", "minecraft:block_state", "minecraft:blocks_attacks",
							"minecraft:break_sound", "minecraft:bucket_entity_data", "minecraft:bundle_contents",
							"minecraft:can_break", "minecraft:can_place_on", "minecraft:charged_projectiles",
							"minecraft:consumable", "minecraft:container", "minecraft:container_loot",
							"minecraft:damage", "minecraft:damage_resistant", "minecraft:debug_stick_state",
							"minecraft:death_protection", "minecraft:dyed_color", "minecraft:enchantable",
							"minecraft:enchantment_glint_override", "minecraft:enchantments", "minecraft:entity_data",
							"minecraft:equippable", "minecraft:firework_explosion", "minecraft:fireworks",
							"minecraft:food", "minecraft:glider", "minecraft:instrument",
							"minecraft:intangible_projectile", "minecraft:jukebox_playable", "minecraft:lock",
							"minecraft:lodestone_tracker", "minecraft:map_color", "minecraft:map_decorations",
							"minecraft:map_id", "minecraft:max_damage", "minecraft:max_stack_size",
							"minecraft:note_block_sound", "minecraft:ominous_bottle_amplifier",
							"minecraft:potion_duration_scale", "minecraft:pot_decorations",
							"minecraft:provides_banner_patterns", "minecraft:provides_trim_material",
							"minecraft:rarity", "minecraft:recipes", "minecraft:repair_cost", "minecraft:repairable",
							"minecraft:stored_enchantments", "minecraft:suspicious_stew_effects", "minecraft:tool",
							"minecraft:trim", "minecraft:unbreakable", "minecraft:use_cooldown",
							"minecraft:use_remainder", "minecraft:weapon", "minecraft:writable_book_content",
							"minecraft:written_book_content"));
					// 1.21.11+ components
					if (VersionHelper.isVersionNewerThan1_21_11()) {
						hiddenComponents.addAll(List.of("minecraft:attack_range", "minecraft:damage_type",
								"minecraft:kinetic_weapon", "minecraft:minimum_attack_charge", "minecraft:use_effects",
								"minecraft:piercing_weapon", "minecraft:swing_animation", "minecraft:use_effects"));
					}
					item.setComponent("minecraft:tooltip_display", Map.of("hidden_components", hiddenComponents));
				});
				continue;
			}
			final Object value = entry.getValue();
			if (value instanceof Section inner) {
				final Map<String, Object> innerMap = new HashMap<>();
				sectionToMap(inner, innerMap);
				final TagMapInterface tagMap = TagMapInterface.of(innerMap);
				itemEditors.add(((item, context) -> item.setComponent(component, tagMap.apply(context))));
			} else if (value instanceof List<?> list) {
				final Object first = list.get(0);
				if (first instanceof Map<?, ?>) {
					final List<TagMapInterface> output = new ArrayList<>(
							list.stream().map((Object o) -> (Map<String, Object>) o).map(TagMapInterface::of)
									.collect(Collectors.toList()));
					itemEditors.add(((item, context) -> {
						final List<Map<String, Object>> maps = output.stream().map(unparsed -> unparsed.apply(context))
								.toList();
						item.setComponent(component, maps);
					}));
				} else if (first instanceof String str) {
					final Pair<TagValueType, String> pair = toTypeAndData(str);
					switch (pair.left()) {
					case INT -> {
						final List<MathValue<Player>> values = new ArrayList<>();
						list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
						itemEditors.add(((item, context) -> {
							final List<Integer> integers = values.stream()
									.map(unparsed -> (int) unparsed.evaluate(context)).toList();
							item.setComponent(component, integers);
						}));
					}
					case SHORT -> {
						final List<MathValue<Player>> values = new ArrayList<>();
						list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
						itemEditors.add(((item, context) -> {
							final List<Short> shorts = values.stream()
									.map(unparsed -> (short) unparsed.evaluate(context)).toList();
							item.setComponent(component, shorts);
						}));
					}
					case BYTE -> {
						final List<MathValue<Player>> values = new ArrayList<>();
						list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
						itemEditors.add(((item, context) -> {
							final List<Byte> bytes = values.stream().map(unparsed -> (byte) unparsed.evaluate(context))
									.toList();
							item.setComponent(component, bytes);
						}));
					}
					case LONG -> {
						final List<MathValue<Player>> values = new ArrayList<>();
						list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
						itemEditors.add(((item, context) -> {
							final List<Long> longs = values.stream().map(unparsed -> (long) unparsed.evaluate(context))
									.toList();
							item.setComponent(component, longs);
						}));
					}
					case FLOAT -> {
						final List<MathValue<Player>> values = new ArrayList<>();
						list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
						itemEditors.add(((item, context) -> {
							final List<Float> floats = values.stream()
									.map(unparsed -> (float) unparsed.evaluate(context)).toList();
							item.setComponent(component, floats);
						}));
					}
					case DOUBLE -> {
						final List<MathValue<Player>> values = new ArrayList<>();
						list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
						itemEditors.add(((item, context) -> {
							final List<Double> doubles = values.stream().map(unparsed -> unparsed.evaluate(context))
									.toList();
							item.setComponent(component, doubles);
						}));
					}
					case STRING -> {
						final List<TextValue<Player>> values = new ArrayList<>();
						list.forEach((Object o) -> values.add(TextValue.auto(toTypeAndData((String) o).right())));
						itemEditors.add(((item, context) -> {
							final List<String> texts = values.stream().map(unparsed -> unparsed.render(context))
									.toList();
							item.setComponent(component, texts);
						}));
					}
					default -> throw new IllegalArgumentException("Unexpected value: " + pair.left());
					}

				} else {
					itemEditors.add(((item, context) -> item.setComponent(component, list)));
				}
			} else if (value instanceof String str) {
				final Pair<TagValueType, String> pair = toTypeAndData(str);
				switch (pair.left()) {
				case INT -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors
							.add(((item, context) -> item.setComponent(component, (int) mathValue.evaluate(context))));
				}
				case BYTE -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors
							.add(((item, context) -> item.setComponent(component, (byte) mathValue.evaluate(context))));
				}
				case FLOAT -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(
							((item, context) -> item.setComponent(component, (float) mathValue.evaluate(context))));
				}
				case LONG -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors
							.add(((item, context) -> item.setComponent(component, (long) mathValue.evaluate(context))));
				}
				case SHORT -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(
							((item, context) -> item.setComponent(component, (short) mathValue.evaluate(context))));
				}
				case DOUBLE -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> item.setComponent(component, mathValue.evaluate(context))));
				}
				case STRING -> {
					final TextValue<Player> textValue = TextValue.auto(pair.right());
					itemEditors.add(((item, context) -> item.setComponent(component, textValue.render(context))));
				}
				case INT_ARRAY -> {
					final String[] split = splitValue(str);
					final int[] array = Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
					itemEditors.add(((item, context) -> item.setComponent(component, array)));
				}
				case LONG_ARRAY -> {
					final String[] split = splitValue(str);
					final long[] array = Arrays.stream(split).mapToLong(Long::parseLong).toArray();
					itemEditors.add(((item, context) -> item.setComponent(component, array)));
				}
				case LIST -> {
					final String[] split = splitValue(str);
					final List<String> list = Arrays.asList(split);
					itemEditors.add((item, context) -> item.setComponent(component, list));
				}
				case COMPOUND -> {
					final Map<String, Object> map = parseCompoundFromString(pair.right());
					itemEditors.add((item, context) -> item.setComponent(component, map));
				}
				case BYTE_ARRAY -> {
					final String[] split = splitValue(str);
					final byte[] bytes = new byte[split.length];
					for (int i = 0; i < split.length; i++) {
						bytes[i] = Byte.parseByte(split[i]);
					}
					itemEditors.add(((item, context) -> item.setComponent(component, bytes)));
				}
				}
			} else {
				itemEditors.add(((item, context) -> item.setComponent(component, value)));
			}
		}
	}

	// ugly codes, remaining improvements
	@SuppressWarnings("unchecked")
	public static void sectionToTagEditor(Section section, List<ItemEditor> itemEditors, String... route) {
		section.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
			final Object value = entry.getValue();
			final String key = entry.getKey();
			final String[] currentRoute = ArrayUtils.appendElementToArray(route, key);
			if (value instanceof Section inner) {
				sectionToTagEditor(inner, itemEditors, currentRoute);
			} else if (value instanceof List<?> list) {
				final Object first = list.get(0);
				if (first instanceof Map<?, ?>) {
					final List<TagMapInterface> maps = new ArrayList<>();
					list.forEach((Object o) -> {
						final Map<String, Object> map = (Map<String, Object>) o;
						maps.add(TagMapInterface.of(map));
					});
					itemEditors.add(((item, context) -> {
						final List<Map<String, Object>> parsed = maps.stream().map(render -> render.apply(context))
								.toList();
						item.set(parsed, (Object[]) currentRoute);
					}));
				} else {
					if (first instanceof String str) {
						final Pair<TagValueType, String> pair = toTypeAndData(str);
						switch (pair.left()) {
						case INT -> {
							final List<MathValue<Player>> values = new ArrayList<>();
							list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
							itemEditors.add(((item, context) -> {
								final List<Integer> integers = values.stream()
										.map(unparsed -> (int) unparsed.evaluate(context)).toList();
								item.set(integers, (Object[]) currentRoute);
							}));
						}
						case SHORT -> {
							final List<MathValue<Player>> values = new ArrayList<>();
							list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
							itemEditors.add(((item, context) -> {
								final List<Short> shorts = values.stream()
										.map(unparsed -> (short) unparsed.evaluate(context)).toList();
								item.set(shorts, (Object[]) currentRoute);
							}));
						}
						case BYTE -> {
							final List<MathValue<Player>> values = new ArrayList<>();
							list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
							itemEditors.add(((item, context) -> {
								final List<Byte> bytes = values.stream()
										.map(unparsed -> (byte) unparsed.evaluate(context)).toList();
								item.set(bytes, (Object[]) currentRoute);
							}));
						}
						case LONG -> {
							final List<MathValue<Player>> values = new ArrayList<>();
							list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
							itemEditors.add(((item, context) -> {
								final List<Long> longs = values.stream()
										.map(unparsed -> (long) unparsed.evaluate(context)).toList();
								item.set(longs, (Object[]) currentRoute);
							}));
						}
						case FLOAT -> {
							final List<MathValue<Player>> values = new ArrayList<>();
							list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
							itemEditors.add(((item, context) -> {
								final List<Float> floats = values.stream()
										.map(unparsed -> (float) unparsed.evaluate(context)).toList();
								item.set(floats, (Object[]) currentRoute);
							}));
						}
						case DOUBLE -> {
							final List<MathValue<Player>> values = new ArrayList<>();
							list.forEach((Object o) -> values.add(MathValue.auto(toTypeAndData((String) o).right())));
							itemEditors.add(((item, context) -> {
								final List<Double> doubles = values.stream().map(unparsed -> unparsed.evaluate(context))
										.toList();
								item.set(doubles, (Object[]) currentRoute);
							}));
						}
						case STRING -> {
							final List<TextValue<Player>> values = new ArrayList<>();
							list.forEach((Object o) -> values.add(TextValue.auto(toTypeAndData((String) o).right())));
							itemEditors.add(((item, context) -> {
								final List<String> texts = values.stream().map(unparsed -> unparsed.render(context))
										.toList();
								item.set(texts, (Object[]) currentRoute);
							}));
						}
						default -> throw new IllegalArgumentException("Unexpected value: " + pair.left());
						}
					} else {
						itemEditors.add(((item, context) -> item.set(list, (Object[]) currentRoute)));
					}
				}
			} else if (value instanceof String str) {
				final Pair<TagValueType, String> pair = toTypeAndData(str);
				switch (pair.left()) {
				case INT -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(
							((item, context) -> item.set((int) mathValue.evaluate(context), (Object[]) currentRoute)));
				}
				case BYTE -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(
							((item, context) -> item.set((byte) mathValue.evaluate(context), (Object[]) currentRoute)));
				}
				case LONG -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(
							((item, context) -> item.set((long) mathValue.evaluate(context), (Object[]) currentRoute)));
				}
				case SHORT -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> item.set((short) mathValue.evaluate(context),
							(Object[]) currentRoute)));
				}
				case DOUBLE -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors
							.add(((item, context) -> item.set(mathValue.evaluate(context), (Object[]) currentRoute)));
				}
				case FLOAT -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					itemEditors.add(((item, context) -> item.set((float) mathValue.evaluate(context),
							(Object[]) currentRoute)));
				}
				case STRING -> {
					final TextValue<Player> textValue = TextValue.auto(pair.right());
					itemEditors.add(((item, context) -> item.set(textValue.render(context), (Object[]) currentRoute)));
				}
				case INT_ARRAY -> {
					final String[] split = splitValue(str);
					final int[] array = Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
					itemEditors.add(((item, context) -> item.set(array, (Object[]) currentRoute)));
				}
				case LONG_ARRAY -> {
					final String[] split = splitValue(str);
					final long[] array = Arrays.stream(split).mapToLong(Long::parseLong).toArray();
					itemEditors.add(((item, context) -> item.set(array, (Object[]) currentRoute)));
				}
				case BYTE_ARRAY -> {
					final String[] split = splitValue(str);
					final byte[] bytes = new byte[split.length];
					for (int i = 0; i < split.length; i++) {
						bytes[i] = Byte.parseByte(split[i]);
					}
					itemEditors.add(((item, context) -> item.set(bytes, (Object[]) currentRoute)));
				}
				case LIST -> {
					final String[] split = splitValue(str);
					final List<String> list = Arrays.asList(split);
					itemEditors.add((item, context) -> item.set(list, (Object[]) currentRoute));
				}
				case COMPOUND -> {
					final Map<String, Object> map = parseCompoundFromString(pair.right());
					itemEditors.add((item, context) -> item.set(map, (Object[]) currentRoute));
				}
				}
			} else {
				itemEditors.add(((item, context) -> item.set(value, (Object[]) currentRoute)));
			}
		});
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
			final Object tag = entry.getValue();
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
						HellblockPlugin.getInstance().getPluginLogger()
								.warn("This class is not recognized as tag data: %s".formatted(innerTag.getClass()));
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
						.warn("This class is not recognized as tag data: %s".formatted(tag.getClass()));
				break;
			}
		}
	}

	/**
	 * Parses a compound-like string into a Map. Expected format:
	 * "key1=value1,key2=value2"
	 */
	private static Map<String, Object> parseCompoundFromString(String input) {
		Map<String, Object> map = new HashMap<>();
		if (input == null || input.isBlank())
			return map;

		String[] entries = input.split(",");
		for (String entry : entries) {
			String[] pair = entry.split("=", 2);
			if (pair.length == 2) {
				map.put(pair[0].trim(), pair[1].trim());
			}
		}
		return map;
	}
}