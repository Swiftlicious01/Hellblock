package com.swiftlicious.hellblock.listeners.fishing;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.effects.FishingEffect;
import com.swiftlicious.hellblock.effects.LavaEffectTask;
import com.swiftlicious.hellblock.events.fishing.FishHookLandEvent;
import com.swiftlicious.hellblock.events.fishing.LavaFishingEvent;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.scheduler.CancellableTask;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;

import net.kyori.adventure.sound.Sound;

/**
 * A task responsible for checking the state of a fishing hook and handling lava
 * fishing mechanics.
 */
public class HookCheckTimerTask implements Runnable {

	private final FishingManager manager;
	private final CancellableTask hookMovementTask;
	private LavaEffectTask lavaFishingTask;
	private final FishHook fishHook;
	private final FishingPreparation fishingPreparation;
	private final FishingEffect initialEffect;
	private Effect tempEffect;
	private final int lureLevel;
	private boolean firstTime;
	private boolean fishHooked;
	private boolean reserve;
	private int jumpTimer;
	private Entity hookedEntity;
	private Loot loot;
	@SuppressWarnings("unused")
	private boolean inLava;

	/**
	 * Constructs a new HookCheckTimerTask.
	 *
	 * @param manager            The FishingManager instance.
	 * @param fishHook           The FishHook entity being checked.
	 * @param fishingPreparation The FishingPreparation instance.
	 * @param initialEffect      The initial fishing effect.
	 */
	public HookCheckTimerTask(FishingManager manager, FishHook fishHook, FishingPreparation fishingPreparation,
			FishingEffect initialEffect) {
		this.manager = manager;
		this.fishHook = fishHook;
		this.initialEffect = initialEffect;
		this.fishingPreparation = fishingPreparation;
		this.hookMovementTask = HellblockPlugin.getInstance().getScheduler().runTaskSyncTimer(this,
				fishHook.getLocation(), 1, 1);
		this.lureLevel = fishingPreparation.getRodItemStack().getEnchantmentLevel(Enchantment.LURE);
		this.firstTime = true;
		this.tempEffect = new FishingEffect();
		this.inLava = false;
	}

	@Override
	public void run() {
		if (!this.fishHook.isValid()
		// || (fishHook.getHookedEntity() != null &&
		// fishHook.getHookedEntity().getType() != EntityType.ARMOR_STAND)
		) {
			// This task would be cancelled when hook is removed
			this.destroy();
			return;
		}
		if (this.fishHook.isOnGround()) {
			this.inLava = false;
			return;
		}
		if (this.fishHook.getLocation().getBlock().getType() == Material.LAVA) {
			this.inLava = true;
			// if player can fish in lava
			if (firstTime) {
				this.firstTime = false;

				this.fishingPreparation.setLocation(this.fishHook.getLocation());
				this.fishingPreparation.mergeEffect(this.initialEffect);
				if (!initialEffect.canLavaFishing()) {
					this.destroy();
					return;
				}

				FishHookLandEvent event = new FishHookLandEvent(this.fishingPreparation.getPlayer(),
						FishHookLandEvent.Target.LAVA, this.fishHook, true, this.initialEffect);
				Bukkit.getPluginManager().callEvent(event);

				this.fishingPreparation.insertArg("{lava}", "true");
				this.fishingPreparation.triggerActions(ActionTrigger.LAND);
			}

			// simulate fishing mechanic
			if (this.fishHooked) {
				this.jumpTimer++;
				if (this.jumpTimer < 4)
					return;
				this.jumpTimer = 0;
				this.fishHook.setVelocity(new Vector(0, 0.24, 0));
				return;
			}

			if (!this.reserve) {
				// jump
				if (this.jumpTimer < 5) {
					this.jumpTimer++;
					this.fishHook.setVelocity(new Vector(0, 0.2 - this.jumpTimer * 0.02, 0));
					return;
				}

				this.reserve = true;

				this.setNextLoot();
				if (this.loot != null) {
					this.tempEffect = this.loot.getBaseEffect().build(fishingPreparation.getPlayer(),
							fishingPreparation.getArgs());
					this.tempEffect.merge(this.initialEffect);
					this.setTempState();
					this.startLavaFishingMechanic();
				} else {
					this.tempEffect = new FishingEffect();
					this.tempEffect.merge(this.initialEffect);
					this.manager.removeTempFishingState(fishingPreparation.getPlayer());
					HellblockPlugin.getInstance().debug("No loot available for " + fishingPreparation.getPlayer().getName()
							+ " at " + fishingPreparation.getLocation());
				}

				this.makeHookStatic(this.fishHook.getLocation());
			}
			return;
		}
	}

