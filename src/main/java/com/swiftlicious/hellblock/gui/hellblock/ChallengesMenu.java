package com.swiftlicious.hellblock.gui.hellblock;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.challenges.ProgressBar;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.player.OnlineUser;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class ChallengesMenu {

	public ChallengesMenu(Player player) {

		OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser == null) {
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>Still loading your player data... please try again in a few seconds.");
			return;
		}

		Gui gui = Gui.normal().setStructure(" # 1 2 3 4 5 6 7 # ", " # # # 8 9 0 # # x ")
				.addIngredient('#', new BackGroundItem()).addIngredient('x', new BackToMainMenuItem())
				.addIngredient('!', new ItemStack(Material.AIR))
				.addIngredient('1', new ChallengeOneItem(player.getUniqueId()))
				.addIngredient('2', new ChallengeTwoItem(player.getUniqueId()))
				.addIngredient('3', new ChallengeThreeItem(player.getUniqueId()))
				.addIngredient('4', new ChallengeFourItem(player.getUniqueId()))
				.addIngredient('5', new ChallengeFiveItem(player.getUniqueId()))
				.addIngredient('6', new ChallengeSixItem(player.getUniqueId()))
				.addIngredient('7', new ChallengeSevenItem(player.getUniqueId()))
				.addIngredient('8', new ChallengeEightItem(player.getUniqueId()))
				.addIngredient('9', new ChallengeNineItem(player.getUniqueId()))
				.addIngredient('0', new ChallengeTenItem(player.getUniqueId())).build();

		Window window = Window
				.single().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage("<red>Hellblock Challenges")))
				.setGui(gui).build();

		window.open();
	}

	public class ChallengeOneItem extends AbstractItem {

		private UUID playerUUID;

		public ChallengeOneItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)) {
				return new ItemBuilder(Material.GOLDEN_PICKAXE).addAllItemFlags()
						.addEnchantment(Enchantment.UNBREAKING, 1, false)
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<green>Break %s generated blocks!",
												ChallengeType.NETHERRACK_GENERATOR_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage(String.format(
														"<dark_green>Completed!: <gray>(%s/%s)",
														ChallengeType.NETHERRACK_GENERATOR_CHALLENGE.getNeededAmount(),
														ChallengeType.NETHERRACK_GENERATOR_CHALLENGE
																.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(" "))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(!onlineUser.getHellblockData()
												.isChallengeRewardClaimed(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)
														? "<yellow>Click to claim your reward!"
														: "<yellow>Reward Claimed!"))));
			} else {
				return new ItemBuilder(Material.GOLDEN_PICKAXE).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<red>Break %s generated blocks!",
												ChallengeType.NETHERRACK_GENERATOR_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<dark_red>Progress: <gray>(%s/%s)",
												onlineUser.getHellblockData().getChallengeProgress(
														ChallengeType.NETHERRACK_GENERATOR_CHALLENGE),
												ChallengeType.NETHERRACK_GENERATOR_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(
														new ProgressBar(
																ChallengeType.NETHERRACK_GENERATOR_CHALLENGE
																		.getNeededAmount(),
																onlineUser.getHellblockData().getChallengeProgress(
																		ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)
					&& !onlineUser.getHellblockData()
							.isChallengeRewardClaimed(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.getRewardConfig()
						.getConfigurationSection("rewards." + ChallengeType.NETHERRACK_GENERATOR_CHALLENGE.toString());
				HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeRewardAction(player, section,
						ChallengeType.NETHERRACK_GENERATOR_CHALLENGE);
			}
		}
	}

	public class ChallengeTwoItem extends AbstractItem {

		private UUID playerUUID;

		public ChallengeTwoItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
				return new ItemBuilder(Material.GLOWSTONE).addAllItemFlags()
						.addEnchantment(Enchantment.UNBREAKING, 1, false)
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<green>Grow %s glowstone trees!",
												ChallengeType.GLOWSTONE_TREE_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage(String.format(
														"<dark_green>Completed!: <gray>(%s/%s)",
														ChallengeType.GLOWSTONE_TREE_CHALLENGE.getNeededAmount(),
														ChallengeType.GLOWSTONE_TREE_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(" "))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(!onlineUser.getHellblockData()
												.isChallengeRewardClaimed(ChallengeType.GLOWSTONE_TREE_CHALLENGE)
														? "<yellow>Click to claim your reward!"
														: "<yellow>Reward Claimed!"))));
			} else {
				return new ItemBuilder(Material.GLOWSTONE).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<red>Grow %s glowstone trees!",
												ChallengeType.GLOWSTONE_TREE_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<dark_red>Progress: <gray>(%s/%s)",
												onlineUser.getHellblockData()
														.getChallengeProgress(ChallengeType.GLOWSTONE_TREE_CHALLENGE),
												ChallengeType.GLOWSTONE_TREE_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(
														new ProgressBar(
																ChallengeType.GLOWSTONE_TREE_CHALLENGE
																		.getNeededAmount(),
																onlineUser.getHellblockData().getChallengeProgress(
																		ChallengeType.GLOWSTONE_TREE_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)
					&& !onlineUser.getHellblockData()
							.isChallengeRewardClaimed(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.getRewardConfig()
						.getConfigurationSection("rewards." + ChallengeType.GLOWSTONE_TREE_CHALLENGE.toString());
				HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeRewardAction(player, section,
						ChallengeType.GLOWSTONE_TREE_CHALLENGE);
			}
		}
	}

	public class ChallengeThreeItem extends AbstractItem {

		private UUID playerUUID;

		public ChallengeThreeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.LAVA_FISHING_CHALLENGE)) {
				return new ItemBuilder(Material.FISHING_ROD).addAllItemFlags()
						.addEnchantment(Enchantment.UNBREAKING, 1, false)
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<green>Catch %s items in lava!",
												ChallengeType.LAVA_FISHING_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												String.format("<dark_green>Completed!: <gray>(%s/%s)",
														ChallengeType.LAVA_FISHING_CHALLENGE.getNeededAmount(),
														ChallengeType.LAVA_FISHING_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(" "))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(!onlineUser.getHellblockData()
												.isChallengeRewardClaimed(ChallengeType.LAVA_FISHING_CHALLENGE)
														? "<yellow>Click to claim your reward!"
														: "<yellow>Reward Claimed!"))));
			} else {
				return new ItemBuilder(Material.FISHING_ROD).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<red>Catch %s items in lava!",
												ChallengeType.LAVA_FISHING_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<dark_red>Progress: <gray>(%s/%s)",
												onlineUser.getHellblockData()
														.getChallengeProgress(ChallengeType.LAVA_FISHING_CHALLENGE),
												ChallengeType.LAVA_FISHING_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(new ProgressBar(
														ChallengeType.LAVA_FISHING_CHALLENGE.getNeededAmount(),
														onlineUser.getHellblockData().getChallengeProgress(
																ChallengeType.LAVA_FISHING_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.LAVA_FISHING_CHALLENGE)
					&& !onlineUser.getHellblockData().isChallengeRewardClaimed(ChallengeType.LAVA_FISHING_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.getRewardConfig()
						.getConfigurationSection("rewards." + ChallengeType.LAVA_FISHING_CHALLENGE.toString());
				HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeRewardAction(player, section,
						ChallengeType.LAVA_FISHING_CHALLENGE);
			}
		}
	}

	public class ChallengeFourItem extends AbstractItem {

		private UUID playerUUID;

		public ChallengeFourItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHER_CRAFTING_CHALLENGE)) {
				return new ItemBuilder(Material.CRAFTING_TABLE).addAllItemFlags()
						.addEnchantment(Enchantment.UNBREAKING, 1, false)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<green>Craft %s unique nether recipes!",
										ChallengeType.NETHER_CRAFTING_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage(String.format(
														"<dark_green>Completed!: <gray>(%s/%s)",
														ChallengeType.NETHER_CRAFTING_CHALLENGE.getNeededAmount(),
														ChallengeType.NETHER_CRAFTING_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(" "))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(!onlineUser.getHellblockData()
												.isChallengeRewardClaimed(ChallengeType.NETHER_CRAFTING_CHALLENGE)
														? "<yellow>Click to claim your reward!"
														: "<yellow>Reward Claimed!"))));
			} else {
				return new ItemBuilder(Material.CRAFTING_TABLE).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<red>Craft %s unique nether recipes!",
										ChallengeType.NETHER_CRAFTING_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<dark_red>Progress: <gray>(%s/%s)",
												onlineUser.getHellblockData()
														.getChallengeProgress(ChallengeType.NETHER_CRAFTING_CHALLENGE),
												ChallengeType.NETHER_CRAFTING_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(
														new ProgressBar(
																ChallengeType.NETHER_CRAFTING_CHALLENGE
																		.getNeededAmount(),
																onlineUser.getHellblockData().getChallengeProgress(
																		ChallengeType.NETHER_CRAFTING_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHER_CRAFTING_CHALLENGE)
					&& !onlineUser.getHellblockData()
							.isChallengeRewardClaimed(ChallengeType.NETHER_CRAFTING_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.getRewardConfig()
						.getConfigurationSection("rewards." + ChallengeType.NETHER_CRAFTING_CHALLENGE.toString());
				HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeRewardAction(player, section,
						ChallengeType.NETHER_CRAFTING_CHALLENGE);
			}
		}
	}

	public class ChallengeFiveItem extends AbstractItem {

		private UUID playerUUID;

		public ChallengeFiveItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHER_GOLEM_CHALLENGE)) {
				return new ItemBuilder(Material.SNOW_GOLEM_SPAWN_EGG).addAllItemFlags()
						.addEnchantment(Enchantment.UNBREAKING, 1, false)
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<green>Build %s nether snow golems!",
										ChallengeType.NETHER_GOLEM_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												String.format("<dark_green>Completed!: <gray>(%s/%s)",
														ChallengeType.NETHER_GOLEM_CHALLENGE.getNeededAmount(),
														ChallengeType.NETHER_GOLEM_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(" "))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(!onlineUser.getHellblockData()
												.isChallengeRewardClaimed(ChallengeType.NETHER_GOLEM_CHALLENGE)
														? "<yellow>Click to claim your reward!"
														: "<yellow>Reward Claimed!"))));
			} else {
				return new ItemBuilder(Material.SNOW_GOLEM_SPAWN_EGG).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<red>Build %s nether snow golems!",
												ChallengeType.NETHER_GOLEM_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<dark_red>Progress: <gray>(%s/%s)",
												onlineUser.getHellblockData()
														.getChallengeProgress(ChallengeType.NETHER_GOLEM_CHALLENGE),
												ChallengeType.NETHER_GOLEM_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(new ProgressBar(
														ChallengeType.NETHER_GOLEM_CHALLENGE.getNeededAmount(),
														onlineUser.getHellblockData().getChallengeProgress(
																ChallengeType.NETHER_GOLEM_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHER_GOLEM_CHALLENGE)
					&& !onlineUser.getHellblockData().isChallengeRewardClaimed(ChallengeType.NETHER_GOLEM_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.getRewardConfig()
						.getConfigurationSection("rewards." + ChallengeType.NETHER_GOLEM_CHALLENGE.toString());
				HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeRewardAction(player, section,
						ChallengeType.NETHER_GOLEM_CHALLENGE);
			}
		}
	}

	public class ChallengeSixItem extends AbstractItem {

		private UUID playerUUID;

		public ChallengeSixItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)) {
				return new ItemBuilder(Material.GLASS_BOTTLE).addAllItemFlags()
						.addEnchantment(Enchantment.UNBREAKING, 1, false)
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(
												"<green>Use glass bottles on lava %s times to get lava potions!",
												ChallengeType.NETHER_BREWING_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage(String.format(
														"<dark_green>Completed!: <gray>(%s/%s)",
														ChallengeType.NETHER_BREWING_CHALLENGE.getNeededAmount(),
														ChallengeType.NETHER_BREWING_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(" "))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(!onlineUser.getHellblockData()
												.isChallengeRewardClaimed(ChallengeType.NETHER_BREWING_CHALLENGE)
														? "<yellow>Click to claim your reward!"
														: "<yellow>Reward Claimed!"))));
			} else {
				return new ItemBuilder(Material.GLASS_BOTTLE).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(
												"<red>Use glass bottles on lava %s times to get lava potions!",
												ChallengeType.NETHER_BREWING_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<dark_red>Progress: <gray>(%s/%s)",
												onlineUser.getHellblockData()
														.getChallengeProgress(ChallengeType.NETHER_BREWING_CHALLENGE),
												ChallengeType.NETHER_BREWING_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(
														new ProgressBar(
																ChallengeType.NETHER_BREWING_CHALLENGE
																		.getNeededAmount(),
																onlineUser.getHellblockData().getChallengeProgress(
																		ChallengeType.NETHER_BREWING_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)
					&& !onlineUser.getHellblockData()
							.isChallengeRewardClaimed(ChallengeType.NETHER_BREWING_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.getRewardConfig()
						.getConfigurationSection("rewards." + ChallengeType.NETHER_BREWING_CHALLENGE.toString());
				HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeRewardAction(player, section,
						ChallengeType.NETHER_BREWING_CHALLENGE);
			}
		}
	}

	public class ChallengeSevenItem extends AbstractItem {

		private UUID playerUUID;

		public ChallengeSevenItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHER_TRADING_CHALLENGE)) {
				return new ItemBuilder(Material.GOLD_INGOT).addAllItemFlags()
						.addEnchantment(Enchantment.UNBREAKING, 1, false)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<green>Barter for the %s items that piglins trade for!",
												ChallengeType.NETHER_TRADING_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage(String.format(
														"<dark_green>Completed!: <gray>(%s/%s)",
														ChallengeType.NETHER_TRADING_CHALLENGE.getNeededAmount(),
														ChallengeType.NETHER_TRADING_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(" "))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(!onlineUser.getHellblockData()
												.isChallengeRewardClaimed(ChallengeType.NETHER_TRADING_CHALLENGE)
														? "<yellow>Click to claim your reward!"
														: "<yellow>Reward Claimed!"))));
			} else {
				return new ItemBuilder(Material.GOLD_INGOT).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<red>Barter for the %s items that piglins trade for!",
												ChallengeType.NETHER_TRADING_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<dark_red>Progress: <gray>(%s/%s)",
												onlineUser.getHellblockData()
														.getChallengeProgress(ChallengeType.NETHER_TRADING_CHALLENGE),
												ChallengeType.NETHER_TRADING_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(
														new ProgressBar(
																ChallengeType.NETHER_TRADING_CHALLENGE
																		.getNeededAmount(),
																onlineUser.getHellblockData().getChallengeProgress(
																		ChallengeType.NETHER_TRADING_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHER_TRADING_CHALLENGE)
					&& !onlineUser.getHellblockData()
							.isChallengeRewardClaimed(ChallengeType.NETHER_TRADING_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.getRewardConfig()
						.getConfigurationSection("rewards." + ChallengeType.NETHER_TRADING_CHALLENGE.toString());
				HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeRewardAction(player, section,
						ChallengeType.NETHER_TRADING_CHALLENGE);
			}
		}
	}

	public class ChallengeEightItem extends AbstractItem {

		private UUID playerUUID;

		public ChallengeEightItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.ENHANCED_WITHER_CHALLENGE)) {
				return new ItemBuilder(Material.NETHER_STAR).addAllItemFlags()
						.addEnchantment(Enchantment.UNBREAKING, 1, false)
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<green>Slay %s enhanced withers!",
												ChallengeType.ENHANCED_WITHER_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage(String.format(
														"<dark_green>Completed!: <gray>(%s/%s)",
														ChallengeType.ENHANCED_WITHER_CHALLENGE.getNeededAmount(),
														ChallengeType.ENHANCED_WITHER_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(" "))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(!onlineUser.getHellblockData()
												.isChallengeRewardClaimed(ChallengeType.ENHANCED_WITHER_CHALLENGE)
														? "<yellow>Click to claim your reward!"
														: "<yellow>Reward Claimed!"))));
			} else {
				return new ItemBuilder(Material.NETHER_STAR).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<red>Slay %s enhanced withers!",
												ChallengeType.ENHANCED_WITHER_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<dark_red>Progress: <gray>(%s/%s)",
												onlineUser.getHellblockData()
														.getChallengeProgress(ChallengeType.ENHANCED_WITHER_CHALLENGE),
												ChallengeType.ENHANCED_WITHER_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(
														new ProgressBar(
																ChallengeType.ENHANCED_WITHER_CHALLENGE
																		.getNeededAmount(),
																onlineUser.getHellblockData().getChallengeProgress(
																		ChallengeType.ENHANCED_WITHER_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.ENHANCED_WITHER_CHALLENGE)
					&& !onlineUser.getHellblockData()
							.isChallengeRewardClaimed(ChallengeType.ENHANCED_WITHER_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.getRewardConfig()
						.getConfigurationSection("rewards." + ChallengeType.ENHANCED_WITHER_CHALLENGE.toString());
				HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeRewardAction(player, section,
						ChallengeType.ENHANCED_WITHER_CHALLENGE);
			}
		}
	}

	public class ChallengeNineItem extends AbstractItem {

		private UUID playerUUID;

		public ChallengeNineItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.INFINITE_LAVA_CHALLENGE)) {
				return new ItemBuilder(Material.LAVA_BUCKET).addAllItemFlags()
						.addEnchantment(Enchantment.UNBREAKING, 1, false)
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(
												"<green>Gather lava from an infinite lava source %s times!",
												ChallengeType.INFINITE_LAVA_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												String.format("<dark_green>Completed!: <gray>(%s/%s)",
														ChallengeType.INFINITE_LAVA_CHALLENGE.getNeededAmount(),
														ChallengeType.INFINITE_LAVA_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(" "))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(!onlineUser.getHellblockData()
												.isChallengeRewardClaimed(ChallengeType.INFINITE_LAVA_CHALLENGE)
														? "<yellow>Click to claim your reward!"
														: "<yellow>Reward Claimed!"))));
			} else {
				return new ItemBuilder(Material.LAVA_BUCKET).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<red>Gather lava from an infinite lava source %s times!",
												ChallengeType.INFINITE_LAVA_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<dark_red>Progress: <gray>(%s/%s)",
												onlineUser.getHellblockData()
														.getChallengeProgress(ChallengeType.INFINITE_LAVA_CHALLENGE),
												ChallengeType.INFINITE_LAVA_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(
														new ProgressBar(
																ChallengeType.INFINITE_LAVA_CHALLENGE.getNeededAmount(),
																onlineUser.getHellblockData().getChallengeProgress(
																		ChallengeType.INFINITE_LAVA_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.INFINITE_LAVA_CHALLENGE)
					&& !onlineUser.getHellblockData().isChallengeRewardClaimed(ChallengeType.INFINITE_LAVA_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.getRewardConfig()
						.getConfigurationSection("rewards." + ChallengeType.INFINITE_LAVA_CHALLENGE.toString());
				HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeRewardAction(player, section,
						ChallengeType.INFINITE_LAVA_CHALLENGE);
			}
		}
	}

	public class ChallengeTenItem extends AbstractItem {

		private UUID playerUUID;

		public ChallengeTenItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHER_FARM_CHALLENGE)) {
				return new ItemBuilder(Material.NETHERITE_HOE).addAllItemFlags()
						.addEnchantment(Enchantment.UNBREAKING, 1, false)
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(
												"<green>Create a nether farm using lava that consists of %s farmland!",
												ChallengeType.NETHER_FARM_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
												String.format("<dark_green>Completed!: <gray>(%s/%s)",
														ChallengeType.NETHER_FARM_CHALLENGE.getNeededAmount(),
														ChallengeType.NETHER_FARM_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(" "))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(!onlineUser.getHellblockData()
												.isChallengeRewardClaimed(ChallengeType.NETHER_FARM_CHALLENGE)
														? "<yellow>Click to claim your reward!"
														: "<yellow>Reward Claimed!"))));
			} else {
				return new ItemBuilder(Material.NETHERITE_HOE).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format(
												"<red>Create a nether farm using lava that consists of %s farmland!",
												ChallengeType.NETHER_FARM_CHALLENGE.getNeededAmount()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<dark_red>Progress: <gray>(%s/%s)",
												onlineUser.getHellblockData()
														.getChallengeProgress(ChallengeType.NETHER_FARM_CHALLENGE),
												ChallengeType.NETHER_FARM_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(new ProgressBar(
														ChallengeType.NETHER_FARM_CHALLENGE.getNeededAmount(),
														onlineUser.getHellblockData().getChallengeProgress(
																ChallengeType.NETHER_FARM_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			OnlineUser onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (onlineUser.getHellblockData().isChallengeCompleted(ChallengeType.NETHER_FARM_CHALLENGE)
					&& !onlineUser.getHellblockData().isChallengeRewardClaimed(ChallengeType.NETHER_FARM_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.getRewardConfig()
						.getConfigurationSection("rewards." + ChallengeType.NETHER_FARM_CHALLENGE.toString());
				HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeRewardAction(player, section,
						ChallengeType.NETHER_FARM_CHALLENGE);
			}
		}
	}

	public class BackToMainMenuItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).addAllItemFlags()
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<gold>Return to Hellblock Menu")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			new HellblockMenu(player);
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}
}
