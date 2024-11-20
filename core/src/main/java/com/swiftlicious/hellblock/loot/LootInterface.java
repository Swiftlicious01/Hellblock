package com.swiftlicious.hellblock.loot;

import java.util.Map;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.loot.Loot.Builder;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.TextValue;

public interface LootInterface {

	class DefaultProperties {
		public static boolean DEFAULT_SHOW_IN_FINDER = true;
        public static boolean DEFAULT_DISABLE_STATS = false;
	}

	LootType DEFAULT_TYPE = LootType.ITEM;

	/**
	 * Check if statistics recording is disabled for this loot.
	 *
	 * @return True if statistics are disabled, false otherwise.
	 */
	boolean disableStats();

	/**
	 * Check if this loot should be displayed in the finder tool.
	 *
	 * @return True if it should be shown in the finder, false otherwise.
	 */
	boolean showInFinder();

	/**
	 * Check if players can't grab the loot
	 *
	 * @return True if players can't grab the loot, false otherwise.
	 */
	boolean preventGrabbing();

	/**
	 * If the loot item should go directly into inventory
	 *
	 * @return True if loot go directly into inventory
	 */
	MathValue<Player> toInventory();

	/**
	 * Get the unique identifier for this loot.
	 *
	 * @return The unique ID of the loot.
	 */
	String id();

	/**
	 * Get the type of this loot.
	 *
	 * @return The type of the loot.
	 */
	LootType type();

	/**
	 * Get the display nickname for this loot.
	 *
	 * @return The nickname of the loot.
	 */
	@NotNull
	String nick();

	/**
	 * Get the statistics key associated with this loot.
	 *
	 * @return The statistics key for this loot.
	 */
	StatisticsKeys statisticKey();

	/**
	 * Get the groups this loot belongs to.
	 *
	 * @return An array of group names.
	 */
	String[] lootGroup();

	/**
	 * Get the base effect associated with this loot.
	 *
	 * @return The base effect for the loot.
	 */
	LootBaseEffect baseEffect();

	/**
	 * Get the custom data
	 *
	 * @return custom data
	 */
	Map<String, TextValue<Player>> customData();

	/**
	 * Create a new builder for constructing a Loot instance.
	 *
	 * @return A new Loot builder.
	 */
	static Builder builder() {
		return new Loot.Builder();
	}

	/**
	 * Builder interface for constructing instances of Loot.
	 */
	interface BuilderInterface {

		/**
		 * Set the type of the loot.
		 *
		 * @param type The type of the loot.
		 * @return The builder instance.
		 */
		Builder type(LootType type);

		/**
		 * Specify whether players are prevented from grabbing the loot
		 *
		 * @param preventGrabbing True if grabbing should be prevented.
		 * @return The builder instance.
		 */
		Builder preventGrabbing(boolean preventGrabbing);

		/**
		 * Specify whether statistics recording is disabled for this loot.
		 *
		 * @param disableStatistics True if statistics should be disabled.
		 * @return The builder instance.
		 */
		Builder disableStatistics(boolean disableStatistics);

		/**
		 * Specify whether the loot should be shown in the finder tool.
		 *
		 * @param showInFinder True if it should be shown in the finder.
		 * @return The builder instance.
		 */
		Builder showInFinder(boolean showInFinder);

		/**
		 * Set the unique ID for the loot.
		 *
		 * @param id The unique identifier.
		 * @return The builder instance.
		 */
		Builder id(String id);

		/**
		 * Set the nickname for the loot.
		 *
		 * @param nick The nickname.
		 * @return The builder instance.
		 */
		Builder nick(String nick);

		/**
		 * Set the statistics key for the loot.
		 *
		 * @param statisticsKeys The statistics key.
		 * @return The builder instance.
		 */
		Builder statisticsKeys(StatisticsKeys statisticsKeys);

		/**
		 * Set the groups that the loot belongs to.
		 *
		 * @param groups An array of group names.
		 * @return The builder instance.
		 */
		Builder groups(String[] groups);

		/**
		 * Set the base effect for the loot.
		 *
		 * @param lootBaseEffect The base effect.
		 * @return The builder instance.
		 */
		Builder lootBaseEffect(LootBaseEffect lootBaseEffect);

		/**
		 * Set the custom data
		 *
		 * @param customData the custom data
		 * @return The builder instance.
		 */
		Builder customData(Map<String, TextValue<Player>> customData);

		/**
		 * Set if the loot go directly into inventory
		 *
		 * @param toInventory go directly into the inventory
		 * @return The builder instance.
		 */
		Builder toInventory(MathValue<Player> toInventory);

		/**
		 * Build and return the Loot instance.
		 *
		 * @return The constructed Loot instance.
		 */
		Loot build();
	}
}