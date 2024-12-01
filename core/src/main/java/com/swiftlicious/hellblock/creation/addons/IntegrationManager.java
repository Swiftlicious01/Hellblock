package com.swiftlicious.hellblock.creation.addons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.grinderwolf.swm.api.SlimePlugin;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.VaultHook;
import com.swiftlicious.hellblock.creation.addons.enchant.AdvancedEnchantmentsProvider;
import com.swiftlicious.hellblock.creation.addons.enchant.EnchantmentProvider;
import com.swiftlicious.hellblock.creation.addons.enchant.VanillaEnchantmentsProvider;
import com.swiftlicious.hellblock.creation.addons.level.JobsRebornLevelerProvider;
import com.swiftlicious.hellblock.creation.addons.level.LevelerProvider;
import com.swiftlicious.hellblock.creation.addons.papi.HellblockPapi;
import com.swiftlicious.hellblock.creation.addons.papi.StatisticsPapi;
import com.swiftlicious.hellblock.creation.block.BlockManager;
import com.swiftlicious.hellblock.creation.block.BlockProvider;
import com.swiftlicious.hellblock.creation.entity.EntityManager;
import com.swiftlicious.hellblock.creation.entity.EntityProvider;
import com.swiftlicious.hellblock.creation.entity.MythicEntityProvider;
import com.swiftlicious.hellblock.creation.item.ItemManager;
import com.swiftlicious.hellblock.creation.item.ItemProvider;
import com.swiftlicious.hellblock.creation.item.MythicMobsItemProvider;
import com.swiftlicious.hellblock.events.worldguard.Entry;
import com.swiftlicious.hellblock.utils.extras.Pair;

public class IntegrationManager implements IntegrationManagerInterface {

	protected final HellblockPlugin instance;
	private final Map<String, LevelerProvider> levelerProviders = new HashMap<>();
	private final Map<String, EnchantmentProvider> enchantmentProviders = new HashMap<>();

	public IntegrationManager(HellblockPlugin plugin) {
		instance = plugin;
		try {
			this.load();
		} catch (Exception e) {
			plugin.getPluginLogger().warn("Failed to load integrations", e);
		}
	}

	@Override
	public void disable() {
		this.enchantmentProviders.clear();
		this.levelerProviders.clear();
	}

	@Override
	public void load() {
		if (isHooked("MythicMobs", "5")) {
			registerItemProvider(new MythicMobsItemProvider());
			registerEntityProvider(new MythicEntityProvider());
		}
		if (isHooked("Jobs")) {
			registerLevelerProvider(new JobsRebornLevelerProvider());
		}
		registerEnchantmentProvider(new VanillaEnchantmentsProvider());
		if (isHooked("AdvancedEnchantments")) {
			registerEnchantmentProvider(new AdvancedEnchantmentsProvider());
		}
		if (isHooked("Vault")) {
			VaultHook.init();
		}
		if (isHooked("PlaceholderAPI")) {
			new HellblockPapi(instance).load();
			new StatisticsPapi(instance).load();
		}
		if (isHooked("Multiverse-Core")) {
			Plugin multiverse = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
			if (multiverse != null && multiverse instanceof MultiverseCore) {
				instance.getHellblockHandler().setMVWorldManager(((MultiverseCore) multiverse).getMVWorldManager());
			}
		}
		if (isHooked("SlimeWorldManager")) {
			Plugin slimeWorld = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
			if (slimeWorld != null && slimeWorld instanceof SlimePlugin) {
				instance.getHellblockHandler().setSlimeWorldManager(((SlimePlugin) slimeWorld));
			}
		}
		if (isHooked("WorldGuard", "7")) {
			if (instance.getConfigManager().worldguardProtect()) {
				Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
				if (worldGuard != null && worldGuard instanceof WorldGuardPlugin) {
					if (!WorldGuard.getInstance().getPlatform().getSessionManager().registerHandler(Entry.factory,
							null)) {
						instance.getPluginLogger().warn("Could not register the WorldGuard handler for Hellblock.");
						return;
					}
					instance.getWorldGuardHandler().setWorldGuardPlatform(WorldGuard.getInstance().getPlatform());
				}
			}
		} else {
			if (instance.getConfigManager().worldguardProtect()) {
				Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
				if (worldGuard != null && worldGuard instanceof WorldGuardPlugin) {
					String version = WorldGuard.getVersion();
					if (!version.startsWith("7.")) {
						instance.getPluginLogger()
								.warn("WorldGuard version must be 7.0 or higher to be able to use it for Hellblock.");
						return;
					}
				}
			}
		}
	}

	public boolean isHooked(String hooked) {
		if (Bukkit.getPluginManager().getPlugin(hooked) != null) {
			instance.getPluginLogger().info(hooked + " hooked!");
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	public boolean isHooked(String hooked, String... versionPrefix) {
		Plugin p = Bukkit.getPluginManager().getPlugin(hooked);
		if (p != null) {
			String ver = p.getDescription().getVersion();
			for (String prefix : versionPrefix) {
				if (ver.startsWith(prefix)) {
					instance.getPluginLogger().info(hooked + " hooked!");
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean registerLevelerProvider(@NotNull LevelerProvider leveler) {
		if (levelerProviders.containsKey(leveler.identifier()))
			return false;
		levelerProviders.put(leveler.identifier(), leveler);
		return true;
	}

	@Override
	public boolean unregisterLevelerProvider(@NotNull String id) {
		return levelerProviders.remove(id) != null;
	}

	@Override
	public boolean registerEnchantmentProvider(@NotNull EnchantmentProvider enchantment) {
		if (enchantmentProviders.containsKey(enchantment.identifier()))
			return false;
		enchantmentProviders.put(enchantment.identifier(), enchantment);
		return true;
	}

	@Override
	public boolean unregisterEnchantmentProvider(@NotNull String id) {
		return enchantmentProviders.remove(id) != null;
	}

	@Override
	@Nullable
	public LevelerProvider getLevelerProvider(String plugin) {
		return levelerProviders.get(plugin);
	}

	@Override
	@Nullable
	public EnchantmentProvider getEnchantmentProvider(String id) {
		return enchantmentProviders.get(id);
	}

	@Override
	public List<Pair<String, Short>> getEnchantments(ItemStack itemStack) {
		List<Pair<String, Short>> list = new ArrayList<>();
		for (EnchantmentProvider enchantmentProvider : enchantmentProviders.values()) {
			list.addAll(enchantmentProvider.getEnchants(itemStack));
		}
		return list;
	}

	@Override
	public boolean registerEntityProvider(@NotNull EntityProvider entity) {
		return ((EntityManager) instance.getEntityManager()).registerEntityProvider(entity);
	}

	@Override
	public boolean unregisterEntityProvider(@NotNull String id) {
		return ((EntityManager) instance.getEntityManager()).unregisterEntityProvider(id);
	}

	@Override
	public boolean registerItemProvider(@NotNull ItemProvider item) {
		return ((ItemManager) instance.getItemManager()).registerItemProvider(item);
	}

	@Override
	public boolean unregisterItemProvider(@NotNull String id) {
		return ((ItemManager) instance.getItemManager()).unregisterItemProvider(id);
	}

	@Override
	public boolean registerBlockProvider(@NotNull BlockProvider block) {
		return ((BlockManager) instance.getBlockManager()).registerBlockProvider(block);
	}

	@Override
	public boolean unregisterBlockProvider(@NotNull String id) {
		return ((BlockManager) instance.getBlockManager()).unregisterBlockProvider(id);
	}
}