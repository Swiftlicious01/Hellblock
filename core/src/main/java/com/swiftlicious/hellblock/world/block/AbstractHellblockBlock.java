package com.swiftlicious.hellblock.world.block;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.utils.extras.NamedTextColor;
import com.swiftlicious.hellblock.world.HellblockBlockState;
import com.swiftlicious.hellblock.world.HellblockBlockStateInterface;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;
import com.swiftlicious.hellblock.world.wrapper.WrappedBreakEvent;
import com.swiftlicious.hellblock.world.wrapper.WrappedInteractEvent;
import com.swiftlicious.hellblock.world.wrapper.WrappedPlaceEvent;

public abstract class AbstractHellblockBlock implements HellblockBlock {

	private final Key type;

	public AbstractHellblockBlock(Key type) {
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
	}

	@Override
	public void randomTick(HellblockBlockState state, HellblockWorld<?> world, Pos3 location, boolean offlineTick) {
	}

	@Override
	public void onInteract(WrappedInteractEvent event) {
	}

	@Override
	public void onBreak(WrappedBreakEvent event) {
	}

	@Override
	public void onPlace(WrappedPlaceEvent event) {
	}

	@Override
	public NamedTextColor insightColor() {
		return NamedTextColor.WHITE;
	}
}