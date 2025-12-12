package com.swiftlicious.hellblock.nms;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.nms.border.BorderColor;
import com.swiftlicious.hellblock.nms.bossbar.BossBarColor;
import com.swiftlicious.hellblock.nms.bossbar.BossBarOverlay;
import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.nms.entity.display.FakeItemDisplay;
import com.swiftlicious.hellblock.nms.entity.display.FakeTextDisplay;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.nms.fluid.FluidData;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;

public interface NMSHandler {

	/**
	 * Sends a JSON message to the player.
	 * 
	 * @param player      the player to send the message to.
	 * @param messageJson the JSON message to send.
	 */
	abstract void sendMessage(@NotNull Player player, @NotNull String messageJson);

	/**
	 * Sends a JSON action bar message to the player.
	 * 
	 * @param player        the player to send the action bar to.
	 * @param actionBarJson the JSON action bar message to send.
	 */
	abstract void sendActionBar(@NotNull Player player, @NotNull String actionBarJson);

	/**
	 * Sends a JSON title and subtitle to the player.
	 * 
	 * @param player       the player to send the title to.
	 * @param titleJson    the JSON title to send.
	 * @param subTitleJson the JSON subtitle to send.
	 * @param fadeInTicks  the amount of ticks it takes for the title to fade in.
	 * @param stayTicks    the amount of ticks the title stays on screen.
	 * @param fadeOutTicks the amount of ticks it takes for the title to fade out.
	 */
	abstract void sendTitle(@NotNull Player player, @NotNull String titleJson, @NotNull String subTitleJson,
			int fadeInTicks, int stayTicks, int fadeOutTicks);

	/**
	 * Creates and displays a boss bar to the specified player.
	 *
	 * @param player         The player to whom the boss bar should be shown.
	 * @param uuid           A unique identifier for the boss bar instance.
	 * @param component      The name or title of the boss bar (represented as a
	 *                       component object).
	 * @param color          The color of the boss bar (e.g., PINK, BLUE, RED).
	 * @param overlay        The overlay style of the boss bar (e.g., PROGRESS,
	 *                       NOTCHED_10).
	 * @param progress       The initial progress of the boss bar (from 0.0 to 1.0).
	 * @param createWorldFog Whether to create fog in the world while the boss bar
	 *                       is displayed.
	 * @param playBossMusic  Whether to play boss music when the boss bar is
	 *                       visible.
	 * @param darkenScreen   Whether to darken the screen background when the boss
	 *                       bar appears.
	 */
	abstract void createBossBar(@NotNull Player player, @NotNull UUID uuid, @NotNull Object component,
			@NotNull BossBarColor color, @NotNull BossBarOverlay overlay, float progress, boolean createWorldFog,
			boolean playBossMusic, boolean darkenScreen);

	/**
	 * Removes an existing boss bar from the specified player.
	 *
	 * @param player The player from whom the boss bar should be removed.
	 * @param uuid   The unique identifier of the boss bar to be removed.
	 */
	abstract void removeBossBar(@NotNull Player player, @NotNull UUID uuid);

	/**
	 * Updates the title (name) of an existing boss bar for the specified player.
	 *
	 * @param player    The player for whom the boss bar title should be updated.
	 * @param uuid      The unique identifier of the boss bar to update.
	 * @param component The new title or name of the boss bar (as a component
	 *                  object).
	 */
	abstract void updateBossBarName(@NotNull Player player, @NotNull UUID uuid, @NotNull Object component);

	/**
	 * Updates the progress value of an existing boss bar for the specified player.
	 *
	 * @param player   The player for whom the boss bar progress should be updated.
	 * @param uuid     The unique identifier of the boss bar to update.
	 * @param progress The new progress value (from 0.0 to 1.0) to set for the boss
	 *                 bar.
	 */
	abstract void updateBossBarProgress(@NotNull Player player, @NotNull UUID uuid, float progress);

	/**
	 * Sends a toast (tutorial / advancement tab in the top right corner of the
	 * screen).
	 * 
	 * @param player          the player to show the toast to.
	 * @param icon            the item of the toast.
	 * @param titleJson       the title of the toast.
	 * @param advancementType the advancement type of the toast.
	 */
	abstract void sendToast(Player player, @NotNull ItemStack icon, @NotNull String titleJson,
			@NotNull String advancementType);

	/**
	 * Plays an open or close animation for a container block (chest, barrel, etc.)
	 * at the given location, visible only to the specified player.
	 *
	 * This method sends a block event packet and a matching open/close sound effect
	 * that mimics the container being opened or closed, without actually affecting
	 * its state.
	 * <p>
	 * Supported block types:
	 * <li>Single or double chests</li>
	 * <li>Trapped chests</li>
	 * <li>Ender chests</li>
	 * <li>Barrels</li>
	 * <li>Shulker boxes (all colors)</li>
	 * <p>
	 * 
	 * @param location the location of the container block
	 * @param open     true to play the open animation, false for close
	 * @param player   the target player to show the animation and sound to
	 */
	abstract void playChestAnimation(@NotNull Player player, @NotNull Location location, boolean open);

