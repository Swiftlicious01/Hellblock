package com.swiftlicious.hellblock.spigot.v1_20_r2;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftFishHook;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftWither;
import org.bukkit.craftbukkit.v1_20_R2.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftContainer;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftChatMessage;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftLocation;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftVector;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.nms.NMSHandler;
import com.swiftlicious.hellblock.nms.border.BorderColor;
import com.swiftlicious.hellblock.nms.bossbar.BossBarColor;
import com.swiftlicious.hellblock.nms.bossbar.BossBarOverlay;
import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.nms.entity.display.FakeItemDisplay;
import com.swiftlicious.hellblock.nms.entity.display.FakeTextDisplay;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.nms.fluid.FluidData;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.nms.util.BossBarUtils;
import com.swiftlicious.hellblock.nms.util.ReflectionUtils;
import com.swiftlicious.hellblock.nms.util.SelfIncreaseEntityID;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class NMSUtils1_20_R2 implements NMSHandler {

	private final Enum<?> addBossBarOperation;
	private final Enum<?> updateBossBarNameOperation;
	private final Enum<?> updateBossBarProgressOperation;

	private final EntityDataAccessor<Boolean> dataBiting;
	private final Field structureStartsField;

	private final Field fishingHookOwnerUUIDField;

	private Field connectionField;
	private Field channelField;

	@SuppressWarnings("unchecked")
	public NMSUtils1_20_R2() {
		try {
			Class<?> cls = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutBoss$d");
			Field fieldAdd = cls.getDeclaredField("a");
			fieldAdd.setAccessible(true);
			addBossBarOperation = (Enum<?>) fieldAdd.get(null);
			Field fieldUpdateProgress = cls.getDeclaredField("c");
			fieldUpdateProgress.setAccessible(true);
			updateBossBarProgressOperation = (Enum<?>) fieldUpdateProgress.get(null);
			Field fieldUpdateName = cls.getDeclaredField("d");
			fieldUpdateName.setAccessible(true);
			updateBossBarNameOperation = (Enum<?>) fieldUpdateName.get(null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get add boss bar operation", e);
		}
		try {
			structureStartsField = ReflectionUtils.getDeclaredField(ChunkAccess.class, "structureStarts");
			structureStartsField.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get structureStarts field", e);
		}
		try {
			Field dataBitingField = FishingHook.class.getDeclaredField("h");
			dataBitingField.setAccessible(true);
			dataBiting = (EntityDataAccessor<Boolean>) dataBitingField.get(null);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to get hook biting state", e);
		}
		try {
			fishingHookOwnerUUIDField = FishingHook.class.getDeclaredField("ownerUUID");
			fishingHookOwnerUUIDField.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException("Could not access FishingHook.ownerUUID", e);
		}
		try {
			// Step 1: Find Connection field in ServerGamePacketListenerImpl
			for (Field field : ServerGamePacketListenerImpl.class.getDeclaredFields()) {
				if (Connection.class.isAssignableFrom(field.getType())) {
					field.setAccessible(true);
					connectionField = field;
					break;
				}
			}

			if (connectionField == null) {
				throw new IllegalStateException("Connection field not found in ServerGamePacketListenerImpl");
			}

			// Step 2: Find Channel field in Connection
			for (Field field : Connection.class.getDeclaredFields()) {
				if (Channel.class.isAssignableFrom(field.getType())) {
					field.setAccessible(true);
					channelField = field;
					break;
				}
			}

			if (channelField == null) {
				throw new IllegalStateException("Channel field not found in Connection");
			}

		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize ChannelAccess", e);
		}
		FluidDataInstance.register(LavaFluid.Source.class, FallingFluidDataInstance::new);
		FluidDataInstance.register(WaterFluid.Source.class, FallingFluidDataInstance::new);
		FluidDataInstance.register(LavaFluid.Flowing.class, FlowingFluidDataInstance::new);
		FluidDataInstance.register(WaterFluid.Flowing.class, FlowingFluidDataInstance::new);
	}

	@Override
	public void sendMessage(Player player, String messageJson) {
		ClientboundSystemChatPacket systemChatPacket = new ClientboundSystemChatPacket(
				CraftChatMessage.fromJSON(messageJson), false);
		((CraftPlayer) player).getHandle().connection.send(systemChatPacket);
	}

	public Channel getChannel(ServerPlayer player) {
		try {
			Connection connection = (Connection) connectionField.get(player.connection);
			return (Channel) channelField.get(connection);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get channel from ServerPlayer", e);
		}
	}

	private void sendPacketImmediately(ServerPlayer serverPlayer, Packet<ClientGamePacketListener> packet) {
		try {
			Channel channel = getChannel(serverPlayer);
			channel.writeAndFlush(packet);
		} catch (Exception e) {
			throw new RuntimeException("Failed to send raw packet via channel", e);
		}
	}

	@Override
	public void sendActionBar(Player player, String json) {
		CraftPlayer craftPlayer = (CraftPlayer) player;
		ServerPlayer serverPlayer = craftPlayer.getHandle();
		ClientboundSetActionBarTextPacket packet = new ClientboundSetActionBarTextPacket(
				Objects.requireNonNull(Component.Serializer.fromJson(json)));
		serverPlayer.connection.send(packet);
	}

	@Override
	public void sendTitle(Player player, @Nullable String titleJson, @Nullable String subTitleJson, int fadeInTicks,
			int stayTicks, int fadeOutTicks) {
		CraftPlayer craftPlayer = (CraftPlayer) player;
		ServerPlayer serverPlayer = craftPlayer.getHandle();
		ArrayList<Packet<ClientGamePacketListener>> packetListeners = new ArrayList<>();
		packetListeners.add(new ClientboundSetTitlesAnimationPacket(fadeInTicks, stayTicks, fadeOutTicks));
		if (titleJson != null) {
			packetListeners.add(new ClientboundSetTitleTextPacket(
					Objects.requireNonNull(Component.Serializer.fromJson(titleJson))));
		} else {
			packetListeners.add(new ClientboundSetTitleTextPacket(Objects.requireNonNull(Component.empty())));
		}
		if (subTitleJson != null) {
			packetListeners.add(new ClientboundSetSubtitleTextPacket(
					Objects.requireNonNull(Component.Serializer.fromJson(subTitleJson))));
		}
		ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(packetListeners);
		sendPacketImmediately(serverPlayer, bundlePacket);
	}

	@Override
	public void createBossBar(Player player, UUID uuid, Object component, BossBarColor color, BossBarOverlay overlay,
			float progress, boolean createWorldFog, boolean playBossMusic, boolean darkenScreen) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeUUID(uuid);
		buf.writeEnum(addBossBarOperation);
		buf.writeComponent((Component) component);
		buf.writeFloat(progress);
		buf.writeEnum(BossEvent.BossBarColor.valueOf(color.name()));
		buf.writeEnum(BossEvent.BossBarOverlay.valueOf(overlay.name()));
		buf.writeByte(BossBarUtils.encodeProperties(darkenScreen, playBossMusic, createWorldFog));
		ClientboundBossEventPacket packet = new ClientboundBossEventPacket(buf);
		((CraftPlayer) player).getHandle().connection.send(packet);
	}

	@Override
	public void removeBossBar(Player player, UUID uuid) {
		((CraftPlayer) player).getHandle().connection.send(ClientboundBossEventPacket.createRemovePacket(uuid));
	}

	@Override
	public void updateBossBarName(Player player, UUID uuid, Object component) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeUUID(uuid);
		buf.writeEnum(updateBossBarNameOperation);
		buf.writeComponent((Component) component);
		ClientboundBossEventPacket packet = new ClientboundBossEventPacket(buf);
		((CraftPlayer) player).getHandle().connection.send(packet);
	}

	@Override
	public void updateBossBarProgress(Player player, UUID uuid, float progress) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeUUID(uuid);
		buf.writeEnum(updateBossBarProgressOperation);
		buf.writeFloat(progress);
		ClientboundBossEventPacket packet = new ClientboundBossEventPacket(buf);
		((CraftPlayer) player).getHandle().connection.send(packet);
	}

	@Override
	public void sendToast(Player player, ItemStack icon, String titleJson, String advancementType) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(icon);
		Optional<DisplayInfo> displayInfo = Optional
				.of(new DisplayInfo(nmsStack, Objects.requireNonNull(Component.Serializer.fromJson(titleJson)),
						Component.literal(""), null, FrameType.valueOf(advancementType), true, false, true));
		AdvancementRewards advancementRewards = AdvancementRewards.EMPTY;
		Optional<ResourceLocation> id = Optional.of(new ResourceLocation("hellblock", "toast"));
		Criterion<ImpossibleTrigger.TriggerInstance> impossibleTrigger = new Criterion<>(new ImpossibleTrigger(),
				new ImpossibleTrigger.TriggerInstance());
		HashMap<String, Criterion<?>> criteria = new HashMap<>(Map.of("impossible", impossibleTrigger));
		String[][] requirements = { { "impossible" } };
		AdvancementRequirements advancementRequirements = new AdvancementRequirements(requirements);
		Advancement advancement = new Advancement(Optional.empty(), displayInfo, advancementRewards, criteria,
				advancementRequirements, false);
		Map<ResourceLocation, AdvancementProgress> advancementsToGrant = new HashMap<>();
		AdvancementProgress advancementProgress = new AdvancementProgress();
		advancementProgress.update(advancementRequirements);
		Objects.requireNonNull(advancementProgress.getCriterion("impossible")).grant();
		advancementsToGrant.put(id.get(), advancementProgress);
		ClientboundUpdateAdvancementsPacket packet1 = new ClientboundUpdateAdvancementsPacket(false,
				new ArrayList<>(List.of(new AdvancementHolder(id.get(), advancement))), new HashSet<>(),
				advancementsToGrant);
		ClientboundUpdateAdvancementsPacket packet2 = new ClientboundUpdateAdvancementsPacket(false, new ArrayList<>(),
				new HashSet<>(List.of(id.get())), new HashMap<>());
		ArrayList<Packet<ClientGamePacketListener>> packetListeners = new ArrayList<>();
		packetListeners.add(packet1);
		packetListeners.add(packet2);
		ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(packetListeners);
		serverPlayer.connection.send(bundlePacket);
	}

	@Override
	public void playChestAnimation(Player player, Location location, boolean open) {
		ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();
		BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		BlockState blockState = serverLevel.getBlockState(pos);
		Block block = blockState.getBlock();

		int actionId = open ? 1 : 0;

		// Get the NMS player connection
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

		// Handle (double/single) chests and trapped chests
		if (block instanceof ChestBlock || block instanceof TrappedChestBlock) {
			// Send animation for the current chest block
			serverPlayer.connection.send(new ClientboundBlockEventPacket(pos, block, 1, actionId));

			// Play appropriate sound
			player.playSound(location, open ? Sound.BLOCK_CHEST_OPEN : Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);

			// Check and handle the second half of a double chest
			ChestType type = blockState.getValue(ChestBlock.TYPE);
			if (type != ChestType.SINGLE) {
				Direction facing = blockState.getValue(ChestBlock.FACING);
				Direction offset = getConnectedChestOffset(type, facing);

				BlockPos otherHalfPos = new BlockPos(pos.getX() + offset.getStepX(), pos.getY() + offset.getStepY(),
						pos.getZ() + offset.getStepZ());

				BlockState otherHalfState = serverLevel.getBlockState(otherHalfPos);
				if (otherHalfState.getBlock() == block) {
					serverPlayer.connection.send(new ClientboundBlockEventPacket(otherHalfPos, block, 1, actionId));
				}
			}
			return;
		}

		// Ender Chest
		if (block instanceof EnderChestBlock) {
			serverPlayer.connection.send(new ClientboundBlockEventPacket(pos, block, 1, actionId));
			player.playSound(location, open ? Sound.BLOCK_ENDER_CHEST_OPEN : Sound.BLOCK_ENDER_CHEST_CLOSE, 1.0f, 1.0f);
			return;
		}

		// Barrel
		if (block instanceof BarrelBlock) {
			serverPlayer.connection.send(new ClientboundBlockEventPacket(pos, block, 1, actionId));
			player.playSound(location, open ? Sound.BLOCK_BARREL_OPEN : Sound.BLOCK_BARREL_CLOSE, 1.0f, 1.0f);
			return;
		}

		// Shulker Boxes (all colors)
		if (block instanceof ShulkerBoxBlock) {
			serverPlayer.connection.send(new ClientboundBlockEventPacket(pos, block, 1, actionId));
			player.playSound(location, open ? Sound.BLOCK_SHULKER_BOX_OPEN : Sound.BLOCK_SHULKER_BOX_CLOSE, 1.0f, 1.0f);
			return;
		}
	}

	/**
	 * Gets the relative direction to the other half of a double chest based on its
	 * type and facing.
	 *
	 * LEFT and RIGHT are determined from the player's point of view looking at the
	 * front of the chest.
	 *
	 * @param chestType the type of the chest (LEFT, RIGHT, SINGLE)
	 * @param facing    the front-facing direction of the chest
	 * @return the direction toward the connected chest block
	 */
	private Direction getConnectedChestOffset(ChestType chestType, Direction facing) {
		return chestType == ChestType.LEFT ? facing.getClockWise() // LEFT chest connects clockwise
				: facing.getCounterClockWise(); // RIGHT chest connects counter-clockwise
	}

	@Override
	public void injectFakeFortress(World world, BoundingBox bounds) {
		if (world.getEnvironment() != World.Environment.NETHER) {
			throw new IllegalStateException("Fake fortress can only be injected in a Nether world!");
		}

		// Step 1: Get NMS world
		ServerLevel nmsWorld = ((CraftWorld) world).getHandle();

		// Step 2: Get fortress structure from registry
		var registryAccess = nmsWorld.registryAccess();
		var structureRegistry = registryAccess.lookupOrThrow(Registries.STRUCTURE);

		var fortressKey = ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft", "fortress"));

		Structure fortressStructure = structureRegistry.getOrThrow(fortressKey).value();

		if (fortressStructure == null) {
			throw new IllegalStateException("Could not find fortress structure in registry!");
		}

		// Step 3: Determine which chunks the bounding box spans
		int minChunkX = ((int) bounds.getMinX()) >> 4;
		int maxChunkX = ((int) bounds.getMaxX()) >> 4;
		int minChunkZ = ((int) bounds.getMinZ()) >> 4;
		int maxChunkZ = ((int) bounds.getMaxZ()) >> 4;

		// Step 4: Create an empty PiecesContainer (no actual structure pieces)
		PiecesContainer emptyPieces = new PiecesContainer(List.of());

		// Step 5: Loop through all chunks overlapping the bounding box
		try {
			for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
				for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
					ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

					// Create the StructureStart for this chunk
					StructureStart start = new StructureStart(fortressStructure, chunkPos, 0, emptyPieces);

					// Inject into the chunk's structure map
					LevelChunk chunk = nmsWorld.getChunk(chunkPos.x, chunkPos.z);

					@SuppressWarnings("unchecked")
					Map<Structure, StructureStart> starts = (Map<Structure, StructureStart>) structureStartsField
							.get(chunk);

					// Create a new mutable copy with the fortress added
					Map<Structure, StructureStart> newStarts = new HashMap<>();
					if (starts != null) {
						newStarts.putAll(starts);
					}
					newStarts.put(fortressStructure, start);
					structureStartsField.set(chunk, newStarts);
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Failed to inject fortress structure", e);
		}
	}

	@Override
	public void removeFakeFortress(World world, BoundingBox bounds) {
		if (world.getEnvironment() != World.Environment.NETHER) {
			throw new IllegalStateException("Fake fortress can only be removed from a Nether world!");
		}

		ServerLevel nmsWorld = ((CraftWorld) world).getHandle();

		var registryAccess = nmsWorld.registryAccess();
		var structureRegistry = registryAccess.lookupOrThrow(Registries.STRUCTURE);

		var fortressKey = ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft", "fortress"));

		Structure fortressStructure = structureRegistry.getOrThrow(fortressKey).value();

		if (fortressStructure == null) {
			throw new IllegalStateException("Could not find fortress structure in registry!");
		}

		// Compute chunk range
		int minChunkX = ((int) bounds.getMinX()) >> 4;
		int maxChunkX = ((int) bounds.getMaxX()) >> 4;
		int minChunkZ = ((int) bounds.getMinZ()) >> 4;
		int maxChunkZ = ((int) bounds.getMaxZ()) >> 4;

		try {
			for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
				for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
					ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
					LevelChunk chunk = nmsWorld.getChunk(chunkPos.x, chunkPos.z);

					@SuppressWarnings("unchecked")
					Map<Structure, StructureStart> starts = (Map<Structure, StructureStart>) structureStartsField
							.get(chunk);
					if (starts != null && starts.containsKey(fortressStructure)) {
						// Create a new mutable copy without the fortress
						Map<Structure, StructureStart> newStarts = new HashMap<>(starts);
						newStarts.remove(fortressStructure);
						structureStartsField.set(chunk, newStarts);
					}
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Failed to remove fortress structure", e);
		}
	}

	@Override
	public void sendWorldBorder(Player player, BoundingBox box, BorderColor borderColor) {
		org.bukkit.World world = player.getWorld();
		ServerLevel serverLevel = ((CraftWorld) world).getHandle();
		Location center = box.getCenter().toLocation(world);

		double minX = box.getMinX();
		double minZ = box.getMinZ();

		double maxX = box.getMaxX();
		double maxZ = box.getMaxZ();

		double width = maxX - minX; // Dimension along the X-axis
		double length = maxZ - minZ; // Dimension along the Z-axis

		double size = Math.max(width, length) / 2.0D;

		WorldBorder worldBorder = new WorldBorder();
		worldBorder.world = serverLevel;
		worldBorder.setWarningBlocks(0);
		worldBorder.setCenter(center.getX() * 8.0D, center.getZ() * 8.0D);

		switch (borderColor) {
		case BLUE -> {
			worldBorder.setSize((size * 2) + 1D);
		}
		case GREEN -> {
			worldBorder.setSize((size * 2) + 1.001D);
			worldBorder.lerpSizeBetween(worldBorder.getSize() - 0.001D, worldBorder.getSize(), Long.MAX_VALUE);
		}
		case RED -> {
			worldBorder.setSize((size * 2) + 1D);
			worldBorder.lerpSizeBetween(worldBorder.getSize(), worldBorder.getSize() - 0.001D, Long.MAX_VALUE);
		}
		}

		ClientboundInitializeBorderPacket initializeBorderPacket = new ClientboundInitializeBorderPacket(worldBorder);
		((CraftPlayer) player).getHandle().connection.send(initializeBorderPacket);
	}

	@Override
	public void updateWorldBorder(Player player, Location center, double startSize, double endSize, long durationMs,
			BorderColor borderColor) {
		ServerLevel serverLevel = ((CraftWorld) center.getWorld()).getHandle();

		WorldBorder worldBorder = new WorldBorder();
		worldBorder.world = serverLevel;
		worldBorder.setWarningBlocks(0);
		worldBorder.setCenter(center.getX() * 8.0D, center.getZ() * 8.0D);

		switch (borderColor) {
		case BLUE -> {
			// Smooth expansion, static blue
			worldBorder.lerpSizeBetween(startSize, endSize, durationMs);
		}
		case GREEN -> {
			// Smooth expansion, then apply pulsing effect
			worldBorder.lerpSizeBetween(startSize, endSize + 0.001D, durationMs);
			// After animation ends, keep pulsing infinitely
			worldBorder.lerpSizeBetween(worldBorder.getSize() - 0.001D, worldBorder.getSize(), Long.MAX_VALUE);
		}
		case RED -> {
			// Smooth expansion, then apply pulsing effect
			worldBorder.lerpSizeBetween(startSize, endSize - 0.001D, durationMs);
			// After animation ends, keep pulsing infinitely
			worldBorder.lerpSizeBetween(worldBorder.getSize(), worldBorder.getSize() - 0.001D, Long.MAX_VALUE);
		}
		}

		ClientboundInitializeBorderPacket packet = new ClientboundInitializeBorderPacket(worldBorder);
		((CraftPlayer) player).getHandle().connection.send(packet);
	}

	@Override
	public void clearWorldBorder(Player player) {
		ServerLevel serverLevel = ((CraftWorld) player.getWorld()).getHandle();
		WorldBorder worldBorder = new WorldBorder();
		worldBorder.world = serverLevel;
		worldBorder.setCenter(0, 0); // default world spawn
		worldBorder.setSize(30_000_000); // vanilla max border size

		ClientboundInitializeBorderPacket packet = new ClientboundInitializeBorderPacket(worldBorder);
		((CraftPlayer) player).getHandle().connection.send(packet);
	}

	@Override
	public Object getMinecraftComponent(String json) {
		return CraftChatMessage.fromJSON(json);
	}

	@Override
	public void restrictWitherAI(Wither bukkitWither) {
		// Convert the Bukkit Wither to its NMS equivalent
		WitherBoss wither = ((CraftWither) bukkitWither).getHandle();

		// Access and clear both goal and target selectors
		GoalSelector goalSelector = wither.goalSelector;
		GoalSelector targetSelector = wither.targetSelector;
		goalSelector.removeAllGoals(goal -> true);
		targetSelector.removeAllGoals(goal -> true);

		// Add a target goal to attack players only
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(wither, ServerPlayer.class, true));
	}

	@Override
	public FluidData getFluidData(Location location) {
		World world = location.getWorld();
		FluidState state = ((CraftWorld) world).getHandle().getFluidState(CraftLocation.toBlockPosition(location));
		return FluidDataInstance.createData(state);
	}

	@Override
	public String getBiomeResourceLocation(Location location) {
		ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();

		// Get the biome Holder at the location
		Holder<Biome> biomeHolder = world.getNoiseBiome(location.getBlockX() >> 2, location.getBlockY() >> 2,
				location.getBlockZ() >> 2);

		// Get biome key from the Holder
		return biomeHolder.unwrapKey().map(resourceKey -> resourceKey.location().toString()) // "minecraft:plains"
				.orElse("void"); // fallback if unregistered
	}

	@Override
	public void openCustomInventory(Player player, Inventory inventory, String jsonTitle) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		MenuType<?> menuType = CraftContainer.getNotchInventoryType(inventory);
		AbstractContainerMenu menu = new CraftContainer(inventory, serverPlayer, serverPlayer.nextContainerCounter());
		menu = CraftEventFactory.callInventoryOpenEvent(serverPlayer, menu);
		if (menu != null) {
			Component titleComponent = CraftChatMessage.fromJSON(jsonTitle);
			menu.checkReachable = false;
			serverPlayer.connection.send(new ClientboundOpenScreenPacket(menu.containerId, menuType, titleComponent));
			serverPlayer.containerMenu = menu;
			serverPlayer.initMenu(menu);
		}
	}

	@Override
	public void updateInventoryTitle(Player player, String jsonTitle) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		AbstractContainerMenu menu = serverPlayer.containerMenu;
		serverPlayer.connection.send(new ClientboundOpenScreenPacket(menu.containerId, menu.getType(),
				CraftChatMessage.fromJSON(jsonTitle)));
		serverPlayer.initMenu(menu);
	}

	@Override
	public boolean isFishingHookBit(FishHook hook) {
		FishingHook fishingHook = ((CraftFishHook) hook).getHandle();
		return fishingHook.getEntityData().get(dataBiting);
	}

	@Override
	public UUID getFishingHookOwner(FishHook hook) {
		FishingHook fishingHook = ((CraftFishHook) hook).getHandle();
		try {
			return (UUID) fishingHookOwnerUUIDField.get(fishingHook);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Failed to read ownerUUID from FishingHook", e);
		}
	}

	@Override
	public List<ItemStack> getFishingLoot(Player player, FishHook hook, ItemStack rod) {
		Location location = hook.getLocation();
		ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
		LootParams lootparams = (new LootParams.Builder(level))
				.withParameter(LootContextParams.ORIGIN, CraftLocation.toVec3D(location))
				.withParameter(LootContextParams.TOOL, CraftItemStack.asNMSCopy(rod))
				.withParameter(LootContextParams.THIS_ENTITY, ((CraftFishHook) hook).getHandle())
				.withLuck((float) (rod.getEnchantmentLevel(Enchantment.LUCK)
						+ Optional.ofNullable(player.getAttribute(Attribute.GENERIC_LUCK))
								.map(AttributeInstance::getValue).orElse(0d)))
				.create(LootContextParamSets.FISHING);
		LootTable loottable = level.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
		List<net.minecraft.world.item.ItemStack> list = loottable.getRandomItems(lootparams);
		return list.stream().filter(itemStack -> itemStack != null && !itemStack.isEmpty())
				.map(CraftItemStack::asBukkitCopy).toList();
	}

	@Override
	public void removeClientSideEntity(Player player, int... entityIDs) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entityIDs);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void sendClientSideTeleportEntity(Player player, Location location, Vector motion, boolean onGround,
			int... entityIDs) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ArrayList<Packet<ClientGamePacketListener>> packets = new ArrayList<>();
		float ROTATION_FACTOR = 256.0F / 360.0F;
		float yaw = location.getYaw() * ROTATION_FACTOR;
		float pitch = location.getPitch() * ROTATION_FACTOR;
		for (int entityID : entityIDs) {
			FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
			buf.writeVarInt(entityID);
			buf.writeDouble(location.getX());
			buf.writeDouble(location.getY());
			buf.writeDouble(location.getZ());
			buf.writeByte((byte) yaw);
			buf.writeByte((byte) pitch);
			buf.writeBoolean(onGround);
			ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(buf);
			packets.add(packet);
		}
		ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(packets);
		serverPlayer.connection.send(bundlePacket);
	}

	@Override
	public void sendClientSideEntityMotion(Player player, Vector vector, int... entityIDs) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ArrayList<Packet<ClientGamePacketListener>> packets = new ArrayList<>();
		Vec3 vec3 = CraftVector.toNMS(vector);
		for (int entityID : entityIDs) {
			ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(entityID, vec3);
			packets.add(packet);
		}
		ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(packets);
		serverPlayer.connection.send(bundlePacket);
	}

	@Override
	public void swingHand(Player player, HandSlot slot) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundAnimatePacket packet = new ClientboundAnimatePacket(serverPlayer, slot.getId());
		serverPlayer.connection.send(packet);
		player.getWorld().getPlayers().stream().filter(other -> !other.equals(player) && other.canSee(player))
				.forEach(other -> ((CraftPlayer) other).getHandle().connection.send(packet));
	}

	@Override
	public void useItem(Player player, HandSlot handSlot, @Nullable ItemStack itemStack) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.gameMode.useItem(serverPlayer, ((CraftWorld) player.getWorld()).getHandle(),
				Optional.ofNullable(itemStack).map(CraftItemStack::asNMSCopy)
						.orElse(serverPlayer.getItemBySlot(EquipmentSlot.valueOf(handSlot.name() + "HAND"))),
				InteractionHand.valueOf(handSlot.name() + "_HAND"));
	}

	@Override
	public int dropFakeItem(Player player, ItemStack itemStack, Location location) {
		UUID uuid = UUID.randomUUID();
		int entityID = SelfIncreaseEntityID.getAndIncrease();
		ClientboundAddEntityPacket entityPacket = new ClientboundAddEntityPacket(entityID, uuid, location.getX(),
				location.getY(), location.getZ(), 0, 0, EntityType.ITEM, 0, Vec3.ZERO, 0);
		ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityID, List.of(
				SynchedEntityData.DataValue.create(new EntityDataAccessor<>(8, EntityDataSerializers.ITEM_STACK),
						CraftItemStack.asNMSCopy(itemStack)),
				SynchedEntityData.DataValue.create(new EntityDataAccessor<>(5, EntityDataSerializers.BOOLEAN), true)));
		ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(List.of(entityPacket, dataPacket));
		((CraftPlayer) player).getHandle().connection.send(bundlePacket);
		return entityID;
	}

	@Override
	public FakeArmorStand createFakeArmorStand(Location location) {
		return new ArmorStandInstance(location);
	}

	@Override
	public FakeItemDisplay createFakeItemDisplay(Location location) {
		return new ItemDisplayInstance(location);
	}

	@Override
	public FakeTextDisplay createFakeTextDisplay(Location location) {
		return new TextDisplayInstance(location);
	}

	@Override
	public FakeFirework createFakeFirework(Location location, Color color) {
		return new FireworkInstance(location, color);
	}
}