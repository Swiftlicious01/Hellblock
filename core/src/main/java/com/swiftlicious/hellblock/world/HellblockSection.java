package com.swiftlicious.hellblock.world;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.NotNull;

public class HellblockSection implements HellblockSectionInterface {

	private final int sectionID;
	private final ConcurrentMap<BlockPos, HellblockBlockState> blocks;

	protected HellblockSection(int sectionID) {
		this.sectionID = sectionID;
		this.blocks = new ConcurrentHashMap<>();
	}

	protected HellblockSection(int sectionID, ConcurrentMap<BlockPos, HellblockBlockState> blocks) {
		this.sectionID = sectionID;
		this.blocks = blocks;
	}

	@Override
	public int getSectionID() {
		return sectionID;
	}

	@NotNull
	@Override
	public Optional<HellblockBlockState> getBlockState(BlockPos pos) {
		return Optional.ofNullable(blocks.get(pos));
	}

	@NotNull
	@Override
	public Optional<HellblockBlockState> removeBlockState(BlockPos pos) {
		return Optional.ofNullable(blocks.remove(pos));
	}

	@NotNull
	@Override
	public Optional<HellblockBlockState> addBlockState(BlockPos pos, HellblockBlockState block) {
		return Optional.ofNullable(blocks.put(pos, block));
	}

	@Override
	public boolean canPrune() {
		return blocks.isEmpty();
	}

	@Override
	public HellblockBlockState[] blocks() {
		return blocks.values().toArray(new HellblockBlockState[0]);
	}

	@Override
	public Map<BlockPos, HellblockBlockState> blockMap() {
		return blocks;
	}
}