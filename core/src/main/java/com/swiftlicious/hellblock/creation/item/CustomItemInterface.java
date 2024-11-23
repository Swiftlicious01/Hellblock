package com.swiftlicious.hellblock.creation.item;

import java.util.List;
import java.util.function.BiConsumer;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.CustomItem.Builder;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.PriorityFunction;

/**
 * Interface representing a custom item
 */
public interface CustomItemInterface {

	String DEFAULT_MATERIAL = "PAPER";

	/**
	 * Returns the material type of the custom item.
	 *
	 * @return the material type as a String.
	 */
	String material();

	/**
	 * Returns the unique identifier of the custom item.
	 *
	 * @return the unique identifier as a String.
	 */
	String id();

	/**
	 * Returns the amount of the item
	 *
	 * @return the amount of the item
	 */
	MathValue<Player> amount();

	/**
	 * Returns a list of tag consumers. Tag consumers are functions that take an
	 * {@link Item} and a {@link Context} as parameters and perform some operation
	 * on them.
	 *
	 * @return a list of {@link BiConsumer} instances.
	 */
	List<BiConsumer<Item<ItemStack>, Context<Player>>> tagConsumers();

	/**
	 * Builds the custom item using the given context.
	 *
	 * @param context the {@link Context} in which the item is built.
	 * @return the built {@link ItemStack}.
	 */
	default ItemStack build(Context<Player> context) {
		return HellblockPlugin.getInstance().getItemManager().build(context, this);
	}

	/**
	 * Creates a new {@link Builder} instance to construct a {@link CustomItem}.
	 *
	 * @return a new {@link Builder} instance.
	 */
	static Builder builder() {
		return new CustomItem.Builder();
	}

	/**
	 * Builder interface for constructing instances of {@link CustomItem}.
	 */
	interface BuilderInterface {

		/**
		 * Sets the unique identifier for the {@link CustomItem} being built.
		 *
		 * @param id the unique identifier as a String.
		 * @return the {@link Builder} instance for method chaining.
		 */
		Builder id(String id);

		/**
		 * Sets the material type for the {@link CustomItem} being built.
		 *
		 * @param material the material type as a String.
		 * @return the {@link Builder} instance for method chaining.
		 */
		Builder material(String material);

		/**
		 * Sets the amount of the item
		 *
		 * @param amount amount
		 * @return the {@link Builder} instance for method chaining.
		 */
		Builder amount(MathValue<Player> amount);

		/**
		 * Sets the list of tag consumers for the {@link CustomItem} being built. Tag
		 * consumers are functions that take an {@link Item} and a {@link Context} as
		 * parameters and perform some operation on them.
		 *
		 * @param tagConsumers a list of {@link PriorityFunction} instances wrapping
		 *                     {@link BiConsumer} functions.
		 * @return the {@link Builder} instance for method chaining.
		 */
		Builder tagConsumers(List<PriorityFunction<BiConsumer<Item<ItemStack>, Context<Player>>>> tagConsumers);

		/**
		 * Builds and returns a new {@link CustomItem} instance.
		 *
		 * @return a new {@link CustomItem} instance.
		 */
		CustomItem build();
	}
}