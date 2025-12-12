package com.swiftlicious.hellblock.listeners;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;

import net.kyori.adventure.text.Component;

public class HellblockPortalListener implements Listener, Reloadable {

	private final HellblockPlugin instance;

	private final Map<UUID, SchedulerTask> cancellablePortal = new ConcurrentHashMap<>();
	private final Set<UUID> linkPortalCatcher = ConcurrentHashMap.newKeySet();

	public HellblockPortalListener(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		cancellablePortal.keySet().forEach(this::cancelPortalTask);
		cancellablePortal.clear();
		linkPortalCatcher.clear();
	}

	@EventHandler
	public void onUsePortal(PlayerPortalEvent event) {
		final Player player = event.getPlayer();

		if (!(player.getWorld().getEnvironment() == Environment.NETHER
				&& event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)) {
			return;
		}

		event.setCancelled(true);

		final Optional<UserData> onlineUserOpt = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUserOpt.isEmpty()) {
			return;
		}

		final UserData onlineUser = onlineUserOpt.get();
		final HellblockData hellblockData = onlineUser.getHellblockData();

		final SchedulerTask portalTask = instance.getScheduler().sync().runLater(() -> {
			if (!player.isOnline()) {
				return;
			}

			if (!hellblockData.hasHellblock()) {
				instance.getIslandChoiceGUIManager().openIslandChoiceGUI(player, false);
				return;
			}

			final UUID linkedUUID = hellblockData.getLinkedPortalUUID();

			if (instance.getConfigManager().linkHellblocks() && linkedUUID != null) {
				instance.debug("Player has linked island: " + linkedUUID + " for " + player.getName());
				fetchAndTeleportToLinkedIsland(linkedUUID, onlineUser, event.getTo());
			} else {
				if (hellblockData.getOwnerUUID() == null) {
					instance.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName()
							+ " (" + player.getUniqueId() + "). This indicates corrupted data.");
					throw new IllegalStateException(
							"Owner reference was null. This should never happen — please report to the developer.");
				}
				instance.debug("Teleporting to own island with portal: " + player.getName());
				teleportToOwnIsland(onlineUser, hellblockData.getOwnerUUID());
			}

		}, 5 * 20, hellblockData.getHomeLocation());

		this.cancellablePortal.putIfAbsent(player.getUniqueId(), portalTask);
	}

	private void fetchAndTeleportToLinkedIsland(UUID linkedUUID, UserData playerData, Location targetLocation) {
		instance.getStorageManager().getCachedUserDataWithFallback(linkedUUID, instance.getConfigManager().lockData())
				.thenAccept(linkedResult -> {
					if (linkedResult.isEmpty()) {
						return;
					}

					final UserData linkedUser = linkedResult.get();
					final UUID actualOwnerId = linkedUser.getHellblockData().getOwnerUUID();

					if (actualOwnerId == null) {
						return;
					}

					checkPlayerBannedAndTeleport(playerData, actualOwnerId, targetLocation);
				});
	}

	private void checkPlayerBannedAndTeleport(UserData playerData, UUID ownerId, Location targetLocation) {
		instance.getCoopManager().isPlayerBannedInLocation(ownerId, playerData.getUUID(), targetLocation)
				.thenAccept(banned -> {
					if (banned) {
						instance.debug("Player is banned from target island: " + playerData.getName());
						runSync(() -> instance.getSenderFactory().wrap(playerData.getPlayer()).sendMessage(instance
								.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY.build())));
						return;
					}

					teleportToIslandOwner(playerData, ownerId);
				});
	}

	private void teleportToIslandOwner(UserData playerData, UUID ownerId) {
		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(ownerResult -> {
					if (ownerResult.isEmpty()) {
						return;
					}

					final UserData ownerData = ownerResult.get();
					final HellblockData ownerHellblock = ownerData.getHellblockData();

					if (!validateHellblock(playerData, ownerHellblock)) {
						return;
					}

					final Location home = ownerHellblock.getHomeLocation();
					LocationUtils.isSafeLocationAsync(home).thenAccept(safe -> {
						if (!safe) {
							instance.debug("Unsafe teleport location, making safe: " + home);
							instance.getCoopManager().makeHomeLocationSafe(ownerData, playerData);
						}

						runSync(() -> {
							if (playerData.getPlayer() != null && playerData.getPlayer().isOnline()) {
								ChunkUtils.teleportAsync(playerData.getPlayer(), home,
										PlayerTeleportEvent.TeleportCause.PLUGIN);
								instance.debug("Teleported player to island: " + playerData.getName());
							}
						});
					});
				});
	}

	private void teleportToOwnIsland(UserData playerData, @Nullable UUID ownerId) {
		if (ownerId == null) {
			return;
		}

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
				.thenAccept(owner -> {
					if (owner.isPresent()) {
						instance.debug("Making home location safe for: " + playerData.getName());
						instance.getCoopManager().makeHomeLocationSafe(owner.get(), playerData);
					}
				});
	}

	private boolean validateHellblock(UserData playerData, HellblockData hellblockData) {
		if (!hellblockData.hasHellblock() || hellblockData.isAbandoned() || hellblockData.getHomeLocation() == null) {
			runSync(() -> instance.getSenderFactory().wrap(playerData.getPlayer()).sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_UNSAFE_LINKING.build())));
			instance.debug(playerData.getName() + " attempted to teleport to a now-invalid link: "
					+ hellblockData.getLinkedPortalUUID());
			playerData.getHellblockData().setLinkedPortalUUID(null);
			return false;
		}
		return true;
	}

	@EventHandler
	public void onLinkPortalCreate(PortalCreateEvent event) {
		if (!instance.getConfigManager().linkHellblocks())
			return;
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getWorld()))
			return;
		if (event.getWorld().getEnvironment() != Environment.NETHER)
			return;
		if (event.getReason() != CreateReason.FIRE)
			return;

		final Entity entity = event.getEntity();
		if (!(entity instanceof Player player))
			return;

		final UUID id = player.getUniqueId();
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isEmpty())
			return;

		final UserData user = onlineUser.get();
		if (!user.getHellblockData().hasHellblock() || linkPortalCatcher.contains(id))
			return;

		final UUID linked = user.getHellblockData().getLinkedPortalUUID();
		final String owner = linked != null && Bukkit.getOfflinePlayer(linked).getName() != null
				? Bukkit.getOfflinePlayer(linked).getName()
				: null;

		sendLinkTutorial(player, owner);
		linkPortalCatcher.add(id);
	}

	@EventHandler
	public void onLinkPortalInteract(PlayerInteractEvent event) {
		if (!instance.getConfigManager().linkHellblocks())
			return;

		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;
		if (player.getWorld().getEnvironment() != Environment.NETHER)
			return;

		final Block block = event.getClickedBlock();
		if (block == null || block.getType() != Material.NETHER_PORTAL)
			return;
		if (block.hasMetadata("invasion_portal"))
			return;

		final UUID id = player.getUniqueId();
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isEmpty())
			return;

		final UserData user = onlineUser.get();
		if (!user.getHellblockData().hasHellblock() || linkPortalCatcher.contains(id))
			return;

		final UUID linked = user.getHellblockData().getLinkedPortalUUID();
		final String owner = linked != null && Bukkit.getOfflinePlayer(linked).getName() != null
				? Bukkit.getOfflinePlayer(linked).getName()
				: null;

		sendLinkTutorial(player, owner);
		linkPortalCatcher.add(id);
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onLinkPortalChat(AsyncPlayerChatEvent event) {
		if (!instance.getConfigManager().linkHellblocks())
			return;

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		if (!linkPortalCatcher.contains(id))
			return;

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isEmpty())
			return;

		event.setCancelled(true);
		final String username = event.getMessage();
		final Sender audience = instance.getSenderFactory().wrap(player);

		if ("none".equalsIgnoreCase(username) || username.equalsIgnoreCase(player.getName())) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_OWN.build()));
			linkPortalCatcher.remove(id);
			return;
		}

		final Player linkedOnline = Bukkit.getPlayer(username);
		if (linkedOnline != null) {
			final Optional<UserData> linked = instance.getStorageManager().getOnlineUser(linkedOnline.getUniqueId());
			if (linked.isEmpty()) {
				linkPortalCatcher.remove(id);
				return;
			}

			final UserData linkedUser = linked.get();
			final HellblockData linkedData = linkedUser.getHellblockData();

			final UUID ownerId = linkedData.getOwnerUUID();
			if (ownerId == null) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_NO_ISLAND.build()));
				linkPortalCatcher.remove(id);
				return;
			}

			final Optional<UserData> linkedOwnerOpt = instance.getStorageManager().getCachedUserData(ownerId);
			if (linkedOwnerOpt.isEmpty()) {
				linkPortalCatcher.remove(id);
				return;
			}

			final UserData linkedOwner = linkedOwnerOpt.get();
			final HellblockData linkedOwnerData = linkedOwner.getHellblockData();

			if (!linkedOwnerData.hasHellblock() || linkedOwnerData.isAbandoned()
					|| linkedOwnerData.getHomeLocation() == null) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_NO_ISLAND.build()));
				linkPortalCatcher.remove(id);
				return;
			}

			if (linkedOwnerData.getPartyMembers().contains(id)) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_OWN.build()));
				linkPortalCatcher.remove(id);
				return;
			}

			if (linkedOwnerData.isLocked()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_LOCKED.build()));
				linkPortalCatcher.remove(id);
				return;
			}

			if (linkedOwnerData.getBannedMembers().contains(id)) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_BANNED.build()));
				linkPortalCatcher.remove(id);
				return;
			}

			onlineUser.get().getHellblockData().setLinkedPortalUUID(ownerId);
			audience.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_SUCCESS
					.arguments(AdventureHelper.miniMessageToComponent(username)).build()));
			linkPortalCatcher.remove(id);
			return;
		}

		// Offline player handling
		final UUID linkID = UUIDFetcher.getUUID(username).orElse(null);
		if (linkID == null || !Bukkit.getOfflinePlayer(linkID).hasPlayedBefore()) {
			audience.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE
					.arguments(AdventureHelper.miniMessageToComponent(username)).build()));
			linkPortalCatcher.remove(id);
			return;
		}

		instance.getStorageManager().getCachedUserDataWithFallback(linkID, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						runSync(() -> audience.sendMessage(
								instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE
										.arguments(AdventureHelper.miniMessageToComponent(username)).build())));
						linkPortalCatcher.remove(id);
						return;
					}

					final UserData offlineUser = result.get();
					final HellblockData linkedData = offlineUser.getHellblockData();

					final UUID ownerId = linkedData.getOwnerUUID();
					if (ownerId == null) {
						runSync(() -> audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_NO_ISLAND.build())));
						linkPortalCatcher.remove(id);
						return;
					}

					final Optional<UserData> linkedOwnerOpt = instance.getStorageManager().getCachedUserData(ownerId);
					if (linkedOwnerOpt.isEmpty()) {
						linkPortalCatcher.remove(id);
						return;
					}

					final UserData linkedOwner = linkedOwnerOpt.get();
					final HellblockData linkedOwnerData = linkedOwner.getHellblockData();

					if (!linkedOwnerData.hasHellblock() || linkedOwnerData.isAbandoned()
							|| linkedOwnerData.getHomeLocation() == null) {
						runSync(() -> audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_NO_ISLAND.build())));
						linkPortalCatcher.remove(id);
						return;
					}

					if (linkedOwnerData.getPartyMembers().contains(id)) {
						audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_LINK_OWN.build()));
						linkPortalCatcher.remove(id);
						return;
					}

					if (linkedOwnerData.isLocked()) {
						runSync(() -> audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_LOCKED.build())));
						linkPortalCatcher.remove(id);
						return;
					}

					if (linkedOwnerData.getBannedMembers().contains(id)) {
						runSync(() -> audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_BANNED.build())));
						linkPortalCatcher.remove(id);
						return;
					}

					onlineUser.get().getHellblockData().setLinkedPortalUUID(ownerId);
					runSync(() -> audience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_SUCCESS
									.arguments(AdventureHelper.miniMessageToComponent(username)).build())));
					linkPortalCatcher.remove(id);
				});
	}

	@EventHandler
	public void onMoveIfInPortal(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		// ignore same-block moves
		if (event.getTo().getBlockX() == event.getFrom().getBlockX()
				&& event.getTo().getBlockY() == event.getFrom().getBlockY()
				&& event.getTo().getBlockZ() == event.getFrom().getBlockZ()) {
			return;
		}
		if (playerInPortal(player)) {
			return;
		}
		final UUID id = player.getUniqueId();
		if (cancellablePortal.containsKey(id)) {
			cancelPortalTask(id);
		}
	}

	@EventHandler
	public void onEntityPortal(EntityPortalEvent event) {
		// prevent non-player entities from using nether portals in hellblock world
		final Entity ent = event.getEntity();
		if (!(ent instanceof Player) && ent.getWorld().getEnvironment() == Environment.NETHER
				&& instance.getHellblockHandler().isInCorrectWorld(event.getFrom().getWorld())) {
			event.setCancelled(true);
		}
	}

	private boolean playerInPortal(Player player) {
		try {
			final Block target = player.getTargetBlockExact(1);
			if (target == null) {
				return false;
			}
			final Material portal = target.getType();
			final Material standing = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
			return portal == Material.NETHER_PORTAL && (standing == Material.OBSIDIAN
					|| standing == Material.NETHER_PORTAL || standing == Material.AIR);
		} catch (IllegalStateException ex) {
			return false;
		}
	}

	private void runSync(Runnable task) {
		instance.getScheduler().executeSync(task);
	}

	private void cancelPortalTask(UUID id) {
		final SchedulerTask task = cancellablePortal.remove(id);
		if (task != null && !task.isCancelled()) {
			task.cancel();
		}
	}

	/**
	 * Sends the link portal tutorial message and plays linking sound (if
	 * configured).
	 */
	private void sendLinkTutorial(Player player, @Nullable String ownerName) {
		final Sender audience = instance.getSenderFactory().wrap(player);
		final Component arg = ownerName != null && !ownerName.isEmpty() ? AdventureHelper.miniMessageToComponent(ownerName)
				: AdventureHelper.miniMessageToComponent(instance.getTranslationManager()
						.miniMessageTranslation(MessageConstants.FORMAT_NONE.build().key()));
		audience.sendMessage(instance.getTranslationManager()
				.render(MessageConstants.MSG_HELLBLOCK_LINK_TUTORIAL.arguments(arg).build()));
		if (instance.getConfigManager().linkingHellblockSound() != null) {
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					instance.getConfigManager().linkingHellblockSound());
		}
	}

	@EventHandler
	public void onPortalQuit(PlayerQuitEvent event) {
		final UUID id = event.getPlayer().getUniqueId();
		if (cancellablePortal.containsKey(id)) {
			cancelPortalTask(id);
		}
		linkPortalCatcher.remove(id);
	}
}