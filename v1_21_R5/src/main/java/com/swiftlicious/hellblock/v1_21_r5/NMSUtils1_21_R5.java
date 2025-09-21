package com.swiftlicious.hellblock.v1_21_r5;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftFishHook;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftContainer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
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
import com.swiftlicious.hellblock.nms.util.ReflectionUtils;
import com.swiftlicious.hellblock.nms.util.SelfIncreaseEntityID;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class NMSUtils1_21_R5 implements NMSHandler {

	private final Registry<Biome> biomeRegistry = MinecraftServer.getServer().registries().compositeAccess()
			.lookupOrThrow(Registries.BIOME);

	private final EntityDataAccessor<Boolean> dataBiting;

	@SuppressWarnings("unchecked")
	public NMSUtils1_21_R5() {
		try {
			Field dataBitingField = ReflectionUtils.getDeclaredField(FishingHook.class, EntityDataAccessor.class, 1);
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
	public void sendToast(Player player, ItemStack icon, String titleJson, String advancementType) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(icon);
		Optional<DisplayInfo> displayInfo = Optional.of(new DisplayInfo(nmsStack,
				Objects.requireNonNull(CraftChatMessage.fromJSON(titleJson)), Component.literal(""), Optional.empty(),
				AdvancementType.valueOf(advancementType), true, false, true));
		AdvancementRewards advancementRewards = AdvancementRewards.EMPTY;
		Optional<ResourceLocation> id = Optional.of(ResourceLocation.fromNamespaceAndPath("hellblock", "toast"));
		Criterion<ImpossibleTrigger.TriggerInstance> impossibleTrigger = new Criterion<>(new ImpossibleTrigger(),
				new ImpossibleTrigger.TriggerInstance());
		HashMap<String, Criterion<?>> criteria = new HashMap<>(Map.of("impossible", impossibleTrigger));
		AdvancementRequirements advancementRequirements = new AdvancementRequirements(
				new ArrayList<>(List.of(new ArrayList<>(List.of("impossible")))));
		Advancement advancement = new Advancement(Optional.empty(), displayInfo, advancementRewards, criteria,
				advancementRequirements, false);
		Map<ResourceLocation, AdvancementProgress> advancementsToGrant = new HashMap<>();
		AdvancementProgress advancementProgress = new AdvancementProgress();
		advancementProgress.update(advancementRequirements);
		Objects.requireNonNull(advancementProgress.getCriterion("impossible")).grant();
		advancementsToGrant.put(id.get(), advancementProgress);
		ClientboundUpdateAdvancementsPacket packet1 = new ClientboundUpdateAdvancementsPacket(false,
				new ArrayList<>(List.of(new AdvancementHolder(id.get(), advancement))), new HashSet<>(),
				advancementsToGrant, true);
		ClientboundUpdateAdvancementsPacket packet2 = new ClientboundUpdateAdvancementsPacket(false, new ArrayList<>(),
				new HashSet<>(List.of(id.get())), new HashMap<>(), true);
		ArrayList<Packet<? super ClientGamePacketListener>> packetListeners = new ArrayList<>();
		packetListeners.add(packet1);
		packetListeners.add(packet2);
		ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(packetListeners);
		serverPlayer.connection.send(bundlePacket);
	}

	@Override
	public Object getMinecraftComponent(String json) {
		return CraftChatMessage.fromJSON(json);
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
	public UUID getFishingHookOwner(FishHook hook) {
		FishingHook fishingHook = ((CraftFishHook) hook).getHandle();
		Entity owner = fishingHook.getOwner();
		if (owner == null)
			return null;
		return owner.getUUID();
	}

	@Override
	public List<ItemStack> getFishingLoot(Player player, FishHook hook, ItemStack rod) {
		Location location = hook.getLocation();
		ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
		LootParams lootparams = (new LootParams.Builder(level))
				.withParameter(LootContextParams.ORIGIN, CraftLocation.toVec3(location))
				.withParameter(LootContextParams.TOOL, CraftItemStack.asNMSCopy(rod))
				.withParameter(LootContextParams.THIS_ENTITY, ((CraftFishHook) hook).getHandle())
				.withLuck(
						(float) (rod.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA) + Optional
								.ofNullable(player.getAttribute(Objects.requireNonNull(
										org.bukkit.Registry.ATTRIBUTE.get(NamespacedKey.minecraft("luck")))))
								.map(AttributeInstance::getValue).orElse(0d)))
				.create(LootContextParamSets.FISHING);
		LootTable loottable = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
		List<net.minecraft.world.item.ItemStack> list = loottable.getRandomItems(lootparams);
		return list.stream().filter(itemStack -> itemStack != null && !itemStack.isEmpty())
				.map(net.minecraft.world.item.ItemStack::getBukkitStack).toList();
	}

	@Override
	public Map<String, Integer> itemEnchantmentsToMap(Object item) {
		ItemEnchantments enchantments = (ItemEnchantments) item;
		Map<String, Integer> map = new HashMap<>();
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
			Holder<Enchantment> enchantmentHolder = entry.getKey();
			int level = entry.getIntValue();
			map.put(enchantmentHolder.getRegisteredName(), level);
		}
		return map;
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
		ArrayList<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
		for (int entityID : entityIDs) {
			ClientboundTeleportEntityPacket packet = ClientboundTeleportEntityPacket.teleport(entityID,
					PositionMoveRotation.of(new TeleportTransition(serverPlayer.level(),
							new Vec3(location.getX(), location.getY(), location.getZ()),
							new Vec3(motion.getX(), motion.getY(), motion.getZ()), location.getYaw(),
							location.getPitch(), false, false, Set.of(), (entity -> {
							}), PlayerTeleportEvent.TeleportCause.PLUGIN)),
					Set.of(), false);
			packets.add(packet);
		}
		ClientboundBundlePacket bundlePacket = new ClientboundBundlePacket(packets);
		serverPlayer.connection.send(bundlePacket);
	}

	@Override
	public void sendClientSideEntityMotion(Player player, Vector vector, int... entityIDs) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ArrayList<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
		Vec3 vec3 = CraftVector.toVec3(vector);
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
		ChunkMap.TrackedEntity tracker = serverPlayer.moonrise$getTrackedEntity();
		for (ServerPlayerConnection connection : tracker.seenBy) {
			connection.send(packet);
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