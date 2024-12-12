package com.swiftlicious.hellblock.listeners;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.RandomUtils;

public class PiglinBarterHandler implements Listener {

	protected final HellblockPlugin instance;

	private final Map<UUID, Set<UUID>> barterTracker;

	public PiglinBarterHandler(HellblockPlugin plugin) {
		instance = plugin;
		this.barterTracker = new LinkedHashMap<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onGiveItemOfInterest(PlayerInteractEntityEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;
		final UUID id = player.getUniqueId();
		ItemStack inHand = player.getInventory().getItem(event.getHand());
		if (inHand.getType() == Material.GOLD_INGOT) {
			if (event.getRightClicked() instanceof Piglin piglin) {
				if (!piglin.isAdult())
					return;
				if (piglin.getEquipment().getItemInOffHand().getType() == Material.AIR) {
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
			if (!instance.getHellblockHandler().isInCorrectWorld(piglin.getWorld()))
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
		if (!instance.getHellblockHandler().isInCorrectWorld(piglin.getWorld()))
			return;

		List<ItemStack> barteredItems = event.getOutcome();
		if (instance.getConfigManager().clearDefaultOutcome()
				&& !instance.getConfigManager().barteringItems().isEmpty())
			barteredItems.clear();

		UUID playerUUID = null;
		for (Map.Entry<UUID, Set<UUID>> entry : barterTracker.entrySet()) {
			if (entry.getValue().contains(piglin.getUniqueId())) {
				playerUUID = entry.getKey();
				break;
			}
		}

		if (playerUUID != null) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null)
				return;
			Context<Player> context = Context.player(onlineUser.get().getPlayer());
			if (!instance.getConfigManager().barteringItems().isEmpty()) {
				instance.getConfigManager().barteringItems().forEach((entry) -> {
					Item<ItemStack> customItem = instance.getItemManager().wrap(entry.build(context));
					ItemStack item = customItem.load();
					item.setAmount(RandomUtils.generateRandomInt(1, (int) entry.amount().evaluate(context)));
					barteredItems.add(item);
				});
			}

			if (barteredItems.stream().filter((item) -> {
				Set<ItemStack> local = new HashSet<>();
				instance.getConfigManager().barteringItems().forEach(customItem -> {
					local.add(customItem.build(context));
				});
				return local.contains(item);
			}).findAny().isPresent()) {
				if (!onlineUser.get().getHellblockData().hasHellblock())
					return;
				if (!onlineUser.get().getChallengeData()
						.isChallengeActive(instance.getChallengeManager().getByActionType(ActionType.BARTER))
						&& !onlineUser.get().getChallengeData().isChallengeCompleted(
								instance.getChallengeManager().getByActionType(ActionType.BARTER))) {
					onlineUser.get().getChallengeData().beginChallengeProgression(onlineUser.get().getPlayer(),
							instance.getChallengeManager().getByActionType(ActionType.BARTER));
				} else {
					onlineUser.get().getChallengeData().updateChallengeProgression(onlineUser.get().getPlayer(),
							instance.getChallengeManager().getByActionType(ActionType.BARTER), 1);
					if (onlineUser.get().getChallengeData()
							.isChallengeCompleted(instance.getChallengeManager().getByActionType(ActionType.BARTER))) {
						onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
								instance.getChallengeManager().getByActionType(ActionType.BARTER));
					}
				}
				barterTracker.get(playerUUID).remove(piglin.getUniqueId());
				if (barterTracker.get(playerUUID).isEmpty())
					barterTracker.remove(playerUUID);
			}
		}
	}
}