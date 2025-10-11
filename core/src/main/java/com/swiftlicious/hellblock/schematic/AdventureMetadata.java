package com.swiftlicious.hellblock.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.block.CommandBlock;
import org.bukkit.block.EnchantingTable;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Wither;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.inventory.meta.BookMeta;

import com.swiftlicious.hellblock.handlers.AdventureHelper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

@SuppressWarnings("deprecation")
public class AdventureMetadata {

	private static final boolean PAPER_ADVENTURE_SUPPORTED;

	static {
		boolean supported;
		try {
			BookMeta.class.getMethod("pages", List.class);
			SignSide.class.getMethod("line", int.class, Component.class);
			Entity.class.getMethod("customName", Component.class);
			supported = true;
		} catch (NoSuchMethodException e) {
			supported = false;
		}
		PAPER_ADVENTURE_SUPPORTED = supported;
	}

	public static boolean isAdventureSupported() {
		return PAPER_ADVENTURE_SUPPORTED;
	}

	private static final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

	public static Component getEnchantingTableName(EnchantingTable table) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			return table.customName();
		}
		String legacyName = table.getCustomName();
		return legacyName != null ? legacy.deserialize(legacyName) : null;
	}

	public static void setEnchantingTableName(EnchantingTable table, Component name) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			table.customName(name);
		} else {
			table.setCustomName(name != null ? legacy.serialize(name) : null);
		}
	}

	public static Component getCommandBlockName(CommandBlock block) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			return block.name();
		}
		return legacy.deserialize(block.getName());
	}

	public static void setCommandBlockName(CommandBlock block, Component name) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			block.name(name);
		} else {
			block.setName(name != null ? legacy.serialize(name) : null);
		}
	}

	public static void setBookPages(BookMeta meta, List<Component> pages) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			meta.pages(pages);
		} else {
			List<String> legacy = pages.stream().map(AdventureMetadata.legacy::serialize).collect(Collectors.toList());
			meta.setPages(legacy);
		}
	}

	public static List<Component> getSignLines(SignSide side) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			return side.lines();
		} else {
			List<Component> list = new ArrayList<>();
			for (int i = 0; i < 4; i++) {
				list.add(legacy.deserialize(side.getLine(i)));
			}
			return list;
		}
	}

	public static void setSignLine(SignSide side, int line, Component component) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			side.line(line, component);
		} else {
			side.setLine(line, component != null ? legacy.serialize(component) : null);
		}
	}

	public static Component getEntityCustomName(Entity entity) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			return entity.customName();
		} else {
			String legacyName = entity.getCustomName();
			return legacyName != null ? legacy.deserialize(legacyName) : null;
		}
	}

	public static void setEntityCustomName(Entity entity, Component name) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			entity.customName(name);
		} else {
			entity.setCustomName(name != null ? legacy.serialize(name) : null);
		}
	}

	public static Component getCommandMinecartName(CommandMinecart minecart) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			return minecart.customName();
		}
		String legacyName = minecart.getCustomName();
		return legacyName != null ? legacy.deserialize(legacyName) : null;
	}

	public static void setCommandMinecartName(CommandMinecart minecart, Component name) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			minecart.customName(name);
		} else {
			minecart.setCustomName(name != null ? legacy.serialize(name) : null);
		}
	}

	public static int getVexLifetimeTicks(Vex vex) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			return vex.getLimitedLifetimeTicks();
		} else {
			return vex.getLifeTicks();
		}
	}

	public static void setVexLifetimeTicks(Vex vex, int ticks) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			vex.setLimitedLifetimeTicks(ticks);
		} else {
			vex.setLifeTicks(ticks);
		}
	}

	@SuppressWarnings("removal")
	public static int getWitherInvulnerableTicks(Wither wither) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			return wither.getInvulnerableTicks();
		} else {
			return wither.getInvulnerabilityTicks();
		}
	}

	@SuppressWarnings("removal")
	public static void setWitherInvulnerableTicks(Wither wither, int ticks) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			wither.setInvulnerableTicks(ticks);
		} else {
			wither.setInvulnerabilityTicks(ticks);
		}
	}

	public static UUID getHorseOwnerUUID(AbstractHorse horse) {
		if (PAPER_ADVENTURE_SUPPORTED) {
			return horse.getOwnerUniqueId() != null ? horse.getOwnerUniqueId() : null;
		} else {
			AnimalTamer owner = horse.getOwner();
			if (owner == null) {
				return null;
			}
			UUID legacyID = owner.getUniqueId();
			return legacyID;
		}
	}

	public static String serialize(Component component) {
		return AdventureHelper.getGson().serialize(component);
	}

	public static Component deserialize(String input) {
		try {
			return AdventureHelper.getGson().deserialize(input);
		} catch (Exception e) {
			return legacy.deserialize(input);
		}
	}
}