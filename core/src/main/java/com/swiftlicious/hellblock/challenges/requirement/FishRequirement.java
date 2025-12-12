package com.swiftlicious.hellblock.challenges.requirement;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeRequirement;
import com.swiftlicious.hellblock.challenges.ItemResolver;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.entity.EntityProvider;
import com.swiftlicious.hellblock.creation.entity.MythicEntityProvider;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Represents a challenge requirement where a player must successfully <b>fish
 * up</b> a specific item, entity, or block.
 * <p>
 * This requirement is used in "fishing" style challenges that trigger whenever
 * a player catches something via custom fishing mechanics (e.g., items,
 * MythicMobs, or blocks).
 * </p>
 *
 * <p>
 * <b>Example configurations (in challenges.yml):</b>
 * </p>
 *
 * <pre>
 *   FISH_RUBBISH:
 *     needed-amount: 5
 *     action: FISH
 *     data:
 *       item: rubbish
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: COAL
 *           amount: 8
 *
 *   FISH_SKELETON:
 *     needed-amount: 3
 *     action: FISH
 *     data:
 *       entity: skeleton
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: BONE
 *           amount: 5
 *
 *   FISH_MYTHIC_KNIGHT:
 *     needed-amount: 2
 *     action: FISH
 *     data:
 *       entity: skeletalknight
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: IRON_SWORD
 * </pre>
 *
 * <p>
 * In these examples:
 * <ul>
 * <li><b>FISH_RUBBISH</b> triggers when a player fishes an item defined as
 * <code>rubbish</code> in <code>contents/item/</code>.</li>
 * <li><b>FISH_SKELETON</b> triggers when a vanilla <code>SKELETON</code> is
 * pulled up.</li>
 * <li><b>FISH_MYTHIC_KNIGHT</b> triggers when a MythicMobs entity with ID
 * <code>skeletalknight</code> is caught.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Fishable types can be defined by:
 * <ul>
 * <li><b>item:</b> Custom or inline item (YAML under
 * <code>contents/item/</code>).</li>
 * <li><b>entity:</b> Vanilla or MythicMobs entity (YAML under
 * <code>contents/entity/</code>).</li>
 * <li><b>block:</b> Block data (YAML under <code>contents/block/</code>).</li>
 * </ul>
 * </p>
 */
public class FishRequirement implements ChallengeRequirement {

	private final String itemId;
	private final String entityId;
	private final String blockId;
	private final boolean expectsItem;
	private final boolean expectsEntity;
	private final boolean expectsBlock;

	// --- Global shared caches for fishable content ---
	private static final Map<String, Section> ITEM_CACHE = new HashMap<>();
	private static final Map<String, EntityDefinition> ENTITY_CACHE = new HashMap<>();
	private static final Map<String, BlockData> BLOCK_CACHE = new HashMap<>();

	private static volatile boolean cacheLoaded = false;

	// --- Cached per-instance values for quick comparison ---
	private final Section cachedItemSec;
	private final String cachedEntityId;
	private final BlockData cachedBlock;
	private final boolean cachedIsMythic;

	private final ItemResolver resolver;

	public FishRequirement(@NotNull Section data, @NotNull ItemResolver resolver) {
		this.itemId = data.getString("item", null);
		this.entityId = data.getString("entity", null);
		this.blockId = data.getString("block", null);
		this.expectsItem = this.itemId != null;
		this.expectsEntity = this.entityId != null;
		this.expectsBlock = this.blockId != null;
		this.resolver = resolver;

		if (!expectsItem && !expectsEntity && !expectsBlock) {
			throw new IllegalArgumentException("FISH requires either data.item, data.entity or data.block");
		}

		if (!cacheLoaded) {
			resolver.getPlugin().getPluginLogger().warn("FishRequirement cache was accessed before initialization — "
					+ "make sure FishRequirement.reloadContents(plugin) is called on startup!");
		}

		// Resolve cached reference for this requirement
		Section itCfg = null;
		BlockData blck = null;
		String entId = null;
		boolean isMythic = false;

		if (expectsItem) {
			itCfg = ITEM_CACHE.get(itemId.toLowerCase(Locale.ROOT));
		}
		if (expectsBlock) {
			blck = BLOCK_CACHE.get(blockId.toLowerCase(Locale.ROOT));
		}
		if (expectsEntity) {
			EntityDefinition def = ENTITY_CACHE.get(entityId.toLowerCase(Locale.ROOT));
			if (def != null) {
				entId = def.id;
				isMythic = def.isMythic;
			}
		}

		this.cachedItemSec = itCfg;
		this.cachedBlock = blck;
		this.cachedEntityId = entId;
		this.cachedIsMythic = isMythic;
	}

