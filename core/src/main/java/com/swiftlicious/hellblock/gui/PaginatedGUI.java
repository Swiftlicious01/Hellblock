package com.swiftlicious.hellblock.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.utils.extras.Action;

public abstract class PaginatedGUI<T extends BaseGUIElement> {

	protected final Map<Integer, T> itemsSlotMap = new HashMap<>();
	protected final Map<Character, T> itemsCharMap = new HashMap<>();
	protected final List<List<T>> pages = new ArrayList<>();

	protected int currentPage = 0;
	protected int maxPage = 0;

	protected final CustomItem leftIconItem;
	protected final CustomItem rightIconItem;
	protected final Action<Player>[] leftActions;
	protected final Action<Player>[] rightActions;

	protected Inventory inventory;
	protected Player player;

	@SuppressWarnings("unchecked")
	public PaginatedGUI(@Nullable CustomItem leftIconItem, @Nullable Action<Player>[] leftActions,
			@Nullable CustomItem rightIconItem, @Nullable Action<Player>[] rightActions) {
		this.leftIconItem = leftIconItem;
		this.leftActions = (leftActions != null) ? leftActions : (Action<Player>[]) new Action<?>[0];
		this.rightIconItem = rightIconItem;
		this.rightActions = (rightActions != null) ? rightActions : (Action<Player>[]) new Action<?>[0];
	}

	public void setPages(List<List<T>> pages) {
		this.pages.clear();
		this.pages.addAll(pages);
		this.maxPage = Math.max(0, pages.size() - 1);
	}

	public void openPage(int index) {
		if (index < 0 || index > maxPage)
			return;
		this.currentPage = index;
		refreshPage();
	}

	public void nextPage() {
		if (currentPage < maxPage) {
			openPage(currentPage + 1);
			Context<Player> context = Context.player(player);
			ActionManager.trigger(context, rightActions);
		}
	}

	public void previousPage() {
		if (currentPage > 0) {
			openPage(currentPage - 1);
			Context<Player> context = Context.player(player);
			ActionManager.trigger(context, leftActions);
		}
	}

	protected void refreshPage() {
		if (inventory == null || pages.isEmpty())
			return;

		// clear full inventory before redraw
		inventory.clear();

		List<T> elements = pages.get(currentPage);
		// Redraw the current page elements
		for (T element : elements) {
			for (int slot : element.getSlots()) {
				inventory.setItem(slot, element.getItemStack());
			}
		}

		// Add navigation icons
		Context<Player> context = Context.player(player);
		int leftSlot = getLeftIconSlot();
		if (leftIconItem != null && currentPage > 0 && leftSlot >= 0) {
			inventory.setItem(leftSlot, leftIconItem.build(context));
		}

		int rightSlot = getRightIconSlot();
		if (rightIconItem != null && currentPage < maxPage && rightSlot >= 0) {
			inventory.setItem(rightSlot, rightIconItem.build(context));
		}
	}

	protected abstract int getLeftIconSlot();

	protected abstract int getRightIconSlot();

	public int getCurrentPage() {
		return currentPage;
	}

	public int getPageCount() {
		return pages.size();
	}
}