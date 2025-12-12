package com.swiftlicious.hellblock.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class RecipeHelper {

	/**
	 * Trims a raw 3×3 recipe shape by removing any fully empty rows or columns.
	 * <p>
	 * This ensures the recipe pattern is reduced to the smallest possible bounding
	 * box before it is expanded into multiple shifted variants.
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Input:
	 * ["   ",
	 *  " # ",
	 *  "   "]
	 *
	 * Output:
	 * ["#"]
	 * </pre>
	 *
	 * @param shape The list of up to three strings representing the original recipe
	 *              shape.
	 * @return A new list containing only the trimmed rows and columns that include
	 *         ingredients.
	 */
	@NotNull
	public List<String> trimShape(@NotNull List<String> shape) {
		// Remove empty rows
		while (!shape.isEmpty() && shape.get(0).trim().isEmpty())
			shape.remove(0);
		while (!shape.isEmpty() && shape.get(shape.size() - 1).trim().isEmpty())
			shape.remove(shape.size() - 1);

		int minCol = 3, maxCol = 0;
		for (String row : shape) {
			for (int i = 0; i < 3; i++) {
				if (row.length() > i && row.charAt(i) != ' ') {
					minCol = Math.min(minCol, i);
					maxCol = Math.max(maxCol, i);
				}
			}
		}

		List<String> trimmed = new ArrayList<>();
		for (String row : shape) {
			StringBuilder sb = new StringBuilder();
			for (int i = minCol; i <= maxCol; i++) {
				sb.append(i < row.length() ? row.charAt(i) : ' ');
			}
			trimmed.add(sb.toString());
		}

		return trimmed;
	}

	/**
	 * Generates all possible shifted variants of a given trimmed recipe shape
	 * within a 3×3 crafting grid.
	 * <p>
	 * Each variant preserves the relative pattern of ingredients while moving it to
	 * every valid position in the crafting grid (e.g. left, center, right, top,
	 * middle, bottom).
	 * <p>
	 * This allows shaped recipes to match the same layout in multiple grid
	 * positions without requiring separate configuration.
	 * <p>
	 * Example: A 2×2 pattern produces up to four shifted variants across the grid.
	 *
	 * @param shape The trimmed recipe shape (as produced by
	 *              {@link #trimShape(List)}).
	 * @return A list of 3×3 string arrays representing all valid shifted grid
	 *         positions.
	 */
	@NotNull
	public List<String[][]> generateShiftedShapes(@NotNull List<String> shape) {
		int height = shape.size();
		int width = shape.get(0).length();

		List<String[][]> variants = new ArrayList<>();

		for (int rowOffset = 0; rowOffset <= 3 - height; rowOffset++) {
			for (int colOffset = 0; colOffset <= 3 - width; colOffset++) {
				String[][] grid = new String[3][3];

				// Fill grid with spaces
				for (int i = 0; i < 3; i++)
					Arrays.fill(grid[i], " ");

				// Copy trimmed shape into shifted position
				for (int i = 0; i < height; i++) {
					for (int j = 0; j < width; j++) {
						grid[i + rowOffset][j + colOffset] = String.valueOf(shape.get(i).charAt(j));
					}
				}

				variants.add(grid);
			}
		}

		return variants;
	}
}