	/**
	 * Checks whether the provided context (e.g., caught item, entity, or block)
	 * matches this requirement.
	 * <p>
	 * This method supports:
	 * <ul>
	 * <li>{@link ItemStack} or {@link Item} — for caught items.</li>
	 * <li>{@link Entity} — for entities (vanilla or MythicMobs).</li>
	 * <li>{@link BlockData} — for blocks fished via lava systems.</li>
	 * </ul>
	 * </p>
	 */
	public boolean matchesWithContext(@NotNull Object context, @Nullable Context<Player> ctx) {
		// --- Item match ---
		if (expectsItem && (context instanceof ItemStack || context instanceof Item)) {
			ItemStack expected = resolveExpectedItemWithContext(ctx);
			if (expected == null)
				return false;

			ItemStack compared = (context instanceof ItemStack) ? (ItemStack) context : ((Item) context).getItemStack();
			return expected.isSimilar(compared);
		}

		// --- Block match ---
		if (expectsBlock && context instanceof BlockData block) {
			return cachedBlock != null && cachedBlock.matches(block);
		}

		// --- Entity match ---
		if (expectsEntity && context instanceof Entity ent) {
			if (cachedEntityId == null)
				return false;

			if (cachedIsMythic) {
				if (!this.resolver.getPlugin().getIntegrationManager().isHooked("MythicMobs", "5")) {
					return false;
				}
				EntityProvider provider = this.resolver.getPlugin().getEntityManager().getEntityProvider("MythicMobs");
				if (provider instanceof MythicEntityProvider mythicProvider)
					return mythicProvider.isMythicMob(ent, cachedEntityId);
			} else {
				EntityType et = ent.getType();
				return et.name().equalsIgnoreCase(cachedEntityId);
			}
		}

		return false;
	}

	@Override
	public boolean matches(@NotNull Object context) {
		this.resolver.getPlugin().getPluginLogger()
				.warn("FishRequirement#matches called without player context; using Context.empty()");
		return matchesWithContext(context, Context.playerEmpty());
	}

	/**
	 * Resolves the expected {@link ItemStack} from cached configuration data.
	 */
	@Nullable
	private ItemStack resolveExpectedItemWithContext(@NotNull Context<Player> ctx) {
		return cachedItemSec != null ? resolver.resolveItemStack(cachedItemSec, ctx) : null;
	}

	/**
	 * Clears and reloads all fishing-related caches synchronously. Called
	 * automatically on first construction or via manual reload.
	 */
	public static synchronized void reloadContents(@NotNull HellblockPlugin plugin) {
		ITEM_CACHE.clear();
		BLOCK_CACHE.clear();
		ENTITY_CACHE.clear();

		File contentsRoot = new File(plugin.getDataFolder(), "contents");
		if (contentsRoot.exists()) {

			// --- Items ---
			File itemsFolder = new File(contentsRoot, "item");
			if (itemsFolder.exists())
				loadItemsFromFolder(itemsFolder, plugin);

			// --- Blocks ---
			File blocksFolder = new File(contentsRoot, "block");
			if (blocksFolder.exists())
				loadBlocksFromFolder(blocksFolder, plugin);

			// --- Entities ---
			File entitiesFolder = new File(contentsRoot, "entity");
			if (entitiesFolder.exists())
				loadEntitiesFromFolder(entitiesFolder, plugin);

			cacheLoaded = true;
			int items = ITEM_CACHE.size();
			int blocks = BLOCK_CACHE.size();
			int entities = ENTITY_CACHE.size();

			List<String> parts = new ArrayList<>();
			if (items > 0)
				parts.add(items + " item" + (items == 1 ? "" : "s"));
			if (blocks > 0)
				parts.add(blocks + " block" + (blocks == 1 ? "" : "s"));
			if (entities > 0)
				parts.add(entities + " entit" + (entities == 1 ? "y" : "ies"));

			if (parts.isEmpty()) {
				plugin.debug("FishRequirement Challenge: No contents found to load.");
			} else {
				String message;
				if (parts.size() == 1) {
					message = parts.get(0);
				} else if (parts.size() == 2) {
					message = parts.get(0) + " and " + parts.get(1);
				} else {
					message = String.join(", ", parts.subList(0, parts.size() - 1)) + " and "
							+ parts.get(parts.size() - 1);
				}
				plugin.debug("FishRequirement Challenge: loaded " + message + ".");
			}
		}
	}

