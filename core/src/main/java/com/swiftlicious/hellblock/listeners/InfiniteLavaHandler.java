package com.swiftlicious.hellblock.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Fluid;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.fluid.FallingFluidData;
import com.swiftlicious.hellblock.nms.fluid.FluidData;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.utils.PlayerUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

public class InfiniteLavaHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	public InfiniteLavaHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler
	public void onInfiniteLavaFall(PlayerInteractEvent event) {
		if (!instance.getConfigManager().infiniteLavaEnabled()) {
			return;
		}

		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final ItemStack hand = event.getItem();
		if (hand == null || hand.getType() != Material.BUCKET) {
			return;
		}

		final Block lavaBlock = findLavaInRange(player, 5);
		if (lavaBlock == null || !isLavaFall(lavaBlock)) {
			return;
		}

		// Consume bucket and give lava
		event.setUseItemInHand(Result.ALLOW);
		if (player.getGameMode() != GameMode.CREATIVE) {
			hand.setAmount(0); // bucket items normally don’t stack
		}

		final ItemStack lavaBucket = new ItemStack(Material.LAVA_BUCKET);
		if (player.getInventory().firstEmpty() != -1) {
			PlayerUtils.giveItem(player, lavaBucket, 1);
		} else {
			PlayerUtils.dropItem(player, lavaBucket, false, true, false);
		}

		// Swing + sound
		VersionHelper.getNMSManager().swingHand(player,
				event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
		AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
				Sound.sound(Key.key("minecraft:item.bucket.fill_lava"), Source.PLAYER, 1, 1));
	}

	private Block findLavaInRange(Player player, int range) {
		final BlockIterator iter = new BlockIterator(player, range);
		while (iter.hasNext()) {
			final Block block = iter.next();
			if (block.getType() == Material.LAVA) {
				return block;
			}
		}
		return null;
	}

	private boolean isLavaFall(@NotNull Block block) {
		final FluidData lava = VersionHelper.getNMSManager().getFluidData(block.getLocation());
		final boolean isLava = lava.getFluidType() == Fluid.LAVA || lava.getFluidType() == Fluid.FLOWING_LAVA;
		return isLava && (lava instanceof FallingFluidData falling) && falling.isFalling();
	}
}