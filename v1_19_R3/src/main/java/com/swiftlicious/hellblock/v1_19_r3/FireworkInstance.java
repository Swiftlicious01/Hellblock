package com.swiftlicious.hellblock.v1_19_r3;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.nms.util.SelfIncreaseEntityID;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;

public class FireworkInstance implements FakeFirework {

	private final Location location;
	private int flightTime = 0;
	private boolean invisible = false;
	private final int entityID = SelfIncreaseEntityID.getAndIncrease();
	private final UUID uuid = UUID.randomUUID();

	public FireworkInstance(Location location) {
		this.location = location;
	}

	@Override
	public void flightTime(int flightTime) {
		this.flightTime = flightTime;
	}

	@Override
	public void invisible(boolean invisible) {
		this.invisible = invisible;
	}

	@Override
	public void updateMetaData(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.connection.send(getMetaPacket(serverPlayer));
	}

	@Override
	public void destroy(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entityID);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void spawn(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundAddEntityPacket entityPacket = new ClientboundAddEntityPacket(entityID, uuid, location.getX(),
				location.getY(), location.getZ(), location.getPitch(), location.getYaw(), EntityType.FIREWORK_ROCKET, 0,
				Vec3.ZERO, 0);
		serverPlayer.connection.send(entityPacket);
		serverPlayer.connection.send(getMetaPacket(serverPlayer));
		FireworkRocketEntity firework = new FireworkRocketEntity(((CraftWorld) location.getWorld()).getHandle(),
				location.getX(), location.getY(), location.getZ(), CraftItemStack.asNMSCopy(getFireworkStack()));
		serverPlayer.level.broadcastEntityEvent(firework, (byte) 17);
	}

	private ClientboundSetEntityDataPacket getMetaPacket(ServerPlayer player) {
		ArrayList<SynchedEntityData.DataValue<?>> values = new ArrayList<>();
		if (invisible) {
			values.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(0, EntityDataSerializers.BYTE),
					(byte) (0x20)));
		}
		return new ClientboundSetEntityDataPacket(entityID, values);
	}

	@Override
	public int entityID() {
		return entityID;
	}

	@Override
	public ItemStack getFireworkStack() {
		ItemStack stackFirework = new ItemStack(Material.FIREWORK_ROCKET);
		FireworkMeta fireworkMeta = (FireworkMeta) stackFirework.getItemMeta();
		FireworkEffect effect = FireworkEffect.builder().flicker(false).withColor(Color.GREEN).with(Type.BURST).build();
		fireworkMeta.addEffect(effect);
		stackFirework.setItemMeta(fireworkMeta);
		return stackFirework;
	}
}
