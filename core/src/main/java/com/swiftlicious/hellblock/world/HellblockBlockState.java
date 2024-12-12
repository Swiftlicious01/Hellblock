package com.swiftlicious.hellblock.world;

import org.jetbrains.annotations.NotNull;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.Tag;
import com.swiftlicious.hellblock.utils.TagUtils;
import com.swiftlicious.hellblock.utils.extras.SynchronizedCompoundMap;

public class HellblockBlockState implements HellblockBlockStateInterface {

	private final SynchronizedCompoundMap compoundMap;
	private final HellblockBlock owner;

	protected HellblockBlockState(HellblockBlock owner, CompoundMap compoundMap) {
		this.compoundMap = new SynchronizedCompoundMap(compoundMap);
		this.owner = owner;
	}

	@NotNull
	@Override
	public HellblockBlock type() {
		return owner;
	}

	@Override
	public byte[] getNBTDataAsBytes() {
		return TagUtils.toBytes(new CompoundTag("data", compoundMap.originalMap()));
	}

	@Override
	public String asString() {
		return owner.type().asString() + compoundMap.asString();
	}

	@Override
	public Tag<?> set(String key, Tag<?> tag) {
		return compoundMap.put(key, tag);
	}

	@Override
	public Tag<?> get(String key) {
		return compoundMap.get(key);
	}

	@Override
	public Tag<?> remove(String key) {
		return compoundMap.remove(key);
	}

	@Override
	public SynchronizedCompoundMap compoundMap() {
		return compoundMap;
	}

	@Override
	public String toString() {
		return "HellblockBlockState{" + owner.type().asString() + compoundMap.asString() + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		HellblockBlockState that = (HellblockBlockState) o;
		return compoundMap.equals(that.compoundMap);
	}

	// Due to the defects of flownbt itself, hash efficiency is very low
	@Override
	public int hashCode() {
		Tag<?> id = compoundMap.get("id");
		if (id != null) {
			return 7 * id.hashCode() + 13 * owner.type().hashCode();
		}
		return owner.type().hashCode();
	}
}