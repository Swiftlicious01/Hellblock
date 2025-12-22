package com.swiftlicious.hellblock.schematic;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.block.CommandBlock;
import org.bukkit.block.Container;
import org.bukkit.block.EnchantingTable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.handlers.AdventureHelper;

import net.kyori.adventure.text.Component;

@SuppressWarnings("deprecation")
public class AdventureMetadata {

	private AdventureMetadata() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	private static final boolean PAPER_ADVENTURE_SUPPORTED;

	private static final Method ENTITY_GET_CUSTOM_NAME;
	private static final Method ENTITY_SET_CUSTOM_NAME;
	private static final Method TILE_ENTITY_GET_CUSTOM_NAME;
	private static final Method TILE_ENTITY_SET_CUSTOM_NAME;
	private static final Method BOOK_META_PAGES;
	private static final Method TABLE_CUSTOM_NAME_GET;
	private static final Method TABLE_CUSTOM_NAME_SET;
	private static final Method CMD_BLOCK_NAME_GET;
	private static final Method CMD_BLOCK_NAME_SET;
	private static final Method CMD_MINECART_CUSTOM_NAME_GET;
	private static final Method CMD_MINECART_CUSTOM_NAME_SET;

	static {
		boolean supported;
		Method getCustomName = null, setCustomName = null;
		Method getTileCustomName = null, setTileCustomName = null;
		Method pages = null;
		Method tabGet = null, tabSet = null;
		Method cmdGet = null, cmdSet = null;
		Method minecartGet = null, minecartSet = null;

		try {
			// Paper Adventure API method checks
			Class<?> component = Class.forName("net.kyori.adventure.text.Component");

			getCustomName = Entity.class.getMethod("customName");
			setCustomName = Entity.class.getMethod("customName", component);

			getTileCustomName = Container.class.getMethod("customName");
			setTileCustomName = Container.class.getMethod("customName", component);

			pages = BookMeta.class.getMethod("pages", List.class);

			tabGet = EnchantingTable.class.getMethod("customName");
			tabSet = EnchantingTable.class.getMethod("customName", component);

			cmdGet = CommandBlock.class.getMethod("name");
			cmdSet = CommandBlock.class.getMethod("name", component);

			minecartGet = CommandMinecart.class.getMethod("customName");
			minecartSet = CommandMinecart.class.getMethod("customName", component);

			supported = true;
		} catch (Throwable e) {
			supported = false;
		}
		PAPER_ADVENTURE_SUPPORTED = supported;

		ENTITY_GET_CUSTOM_NAME = getCustomName;
		ENTITY_SET_CUSTOM_NAME = setCustomName;
		TILE_ENTITY_GET_CUSTOM_NAME = getTileCustomName;
		TILE_ENTITY_SET_CUSTOM_NAME = setTileCustomName;
		BOOK_META_PAGES = pages;
		TABLE_CUSTOM_NAME_GET = tabGet;
		TABLE_CUSTOM_NAME_SET = tabSet;
		CMD_BLOCK_NAME_GET = cmdGet;
		CMD_BLOCK_NAME_SET = cmdSet;
		CMD_MINECART_CUSTOM_NAME_GET = minecartGet;
		CMD_MINECART_CUSTOM_NAME_SET = minecartSet;
	}

	public static boolean isAdventureSupported() {
		return PAPER_ADVENTURE_SUPPORTED;
	}

	@Nullable
	public static Component getEnchantingTableName(@NotNull EnchantingTable table) {
		if (TABLE_CUSTOM_NAME_GET != null) {
			try {
				Object component = TABLE_CUSTOM_NAME_GET.invoke(table);
				return component instanceof Component ? (Component) component : null;
			} catch (Exception e) {
				return null;
			}
		}

		String legacyName = table.getCustomName();
		return legacyName != null ? AdventureHelper.legacyToComponent(legacyName) : null;
	}

	public static void setEnchantingTableName(@NotNull EnchantingTable table, @Nullable Component name) {
		if (TABLE_CUSTOM_NAME_SET != null) {
			try {
				TABLE_CUSTOM_NAME_SET.invoke(table, name);
				return;
			} catch (Exception ignored) {
			}
		}
		table.setCustomName(name != null ? AdventureHelper.componentToLegacy(name) : null);
	}

