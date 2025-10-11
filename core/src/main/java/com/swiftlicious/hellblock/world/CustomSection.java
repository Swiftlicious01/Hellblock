package com.swiftlicious.hellblock.world;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.NotNull;

public class CustomSection implements CustomSectionInterface {

	private final int sectionID;
	private final ConcurrentMap<BlockPos, CustomBlockState> blocks;

	protected CustomSection(int sectionID) {
		this.sectionID = sectionID;
		this.blocks = new ConcurrentHashMap<>();
	}

	protected CustomSection(int sectionID, ConcurrentMap<BlockPos, CustomBlockState> blocks) {
		this.sectionID = sectionID;
		this.blocks = blocks;
	}

	@Override
	public int getSectionID() {
		return sectionID;
	}

	@NotNull
	@Override
	public Optional<CustomBlockState> getBlockState(BlockPos pos) {
		return Optional.ofNullable(blocks.get(pos));
	}

	@NotNull
	@Override
	public Optional<CustomBlockState> removeBlockState(BlockPos pos) {
		return Optional.ofNullable(blocks.remove(pos));
	}

	@NotNull
	@Override
	public Optional<CustomBlockState> addBlockState(BlockPos pos, CustomBlockState block) {
		return Optional.ofNullable(blocks.put(pos, block));
	}

	@Override
	public boolean canPrune() {
		return blocks.isEmpty();
	}

	@Override
	public CustomBlockState[] blocks() {
		return blocks.values().toArray(new CustomBlockState[0]);
	}

	@Override
	public Map<BlockPos, CustomBlockState> blockMap() {
		return blocks;
	}
}