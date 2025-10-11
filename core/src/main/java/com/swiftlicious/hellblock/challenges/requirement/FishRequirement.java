package com.swiftlicious.hellblock.challenges.requirement;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeRequirement;
import com.swiftlicious.hellblock.challenges.ItemResolver;
import com.swiftlicious.hellblock.context.Context;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class FishRequirement implements ChallengeRequirement {
	private final String itemId;
	private final String entityId;
	private final String blockId;
	private final boolean expectsItem;
	private final boolean expectsEntity;
	private final boolean expectsBlock;

	// Global caches
	private static final Map<String, Section> ITEM_CACHE = new HashMap<>();
	private static final Map<String, EntityDefinition> ENTITY_CACHE = new HashMap<>();
	private static final Map<String, BlockData> BLOCK_CACHE = new HashMap<>();
	private static volatile boolean cacheLoaded = false;

	// Per-requirement resolved targets
	private final Section cachedItemSec;
	private final String cachedEntityId;
	private final BlockData cachedBlock;
	private final boolean cachedIsMythic;

	private final ItemResolver resolver;

	public FishRequirement(Section data, ItemResolver resolver) {
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

		// Preload if not yet done
		if (!cacheLoaded) {
			preloadAsync(HellblockPlugin.getInstance());
		}

		// Resolve for this requirement
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

	public boolean matchesWithContext(Object context, @Nullable Context<Player> ctx) {
		// Item match
		if (expectsItem && (context instanceof ItemStack || context instanceof Item)) {
			ItemStack expected = resolveExpectedItemWithContext(ctx);
			if (expected == null)
				return false;

			ItemStack compared = (context instanceof ItemStack) ? (ItemStack) context : ((Item) context).getItemStack();
			return expected.isSimilar(compared);
		}
		// Block match
		if (expectsBlock && context instanceof BlockData block) {
			return cachedBlock != null && cachedBlock.matches(block);
		}

		// Entity match
		if (expectsEntity && context instanceof Entity ent) {
			if (cachedEntityId == null)
				return false;

			if (cachedIsMythic) {
				return isMythicMob(ent, cachedEntityId);
			} else {
				EntityType et = ent.getType();
				return et != null && et.name().equalsIgnoreCase(cachedEntityId);
			}
		}

		return false;
	}

	@Override
	public boolean matches(Object context) {
		HellblockPlugin.getInstance().getPluginLogger()
				.warn("FishRequirement#matches called without player context; using Context.empty()");
		return matchesWithContext(context, Context.empty());
	}

	private ItemStack resolveExpectedItemWithContext(Context<Player> ctx) {
		return cachedItemSec != null ? resolver.resolveItemStack(cachedItemSec, ctx) : null;
	}

	/**
	 * Asynchronously preload items/entities from contents folder.
	 */
	private void preloadAsync(HellblockPlugin plugin) {
		CompletableFuture.runAsync(() -> reloadContents(plugin)).exceptionally(ex -> {
			plugin.getPluginLogger().severe("FishRequirement async preload failed: " + ex.getMessage());
			return null;
		});
	}

	/**
	 * Clear and reload caches synchronously.
	 */
	public synchronized static void reloadContents(HellblockPlugin plugin) {
		ITEM_CACHE.clear();
		BLOCK_CACHE.clear();
		ENTITY_CACHE.clear();

		File contentsRoot = new File(plugin.getDataFolder(), "contents");

		// Items
		File itemsFolder = new File(contentsRoot, "item");
		if (itemsFolder.exists()) {
			loadItemsFromFolder(itemsFolder, plugin);
		}

		// Blocks
		File blocksFolder = new File(contentsRoot, "block");
		if (blocksFolder.exists()) {
			loadBlocksFromFolder(blocksFolder, plugin);
		}

		// Entities
		File entitiesFolder = new File(contentsRoot, "entity");
		if (entitiesFolder.exists()) {
			loadEntitiesFromFolder(entitiesFolder, plugin);
		}

		cacheLoaded = true;
		plugin.getPluginLogger()
				.info(String.format("FishRequirement: loaded %d item%s, %d block%s, %d entit%s.", ITEM_CACHE.size(),
						ITEM_CACHE.size() == 1 ? "" : "s", BLOCK_CACHE.size(), BLOCK_CACHE.size() == 1 ? "" : "s",
						ENTITY_CACHE.size(), ENTITY_CACHE.size() == 1 ? "y" : "ies"));
	}

	private static void loadItemsFromFolder(File folder, HellblockPlugin plugin) {
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

	private static void loadBlocksFromFolder(File folder, HellblockPlugin plugin) {
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

					try {
						String rawBlock = blockSec.getString("block", "").trim();
						if (rawBlock.isEmpty())
							continue;

						// Parse into BlockData instead of just Material
						BlockData blockData = Bukkit.createBlockData(rawBlock.toLowerCase(Locale.ROOT));
						if (blockData == null) {
							plugin.getPluginLogger().warn("Unknown block type '" + rawBlock + "' in " + file.getName());
							continue;
						}

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

	private static void loadEntitiesFromFolder(File folder, HellblockPlugin plugin) {
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

	private boolean isMythicMob(Entity ent, String targetId) {
		if (!HellblockPlugin.getInstance().getIntegrationManager().isHooked("MythicMobs"))
			return false;

		try {
			Class<?> mythicClass = findClass("io.lumine.mythic.bukkit.MythicBukkit",
					"io.lumine.xikage.mythicmobs.MythicBukkit");
			if (mythicClass == null)
				return false;

			Method inst = mythicClass.getMethod("inst");
			Object instObj = inst.invoke(null);
			Method getMobManager = instObj.getClass().getMethod("getMobManager");
			Object mobManager = getMobManager.invoke(instObj);

			Method getActiveMob = mobManager.getClass().getMethod("getActiveMob", UUID.class);
			Object maybeActive = getActiveMob.invoke(mobManager, ent.getUniqueId());

			if (maybeActive instanceof Optional<?> opt && opt.isPresent()) {
				Object activeMob = opt.get();
				Method getType = activeMob.getClass().getMethod("getType");
				Object typeObj = getType.invoke(activeMob);
				Method getInternalName = typeObj.getClass().getMethod("getInternalName");
				String name = (String) getInternalName.invoke(typeObj);
				return name.equalsIgnoreCase(targetId);
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	@Nullable
	private Class<?> findClass(String... names) {
		for (String n : names) {
			try {
				return Class.forName(n);
			} catch (ClassNotFoundException ignored) {
			}
		}
		return null;
	}

	private final static class EntityDefinition {
		final String id;
		final boolean isMythic;

		EntityDefinition(String id, boolean isMythic) {
			this.id = id;
			this.isMythic = isMythic;
		}
	}
}