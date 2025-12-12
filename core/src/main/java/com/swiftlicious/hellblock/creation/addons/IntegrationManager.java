package com.swiftlicious.hellblock.creation.addons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.addons.enchant.AdvancedEnchantmentsProvider;
import com.swiftlicious.hellblock.creation.addons.enchant.CustomEnchantmentsProvider;
import com.swiftlicious.hellblock.creation.addons.enchant.EnchantmentProvider;
import com.swiftlicious.hellblock.creation.addons.enchant.VanillaEnchantmentsProvider;
import com.swiftlicious.hellblock.creation.addons.level.JobsRebornLevelerProvider;
import com.swiftlicious.hellblock.creation.addons.level.LevelerProvider;
import com.swiftlicious.hellblock.creation.addons.level.McMMOLevelerProvider;
import com.swiftlicious.hellblock.creation.addons.mobstacker.StackMobHook;
import com.swiftlicious.hellblock.creation.addons.mobstacker.WildStackerHook;
import com.swiftlicious.hellblock.creation.addons.papi.HellblockPapi;
import com.swiftlicious.hellblock.creation.addons.papi.StatisticsPapi;
import com.swiftlicious.hellblock.creation.addons.pet.PetProvider;
import com.swiftlicious.hellblock.creation.addons.pet.SimplePetsHook;
import com.swiftlicious.hellblock.creation.addons.shop.ShopGUIHook;
import com.swiftlicious.hellblock.creation.addons.shop.sign.ChestShopHook;
import com.swiftlicious.hellblock.creation.addons.shop.sign.QuickShopHook;
import com.swiftlicious.hellblock.creation.addons.shop.sign.ShopSignProvider;
import com.swiftlicious.hellblock.creation.addons.shop.sign.TradeShopHook;
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
	private final Map<String, PetProvider> petProviders = new HashMap<>();
	private final Map<String, ShopSignProvider> shopSignProviders = new HashMap<>();
	private final Map<String, EnchantmentProvider> enchantmentProviders = new HashMap<>();

	private boolean hasFloodGate;
	private boolean hasGeyser;

	public IntegrationManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void initialize() {
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
		this.petProviders.clear();
		this.shopSignProviders.clear();
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
		if (isHooked("mcMMO")) {
			registerLevelerProvider(new McMMOLevelerProvider());
		}

		registerItemProvider(new SNBTItemProvider());

		registerEnchantmentProvider(new VanillaEnchantmentsProvider());
		registerEnchantmentProvider(new CustomEnchantmentsProvider(instance));
		if (isHooked("AdvancedEnchantments")) {
			registerEnchantmentProvider(new AdvancedEnchantmentsProvider());
		}

		if (isHooked("Vault")) {
			VaultHook.init();
		}
		
		if (isHooked("PlayerPoints")) {
			PlayerPointsHook.register();
		}

		if (isHooked("PlaceholderAPI")) {
			new HellblockPapi(instance).load();
			new StatisticsPapi(instance).load();
		}

		if (isHooked("ShopGUIPlus")) {
			ShopGUIHook.register();
		}

		if (isHooked("StackMob")) {
			StackMobHook.register();
		}
		if (isHooked("WildStacker")) {
			WildStackerHook.register();
		}

		if (isHooked("SimplePets")) {
			registerPetProvider(new SimplePetsHook());
		}

		if (isHooked("ChestShop")) {
			registerShopSignProvider(new ChestShopHook());
		}
		if (isHooked("TradeShop")) {
			registerShopSignProvider(new TradeShopHook());
		}
		if (isHooked("QuickShop")) {
			registerShopSignProvider(new QuickShopHook());
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
	public boolean registerPetProvider(@NotNull PetProvider pet) {
		if (petProviders.containsKey(pet.identifier())) {
			return false;
		}
		petProviders.put(pet.identifier(), pet);
		return true;
	}

	@Override
	public boolean unregisterPetProvider(@NotNull String id) {
		return petProviders.remove(id) != null;
	}

	@Override
	@Nullable
	public PetProvider getPetProvider(String plugin) {
		return petProviders.get(plugin);
	}

	@Override
	public Set<PetProvider> getPetProviders() {
		return new HashSet<>(petProviders.values());
	}

	@Override
	public boolean registerShopSignProvider(@NotNull ShopSignProvider sign) {
		if (shopSignProviders.containsKey(sign.identifier())) {
			return false;
		}
		shopSignProviders.put(sign.identifier(), sign);
		return true;
	}

	@Override
	public boolean unregisterShopSignProvider(@NotNull String id) {
		return shopSignProviders.remove(id) != null;
	}

	@Override
	@Nullable
	public ShopSignProvider getShopSignProvider(String plugin) {
		return shopSignProviders.get(plugin);
	}

	@Override
	public Set<ShopSignProvider> getShopSignProviders() {
		return new HashSet<>(shopSignProviders.values());
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
	@Nullable
	public LevelerProvider getLevelerProvider(String plugin) {
		return levelerProviders.get(plugin);
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