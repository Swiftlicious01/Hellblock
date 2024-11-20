package com.swiftlicious.hellblock.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.RandomUtils;

public class PiglinBartering implements Listener {

	protected final HellblockPlugin instance;

	private final Map<Material, Integer> netherBarteringItems;

	private final Map<UUID, Set<UUID>> barterTracker;

	public PiglinBartering(HellblockPlugin plugin) {
		instance = plugin;
		this.netherBarteringItems = new HashMap<>();
		this.barterTracker = new LinkedHashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	private Map<Material, Integer> getNetherBarteringItems() {
		if (!this.netherBarteringItems.isEmpty())
			return this.netherBarteringItems;
		Set<String> barteringItems = new HashSet<>(instance.getConfigManager().barteringItems());
		for (String barterItem : barteringItems) {
			String[] split = barterItem.split(":");
			Material mat = Material.getMaterial(split[0].toUpperCase());
			if (mat == null || mat == Material.AIR)
				continue;
			int amount = 1;
			try {
				amount = Integer.parseInt(split[1]);
			} catch (NumberFormatException ex) {
				instance.getPluginLogger().severe(String.format("Invalid quantity: %s!", split[1]));
				continue;
			}
			this.netherBarteringItems.put(mat, amount);
		}
		return this.netherBarteringItems;
	}

	@EventHandler
	public void onGiveItemOfInterest(PlayerInteractEntityEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
			if (!piglin.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
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
		if (!piglin.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		List<ItemStack> barteredItems = event.getOutcome();
		if (instance.getConfigManager().clearDefaultOutcome() && !getNetherBarteringItems().isEmpty())
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
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerUUID);
				if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null)
					return;
				if (!onlineUser.get().getChallengeData().isChallengeActive(ChallengeType.NETHER_TRADING_CHALLENGE)
						&& !onlineUser.get().getChallengeData()
								.isChallengeCompleted(ChallengeType.NETHER_TRADING_CHALLENGE)) {
					onlineUser.get().getChallengeData().beginChallengeProgression(onlineUser.get().getPlayer(),
							ChallengeType.NETHER_TRADING_CHALLENGE);
				} else {
					onlineUser.get().getChallengeData().updateChallengeProgression(onlineUser.get().getPlayer(),
							ChallengeType.NETHER_TRADING_CHALLENGE, 1);
					if (onlineUser.get().getChallengeData()
							.isChallengeCompleted(ChallengeType.NETHER_TRADING_CHALLENGE)) {
						onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
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
