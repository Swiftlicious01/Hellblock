package com.swiftlicious.hellblock.gui.hellblock;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class HellblockMenu {

	public HellblockMenu(Player player) {

		Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
				.getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>Still loading your player data... please try again in a few seconds.");
			return;
		}
		if (onlineUser.get().getHellblockData().hasHellblock()) {
			if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
				throw new NullPointerException("Owner reference returned null, please report this to the developer.");
			}
			HellblockPlugin.getInstance().getStorageManager()
					.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(), HBConfig.lockData)
					.thenAccept((result) -> {
						UserData offlineUser = result.orElseThrow();
						if (offlineUser.getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
							return;
						}
					}).thenRun(() -> {
						Gui gui;
						if (onlineUser.get().getHellblockData().getOwnerUUID() != null
								&& onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							gui = Gui.normal().setStructure("t i c p b l f r x")
									.addIngredient('i', new IslandLevelItem(player.getUniqueId()))
									.addIngredient('t', new TeleportIslandItem())
									.addIngredient('p', new ViewPartyMembersItem())
									.addIngredient('c', new IslandChallengesItem())
									.addIngredient('b', new BiomeItem(player.getUniqueId()))
									.addIngredient('l', new LockIslandItem(player.getUniqueId()))
									.addIngredient('r', new ResetIslandItem(player.getUniqueId()))
									.addIngredient('f', new ProtectionFlagItem())
									.addIngredient('x', new CloseMenuItem()).build();
						} else {
							gui = Gui.normal().setStructure("t i c p x")
									.addIngredient('i', new IslandLevelItem(player.getUniqueId()))
									.addIngredient('t', new TeleportIslandItem())
									.addIngredient('p', new ViewPartyMembersItem())
									.addIngredient('c', new IslandChallengesItem())
									.addIngredient('x', new CloseMenuItem()).build();
						}
						Window window = Window.single().setViewer(player)
								.setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
										.getAdventureManager().getComponentFromMiniMessage("<red>Hellblock Menu")))
								.setGui(gui).build();

						window.open();
					});
		} else {
			new IslandChoiceMenu(player, false);
		}
	}

	public class ProtectionFlagItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.REPEATER).addAllItemFlags()
					.setDisplayName(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage("<green>Change your Hellblock Protection Flags!")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<aqua>Click to change your hellblock protection flags!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
						"<red>Still loading your player data... please try again in a few seconds.");
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (onlineUser.get().getHellblockData().hasHellblock()) {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				if (onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							HBLocale.MSG_Not_Owner_Of_Hellblock);
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
					return;
				}
				if (onlineUser.get().getHellblockData().isAbandoned()) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							HBLocale.MSG_Hellblock_Is_Abandoned);
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
					return;
				}
				new FlagMenu(player);
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						HBLocale.MSG_Hellblock_Not_Found);
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
			}
		}
	}

	public class IslandChallengesItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.KNOWLEDGE_BOOK).addAllItemFlags()
					.setDisplayName(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage("<green>View all Hellblock Challenges!")))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<aqua>Click to view a list of challenges for you to complete!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
						"<red>Still loading your player data... please try again in a few seconds.");
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			new ChallengesMenu(player);
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class IslandLevelItem extends AbstractItem {

		private UUID playerUUID;

		public IslandLevelItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
				throw new NullPointerException("Owner reference returned null, please report this to the developer.");
			}
			ItemBuilder itemBuilder = new ItemBuilder(Material.BARRIER)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<red>Failed to Load")));
			HellblockPlugin.getInstance().getStorageManager()
					.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(), HBConfig.lockData)
					.thenAccept((result) -> {
						UserData ownerUser = result.orElseThrow();
						itemBuilder.setMaterial(Material.EXPERIENCE_BOTTLE).addAllItemFlags()
								.setDisplayName(new ShadedAdventureComponentWrapper(
										HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage("<green>View your island level!")))
								.addLoreLines(
										new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
												.getAdventureManager()
												.getComponentFromMiniMessage(String.format("<gold>Level: <yellow>%s",
														ownerUser.getHellblockData().hasHellblock()
																? ownerUser.getHellblockData().getLevel()
																: 0))),
										new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
												.getAdventureManager().getComponentFromMiniMessage(" ")),
										new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
												.getAdventureManager()
												.getComponentFromMiniMessage(String.format(
														"<gold>Your overall rank is <yellow>%s",
														(HellblockPlugin.getInstance().getIslandLevelManager()
																.getLevelRank(ownerUser.getUUID()) > 0
																		? "#" + HellblockPlugin.getInstance()
																				.getIslandLevelManager()
																				.getLevelRank(ownerUser.getUUID())
																		: "Unranked")))));
					}).join();
			return itemBuilder;
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
		}
	}

	public class LockIslandItem extends AbstractItem {

		private UUID playerUUID;

		public LockIslandItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID).get()
					.getHellblockData().isLocked() ? Material.ENDER_CHEST : Material.CHEST)
					.addAllItemFlags()
					.setDisplayName(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage(String.format("<green>%s your Hellblock!",
											(HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID)
													.get().getHellblockData().isLocked() ? "Unlock" : "Lock")))))
					.addLoreLines(new ShadedAdventureComponentWrapper(
							HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
									"<aqua>Click to change the visitor status of your hellblock!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
						"<red>Still loading your player data... please try again in a few seconds.");
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (onlineUser.get().getHellblockData().hasHellblock()) {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				if (onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							HBLocale.MSG_Not_Owner_Of_Hellblock);
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
					return;
				}
				if (onlineUser.get().getHellblockData().isAbandoned()) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							HBLocale.MSG_Hellblock_Is_Abandoned);
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
					return;
				}
				onlineUser.get().getHellblockData().setLockedStatus(!onlineUser.get().getHellblockData().isLocked());
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You've just <dark_red>%s <red>your hellblock island!",
								(onlineUser.get().getHellblockData().isLocked() ? "locked" : "unlocked")));
				if (onlineUser.get().getHellblockData().isLocked()) {
					HellblockPlugin.getInstance().getCoopManager().kickVisitorsIfLocked(player.getUniqueId());
					HellblockPlugin.getInstance().getCoopManager().changeLockStatus(onlineUser.get());
				}
				new HellblockMenu(player);
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						HBLocale.MSG_Hellblock_Not_Found);
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
			}
		}
	}

	public class ViewPartyMembersItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.BEACON).addAllItemFlags()
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<green>View your party members!")))
					.addLoreLines(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage("<aqua>Click to see a list of your party members!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
						"<red>Still loading your player data... please try again in a few seconds.");
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (onlineUser.get().getHellblockData().hasHellblock()) {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				HellblockPlugin.getInstance().getStorageManager()
						.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(), HBConfig.lockData)
						.thenAccept((owner) -> {
							UserData ownerUser = owner.orElseThrow();
							if (ownerUser.getHellblockData().isAbandoned()) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										HBLocale.MSG_Hellblock_Is_Abandoned);
								HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
										net.kyori.adventure.sound.Sound.Source.PLAYER,
										net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
								return;
							}
							new CoopMenu(player);
							HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
									net.kyori.adventure.sound.Sound.Source.PLAYER,
									net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
						});
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						HBLocale.MSG_Hellblock_Not_Found);
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
			}
		}
	}

	public class BiomeItem extends AbstractItem {

		private UUID playerUUID;

		public BiomeItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.get().getHellblockData().getBiomeCooldown() == 0) {
				return new ItemBuilder(Material.WARPED_FUNGUS).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<green>Change island biome!")))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage("<aqua>Click to view all of the biome options!")));
			} else {
				return new ItemBuilder(Material.BARRIER).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<red>Biome Change on Cooldown!")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<dark_red>Your ability to change your biome is on cooldown!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
						"<red>Still loading your player data... please try again in a few seconds.");
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.WARPED_FUNGUS) {
				if (onlineUser.get().getHellblockData().hasHellblock()) {
					if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
						throw new NullPointerException(
								"Owner reference returned null, please report this to the developer.");
					}
					if (onlineUser.get().getHellblockData().getOwnerUUID() != null
							&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Not_Owner_Of_Hellblock);
						HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
								net.kyori.adventure.sound.Sound.Source.PLAYER,
								net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
						return;
					}
					if (onlineUser.get().getHellblockData().isAbandoned()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Is_Abandoned);
						HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
								net.kyori.adventure.sound.Sound.Source.PLAYER,
								net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
						return;
					}
					new BiomeMenu(player);
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
				} else {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							HBLocale.MSG_Hellblock_Not_Found);
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				}
			} else {
				if (onlineUser.get().getHellblockData().getBiomeCooldown() > 0) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							String.format("<red>You've recently changed your biome already, you must wait for %s!",
									HellblockPlugin.getInstance().getFormattedCooldown(
											onlineUser.get().getHellblockData().getBiomeCooldown())));
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
					return;
				}
			}
		}
	}

	public class TeleportIslandItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.ENDER_PEARL).addAllItemFlags()
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<green>Teleport to your Hellblock!")))
					.addLoreLines(
							new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
									.getComponentFromMiniMessage("<aqua>Click to go to your home island!")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
						"<red>Still loading your player data... please try again in a few seconds.");
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (!onlineUser.get().getHellblockData().hasHellblock()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						HBLocale.MSG_Hellblock_Not_Found);
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
			} else {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				HellblockPlugin.getInstance().getStorageManager()
						.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(), HBConfig.lockData)
						.thenAccept((owner) -> {
							UserData ownerUser = owner.orElseThrow();
							if (ownerUser.getHellblockData().isAbandoned()) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										HBLocale.MSG_Hellblock_Is_Abandoned);
								HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
										net.kyori.adventure.sound.Sound.Source.PLAYER,
										net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
								return;
							}
							if (ownerUser.getHellblockData().getHomeLocation() != null) {
								HellblockPlugin.getInstance().getCoopManager()
										.makeHomeLocationSafe(ownerUser, onlineUser.get()).thenRun(() -> {
											HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
													player, "<red>Teleporting you to your hellblock!");
											player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 5);
											HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
													net.kyori.adventure.sound.Sound.Source.PLAYER,
													net.kyori.adventure.key.Key
															.key("minecraft:entity.enderman.teleport"),
													1, 1);
										});
							} else {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>Error teleporting you to your hellblock!");
								HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
										net.kyori.adventure.sound.Sound.Source.PLAYER,
										net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
								throw new NullPointerException(
										"Hellblock home location returned null, please report this to the developer.");
							}
						});
			}
		}
	}

	public class ResetIslandItem extends AbstractItem {

		private UUID playerUUID;

		public ResetIslandItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.get().getHellblockData().getResetCooldown() == 0) {
				return new ItemBuilder(Material.NETHER_BRICKS).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<green>Reset your Hellblock!")))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage("<aqua>Click to reset your hellblock island!")));
			} else {
				return new ItemBuilder(Material.BARRIER).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager().getComponentFromMiniMessage("<red>Reset on Cooldown!")))
						.addLoreLines(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										"<dark_red>Your ability to reset your hellblock is on cooldown!")));
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
						"<red>Still loading your player data... please try again in a few seconds.");
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
				return;
			}
			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.NETHER_BRICKS) {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				if (onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							HBLocale.MSG_Not_Owner_Of_Hellblock);
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
					return;
				}
				if (onlineUser.get().getHellblockData().isAbandoned()) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							HBLocale.MSG_Hellblock_Is_Abandoned);
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
					return;
				}
				new ConfirmMenu(player, "Reset");
				HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
						net.kyori.adventure.sound.Sound.Source.PLAYER,
						net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
			} else {
				if (onlineUser.get().getHellblockData().isAbandoned()) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							HBLocale.MSG_Hellblock_Is_Abandoned);
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
					return;
				}
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				if (onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							HBLocale.MSG_Not_Owner_Of_Hellblock);
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
					return;
				}
				if (onlineUser.get().getHellblockData().getResetCooldown() > 0) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							String.format("<red>You've recently reset your hellblock already, you must wait for %s!",
									HellblockPlugin.getInstance().getFormattedCooldown(
											onlineUser.get().getHellblockData().getResetCooldown())));
					HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
							net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
					return;
				}
			}
		}
	}

	public class CloseMenuItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.RED_STAINED_GLASS_PANE).addAllItemFlags()
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<red>Close Menu")));
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			player.closeInventory();
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}
}
