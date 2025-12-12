package com.swiftlicious.hellblock.creation.addons;

import java.util.List;
import java.util.Set;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.creation.addons.enchant.EnchantmentProvider;
import com.swiftlicious.hellblock.creation.addons.level.LevelerProvider;
import com.swiftlicious.hellblock.creation.addons.pet.PetProvider;
import com.swiftlicious.hellblock.creation.addons.shop.sign.ShopSignProvider;
import com.swiftlicious.hellblock.creation.block.BlockProvider;
import com.swiftlicious.hellblock.creation.entity.EntityProvider;
import com.swiftlicious.hellblock.creation.item.ItemProvider;
import com.swiftlicious.hellblock.utils.extras.Pair;

/**
 * Interface for managing integration providers. This allows for the
 * registration and retrieval of various types of providers such as Leveler,
 * Enchantment, and other providers.
 */
public interface IntegrationManagerInterface extends Reloadable {

	/**
	 * Registers a LevelerProvider.
	 *
	 * @param levelerProvider the LevelerProvider to register
	 * @return true if registration is successful, false otherwise
	 */
	boolean registerLevelerProvider(@NotNull LevelerProvider levelerProvider);

	/**
	 * Unregisters a LevelerProvider by its ID.
	 *
	 * @param id the ID of the LevelerProvider to unregister
	 * @return true if unregistration is successful, false otherwise
	 */
	boolean unregisterLevelerProvider(@NotNull String id);

	/**
	 * Registers a PetProvider.
	 *
	 * @param petProvider the PetProvider to register
	 * @return true if registration is successful, false otherwise
	 */
	boolean registerPetProvider(@NotNull PetProvider petProvider);

	/**
	 * Unregisters a PetProvider by its ID.
	 *
	 * @param id the ID of the PetProvider to unregister
	 * @return true if unregistration is successful, false otherwise
	 */
	boolean unregisterPetProvider(@NotNull String id);

	/**
	 * Registers a ShopSignProvider.
	 *
	 * @param shopSignProvider the ShopSignProvider to register
	 * @return true if registration is successful, false otherwise
	 */
	boolean registerShopSignProvider(@NotNull ShopSignProvider shopSignProvider);

	/**
	 * Unregisters a ShopSignProvider by its ID.
	 *
	 * @param id the ID of the ShopSignProvider to unregister
	 * @return true if unregistration is successful, false otherwise
	 */
	boolean unregisterShopSignProvider(@NotNull String id);

	/**
	 * Registers an EnchantmentProvider.
	 *
	 * @param enchantmentProvider the EnchantmentProvider to register
	 * @return true if registration is successful, false otherwise
	 */
	boolean registerEnchantmentProvider(@NotNull EnchantmentProvider enchantmentProvider);

	/**
	 * Unregisters an EnchantmentProvider by its ID.
	 *
	 * @param id the ID of the EnchantmentProvider to unregister
	 * @return true if unregistration is successful, false otherwise
	 */
	boolean unregisterEnchantmentProvider(@NotNull String id);

	/**
	 * Registers an EntityProvider.
	 *
	 * @param entityProvider the EntityProvider to register
	 * @return true if registration is successful, false otherwise.
	 */
	boolean registerEntityProvider(@NotNull EntityProvider entityProvider);

	/**
	 * Unregisters an EntityProvider by its ID.
	 *
	 * @param id the ID of the EntityProvider to unregister
	 * @return true if unregistration is successful, false otherwise.
	 */
	boolean unregisterEntityProvider(@NotNull String id);

	/**
	 * Retrieves a registered EntityProvider by its ID.
	 *
	 * @param id the ID of the EntityProvider to retrieve
	 * @return the EntityProvider if found, or null if not found
	 */
	@Nullable
	EntityProvider getEntityProvider(String id);

	/**
	 * Retrieves a registered LevelerProvider by its ID.
	 *
	 * @param id the ID of the LevelerProvider to retrieve
	 * @return the LevelerProvider if found, or null if not found
	 */
	@Nullable
	LevelerProvider getLevelerProvider(String id);

	/**
	 * Retrieves a registered PetProvider by its ID.
	 *
	 * @param id the ID of the PetProvider to retrieve
	 * @return the PetProvider if found, or null if not found
	 */
	@Nullable
	PetProvider getPetProvider(String id);

	/**
	 * Retrieves a set of all registered PetProviders.
	 *
	 * @return the PetProvider set or empty if none registered.
	 */
	@NotNull
	Set<PetProvider> getPetProviders();

	/**
	 * Retrieves a registered ShopSignProvider by its ID.
	 *
	 * @param id the ID of the ShopSignProvider to retrieve
	 * @return the ShopSignProvider if found, or null if not found
	 */
	@Nullable
	ShopSignProvider getShopSignProvider(String id);

	/**
	 * Retrieves a set of all registered ShopSignProviders.
	 *
	 * @return the ShopSignProvider set or empty if none registered.
	 */
	@NotNull
	Set<ShopSignProvider> getShopSignProviders();

	/**
	 * Retrieves a registered EnchantmentProvider by its ID.
	 *
	 * @param id the ID of the EnchantmentProvider to retrieve
	 * @return the EnchantmentProvider if found, or null if not found
	 */
	@Nullable
	EnchantmentProvider getEnchantmentProvider(String id);

	/**
	 * Retrieves the list of enchantments for a given ItemStack using the registered
	 * EnchantmentProviders.
	 *
	 * @param itemStack the ItemStack for which to retrieve the enchantments
	 * @return a list of enchantments and their levels
	 */
	List<Pair<String, Short>> getEnchantments(ItemStack itemStack);

	/**
	 * Registers an ItemProvider.
	 *
	 * @param itemProvider the ItemProvider to register
	 * @return true if registration is successful, false otherwise.
	 */
	boolean registerItemProvider(@NotNull ItemProvider itemProvider);

	/**
	 * Unregisters an ItemProvider by its ID.
	 *
	 * @param id the ID of the ItemProvider to unregister
	 * @return true if unregistration is successful, false otherwise.
	 */
	boolean unregisterItemProvider(@NotNull String id);

	/**
	 * Retrieves a registered ItemProvider by its ID.
	 *
	 * @param id the ID of the ItemProvider to retrieve
	 * @return the ItemProvider if found, or null if not found
	 */
	@Nullable
	ItemProvider getItemProvider(String id);

	/**
	 * Registers a BlockProvider.
	 *
	 * @param block the BlockProvider to register
	 * @return true if registration is successful, false otherwise.
	 */
	boolean registerBlockProvider(@NotNull BlockProvider block);

	/**
	 * Unregisters a BlockProvider by its ID.
	 *
	 * @param id the ID of the BlockProvider to unregister
	 * @return true if unregistration is successful, false otherwise.
	 */
	boolean unregisterBlockProvider(@NotNull String id);

	/**
	 * Retrieves a registered BlockProvider by its ID.
	 *
	 * @param id the ID of the BlockProvider to retrieve
	 * @return the BlockProvider if found, or null if not found
	 */
	@Nullable
	BlockProvider getBlockProvider(String id);
}