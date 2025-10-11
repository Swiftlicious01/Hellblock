package com.swiftlicious.hellblock.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.swiftlicious.hellblock.utils.extras.Pair;

public class FishingStatistics implements FishingStatisticsInterface {

	private int amountOfFishCaught;
	private final Map<String, Integer> amountMap;
	private final Map<String, Float> sizeMap;

	public FishingStatistics(Map<String, Integer> amountMap, Map<String, Float> sizeMap) {
		this.amountMap = Collections.synchronizedMap(amountMap);
		this.sizeMap = Collections.synchronizedMap(sizeMap);
		this.amountOfFishCaught = amountMap.values().stream().mapToInt(Integer::intValue).sum();
	}

	@Override
	public int amountOfFishCaught() {
		return amountOfFishCaught;
	}

	@Override
	public void amountOfFishCaught(int amountOfFishCaught) {
		this.amountOfFishCaught = amountOfFishCaught;
	}

	@Override
	public int getAmount(String id) {
		return amountMap.getOrDefault(id, 0);
	}

	@Override
	public Pair<Integer, Integer> addAmount(String id, int amount) {
		if (amount <= 0) {
			return Pair.of(-1, -1);
		}
		final int previous = amountMap.getOrDefault(id, 0);
		amountMap.put(id, previous + amount);
		amountOfFishCaught += amount;
		return Pair.of(previous, previous + amount);
	}

	@Override
	public void setAmount(String id, int amount) {
		if (amount < 0) {
			amount = 0;
		}
		final int previous = amountMap.getOrDefault(id, 0);
		final int delta = amount - previous;
		this.amountOfFishCaught += delta;
		amountMap.put(id, amount);
	}

	@Override
	public float getMaxSize(String id) {
		return sizeMap.getOrDefault(id, 0f);
	}

	@Override
	public void setMaxSize(String id, float maxSize) {
		if (maxSize < 0) {
			return;
		}
		sizeMap.put(id, maxSize);
	}

	@Override
	public boolean updateSize(String id, float newSize) {
		if (newSize <= 0) {
			return false;
		}
		final float previous = sizeMap.getOrDefault(id, 0f);
		if (previous >= newSize) {
			return false;
		}
		sizeMap.put(id, newSize);
		return true;
	}

	@Override
	public void reset() {
		this.sizeMap.clear();
		this.amountMap.clear();
		this.amountOfFishCaught = 0;
	}

	@Override
	public Map<String, Integer> amountMap() {
		return amountMap;
	}

	@Override
	public Map<String, Float> sizeMap() {
		return sizeMap;
	}

	public static class Builder implements BuilderInterface {
		private final Map<String, Integer> amountMap = new HashMap<>();
		private final Map<String, Float> sizeMap = new HashMap<>();

		@Override
		public Builder amountMap(Map<String, Integer> amountMap) {
			this.amountMap.putAll(amountMap);
			return this;
		}

		@Override
		public Builder sizeMap(Map<String, Float> sizeMap) {
			this.sizeMap.putAll(sizeMap);
			return this;
		}

		@Override
		public FishingStatistics build() {
			return new FishingStatistics(amountMap, sizeMap);
		}
	}
}