package com.swiftlicious.hellblock.gui.invite;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.authlib.GameProfile;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.GameProfileBuilder;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;

import net.kyori.adventure.text.Component;

public class InviteGUI {

	private final Map<Character, InviteGUIElement> itemsCharMap;
	private final Map<Integer, InviteGUIElement> itemsSlotMap;
	private final Map<Integer, InviteGUIElement> inviteSlotsMap;

	private final InviteGUIManager manager;
	protected final AnvilInventory inventory;
	protected final Context<Player> context;
	protected final Context<Integer> islandContext;
	protected final HellblockData hellblockData;
	protected final boolean isOwner;

	protected static final long POLL_INTERVAL_TICKS = 2L; // how often to poll the anvil rename text

	protected final Map<Integer, InviteDynamicGUIElement> cachedHeads;

	// saved items for this player's inventory while GUI is open
	protected final Map<Integer, ItemStack> savedItems;
	protected final Map<Integer, ItemStack> savedArmor; // optional: save armor pieces
	protected ItemStack savedOffHand;

	// UI state
	protected int currentPage = 0;
	// polling task handle (using your scheduler)
	private SchedulerTask searchPollingTask;
	protected String searchedName = null;

	public InviteGUI(InviteGUIManager manager, Context<Player> context, Context<Integer> islandContext,
			HellblockData hellblockData, boolean isOwner) {
		this.manager = manager;
		this.context = context;
		this.islandContext = islandContext;
		this.searchedName = manager.instance.getItemManager().wrap(manager.searchIcon.build(context)).displayName()
				.orElse("<Type Name Here>");
		this.hellblockData = hellblockData;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		this.inviteSlotsMap = new HashMap<>();
		this.cachedHeads = new HashMap<>();
		this.savedItems = new HashMap<>();
		this.savedArmor = new HashMap<>();
		this.isOwner = isOwner;
		final var holder = new InviteGUIHolder();
		this.inventory = (AnvilInventory) Bukkit.createInventory(holder, InventoryType.ANVIL);
		holder.setInventory(this.inventory);
	}

