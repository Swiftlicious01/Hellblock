package com.swiftlicious.hellblock.gui.hellblock;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.builder.PotionBuilder;
import xyz.xenondevs.invui.item.builder.PotionBuilder.PotionType;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class FlagMenu {

	public FlagMenu(Player player) {

		Gui gui = Gui.normal()
				.setStructure(" a b c d e f g h # ", " i j k l m n o p # ", " q r s t u v w x # ",
						" y z 1 2 3 4 5 # @ ")
				.addIngredient('#', new BackGroundItem())
				.addIngredient('a', new FlagBlockBreakItem(player.getUniqueId()))
				.addIngredient('b', new FlagBlockPlaceItem(player.getUniqueId()))
				.addIngredient('c', new FlagPvPItem(player.getUniqueId()))
				.addIngredient('d', new FlagDamageAnimalsItem(player.getUniqueId()))
				.addIngredient('e', new FlagMobDamageItem(player.getUniqueId()))
				.addIngredient('f', new FlagMobSpawningItem(player.getUniqueId()))
				.addIngredient('g', new FlagChestAccessItem(player.getUniqueId()))
				.addIngredient('h', new FlagInteractItem(player.getUniqueId()))
				.addIngredient('i', new FlagUseItem(player.getUniqueId()))
				.addIngredient('j', new FlagUseAnvilItem(player.getUniqueId()))
				.addIngredient('k', new FlagUseDripleafItem(player.getUniqueId()))
				.addIngredient('l', new FlagPlaceVehicleItem(player.getUniqueId()))
				.addIngredient('m', new FlagDestroyVehicleItem(player.getUniqueId()))
				.addIngredient('n', new FlagRideItem(player.getUniqueId()))
				.addIngredient('o', new FlagRotateItemFrameItem(player.getUniqueId()))
				.addIngredient('p', new FlagTrampleItem(player.getUniqueId()))
				.addIngredient('q', new FlagFallDamageItem(player.getUniqueId()))
				.addIngredient('r', new FlagFireworkDamageItem(player.getUniqueId()))
				.addIngredient('s', new FlagEnderpearlItem(player.getUniqueId()))
				.addIngredient('t', new FlagChorusFruitItem(player.getUniqueId()))
				.addIngredient('u', new FlagLighterItem(player.getUniqueId()))
				.addIngredient('v', new FlagTNTItem(player.getUniqueId()))
				.addIngredient('w', new FlagSleepItem(player.getUniqueId()))
				.addIngredient('x', new FlagRespawnAnchorItem(player.getUniqueId()))
				.addIngredient('y', new FlagWindChargeBurstItem(player.getUniqueId()))
				.addIngredient('z', new FlagPotionSplashItem(player.getUniqueId()))
				.addIngredient('1', new FlagSnowmanTrailsItem(player.getUniqueId()))
				.addIngredient('2', new FlagEnderBuildItem(player.getUniqueId()))
				.addIngredient('3', new FlagGhastFireballItem(player.getUniqueId()))
				.addIngredient('4', new FlagHealthRegenItem(player.getUniqueId()))
				.addIngredient('5', new FlagHungerDrainItem(player.getUniqueId()))
				.addIngredient('@', new BackToMainMenuItem()).build();

		Window window = Window
				.single().setViewer(player).setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage("<red>Hellblock Protection Flags")))
				.setGui(gui).build();

		window.open();
	}

	public class FlagBlockBreakItem extends AbstractItem {

		private UUID playerUUID;

		public FlagBlockBreakItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.STONE_PICKAXE).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.BLOCK_BREAK.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.BLOCK_BREAK)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.BLOCK_BREAK) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.BLOCK_BREAK,
								(pi.getProtectionValue(HellblockFlag.FlagType.BLOCK_BREAK) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagBlockPlaceItem extends AbstractItem {

		private UUID playerUUID;

		public FlagBlockPlaceItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.COBBLESTONE).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.BLOCK_PLACE.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.BLOCK_PLACE)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.BLOCK_PLACE) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.BLOCK_PLACE,
								(pi.getProtectionValue(HellblockFlag.FlagType.BLOCK_PLACE) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagPvPItem extends AbstractItem {

		private UUID playerUUID;

		public FlagPvPItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.IRON_SWORD).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														HellblockFlag.FlagType.PVP.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.PVP).getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.PVP) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.PVP,
								(pi.getProtectionValue(HellblockFlag.FlagType.PVP) == AccessType.ALLOW ? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagDamageAnimalsItem extends AbstractItem {

		private UUID playerUUID;

		public FlagDamageAnimalsItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.PORKCHOP).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.DAMAGE_ANIMALS.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.DAMAGE_ANIMALS)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.DAMAGE_ANIMALS) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.DAMAGE_ANIMALS,
								(pi.getProtectionValue(HellblockFlag.FlagType.DAMAGE_ANIMALS) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagMobDamageItem extends AbstractItem {

		private UUID playerUUID;

		public FlagMobDamageItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.ZOMBIE_HEAD).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.MOB_DAMAGE.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.MOB_DAMAGE)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.MOB_DAMAGE) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.MOB_DAMAGE,
								(pi.getProtectionValue(HellblockFlag.FlagType.MOB_DAMAGE) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagMobSpawningItem extends AbstractItem {

		private UUID playerUUID;

		public FlagMobSpawningItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.SPAWNER).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.MOB_SPAWNING.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.MOB_SPAWNING,
								(pi.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagChestAccessItem extends AbstractItem {

		private UUID playerUUID;

		public FlagChestAccessItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.CHEST).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.CHEST_ACCESS.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.CHEST_ACCESS)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.CHEST_ACCESS) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.CHEST_ACCESS,
								(pi.getProtectionValue(HellblockFlag.FlagType.CHEST_ACCESS) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagInteractItem extends AbstractItem {

		private UUID playerUUID;

		public FlagInteractItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.LEVER).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														HellblockFlag.FlagType.INTERACT.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.INTERACT)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.INTERACT) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.INTERACT,
								(pi.getProtectionValue(HellblockFlag.FlagType.INTERACT) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagUseItem extends AbstractItem {

		private UUID playerUUID;

		public FlagUseItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.OAK_DOOR).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														HellblockFlag.FlagType.USE.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.USE).getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.USE) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.USE,
								(pi.getProtectionValue(HellblockFlag.FlagType.USE) == AccessType.ALLOW ? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagUseAnvilItem extends AbstractItem {

		private UUID playerUUID;

		public FlagUseAnvilItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.ANVIL).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.USE_ANVIL.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.USE_ANVIL)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.USE_ANVIL) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.USE_ANVIL,
								(pi.getProtectionValue(HellblockFlag.FlagType.USE_ANVIL) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagUseDripleafItem extends AbstractItem {

		private UUID playerUUID;

		public FlagUseDripleafItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.BIG_DRIPLEAF).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.USE_DRIPLEAF.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.USE_DRIPLEAF)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.USE_DRIPLEAF) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.USE_DRIPLEAF,
								(pi.getProtectionValue(HellblockFlag.FlagType.USE_DRIPLEAF) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagPlaceVehicleItem extends AbstractItem {

		private UUID playerUUID;

		public FlagPlaceVehicleItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.MINECART).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.PLACE_VEHICLE.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.PLACE_VEHICLE)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.PLACE_VEHICLE) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.PLACE_VEHICLE,
								(pi.getProtectionValue(HellblockFlag.FlagType.PLACE_VEHICLE) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagDestroyVehicleItem extends AbstractItem {

		private UUID playerUUID;

		public FlagDestroyVehicleItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.OAK_BOAT).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.DESTROY_VEHICLE.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.DESTROY_VEHICLE)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.DESTROY_VEHICLE) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.DESTROY_VEHICLE,
								(pi.getProtectionValue(HellblockFlag.FlagType.DESTROY_VEHICLE) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagRideItem extends AbstractItem {

		private UUID playerUUID;

		public FlagRideItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.SADDLE).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														HellblockFlag.FlagType.RIDE.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.RIDE).getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.RIDE) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.RIDE,
								(pi.getProtectionValue(HellblockFlag.FlagType.RIDE) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagRotateItemFrameItem extends AbstractItem {

		private UUID playerUUID;

		public FlagRotateItemFrameItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.ITEM_FRAME).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(HellblockFlag.FlagType.ITEM_FRAME_ROTATE
														.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.ITEM_FRAME_ROTATE)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.ITEM_FRAME_ROTATE) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.ITEM_FRAME_ROTATE,
								(pi.getProtectionValue(HellblockFlag.FlagType.ITEM_FRAME_ROTATE) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagTrampleItem extends AbstractItem {

		private UUID playerUUID;

		public FlagTrampleItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.TURTLE_EGG).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.TRAMPLE_BLOCKS.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.TRAMPLE_BLOCKS)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.TRAMPLE_BLOCKS) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.TRAMPLE_BLOCKS,
								(pi.getProtectionValue(HellblockFlag.FlagType.TRAMPLE_BLOCKS) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagFallDamageItem extends AbstractItem {

		private UUID playerUUID;

		public FlagFallDamageItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.FEATHER).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.FALL_DAMAGE.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.FALL_DAMAGE)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.FALL_DAMAGE) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.FALL_DAMAGE,
								(pi.getProtectionValue(HellblockFlag.FlagType.FALL_DAMAGE) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagFireworkDamageItem extends AbstractItem {

		private UUID playerUUID;

		public FlagFireworkDamageItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.FIREWORK_ROCKET).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.FIREWORK_DAMAGE.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.FIREWORK_DAMAGE)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.FIREWORK_DAMAGE) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.FIREWORK_DAMAGE,
								(pi.getProtectionValue(HellblockFlag.FlagType.FIREWORK_DAMAGE) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagEnderpearlItem extends AbstractItem {

		private UUID playerUUID;

		public FlagEnderpearlItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.ENDER_PEARL).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.ENDERPEARL.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.ENDERPEARL)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.ENDERPEARL) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.ENDERPEARL,
								(pi.getProtectionValue(HellblockFlag.FlagType.ENDERPEARL) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagChorusFruitItem extends AbstractItem {

		private UUID playerUUID;

		public FlagChorusFruitItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.CHORUS_FRUIT).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.CHORUS_TELEPORT.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.CHORUS_TELEPORT)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.CHORUS_TELEPORT) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.CHORUS_TELEPORT,
								(pi.getProtectionValue(HellblockFlag.FlagType.CHORUS_TELEPORT) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagLighterItem extends AbstractItem {

		private UUID playerUUID;

		public FlagLighterItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.FLINT_AND_STEEL).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														HellblockFlag.FlagType.LIGHTER.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.LIGHTER)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.LIGHTER) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.LIGHTER,
								(pi.getProtectionValue(HellblockFlag.FlagType.LIGHTER) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagTNTItem extends AbstractItem {

		private UUID playerUUID;

		public FlagTNTItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.TNT).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														HellblockFlag.FlagType.TNT.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.TNT).getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.TNT) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.TNT,
								(pi.getProtectionValue(HellblockFlag.FlagType.TNT) == AccessType.ALLOW ? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagSleepItem extends AbstractItem {

		private UUID playerUUID;

		public FlagSleepItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.RED_BED).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														HellblockFlag.FlagType.SLEEP.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.SLEEP).getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.SLEEP) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.SLEEP,
								(pi.getProtectionValue(HellblockFlag.FlagType.SLEEP) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagRespawnAnchorItem extends AbstractItem {

		private UUID playerUUID;

		public FlagRespawnAnchorItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.RESPAWN_ANCHOR).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.RESPAWN_ANCHORS.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.RESPAWN_ANCHORS)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.RESPAWN_ANCHORS) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.RESPAWN_ANCHORS,
								(pi.getProtectionValue(HellblockFlag.FlagType.RESPAWN_ANCHORS) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagWindChargeBurstItem extends AbstractItem {

		private UUID playerUUID;

		public FlagWindChargeBurstItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.WIND_CHARGE).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(HellblockFlag.FlagType.WIND_CHARGE_BURST
														.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.WIND_CHARGE_BURST)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.WIND_CHARGE_BURST) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.WIND_CHARGE_BURST,
								(pi.getProtectionValue(HellblockFlag.FlagType.WIND_CHARGE_BURST) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagPotionSplashItem extends AbstractItem {

		private UUID playerUUID;

		public FlagPotionSplashItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				PotionBuilder item = new PotionBuilder(PotionType.SPLASH)
						.addEffect(new PotionEffect(PotionEffectType.SPEED, 1, 1)).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.POTION_SPLASH.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.POTION_SPLASH)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.POTION_SPLASH) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.POTION_SPLASH,
								(pi.getProtectionValue(HellblockFlag.FlagType.POTION_SPLASH) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagSnowmanTrailsItem extends AbstractItem {

		private UUID playerUUID;

		public FlagSnowmanTrailsItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.SNOWBALL).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.SNOWMAN_TRAILS.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.SNOWMAN_TRAILS)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.SNOWMAN_TRAILS) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.SNOWMAN_TRAILS,
								(pi.getProtectionValue(HellblockFlag.FlagType.SNOWMAN_TRAILS) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagEnderBuildItem extends AbstractItem {

		private UUID playerUUID;

		public FlagEnderBuildItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.ENDER_CHEST).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.ENDER_BUILD.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.ENDER_BUILD)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.ENDER_BUILD) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.ENDER_BUILD,
								(pi.getProtectionValue(HellblockFlag.FlagType.ENDER_BUILD) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagGhastFireballItem extends AbstractItem {

		private UUID playerUUID;

		public FlagGhastFireballItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.GHAST_TEAR).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.GHAST_FIREBALL.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.GHAST_FIREBALL)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.GHAST_FIREBALL) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.GHAST_FIREBALL,
								(pi.getProtectionValue(HellblockFlag.FlagType.GHAST_FIREBALL) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagHealthRegenItem extends AbstractItem {

		private UUID playerUUID;

		public FlagHealthRegenItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.GOLDEN_APPLE).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.HEALTH_REGEN.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.HEALTH_REGEN)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.HEALTH_REGEN) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.HEALTH_REGEN,
								(pi.getProtectionValue(HellblockFlag.FlagType.HEALTH_REGEN) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
		}
	}

	public class FlagHungerDrainItem extends AbstractItem {

		private UUID playerUUID;

		public FlagHungerDrainItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				ItemBuilder item = new ItemBuilder(Material.ROTTEN_FLESH).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(
												HellblockFlag.FlagType.HUNGER_DRAIN.getName().replace("-", " "))))))
						.addLoreLines(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
										HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
												.getProtectionValue(HellblockFlag.FlagType.HUNGER_DRAIN)
												.getReturnValue()))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(HellblockFlag.FlagType.HUNGER_DRAIN) == AccessType.ALLOW) {
					item.addEnchantment(Enchantment.UNBREAKING, 1, false);
				}
				return item;
			} else {
				return new ItemBuilder(Material.AIR);
			}
		}

		@Override
		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
				@NotNull InventoryClickEvent event) {
			HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
					.getActivePlayer(player.getUniqueId());
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtected()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						new HellblockFlag(HellblockFlag.FlagType.HUNGER_DRAIN,
								(pi.getProtectionValue(HellblockFlag.FlagType.HUNGER_DRAIN) == AccessType.ALLOW
										? AccessType.DENY
										: AccessType.ALLOW)));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
			HellblockPlugin.getInstance().getAdventureManager().sendSound(player,
					net.kyori.adventure.sound.Sound.Source.PLAYER,
					net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
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
