package com.swiftlicious.hellblock.listeners.fishing;

import java.util.concurrent.TimeUnit;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;

/**
 * A task responsible for animating bait when it's attached to a fishing hook.
 */
public class BaitAnimationTask implements Runnable {

	private final SchedulerTask task;
	private final int entityID;
	private final Player player;
	private final FishHook fishHook;

	/**
	 * Constructs a new BaitAnimationTask.
	 *
	 * @param plugin   The Plugin instance.
	 * @param player   The player who cast the fishing rod.
	 * @param fishHook The FishHook entity.
	 * @param baitItem The bait ItemStack.
	 */
	public BaitAnimationTask(HellblockPlugin plugin, Player player, FishHook fishHook, ItemStack baitItem) {
		this.player = player;
		this.fishHook = fishHook;
		this.task = plugin.getScheduler().asyncRepeating(this, 50, 50, TimeUnit.MILLISECONDS);
		ItemStack itemStack = baitItem.clone();
		itemStack.setAmount(1);
		this.entityID = plugin.getVersionManager().getNMSManager().dropFakeItem(player, itemStack,
				fishHook.getLocation().clone().subtract(0, 0.6, 0));
	}

	@Override
	public void run() {
		HellblockPlugin.getInstance().getVersionManager().getNMSManager().sendClientSideEntityMotion(player,
				fishHook.getVelocity(), entityID);
		HellblockPlugin.getInstance().getVersionManager().getNMSManager().sendClientSideTeleportEntity(player,
				fishHook.getLocation().clone().subtract(0, 0.6, 0), fishHook.getVelocity(), false, entityID);
	}

	/**
	 * Cancels the bait animation and cleans up resources.
	 */
	public void cancelAnimation() {
		task.cancel();
		HellblockPlugin.getInstance().getVersionManager().getNMSManager().removeClientSideEntity(player, entityID);
	}
}