package com.swiftlicious.hellblock.gui.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mojang.authlib.GameProfile;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.GameProfileBuilder;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public abstract class BasePaginatedMemberGUI extends MemberGUI {

	protected final HellblockPlugin plugin;
	protected final Player player;
	protected final Context<Integer> islandContext;
	protected final Section config;
	protected final HellblockData data;
	protected final boolean isOwner;

	protected final char memberSlot;
	protected final char leftArrow;
	protected final char rightArrow;
	protected final char backButton;
	protected final char addButton;
	protected final char viewButton;

	protected int currentPage = 0;
	protected final int itemsPerPage;

	public BasePaginatedMemberGUI(HellblockPlugin plugin, Player player, Context<Integer> islandContext, Section config,
			HellblockData data, boolean isOwner) {
		super(player, config.getString("title"), config.getStringList("layout").toArray(new String[0]));
		this.plugin = plugin;
		this.player = player;
		this.islandContext = islandContext;
		this.config = config;
		this.data = data;
		this.isOwner = isOwner;

		this.memberSlot = config.getString("member-icon.symbol", "M").charAt(0);
		this.leftArrow = config.getString("left-icon.symbol", "L").charAt(0);
		this.rightArrow = config.getString("right-icon.symbol", "R").charAt(0);
		this.backButton = config.getString("back-icon.symbol", "K").charAt(0);
		this.addButton = config.getString("add-icon.symbol", "B").charAt(0);
		this.viewButton = config.getString("view-icon.symbol", "V").charAt(0);

		this.itemsPerPage = countMemberSlots();
	}

	private int countMemberSlots() {
		int count = 0;
		for (String row : layout) {
			for (char c : row.toCharArray()) {
				if (c == memberSlot)
					count++;
			}
		}
		return count;
	}

	protected abstract List<UUID> getTargetList();

	protected abstract void onBack();

	protected abstract void onAdd();

	protected abstract void onView();

	protected abstract void onRemove(UUID targetUUID);

	protected void addMemberElement(char symbol, UUID uuid) {
		OfflinePlayer offline = plugin.getServer().getOfflinePlayer(uuid);
		String name = offline.getName() != null ? offline.getName()
				: plugin.getTranslationManager().miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key());
		boolean online = offline.isOnline();
		Context<Player> context = Context.player(player);

		Section memberSection = config.getSection("member-icon");
		CustomItem memberItem = new SingleItemParser("member", memberSection,
				plugin.getConfigManager().getItemFormatFunctions()).getItem();
		Item<ItemStack> item = plugin.getItemManager().wrap(memberItem.build(context));

		String displayName = memberSection.getString("display.name", "<aqua>{player}").replace("{player}", name)
				.replace("{login_status}",
						online ? plugin.getPartyGUIManager().onlineStatus : plugin.getPartyGUIManager().offlineStatus);

		item.displayName(AdventureHelper.miniMessageToJson(displayName));

		List<String> loreRaw = memberSection.getStringList(isOwner ? "display.lore" : "display.read-only-lore");
		List<String> lore = new ArrayList<>();
		loreRaw.forEach(line -> lore.add(AdventureHelper.miniMessageToJson(line.replace("{player}", name).replace(
				"{login_status}",
				online ? plugin.getPartyGUIManager().onlineStatus : plugin.getPartyGUIManager().offlineStatus))));
		item.lore(lore);

		if (item.getItem().getType() == Material.PLAYER_HEAD) {
			try {
				GameProfile profile = GameProfileBuilder.fetch(uuid);
				item.skull(profile.getProperties().get("textures").iterator().next().getValue());
			} catch (Exception ignored) {
			}
		}

		if (isOwner) {
			addElement(symbol, new MemberGUIElement(item.loadCopy(), () -> {
				onRemove(uuid);
				plugin.getScheduler().executeSync(this::refresh);
				ActionManager.trigger(context,
						plugin.getActionManager(Player.class).parseActions(memberSection.getSection("action")));
			}));
		} else {
			addElement(symbol, new MemberGUIElement(item.loadCopy())); // No click
		}
	}

	@Override
	public void build() {
		List<UUID> members = getTargetList();
		int from = currentPage * itemsPerPage;
		int to = Math.min(members.size(), from + itemsPerPage);
		int totalPages = (int) Math.ceil((double) members.size() / itemsPerPage);
		boolean hasPrev = currentPage > 0;
		boolean hasNext = currentPage + 1 < totalPages;
		List<UUID> page = from >= members.size() ? Collections.emptyList() : members.subList(from, to);
		Context<Player> context = Context.player(player);

		int index = 0;

		for (String line : layout) {
			for (int col = 0; col < line.length(); col++) {
				char symbol = line.charAt(col);

				if (symbol == memberSlot) {
					if (index < page.size()) {
						UUID uuid = page.get(index++);
						addMemberElement(symbol, uuid);
					} else {
						addElement(symbol, new MemberGUIElement(new ItemStack(Material.AIR)));
					}
					continue;
				}

				if (symbol == leftArrow) {
					if (hasPrev) {
						CustomItem leftItem = new SingleItemParser("left", config.getSection("left-icon"),
								plugin.getConfigManager().getItemFormatFunctions()).getItem();
						addElement(symbol, new MemberGUIElement(leftItem.build(context), () -> {
							currentPage--;
							refresh();
							ActionManager.trigger(context, plugin.getActionManager(Player.class)
									.parseActions(config.getSection("left-icon").getSection("action")));
						}));
					} else {
						setPlaceholderIcon(symbol);
					}
					continue;
				}

				if (symbol == rightArrow) {
					if (hasNext) {
						CustomItem rightItem = new SingleItemParser("right", config.getSection("right-icon"),
								plugin.getConfigManager().getItemFormatFunctions()).getItem();
						addElement(symbol, new MemberGUIElement(rightItem.build(context), () -> {
							currentPage++;
							refresh();
							ActionManager.trigger(context, plugin.getActionManager(Player.class)
									.parseActions(config.getSection("right-icon").getSection("action")));
						}));
					} else {
						setPlaceholderIcon(symbol);
					}
					continue;
				}

				if (symbol == backButton) {
					CustomItem backItem = new SingleItemParser("back", config.getSection("back-icon"),
							plugin.getConfigManager().getItemFormatFunctions()).getItem();
					addElement(symbol, new MemberGUIElement(backItem.build(context), this::onBack));
					continue;
				}

				if (symbol == addButton) {
					if (isOwner) {
						CustomItem addItem = new SingleItemParser("add", config.getSection("add-icon"),
								plugin.getConfigManager().getItemFormatFunctions()).getItem();
						addElement(symbol, new MemberGUIElement(addItem.build(context), this::onAdd));
					} else {
						setPlaceholderIcon(symbol);
					}
					continue;
				}

				if (symbol == viewButton) {
					CustomItem viewItem = new SingleItemParser("view", config.getSection("view-icon"),
							plugin.getConfigManager().getItemFormatFunctions()).getItem();
					addElement(symbol, new MemberGUIElement(viewItem.build(context), this::onView));
					continue;
				}

				// Decorative / Fallback slot
				setPlaceholderIcon(symbol);
			}
		}
	}

	protected void handlePostRemoval() {
		List<UUID> list = getTargetList();

		if (list.isEmpty()) {
			plugin.getScheduler().executeSync(
					() -> plugin.getPartyGUIManager().openPartyGUI(player, islandContext.holder(), isOwner));
			return;
		}

		int totalPages = (int) Math.ceil((double) list.size() / itemsPerPage);
		if (currentPage >= totalPages) {
			currentPage = Math.max(0, totalPages - 1);
		}

		plugin.getScheduler().executeSync(this::refresh);
	}

	private void setPlaceholderIcon(char symbol) {
		Context<Player> context = Context.player(player);
		Pair<CustomItem, Action<Player>[]> deco = plugin.getPartyGUIManager().decorativeIcons.get(symbol);

		// If not found directly, grab any available decorative icon
		if (deco == null && !plugin.getPartyGUIManager().decorativeIcons.isEmpty()) {
			deco = plugin.getPartyGUIManager().decorativeIcons.values().iterator().next(); // First entry
		}

		if (deco != null) {
			addElement(symbol, new MemberGUIElement(deco.left().build(context)));
		} else {
			addElement(symbol, new MemberGUIElement(new ItemStack(Material.AIR)));
		}
	}

	public void open() {
		show();
	}

	public void refresh() {
		clear();
		build();
		open();
	}
}