package com.swiftlicious.hellblock.commands.sub;

import dev.jorel.commandapi.CommandAPICommand;

import java.io.File;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.gui.page.file.FileSelector;

public class GUIEditorCommand {

	public static GUIEditorCommand INSTANCE = new GUIEditorCommand();

	public CommandAPICommand getEditorCommand() {
		return new CommandAPICommand("browser").executesPlayer((player, arg) -> {
			new FileSelector(player, new File(HellblockPlugin.getInstance().getDataFolder(), "contents"));
		});
	}
}
