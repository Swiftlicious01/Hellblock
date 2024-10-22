package com.swiftlicious.hellblock.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.swiftlicious.hellblock.utils.extras.Pair;

/**
 * Utility class for selecting random items based on weights.
 */
public class WeightUtils {

	/**
	 * Get a random item from a list of pairs, each associated with a weight.
	 *
	 * @param pairs A list of pairs where the left element is the item and the right
	 *              element is its weight.
	 * @param <T>   The type of items in the list.
	 * @return A randomly selected item from the list, or null if no item was
	 *         selected.
	 */
	public <T> T getRandom(List<Pair<T, Double>> pairs) {
		List<T> available = new ArrayList<>();
		double[] weights = new double[pairs.size()];
		int index = 0;
		for (Pair<T, Double> pair : pairs) {
			double weight = pair.right();
			T key = pair.left();
			if (weight <= 0)
				continue;
			available.add(key);
			weights[index++] = weight;
		}
		return getRandom(weights, available, index);
	}

	/**
	 * Get a random item from a map where each entry is associated with a weight.
	 *
	 * @param map A map where each entry's key is an item, and the value is its
	 *            weight.
	 * @param <T> The type of items in the map.
	 * @return A randomly selected item from the map, or null if no item was
	 *         selected.
	 */
	public <T> T getRandom(Map<T, Double> map) {
		List<T> available = new ArrayList<>();
		double[] weights = new double[map.size()];
		int index = 0;
		for (Map.Entry<T, Double> entry : map.entrySet()) {
			double weight = entry.getValue();
			T key = entry.getKey();
			if (weight <= 0)
				continue;
			available.add(key);
			weights[index++] = weight;
		}
		return getRandom(weights, available, index);
	}

	/**
	 * Get a random item from a list of items with associated weights.
	 *
	 * @param weights       An array of weights corresponding to the available
	 *                      items.
	 * @param available     A list of available items.
	 * @param effectiveSize The effective size of the array and list after filtering
	 *                      out items with non-positive weights.
	 * @param <T>           The type of items.
	 * @return A randomly selected item from the list, or null if no item was
	 *         selected.
	 */
	private <T> T getRandom(double[] weights, List<T> available, int effectiveSize) {
		double total = Arrays.stream(weights).sum();
		double[] weightRatios = new double[effectiveSize];
		for (int i = 0; i < effectiveSize; i++) {
			weightRatios[i] = weights[i] / total;
		}
		double[] weightRange = new double[effectiveSize];
		double startPos = 0;
		for (int i = 0; i < effectiveSize; i++) {
			weightRange[i] = startPos + weightRatios[i];
			startPos += weightRatios[i];
		}
		double random = Math.random();
		int pos = Arrays.binarySearch(weightRange, random);

		if (pos < 0) {
			pos = -pos - 1;
		}
		if (pos < weightRange.length && random < weightRange[pos]) {
			return available.get(pos);
		}
		return null;
	}
}