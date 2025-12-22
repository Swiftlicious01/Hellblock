package com.swiftlicious.hellblock.gui.party;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Key;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class TrustedMembersGUI extends BasePaginatedMemberGUI {

	public TrustedMembersGUI(HellblockPlugin plugin, Player player, Context<Integer> islandContext, Section config,
			UserData data, boolean isOwner) {
		super(plugin, player, islandContext, config, data, isOwner);
	}

	@Override
	protected List<UUID> getTargetList() {
		return new ArrayList<>(data.getHellblockData().getTrustedMembers());
	}

	@Override
	protected void onBack() {
		plugin.getScheduler()
				.executeSync(() -> plugin.getPartyGUIManager().openPartyGUI(player, islandContext.holder(), isOwner));
		ActionManager.trigger(Context.player(player), plugin.getActionManager(Player.class)
				.parseActions(config.getSection("back-icon").getSection("action")));
	}

	@Override
	protected void onAdd() {
		plugin.getScheduler()
				.executeSync(() -> new AnvilTrustInputGUI(plugin, player, islandContext, data, isOwner).open());
		ActionManager.trigger(Context.player(player),
				plugin.getActionManager(Player.class).parseActions(config.getSection("add-icon").getSection("action")));
	}

	@Override
	protected void onView() {
		UUID playerUUID = player.getUniqueId();

		plugin.getCoopManager().getCachedIslandOwnerData().thenAcceptAsync(allUserData -> {
			List<String> trustedOnIslands = allUserData.stream().filter(userData -> {
				HellblockData islandData = userData.getHellblockData();
				return userData.getHellblockData().getOwnerUUID() != null
						&& islandData.getTrustedMembers().contains(playerUUID);
			}).map(userData -> {
				OfflinePlayer owner = Bukkit.getOfflinePlayer(userData.getHellblockData().getOwnerUUID());
				return (owner.hasPlayedBefore() && owner.getName() != null) ? owner.getName()
						: plugin.getTranslationManager()
								.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key());
			}).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());

			if (trustedOnIslands.isEmpty()) {
				trustedOnIslands = List.of(this.config.getSection("book-settings").getString("no-trusted-found",
						"You're not trusted on any islands!"));
			}

			List<String> pages = paginateText(trustedOnIslands, 14);
			Item<ItemStack> book = buildBookItem(pages);

			plugin.getScheduler().executeSync(() -> plugin.getPartyGUIManager().openCustomBook(player, book,
					() -> plugin.getScheduler().executeSync(this::open)));
		});
	}

	@Override
	protected CompletableFuture<Boolean> onRemove(UUID targetUUID) {
		final String input = Bukkit.getOfflinePlayer(targetUUID).getName();
		final String displayName = input != null ? input
				: plugin.getTranslationManager().miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key());

		return plugin.getCoopManager().removeTrustAccess(data, displayName, targetUUID).thenApply(success -> {
			handlePostRemoval();
			return success;
		}).exceptionally(ex -> {
			plugin.getPluginLogger().warn("removeTrustAccess failed for " + displayName, ex);
			return false;
		});
	}

	private Item<ItemStack> buildBookItem(List<String> pages) {
		ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
		Item<ItemStack> book = plugin.getItemManager().wrap(bookItem);

		book.setTag(Key.of("minecraft:written_book_content"),
				Map.of("title", "<dark_green>Trusted Islands</dark_green>", "author", player.getName(), "pages",
						pages.stream().map(page -> Map.of("text", page)).toList()));

		book.setTag(Key.of("minecraft:hide_additional_tooltip"), Map.of());

		return book;
	}

	private List<String> paginateText(List<String> lines, int linesPerPage) {
		List<String> pages = new ArrayList<>();
		StringBuilder pageBuilder = new StringBuilder();
		int count = 0;

		for (String line : lines) {
			pageBuilder.append(line).append("\n");
			count++;
			if (count >= linesPerPage) {
				pages.add(pageBuilder.toString().trim());
				pageBuilder.setLength(0);
				count = 0;
			}
		}

		if (pageBuilder.length() > 0) {
			pages.add(pageBuilder.toString().trim());
		}

		return pages;
	}
}