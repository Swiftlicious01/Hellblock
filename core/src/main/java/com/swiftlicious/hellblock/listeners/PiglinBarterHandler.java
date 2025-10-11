package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeData;
import com.swiftlicious.hellblock.upgrades.UpgradeTier;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;

public final class PiglinBarterHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private final Map<UUID, Set<UUID>> barterTracker = new ConcurrentHashMap<>();

	// Cache of island owner UUID to barter bonus
	private final Map<UUID, Double> piglinBarterBonusCache = new ConcurrentHashMap<>();

	public PiglinBarterHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		barterTracker.clear();
		piglinBarterBonusCache.clear();
	}

	private void trackBarter(UUID playerId, UUID piglinId) {
		barterTracker.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(piglinId);
	}

	@EventHandler
	public void onGiveItemOfInterest(PlayerInteractEntityEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final ItemStack inHand = player.getInventory().getItem(event.getHand());
		if (inHand.getType() != Material.GOLD_INGOT || !(event.getRightClicked() instanceof Piglin piglin)) {
			return;
		}

		if (!piglin.isAdult() || piglin.getEquipment().getItemInOffHand().getType() != Material.AIR) {
			return;
		}

		trackBarter(player.getUniqueId(), piglin.getUniqueId());
	}

	@EventHandler
	public void onPiglinPickUpItemOfInterest(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Piglin piglin)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(piglin.getWorld())) {
			return;
		}
		if (!piglin.isAdult()) {
			return;
		}
		if (event.getItem().getItemStack().getType() != Material.GOLD_INGOT) {
			return;
		}

		final UUID thrower = event.getItem().getThrower();
		if (thrower != null && Bukkit.getPlayer(thrower) != null) {
			trackBarter(thrower, piglin.getUniqueId());
		}
	}

	@EventHandler
	public void onBarterWithPiglin(PiglinBarterEvent event) {
		final Piglin piglin = event.getEntity();
		if (!instance.getHellblockHandler().isInCorrectWorld(piglin.getWorld())) {
			return;
		}

		final List<ItemStack> barteredItems = event.getOutcome();

		if (instance.getConfigManager().clearDefaultOutcome()
				&& !instance.getConfigManager().barteringItems().isEmpty()) {
			barteredItems.clear();
		}

		// Find player who bartered with this piglin
		final Optional<UUID> playerOpt = barterTracker.entrySet().stream()
				.filter(entry -> entry.getValue().contains(piglin.getUniqueId())).map(Map.Entry::getKey).findFirst();

		if (playerOpt.isEmpty())
			return;

		final UUID playerUUID = playerOpt.get();
		final Player player = Bukkit.getPlayer(playerUUID);
		if (player == null)
			return;

		final Context<Player> context = Context.player(player);

		// Check if the piglin is in a valid island region
		instance.getCoopManager().getHellblockOwnerOfBlock(piglin.getLocation().getBlock())
				.thenAcceptAsync(ownerUUID -> {
					if (ownerUUID == null) {
						return; // Piglin is not within any island
					}

					instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
							.thenAccept(userDataOpt -> {
								if (userDataOpt.isEmpty())
									return;

								HellblockData hellblockData = userDataOpt.get().getHellblockData();
								Set<UUID> partyPlusOwner = hellblockData.getPartyPlusOwner();
								BoundingBox box = hellblockData.getBoundingBox();

								boolean isMember = partyPlusOwner.contains(playerUUID);
								boolean isInsideIsland = box != null && box.contains(piglin.getLocation().toVector());

								double bonus = 0.0;
								if (isMember && isInsideIsland) {
									bonus = getCachedBarterBonus(hellblockData);
								}

								// Apply custom barter logic
								if (!instance.getConfigManager().barteringItems().isEmpty()) {
									final int rolls = RandomUtils.generateRandomInt(1, 3);
									final List<ItemStack> customItems = pickWeightedItems(context,
											instance.getConfigManager().barteringItems(), rolls, bonus);
									barteredItems.addAll(customItems);
								}

								// Progress challenges for every outcome item
								barteredItems.forEach(bartered -> {
									final int amount = bartered.getAmount();
									instance.getChallengeManager().handleChallengeProgression(player, ActionType.BARTER,
											bartered.clone(), amount);
								});

								// Cleanup
								barterTracker.computeIfPresent(playerUUID, (uuid, piglins) -> {
									piglins.remove(piglin.getUniqueId());
									return piglins.isEmpty() ? null : piglins;
								});
							});
				});
	}

	/**
	 * Picks weighted random items from a map of CustomItem -> weight expression.
	 *
	 * @param context MathContext (usually with Player info) for evaluation
	 * @param entries map of items with their weight values
	 * @param rolls   how many times to roll
	 * @param bonus   the bonus to apply to the weight
	 * @return list of selected ItemStacks
	 */
	private List<ItemStack> pickWeightedItems(Context<Player> context, Map<CustomItem, MathValue<Player>> entries,
			int rolls, double bonus) {

		final List<ItemStack> results = new ArrayList<>();
		if (entries.isEmpty() || rolls <= 0) {
			return results;
		}

		// Compute total weight with bonus applied for items < 5.0 base weight
		final Map<CustomItem, Integer> effectiveWeights = new HashMap<>();
		int totalWeight = 0;

		for (Map.Entry<CustomItem, MathValue<Player>> entry : entries.entrySet()) {
			double baseWeight = entry.getValue().evaluate(context);
			if (baseWeight <= 0)
				baseWeight = 1.0;

			double effectiveWeight = baseWeight;

			// Apply bonus only if base weight is below 5.0
			if (baseWeight < 5.0) {
				effectiveWeight += bonus;
			}

			int finalWeight = (int) Math.max(1, Math.floor(effectiveWeight));
			effectiveWeights.put(entry.getKey(), finalWeight);
			totalWeight += finalWeight;
		}

		// Perform N rolls
		for (int i = 0; i < rolls; i++) {
			int random = RandomUtils.generateRandomInt(1, totalWeight);
			CustomItem selectedItem = null;

			for (Map.Entry<CustomItem, Integer> entry : effectiveWeights.entrySet()) {
				int weight = entry.getValue();
				if (random <= weight) {
					selectedItem = entry.getKey();
					break;
				}
				random -= weight;
			}

			if (selectedItem != null) {
				final Item<ItemStack> wrapped = instance.getItemManager().wrap(selectedItem.build(context));
				final ItemStack item = wrapped.loadCopy();
				item.setAmount(RandomUtils.generateRandomInt(1, (int) selectedItem.amount().evaluate(context)));
				results.add(item);
			}
		}
		return results;
	}

	private double calculateBarterBonus(@NotNull HellblockData data) {
		int level = data.getUpgradeLevel(IslandUpgradeType.PIGLIN_BARTERING);
		double total = 0.0;

		for (int i = 0; i <= level; i++) {
			UpgradeTier tier = instance.getUpgradeManager().getTier(i);
			if (tier != null) {
				UpgradeData upgrade = tier.getUpgrade(IslandUpgradeType.PIGLIN_BARTERING);
				if (upgrade != null && upgrade.getValue() != null) {
					total += upgrade.getValue().doubleValue();
				}
			}
		}
		return total;
	}

	public double getCachedBarterBonus(@NotNull HellblockData data) {
		UUID ownerUUID = data.getOwnerUUID();
		return piglinBarterBonusCache.computeIfAbsent(ownerUUID, id -> calculateBarterBonus(data));
	}

	public void updateBarterBonusCache(@NotNull HellblockData data) {
		piglinBarterBonusCache.put(data.getOwnerUUID(), calculateBarterBonus(data));
	}

	public void invalidateBarterBonusCache(@NotNull UUID ownerUUID) {
		piglinBarterBonusCache.remove(ownerUUID);
	}
}