	/**
	 * Creates a fake Nether Fortress structure region within the given world and
	 * bounding box. No blocks are modified — this only affects internal structure
	 * data, which influences mob spawning and structure lookups.
	 *
	 * @param world  Bukkit world (must be a server world)
	 * @param bounds Bukkit BoundingBox defining the fortress area
	 */
	abstract void injectFakeFortress(@NotNull World world, @NotNull BoundingBox bounds);

	/**
	 * Removes any previously injected fake Nether Fortress data within the
	 * specified bounding box.
	 *
	 * @param world  Bukkit world (must be a server world)
	 * @param bounds BoundingBox region to clear
	 */
	abstract void removeFakeFortress(@NotNull World world, @NotNull BoundingBox bounds);

	/**
	 * Sets the world border for the defined player.
	 * 
	 * @param player      the player to set the world border for.
	 * @param bounds      the center location of the world border.
	 * @param borderColor the color of the border.
	 */
	abstract void sendWorldBorder(@NotNull Player player, @NotNull BoundingBox bounds,
			@NotNull BorderColor borderColor);

	/**
	 * Updates the world border when expanding.
	 * 
	 * @param player      the player to update the world border for.
	 * @param center      the center of the border.
	 * @param startSize   the starting size of the border.
	 * @param endSize     the end size the border should be.
	 * @param durationMs  the duration it will last.
	 * @param borderColor the color of the border.
	 */
	abstract void updateWorldBorder(Player player, @NotNull Location center, double startSize, double endSize,
			long durationMs, @NotNull BorderColor borderColor);

	/**
	 * Clears the world border by making it huge.
	 * 
	 * @param player the player to clear the world border for.
	 */
	abstract void clearWorldBorder(@NotNull Player player);

	/**
	 * Retrieve the fluid data from the defined location.
	 * 
	 * @param location the location to retrieve the fluid data from.
	 * @return the fluid data reference.
	 */
	@NotNull
	abstract FluidData getFluidData(@NotNull Location location);

	/**
	 * Retrieves the biome from the defined location.
	 * 
	 * @param location the location to retrieve the biome from.
	 * @return the biome that resides in the given location.
	 */
	@NotNull
	abstract String getBiomeResourceLocation(@NotNull Location location);

	/**
	 * Converts a JSON string into a Minecraft chat component.
	 * 
	 * @param json the JSON string to convert.
	 * @return the Minecraft chat component.
	 */
	@NotNull
	abstract Object getMinecraftComponent(@NotNull String json);

	/**
	 * Restricts the AI behavior of a Wither entity by clearing all default goals
	 * and targets, and setting it to only target players.
	 * 
	 * It ensures the Wither will no longer attack other entities or wander, but
	 * will still attempt to attack nearby players.
	 *
	 * @param bukkitWither the Bukkit Wither entity to restrict
	 */
	abstract void restrictWitherAI(@NotNull Wither bukkitWither);

	/**
	 * Ignites the ExplosiveMinecart. This is mainly needed for versions below
	 * 1.19.4 because there is no given ignite() method for the ExplosiveMinecart
	 * class. Default set to use the given method if available and not overriden.
	 * 
	 * @param bukkitExplosiveMinecart the Bukkit ExplosiveMinecart entity to ignite
	 */
	default void igniteTNTMinecart(@NotNull ExplosiveMinecart bukkitExplosiveMinecart) {
		bukkitExplosiveMinecart.ignite();
	}

	/**
	 * Opens an custom inventory using packets.
	 * 
	 * @param player    the player to open an inventory for.
	 * @param inventory the inventory to open.
	 * @param jsonTitle the title of the inventory.
	 */
	abstract void openCustomInventory(@NotNull Player player, @NotNull Inventory inventory, @NotNull String jsonTitle);

	/**
	 * Updates the inventory's title.
	 * 
	 * @param player    the player to update the title for.
	 * @param jsonTitle the title to change it to.
	 */
	abstract void updateInventoryTitle(@NotNull Player player, @NotNull String jsonTitle);

	/**
	 * Retrieves the fishing loot for the defined hook and rod used.
	 * 
	 * @param player the player fishing.
	 * @param hook   the hook that caught the loot.
	 * @param rod    the fishing rod used.
	 * @return the loot that was captured.
	 */
	@NotNull
	abstract List<ItemStack> getFishingLoot(@NotNull Player player, @NotNull FishHook hook, @NotNull ItemStack rod);

