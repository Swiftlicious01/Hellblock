package com.swiftlicious.hellblock.api.compatibility;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;

import org.bukkit.World;

public class WorldEditHook {

	@Getter
	@Setter
	private WorldEditPlugin worldEdit = null;

	public boolean loadIslandSchematic(@NonNull World world, @NonNull File file) {
		BukkitWorld weWorld = new BukkitWorld(world);
		Clipboard clipboard;

		ClipboardFormat format = ClipboardFormats.findByFile(file);
		try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
			clipboard = reader.read();

			try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
				Operation operation = new ClipboardHolder(clipboard).createPaste(editSession).to(clipboard.getOrigin())
						.copyBiomes(true).copyEntities(true).ignoreAirBlocks(false).build();
				Operations.complete(operation);
				return true;
			}
		} catch (Exception e) {
			LogUtils.severe("Unable to load hellblock island schematic with WorldEdit.", e);
			return false;
		}
	}
}