	@Nullable
	public static Component getCommandBlockName(@NotNull CommandBlock block) {
		if (CMD_BLOCK_NAME_GET != null) {
			try {
				Object component = CMD_BLOCK_NAME_GET.invoke(block);
				return component instanceof Component ? (Component) component : null;
			} catch (Exception e) {
				return null;
			}
		}

		return AdventureHelper.legacyToComponent(block.getName());
	}

	public static void setCommandBlockName(@NotNull CommandBlock block, @Nullable Component name) {
		if (CMD_BLOCK_NAME_SET != null) {
			try {
				CMD_BLOCK_NAME_SET.invoke(block, name);
				return;
			} catch (Exception ignored) {
			}
		}

		block.setName(name != null ? AdventureHelper.componentToLegacy(name) : null);
	}

	public static void setBookPages(@NotNull BookMeta meta, @NotNull List<Component> pages) {
		if (BOOK_META_PAGES != null) {
			try {
				BOOK_META_PAGES.invoke(meta, pages);
				return;
			} catch (Exception ignored) {
			}
		}

		List<String> legacyPages = pages.stream().map(AdventureHelper::componentToLegacy).collect(Collectors.toList());
		meta.setPages(legacyPages);
	}

	@Nullable
	public static Component getEntityCustomName(@NotNull Entity entity) {
		if (ENTITY_GET_CUSTOM_NAME != null) {
			try {
				Object component = ENTITY_GET_CUSTOM_NAME.invoke(entity);
				return component instanceof Component ? (Component) component : null;
			} catch (Exception e) {
				return null;
			}
		}

		String legacyName = entity.getCustomName();
		return legacyName != null ? AdventureHelper.legacyToComponent(legacyName) : null;
	}

	public static void setEntityCustomName(@NotNull Entity entity, @Nullable Component component) {
		if (ENTITY_SET_CUSTOM_NAME != null) {
			try {
				ENTITY_SET_CUSTOM_NAME.invoke(entity, component);
				return;
			} catch (Exception ignored) {
			}
		}

		entity.setCustomName(component != null ? AdventureHelper.componentToLegacy(component) : null);
	}

	@Nullable
	public static Component getTileEntityCustomName(@NotNull Container container) {
		if (TILE_ENTITY_GET_CUSTOM_NAME != null) {
			try {
				Object component = TILE_ENTITY_GET_CUSTOM_NAME.invoke(container);
				return component instanceof Component ? (Component) component : null;
			} catch (Exception e) {
				return null;
			}
		}

		String legacyName = container.getCustomName();
		return legacyName != null ? AdventureHelper.legacyToComponent(legacyName) : null;
	}

	public static void setTileEntityCustomName(@NotNull Container container, @Nullable Component component) {
		if (TILE_ENTITY_SET_CUSTOM_NAME != null) {
			try {
				TILE_ENTITY_SET_CUSTOM_NAME.invoke(container, component);
				return;
			} catch (Exception ignored) {
			}
		}

		container.setCustomName(component != null ? AdventureHelper.componentToLegacy(component) : null);
	}

	@Nullable
	public static Component getCommandMinecartName(@NotNull CommandMinecart minecart) {
		if (CMD_MINECART_CUSTOM_NAME_GET != null) {
			try {
				Object component = CMD_MINECART_CUSTOM_NAME_GET.invoke(minecart);
				return component instanceof Component ? (Component) component : null;
			} catch (Exception ignored) {
			}
		}

		String legacyName = minecart.getCustomName();
		return legacyName != null ? AdventureHelper.legacyToComponent(legacyName) : null;
	}

	public static void setCommandMinecartName(@NotNull CommandMinecart minecart, @Nullable Component name) {
		if (CMD_MINECART_CUSTOM_NAME_SET != null) {
			try {
				CMD_MINECART_CUSTOM_NAME_SET.invoke(minecart, name);
				return;
			} catch (Exception ignored) {
			}
		}

		minecart.setCustomName(name != null ? AdventureHelper.componentToLegacy(name) : null);
	}

	public static String serialize(Component component) {
		return AdventureHelper.componentToJson(component);
	}

	public static Component deserialize(String input) {
		try {
			return AdventureHelper.jsonToComponent(input);
		} catch (Exception e) {
			return AdventureHelper.legacyToComponent(input);
		}
	}
}