	private void init() {
		int line = 0;
		if (manager.layout.length != 4) {
			throw new IllegalArgumentException("Invitation GUI layout must have exactly 4 rows of 9 characters");
		}
		for (String content : manager.layout) {
			for (int index = 0; index < 9; index++) {
				final String column = Arrays.asList(manager.layout).get(index);
				if (column.length() != 9) {
					throw new IllegalArgumentException("Each layout row must be exactly 9 characters: " + column);
				}
				final char symbol = index < content.length() ? content.charAt(index) : ' ';
				final InviteGUIElement element = this.itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * 9);
					this.itemsSlotMap.put(index + line * 9, element);
				}
			}
			line++;
		}
		this.itemsSlotMap.entrySet().forEach(entry -> context.holder().getInventory().setItem(entry.getKey(),
				entry.getValue().getItemStack().clone()));
		this.inventory.setItem(0, manager.searchIcon.build(context));
		this.inventory.setItem(2, manager.playerNotFoundIcon.build(context));
		this.inviteSlotsMap.put(0, new InviteDynamicGUIElement('S', this.inventory.getItem(0)));
		this.inviteSlotsMap.put(2, new InviteDynamicGUIElement('R', this.inventory.getItem(2)));
	}

	public InviteGUI addElement(InviteGUIElement... elements) {
		for (InviteGUIElement element : elements) {
			this.itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public InviteGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(this.inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
	}

	@Nullable
	public InviteGUIElement getElement(int slot) {
		return this.itemsSlotMap.get(slot);
	}

	@Nullable
	public InviteGUIElement getElement(char slot) {
		return this.itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The InviteGUI instance.
	 */
	public InviteGUI refresh() {
		final InviteDynamicGUIElement backElement = (InviteDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			backElement.setItemStack(manager.backIcon.build(context));
		}
		final InviteDynamicGUIElement leftElement = (InviteDynamicGUIElement) getElement(manager.leftSlot);
		if (leftElement != null && !leftElement.getSlots().isEmpty()) {
			leftElement.setItemStack(manager.leftIcon.build(context));
		}
		final InviteDynamicGUIElement rightElement = (InviteDynamicGUIElement) getElement(manager.rightSlot);
		if (rightElement != null && !rightElement.getSlots().isEmpty()) {
			rightElement.setItemStack(manager.rightIcon.build(context));
		}
		this.itemsSlotMap.entrySet().stream().filter(entry -> entry.getValue() instanceof InviteDynamicGUIElement)
				.forEach(entry -> {
					InviteDynamicGUIElement dynamicGUIElement = (InviteDynamicGUIElement) entry.getValue();
					context.holder().getInventory().setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
				});
		refreshSearch();
		refreshPlayerHeads(this.currentPage);
		return this;
	}

	/**
	 * Save player's inventory (clone stacks).
	 */
	public void saveItems(@NotNull Player player) {
		this.savedItems.clear();
		this.savedArmor.clear();
		final PlayerInventory inv = player.getInventory();
		for (int slot = 0; slot < inv.getSize(); slot++) {
			final ItemStack item = inv.getItem(slot);
			if (item != null && item.getType() != Material.AIR) {
				this.savedItems.put(slot, item.clone());
			}
		}

		// Save off-hand
		final ItemStack offHand = inv.getItemInOffHand();
		if (offHand.getType() != Material.AIR) {
			this.savedOffHand = offHand.clone();
		}

		// Save armor contents
		final ItemStack[] armor = inv.getArmorContents();
		for (int i = 0; i < armor.length; i++) {
			final ItemStack is = armor[i];
			if (is != null && is.getType() != Material.AIR) {
				this.savedArmor.put(i, is.clone());
			}
		}
	}

	/**
	 * Clears player's inventory while GUI is open.
	 */
	public void clearPlayerInventory(@NotNull Player player) {
		final Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getSize(); i++) {
			inv.setItem(i, new ItemStack(Material.AIR));
		}
		// Clear off-hand
		player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
		// clear armor
		player.getInventory().setArmorContents(new ItemStack[4]);
		player.updateInventory();
	}

	/**
	 * Restore saved items to player and clear the saved maps.
	 */
	public void returnItems(@NotNull Player player) {
		final Inventory inv = player.getInventory();
		this.savedItems.entrySet().forEach(e -> {
			final ItemStack is = e.getValue();
			if (is != null && is.getType() != Material.AIR) {
				inv.setItem(e.getKey(), is.clone());
			}
		});
		// Restore off-hand
		if (this.savedOffHand != null && this.savedOffHand.getType() != Material.AIR) {
			player.getInventory().setItemInOffHand(this.savedOffHand.clone());
		}
		if (!this.savedArmor.isEmpty()) {
			final ItemStack[] armor = new ItemStack[player.getInventory().getArmorContents().length];
			savedArmor.entrySet().forEach(e -> armor[e.getKey()] = e.getValue().clone());
			player.getInventory().setArmorContents(armor);
		}
		player.updateInventory();
		this.savedItems.clear();
		this.savedArmor.clear();
		this.savedOffHand = null;
	}

	/**
	 * Start polling the anvil rename text to update the search and result slots.
	 */
	public void startSearchPolling() {
		cancelSearchPolling(); // safety first

		this.searchPollingTask = manager.instance.getScheduler().sync().runRepeating(() -> {
			try {
				refreshSearch();
			} catch (Exception ex) {
				manager.instance.getPluginLogger().warn("Error while polling invite search", ex);
			}
		}, 0L, POLL_INTERVAL_TICKS, LocationUtils.getAnyLocationInstance());
	}

	/**
	 * Cancel search polling if running.
	 */
	public void cancelSearchPolling() {
		if (this.searchPollingTask == null || this.searchPollingTask.isCancelled()) {
			return;
		}
		try {
			this.searchPollingTask.cancel();
		} catch (Throwable t) {
			manager.instance.getPluginLogger().warn("Error while cancelling search polling task", t);
		}
		this.searchPollingTask = null;
	}

	/**
	 * Refresh top anvil search UI: slot 0 shows search icon labeled with rename
	 * text; slot 2 shows either player head (found) or playerNotFoundIcon
	 * (barrier). Heavy operations (UUID/profile fetch) happen async.
	 */
	public InviteGUI refreshSearch() {
		// update the search icon (slot 0)
		final InviteDynamicGUIElement searchElement = (InviteDynamicGUIElement) this.inviteSlotsMap.get(0);
		if (searchElement != null && !searchElement.getSlots().isEmpty()) {
			final Item<ItemStack> search = manager.instance.getItemManager().wrap(manager.searchIcon.build(context));
			final Component username = getInputText(context.holder());
			final String plainName = AdventureHelper.componentToPlainText(username);
			search.displayName(AdventureHelper.miniMessageToJson("<gray>" + plainName));
			searchElement.setItemStack(search.load());
			// Also place into the actual anvil inventory top, if needed:
			searchElement.getSlots().stream().mapToInt(Integer::valueOf)
					.forEach(slot -> this.inventory.setItem(slot, searchElement.getItemStack()));
		}

		// update the result icon (slot 2)
		final InviteDynamicGUIElement headElement = (InviteDynamicGUIElement) this.inviteSlotsMap.get(2);
		final Component renameText = getInputText(context.holder());
		final String plainInput = AdventureHelper.componentToPlainText(renameText);

		if (headElement != null && !headElement.getSlots().isEmpty()) {
			// Quick check if username is syntactically valid
			final boolean syntacticallyValid = !plainInput.isEmpty() && plainInput.matches("^[a-zA-Z0-9_]+$");

			if (!syntacticallyValid) {
				// show not found icon immediately
				runFallbackUI(headElement);
				return this;
			}

			// check online user presence (synchronous, cheap)
			final boolean playerFound = manager.instance.getStorageManager().getOnlineUsers().stream()
					.filter(Objects::nonNull).map(UserData::getName)
					.anyMatch(userName -> userName.equalsIgnoreCase(plainInput));

			if (!playerFound) {
				runFallbackUI(headElement);
				return this;
			}

			// Fetch UUID & profile async, then update inventory sync
			CompletableFuture.runAsync(() -> UUIDFetcher.getUUID(plainInput).ifPresentOrElse(uuid -> {
				try {
					GameProfile profile = GameProfileBuilder.fetch(uuid);
					String texture = profile.getProperties().get("textures").iterator().next().getValue();
					Item<ItemStack> head = manager.instance.getItemManager()
							.wrap(manager.playerFoundIcon.build(context));
					String displayName = AdventureHelper
							.miniMessageToJson(manager.playerFoundName.replace("{player}", plainInput));
					head.displayName(displayName);
					head.skull(texture);
					ItemStack loaded = head.loadCopy();
					manager.instance.getScheduler().sync().run(() -> {
						headElement.setUUID(uuid);
						headElement.setItemStack(loaded);
						headElement.getSlots().stream().mapToInt(Integer::valueOf)
								.forEach(slot -> this.inventory.setItem(slot, loaded));
					});
				} catch (IOException | IllegalArgumentException ex) {
					manager.instance.getPluginLogger().warn("Error fetching profile for search", ex);
					runFallbackUI(headElement);
				}
			}, () -> runFallbackUI(headElement)));
		}

		// Update cached online heads displayed: this keeps displayed items in-sync with
		// cachedHeads
		refreshOnlineHeads();

		return this;
	}

	/**
	 * Fallback UI update on sync thread if UUID/profile could not be loaded.
	 */
	private void runFallbackUI(InviteDynamicGUIElement headElement) {
		manager.instance.getScheduler().sync().run(() -> {
			headElement.setItemStack(manager.playerNotFoundIcon.build(context));
			headElement.setUUID(null);
			headElement.getSlots().stream().mapToInt(Integer::valueOf)
					.forEach(slot -> this.inventory.setItem(slot, headElement.getItemStack()));
		});
	}

	public void refreshOnlineHeads() {
		// clone to avoid mutating the original
		getOnlinePlayerHeadsAsync(onlineHeads -> onlineHeads.entrySet().forEach(entry -> {
			if (entry.getValue() instanceof InviteDynamicGUIElement dynamicGUIElement) {
				final int slot = entry.getKey();
				final InviteDynamicGUIElement clone = new InviteDynamicGUIElement(manager.playerSlot,
						dynamicGUIElement.getItemStack().clone());
				clone.setUUID(dynamicGUIElement.getUUID());
				context.holder().getInventory().setItem(slot, clone.getItemStack());
				this.cachedHeads.put(slot, clone);
			}
		}));
	}

	/**
	 * Build a map of possible online player heads that satisfy the filter (not
	 * self, online, no hellblock, not in searchedName, not already invited). This
	 * function does not do heavy network operation; it prepares the template item
	 * and UUIDs.
	 */
	public void getOnlinePlayerHeadsAsync(Consumer<Map<Integer, InviteDynamicGUIElement>> callback) {
		final Collection<UserData> online = manager.instance.getStorageManager().getOnlineUsers();

		// Run callback sync
		CompletableFuture.supplyAsync(() -> {
			final Map<Integer, InviteDynamicGUIElement> heads = new HashMap<>();
			int slotIndex = 0;

			for (UserData userData : online) {
				final UUID id = userData.getUUID();

				// Filters
				if (id.equals(context.holder().getUniqueId())) {
					continue;
				}
				if (!userData.isOnline()) {
					continue;
				}
				if (userData.getHellblockData().hasHellblock()) {
					continue;
				}
				if (this.searchedName != null && userData.getName().equalsIgnoreCase(this.searchedName)) {
					continue;
				}
				if (userData.getHellblockData().hasInvite(context.holder().getUniqueId())) {
					continue;
				}
				if (hellblockData.isInParty(id)) {
					continue;
				}

				try {
					final GameProfile profile = GameProfileBuilder.fetch(id);
					final String texture = profile.getProperties().get("textures").iterator().next().getValue();

					final Item<ItemStack> head = manager.instance.getItemManager()
							.wrap(manager.playerIcon.build(context));
					final String username = AdventureHelper
							.miniMessageToJson(manager.playerName.replace("{player}", userData.getName()));
					head.displayName(username);
					head.skull(texture);

					final InviteDynamicGUIElement element = new InviteDynamicGUIElement(manager.playerSlot,
							head.load());
					element.setUUID(id);

					if (slotIndex < manager.headSlots.size()) {
						final int slot = manager.headSlots.get(slotIndex++);
						heads.put(slot, element);
					} else {
						break;
					}
				} catch (Exception ex) {
					manager.instance.getPluginLogger().warn("Failed to build head template for " + userData.getName(),
							ex);
				}
			}
			return heads;
		}).thenAccept(heads -> manager.instance.getScheduler().sync().run(() -> callback.accept(heads),
				context.holder().getLocation()));
	}

	/**
	 * Refresh the paged player heads for the given page (0-indexed). This method: -
	 * collects eligible users - selects the page subset - fetches GameProfile
	 * textures async and updates inventory sync
	 */
	public void refreshPlayerHeads(int page) {
		this.currentPage = Math.max(0, page);

		// Collect eligible users
		final List<UserData> eligible = manager.instance.getStorageManager().getOnlineUsers().stream()
				.filter(userData -> {
					final UUID id = userData.getUUID();
					if (id.equals(context.holder().getUniqueId())) {
						return false;
					}
					if (!userData.isOnline()) {
						return false;
					}
					if (userData.getHellblockData().hasHellblock()) {
						return false;
					}
					if (this.searchedName != null && userData.getName().equalsIgnoreCase(this.searchedName)) {
						return false;
					}
					if (userData.getHellblockData().hasInvite(context.holder().getUniqueId())) {
						return false;
					}
					if (hellblockData.isInParty(id)) {
						return false;
					}
					return true;
				}).toList();

		final int pageSize = manager.getPageSize();
		int start = this.currentPage * pageSize;
		if (start >= eligible.size()) {
			// Clamp to last page
			this.currentPage = Math.max(0, (eligible.size() - 1) / pageSize);
			start = this.currentPage * pageSize;
		}

		final List<UserData> pageList = eligible.stream().skip(start).limit(pageSize).toList();

		// Clear head slots sync
		manager.instance.getScheduler().sync()
				.run(() -> manager.headSlots.stream().mapToInt(Integer::valueOf).forEach(slot -> {
					context.holder().getInventory().setItem(slot, new ItemStack(Material.AIR));
					this.cachedHeads.remove(slot);
				}));

		// Fetch profiles async
		CompletableFuture.runAsync(() -> {
			final Map<Integer, InviteDynamicGUIElement> toPlace = new HashMap<>();
			int index = 0;
			for (UserData user : pageList) {
				try {
					final UUID id = user.getUUID();
					final GameProfile profile = GameProfileBuilder.fetch(id);
					final String texture = profile.getProperties().get("textures").iterator().next().getValue();

					final Item<ItemStack> head = manager.instance.getItemManager()
							.wrap(manager.playerIcon.build(context));
					final String username = AdventureHelper
							.miniMessageToJson(manager.playerName.replace("{player}", user.getName()));
					head.displayName(username);
					head.skull(texture);

					final InviteDynamicGUIElement element = new InviteDynamicGUIElement(manager.playerSlot,
							head.load());
					element.setUUID(id);

					final int slot = manager.headSlots.get(index++);
					toPlace.put(slot, element);
				} catch (Exception ex) {
					manager.instance.getPluginLogger().warn("Failed to fetch profile for invite head", ex);
				}
			}

			// Apply sync
			manager.instance.getScheduler().sync().run(() -> {
				toPlace.entrySet().forEach(e -> {
					context.holder().getInventory().setItem(e.getKey(), e.getValue().getItemStack());
					this.cachedHeads.put(e.getKey(), e.getValue());
				});
				updateNavigationIcons(eligible.size());
			});
		});
	}

	private void updateNavigationIcons(int totalEligible) {
		int totalPages = (int) Math.ceil((double) totalEligible / manager.getPageSize());
		if (totalPages <= 0) {
			totalPages = 1;
		}

		// LEFT slot
		if (this.currentPage > 0) {
			// functional left icon
			context.holder().getInventory().setItem(manager.leftSlot, manager.leftIcon.build(context));
		} else {
			// decorative fallback (doesn't assume the slot symbol is decorative)
			context.holder().getInventory().setItem(manager.leftSlot,
					getDecorativePlaceholderForSlot(manager.leftSlot));
		}

		// RIGHT slot
		if (this.currentPage < totalPages - 1) {
			// functional right icon
			context.holder().getInventory().setItem(manager.rightSlot, manager.rightIcon.build(context));
		} else {
			// decorative fallback
			context.holder().getInventory().setItem(manager.rightSlot,
					getDecorativePlaceholderForSlot(manager.rightSlot));
		}

		// BACK always shown
		context.holder().getInventory().setItem(manager.backSlot, manager.backIcon.build(context));
	}

	/**
	 * Returns an ItemStack to use as the decorative placeholder for `slot`.
	 * Preferred order: 1) decorativeIcons.get(symbolForSlot) if present 2) first
	 * entry in decorativeIcons 3) hard fallback gray pane
	 */
	private ItemStack getDecorativePlaceholderForSlot(int slot) {
		final InviteGUIElement element = getElement(slot);
		if (element != null) {
			final char symbol = element.getSymbol();
			final Pair<CustomItem, Action<Player>[]> mapped = manager.decorativeIcons.get(symbol);
			if (mapped != null && mapped.left() != null) {
				try {
					return mapped.left().build(context);
				} catch (Exception ignored) {
					/* fall through to next option */ }
			}
		}

		// fallback: use the first decorative icon configured (if any)
		for (Pair<CustomItem, Action<Player>[]> pair : manager.decorativeIcons.values()) {
			if (pair != null && pair.left() != null) {
				try {
					return pair.left().build(context);
				} catch (Exception ignored) {
					/* try next */ }
			}
		}

		// final fallback: a plain black pane so slot isn't empty
		final ItemStack fallback = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		return fallback;
	}

	@SuppressWarnings("removal")
	@NotNull
	public Component getInputText(@NotNull Player player) {
		InventoryView view = player.getOpenInventory();
		if (view == null)
			return Component.empty();

		String raw = null;

		// Try using org.bukkit.inventory.view.AnvilView#getRenameText (1.21+)
		if (VersionHelper.isVersionNewerThan1_21()) {
			try {
				Class<?> anvilViewClass = Class.forName("org.bukkit.inventory.view.AnvilView");
				if (anvilViewClass.isInstance(view)) {
					Method getRenameText = anvilViewClass.getMethod("getRenameText");
					Object result = getRenameText.invoke(view);
					if (result instanceof String str) {
						raw = str;
					}
				}
			} catch (ClassNotFoundException e) {
				// <1.21 — class doesn’t exist, ignore
			} catch (Throwable t) {
				manager.instance.getPluginLogger().warn("Failed to access AnvilView#getRenameText", t);
			}
		} else {
			// Fallback to older API (1.20 and below)
			if (raw == null) {
				try {
					raw = inventory.getRenameText();
				} catch (Throwable ignored) {
				}
			}
		}

		if (raw == null || raw.isEmpty())
			return Component.empty();

		// Deserialize into a Component (plain text or MiniMessage)
		return AdventureHelper.miniMessageToComponent(raw);
	}
}