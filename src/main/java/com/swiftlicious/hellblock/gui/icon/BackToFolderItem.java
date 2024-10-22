package com.swiftlicious.hellblock.gui.icon;

import java.io.File;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.gui.Icon;
import com.swiftlicious.hellblock.gui.page.file.FileSelector;
import com.swiftlicious.hellblock.utils.wrappers.ShadedAdventureComponentWrapper;

import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class BackToFolderItem extends AbstractItem implements Icon {

	private final File file;

	public BackToFolderItem(File file) {
		this.file = file;
	}

	@Override
	public ItemProvider getItemProvider() {
		if (file != null && (file.getPath().startsWith("plugins\\Hellblock\\contents")
				|| file.getPath().startsWith("plugins/Hellblock/contents"))) {
			return new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
					.setDisplayName(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage(HBLocale.GUI_BACK_TO_PARENT_FOLDER)))
					.setLore(List.of(new ShadedAdventureComponentWrapper(HellblockPlugin.getInstance()
							.getAdventureManager().getComponentFromMiniMessage("<#FFA500>-> " + file.getName()))));
		} else {
			return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE);
		}
	}

	@Override
	public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
		if (file != null && (file.getPath().startsWith("plugins\\Hellblock\\contents")
				|| file.getPath().startsWith("plugins/Hellblock/contents")))
			new FileSelector(player, file);
	}
}