	/**
	 * Checks whether or not a fish hook is bit.
	 * 
	 * @param hook the hook to check if it is bit.
	 * @return whether or not the hook is bit.
	 */
	abstract boolean isFishingHookBit(@NotNull FishHook hook);

	/**
	 * Gets the UUID of the owner of the given fish hook.
	 * 
	 * @param hook the hook to check for the owner of.
	 * @return the uuid of the fish hook's holder.
	 */
	@Nullable
	abstract UUID getFishingHookOwner(@NotNull FishHook hook);

	/**
	 * Sets the vanilla fishing wait time.
	 * 
	 * @param hook  the hook to set the wait time for.
	 * @param ticks the amount of ticks the hook needs to wait for.
	 */
	default void setWaitTime(@NotNull FishHook hook, int ticks) {
		hook.setWaitTime(ticks);
	}

	/**
	 * Gets the vanilla fishing wait time.
	 * 
	 * @param hook the hook to get the wait time for.
	 * @return the amount of ticks before the wait time is over.
	 */
	default int getWaitTime(@NotNull FishHook hook) {
		return hook.getWaitTime();
	}

	/**
	 * Get the enchantment map for this item.
	 * 
	 * @param item the item to retrieve the enchantment map from.
	 * @return the enchantment map for this item.
	 */
	@NotNull
	default Map<String, Integer> itemEnchantmentsToMap(@NotNull Object item) {
		return Map.of();
	}

	/**
	 * Swing a player's hand using packets.
	 * 
	 * @param player the player to swing their hand for.
	 * @param slot   the offhand or main hand to swing.
	 */
	abstract void swingHand(@NotNull Player player, @NotNull HandSlot slot);

	/**
	 * Perform a use item action for the given itemstack.
	 * 
	 * @param player    the player to perform the action for.
	 * @param handSlot  the hand slot the item is in.
	 * @param itemStack the item to perform the use action on.
	 */
	abstract void useItem(@NotNull Player player, @NotNull HandSlot handSlot, @Nullable ItemStack itemStack);

	/**
	 * Removes the entity using packets.
	 * 
	 * @param player    the player to show packet for.
	 * @param entityIDs an array of entities to remove.
	 */
	abstract void removeClientSideEntity(@NotNull Player player, int... entityIDs);

	/**
	 * Teleports an entity using packets.
	 * 
	 * @param player    the player to show the packet to.
	 * @param location  the location the entity will teleport to.
	 * @param motion    the vector motion of the entity.
	 * @param onGround  whether or not the entity is on the ground.
	 * @param entityIDs an array of entities to teleport.
	 */
	public abstract void sendClientSideTeleportEntity(@NotNull Player player, @NotNull Location location,
			@NotNull Vector motion, boolean onGround, int... entityIDs);

	default void sendClientSideTeleportEntity(@NotNull Player player, @NotNull Location location, boolean onGround,
			int... entityIDs) {
		this.sendClientSideTeleportEntity(player, location, new Vector(0, 0, 0), onGround, entityIDs);
	}

	/**
	 * Moves an entity using packets.
	 * 
	 * @param player    the player to show the packet to.
	 * @param vector    the vector amount to move the entity.
	 * @param entityIDs an array of entities to move.
	 */
	abstract void sendClientSideEntityMotion(@NotNull Player player, @NotNull Vector vector, int... entityIDs);

	/**
	 * Drops a fake packet item at the provided location.
	 * 
	 * @param player    the player to see the fake item.
	 * @param itemStack the itemstack to drop.
	 * @param location  the location to place it at.
	 * @return the id of the itemstack.
	 */
	abstract int dropFakeItem(Player player, @NotNull ItemStack itemStack, @NotNull Location location);

	/**
	 * Creates a fake armor stand.
	 * 
	 * @param location the location to create the fake armor stand.
	 * @return the fake armor stand instance.
	 */
	@NotNull
	abstract FakeArmorStand createFakeArmorStand(@NotNull Location location);

	/**
	 * Creates a fake item display.
	 * 
	 * @param location the location to create the fake item display.
	 * @return the fake item display instance.
	 */
	@NotNull
	abstract FakeItemDisplay createFakeItemDisplay(@NotNull Location location);

	/**
	 * Creates a fake text display.
	 * 
	 * @param location the location to create the fake text display.
	 * @return the fake text display instance.
	 */
	@NotNull
	abstract FakeTextDisplay createFakeTextDisplay(@NotNull Location location);

	/**
	 * Create a fake firework.
	 * 
	 * @param location the location to create the fake firework.
	 * @param color    the color of the firework effect.
	 * @return the fake firework instance.
	 */
	@NotNull
	abstract FakeFirework createFakeFirework(@NotNull Location location, @NotNull Color color);
}