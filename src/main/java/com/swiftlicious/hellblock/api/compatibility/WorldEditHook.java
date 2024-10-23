package com.swiftlicious.hellblock.api.compatibility;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.World;

public class WorldEditHook {

	@Getter
	@Setter
	private WorldEditPlugin worldEdit = null;

	public boolean loadIslandSchematic(@NonNull World world, @NonNull Location loc, @NonNull File file) {
		ClipboardFormat format = ClipboardFormats.findByFile(file);
		try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
			Clipboard clipboard = reader.read();
			com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

			try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
				Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
						.to(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ())).copyBiomes(false).copyEntities(false)
						.ignoreStructureVoidBlocks(true).ignoreAirBlocks(false).build();
				Operations.complete(operation);
				editSession.close();
				return true;
			}
		} catch (WorldEditException | IOException e) {
			LogUtils.severe("Unable to load hellblock island schematic with WorldEdit.", e);
			return false;
		}
	}
}