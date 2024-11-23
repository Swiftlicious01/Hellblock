package com.swiftlicious.hellblock.gui.hellblock;

public class IslandChoiceMenu {

//	public IslandChoiceMenu(Player player, boolean isReset) {
//
//		Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
//				.getOnlineUser(player.getUniqueId());
//		if (onlineUser.isEmpty()) {
//			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
//					"<red>Still loading your player data... please try again in a few seconds.");
//			return;
//		}
//
//		if (!HellblockPlugin.getInstance().getConfigManager().islandOptions().isEmpty()) {
//			Gui gui = Gui.normal().setStructure("# d c s #").addIngredient('d', new DefaultIslandChoiceItem(isReset))
//					.addIngredient('c', new ClassicIslandChoiceItem(isReset))
//					.addIngredient('s', new SchematicIslandChoiceItem(isReset)).addIngredient('#', new BackGroundItem())
//					.build();
//
//			Window window = Window.single().setViewer(player)
//					.setTitle(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
//							.getComponentFromMiniMessage("<red>Hellblock Island Options")))
//					.setGui(gui).setCloseable(onlineUser.get().getHellblockData().hasHellblock()).build();
//
//			window.open();
//		} else {
//			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
//					"<red>The island options list is empty, creating classic hellblock!");
//			HellblockPlugin.getInstance().getHellblockHandler().createHellblock(player, IslandOptions.CLASSIC, isReset);
//		}
//	}
//
//	public class DefaultIslandChoiceItem extends AbstractItem {
//
//		boolean isReset = false;
//
//		public DefaultIslandChoiceItem(boolean isReset) {
//			this.isReset = isReset;
//		}
//
//		@Override
//		public ItemProvider getItemProvider() {
//			if (HellblockPlugin.getInstance().getConfigManager().islandOptions()
//					.contains(IslandOptions.DEFAULT.getName())) {
//				return new ItemBuilder(Material.NETHERRACK)
//						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
//								.getAdventureManager().getComponentFromMiniMessage("<red>Default Hellblock Island")))
//						.addLoreLines(new ShadedAdventureComponentWrapper(
//								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
//										"<gold>Click to generate the default hellblock island type!")));
//			} else {
//				return new ItemBuilder(Material.BARRIER)
//						.setDisplayName(
//								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
//										.getComponentFromMiniMessage("<red>Default Hellblock Unavailable!")))
//						.addLoreLines(new ShadedAdventureComponentWrapper(
//								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
//										"<gold>This type of hellblock isn't available to choose!")));
//			}
//		}
//
//		@Override
//		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
//				@NotNull InventoryClickEvent event) {
//			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
//					.getOnlineUser(player.getUniqueId());
//			if (onlineUser.isEmpty())
//				return;
//			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.NETHERRACK) {
//				if (isReset && onlineUser.get().getHellblockData().getResetCooldown() > 0) {
//					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
//							String.format("<red>You've recently reset your hellblock already, you must wait for %s!",
//									HellblockPlugin.getInstance().getFormattedCooldown(
//											onlineUser.get().getHellblockData().getResetCooldown())));
//					HellblockPlugin.getInstance().getAdventureManager().playSound(player,
//							net.kyori.adventure.sound.Sound.Source.PLAYER,
//							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
//					return;
//				}
//				HellblockPlugin.getInstance().getScheduler().sync().runLater(() -> {
//					for (Iterator<Window> windows = getWindows().iterator(); windows.hasNext();) {
//						Window window = windows.next();
//						if (window.getViewerUUID().equals(player.getUniqueId())) {
//							window.setCloseable(true);
//							window.close();
//						}
//					}
//				}, 1 * 20L, player.getLocation());
//				HellblockPlugin.getInstance().getHellblockHandler().createHellblock(player, IslandOptions.DEFAULT,
//						isReset);
//				HellblockPlugin.getInstance().getAdventureManager().playSound(player,
//						net.kyori.adventure.sound.Sound.Source.PLAYER,
//						net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
//			} else {
//				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
//						"<red>The default hellblock type isn't available to generate!");
//				HellblockPlugin.getInstance().getAdventureManager().playSound(player,
//						net.kyori.adventure.sound.Sound.Source.PLAYER,
//						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
//			}
//		}
//	}
//
//	public class ClassicIslandChoiceItem extends AbstractItem {
//
//		boolean isReset = false;
//
//		public ClassicIslandChoiceItem(boolean isReset) {
//			this.isReset = isReset;
//		}
//
//		@Override
//		public ItemProvider getItemProvider() {
//			if (HellblockPlugin.getInstance().getConfigManager().islandOptions()
//					.contains(IslandOptions.CLASSIC.getName())) {
//				return new ItemBuilder(Material.SOUL_SAND)
//						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
//								.getAdventureManager().getComponentFromMiniMessage("<red>Classic Hellblock Island")))
//						.addLoreLines(new ShadedAdventureComponentWrapper(
//								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
//										"<gold>Click to generate the classic hellblock island type!")));
//			} else {
//				return new ItemBuilder(Material.BARRIER)
//						.setDisplayName(
//								new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance().getAdventureManager()
//										.getComponentFromMiniMessage("<red>Classic Hellblock Unavailable!")))
//						.addLoreLines(new ShadedAdventureComponentWrapper(
//								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
//										"<gold>This type of hellblock isn't available to choose!")));
//			}
//		}
//
//		@Override
//		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
//				@NotNull InventoryClickEvent event) {
//			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
//					.getOnlineUser(player.getUniqueId());
//			if (onlineUser.isEmpty())
//				return;
//			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.SOUL_SAND) {
//				if (isReset && onlineUser.get().getHellblockData().getResetCooldown() > 0) {
//					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
//							String.format("<red>You've recently reset your hellblock already, you must wait for %s!",
//									HellblockPlugin.getInstance().getFormattedCooldown(
//											onlineUser.get().getHellblockData().getResetCooldown())));
//					HellblockPlugin.getInstance().getAdventureManager().playSound(player,
//							net.kyori.adventure.sound.Sound.Source.PLAYER,
//							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
//					return;
//				}
//				HellblockPlugin.getInstance().getScheduler().sync().runLater(() -> {
//					for (Iterator<Window> windows = getWindows().iterator(); windows.hasNext();) {
//						Window window = windows.next();
//						if (window.getViewerUUID().equals(player.getUniqueId())) {
//							window.setCloseable(true);
//							window.close();
//						}
//					}
//				}, 1 * 20L, player.getLocation());
//				HellblockPlugin.getInstance().getHellblockHandler().createHellblock(player, IslandOptions.CLASSIC,
//						isReset);
//				HellblockPlugin.getInstance().getAdventureManager().playSound(player,
//						net.kyori.adventure.sound.Sound.Source.PLAYER,
//						net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
//			} else {
//				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
//						"<red>The classic hellblock type isn't available to generate!");
//				HellblockPlugin.getInstance().getAdventureManager().playSound(player,
//						net.kyori.adventure.sound.Sound.Source.PLAYER,
//						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
//			}
//		}
//	}
//
//	public class SchematicIslandChoiceItem extends AbstractItem {
//
//		boolean isReset = false;
//
//		public SchematicIslandChoiceItem(boolean isReset) {
//			this.isReset = isReset;
//		}
//
//		@Override
//		public ItemProvider getItemProvider() {
//			boolean schematicsAvailable = false;
//			for (String list : HellblockPlugin.getInstance().getConfigManager().islandOptions()) {
//				if (list.equalsIgnoreCase(IslandOptions.CLASSIC.getName())
//						|| list.equalsIgnoreCase(IslandOptions.DEFAULT.getName()))
//					continue;
//				if (!HellblockPlugin.getInstance().getSchematicManager().availableSchematics.contains(list))
//					continue;
//
//				schematicsAvailable = true;
//				break;
//			}
//			if (schematicsAvailable) {
//				return new ItemBuilder(Material.MAP)
//						.setDisplayName(new ShadedAdventureComponentWrapper(
//								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
//										"<red>Choose a schematic for your Hellblock Island!")))
//						.addLoreLines(new ShadedAdventureComponentWrapper(
//								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
//										"<gold>Click to view the different schematic options you have available!")));
//			} else {
//				return new ItemBuilder(Material.BARRIER)
//						.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
//								.getAdventureManager().getComponentFromMiniMessage("<red>No schematics available!")))
//						.addLoreLines(new ShadedAdventureComponentWrapper(
//								HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
//										"<gold>There are no options to choose from here!")));
//			}
//		}
//
//		@Override
//		public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
//				@NotNull InventoryClickEvent event) {
//			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
//					.getOnlineUser(player.getUniqueId());
//			if (onlineUser.isEmpty())
//				return;
//			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.MAP) {
//				if (isReset && onlineUser.get().getHellblockData().getResetCooldown() > 0) {
//					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
//							String.format("<red>You've recently reset your hellblock already, you must wait for %s!",
//									HellblockPlugin.getInstance().getFormattedCooldown(
//											onlineUser.get().getHellblockData().getResetCooldown())));
//					HellblockPlugin.getInstance().getAdventureManager().playSound(player,
//							net.kyori.adventure.sound.Sound.Source.PLAYER,
//							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
//					return;
//				}
//				boolean schematicsAvailable = false;
//				for (String list : HellblockPlugin.getInstance().getConfigManager().islandOptions()) {
//					if (list.equalsIgnoreCase(IslandOptions.CLASSIC.getName())
//							|| list.equalsIgnoreCase(IslandOptions.DEFAULT.getName()))
//						continue;
//					if (!HellblockPlugin.getInstance().getSchematicManager().availableSchematics.contains(list))
//						continue;
//
//					schematicsAvailable = true;
//					break;
//				}
//				if (schematicsAvailable) {
//					new SchematicMenu(player, isReset);
//					HellblockPlugin.getInstance().getAdventureManager().playSound(player,
//							net.kyori.adventure.sound.Sound.Source.PLAYER,
//							net.kyori.adventure.key.Key.key("minecraft:ui.button.click"), 1, 1);
//				} else {
//					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
//							"<red>There are no hellblock schematics for you to choose from!");
//					HellblockPlugin.getInstance().getAdventureManager().playSound(player,
//							net.kyori.adventure.sound.Sound.Source.PLAYER,
//							net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
//				}
//			} else {
//				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
//						"<red>There are no hellblock schematics for you to choose from!");
//				HellblockPlugin.getInstance().getAdventureManager().playSound(player,
//						net.kyori.adventure.sound.Sound.Source.PLAYER,
//						net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
//			}
//		}
//	}
}
