package com.swiftlicious.hellblock.creation.block;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Note;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.addons.ExternalProvider;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class BlockManager implements BlockManagerInterface, Listener {

	protected final HellblockPlugin instance;
	private final Map<String, BlockProvider> blockProviders = new HashMap<>();
	private final Map<String, BlockConfig> blocks = new HashMap<>();
	private final Map<String, BlockDataModifierFactory> dataFactories = new HashMap<>();
	private final Map<String, BlockStateModifierFactory> stateFactories = new HashMap<>();
	private BlockProvider[] blockDetectArray = new BlockProvider[0];

	public BlockManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public void registerProviders() {
		this.registerBuiltInProperties();
		this.registerBlockProvider(new BlockProvider() {
			@Override
			public String identifier() {
				return "vanilla";
			}

			@Override
			public BlockData blockData(@NotNull Context<Player> context, @NotNull String id,
					List<BlockDataModifier> modifiers) {
				Material material;
				try {
					material = Material.valueOf(id.toUpperCase(Locale.ENGLISH));
				} catch (IllegalArgumentException e) {
					material = Registry.MATERIAL.get(new NamespacedKey("minecraft", id.toLowerCase(Locale.ENGLISH)));
				}
				if (material == null) {
					throw new IllegalArgumentException("Material " + id + " is not a valid material");
				}
				final BlockData blockData = material.createBlockData();
				modifiers.forEach(modifier -> modifier.apply(context, blockData));
				return blockData;
			}

			@NotNull
			@Override
			public String blockID(@NotNull Block block) {
				return block.getType().name();
			}
		});
	}

	@Nullable
	@Override
	public BlockDataModifierFactory getBlockDataModifierFactory(@NotNull String id) {
		return dataFactories.get(id);
	}

	@Nullable
	@Override
	public BlockStateModifierFactory getBlockStateModifierFactory(@NotNull String id) {
		return stateFactories.get(id);
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		this.resetBlockDetectionOrder();
		blockProviders.values()
				.forEach(provider -> instance.debug("Registered BlockProvider: " + provider.identifier()));
		instance.debug(blocks.size() > 0 ? "Loaded " + blocks.size() + " block" + (blocks.size() == 1 ? "" : "s")
				: "No blocks found to load");
		if (blockDetectArray.length > 0) {
			instance.debug("Block order: " + Arrays.toString(
					Arrays.stream(blockDetectArray).map(ExternalProvider::identifier).toList().toArray(new String[0])));
		}
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.blocks.clear();
	}

	@Override
	public void disable() {
		unload();
		this.blockProviders.clear();
	}

	private void resetBlockDetectionOrder() {
		final List<BlockProvider> list = new ArrayList<>();
		for (String plugin : instance.getConfigManager().blockDetectOrder()) {
			final BlockProvider library = blockProviders.get(plugin);
			if (library != null) {
				list.add(library);
			}
		}
		this.blockDetectArray = list.toArray(new BlockProvider[0]);
	}

	@Override
	public boolean registerBlock(@NotNull BlockConfig block) {
		if (blocks.containsKey(block.id())) {
			return false;
		}
		blocks.put(block.id(), block);
		return true;
	}

	/**
	 * Event handler for the EntityChangeBlockEvent. This method is triggered when
	 * an entity changes a block, typically when a block falls or lands.
	 */
	@EventHandler
	public void onBlockLands(EntityChangeBlockEvent event) {
		if (event.isCancelled()) {
			return;
		}

		// Retrieve a custom string value stored in the entity's persistent data
		// container.
		final String temp = event.getEntity().getPersistentDataContainer()
				.get(requireNonNull(NamespacedKey.fromString("block", instance)), PersistentDataType.STRING);

		// If the custom string value is not present, return without further action.
		if (temp == null) {
			return;
		}

		// "BLOCK;PLAYER"
		final String[] split = temp.split(";");

		// If no BlockConfig is found for the specified key, return without further
		// action.
		final BlockConfig blockConfig = blocks.get(split[0]);
		if (blockConfig == null) {
			return;
		}

		// If the player is not online or not found, remove the entity and set the block
		// to air
		final Player player = Bukkit.getPlayer(split[1]);
		if (player == null) {
			event.getEntity().remove();
			event.getBlock().setType(Material.AIR);
			return;
		}

		final Context<Player> context = Context.player(player);
		final Location location = event.getBlock().getLocation();

		// Apply block state modifiers from the BlockConfig to the block 1 tick later.
		instance.getScheduler().sync().runLater(() -> {
			final BlockState state = location.getBlock().getState();
			blockConfig.stateModifiers().forEach(modifier -> modifier.apply(context, state));
		}, 1, location);
	}

	public boolean registerBlockProvider(BlockProvider blockProvider) {
		if (this.blockProviders.containsKey(blockProvider.identifier())) {
			return false;
		}
		this.blockProviders.put(blockProvider.identifier(), blockProvider);
		this.resetBlockDetectionOrder();
		return true;
	}

	public boolean unregisterBlockProvider(String identification) {
		final boolean success = blockProviders.remove(identification) != null;
		if (success) {
			this.resetBlockDetectionOrder();
		}
		return success;
	}

	public BlockProvider getBlockProvider(String id) {
		return blockProviders.get(id);
	}

	public boolean registerBlockDataModifierBuilder(String type, BlockDataModifierFactory factory) {
		if (this.dataFactories.containsKey(type)) {
			return false;
		}
		this.dataFactories.put(type, factory);
		return true;
	}

	public boolean registerBlockStateModifierBuilder(String type, BlockStateModifierFactory factory) {
		if (stateFactories.containsKey(type)) {
			return false;
		}
		this.stateFactories.put(type, factory);
		return true;
	}

	public boolean unregisterBlockDataModifierBuilder(String type) {
		return this.dataFactories.remove(type) != null;
	}

	public boolean unregisterBlockStateModifierBuilder(String type) {
		return this.stateFactories.remove(type) != null;
	}

	private void registerBuiltInProperties() {
		this.registerDirectional();
		this.registerStorage();
		this.registerRotatable();
		this.registerNoteBlock();
	}

	@SuppressWarnings("deprecation")
	@Override
	@NotNull
	public FallingBlock summonBlockLoot(@NotNull Context<Player> context) {
		final String id = context.arg(ContextKeys.ID);
		final BlockConfig config = requireNonNull(blocks.get(id), "Block " + id + " not found");
		final String blockID = config.blockID();
		final BlockData blockData;
		if (blockID.contains(":")) {
			final String[] split = blockID.split(":", 2);
			final BlockProvider provider = requireNonNull(blockProviders.get(split[0]),
					"BlockProvider " + split[0] + " doesn't exist");
			blockData = requireNonNull(provider.blockData(context, split[1], config.dataModifier()),
					"Block " + split[1] + " doesn't exist");
		} else {
			blockData = blockProviders.get("vanilla").blockData(context, blockID, config.dataModifier());
		}
		final Location hookLocation = requireNonNull(context.arg(ContextKeys.OTHER_LOCATION));
		final Location playerLocation = requireNonNull(context.holder()).getLocation();
		FallingBlock fallingBlock = hookLocation.getWorld().spawnFallingBlock(hookLocation, blockData);
		fallingBlock.setDropItem(false);
		fallingBlock.setHurtEntities(false);
		fallingBlock.getPersistentDataContainer().set(requireNonNull(NamespacedKey.fromString("block", instance)),
				PersistentDataType.STRING, id + ";" + context.holder().getName());
		final double d0 = playerLocation.getX() - hookLocation.getX();
		final double d1 = playerLocation.getY() - hookLocation.getY();
		final double d2 = playerLocation.getZ() - hookLocation.getZ();
		final double d3 = config.horizontalVector().evaluate(context);
		final double d4 = config.verticalVector().evaluate(context);
		final Vector vector = new Vector(d0 * 0.1D * d3,
				d1 * 0.1D + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08D * d4, d2 * 0.1D * d3);
		fallingBlock.setVelocity(vector);
		return fallingBlock;
	}

	@Override
	@NotNull
	public String getBlockID(@NotNull Block block) {
		for (BlockProvider blockProvider : blockDetectArray) {
			final String id = blockProvider.blockID(block);
			if (id != null) {
				return id;
			}
		}
		// Should not reach this because vanilla library would always work
		return "AIR";
	}

	private void registerDirectional() {
		this.registerBlockDataModifierBuilder("directional", (args) -> (context, blockData) -> {
			final boolean arg = (boolean) args;
			if (arg && blockData instanceof Directional directional) {
				directional.setFacing(BlockFace.values()[RandomUtils.generateRandomInt(0, 3)]);
			}
		});
		this.registerBlockDataModifierBuilder("directional-4", (args) -> (context, blockData) -> {
			final boolean arg = (boolean) args;
			if (arg && blockData instanceof Directional directional) {
				directional.setFacing(BlockFace.values()[RandomUtils.generateRandomInt(0, 3)]);
			}
		});
		this.registerBlockDataModifierBuilder("directional-6", (args) -> (context, blockData) -> {
			final boolean arg = (boolean) args;
			if (arg && blockData instanceof Directional directional) {
				directional.setFacing(BlockFace.values()[RandomUtils.generateRandomInt(0, 5)]);
			}
		});
	}

	private void registerRotatable() {
		this.registerBlockDataModifierBuilder("rotatable", (args) -> {
			final boolean arg = (boolean) args;
			return (context, blockData) -> {
				if (arg && blockData instanceof Rotatable rotatable) {
					rotatable.setRotation(BlockFace.values()[RandomUtils.generateRandomInt(BlockFace.values().length)]);
				}
			};
		});
	}

	private void registerNoteBlock() {
		this.registerBlockDataModifierBuilder("noteblock", (args) -> {
			if (args instanceof Section section) {
				final var instrument = Instrument.valueOf(section.getString("instrument"));
				final var note = new Note(section.getInt("note"));
				return (context, blockData) -> {
					if (blockData instanceof NoteBlock noteBlock) {
						noteBlock.setNote(note);
						noteBlock.setInstrument(instrument);
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at noteblock property which should be Section");
				return EmptyBlockDataModifier.INSTANCE;
			}
		});
	}

	private void registerStorage() {
		this.registerBlockStateModifierBuilder("storage", (args) -> {
			if (args instanceof Section section) {
				final List<Tuple<MathValue<Player>, String, MathValue<Player>>> contents = new ArrayList<>();
				section.getStringRouteMappedValues(false).entrySet().forEach(entry -> {
					if (entry.getValue() instanceof Section inner) {
						final String item = inner.getString("item");
						final MathValue<Player> amount = MathValue.auto(inner.getString("amount", "1~1"), true);
						final MathValue<Player> chance = MathValue.auto(inner.get("chance", 1d));
						contents.add(Tuple.of(chance, item, amount));
					}
				});
				return (context, blockState) -> {
					if (blockState instanceof Container container) {
						setInventoryItems(contents, context, container.getInventory());
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at storage property which should be Section");
				return EmptyBlockStateModifier.INSTANCE;
			}
		});
	}

	private void setInventoryItems(List<Tuple<MathValue<Player>, String, MathValue<Player>>> contents,
			Context<Player> context, Inventory inventory) {
		final LinkedList<Integer> unused = new LinkedList<>();
		for (int i = 0; i < inventory.getSize(); i++) {
			unused.add(i);
		}
		Collections.shuffle(unused);
		contents.stream().filter(tuple -> tuple.left().evaluate(context) > Math.random()).forEach(tuple -> {
			final ItemStack itemStack = instance.getItemManager().buildAny(context, tuple.mid());
			if (itemStack != null) {
				itemStack.setAmount((int) tuple.right().evaluate(context));
				inventory.setItem(unused.pop(), itemStack);
			}
		});
	}
}