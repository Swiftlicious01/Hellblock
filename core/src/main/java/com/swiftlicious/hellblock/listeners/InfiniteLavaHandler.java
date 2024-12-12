package com.swiftlicious.hellblock.listeners;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Fluid;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.fluid.FallingFluidData;
import com.swiftlicious.hellblock.nms.fluid.FluidData;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.PlayerUtils;

import net.kyori.adventure.sound.Sound;

public class InfiniteLavaHandler implements Listener {

	protected final HellblockPlugin instance;

	public InfiniteLavaHandler(HellblockPlugin plugin) {
		instance = plugin;
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onInfiniteLavaFall(PlayerInteractEvent event) {
		if (!instance.getConfigManager().infiniteLavaEnabled()) {
			return;
		}
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		ItemStack hand = event.getItem();
		if (hand != null && hand.getType() == Material.BUCKET) {
			BlockIterator iter = new BlockIterator(player, 5);
			Block lastBlock = iter.next();
			while (iter.hasNext()) {
				lastBlock = iter.next();
				if (lastBlock.getType() != Material.LAVA) {
					continue;
				}
				break;
			}
			if (lastBlock.getType() == Material.LAVA && isLavaFall(lastBlock)) {
				event.setUseItemInHand(Result.ALLOW);
				if (player.getGameMode() != GameMode.CREATIVE)
					event.getItem().setAmount(event.getItem().getAmount() > 0 ? event.getItem().getAmount() - 1 : 0);
				if (player.getInventory().firstEmpty() != -1) {
					PlayerUtils.giveItem(player, new ItemStack(Material.LAVA_BUCKET), 1);
				} else {
					PlayerUtils.dropItem(player, new ItemStack(Material.LAVA_BUCKET), false, true, false);
				}
				VersionHelper.getNMSManager().swingHand(player,
						event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
				instance.getSenderFactory().getAudience(player)
						.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:item.bucket.fill_lava"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				player.updateInventory();
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
				if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null
						|| !onlineUser.get().getHellblockData().hasHellblock())
					return;
				if (!onlineUser.get().getChallengeData()
						.isChallengeActive(instance.getChallengeManager().getByActionType(ActionType.INTERACT))
						&& !onlineUser.get().getChallengeData().isChallengeCompleted(
								instance.getChallengeManager().getByActionType(ActionType.INTERACT))) {
					onlineUser.get().getChallengeData().beginChallengeProgression(onlineUser.get().getPlayer(),
							instance.getChallengeManager().getByActionType(ActionType.INTERACT));
				} else {
					onlineUser.get().getChallengeData().updateChallengeProgression(onlineUser.get().getPlayer(),
							instance.getChallengeManager().getByActionType(ActionType.INTERACT), 1);
					if (onlineUser.get().getChallengeData().isChallengeCompleted(
							instance.getChallengeManager().getByActionType(ActionType.INTERACT))) {
						onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
								instance.getChallengeManager().getByActionType(ActionType.INTERACT));
					}
				}
			}
		}
	}

	private boolean isLavaFall(@NotNull Block block) {
		FluidData lava = VersionHelper.getNMSManager().getFluidData(block.getLocation());
		boolean isLava = lava.getFluidType() == Fluid.LAVA || lava.getFluidType() == Fluid.FLOWING_LAVA;
		boolean isLavaFall = isLava && (lava instanceof FallingFluidData falling) && falling.isFalling();
		return isLavaFall;
	}
}