package com.swiftlicious.hellblock.player;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory.EmptyCheck;

/**
 * The StatisticData class stores fishing statistics including amounts and sizes
 * of fish caught, represented as maps.
 */
public class StatisticData implements EmptyCheck {

	@Expose
	@SerializedName(value = "amount", alternate = { "map" })
	public Map<String, Integer> amountMap;

	@Expose
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
		this.amountMap = new HashMap<>(amount);
		this.sizeMap = new HashMap<>(size);
	}

	@NotNull
	public Map<String, Integer> getAmountMap() {
		return this.amountMap;
	}

	@NotNull
	public Map<String, Float> getSizeMap() {
		return this.sizeMap;
	}

	/**
	 * Creates an instance of StatisticData with empty maps.
	 *
	 * @return a new instance of StatisticData with empty maps.
	 */
	@NotNull
	public static StatisticData empty() {
		return new StatisticData();
	}

	@NotNull
	public final StatisticData copy() {
		return new StatisticData(new HashMap<>(amountMap), new HashMap<>(sizeMap));
	}

	@Override
	public boolean isEmpty() {
		return this.amountMap.isEmpty() && this.sizeMap.isEmpty();
	}
}