package com.swiftlicious.hellblock.player;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.SerializedName;

/**
 * The StatisticData class stores fishing statistics including amounts and sizes
 * of fish caught, represented as maps.
 */
public class StatisticData {

	@SerializedName(value = "amount", alternate = { "map" })
	public Map<String, Integer> amountMap;

	@SerializedName("size")
	public Map<String, Float> sizeMap;

	/**
	 * Default constructor that initializes the sizeMap and amountMap as empty
	 * HashMaps.
	 */
	private StatisticData() {
		this.sizeMap = new HashMap<>();
		this.amountMap = new HashMap<>();
	}

	/**
	 * Parameterized constructor that initializes the sizeMap and amountMap with
	 * provided values.
	 *
	 * @param amount a map containing the amount of each type of fish caught.
	 * @param size   a map containing the size of each type of fish caught.
	 */
	public StatisticData(@NotNull Map<String, Integer> amount, @NotNull Map<String, Float> size) {
		this.amountMap = amount;
		this.sizeMap = size;
	}

	public Map<String, Integer> getAmountMap() {
		return this.amountMap;
	}

	public Map<String, Float> getSizeMap() {
		return this.sizeMap;
	}

	/**
	 * Creates an instance of StatisticData with empty maps.
	 *
	 * @return a new instance of StatisticData with empty maps.
	 */
	public static StatisticData empty() {
		return new StatisticData();
	}
}