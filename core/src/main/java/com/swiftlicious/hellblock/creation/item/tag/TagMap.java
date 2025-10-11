package com.swiftlicious.hellblock.creation.item.tag;

import static com.swiftlicious.hellblock.utils.ArrayUtils.splitValue;
import static com.swiftlicious.hellblock.utils.TagUtils.toTypeAndData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

public class TagMap implements TagMapInterface {

	private final Map<String, Object> convertedMap = new HashMap<>();

	public TagMap(Map<String, Object> inputMap) {
		this.analyze(inputMap, convertedMap);
	}

	// Recursive analysis of the input map to convert strings to appropriate types
	@SuppressWarnings("unchecked")
	private void analyze(Map<String, Object> inputMap, Map<String, Object> outPutMap) {
		inputMap.entrySet().forEach(entry -> {
			final String key = entry.getKey();
			final Object value = entry.getValue();
			if (value instanceof Map<?, ?> inner) {
				final Map<String, Object> inputInnerMap = (Map<String, Object>) inner;
				final Map<String, Object> outputInnerMap = new HashMap<>();
				outPutMap.put(key, outputInnerMap);
				analyze(inputInnerMap, outputInnerMap);
			} else if (value instanceof List<?> list) {
				final Object first = list.get(0);
				final List<Object> outputList = new ArrayList<>();
				if (first instanceof Map<?, ?>) {
					list.forEach((Object o) -> {
						final Map<String, Object> inputListMap = (Map<String, Object>) o;
						outputList.add(TagMapInterface.of(inputListMap));
					});
				} else if (first instanceof String) {
					list.forEach((Object o) -> {
						final String str = (String) o;
						final Pair<TagValueType, String> pair = toTypeAndData(str);
						switch (pair.left()) {
						case STRING -> {
							final TextValue<Player> textValue = TextValue.auto(pair.right());
							outputList.add((ValueProvider) textValue::render);
						}
						case BYTE -> {
							final MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) context -> (byte) mathValue.evaluate(context));
						}
						case SHORT -> {
							final MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) context -> (short) mathValue.evaluate(context));
						}
						case INT -> {
							final MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) context -> (int) mathValue.evaluate(context));
						}
						case LONG -> {
							final MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) context -> (long) mathValue.evaluate(context));
						}
						case FLOAT -> {
							final MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) context -> (float) mathValue.evaluate(context));
						}
						case DOUBLE -> {
							final MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) mathValue::evaluate);
						}
						default -> throw new IllegalArgumentException("Unexpected value: " + pair.left());
						}
					});
				} else {
					outputList.addAll(list);
				}
				outPutMap.put(key, outputList);
			} else if (value instanceof String str) {
				final Pair<TagValueType, String> pair = toTypeAndData(str);
				switch (pair.left()) {
				case INT_ARRAY -> {
					final String[] split = splitValue(str);
					final int[] array = Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
					outPutMap.put(key, array);
				}
				case LONG_ARRAY -> {
					final String[] split = splitValue(str);
					final long[] array = Arrays.stream(split).mapToLong(Long::parseLong).toArray();
					outPutMap.put(key, array);
				}
				case BYTE_ARRAY -> {
					final String[] split = splitValue(str);
					final byte[] bytes = new byte[split.length];
					for (int i = 0; i < split.length; i++) {
						bytes[i] = Byte.parseByte(split[i]);
					}
					outPutMap.put(key, bytes);
				}
				case LIST -> {
					// Expect a comma-separated list of stringified values
					final String[] elements = splitValue(pair.right());
					final List<String> list = Arrays.asList(elements);
					outPutMap.put(key, list);
				}
				case COMPOUND -> {
					// Expect key1=value1;key2=value2 format inside
					final Map<String, String> compoundMap = Arrays.stream(pair.right().split(";"))
							.map(e -> e.split("=", 2))
							.collect(Collectors.toMap(e -> e[0].trim(), e -> e.length > 1 ? e[1].trim() : ""));
					outPutMap.put(key, compoundMap);
				}
				case STRING -> {
					final TextValue<Player> textValue = TextValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) textValue::render);
				}
				case BYTE -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) context -> (byte) mathValue.evaluate(context));
				}
				case SHORT -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) context -> (short) mathValue.evaluate(context));
				}
				case INT -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) context -> (int) mathValue.evaluate(context));
				}
				case LONG -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) context -> (long) mathValue.evaluate(context));
				}
				case FLOAT -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) context -> (float) mathValue.evaluate(context));
				}
				case DOUBLE -> {
					final MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) mathValue::evaluate);
				}
				default -> throw new IllegalArgumentException("Unexpected value: " + pair.left());
				}
			} else {
				outPutMap.put(key, value);
			}
		});
	}

	// Apply the context to generate the final map with evaluated values
	@Override
	public Map<String, Object> apply(Context<Player> context) {
		final Map<String, Object> output = new HashMap<>();
		setMapValue(convertedMap, output, context);
		return output;
	}

	// Recursive method to set values in the output map based on the converted map
	@SuppressWarnings("unchecked")
	private void setMapValue(Map<String, Object> inputMap, Map<String, Object> outPutMap, Context<Player> context) {
		inputMap.entrySet().forEach(entry -> {
			final String key = entry.getKey();
			final Object value = entry.getValue();
			if (value instanceof Map<?, ?> inner) {
				final Map<String, Object> inputInnerMap = (Map<String, Object>) inner;
				final Map<String, Object> outputInnerMap = new HashMap<>();
				outPutMap.put(key, outputInnerMap);
				setMapValue(inputInnerMap, outputInnerMap, context);
			} else if (value instanceof List<?> list) {
				final List<Object> convertedList = new ArrayList<>();
				final Object first = list.get(0);
				if (first instanceof TagMap) {
					list.forEach((Object o) -> {
						final TagMap map = (TagMap) o;
						convertedList.add(map.apply(context));
					});
				} else if (first instanceof ValueProvider) {
					list.forEach((Object o) -> {
						final ValueProvider pd = (ValueProvider) o;
						convertedList.add(pd.apply(context));
					});
				} else {
					convertedList.addAll(list);
				}
				outPutMap.put(key, convertedList);
			} else if (value instanceof ValueProvider provider) {
				outPutMap.put(key, provider.apply(context));
			} else {
				outPutMap.put(key, value);
			}
		});
	}

	@FunctionalInterface
	public interface ValueProvider {
		Object apply(Context<Player> context);
	}
}