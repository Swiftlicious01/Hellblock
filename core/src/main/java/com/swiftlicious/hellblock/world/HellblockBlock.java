package com.swiftlicious.hellblock.world;

import org.bukkit.Location;
import org.bukkit.World;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.extras.Key;

public class HellblockBlock implements HellblockBlockInterface {

	private final Key type;

	public HellblockBlock(Key type) {
		this.type = type;
	}

	@Override
	public Key type() {
		return type;
	}

	@Override
	public HellblockBlockState createBlockState() {
		return HellblockBlockStateInterface.create(this, new CompoundMap());
	}

	@Override
	public HellblockBlockState createBlockState(CompoundMap compoundMap) {
		return HellblockBlockStateInterface.create(this, compoundMap);
	}

	@Override
	public HellblockBlockState createBlockState(String itemID) {
		return createBlockState();
	}

	public String id(HellblockBlockState state) {
		return state.get("key").getAsStringTag().map(StringTag::getValue).orElse("");
	}

	public void id(HellblockBlockState state, String id) {
		state.set("key", new StringTag("key", id));
	}

	protected boolean canTick(HellblockBlockState state, int interval) {
		if (interval <= 0)
			return false;
		if (interval == 1)
			return true;
		Tag<?> tag = state.get("tick");
		int tick = 0;
		if (tag != null)
			tick = tag.getAsIntTag().map(IntTag::getValue).orElse(0);
		if (++tick >= interval) {
			state.set("tick", new IntTag("tick", 0));
			return true;
		} else {
			state.set("tick", new IntTag("tick", tick));
			return false;
		}
	}

	@Override
	public void scheduledTick(HellblockBlockState state, HellblockWorld<?> world, Pos3 location, boolean offlineTick) {
		// ignore random tick
		if (world.setting().tickCropMode() == 1)
			return;
		if (canTick(state, world.setting().tickCropInterval())) {
			tickCrop(state, world, location, offlineTick);
		}
	}

	@Override
	public void randomTick(HellblockBlockState state, HellblockWorld<?> world, Pos3 location, boolean offlineTick) {
		// ignore scheduled tick
		if (world.setting().tickCropMode() == 2)
			return;
		if (canTick(state, world.setting().tickCropInterval())) {
			tickCrop(state, world, location, offlineTick);
		}
	}

	private void tickCrop(HellblockBlockState state, HellblockWorld<?> world, Pos3 location, boolean offline) {
		HellblockPlugin plugin = HellblockPlugin.getInstance();

		World bukkitWorld = world.bukkitWorld();
		Location bukkitLocation = location.toLocation(bukkitWorld);

		Runnable task = () -> {

			plugin.getScheduler().sync().run(() -> {

			}, bukkitLocation);
		};
		task.run();
	}
}