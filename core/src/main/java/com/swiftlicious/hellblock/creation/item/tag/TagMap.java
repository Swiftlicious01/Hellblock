package com.swiftlicious.hellblock.creation.item.tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import static com.swiftlicious.hellblock.utils.TagUtils.toTypeAndData;
import static com.swiftlicious.hellblock.utils.ArrayUtils.splitValue;

public class TagMap implements TagMapInterface {

	private final Map<String, Object> convertedMap = new HashMap<>();

	public TagMap(Map<String, Object> inputMap) {
		this.analyze(inputMap, convertedMap);
	}

	@SuppressWarnings("unchecked")
	private void analyze(Map<String, Object> inputMap, Map<String, Object> outPutMap) {
		for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Map<?, ?> inner) {
				Map<String, Object> inputInnerMap = (Map<String, Object>) inner;
				Map<String, Object> outputInnerMap = new HashMap<>();
				outPutMap.put(key, outputInnerMap);
				analyze(inputInnerMap, outputInnerMap);
			} else if (value instanceof List<?> list) {
				Object first = list.get(0);
				List<Object> outputList = new ArrayList<>();
				if (first instanceof Map<?, ?>) {
					for (Object o : list) {
						Map<String, Object> inputListMap = (Map<String, Object>) o;
						outputList.add(TagMapInterface.of(inputListMap));
					}
				} else if (first instanceof String) {
					for (Object o : list) {
						String str = (String) o;
						Pair<TagValueType, String> pair = toTypeAndData(str);
						switch (pair.left()) {
						case STRING -> {
							TextValue<Player> textValue = TextValue.auto(pair.right());
							outputList.add((ValueProvider) textValue::render);
						}
						case BYTE -> {
							MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) context -> (byte) mathValue.evaluate(context));
						}
						case SHORT -> {
							MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) context -> (short) mathValue.evaluate(context));
						}
						case INT -> {
							MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) context -> (int) mathValue.evaluate(context));
						}
						case LONG -> {
							MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) context -> (long) mathValue.evaluate(context));
						}
						case FLOAT -> {
							MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) context -> (float) mathValue.evaluate(context));
						}
						case DOUBLE -> {
							MathValue<Player> mathValue = MathValue.auto(pair.right());
							outputList.add((ValueProvider) context -> (double) mathValue.evaluate(context));
						}
						default -> throw new IllegalArgumentException("Unexpected value: " + pair.left());
						}
					}
				} else {
					outputList.addAll(list);
				}
				outPutMap.put(key, outputList);
			} else if (value instanceof String str) {
				Pair<TagValueType, String> pair = toTypeAndData(str);
				switch (pair.left()) {
				case INTARRAY -> {
					String[] split = splitValue(str);
					int[] array = Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
					outPutMap.put(pair.right(), array);
				}
				case BYTEARRAY -> {
					String[] split = splitValue(str);
					byte[] bytes = new byte[split.length];
					for (int i = 0; i < split.length; i++) {
						bytes[i] = Byte.parseByte(split[i]);
					}
					outPutMap.put(pair.right(), bytes);
				}
				case STRING -> {
					TextValue<Player> textValue = TextValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) textValue::render);
				}
				case BYTE -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) context -> (byte) mathValue.evaluate(context));
				}
				case SHORT -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) context -> (short) mathValue.evaluate(context));
				}
				case INT -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) context -> (int) mathValue.evaluate(context));
				}
				case LONG -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) context -> (long) mathValue.evaluate(context));
				}
				case FLOAT -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) context -> (float) mathValue.evaluate(context));
				}
				case DOUBLE -> {
					MathValue<Player> mathValue = MathValue.auto(pair.right());
					outPutMap.put(key, (ValueProvider) context -> (double) mathValue.evaluate(context));
				}
				}
			} else {
				outPutMap.put(key, value);
			}
		}
	}

	@Override
	public Map<String, Object> apply(Context<Player> context) {
		Map<String, Object> output = new HashMap<>();
		setMapValue(convertedMap, output, context);
		return output;
	}

	@SuppressWarnings("unchecked")
	private void setMapValue(Map<String, Object> inputMap, Map<String, Object> outPutMap, Context<Player> context) {
		for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Map<?, ?> inner) {
				Map<String, Object> inputInnerMap = (Map<String, Object>) inner;
				Map<String, Object> outputInnerMap = new HashMap<>();
				outPutMap.put(key, outputInnerMap);
				setMapValue(inputInnerMap, outputInnerMap, context);
			} else if (value instanceof List<?> list) {
				ArrayList<Object> convertedList = new ArrayList<>();
				Object first = list.get(0);
				if (first instanceof TagMap) {
					for (Object o : list) {
						TagMap map = (TagMap) o;
						convertedList.add(map.apply(context));
					}
				} else if (first instanceof ValueProvider) {
					for (Object o : list) {
						ValueProvider pd = (ValueProvider) o;
						convertedList.add(pd.apply(context));
					}
				} else {
					convertedList.addAll(list);
				}
				outPutMap.put(key, convertedList);
			} else if (value instanceof ValueProvider provider) {
				outPutMap.put(key, provider.apply(context));
			} else {
				outPutMap.put(key, value);
			}
		}
	}

	@FunctionalInterface
	public interface ValueProvider {
		Object apply(Context<Player> context);
	}
}