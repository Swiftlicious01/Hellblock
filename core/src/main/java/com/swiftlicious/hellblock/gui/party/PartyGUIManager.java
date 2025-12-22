package com.swiftlicious.hellblock.gui.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;

public class PartyGUIManager implements PartyGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] layout;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final List<Tuple<Character, Section, Tuple<CustomItem, UUID, Action<Player>[]>>> memberIcons = new ArrayList<>();
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> newMemberIcons = new HashMap<>();
	protected final Map<Character, Section> newMemberIconSections = new HashMap<>();
	protected final ConcurrentMap<UUID, PartyGUI> partyGUICache = new ConcurrentHashMap<>();

	protected char backSlot;
	protected char ownerSlot;
	protected char trustedSlot;
	protected char bannedSlot;

	protected char memberSlot;

	protected String ownerName;
	protected List<String> ownerLore;
	protected String onlineStatus;
	protected String offlineStatus;

	protected Section bannedSection;
	protected Section trustedSection;
	protected Section banIconSection;
	protected Section trustIconSection;

	protected CustomItem backIcon;
	protected CustomItem ownerIcon;
	protected CustomItem trustedIcon;
	protected CustomItem bannedIcon;
	protected Action<Player>[] backActions;
	protected Action<Player>[] ownerActions;
	protected Action<Player>[] trustedActions;
	protected Action<Player>[] bannedActions;

	protected BookCloseListener bookListener;

	public PartyGUIManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		this.loadConfig();
		this.bookListener = new BookCloseListener();
		Bukkit.getPluginManager().registerEvents(this, instance);
		Bukkit.getPluginManager().registerEvents(bookListener, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		HandlerList.unregisterAll(bookListener);
		this.bookListener = null;
		this.decorativeIcons.clear();
		this.memberIcons.clear();
		this.newMemberIcons.clear();
	}

	private void loadConfig() {
		Section configFile = instance.getConfigManager().getGUIConfig("party.yml");
		if (configFile == null) {
			instance.getPluginLogger().severe("GUI for party.yml was unable to load correctly!");
			return;
		}
		Section config = configFile.getSection("party.gui");
		if (config == null) {
			instance.getPluginLogger().severe("party.gui returned null, please regenerate your party.yml GUI file.");
			return;
		}

		this.layout = config.getStringList("layout", new ArrayList<>()).toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "party.title"));

		this.onlineStatus = config.getString("login-status-placeholders.online", "<green><bold>ONLINE");
		this.offlineStatus = config.getString("login-status-placeholders.offline", "<red><bold>OFFLINE");

		this.trustedSection = config.getSection("trusted-members");
		this.bannedSection = config.getSection("banned-members");

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "X").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager(Player.class).parseActions(backSection.getSection("action"));
		}

		Section trustedSection = config.getSection("trusted-icon");
		if (trustedSection != null) {
			trustIconSection = trustedSection;
			trustedSlot = trustedSection.getString("symbol", "T").charAt(0);

			trustedIcon = new SingleItemParser("trusted", trustedSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			trustedActions = instance.getActionManager(Player.class).parseActions(trustedSection.getSection("action"));
		}

		Section bannedSection = config.getSection("banned-icon");
		if (bannedSection != null) {
			banIconSection = bannedSection;
			bannedSlot = bannedSection.getString("symbol", "B").charAt(0);

			bannedIcon = new SingleItemParser("banned", bannedSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			bannedActions = instance.getActionManager(Player.class).parseActions(bannedSection.getSection("action"));
		}

		Section ownerSection = config.getSection("owner-icon");
		if (ownerSection != null) {
			ownerSlot = ownerSection.getString("symbol", "O").charAt(0);
			ownerName = ownerSection.getString("display.name");
			ownerLore = ownerSection.getStringList("display.lore");

			ownerIcon = new SingleItemParser("owner", ownerSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			ownerActions = instance.getActionManager(Player.class).parseActions(ownerSection.getSection("action"));
		}

		Section memberSection = config.getSection("member-icon");
		if (memberSection != null) {
			memberSlot = memberSection.getString("symbol", "M").charAt(0);

			memberIcons.add(Tuple.of(memberSlot, memberSection, Tuple.of(
					new SingleItemParser("member", memberSection, instance.getConfigManager().getItemFormatFunctions())
							.getItem(),
					null, instance.getActionManager(Player.class).parseActions(memberSection.getSection("action")))));
		}

		Section newMemberSection = config.getSection("new-member-icon");
		if (newMemberSection != null) {
			if (memberSlot == '\u0000') {
				memberSlot = newMemberSection.getString("symbol", "M").charAt(0);
			}

			newMemberIcons.put(memberSlot, Pair.of(
					new SingleItemParser("new_member", newMemberSection,
							instance.getConfigManager().getItemFormatFunctions()).getItem(),
					instance.getActionManager(Player.class).parseActions(newMemberSection.getSection("action"))));

			newMemberIconSections.put(memberSlot, newMemberSection);
		}

		// Load decorative icons from the configuration
		Section decorativeSection = config.getSection("decorative-icons");
		if (decorativeSection != null) {
			for (Map.Entry<String, Object> entry : decorativeSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					try {
						String symbolStr = innerSection.getString("symbol");
						if (symbolStr == null || symbolStr.isEmpty()) {
							instance.getPluginLogger()
									.severe("Decorative icon missing symbol in entry: " + entry.getKey());
							continue;
						}

						char symbol = symbolStr.charAt(0);

						decorativeIcons.put(symbol,
								Pair.of(new SingleItemParser("gui", innerSection,
										instance.getConfigManager().getItemFormatFunctions()).getItem(),
										instance.getActionManager(Player.class)
												.parseActions(innerSection.getSection("action"))));
					} catch (Exception e) {
						instance.getPluginLogger().severe("Failed to load decorative icon entry: " + entry.getKey()
								+ " due to: " + e.getMessage());
					}
				}
			}
		}
	}

	@Override
	public boolean openPartyGUI(Player player, int islandId, boolean isOwner) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		HellblockData hellblockData = optionalUserData.get().getHellblockData();
		if (hellblockData.getOwnerUUID() == null) {
			instance.getPluginLogger()
					.warn("Owner UUID for player " + player.getName() + " was unable to be retrieved.");
			return false;
		}

		Context<Player> context = Context.player(player);
		Context<Integer> islandContext = Context.island(islandId);
		PartyGUI gui = new PartyGUI(this, context, islandContext, optionalUserData.get(), hellblockData, isOwner);

		Optional<UserData> optionalOwnerData = instance.getStorageManager().getOnlineUser(hellblockData.getOwnerUUID());
		if (optionalOwnerData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Hellblock owner data for " + hellblockData.getOwnerUUID() + " not loaded.");
			return false;
		}

		int maxSize = instance.getCoopManager().getMaxPartySize(optionalOwnerData.orElseThrow());
		Set<UUID> partyMembers = optionalOwnerData.get().getHellblockData().getPartyMembers();
		if (partyMembers == null)
			partyMembers = Collections.emptySet();
		Iterator<UUID> memberIt = partyMembers.iterator();

		int filled = 0;

		// 1) Real members
		for (Tuple<Character, Section, Tuple<CustomItem, UUID, Action<Player>[]>> memberSlot : memberIcons) {
			if (filled >= maxSize)
				break;

			UUID memberUUID = memberIt.hasNext() ? memberIt.next() : null;
			if (memberUUID != null) {
				gui.addElement(new PartyDynamicGUIElement(memberSlot.left(), new ItemStack(Material.AIR), memberUUID));
			} else {
				gui.addElement(new PartyDynamicGUIElement(memberSlot.left(), new ItemStack(Material.AIR), null));
			}
			filled++;
		}

		// 2) Invite slots (up to maxSize)
		for (Map.Entry<Character, Pair<CustomItem, Action<Player>[]>> entry : newMemberIcons.entrySet()) {
			if (filled >= maxSize)
				break;

			gui.addElement(new PartyDynamicGUIElement(entry.getKey(), new ItemStack(Material.AIR), null));
			filled++;
		}

		// 3) Filler slots (beyond maxSize, but still in layout)
		for (int i = filled; i < memberIcons.size(); i++) {
			Tuple<Character, Section, Tuple<CustomItem, UUID, Action<Player>[]>> memberSlot = memberIcons.get(i);

			Pair<CustomItem, Action<Player>[]> newMemberIcon = newMemberIcons.get(memberSlot.left());
			if (newMemberIcon != null) {
				gui.addElement(new PartyDynamicGUIElement(memberSlot.left(), new ItemStack(Material.AIR), null));
			} else {
				gui.addElement(
						new PartyGUIElement(memberSlot.left(), buildPlaceholderForSlot(memberSlot.left(), context)));
			}
		}

		// 4) Decorative slots
		decorativeIcons.entrySet().forEach(
				entry -> gui.addElement(new PartyGUIElement(entry.getKey(), entry.getValue().left().build(context))));

		// 5) Back, Owner, Trusted + Banned placeholders (dynamic so refresh() can
		// populate them)
		gui.addElement(new PartyDynamicGUIElement(backSlot, new ItemStack(Material.AIR)));
		gui.addElement(new PartyDynamicGUIElement(ownerSlot, new ItemStack(Material.AIR)));
		gui.addElement(new PartyDynamicGUIElement(trustedSlot, new ItemStack(Material.AIR)));
		gui.addElement(new PartyDynamicGUIElement(bannedSlot, new ItemStack(Material.AIR)));

		gui.build().show();
		gui.refresh();
		partyGUICache.put(player.getUniqueId(), gui);
		return true;
	}

	/**
	 * Build an ItemStack placeholder for a given slot using decorative icons first,
	 * then sensible fallbacks. IMPORTANT: This uses only existing CustomItem
	 * templates (decorative/member/newMember) and never creates a CustomItem via a
	 * factory method.
	 */
	private ItemStack buildPlaceholderForSlot(Character slot, Context<Player> context) {
		// 1) try decorative icon exact match
		Pair<CustomItem, Action<Player>[]> deco = decorativeIcons.get(slot);
		if (deco != null) {
			return deco.left().build(context);
		}

		// 2) try first decorative icon as generic fallback
		if (!decorativeIcons.isEmpty()) {
			return decorativeIcons.values().iterator().next().left().build(context);
		}

		// 3) try member slot template if present
		for (Tuple<Character, Section, Tuple<CustomItem, UUID, Action<Player>[]>> m : memberIcons) {
			if (Objects.equals(m.left(), slot)) {
				return m.right().left().build(context);
			}
		}

		// 4) try new member template if present
		Pair<CustomItem, Action<Player>[]> nm = newMemberIcons.get(slot);
		if (nm != null) {
			return nm.left().build(context);
		}

		// 5) absolute fallback (only if no templates exist) - a plain ItemStack
		return new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
	}

	/**
	 * This method handles the closing of an inventory.
	 *
	 * @param event The InventoryCloseEvent that triggered this method.
	 */
	@EventHandler
	public void onCloseInv(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player player))
			return;
		if (!(event.getInventory().getHolder() instanceof PartyGUIHolder))
			return;
		partyGUICache.remove(player.getUniqueId());
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		partyGUICache.remove(event.getPlayer().getUniqueId());
	}

	/**
	 * This method handles dragging items in an inventory.
	 *
	 * @param event The InventoryDragEvent that triggered this method.
	 */
	@EventHandler
	public void onDragInv(InventoryDragEvent event) {
		if (event.isCancelled())
			return;
		Inventory inventory = event.getInventory();
		if (!(inventory.getHolder() instanceof PartyGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		PartyGUI gui = partyGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		// Check if any dragged slot is in the GUI (top inventory)
		for (int slot : event.getRawSlots()) {
			if (slot < event.getInventory().getSize()) {
				event.setCancelled(true);
				return;
			}
		}

		// If the drag is only in the player inventory, do nothing special
		// but still make sure no ghost items appear
		event.setCancelled(true);

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	/**
	 * This method handles inventory click events.
	 *
	 * @param event The InventoryClickEvent that triggered this method.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onClickInv(InventoryClickEvent event) {
		Inventory clickedInv = event.getClickedInventory();
		if (clickedInv == null)
			return;

		Player player = (Player) event.getWhoClicked();

		// Check if the clicked inventory is a PartyGUI
		if (!(event.getInventory().getHolder() instanceof PartyGUIHolder))
			return;

		PartyGUI gui = partyGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		// Handle shift-clicks, number keys, or clicking outside GUI
		if (event.getClick().isShiftClick() || event.getClick().isKeyboardClick()) {
			event.setCancelled(true);
			return;
		}

		if (!Objects.equals(clickedInv, player.getInventory())) {
			int slot = event.getSlot();
			PartyGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			HellblockData hellblockData = gui.hellblockData;
			Optional<UserData> userData = instance.getStorageManager()
					.getOnlineUser(gui.context.holder().getUniqueId());

			if (userData.isEmpty() || !hellblockData.hasHellblock() || hellblockData.getOwnerUUID() == null) {
				event.setCancelled(true);
				player.closeInventory();
				return;
			}

			Pair<CustomItem, Action<Player>[]> decorativeIcon = this.decorativeIcons.get(element.getSymbol());
			if (decorativeIcon != null) {
				event.setCancelled(true);
				ActionManager.trigger(gui.context, decorativeIcon.right());
				return;
			}

			if (element.getSymbol() == backSlot) {
				event.setCancelled(true);
				boolean opened = instance.getHellblockGUIManager().openHellblockGUI(gui.context.holder(),
						gui.islandContext.holder(), gui.isOwner);
				if (opened)
					ActionManager.trigger(gui.context, backActions);
				return;
			}

			if (element.getSymbol() == ownerSlot) {
				event.setCancelled(true);
				ActionManager.trigger(gui.context, ownerActions);
			}

			Sender audience = instance.getSenderFactory().wrap(gui.context.holder());

			if (element.getSymbol() == trustedSlot) {
				event.setCancelled(true);

				List<UUID> trusted = new ArrayList<>(hellblockData.getTrustedMembers());

				// If not the island owner
				if (!gui.isOwner) {
					if (trusted.isEmpty()) {
						instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_NO_TRUSTED_FOUND.build()));
						AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
								Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
										net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
						return;
					}

					new TrustedMembersGUI(instance, player, gui.islandContext, trustedSection, gui.userData,
							gui.isOwner).open();
					ActionManager.trigger(gui.context, trustedActions);
					return;
				}

				// If owner but no trusted members
				if (trusted.isEmpty()) {
					new AnvilTrustInputGUI(instance, player, gui.islandContext, gui.userData, gui.isOwner).open();
					ActionManager.trigger(gui.context, trustedActions);
					return;
				}

				// Owner and trusted members exist → open interactive GUI
				new TrustedMembersGUI(instance, player, gui.islandContext, trustedSection, gui.userData, gui.isOwner)
						.open();
				ActionManager.trigger(gui.context, trustedActions);
				return;
			}

			if (element.getSymbol() == bannedSlot) {
				event.setCancelled(true);

				List<UUID> banned = new ArrayList<>(hellblockData.getBannedMembers());

				// If not the island owner
				if (!gui.isOwner) {
					if (banned.isEmpty()) {
						instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_NO_BANNED_FOUND.build()));
						AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
								Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
										net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
						return;
					}

					new BannedMembersGUI(instance, player, gui.islandContext, bannedSection, gui.userData, gui.isOwner)
							.open();
					ActionManager.trigger(gui.context, bannedActions);
					return;
				}

				// If owner but no banned members
				if (banned.isEmpty()) {
					new AnvilBanInputGUI(instance, player, gui.islandContext, gui.userData, gui.isOwner).open();
					ActionManager.trigger(gui.context, bannedActions);
					return;
				}

				// Owner and banned members exist → open interactive GUI
				new BannedMembersGUI(instance, player, gui.islandContext, bannedSection, gui.userData, gui.isOwner)
						.open();
				ActionManager.trigger(gui.context, bannedActions);
				return;
			}

			if (!hellblockData.getOwnerUUID().equals(gui.context.holder().getUniqueId())) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (hellblockData.isAbandoned()) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			for (Tuple<Character, Section, Tuple<CustomItem, UUID, Action<Player>[]>> memberIcon : memberIcons) {
				if (element.getSymbol() == memberIcon.left() && element.getUUID() != null) {
					event.setCancelled(true);

					UUID memberUUID = element.getUUID();
					String username = null;

					Player onlinePlayer = Bukkit.getPlayer(memberUUID);
					if (onlinePlayer != null) {
						username = onlinePlayer.getName();
					} else {
						OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
						if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
							username = offlinePlayer.getName();
						}
					}

					if (username != null) {
						if (event.isLeftClick()) {
							instance.getCoopManager().removeMemberFromHellblock(userData.get(), username, memberUUID)
									.thenAccept(result -> {
										if (result)
											ActionManager.trigger(gui.context, memberIcon.right().right());
									});
						} else {
							if (event.isShiftClick()) {
								if (!instance.getConfigManager().transferIslands()) {
									audience.sendMessage(instance.getTranslationManager().render(
											MessageConstants.MSG_HELLBLOCK_COOP_TRANSFER_OWNERSHIP_DISABLED.build()));
									AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
											Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
													net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
									return;
								}
								if (hellblockData.getTransferCooldown() > 0) {
									gui.islandContext
											.arg(ContextKeys.TRANSFER_COOLDOWN, hellblockData.getTransferCooldown())
											.arg(ContextKeys.TRANSFER_COOLDOWN_FORMATTED, instance.getCooldownManager()
													.getFormattedCooldown(hellblockData.getTransferCooldown()));
									audience.sendMessage(
											instance.getTranslationManager()
													.render(MessageConstants.MSG_HELLBLOCK_TRANSFER_ON_COOLDOWN
															.arguments(AdventureHelper.miniMessageToComponent(
																	instance.getCooldownManager().getFormattedCooldown(
																			hellblockData.getTransferCooldown())))
															.build()));
									AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
											Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
													net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
									return;
								}
								// Load target user data async
								// Load target user data async
								instance.getStorageManager().getCachedUserDataWithFallback(memberUUID, true)
										.thenCompose(optData -> {
											if (optData.isEmpty()) {
												instance.getScheduler().executeSync(() -> event.setCancelled(true));
												return CompletableFuture.completedFuture(false);
											}

											final UserData memberData = optData.get();

											return instance.getCoopManager()
													.transferOwnershipOfHellblock(userData.get(), memberData, false)
													.handle((transferSuccess, ex) -> {
														if (ex != null) {
															instance.getPluginLogger()
																	.warn("Failed to transfer ownership to "
																			+ memberData.getName(), ex);
															return false;
														}
														return transferSuccess != null && transferSuccess;
													}).thenCompose(success -> instance.getStorageManager()
															.unlockUserData(memberUUID).thenApply(unused -> success));
										}).thenAccept(transferSuccess -> {
											if (Boolean.TRUE.equals(transferSuccess)) {
												gui.isOwner = false;
												ActionManager.trigger(gui.context, memberIcon.right().right());
												instance.getHellblockGUIManager().openHellblockGUI(gui.context.holder(),
														gui.islandContext.holder(), gui.isOwner);
											}
										});
							}
						}
						break;
					}
				}
			}

			Pair<CustomItem, Action<Player>[]> newMemberIcon = this.newMemberIcons.get(element.getSymbol());
			if (newMemberIcon != null && element.getUUID() == null) {
				int maxSize = instance.getCoopManager().getMaxPartySize(userData.get());
				int slotIndex = getMemberSlotIndex(element.getSymbol());
				if (slotIndex >= 0 && slotIndex < maxSize) {
					// within allowed party size -> interactive invite
					event.setCancelled(true);
					instance.getInviteGUIManager().openInvitationGUI(player, gui.islandContext.holder(), gui.isOwner);
					ActionManager.trigger(gui.context, newMemberIcon.right());
				} else {
					// beyond allowed slots => non-interactive filler (still cancel the click)
					event.setCancelled(true);
				}
				return;
			}
		}

		// Refresh the GUI
		if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 15000)) {
			gui.hellblockData.updateLastIslandActivity();
		}
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	public void openCustomBook(Player player, Item<ItemStack> book, Runnable onClose) {
		player.openBook(book.getItem());
		bookListener.registerCallback(player.getUniqueId(), onClose);
	}

	/**
	 * Return the Section to use for a slot symbol. Prefers new-member-section (if
	 * one exists), otherwise returns the member-icons section (if any), otherwise
	 * null.
	 */
	@Nullable
	public Section getSectionForSlotChar(Character symbol) {
		Section s = newMemberIconSections.get(symbol);
		if (s != null)
			return s;
		return memberIcons.stream().filter(t -> Objects.equals(t.left(), symbol)).findFirst().map(Tuple::mid)
				.orElse(null);
	}

	/**
	 * Returns the zero-based index of the slot in the logical member-slot ordering:
	 * indices 0..memberIcons.size()-1 -> entries from memberIcons (in list order)
	 * indices memberIcons.size().. -> keys from newMemberIcons (iteration order)
	 * Returns -1 if symbol not found.
	 */
	public int getMemberSlotIndex(Character symbol) {
		for (int i = 0; i < memberIcons.size(); i++) {
			if (Objects.equals(memberIcons.get(i).left(), symbol))
				return i;
		}

		int offset = memberIcons.size();
		int idx = 0;
		for (Character key : newMemberIcons.keySet()) {
			if (Objects.equals(key, symbol))
				return offset + idx;
			idx++;
		}

		return -1;
	}

	private class BookCloseListener implements Listener {

		private final Map<UUID, Runnable> closeCallbacks = new HashMap<>();

		public void registerCallback(UUID uuid, Runnable callback) {
			closeCallbacks.put(uuid, callback);
		}

		@EventHandler
		public void onQuit(PlayerQuitEvent event) {
			closeCallbacks.remove(event.getPlayer().getUniqueId());
		}

		@EventHandler
		public void onInventoryClose(InventoryCloseEvent event) {
			UUID uuid = event.getPlayer().getUniqueId();
			Runnable callback = closeCallbacks.remove(uuid);
			if (callback != null) {
				callback.run();
			}
		}
	}
}