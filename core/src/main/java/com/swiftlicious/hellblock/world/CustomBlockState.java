package com.swiftlicious.hellblock.world;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.utils.extras.SynchronizedNBTCompound;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public class CustomBlockState implements CustomBlockStateInterface {

	private static final Set<String> AIR_KEYS = Set.of("minecraft:air", "hellblock:air", "custom:air", "void");

	private final SynchronizedNBTCompound compound;
	private final CustomBlock owner;

	protected CustomBlockState(CustomBlock owner, CompoundBinaryTag compoundTag) {
		this.compound = new SynchronizedNBTCompound(compoundTag);
		this.owner = owner;
	}

	@NotNull
	@Override
	public CustomBlock type() {
		return owner;
	}

	@Override
	public boolean isAir() {
		String keyStr = this.owner.type().value(); // access the Key directly
		return AIR_KEYS.contains(keyStr.toLowerCase(Locale.ROOT));
	}

	@Override
	public boolean hasInventory() {
		// Check for "Items" tag existence
		return compound().get("Items") != null;
	}

	@Override
	public void clearInventory() {
		compound().remove("Items");
	}

	@Override
	public byte[] getNBTDataAsBytes() {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			BinaryTagIO.writer().write(compound.original(), output);
			return output.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Failed to serialize NBT", e);
		}
	}

	@Override
	public String asString() {
		return owner.type().asString() + compound.asString();
	}

	public void set(String key, BinaryTag tag) {
		compound.put(key, tag);
	}

	public BinaryTag get(String key) {
		return compound.get(key);
	}

	public void remove(String key) {
		compound.remove(key);
	}

	public SynchronizedNBTCompound compound() {
		return compound;
	}

	@Override
	public String toString() {
		return "CustomBlockState{" + owner.type().asString() + compound.asString() + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final CustomBlockState that = (CustomBlockState) o;
		return this.owner.type().equals(that.owner.type()) && compound.equals(that.compound);
	}

	@Override
	public int hashCode() {
		final BinaryTag id = compound.get("id");
		if (id != null) {
			return 7 * id.hashCode() + 13 * owner.type().hashCode();
		}
		return owner.type().hashCode();
	}
}