package com.swiftlicious.hellblock.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.player.OnlineUser;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;

public class PiglinBartering implements Listener {

	private final HellblockPlugin instance;

	private boolean clearPiglinBarterOutcome;
	private final Map<Material, Integer> netherBarteringItems;

	private final Map<UUID, Set<UUID>> barterTracker;

	public PiglinBartering(HellblockPlugin plugin) {
		instance = plugin;
		this.clearPiglinBarterOutcome = instance.getConfig("config.yml")
				.getBoolean("piglin-bartering.clear-default-outcome", true);
		this.netherBarteringItems = new HashMap<>();
		this.barterTracker = new LinkedHashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	private Map<Material, Integer> getNetherBarteringItems() {
		if (!this.netherBarteringItems.isEmpty())
			return this.netherBarteringItems;
		Set<String> barteringItems = new HashSet<>(
				instance.getConfig("config.yml").getStringList("piglin-bartering.materials"));
		for (String barterItem : barteringItems) {
			String[] split = barterItem.split(":");
			Material mat = Material.getMaterial(split[0].toUpperCase());
			if (mat == null || mat == Material.AIR)
				continue;
			int amount = 1;
			try {
				amount = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			this.netherBarteringItems.put(mat, amount);
		}
		return this.netherBarteringItems;
	}

	@EventHandler
	public void onGiveItemOfInterest(PlayerInteractEntityEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		final UUID id = player.getUniqueId();
		ItemStack inHand = player.getInventory().getItemInMainHand();
		if (inHand.getType() != Material.GOLD_INGOT) {
			inHand = player.getInventory().getItemInOffHand();
		}
		if (inHand.getType() != Material.GOLD_INGOT) {
			if (event.getRightClicked() instanceof Piglin piglin) {
				if (!piglin.isAdult())
					return;
				if (piglin.getEquipment().getItemInOffHand().getType() != Material.GOLD_INGOT) {
					if (!barterTracker.containsKey(id))
						barterTracker.put(id, new HashSet<>(Set.of(piglin.getUniqueId())));
					else {
						if (!barterTracker.get(id).contains(piglin.getUniqueId()))
							barterTracker.get(id).add(piglin.getUniqueId());
					}
				}
			}
		}
	}

	@EventHandler
	public void onPiglinPickUpItemOfInterest(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Piglin piglin) {
			if (!piglin.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				return;
			if (!piglin.isAdult())
				return;
			if (event.getItem().getItemStack().getType() == Material.GOLD_INGOT) {
				UUID playerUUID = event.getItem().getThrower();
				if (playerUUID != null && Bukkit.getPlayer(playerUUID) != null) {
					if (!barterTracker.containsKey(playerUUID))
						barterTracker.put(playerUUID, new HashSet<>(Set.of(piglin.getUniqueId())));
					else {
						if (!barterTracker.get(playerUUID).contains(piglin.getUniqueId()))
							barterTracker.get(playerUUID).add(piglin.getUniqueId());
					}
				}
			}
		}
	}

	@EventHandler
	public void onBarterWithPiglin(PiglinBarterEvent event) {
		final Piglin piglin = event.getEntity();
		if (!piglin.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		List<ItemStack> barteredItems = event.getOutcome();
		if (this.clearPiglinBarterOutcome && !getNetherBarteringItems().isEmpty())
			barteredItems.clear();
		if (!getNetherBarteringItems().isEmpty()) {
			getNetherBarteringItems().forEach((item, amount) -> {
				barteredItems.add(new ItemStack(item, RandomUtils.generateRandomInt(1, amount)));
			});
		}

		if (barteredItems.stream().filter(item -> getNetherBarteringItems().keySet().contains(item.getType())).findAny()
				.isPresent()) {
			UUID playerUUID = null;
			for (Entry<UUID, Set<UUID>> entry : barterTracker.entrySet()) {
				if (entry.getValue().contains(piglin.getUniqueId())) {
					playerUUID = entry.getKey();
					break;
				}
			}
			if (playerUUID != null) {
				OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(playerUUID);
				if (onlineUser == null)
					return;
				if (!onlineUser.getHellblockData().isChallengeActive(ChallengeType.NETHER_TRADING_CHALLENGE)
						&& !onlineUser.getHellblockData()
								.isChallengeCompleted(ChallengeType.NETHER_TRADING_CHALLENGE)) {
					onlineUser.getHellblockData().beginChallengeProgression(onlineUser.getPlayer(),
							ChallengeType.NETHER_TRADING_CHALLENGE);
				} else {
					onlineUser.getHellblockData().updateChallengeProgression(onlineUser.getPlayer(),
							ChallengeType.NETHER_TRADING_CHALLENGE, 1);
					if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHER_TRADING_CHALLENGE)) {
						onlineUser.getHellblockData().completeChallenge(onlineUser.getPlayer(),
								ChallengeType.NETHER_TRADING_CHALLENGE);
					}
				}
				barterTracker.get(playerUUID).remove(piglin.getUniqueId());
				if (barterTracker.get(playerUUID).isEmpty())
					barterTracker.remove(playerUUID);
			}
		}
	}
}
