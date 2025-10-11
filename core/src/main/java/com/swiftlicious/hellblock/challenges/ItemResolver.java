package com.swiftlicious.hellblock.challenges;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.ConfigRegistry;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ItemResolver {
	private final ConfigRegistry registry;
	private final HellblockPlugin plugin;

	public ItemResolver(HellblockPlugin plugin, ConfigRegistry registry) {
		this.plugin = plugin;
		this.registry = registry;
	}

	/**
	 * Resolves an ItemStack based on boosted-yaml Section configuration.
	 *
	 * - "item": always vanilla Material - "id": always registry/custom item -
	 * inline definitions ("material", "components", "display") are also supported
	 */
	public ItemStack resolveItemStack(Section data, @Nullable Context<Player> ctx) {
		if (data == null) {
			throw new IllegalArgumentException("data section is null");
		}

		// Inline definition
		if (data.contains("material") || data.contains("components") || data.contains("display")) {
			final CustomItem ci = new SingleItemParser("challenge-inline-item", data,
					plugin.getConfigManager().getItemFormatFunctions()).getItem();
			return ci.build(ctx);
		}

		// Vanilla item
		if (data.contains("item")) {
			final String matName = data.getString("item", "").toUpperCase(Locale.ROOT);
			final Material mat = Material.matchMaterial(matName);
			return (mat != null) ? new ItemStack(mat) : null;
		}

		// Custom item
		if (data.contains("id")) {
			final String id = data.getString("id").trim();
			final Section def = registry.getSection(id);

			if (def != null) {
				final CustomItem ci = new SingleItemParser(id, def, plugin.getConfigManager().getItemFormatFunctions())
						.getItem();
				return ci.build(ctx);
			}
			return null;
		}

		return null;
	}
}