	/** Loads all custom items from the contents/item/ directory. */
	private static void loadItemsFromFolder(@Nullable File folder, @NotNull HellblockPlugin plugin) {
		if (folder == null || !folder.isDirectory()) {
			plugin.getPluginLogger().warn("Item folder is invalid or does not exist: " + folder);
			return;
		}
		File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
		if (files == null)
			return;

		for (File file : files) {
			try {
				YamlDocument doc = YamlDocument.create(file);
				for (String key : doc.getRoutesAsStrings(false)) {
					Section itemSec = doc.getSection(key);
					if (itemSec == null)
						continue;
					ITEM_CACHE.put(key.toLowerCase(Locale.ROOT), itemSec);
				}
			} catch (Throwable t) {
				plugin.getPluginLogger().warn("Failed to parse items file " + file.getName(), t);
			}
		}
	}

	/** Loads all custom blocks from the contents/block/ directory. */
	private static void loadBlocksFromFolder(@Nullable File folder, @NotNull HellblockPlugin plugin) {
		if (folder == null || !folder.isDirectory()) {
			plugin.getPluginLogger().warn("Block folder is invalid or does not exist: " + folder);
			return;
		}
		File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
		if (files == null)
			return;

		for (File file : files) {
			try {
				YamlDocument doc = YamlDocument.create(file);
				for (String key : doc.getRoutesAsStrings(false)) {
					Section blockSec = doc.getSection(key);
					if (blockSec == null)
						continue;

					String rawBlock = blockSec.getString("block", "").trim();
					if (rawBlock.isEmpty())
						continue;

					try {
						BlockData blockData = Bukkit.createBlockData(rawBlock.toLowerCase(Locale.ROOT));
						BLOCK_CACHE.put(key.toLowerCase(Locale.ROOT), blockData);
					} catch (Throwable t) {
						plugin.getPluginLogger().warn("Failed to load block '" + key + "' from " + file.getName(), t);
					}
				}
			} catch (Throwable t) {
				plugin.getPluginLogger().warn("Failed to parse blocks file " + file.getName(), t);
			}
		}
	}

	/** Loads all custom entities from the contents/entity/ directory. */
	private static void loadEntitiesFromFolder(@Nullable File folder, @NotNull HellblockPlugin plugin) {
		if (folder == null || !folder.isDirectory()) {
			plugin.getPluginLogger().warn("Entity folder is invalid or does not exist: " + folder);
			return;
		}
		File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
		if (files == null)
			return;

		for (File file : files) {
			try {
				YamlDocument doc = YamlDocument.create(file);
				for (String key : doc.getRoutesAsStrings(false)) {
					Section entSec = doc.getSection(key);
					if (entSec == null)
						continue;

					String rawEntity = entSec.getString("entity", "").trim();
					if (rawEntity.isEmpty())
						continue;

					boolean isMythic = rawEntity.startsWith("MythicMobs:");
					String resolvedId = isMythic ? rawEntity.substring("MythicMobs:".length()).trim()
							: rawEntity.toUpperCase(Locale.ROOT);

					ENTITY_CACHE.put(key.toLowerCase(Locale.ROOT), new EntityDefinition(resolvedId, isMythic));
				}
			} catch (Throwable t) {
				plugin.getPluginLogger().warn("Failed to parse entities file " + file.getName(), t);
			}
		}
	}

	/** Simple data record for an entity definition loaded from contents/entity/. */
	private static final class EntityDefinition {
		final String id;
		final boolean isMythic;

		EntityDefinition(@NotNull String id, boolean isMythic) {
			this.id = id;
			this.isMythic = isMythic;
		}
	}
}