package com.swiftlicious.hellblock.player;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellblockBorderTask;
import com.swiftlicious.hellblock.listeners.NetherAnimalSpawningTask;

/**
 * Implementation of the OnlineUser interface, extending OfflineUser to
 * represent online player data.
 */
public class OnlineUser extends OfflineUser implements OnlineUserInterface {

	private final Player player;

	private HellblockBorderTask borderTask;
	private NetherAnimalSpawningTask animalSpawningTask;
	private boolean wearingGlowstoneArmor, holdingGlowstoneTool;

	/**
	 * Constructor to create an OnlineUser instance.
	 *
	 * @param player     The online player associated with this user.
	 * @param playerData The player's data, including earnings.
	 */
	public OnlineUser(Player player, PlayerData playerData) {
		super(player.getUniqueId(), player.getName(), playerData);
		this.player = player;
	}

	@Override
	public Player getPlayer() {
		return this.player;
	}

	public void showBorder() {
		this.borderTask = new HellblockBorderTask(HellblockPlugin.getInstance(), this.player.getUniqueId());
	}

	public void hideBorder() {
		if (this.borderTask != null) {
			this.borderTask.cancelBorderShowcase();
			this.borderTask = null;
		}
	}

	public void startSpawningAnimals() {
		this.animalSpawningTask = new NetherAnimalSpawningTask(HellblockPlugin.getInstance(),
				this.player.getUniqueId());
	}

	public void stopSpawningAnimals() {
		if (this.animalSpawningTask != null) {
			this.animalSpawningTask.stopAnimalSpawning();
			this.animalSpawningTask = null;
		}
	}

	public boolean hasGlowstoneArmorEffect() {
		return this.wearingGlowstoneArmor;
	}

	public boolean hasGlowstoneToolEffect() {
		return this.holdingGlowstoneTool;
	}

	public void isWearingGlowstoneArmor(boolean wearingGlowstoneArmor) {
		this.wearingGlowstoneArmor = wearingGlowstoneArmor;
	}

	public void isHoldingGlowstoneTool(boolean holdingGlowstoneTool) {
		this.holdingGlowstoneTool = holdingGlowstoneTool;
	}
}