package com.swiftlicious.hellblock.listeners.fishing;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.FakeItemUtils;

/**
 * A task responsible for animating bait when it's attached to a fishing hook.
 */
public class BaitAnimationTask implements Runnable {

	private final SchedulerTask cancellableTask;
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
		entityID = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
		plugin.sendPackets(player, FakeItemUtils.getSpawnPacket(entityID, fishHook.getLocation()),
				FakeItemUtils.getMetaPacket(entityID, baitItem));
		this.cancellableTask = plugin.getScheduler().asyncRepeating(this, 50, 50, TimeUnit.MILLISECONDS);
	}

	@Override
	public void run() {
		HellblockPlugin.getInstance().getProtocolManager().sendServerPacket(player,
				FakeItemUtils.getVelocityPacket(entityID, fishHook.getVelocity()));
		HellblockPlugin.getInstance().getProtocolManager().sendServerPacket(player,
				FakeItemUtils.getTpPacket(entityID, fishHook.getLocation(), false));
	}

	/**
	 * Cancels the bait animation and cleans up resources.
	 */
	public void cancelAnimation() {
		cancellableTask.cancel();
		HellblockPlugin.getInstance().getProtocolManager().sendServerPacket(player,
				FakeItemUtils.getDestroyPacket(entityID));
	}
}
