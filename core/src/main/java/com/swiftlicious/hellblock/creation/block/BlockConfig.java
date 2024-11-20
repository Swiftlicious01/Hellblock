package com.swiftlicious.hellblock.creation.block;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.utils.extras.MathValue;

import static java.util.Objects.requireNonNull;

public class BlockConfig implements BlockConfigInterface {

	private final String blockID;
	private final MathValue<Player> horizontalVector;
	private final MathValue<Player> verticalVector;
	private final List<BlockDataModifier> dataModifierList;
	private final List<BlockStateModifier> stateModifierList;
	private final String id;

	public BlockConfig(String id, String blockID, MathValue<Player> horizontalVector, MathValue<Player> verticalVector,
			List<BlockDataModifier> dataModifierList, List<BlockStateModifier> stateModifierList) {
		this.blockID = blockID;
		this.dataModifierList = dataModifierList;
		this.stateModifierList = stateModifierList;
		this.horizontalVector = horizontalVector;
		this.verticalVector = verticalVector;
		this.id = id;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String blockID() {
		return blockID;
	}

	@Override
	public MathValue<Player> horizontalVector() {
		return horizontalVector;
	}

	@Override
	public MathValue<Player> verticalVector() {
		return verticalVector;
	}

	@Override
	public List<BlockDataModifier> dataModifier() {
		return dataModifierList;
	}

	@Override
	public List<BlockStateModifier> stateModifiers() {
		return stateModifierList;
	}

	public static class Builder implements BuilderInterface {
		private String blockID;
		private final List<BlockDataModifier> dataModifierList = new ArrayList<>();
		private final List<BlockStateModifier> stateModifierList = new ArrayList<>();
		private MathValue<Player> horizontalVector = BlockConfig.DEFAULT_HORIZONTAL_VECTOR;
		private MathValue<Player> verticalVector = BlockConfig.DEFAULT_VERTICAL_VECTOR;
		private String id;

		@Override
		public Builder id(String id) {
			this.id = id;
			return this;
		}

		@Override
		public Builder blockID(String blockID) {
			this.blockID = blockID;
			return this;
		}

		@Override
		public Builder verticalVector(MathValue<Player> value) {
			this.verticalVector = value;
			return this;
		}

		@Override
		public Builder horizontalVector(MathValue<Player> value) {
			this.horizontalVector = value;
			return this;
		}

		@Override
		public Builder dataModifierList(List<BlockDataModifier> dataModifierList) {
			this.dataModifierList.addAll(dataModifierList);
			return this;
		}

		@Override
		public Builder stateModifierList(List<BlockStateModifier> stateModifierList) {
			this.stateModifierList.addAll(stateModifierList);
			return this;
		}

		@Override
		public BlockConfig build() {
			return new BlockConfig(id, requireNonNull(blockID, "Block id should not be null"), horizontalVector,
					verticalVector, dataModifierList, stateModifierList);
		}
	}
}