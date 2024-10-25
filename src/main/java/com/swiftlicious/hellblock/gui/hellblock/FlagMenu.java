package com.swiftlicious.hellblock.gui.hellblock;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
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
				return new ItemBuilder(Material.WOODEN_PICKAXE)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.BLOCK_BREAK.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.BLOCK_BREAK.getName())))));
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
						Flags.BLOCK_BREAK, (pi.getProtectionValue(Flags.BLOCK_BREAK) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.COBBLESTONE)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.BLOCK_PLACE.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.BLOCK_PLACE.getName())))));
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
						Flags.BLOCK_PLACE, (pi.getProtectionValue(Flags.BLOCK_PLACE) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.IRON_SWORD)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.PVP.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.PVP.getName())))));
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
						Flags.PVP, (pi.getProtectionValue(Flags.PVP) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.IRON_SWORD)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.DAMAGE_ANIMALS.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.DAMAGE_ANIMALS.getName())))));
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
						Flags.DAMAGE_ANIMALS, (pi.getProtectionValue(Flags.DAMAGE_ANIMALS) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.IRON_SWORD)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.MOB_DAMAGE.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.MOB_DAMAGE.getName())))));
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
						Flags.MOB_DAMAGE, (pi.getProtectionValue(Flags.MOB_DAMAGE) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.IRON_SWORD)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.MOB_SPAWNING.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.MOB_SPAWNING.getName())))));
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
						Flags.MOB_SPAWNING, (pi.getProtectionValue(Flags.MOB_SPAWNING) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.IRON_SWORD)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.CHEST_ACCESS.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.CHEST_ACCESS.getName())))));
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
						Flags.CHEST_ACCESS, (pi.getProtectionValue(Flags.CHEST_ACCESS) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.OAK_DOOR)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.USE.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.USE.getName())))));
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
						Flags.USE, (pi.getProtectionValue(Flags.USE) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.ANVIL)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.USE_ANVIL.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.USE_ANVIL.getName())))));
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
						Flags.USE_ANVIL, (pi.getProtectionValue(Flags.USE_ANVIL) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.SMALL_DRIPLEAF)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.USE_DRIPLEAF.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.USE_DRIPLEAF.getName())))));
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
						Flags.USE_DRIPLEAF, (pi.getProtectionValue(Flags.USE_DRIPLEAF) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.MINECART)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.PLACE_VEHICLE.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.PLACE_VEHICLE.getName())))));
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
						Flags.PLACE_VEHICLE, (pi.getProtectionValue(Flags.PLACE_VEHICLE) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.OAK_BOAT)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.DESTROY_VEHICLE.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.DESTROY_VEHICLE.getName())))));
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
						Flags.DESTROY_VEHICLE, (pi.getProtectionValue(Flags.DESTROY_VEHICLE) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.SADDLE)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.RIDE.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.RIDE.getName())))));
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
						Flags.RIDE, (pi.getProtectionValue(Flags.RIDE) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.ITEM_FRAME)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.ITEM_FRAME_ROTATE.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.ITEM_FRAME_ROTATE.getName())))));
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
						Flags.ITEM_FRAME_ROTATE, (pi.getProtectionValue(Flags.ITEM_FRAME_ROTATE) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.TURTLE_EGG)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.TRAMPLE_BLOCKS.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.TRAMPLE_BLOCKS.getName())))));
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
						Flags.TRAMPLE_BLOCKS, (pi.getProtectionValue(Flags.TRAMPLE_BLOCKS) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.FIREWORK_ROCKET)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.FIREWORK_DAMAGE.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.FIREWORK_DAMAGE.getName())))));
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
						Flags.FIREWORK_DAMAGE, (pi.getProtectionValue(Flags.FIREWORK_DAMAGE) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.ENDER_PEARL)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.ENDERPEARL.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.ENDERPEARL.getName())))));
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
						Flags.ENDERPEARL, (pi.getProtectionValue(Flags.ENDERPEARL) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.CHORUS_FRUIT)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.CHORUS_TELEPORT.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.CHORUS_TELEPORT.getName())))));
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
						Flags.CHORUS_TELEPORT, (pi.getProtectionValue(Flags.CHORUS_TELEPORT) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.FLINT_AND_STEEL)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.LIGHTER.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.LIGHTER.getName())))));
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
						Flags.LIGHTER, (pi.getProtectionValue(Flags.LIGHTER) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.TNT)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.TNT.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.TNT.getName())))));
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
						Flags.TNT, (pi.getProtectionValue(Flags.TNT) ? "ALLOW" : "DENY"));
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
				return new ItemBuilder(Material.RESPAWN_ANCHOR)
						.setDisplayName(new ShadedAdventureComponentWrapper(
								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
										String.format("<aqua>%s Flag", Flags.RESPAWN_ANCHORS.getName()))))
						.addLoreLines(
								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage(String.format("<yellow>Allowed: <gold>%s",
												HellblockPlugin.getInstance().getHellblockHandler()
														.getActivePlayer(playerUUID)
														.getProtectionValue(Flags.RESPAWN_ANCHORS.getName())))));
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
						Flags.RESPAWN_ANCHORS, (pi.getProtectionValue(Flags.RESPAWN_ANCHORS) ? "ALLOW" : "DENY"));
				new FlagMenu(player);
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public class BackToMainMenuItem extends AbstractItem {

		@Override
		public ItemProvider getItemProvider() {
			return new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
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
