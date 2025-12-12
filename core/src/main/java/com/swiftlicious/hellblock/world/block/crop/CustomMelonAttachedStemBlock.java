package com.swiftlicious.hellblock.world.block.crop;

import java.util.Locale;

import org.bukkit.block.BlockFace;

import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.block.Directional;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

public class CustomMelonAttachedStemBlock extends BaseGrowableBlock implements Directional {

	private static final String FACING_TAG = "facing";

	public CustomMelonAttachedStemBlock(Key type) {
		super(type, "age", 7);
	}

	@Override
	public BlockFace getFacing(CustomBlockState state) {
		BinaryTag tag = state.get(FACING_TAG);
		return BlockFace.valueOf(((tag instanceof StringBinaryTag it) ? it.value() : "NORTH").toUpperCase(Locale.ROOT));
	}

	@Override
	public void setFacing(CustomBlockState state, BlockFace face) {
		state.set(FACING_TAG, StringBinaryTag.stringBinaryTag(face.name().toLowerCase(Locale.ROOT)));
	}
}