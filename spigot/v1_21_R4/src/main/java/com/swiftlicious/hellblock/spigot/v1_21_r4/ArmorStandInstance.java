package com.swiftlicious.hellblock.spigot.v1_21_r4;

import com.mojang.datafixers.util.Pair;
import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.nms.util.SelfIncreaseEntityID;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R4.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R4.util.CraftChatMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ArmorStandInstance implements FakeArmorStand {

	private final Location location;
	private boolean small = false;
	private boolean invisible = false;
	private boolean gravity = false;
	private boolean basePlate = false;
	private boolean marker = false;
	private String name;
	private final List<Pair<EquipmentSlot, ItemStack>> equipments = new ArrayList<>();
	private final int entityID = SelfIncreaseEntityID.getAndIncrease();
	private final int teleportID = SelfIncreaseEntityID.getAndIncrease();
	private final UUID uuid = UUID.randomUUID();
	private boolean isDestroyed = false;
	private ArmorStand armorStand;

	public ArmorStandInstance(Location location) {
		this.location = location.clone();
	}

	@Override
	public void small(boolean small) {
		this.small = small;
	}

	@Override
	public void invisible(boolean invisible) {
		this.invisible = invisible;
	}

	@Override
	public void gravity(boolean gravity) {
		this.gravity = gravity;
	}

	@Override
	public void basePlate(boolean basePlate) {
		this.basePlate = basePlate;
	}

	@Override
	public void marker(boolean marker) {
		this.marker = marker;
	}

	@Override
	public void name(String json) {
		this.name = json;
	}

	@Override
	public void equipment(org.bukkit.inventory.EquipmentSlot slot, org.bukkit.inventory.ItemStack itemStack) {
		this.equipments.add(Pair.of(CraftEquipmentSlot.getNMS(slot), CraftItemStack.asNMSCopy(itemStack)));
	}

	@Override
	public void spawn(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		if (armorStand == null) {
			armorStand = new ArmorStand(EntityType.ARMOR_STAND, serverPlayer.level());
			armorStand.setId(entityID);
			armorStand.setUUID(uuid);
		}

		ClientboundAddEntityPacket entityPacket = new ClientboundAddEntityPacket(entityID, uuid, location.getX(),
				location.getY(), location.getZ(), location.getPitch(), location.getYaw(), EntityType.ARMOR_STAND, 0,
				Vec3.ZERO, 0);

		List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
		packets.add(entityPacket);
		packets.add(getMetaPacket());

		if (!equipments.isEmpty()) {
			packets.add(getEquipmentPacket());
		}

		ClientboundBundlePacket bundle = new ClientboundBundlePacket(packets);
		serverPlayer.connection.send(bundle);
	}

	@Override
	public void destroy(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entityID);
		serverPlayer.connection.send(packet);
		isDestroyed = true;
	}

	@Override
	public void teleport(Player player, Location newLocation) {
		if (isDestroyed || player == null || !player.isOnline())
			return;

		double dx = newLocation.getX() - this.location.getX();
		double dy = newLocation.getY() - this.location.getY();
		double dz = newLocation.getZ() - this.location.getZ();

		byte yaw = (byte) (newLocation.getYaw() * 256.0F / 360.0F);
		byte pitch = (byte) (newLocation.getPitch() * 256.0F / 360.0F);

		this.location.setX(newLocation.getX());
		this.location.setY(newLocation.getY());
		this.location.setZ(newLocation.getZ());
		this.location.setYaw(newLocation.getYaw());
		this.location.setPitch(newLocation.getPitch());

		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundMoveEntityPacket.PosRot packet = new ClientboundMoveEntityPacket.PosRot(entityID,
				(short) (dx * 4096), (short) (dy * 4096), (short) (dz * 4096), yaw, pitch, false);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void moveSmoothly(Player player, Location newLocation, double speedMultiplier) {
		if (isDestroyed || player == null || !player.isOnline())
			return;

		double dx = newLocation.getX() - this.location.getX();
		double dy = newLocation.getY() - this.location.getY();
		double dz = newLocation.getZ() - this.location.getZ();

		// Update local location reference
		this.location.setX(newLocation.getX());
		this.location.setY(newLocation.getY());
		this.location.setZ(newLocation.getZ());
		this.location.setYaw(newLocation.getYaw());
		this.location.setPitch(newLocation.getPitch());

		// Send velocity packet instead of teleport
		Vec3 velocity = new Vec3(dx * speedMultiplier, dy * speedMultiplier, dz * speedMultiplier);

		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(entityID, velocity);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void moveAdaptive(Player player, Location newLocation) {
		double distance = newLocation.distance(this.location);
		if (distance > 10) {
			// Big jump — use teleport (e.g., chunk load)
			teleport(player, newLocation);
		} else {
			// Small step — smooth velocity
			moveSmoothly(player, newLocation, 0.15);
		}
	}

	@Override
	public void updateMetaData(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.connection.send(getMetaPacket());
	}

	@Override
	public void updateEquipment(Player player) {
		if (equipments.isEmpty())
			return;
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.connection.send(getEquipmentPacket());
	}

	private ClientboundSetEntityDataPacket getMetaPacket() {
		List<SynchedEntityData.DataValue<?>> values = new ArrayList<>();

		byte flags = 0x00;
		if (invisible)
			flags |= 0x20; // invisible flag

		values.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(0, EntityDataSerializers.BYTE), flags));

		if (name != null) {
			values.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(3, EntityDataSerializers.BOOLEAN),
					true));
			values.add(SynchedEntityData.DataValue.create(
					new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT),
					Optional.of(CraftChatMessage.fromJSON(name))));
		}

		byte armorStandFlags = 0x00;

		if (small) {
			armorStandFlags |= 0x01; // Small
		}
		if (!gravity) {
			armorStandFlags |= 0x02; // No gravity
		}
		if (!basePlate) {
			armorStandFlags |= 0x08; // No baseplate
		}
		if (marker) {
			armorStandFlags |= 0x10; // Marker (no collision box)
		}

		values.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(15, EntityDataSerializers.BYTE),
				armorStandFlags));

		return new ClientboundSetEntityDataPacket(entityID, values);
	}

	private ClientboundSetEquipmentPacket getEquipmentPacket() {
		return new ClientboundSetEquipmentPacket(entityID, equipments);
	}

	@Override
	public Location getLocation() {
		return location.clone();
	}

	@Override
	public boolean isDead() {
		return isDestroyed;
	}

	@Override
	public int entityID() {
		return entityID;
	}

	@Override
	public void setCamera(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		if (armorStand == null)
			return; // safety
		armorStand.setId(entityID); // ensure synced
		armorStand.setUUID(uuid);
		ClientboundSetCameraPacket packet = new ClientboundSetCameraPacket(armorStand);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void resetCamera(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundSetCameraPacket packet = new ClientboundSetCameraPacket(serverPlayer);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void keepClientCameraStable(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

		// current fake stand position
		Location loc = this.getLocation();
		Vec3 position = new Vec3(loc.getX(), loc.getY(), loc.getZ());
		Vec3 deltaMovement = Vec3.ZERO;

		// Construct the position/rotation data
		PositionMoveRotation posRot = new PositionMoveRotation(position, deltaMovement, loc.getYaw(), loc.getPitch());

		// Teleport id: just increment manually to satisfy the client (doesn’t need to
		// be synced with real teleport logic)
		int teleportId = this.teleportID;

		// Absolute positioning, not relative
		ClientboundPlayerPositionPacket packet = new ClientboundPlayerPositionPacket(teleportId, posRot, Set.of());

		serverPlayer.connection.send(packet);
	}
}