package com.swiftlicious.hellblock.listeners;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Fluid;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.player.UserData;

import io.papermc.paper.block.fluid.FluidData;
import io.papermc.paper.block.fluid.type.FallingFluidData;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class InfiniteLava implements Listener {

	protected final HellblockPlugin instance;

	public InfiniteLava(HellblockPlugin plugin) {
		instance = plugin;
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onInfiniteLavaFall(PlayerInteractEvent event) {
		if (!HBConfig.infiniteLavaEnabled) {
			return;
		}
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
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
				event.getItem().setAmount(event.getItem().getAmount() > 0 ? event.getItem().getAmount() - 1 : 0);
				if (player.getInventory().firstEmpty() != -1) {
					player.getInventory().addItem(new ItemStack(Material.LAVA_BUCKET));
				} else {
					lastBlock.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.LAVA_BUCKET));
				}
				player.swingMainHand();
				instance.getAdventureManager().sendSound(player, net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:item.bucket.fill_lava"), 1, 1);
				player.updateInventory();
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
				if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null)
					return;
				if (!onlineUser.get().getChallengeData().isChallengeActive(ChallengeType.INFINITE_LAVA_CHALLENGE)
						&& !onlineUser.get().getChallengeData()
								.isChallengeCompleted(ChallengeType.INFINITE_LAVA_CHALLENGE)) {
					onlineUser.get().getChallengeData().beginChallengeProgression(onlineUser.get().getPlayer(),
							ChallengeType.INFINITE_LAVA_CHALLENGE);
				} else {
					onlineUser.get().getChallengeData().updateChallengeProgression(onlineUser.get().getPlayer(),
							ChallengeType.INFINITE_LAVA_CHALLENGE, 1);
					if (onlineUser.get().getChallengeData()
							.isChallengeCompleted(ChallengeType.INFINITE_LAVA_CHALLENGE)) {
						onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
								ChallengeType.INFINITE_LAVA_CHALLENGE);
					}
				}
			}
		}
	}

	private boolean isLavaFall(@NonNull Block block) {
		FluidData lava = block.getWorld().getFluidData(block.getLocation());
		boolean isLava = lava.getFluidType() == Fluid.LAVA || lava.getFluidType() == Fluid.FLOWING_LAVA;
		boolean isLavaFall = isLava && (lava instanceof FallingFluidData falling) && falling.isFalling();
		return isLavaFall;
	}
}