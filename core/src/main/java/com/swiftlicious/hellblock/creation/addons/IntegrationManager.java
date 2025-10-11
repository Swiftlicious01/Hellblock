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

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.addons.enchant.AdvancedEnchantmentsProvider;
import com.swiftlicious.hellblock.creation.addons.enchant.EnchantmentProvider;
import com.swiftlicious.hellblock.creation.addons.enchant.VanillaEnchantmentsProvider;
import com.swiftlicious.hellblock.creation.addons.level.JobsRebornLevelerProvider;
import com.swiftlicious.hellblock.creation.addons.level.LevelerProvider;
import com.swiftlicious.hellblock.creation.addons.papi.HellblockPapi;
import com.swiftlicious.hellblock.creation.addons.papi.StatisticsPapi;
import com.swiftlicious.hellblock.creation.addons.shop.ShopGUIHook;
import com.swiftlicious.hellblock.creation.block.BlockProvider;
import com.swiftlicious.hellblock.creation.entity.EntityProvider;
import com.swiftlicious.hellblock.creation.entity.MythicEntityProvider;
import com.swiftlicious.hellblock.creation.item.ItemProvider;
import com.swiftlicious.hellblock.creation.item.MythicMobsItemProvider;
import com.swiftlicious.hellblock.creation.item.SNBTItemProvider;
import com.swiftlicious.hellblock.utils.extras.Pair;

public class IntegrationManager implements IntegrationManagerInterface {

	private static IntegrationManager integration;
	protected final HellblockPlugin instance;
	private final Map<String, LevelerProvider> levelerProviders = new HashMap<>();
	private final Map<String, EnchantmentProvider> enchantmentProviders = new HashMap<>();

	private boolean hasFloodGate;
	private boolean hasGeyser;

	public IntegrationManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void init() {
		try {
			this.load();
		} catch (Throwable ex) {
			instance.getPluginLogger().warn("Failed to load integrations", ex);
		} finally {
			integration = this;
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
		if (isHooked("Jobs") || isHooked("JobsReborn")) {
			registerLevelerProvider(new JobsRebornLevelerProvider());
		}
		registerItemProvider(new SNBTItemProvider());
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
		if (isHooked("ShopGUIPlus")) {
			ShopGUIHook.register();
		}

		if (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null) {
			this.hasGeyser = true;
		}
		if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
			this.hasFloodGate = true;
		}
	}

	public static IntegrationManager getInstance() {
		return integration;
	}

	public boolean isHooked(String hooked) {
		if (Bukkit.getPluginManager().getPlugin(hooked) == null) {
			return false;
		}
		instance.getPluginLogger().info(hooked + " hooked!");
		return true;
	}

	@SuppressWarnings("deprecation")
	public boolean isHooked(String hooked, String... versionPrefix) {
		final Plugin p = Bukkit.getPluginManager().getPlugin(hooked);
		if (p != null) {
			final String ver = p.getDescription().getVersion();
			for (String prefix : versionPrefix) {
				if (ver.startsWith(prefix)) {
					instance.getPluginLogger().info(hooked + " hooked!");
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasFloodGate() {
		return hasFloodGate;
	}

	public boolean hasGeyser() {
		return hasGeyser;
	}

	@Override
	public boolean registerLevelerProvider(@NotNull LevelerProvider leveler) {
		if (levelerProviders.containsKey(leveler.identifier())) {
			return false;
		}
		levelerProviders.put(leveler.identifier(), leveler);
		return true;
	}

	@Override
	public boolean unregisterLevelerProvider(@NotNull String id) {
		return levelerProviders.remove(id) != null;
	}

	@Override
	public boolean registerEnchantmentProvider(@NotNull EnchantmentProvider enchantment) {
		if (enchantmentProviders.containsKey(enchantment.identifier())) {
			return false;
		}
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
		final List<Pair<String, Short>> list = new ArrayList<>();
		enchantmentProviders.values()
				.forEach(enchantmentProvider -> list.addAll(enchantmentProvider.getEnchants(itemStack)));
		return list;
	}

	@Override
	public boolean registerEntityProvider(@NotNull EntityProvider entity) {
		return instance.getEntityManager().registerEntityProvider(entity);
	}

	@Override
	public boolean unregisterEntityProvider(@NotNull String id) {
		return instance.getEntityManager().unregisterEntityProvider(id);
	}

	@Override
	public EntityProvider getEntityProvider(@NotNull String id) {
		return instance.getEntityManager().getEntityProvider(id);
	}

	@Override
	public boolean registerItemProvider(@NotNull ItemProvider item) {
		return instance.getItemManager().registerItemProvider(item);
	}

	@Override
	public boolean unregisterItemProvider(@NotNull String id) {
		return instance.getItemManager().unregisterItemProvider(id);
	}

	@Override
	public ItemProvider getItemProvider(@NotNull String id) {
		return instance.getItemManager().getItemProvider(id);
	}

	@Override
	public boolean registerBlockProvider(@NotNull BlockProvider block) {
		return instance.getBlockManager().registerBlockProvider(block);
	}

	@Override
	public boolean unregisterBlockProvider(@NotNull String id) {
		return instance.getBlockManager().unregisterBlockProvider(id);
	}

	@Override
	public BlockProvider getBlockProvider(@NotNull String id) {
		return instance.getBlockManager().getBlockProvider(id);
	}
}