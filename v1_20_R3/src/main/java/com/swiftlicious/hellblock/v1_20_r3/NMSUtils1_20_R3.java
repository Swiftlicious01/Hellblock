package com.swiftlicious.hellblock.v1_20_r3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftFishHook;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftContainer;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftChatMessage;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftLocation;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftVector;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.nms.NMSHandler;
import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.nms.entity.display.FakeItemDisplay;
import com.swiftlicious.hellblock.nms.entity.display.FakeTextDisplay;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.nms.fluid.FluidData;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.nms.util.SelfIncreaseEntityID;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class NMSUtils1_20_R3 implements NMSHandler {

	private final Registry<Biome> biomeRegistry = MinecraftServer.getServer().registries().compositeAccess()
			.registryOrThrow(Registries.BIOME);

	private final EntityDataAccessor<Boolean> dataBiting;

	@SuppressWarnings("unchecked")
	public NMSUtils1_20_R3() {
		try {
			Field dataBitingField = FishingHook.class.getDeclaredField("h");
			dataBitingField.setAccessible(true);
			dataBiting = (EntityDataAccessor<Boolean>) dataBitingField.get(null);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to get hook biting state", e);
		}
		FluidDataInstance.register(LavaFluid.Source.class, FallingFluidDataInstance::new);
		FluidDataInstance.register(WaterFluid.Source.class, FallingFluidDataInstance::new);
		FluidDataInstance.register(LavaFluid.Flowing.class, FlowingFluidDataInstance::new);
		FluidDataInstance.register(WaterFluid.Flowing.class, FlowingFluidDataInstance::new);
	}

	@Override
	public FluidData getFluidData(Location location) {
		World world = location.getWorld();
		FluidState state = ((CraftWorld) world).getHandle().getFluidState(CraftLocation.toBlockPosition(location));
		return FluidDataInstance.createData(state);
	}

	@Override
	public String getBiomeResourceLocation(Location location) {
		Biome biome = ((CraftWorld) location.getWorld()).getHandle()
				.getNoiseBiome(location.getBlockX() >> 2, location.getBlockY() >> 2, location.getBlockZ() >> 2).value();
		ResourceLocation resourceLocation = biomeRegistry.getKey(biome);
		if (resourceLocation == null) {
			return "void";
		}
		return resourceLocation.toString();
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
				.map(net.minecraft.world.item.ItemStack::getBukkitStack).toList();
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
		ChunkMap.TrackedEntity tracker = serverPlayer.tracker;
		if (tracker != null) {
			for (ServerPlayerConnection connection : tracker.seenBy) {
				connection.send(packet);
			}
		}
	}

	@Override
	public void useItem(Player player, HandSlot handSlot, @Nullable ItemStack itemStack) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.gameMode.useItem(serverPlayer, ((CraftWorld) player.getWorld()).getHandle(),
				Optional.ofNullable(itemStack).map(stack -> ((CraftItemStack) itemStack).handle)
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
	public FakeFirework createFakeFirework(Location location) {
		return new FireworkInstance(location);
	}
}