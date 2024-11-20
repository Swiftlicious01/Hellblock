package com.swiftlicious.hellblock.config.locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public interface MessageConstants {

	TranslatableComponent.Builder MSG_PREFIX = Component.translatable().key("prefix");

	TranslatableComponent.Builder MSG_HELLBLOCK_NOT_FOUND = Component.translatable()
			.key("command.hellblock.not_found");
	TranslatableComponent.Builder MSG_NOT_OWNER_OF_HELLBLOCK = Component.translatable()
			.key("command.hellblock.not_owner");
	TranslatableComponent.Builder MSG_HELLBLOCK_IS_ABANDONED = Component.translatable()
			.key("command.hellblock.is_abandoned");

	TranslatableComponent.Builder FORMAT_SECOND = Component.translatable().key("format.second");
	TranslatableComponent.Builder FORMAT_MINUTE = Component.translatable().key("format.minute");
	TranslatableComponent.Builder FORMAT_HOUR = Component.translatable().key("format.hour");
	TranslatableComponent.Builder FORMAT_DAY = Component.translatable().key("format.day");

	TranslatableComponent.Builder COMMAND_RELOAD_SUCCESS = Component.translatable().key("command.reload.success");
	TranslatableComponent.Builder COMMAND_ITEM_FAILURE_NOT_EXIST = Component.translatable()
			.key("command.item.failure.not_exist");
	TranslatableComponent.Builder COMMAND_ITEM_GIVE_SUCCESS = Component.translatable().key("command.item.give.success");
	TranslatableComponent.Builder COMMAND_ITEM_GET_SUCCESS = Component.translatable().key("command.item.get.success");
	TranslatableComponent.Builder COMMAND_ITEM_IMPORT_FAILURE_NO_ITEM = Component.translatable()
			.key("command.item.import.failure.no_item");
	TranslatableComponent.Builder COMMAND_ITEM_IMPORT_SUCCESS = Component.translatable()
			.key("command.item.import.success");
	TranslatableComponent.Builder COMMAND_FISH_FINDER_POSSIBLE_LOOTS = Component.translatable()
			.key("command.fish_finder.possible_loots");
	TranslatableComponent.Builder COMMAND_FISH_FINDER_NO_LOOT = Component.translatable()
			.key("command.fish_finder.no_loot");
	TranslatableComponent.Builder COMMAND_FISH_FINDER_SPLIT_CHAR = Component.translatable()
			.key("command.fish_finder.split_char");
	TranslatableComponent.Builder COMMAND_DATA_FAILURE_NOT_LOADED = Component.translatable()
			.key("command.data.failure.not_loaded");
	TranslatableComponent.Builder COMMAND_MARKET_OPEN_SUCCESS = Component.translatable()
			.key("command.market.open.success");
	TranslatableComponent.Builder COMMAND_MARKET_OPEN_FAILURE_NOT_LOADED = Component.translatable()
			.key("command.market.open.failure.not_loaded");
	TranslatableComponent.Builder COMMAND_DATA_UNLOCK_SUCCESS = Component.translatable()
			.key("command.data.unlock.success");
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_FAILURE_NOT_EXISTS = Component.translatable()
			.key("command.data.import.failure.not_exists");
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_FAILURE_PLAYER_ONLINE = Component.translatable()
			.key("command.data.import.failure.player_online");
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_FAILURE_INVALID_FILE = Component.translatable()
			.key("command.data.import.failure.invalid_file");
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_START = Component.translatable().key("command.data.import.start");
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_PROGRESS = Component.translatable()
			.key("command.data.import.progress");
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_SUCCESS = Component.translatable()
			.key("command.data.import.success");
	TranslatableComponent.Builder COMMAND_DATA_EXPORT_FAILURE_PLAYER_ONLINE = Component.translatable()
			.key("command.data.export.failure.player_online");
	TranslatableComponent.Builder COMMAND_DATA_EXPORT_START = Component.translatable().key("command.data.export.start");
	TranslatableComponent.Builder COMMAND_DATA_EXPORT_PROGRESS = Component.translatable()
			.key("command.data.export.progress");
	TranslatableComponent.Builder COMMAND_DATA_EXPORT_SUCCESS = Component.translatable()
			.key("command.data.export.success");
	TranslatableComponent.Builder COMMAND_STATISTICS_FAILURE_NOT_LOADED = Component.translatable()
			.key("command.statistics.failure.not_loaded");
	TranslatableComponent.Builder COMMAND_STATISTICS_FAILURE_UNSUPPORTED = Component.translatable()
			.key("command.statistics.failure.unsupported");
	TranslatableComponent.Builder COMMAND_STATISTICS_MODIFY_SUCCESS = Component.translatable()
			.key("command.statistics.modify.success");
	TranslatableComponent.Builder COMMAND_STATISTICS_RESET_SUCCESS = Component.translatable()
			.key("command.statistics.reset.success");
	TranslatableComponent.Builder COMMAND_STATISTICS_QUERY_AMOUNT = Component.translatable()
			.key("command.statistics.query.amount");
	TranslatableComponent.Builder COMMAND_STATISTICS_QUERY_SIZE = Component.translatable()
			.key("command.statistics.query.size");
	TranslatableComponent.Builder COMMAND_DEBUG_LOOT_FAILURE_ROD = Component.translatable()
			.key("command.debug.loot.failure.rod");
	TranslatableComponent.Builder COMMAND_DEBUG_LOOT_SUCCESS = Component.translatable()
			.key("command.debug.loot.success");
	TranslatableComponent.Builder COMMAND_DEBUG_LOOT_FAILURE_NO_LOOT = Component.translatable()
			.key("command.debug.loot.failure.no_loot");

	TranslatableComponent.Builder GUI_SCROLL_UP = Component.translatable().key("gui.scroll_up");
	TranslatableComponent.Builder GUI_SCROLL_DOWN = Component.translatable().key("gui.scroll_down");
	TranslatableComponent.Builder GUI_CANNOT_SCROLL_UP = Component.translatable().key("gui.cannot_scroll_up");
	TranslatableComponent.Builder GUI_CANNOT_SCROLL_DOWN = Component.translatable().key("gui.cannot_scroll_down");
	TranslatableComponent.Builder GUI_NEXT_PAGE = Component.translatable().key("gui.next_page");
	TranslatableComponent.Builder GUI_GOTO_NEXT_PAGE = Component.translatable().key("gui.goto_next_page");
	TranslatableComponent.Builder GUI_CANNOT_GOTO_NEXT_PAGE = Component.translatable().key("gui.cannot_goto_next_page");
	TranslatableComponent.Builder GUI_PREVIOUS_PAGE = Component.translatable().key("gui.previous_page");
	TranslatableComponent.Builder GUI_GOTO_PREVIOUS_PAGE = Component.translatable().key("gui.goto_previous_page");
	TranslatableComponent.Builder GUI_CANNOT_GOTO_PREVIOUS_PAGE = Component.translatable()
			.key("gui.cannot_goto_previous_page");
	TranslatableComponent.Builder GUI_BACK_TO_PARENT_PAGE = Component.translatable().key("gui.back_to_parent_page");
}