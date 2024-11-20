package com.swiftlicious.hellblock.listeners.fishing;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.api.Reloadable;

/**
 * Interface for managing statistics
 */
public interface StatisticsManagerInterface extends Reloadable {

	/**
	 * Retrieves the members of a statistics category identified by the given key.
	 *
	 * @param key the key identifying the statistics category
	 * @return a list of category member identifiers as strings
	 */
	@NotNull
	List<String> getCategoryMembers(String key);
}