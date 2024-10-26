package com.swiftlicious.hellblock.gui.hellblock;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldguard.protection.flags.Flags;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.gui.icon.BackGroundItem;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

public class FlagMenu {

	public FlagMenu(Player player) {

		Gui gui = Gui.normal().setStructure(" # a b c d e f g # ", " # h i j k l m n o ", " # p q r s t u v # ")
				.addIngredient('#', new BackGroundItem()).addIngredient('x', new ItemStack(Material.AIR))
				.addIngredient('a', new FlagBlockBreakItem(player.getUniqueId()))
				.addIngredient('b', new FlagBlockPlaceItem(player.getUniqueId()))
				.addIngredient('c', new FlagPvPItem(player.getUniqueId()))
				.addIngredient('d', new FlagDamageAnimalsItem(player.getUniqueId()))
				.addIngredient('e', new FlagMobDamageItem(player.getUniqueId()))
				.addIngredient('f', new FlagMobSpawningItem(player.getUniqueId()))
				.addIngredient('g', new FlagChestAccessItem(player.getUniqueId()))
				.addIngredient('h', new FlagUseItem(player.getUniqueId()))
				.addIngredient('i', new FlagUseAnvilItem(player.getUniqueId()))
				.addIngredient('j', new FlagUseDripleafItem(player.getUniqueId()))
				.addIngredient('k', new FlagPlaceVehicleItem(player.getUniqueId()))
				.addIngredient('l', new FlagDestroyVehicleItem(player.getUniqueId()))
				.addIngredient('m', new FlagRideItem(player.getUniqueId()))
				.addIngredient('n', new FlagRotateItemFrameItem(player.getUniqueId()))
				.addIngredient('p', new FlagTrampleItem(player.getUniqueId()))
				.addIngredient('q', new FlagFireworkDamageItem(player.getUniqueId()))
				.addIngredient('r', new FlagEnderpearlItem(player.getUniqueId()))
				.addIngredient('s', new FlagChorusFruitItem(player.getUniqueId()))
				.addIngredient('t', new FlagLighterItem(player.getUniqueId()))
				.addIngredient('u', new FlagTNTItem(player.getUniqueId()))
				.addIngredient('v', new FlagRespawnAnchorItem(player.getUniqueId()))
				.addIngredient('o', new BackToMainMenuItem()).build();

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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.STONE_PICKAXE).addAllItemFlags().setDisplayName(
						new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils
												.capitaliseAllWords(Flags.BLOCK_BREAK.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.BLOCK_BREAK.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.BLOCK_BREAK.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.BLOCK_BREAK.getName(), (!pi.getProtectionValue(Flags.BLOCK_BREAK.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagBlockPlaceItem extends AbstractItem {

		private UUID playerUUID;

		public FlagBlockPlaceItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.COBBLESTONE).addAllItemFlags().setDisplayName(
						new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils
												.capitaliseAllWords(Flags.BLOCK_PLACE.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.BLOCK_PLACE.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.BLOCK_PLACE.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.BLOCK_PLACE.getName(), (!pi.getProtectionValue(Flags.BLOCK_PLACE.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagPvPItem extends AbstractItem {

		private UUID playerUUID;

		public FlagPvPItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.IRON_SWORD).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(Flags.PVP.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.PVP.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.PVP.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.PVP.getName(), (!pi.getProtectionValue(Flags.PVP.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagDamageAnimalsItem extends AbstractItem {

		private UUID playerUUID;

		public FlagDamageAnimalsItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.PORKCHOP).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														Flags.DAMAGE_ANIMALS.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.DAMAGE_ANIMALS.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.DAMAGE_ANIMALS.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.DAMAGE_ANIMALS.getName(), (!pi.getProtectionValue(Flags.DAMAGE_ANIMALS.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagMobDamageItem extends AbstractItem {

		private UUID playerUUID;

		public FlagMobDamageItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.ZOMBIE_HEAD).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(Flags.MOB_DAMAGE.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.MOB_DAMAGE.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.MOB_DAMAGE.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.MOB_DAMAGE.getName(), (!pi.getProtectionValue(Flags.MOB_DAMAGE.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagMobSpawningItem extends AbstractItem {

		private UUID playerUUID;

		public FlagMobSpawningItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.SPAWNER).addAllItemFlags().setDisplayName(
						new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils
												.capitaliseAllWords(Flags.MOB_SPAWNING.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.MOB_SPAWNING.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.MOB_SPAWNING.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.MOB_SPAWNING.getName(), (!pi.getProtectionValue(Flags.MOB_SPAWNING.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagChestAccessItem extends AbstractItem {

		private UUID playerUUID;

		public FlagChestAccessItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.CHEST).addAllItemFlags().setDisplayName(
						new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils
												.capitaliseAllWords(Flags.CHEST_ACCESS.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.CHEST_ACCESS.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.CHEST_ACCESS.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.CHEST_ACCESS.getName(), (!pi.getProtectionValue(Flags.CHEST_ACCESS.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagUseItem extends AbstractItem {

		private UUID playerUUID;

		public FlagUseItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.OAK_DOOR).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(Flags.USE.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.USE.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.USE.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.USE.getName(), (!pi.getProtectionValue(Flags.USE.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagUseAnvilItem extends AbstractItem {

		private UUID playerUUID;

		public FlagUseAnvilItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.ANVIL).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(Flags.USE_ANVIL.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.USE_ANVIL.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.USE_ANVIL.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.USE_ANVIL.getName(), (!pi.getProtectionValue(Flags.USE_ANVIL.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagUseDripleafItem extends AbstractItem {

		private UUID playerUUID;

		public FlagUseDripleafItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.BIG_DRIPLEAF).addAllItemFlags().setDisplayName(
						new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils
												.capitaliseAllWords(Flags.USE_DRIPLEAF.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.USE_DRIPLEAF.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.USE_DRIPLEAF.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.USE_DRIPLEAF.getName(), (!pi.getProtectionValue(Flags.USE_DRIPLEAF.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagPlaceVehicleItem extends AbstractItem {

		private UUID playerUUID;

		public FlagPlaceVehicleItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.MINECART).addAllItemFlags().setDisplayName(
						new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils
												.capitaliseAllWords(Flags.PLACE_VEHICLE.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.PLACE_VEHICLE.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.PLACE_VEHICLE.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.PLACE_VEHICLE.getName(), (!pi.getProtectionValue(Flags.PLACE_VEHICLE.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagDestroyVehicleItem extends AbstractItem {

		private UUID playerUUID;

		public FlagDestroyVehicleItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.OAK_BOAT).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														Flags.DESTROY_VEHICLE.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.DESTROY_VEHICLE.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.DESTROY_VEHICLE.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.DESTROY_VEHICLE.getName(), (!pi.getProtectionValue(Flags.DESTROY_VEHICLE.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagRideItem extends AbstractItem {

		private UUID playerUUID;

		public FlagRideItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.SADDLE).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(Flags.RIDE.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.RIDE.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.RIDE.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.RIDE.getName(), (!pi.getProtectionValue(Flags.RIDE.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagRotateItemFrameItem extends AbstractItem {

		private UUID playerUUID;

		public FlagRotateItemFrameItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.ITEM_FRAME).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														Flags.ITEM_FRAME_ROTATE.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.ITEM_FRAME_ROTATE.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.ITEM_FRAME_ROTATE.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.ITEM_FRAME_ROTATE.getName(), (!pi.getProtectionValue(Flags.ITEM_FRAME_ROTATE.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagTrampleItem extends AbstractItem {

		private UUID playerUUID;

		public FlagTrampleItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.TURTLE_EGG).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														Flags.TRAMPLE_BLOCKS.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.TRAMPLE_BLOCKS.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.TRAMPLE_BLOCKS.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.TRAMPLE_BLOCKS.getName(), (!pi.getProtectionValue(Flags.TRAMPLE_BLOCKS.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagFireworkDamageItem extends AbstractItem {

		private UUID playerUUID;

		public FlagFireworkDamageItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.FIREWORK_ROCKET).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														Flags.FIREWORK_DAMAGE.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.FIREWORK_DAMAGE.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.FIREWORK_DAMAGE.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.FIREWORK_DAMAGE.getName(), (!pi.getProtectionValue(Flags.FIREWORK_DAMAGE.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagEnderpearlItem extends AbstractItem {

		private UUID playerUUID;

		public FlagEnderpearlItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.ENDER_PEARL).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(Flags.ENDERPEARL.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.ENDERPEARL.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.ENDERPEARL.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.ENDERPEARL.getName(), (!pi.getProtectionValue(Flags.ENDERPEARL.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagChorusFruitItem extends AbstractItem {

		private UUID playerUUID;

		public FlagChorusFruitItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.CHORUS_FRUIT).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														Flags.CHORUS_TELEPORT.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.CHORUS_TELEPORT.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.CHORUS_TELEPORT.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.CHORUS_TELEPORT.getName(), (!pi.getProtectionValue(Flags.CHORUS_TELEPORT.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagLighterItem extends AbstractItem {

		private UUID playerUUID;

		public FlagLighterItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.FLINT_AND_STEEL).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(Flags.LIGHTER.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.LIGHTER.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.LIGHTER.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.LIGHTER.getName(), (!pi.getProtectionValue(Flags.LIGHTER.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagTNTItem extends AbstractItem {

		private UUID playerUUID;

		public FlagTNTItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.TNT).addAllItemFlags()
						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
								.getAdventureManager()
								.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
										StringUtils.capitaliseAllWords(Flags.TNT.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.TNT.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.TNT.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.TNT.getName(), (!pi.getProtectionValue(Flags.TNT.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class FlagRespawnAnchorItem extends AbstractItem {

		private UUID playerUUID;

		public FlagRespawnAnchorItem(UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		@Override
		public ItemProvider getItemProvider() {
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				ItemBuilder item = new ItemBuilder(Material.RESPAWN_ANCHOR).addAllItemFlags()
						.setDisplayName(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<aqua>%s Flag",
												StringUtils.capitaliseAllWords(
														Flags.RESPAWN_ANCHORS.getName().replace("-", " "))))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.RESPAWN_ANCHORS.getName())))));
				if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(playerUUID)
						.getProtectionValue(Flags.RESPAWN_ANCHORS.getName())) {
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
			if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
				HellblockPlugin.getInstance().getIslandProtectionManager().changeProtectionFlag(player.getUniqueId(),
						Flags.RESPAWN_ANCHORS.getName(), (!pi.getProtectionValue(Flags.RESPAWN_ANCHORS.getName())));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
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
		}
	}
}
