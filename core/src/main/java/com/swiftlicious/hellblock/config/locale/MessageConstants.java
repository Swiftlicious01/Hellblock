package com.swiftlicious.hellblock.config.locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public interface MessageConstants {

	TranslatableComponent.Builder HELLBLOCK_ENTRY_MESSAGE = Component.translatable().key("island.entry.message");
	TranslatableComponent.Builder HELLBLOCK_FAREWELL_MESSAGE = Component.translatable().key("island.farewell.message");
	TranslatableComponent.Builder HELLBLOCK_ABANDONED_ENTRY_MESSAGE = Component.translatable()
			.key("island.entry.abandoned.message");
	TranslatableComponent.Builder HELLBLOCK_ABANDONED_FAREWELL_MESSAGE = Component.translatable()
			.key("island.farewell.abandoned.message");

	TranslatableComponent.Builder MSG_HELLBLOCK_NO_SCHEMATIC_PERMISSION = Component.translatable()
			.key("message.hellblock.permission.schematic.denied");
	TranslatableComponent.Builder MSG_HELLBLOCK_NOT_FOUND = Component.translatable().key("command.hellblock.not_found");
	TranslatableComponent.Builder MSG_NOT_OWNER_OF_HELLBLOCK = Component.translatable()
			.key("command.hellblock.not_owner");
	TranslatableComponent.Builder MSG_HELLBLOCK_IS_ABANDONED = Component.translatable()
			.key("command.hellblock.is_abandoned");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_HELP = Component.translatable().key("command.hellblock.help.coop");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_HELP = Component.translatable()
			.key("command.hellblock.help.admin");
	TranslatableComponent.Builder MSG_HELLBLOCK_USER_HELP = Component.translatable().key("command.hellblock.help.user");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_ON_COOLDOWN = Component.translatable()
			.key("command.hellblock.reset_on_cooldown");
	TranslatableComponent.Builder MSG_HELLBLOCK_BIOME_ON_COOLDOWN = Component.translatable()
			.key("command.hellblock.biome_on_cooldown");
	TranslatableComponent.Builder MSG_HELLBLOCK_TRANSFER_ON_COOLDOWN = Component.translatable()
			.key("command.hellblock.transfer_on_cooldown");
	TranslatableComponent.Builder MSG_HELLBLOCK_BIOME_SAME_BIOME = Component.translatable()
			.key("command.hellblock.biome.same.biome");
	TranslatableComponent.Builder MSG_HELLBLOCK_BIOME_CHANGED = Component.translatable()
			.key("command.hellblock.biome.changed");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVALID_BIOME = Component.translatable()
			.key("command.hellblock.biome.invalid");
	TranslatableComponent.Builder MSG_HELLBLOCK_LOCK_SUCCESS = Component.translatable()
			.key("command.hellblock.locked.island");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNLOCK_SUCCESS = Component.translatable()
			.key("command.hellblock.unlocked.island");
	TranslatableComponent.Builder MSG_HELLBLOCK_NOT_BANNED = Component.translatable()
			.key("command.hellblock.not_banned");
	TranslatableComponent.Builder MSG_HELLBLOCK_ALREADY_BANNED = Component.translatable()
			.key("command.hellblock.already_banned");
	TranslatableComponent.Builder MSG_HELLBLOCK_BANNED_PLAYER = Component.translatable()
			.key("command.hellblock.banned.from.island");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNBANNED_PLAYER = Component.translatable()
			.key("command.hellblock.unbanned.from.island");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_IN_PROCESS = Component.translatable()
			.key("command.hellblock.reset.in_process");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_NOT_IN_PROCESS = Component.translatable()
			.key("command.hellblock.reset.not_in_process");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_CONFIRMATION = Component.translatable()
			.key("command.hellblock.reset.confirmation");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_NEW_LOCATION = Component.translatable()
			.key("command.hellblock.home.new.location");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_SAME_LOCATION = Component.translatable()
			.key("command.hellblock.home.same.location");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_UNSAFE_STANDING_LOCATION = Component.translatable()
			.key("command.hellblock.home.standing.unsafe");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_ERROR_LOCATION = Component.translatable()
			.key("command.hellblock.home.standing.change.error");
	TranslatableComponent.Builder MSG_HELLBLOCK_MUST_BE_ON_ISLAND = Component.translatable()
			.key("command.hellblock.must_be_on_island");
	TranslatableComponent.Builder MSG_HELLBLOCK_NOT_TO_PARTY = Component.translatable()
			.key("command.hellblock.can_not_do_to_party");
	TranslatableComponent.Builder MSG_HELLBLOCK_CREATION_FAILURE_ALREADY_EXISTS = Component.translatable()
			.key("command.hellblock.creation.failure.already_exists");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_HEADER = Component.translatable()
			.key("command.hellblock.top.list.header");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_FORMAT = Component.translatable()
			.key("command.hellblock.top.format");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_NOT_FOUND = Component.translatable()
			.key("command.hellblock.top.not_found");
	TranslatableComponent.Builder MSG_HELLBLOCK_INFORMATION = Component.translatable()
			.key("command.hellblock.information.format");
	TranslatableComponent.Builder MSG_HELLBLOCK_INFO_LIST_STYLE = Component.translatable()
			.key("command.hellblock.information.list.style");
	TranslatableComponent.Builder MSG_HELLBLOCK_VISIT_ABANDONED = Component.translatable()
			.key("command.hellblock.visit.abandoned");
	TranslatableComponent.Builder MSG_HELLBLOCK_VISIT_ENTRY = Component.translatable()
			.key("command.hellblock.visit.entry");
	TranslatableComponent.Builder MSG_HELLBLOCK_LOCKED_FROM_VISITORS = Component.translatable()
			.key("command.hellblock.locked.from.visitors");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNSAFE_TO_VISIT = Component.translatable()
			.key("command.hellblock.unsafe.to.visit");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_TELEPORT = Component.translatable()
			.key("command.hellblock.home.teleport");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_NOT_BROKEN = Component.translatable()
			.key("command.hellblock.home.not.broken");
	TranslatableComponent.Builder MSG_HELLBLOCK_OPEN_SUCCESS = Component.translatable()
			.key("command.hellblock.open.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_OPEN_FAILURE_NOT_LOADED = Component.translatable()
			.key("command.hellblock.open.failure.not_loaded");
	TranslatableComponent.Builder MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED = Component.translatable()
			.key("command.hellblock.owner.data.not_loaded");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNSAFE_ENVIRONMENT = Component.translatable()
			.key("message.hellblock.login.unsafe.environment");
	TranslatableComponent.Builder MSG_HELLBLOCK_LOGIN_ABANDONED = Component.translatable()
			.key("message.hellblock.login.abandoned");
	TranslatableComponent.Builder MSG_HELLBLOCK_GROWING_GLOWSTONE_TREE = Component.translatable()
			.key("message.hellblock.grow.glowstone.tree");
	TranslatableComponent.Builder MSG_HELLBLOCK_BANNED_ENTRY = Component.translatable()
			.key("message.hellblock.banned.from.entry");
	TranslatableComponent.Builder MSG_HELLBLOCK_LOCKED_ENTRY = Component.translatable()
			.key("message.hellblock.locked.no.entry");
	TranslatableComponent.Builder MSG_HELLBLOCK_CREATE_PROCESS = Component.translatable()
			.key("message.hellblock.create.process");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_NEW_OPTION = Component.translatable()
			.key("message.hellblock.reset.choose.new.option");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_PROCESS = Component.translatable()
			.key("message.hellblock.reset.process");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_PARTY_NOTIFICATION = Component.translatable()
			.key("message.hellblock.reset.party.notification");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_PARTY_FORCED_NOTIFICATION = Component.translatable()
			.key("message.hellblock.reset.forceful.party.notification");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNSAFE_CONDITIONS = Component.translatable()
			.key("message.hellblock.spawn.unsafe.conditions");
	TranslatableComponent.Builder MSG_HELLBLOCK_ERROR_HOME_LOCATION = Component.translatable()
			.key("message.hellblock.error.teleport.to.home");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNSAFE_TELEPORT_TO_ISLAND = Component.translatable()
			.key("message.hellblock.unsafe.teleport.to.island");
	TranslatableComponent.Builder MSG_HELLBLOCK_PLAYER_NOT_FOUND = Component.translatable()
			.key("message.hellblock.player.not.found");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNSAFE_LINKING = Component.translatable()
			.key("message.hellblock.unsafe.linking");
	TranslatableComponent.Builder MSG_HELLBLOCK_LINK_OWN = Component.translatable().key("message.hellblock.link.own");
	TranslatableComponent.Builder MSG_HELLBLOCK_LINK_FAILURE_SAME_PARTY = Component.translatable()
			.key("message.hellblock.link.failure.same.party");
	TranslatableComponent.Builder MSG_HELLBLOCK_LINK_FAILURE_NO_ISLAND = Component.translatable()
			.key("message.hellblock.link.failure.no.island");
	TranslatableComponent.Builder MSG_HELLBLOCK_LINK_FAILURE_LOCKED = Component.translatable()
			.key("message.hellblock.link.failure.locked");
	TranslatableComponent.Builder MSG_HELLBLOCK_LINK_FAILURE_BANNED = Component.translatable()
			.key("message.hellblock.link.failure.banned");
	TranslatableComponent.Builder MSG_HELLBLOCK_LINK_SUCCESS = Component.translatable()
			.key("message.hellblock.link.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_LINK_TUTORIAL = Component.translatable()
			.key("message.hellblock.link.tutorial");
	TranslatableComponent.Builder MSG_HELLBLOCK_NO_LEAVING_BORDER = Component.translatable()
			.key("message.hellblock.no.exit.border");
	TranslatableComponent.Builder MSG_HELLBLOCK_PLAYER_OFFLINE = Component.translatable()
			.key("message.hellblock.player.offline");
	TranslatableComponent.Builder MSG_HELLBLOCK_NO_ISLAND_FOUND = Component.translatable()
			.key("message.hellblock.player.no_island_found");
	TranslatableComponent.Builder MSG_HELLBLOCK_NOT_TO_SELF = Component.translatable()
			.key("message.hellblock.player.can_not_do_to_self");
	TranslatableComponent.Builder MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD = Component.translatable()
			.key("message.hellblock.player.data.failure.load");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_WRONG_WORLD = Component.translatable()
			.key("message.hellblock.admin.wrong_world");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_SPAWN_ALREADY_EXISTS = Component.translatable()
			.key("message.hellblock.admin.spawn_already_exists");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_SPAWN_GENERATED = Component.translatable()
			.key("message.hellblock.admin.spawn_generated");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ISLAND_DELETED = Component.translatable()
			.key("message.hellblock.admin.island_deleted");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_PURGE_UNAVAILABLE = Component.translatable()
			.key("message.hellblock.admin.purge.unavailable");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_PURGE_SUCCESS = Component.translatable()
			.key("message.hellblock.admin.purge.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_PURGE_FAILURE = Component.translatable()
			.key("message.hellblock.admin.purge.failure");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_FORCE_TELEPORT = Component.translatable()
			.key("message.hellblock.admin.force.teleport");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_HOME_TO_BEDROCK = Component.translatable()
			.key("message.hellblock.home.set.to.bedrock");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NOT_TRUSTED = Component.translatable()
			.key("command.hellblock.coop.not_trusted");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ALREADY_TRUSTED = Component.translatable()
			.key("command.hellblock.coop.already_trusted");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_GIVEN = Component.translatable()
			.key("command.hellblock.coop.trust_given");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_GAINED = Component.translatable()
			.key("command.hellblock.coop.trust_gained");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_REVOKED = Component.translatable()
			.key("command.hellblock.coop.trust_revoked");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_LOST = Component.translatable()
			.key("command.hellblock.coop.trust_lost");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ALREADY_HAS_HELLBLOCK = Component.translatable()
			.key("message.hellblock.coop.already_has_hellblock");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NO_INVITE_SELF = Component.translatable()
			.key("message.hellblock.coop.can_not_invite_self");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ALREADY_IN_PARTY = Component.translatable()
			.key("message.hellblock.coop.already_part_of_party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY = Component.translatable()
			.key("message.hellblock.coop.not_part_of_party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_BANNED_FROM_INVITE = Component.translatable()
			.key("message.hellblock.coop.banned_from_island");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_CANCELLED = Component.translatable()
			.key("message.hellblock.coop.invite_cancelled");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_REVOKED = Component.translatable()
			.key("message.hellblock.coop.invite_revoked");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_EXISTS = Component.translatable()
			.key("message.hellblock.coop.already_has_invite");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_SENT = Component.translatable()
			.key("message.hellblock.coop.invite_sent");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_RECEIVED = Component.translatable()
			.key("message.hellblock.coop.invite_received");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS = Component.translatable()
			.key("message.hellblock.coop.hellblock_exists");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NO_INVITE_FOUND = Component.translatable()
			.key("message.hellblock.coop.no_invite_found");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NO_INVITES = Component.translatable()
			.key("message.hellblock.coop.no_invites");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_EXPIRED = Component.translatable()
			.key("message.hellblock.coop.invite_expired");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITATION_LIST = Component.translatable()
			.key("message.hellblock.coop.invitation_list");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_ABANDONED = Component.translatable()
			.key("message.hellblock.coop.abandoned_invite");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ALREADY_JOINED_PARTY = Component.translatable()
			.key("message.hellblock.coop.already_joined_party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NOT_IN_PARTY = Component.translatable()
			.key("message.hellblock.coop.not_in_party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ADDED_TO_PARTY = Component.translatable()
			.key("message.hellblock.coop.added_to_party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_JOINED_PARTY = Component.translatable()
			.key("message.hellblock.coop.joined_party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_PARTY_FULL = Component.translatable()
			.key("message.hellblock.coop.party_full");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_REJECTED_TO_OWNER = Component.translatable()
			.key("message.hellblock.coop.invite_rejected_to_owner");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_REJECTED = Component.translatable()
			.key("message.hellblock.coop.invite_rejected");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NO_KICK_SELF = Component.translatable()
			.key("message.hellblock.coop.can_not_kick_self");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_OWNER_NO_LEAVE = Component.translatable()
			.key("message.hellblock.coop.owner_can_not_leave");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_PARTY_LEFT = Component.translatable()
			.key("message.hellblock.coop.party_left");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_LEFT_PARTY = Component.translatable()
			.key("message.hellblock.coop.left_party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_PARTY_KICKED = Component.translatable()
			.key("message.hellblock.coop.party_kicked");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_REMOVED_FROM_PARTY = Component.translatable()
			.key("message.hellblock.coop.removed_from_party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRANSFER_OWNERSHIP_DISABLED = Component.translatable()
			.key("message.hellblock.coop.transfer_ownership_disabled");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NO_TRANSFER_SELF = Component.translatable()
			.key("message.hellblock.coop.can_not_transfer_to_self");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ALREADY_OWNER_OF_ISLAND = Component.translatable()
			.key("message.hellblock.coop.already_owner_of_island");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NEW_OWNER_SET = Component.translatable()
			.key("message.hellblock.coop.new_owner_set");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_OWNER_TRANSFER_SUCCESS = Component.translatable()
			.key("message.hellblock.coop.owner_transfer_success");
	TranslatableComponent.Builder MSG_HELLBLOCK_CHALLENGE_COMPLETED = Component.translatable()
			.key("message.hellblock.challenge.completed");
	TranslatableComponent.Builder MSG_HELLBLOCK_CHALLENGE_PROGRESS_BAR = Component.translatable()
			.key("message.hellblock.challenge.progress.bar");

	TranslatableComponent.Builder FORMAT_OPEN = Component.translatable().key("format.open");
	TranslatableComponent.Builder FORMAT_CLOSED = Component.translatable().key("format.closed");
	TranslatableComponent.Builder FORMAT_NONE = Component.translatable().key("format.none");
	TranslatableComponent.Builder FORMAT_UNRANKED = Component.translatable().key("format.unranked");
	TranslatableComponent.Builder FORMAT_UNKNOWN = Component.translatable().key("format.unknown");

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
	TranslatableComponent.Builder COMMAND_DEBUG_NBT_NO_ITEM_IN_HAND = Component.translatable()
			.key("command.debug.nbt.no.item.in.hand");
}