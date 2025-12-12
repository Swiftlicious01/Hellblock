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

/**
 * Resolves {@link ItemStack} definitions from YAML configuration data for use
 * in challenge requirements, rewards, and custom item systems.
 * <p>
 * This class supports multiple item definition formats:
 * </p>
 * <ul>
 * <li><b>Vanilla items</b> — via <code>item: STONE</code></li>
 * <li><b>Custom registered items</b> — via <code>id: my_custom_item</code></li>
 * <li><b>Inline item definitions</b> — via <code>material:</code>,
 * <code>components:</code>, and <code>display:</code> nodes</li>
 * </ul>
 * <p>
 * When resolving custom or inline items, this class delegates to
 * {@link SingleItemParser} to construct the appropriate {@link CustomItem}.
 * </p>
 */
public class ItemResolver {

	private final ConfigRegistry registry;
	private final HellblockPlugin plugin;

	public ItemResolver(HellblockPlugin plugin, ConfigRegistry registry) {
		this.plugin = plugin;
		this.registry = registry;
	}

	public HellblockPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Resolves an {@link ItemStack} from a given Boosted-YAML {@link Section}.
	 * <p>
	 * The resolution order is as follows:
	 * </p>
	 * <ol>
	 * <li>If the section contains <code>material</code>, <code>components</code>,
	 * or <code>display</code>, an <b>inline item</b> is constructed
	 * dynamically.</li>
	 * <li>If it contains <code>item</code>, a <b>vanilla Material</b> is used.</li>
	 * <li>If it contains <code>id</code>, a <b>custom item</b> from the registry is
	 * built.</li>
	 * </ol>
	 *
	 * @param data The YAML section containing item definition data.
	 * @param ctx  Optional player context (used for placeholder resolution and
	 *             component variables).
	 * @return The resolved {@link ItemStack}, or {@code null} if invalid or not
	 *         found.
	 * @throws IllegalArgumentException If the provided data section is null.
	 */
	@Nullable
	public ItemStack resolveItemStack(@Nullable Section data, @Nullable Context<Player> ctx) {
		if (data == null) {
			throw new IllegalArgumentException("data section is null");
		}

		// --- Inline item definition ---
		if (data.contains("material") || data.contains("components") || data.contains("display")) {
			final CustomItem ci = new SingleItemParser("challenge-inline-item", data,
					plugin.getConfigManager().getItemFormatFunctions()).getItem();
			return ci.build(ctx);
		}

		// --- Vanilla item definition ---
		if (data.contains("item")) {
			final String matName = data.getString("item", "").toUpperCase(Locale.ROOT);
			final Material mat = Material.matchMaterial(matName);
			return (mat != null) ? new ItemStack(mat) : null;
		}

		// --- Custom registry item definition ---
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