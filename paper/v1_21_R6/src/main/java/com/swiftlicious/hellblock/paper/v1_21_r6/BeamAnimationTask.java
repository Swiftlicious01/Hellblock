package com.swiftlicious.hellblock.paper.v1_21_r6;

import java.util.Collection;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.nms.beam.BeamAnimation;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;

public class BeamAnimationTask implements BeamAnimation {

	private final Collection<Player> viewers;
	private final Location location;
	private final int durationTicks;
	private final int worldMaxY;
	private final int entityId;
	private final ClientboundRemoveEntitiesPacket destroyPacket;

	private int ticks = 0;
	private boolean finished = false;

	public BeamAnimationTask(Collection<Player> viewers, Location location, int durationTicks) {
		this.viewers = viewers;
		this.location = location;
		this.durationTicks = durationTicks;
		this.worldMaxY = location.getWorld().getMaxHeight();

		// Prepare the fake End Crystal
		ServerLevel nmsWorld = ((CraftWorld) location.getWorld()).getHandle();
		EndCrystal crystal = new EndCrystal(EntityType.END_CRYSTAL, nmsWorld);
		crystal.setShowBottom(false);
		crystal.setPos(location.getX(), location.getY(), location.getZ());

		this.entityId = crystal.getId(); // Assigned from NMS

		Vec3 motion = new Vec3(0, 0, 0);
		UUID uuid = UUID.randomUUID();

		ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(entityId, uuid, location.getX(),
				location.getY(), location.getZ(), 0f, 0f, EntityType.END_CRYSTAL, 0, motion, 0);

		ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(entityId,
				crystal.getEntityData().packDirty());

		// Send spawn + metadata packets to viewers
		for (Player viewer : viewers) {
			ServerPlayer nmsPlayer = ((CraftPlayer) viewer).getHandle();
			nmsPlayer.connection.send(spawnPacket);
			nmsPlayer.connection.send(metadataPacket);
		}

		this.destroyPacket = new ClientboundRemoveEntitiesPacket(entityId);
	}

	@Override
	public void run() {
		if (finished)
			return;

		ticks++;

		if (ticks > durationTicks) {
			for (Player viewer : viewers) {
				ServerPlayer nmsPlayer = ((CraftPlayer) viewer).getHandle();
				nmsPlayer.connection.send(destroyPacket);
			}
			this.finished = true;
			return;
		}

		// Pulse calculation
		double pulse = (Math.sin(ticks / 6.0) + 1) / 2.0;
		float size = 1.5f + (float) pulse * 1.2f;

		// Vertical black beam
		for (int yOffset = 0; location.getY() + yOffset <= worldMaxY; yOffset += 3) {
			Location loc = location.clone().add(0, yOffset, 0);
			location.getWorld().spawnParticle(Particle.DUST, loc, Math.max(1, (int) (4 + pulse * 8)), 0.15, 0.4, 0.15,
					new Particle.DustOptions(Color.fromRGB(0, 0, 0), size));
		}

		// Smoke puff at base
		location.getWorld().spawnParticle(Particle.LARGE_SMOKE, location.clone().add(0, 0.2, 0),
				10 + (int) (pulse * 20), 1.0, 0.1, 1.0, 0.02);

		// Bottom glow
		location.getWorld().spawnParticle(Particle.DUST, location.clone().add(0, 0.5, 0), 8 + (int) (pulse * 20), 0.7,
				0.2, 0.7, new Particle.DustOptions(Color.fromRGB(30, 30, 30), size));
	}

	/**
	 * @return true if the task has finished and should stop being scheduled
	 */
	@Override
	public boolean isFinished() {
		return finished;
	}
}