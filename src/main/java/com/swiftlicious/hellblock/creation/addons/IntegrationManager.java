package com.swiftlicious.hellblock.creation.addons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.VaultHook;
import com.swiftlicious.hellblock.creation.addons.enchant.AdvancedEnchantments;
import com.swiftlicious.hellblock.creation.addons.level.JobsReborn;
import com.swiftlicious.hellblock.creation.entity.MythicMobsEntity;
import com.swiftlicious.hellblock.creation.item.MythicMobsItem;
import com.swiftlicious.hellblock.utils.LogUtils;

public class IntegrationManager implements IntegrationManagerInterface {

	private final HellblockPlugin instance;
	private final Map<String, LevelInterface> levelPluginMap;
	private final Map<String, EnchantmentInterface> enchantmentPluginMap;

	public IntegrationManager(HellblockPlugin plugin) {
		instance = plugin;
		this.levelPluginMap = new HashMap<>();
		this.enchantmentPluginMap = new HashMap<>();
		this.load();
	}

	public void disable() {
		this.enchantmentPluginMap.clear();
		this.levelPluginMap.clear();
	}

	public void load() {
		if (instance.isHookedPluginEnabled("MythicMobs")) {
			instance.getItemManager().registerItemLibrary(new MythicMobsItem());
			instance.getEntityManager().registerEntityLibrary(new MythicMobsEntity());
			hookMessage("MythicMobs");
		}
		if (instance.isHookedPluginEnabled("Jobs")) {
			registerLevelPlugin("JobsReborn", new JobsReborn());
			hookMessage("JobsReborn");
		}
		if (instance.isHookedPluginEnabled("EcoEnchants")) {
			this.enchantmentPluginMap.put("EcoEnchants", new VanillaEnchantment());
			hookMessage("EcoEnchants");
		} else {
			this.enchantmentPluginMap.put("vanilla", new VanillaEnchantment());
		}
		if (instance.isHookedPluginEnabled("AdvancedEnchantments")) {
			this.enchantmentPluginMap.put("AdvancedEnchantments", new AdvancedEnchantments());
			hookMessage("AdvancedEnchantments");
		}
		if (instance.isHookedPluginEnabled("Vault")) {
			VaultHook.initialize();
			hookMessage("Vault");
		}
		if (instance.isHookedPluginEnabled("PlaceholderAPI")) {
			instance.getPlaceholderManager().loadCustomPlaceholders();
			hookMessage("PlaceholderAPI");
		}
		if (instance.isHookedPluginEnabled("Multiverse-Core")) {
			Plugin multiverse = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
			if (multiverse != null && multiverse instanceof MultiverseCore) {
				instance.getHellblockHandler().setMvWorldManager(((MultiverseCore) multiverse).getMVWorldManager());
				hookMessage("Multiverse-Core");
			}
		}
		if (instance.isHookedPluginEnabled("WorldGuard")) {
			if (instance.getHellblockHandler().isWorldguardProtect()) {
				Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
				if (worldGuard != null && worldGuard instanceof WorldGuardPlugin) {
					instance.getWorldGuardHandler().setWorldGuardPlatform(WorldGuard.getInstance().getPlatform());
					hookMessage("WorldGuard");
				}
			}
		}
	}

	/**
	 * Registers a level plugin with the specified name.
	 *
	 * @param plugin The name of the level plugin.
	 * @param level  The implementation of the LevelInterface.
	 * @return true if the registration was successful, false if the plugin name is
	 *         already registered.
	 */
	@Override
	public boolean registerLevelPlugin(String plugin, LevelInterface level) {
		if (levelPluginMap.containsKey(plugin))
			return false;
		levelPluginMap.put(plugin, level);
		return true;
	}

	/**
	 * Unregisters a level plugin with the specified name.
	 *
	 * @param plugin The name of the level plugin to unregister.
	 * @return true if the unregistration was successful, false if the plugin name
	 *         is not found.
	 */
	@Override
	public boolean unregisterLevelPlugin(String plugin) {
		return levelPluginMap.remove(plugin) != null;
	}

	/**
	 * Registers an enchantment provided by a plugin.
	 *
	 * @param plugin      The name of the plugin providing the enchantment.
	 * @param enchantment The enchantment to register.
	 * @return true if the registration was successful, false if the enchantment
	 *         name is already in use.
	 */
	@Override
	public boolean registerEnchantment(String plugin, EnchantmentInterface enchantment) {
		if (enchantmentPluginMap.containsKey(plugin))
			return false;
		enchantmentPluginMap.put(plugin, enchantment);
		return true;
	}

	/**
	 * Unregisters an enchantment provided by a plugin.
	 *
	 * @param plugin The name of the plugin providing the enchantment.
	 * @return true if the enchantment was successfully unregistered, false if the
	 *         enchantment was not found.
	 */
	@Override
	public boolean unregisterEnchantment(String plugin) {
		return enchantmentPluginMap.remove(plugin) != null;
	}

	public void hookMessage(String plugin) {
		LogUtils.info(String.format("%s hooked!", plugin));
	}

	/**
	 * Get the LevelInterface provided by a plugin.
	 *
	 * @param plugin The name of the plugin providing the LevelInterface.
	 * @return The LevelInterface provided by the specified plugin, or null if the
	 *         plugin is not registered.
	 */
	@Override
	@Nullable
	public LevelInterface getLevelPlugin(String plugin) {
		return levelPluginMap.get(plugin);
	}

	/**
	 * Get an enchantment plugin by its plugin name.
	 *
	 * @param plugin The name of the enchantment plugin.
	 * @return The enchantment plugin interface, or null if not found.
	 */
	@Override
	@Nullable
	public EnchantmentInterface getEnchantmentPlugin(String plugin) {
		return enchantmentPluginMap.get(plugin);
	}

	/**
	 * Get a list of enchantment keys with level applied to the given ItemStack.
	 *
	 * @param itemStack The ItemStack to check for enchantments.
	 * @return A list of enchantment names applied to the ItemStack.
	 */
	@Override
	public List<String> getEnchantments(ItemStack itemStack) {
		ArrayList<String> list = new ArrayList<>();
		for (EnchantmentInterface enchantmentInterface : enchantmentPluginMap.values()) {
			list.addAll(enchantmentInterface.getEnchants(itemStack));
		}
		return list;
	}
}