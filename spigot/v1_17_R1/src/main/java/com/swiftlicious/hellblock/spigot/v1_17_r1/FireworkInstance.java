package com.swiftlicious.hellblock.spigot.v1_17_r1;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FireworkInstance implements FakeFirework {

	private final Location location;
	private final Color color;
	private int flightTime = 0;
	private boolean invisible = false;
	private FireworkRocketEntity firework;

	public FireworkInstance(Location location, Color color) {
		this.location = location;
		this.color = color;
	}

	@Override
	public void invisible(boolean invisible) {
		this.invisible = invisible;
	}

	@Override
	public void flightTime(int flightTime) {
		this.flightTime = flightTime;
	}

	@Override
	public void updateMetaData(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.connection.send(getMetaPacket(serverPlayer));
	}

	@Override
	public void destroy(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundRemoveEntitiesPacket packetDestroy = new ClientboundRemoveEntitiesPacket(firework.getId());
		updateMetaData(player);
		firework.discard();
		serverPlayer.connection.send(packetDestroy);
	}

	@Override
	public void spawn(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
		firework = new FireworkRocketEntity(((CraftWorld) location.getWorld()).getHandle(), location.getX(),
				location.getY(), location.getZ(), getFireworkItem());
		firework.lifetime = flightTime < 0 ? 0 : flightTime;
		ClientboundAddEntityPacket packet = new ClientboundAddEntityPacket(firework, 76);
		world.addFreshEntity(firework);
		serverPlayer.connection.send(packet);
		destroy(player);
	}

	private ClientboundSetEntityDataPacket getMetaPacket(ServerPlayer player) {
		if (firework == null) {
			firework = new FireworkRocketEntity(((CraftWorld) location.getWorld()).getHandle(), location.getX(),
					location.getY(), location.getZ(), getFireworkItem());
		}
		SynchedEntityData entityData = new SynchedEntityData(firework);
		entityData.set(FireworkRocketEntity.DATA_ID_FIREWORKS_ITEM, firework.getItem().copy());
		entityData.markDirty(FireworkRocketEntity.DATA_ID_FIREWORKS_ITEM);
		if (invisible) {
			entityData.define(new EntityDataAccessor<>(0, EntityDataSerializers.BYTE), (byte) (0x20));
		}
		return new ClientboundSetEntityDataPacket(firework.getId(), entityData, true);
	}

	@Override
	public int entityID() {
		return firework.getId();
	}

	private ItemStack getFireworkItem() {
		ItemStack item = new ItemStack(Items.FIREWORK_ROCKET);
		FireworkMeta meta = (FireworkMeta) CraftItemStack.asCraftMirror(item).getItemMeta();
		FireworkEffect effect = FireworkEffect.builder().flicker(false).withColor(color).with(Type.BURST).build();
		meta.addEffect(effect);
		CraftItemStack.setItemMeta(item, meta);
		return item;
	}
}