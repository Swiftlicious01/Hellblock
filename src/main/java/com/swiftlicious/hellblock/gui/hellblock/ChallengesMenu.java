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
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class ChallengesMenu {

	public ChallengesMenu(Player player) {

		Gui gui = Gui.normal().setStructure(" # 1 2 3 4 5 6 ! # ", " # ! ! ! ! ! ! ! x ", " # ! ! ! ! ! ! ! # ")
				.addIngredient('#', new BackGroundItem()).addIngredient('x', new BackToMainMenuItem())
				.addIngredient('!', new ItemStack(Material.AIR))
				.addIngredient('1', new ChallengeOneItem(player.getUniqueId()))
				.addIngredient('2', new ChallengeTwoItem(player.getUniqueId()))
				.addIngredient('3', new ChallengeThreeItem(player.getUniqueId()))
				.addIngredient('4', new ChallengeFourItem(player.getUniqueId()))
				.addIngredient('5', new ChallengeFiveItem(player.getUniqueId()))
				.addIngredient('6', new ChallengeSixItem(player.getUniqueId())).build();

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
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (pi.isChallengeCompleted(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)) {
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
										.getComponentFromMiniMessage(String.format(!pi
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
												pi.getChallengeProgress(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE),
												ChallengeType.NETHERRACK_GENERATOR_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(
														new ProgressBar(
																ChallengeType.NETHERRACK_GENERATOR_CHALLENGE
																		.getNeededAmount(),
																pi.getChallengeProgress(
																		ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (pi.isChallengeCompleted(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)
					&& !pi.isChallengeRewardClaimed(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getConfig("challenge-rewards.yml")
						.getConfigurationSection("rewards." + ChallengeType.NETHERRACK_GENERATOR_CHALLENGE.toString());
				ItemStack reward = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.createChallengeReward(section);
				if (reward != null) {
					if (player.getInventory().firstEmpty() != -1) {
						player.getInventory().addItem(reward);
						player.updateInventory();
						pi.setChallengeRewardAsClaimed(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE, true);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You've claimed your challenge reward!");
						HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
								net.kyori.adventure.sound.Sound.Source.PLAYER,
								net.kyori.adventure.key.Key.key("minecraft:entity.ender_dragon.growl"), 1, 1);
						new ChallengesMenu(player);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Please have an empty slot in your inventory to receive the reward!");
					}
				}
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
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (pi.isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
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
										.getComponentFromMiniMessage(String.format(
												!pi.isChallengeRewardClaimed(ChallengeType.GLOWSTONE_TREE_CHALLENGE)
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
												pi.getChallengeProgress(ChallengeType.GLOWSTONE_TREE_CHALLENGE),
												ChallengeType.GLOWSTONE_TREE_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(
														new ProgressBar(
																ChallengeType.GLOWSTONE_TREE_CHALLENGE
																		.getNeededAmount(),
																pi.getChallengeProgress(
																		ChallengeType.GLOWSTONE_TREE_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (pi.isChallengeCompleted(ChallengeType.GLOWSTONE_TREE_CHALLENGE)
					&& !pi.isChallengeRewardClaimed(ChallengeType.GLOWSTONE_TREE_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getConfig("challenge-rewards.yml")
						.getConfigurationSection("rewards." + ChallengeType.GLOWSTONE_TREE_CHALLENGE.toString());
				ItemStack reward = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.createChallengeReward(section);
				if (reward != null) {
					if (player.getInventory().firstEmpty() != -1) {
						player.getInventory().addItem(reward);
						player.updateInventory();
						pi.setChallengeRewardAsClaimed(ChallengeType.GLOWSTONE_TREE_CHALLENGE, true);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You've claimed your challenge reward!");
						HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
								net.kyori.adventure.sound.Sound.Source.PLAYER,
								net.kyori.adventure.key.Key.key("minecraft:entity.ender_dragon.growl"), 1, 1);
						new ChallengesMenu(player);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Please have an empty slot in your inventory to receive the reward!");
					}
				}
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
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (pi.isChallengeCompleted(ChallengeType.LAVA_FISHING_CHALLENGE)) {
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
										.getComponentFromMiniMessage(String.format(
												!pi.isChallengeRewardClaimed(ChallengeType.LAVA_FISHING_CHALLENGE)
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
												pi.getChallengeProgress(ChallengeType.LAVA_FISHING_CHALLENGE),
												ChallengeType.LAVA_FISHING_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s", ProgressBar.getProgressBar(
												new ProgressBar(ChallengeType.LAVA_FISHING_CHALLENGE.getNeededAmount(),
														pi.getChallengeProgress(ChallengeType.LAVA_FISHING_CHALLENGE)),
												25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (pi.isChallengeCompleted(ChallengeType.LAVA_FISHING_CHALLENGE)
					&& !pi.isChallengeRewardClaimed(ChallengeType.LAVA_FISHING_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getConfig("challenge-rewards.yml")
						.getConfigurationSection("rewards." + ChallengeType.LAVA_FISHING_CHALLENGE.toString());
				ItemStack reward = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.createChallengeReward(section);
				if (reward != null) {
					if (player.getInventory().firstEmpty() != -1) {
						player.getInventory().addItem(reward);
						player.updateInventory();
						pi.setChallengeRewardAsClaimed(ChallengeType.LAVA_FISHING_CHALLENGE, true);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You've claimed your challenge reward!");
						HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
								net.kyori.adventure.sound.Sound.Source.PLAYER,
								net.kyori.adventure.key.Key.key("minecraft:entity.ender_dragon.growl"), 1, 1);
						new ChallengesMenu(player);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Please have an empty slot in your inventory to receive the reward!");
					}
				}
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
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (pi.isChallengeCompleted(ChallengeType.NETHER_CRAFTING_CHALLENGE)) {
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
										.getComponentFromMiniMessage(String.format(
												!pi.isChallengeRewardClaimed(ChallengeType.NETHER_CRAFTING_CHALLENGE)
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
												pi.getChallengeProgress(ChallengeType.NETHER_CRAFTING_CHALLENGE),
												ChallengeType.NETHER_CRAFTING_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(
														new ProgressBar(
																ChallengeType.NETHER_CRAFTING_CHALLENGE
																		.getNeededAmount(),
																pi.getChallengeProgress(
																		ChallengeType.NETHER_CRAFTING_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (pi.isChallengeCompleted(ChallengeType.NETHER_CRAFTING_CHALLENGE)
					&& !pi.isChallengeRewardClaimed(ChallengeType.NETHER_CRAFTING_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getConfig("challenge-rewards.yml")
						.getConfigurationSection("rewards." + ChallengeType.NETHER_CRAFTING_CHALLENGE.toString());
				ItemStack reward = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.createChallengeReward(section);
				if (reward != null) {
					if (player.getInventory().firstEmpty() != -1) {
						player.getInventory().addItem(reward);
						player.updateInventory();
						pi.setChallengeRewardAsClaimed(ChallengeType.NETHER_CRAFTING_CHALLENGE, true);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You've claimed your challenge reward!");
						HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
								net.kyori.adventure.sound.Sound.Source.PLAYER,
								net.kyori.adventure.key.Key.key("minecraft:entity.ender_dragon.growl"), 1, 1);
						new ChallengesMenu(player);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Please have an empty slot in your inventory to receive the reward!");
					}
				}
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
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (pi.isChallengeCompleted(ChallengeType.NETHER_GOLEM_CHALLENGE)) {
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
										.getComponentFromMiniMessage(String.format(
												!pi.isChallengeRewardClaimed(ChallengeType.NETHER_GOLEM_CHALLENGE)
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
												pi.getChallengeProgress(ChallengeType.NETHER_GOLEM_CHALLENGE),
												ChallengeType.NETHER_GOLEM_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s", ProgressBar.getProgressBar(
												new ProgressBar(ChallengeType.NETHER_GOLEM_CHALLENGE.getNeededAmount(),
														pi.getChallengeProgress(ChallengeType.NETHER_GOLEM_CHALLENGE)),
												25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (pi.isChallengeCompleted(ChallengeType.NETHER_GOLEM_CHALLENGE)
					&& !pi.isChallengeRewardClaimed(ChallengeType.NETHER_GOLEM_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getConfig("challenge-rewards.yml")
						.getConfigurationSection("rewards." + ChallengeType.NETHER_GOLEM_CHALLENGE.toString());
				ItemStack reward = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.createChallengeReward(section);
				if (reward != null) {
					if (player.getInventory().firstEmpty() != -1) {
						player.getInventory().addItem(reward);
						player.updateInventory();
						pi.setChallengeRewardAsClaimed(ChallengeType.NETHER_GOLEM_CHALLENGE, true);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You've claimed your challenge reward!");
						HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
								net.kyori.adventure.sound.Sound.Source.PLAYER,
								net.kyori.adventure.key.Key.key("minecraft:entity.ender_dragon.growl"), 1, 1);
						new ChallengesMenu(player);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Please have an empty slot in your inventory to receive the reward!");
					}
				}
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
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID);
			if (pi.isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)) {
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
										.getComponentFromMiniMessage(String.format(
												!pi.isChallengeRewardClaimed(ChallengeType.NETHER_BREWING_CHALLENGE)
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
												pi.getChallengeProgress(ChallengeType.NETHER_BREWING_CHALLENGE),
												ChallengeType.NETHER_BREWING_CHALLENGE.getNeededAmount()))),
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("%s",
												ProgressBar.getProgressBar(
														new ProgressBar(
																ChallengeType.NETHER_BREWING_CHALLENGE
																		.getNeededAmount(),
																pi.getChallengeProgress(
																		ChallengeType.NETHER_BREWING_CHALLENGE)),
														25)))));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			if (pi.isChallengeCompleted(ChallengeType.NETHER_BREWING_CHALLENGE)
					&& !pi.isChallengeRewardClaimed(ChallengeType.NETHER_BREWING_CHALLENGE)) {
				ConfigurationSection section = HellblockPlugin.getInstance().getConfig("challenge-rewards.yml")
						.getConfigurationSection("rewards." + ChallengeType.NETHER_BREWING_CHALLENGE.toString());
				ItemStack reward = HellblockPlugin.getInstance().getChallengeRewardBuilder()
						.createChallengeReward(section);
				if (reward != null) {
					if (player.getInventory().firstEmpty() != -1) {
						player.getInventory().addItem(reward);
						player.updateInventory();
						pi.setChallengeRewardAsClaimed(ChallengeType.NETHER_BREWING_CHALLENGE, true);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You've claimed your challenge reward!");
						HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
								net.kyori.adventure.sound.Sound.Source.PLAYER,
								net.kyori.adventure.key.Key.key("minecraft:entity.ender_dragon.growl"), 1, 1);
						new ChallengesMenu(player);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>Please have an empty slot in your inventory to receive the reward!");
					}
				}
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