	/**
	 * Destroys the task and associated entities.
	 */
	public void destroy() {
		this.cancelSubTask();
		this.removeTempEntity();
		this.hookMovementTask.cancel();
		this.manager.removeHookCheckTask(fishingPreparation.getPlayer());
	}

	/**
	 * Cancels the lava fishing subtask if it's active.
	 */
	public void cancelSubTask() {
		if (lavaFishingTask != null && !lavaFishingTask.isCancelled()) {
			lavaFishingTask.cancel();
			lavaFishingTask = null;
		}
	}

	private void setNextLoot() {
		Loot nextLoot = HellblockPlugin.getInstance().getLootManager().getNextLoot(initialEffect, fishingPreparation);
		if (nextLoot == null) {
			this.loot = null;
			return;
		}
		this.loot = nextLoot;
	}

	/**
	 * Sets temporary state and prepares for the next loot.
	 */
	private void setTempState() {
		fishingPreparation.insertArg("{nick}", loot.getNick());
		fishingPreparation.insertArg("{loot}", loot.getID());
		manager.setTempFishingState(fishingPreparation.getPlayer(),
				new TempFishingState(tempEffect, fishingPreparation, loot));
	}

	/**
	 * Removes the temporary hooked entity.
	 */
	public void removeTempEntity() {
		if (hookedEntity != null && !hookedEntity.isDead())
			hookedEntity.remove();
	}

	/**
	 * Starts the lava fishing mechanic.
	 */
	private void startLavaFishingMechanic() {
		// get random time
		int random;
		if (HBConfig.overrideVanilla) {
			random = ThreadLocalRandom.current().nextInt(HBConfig.lavaMinTime, HBConfig.lavaMaxTime);
			random *= tempEffect.getWaitTimeMultiplier();
			random += tempEffect.getWaitTime();
			random = Math.max(1, random);
		} else {
			random = ThreadLocalRandom.current().nextInt(HBConfig.lavaMinTime, HBConfig.lavaMaxTime);
			random -= lureLevel * 100;
			random = Math.max(HBConfig.lavaMinTime, random);
			random *= tempEffect.getWaitTimeMultiplier();
			random += tempEffect.getWaitTime();
			random = Math.max(1, random);
		}

		// lava effect task (Three seconds in advance)
		this.lavaFishingTask = new LavaEffectTask(this, fishHook.getLocation(), random - 3 * 20);
	}

	/**
	 * Handles the hook state of the fish hook.
	 */
	public void getHooked() {
		LavaFishingEvent lavaFishingEvent = new LavaFishingEvent(fishingPreparation.getPlayer(),
				LavaFishingEvent.State.BITE, fishHook);
		Bukkit.getPluginManager().callEvent(lavaFishingEvent);
		if (lavaFishingEvent.isCancelled()) {
			this.startLavaFishingMechanic();
			return;
		}

		this.loot.triggerActions(ActionTrigger.BITE, fishingPreparation);
		this.fishingPreparation.triggerActions(ActionTrigger.BITE);

		this.fishHooked = true;
		this.removeTempEntity();

		HellblockPlugin.getInstance().getAdventureManager().sendSound(fishingPreparation.getPlayer(),
				Sound.Source.NEUTRAL,
				net.kyori.adventure.key.Key.key("minecraft:block.pointed_dripstone.drip_lava_into_cauldron"), 1, 1);

		HellblockPlugin.getInstance().getScheduler().runTaskAsyncLater(() -> {
			fishHooked = false;
			reserve = false;
		}, (2 * 20) * 50L, TimeUnit.MILLISECONDS);
	}

	private void makeHookStatic(Location armorLoc) {
		armorLoc.setY(armorLoc.getBlockY() + 0.2);
		if (hookedEntity != null && !hookedEntity.isDead())
			hookedEntity.remove();
		hookedEntity = armorLoc.getWorld().spawn(armorLoc, ArmorStand.class, a -> {
			a.setInvisible(true);
			a.setCollidable(false);
			a.setInvulnerable(true);
			a.setVisible(false);
			a.setCustomNameVisible(false);
			a.setSmall(true);
			a.setGravity(false);
			a.getPersistentDataContainer().set(
					Objects.requireNonNull(NamespacedKey.fromString("lavafishing", HellblockPlugin.getInstance())),
					PersistentDataType.STRING, "temp");
		});
		fishHook.setHookedEntity(hookedEntity);
	}

	/**
	 * Checks if the fish hook is currently hooked.
	 *
	 * @return True if the fish hook is hooked, false otherwise.
	 */
	public boolean isFishHooked() {
		return fishHooked;
	}
}