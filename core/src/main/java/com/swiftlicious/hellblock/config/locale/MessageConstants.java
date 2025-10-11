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
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TOGGLE_INVITES_ON = Component.translatable()
			.key("command.hellblock.coop.toggle.invites.on");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TOGGLE_INVITES_OFF = Component.translatable()
			.key("command.hellblock.coop.toggle.invites.off");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TOGGLE_JOIN_ON = Component.translatable()
			.key("command.hellblock.coop.toggle.join.on");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TOGGLE_JOIN_OFF = Component.translatable()
			.key("command.hellblock.coop.toggle.join.off");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TOGGLE_INVALID_ARGUMENT = Component.translatable()
			.key("command.hellblock.coop.toggle.invalid");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_DISPLAY_TOGGLED = Component.translatable()
			.key("command.hellblock.display.toggled");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_DISPLAY_UNCHANGED = Component.translatable()
			.key("command.hellblock.display.unchanged");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_DISPLAY_SET = Component.translatable()
			.key("command.hellblock.display.set");
	TranslatableComponent.Builder MSG_HELLBLOCK_VISIT_LOG_HEADER = Component.translatable()
			.key("message.hellblock.visit.log.header");
	TranslatableComponent.Builder MSG_HELLBLOCK_VISIT_LOG_ENTRY = Component.translatable()
			.key("message.hellblock.visit.log.entry");
	TranslatableComponent.Builder MSG_HELLBLOCK_VISIT_LOG_EMPTY = Component.translatable()
			.key("message.hellblock.visit.log.empty");
	TranslatableComponent.Builder COMMAND_INVALID_PAGE_ARGUMENT = Component.translatable()
			.key("command.invalid.page.argument");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_EMPTY = Component.translatable()
			.key("command.hellblock.name.empty");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_SHOW = Component.translatable()
			.key("command.hellblock.name.show");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_TOO_LONG = Component.translatable()
			.key("command.hellblock.name.too_long");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_BANNED_WORDS = Component.translatable()
			.key("command.hellblock.name.banned_words");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_UNCHANGED = Component.translatable()
			.key("command.hellblock.name.unchanged");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_RESET_DEFAULT = Component.translatable()
			.key("command.hellblock.name.reset_default");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_SET_SUCCESS = Component.translatable()
			.key("command.hellblock.name.set_success");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_DISPLAY_INVALID = Component.translatable()
			.key("command.hellblock.display.invalid");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_TOO_LONG = Component.translatable()
			.key("command.hellblock.bio.too_long");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_BANNED_WORDS = Component.translatable()
			.key("command.hellblock.bio.banned_words");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_RESET_DEFAULT = Component.translatable()
			.key("command.hellblock.bio.reset_default");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_TOO_MANY_LINES = Component.translatable()
			.key("command.hellblock.bio.too_many_lines");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_UNCHANGED = Component.translatable()
			.key("command.hellblock.bio.unchanged");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_EMPTY = Component.translatable()
			.key("command.hellblock.bio.empty");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_SHOW = Component.translatable()
			.key("command.hellblock.bio.show");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_SET_SUCCESS = Component.translatable()
			.key("command.hellblock.bio.set_success");
	TranslatableComponent.Builder MSG_HELLBLOCK_BIO_DEFAULT = Component.translatable()
			.key("message.hellblock.bio.auto_default");
	TranslatableComponent.Builder MSG_HELLBLOCK_NAME_DEFAULT = Component.translatable()
			.key("message.hellblock.name.auto_default");
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
	TranslatableComponent.Builder MSG_HELLBLOCK_WARP_NEW_LOCATION = Component.translatable()
			.key("command.hellblock.warp.new.location");
	TranslatableComponent.Builder MSG_HELLBLOCK_WARP_SAME_LOCATION = Component.translatable()
			.key("command.hellblock.warp.same.location");
	TranslatableComponent.Builder MSG_HELLBLOCK_WARP_UNSAFE_STANDING_LOCATION = Component.translatable()
			.key("command.hellblock.warp.standing.unsafe");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_ERROR_LOCATION = Component.translatable()
			.key("command.hellblock.home.standing.change.error");
	TranslatableComponent.Builder MSG_HELLBLOCK_MUST_BE_ON_ISLAND = Component.translatable()
			.key("command.hellblock.must_be_on_island");
	TranslatableComponent.Builder MSG_HELLBLOCK_NOT_TO_PARTY = Component.translatable()
			.key("command.hellblock.can_not_do_to_party");
	TranslatableComponent.Builder MSG_HELLBLOCK_CREATION_FAILURE_ALREADY_EXISTS = Component.translatable()
			.key("command.hellblock.creation.failure.already_exists");
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
	TranslatableComponent.Builder MSG_HELLBLOCK_NO_VISITORS = Component.translatable()
			.key("command.hellblock.visitors.none");
	TranslatableComponent.Builder MSG_HELLBLOCK_VISITOR_LIST = Component.translatable()
			.key("command.hellblock.visitors.list");
	TranslatableComponent.Builder COMMAND_INVALID_SORT_TYPE = Component.translatable()
		    .key("command.hellblock.visit.menu.invalid_sort");
		TranslatableComponent.Builder COMMAND_VISIT_GUI_FAILED = Component.translatable()
		    .key("command.hellblock.visit.menu.failed"); 
	// Upgrades
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_RANGE = Component.translatable()
			.key("message.hellblock.upgrade.range");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_PARTY = Component.translatable()
			.key("message.hellblock.upgrade.party");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_HOPPER = Component.translatable()
			.key("message.hellblock.upgrade.hopper");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_GENERATOR = Component.translatable()
			.key("message.hellblock.upgrade.generator");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_BARTERING = Component.translatable()
			.key("message.hellblock.upgrade.bartering");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_CROP = Component.translatable()
			.key("message.hellblock.upgrade.crop");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_MOB = Component.translatable()
			.key("message.hellblock.upgrade.mob");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_RANGE_MEMBER = Component.translatable()
			.key("message.hellblock.upgrade.range.member");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_PARTY_MEMBER = Component.translatable()
			.key("message.hellblock.upgrade.party.member");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_HOPPER_MEMBER = Component.translatable()
			.key("message.hellblock.upgrade.hopper.member");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_GENERATOR_MEMBER = Component.translatable()
			.key("message.hellblock.upgrade.generator.member");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_BARTERING_MEMBER = Component.translatable()
			.key("message.hellblock.upgrade.bartering.member");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_CROP_MEMBER = Component.translatable()
			.key("message.hellblock.upgrade.crop.member");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_MOB_MEMBER = Component.translatable()
			.key("message.hellblock.upgrade.mob.member");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_MAX_TIER = Component.translatable()
			.key("message.hellblock.upgrade.max_tier");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVALID_UPGRADE = Component.translatable()
			.key("message.hellblock.upgrade.invalid");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_NO_COST = Component.translatable()
			.key("message.hellblock.upgrade.no_cost");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_CANNOT_AFFORD = Component.translatable()
			.key("message.hellblock.upgrade.cannot_afford");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_SUCCESS = Component.translatable()
			.key("message.hellblock.upgrade.success");
	// Hopper limiting
	TranslatableComponent.Builder MSG_HELLBLOCK_HOPPER_INFO = Component.translatable()
			.key("message.hellblock.hopper.info");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOPPER_LIMIT_REACHED = Component.translatable()
			.key("message.hellblock.hopper.limit_reached");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOPPER_EXCESS_WARNING = Component.translatable()
			.key("message.hellblock.hopper.excess_warning");
	// Featured slots
	TranslatableComponent.Builder MSG_FEATURED_EXISTS = Component.translatable()
			.key("message.hellblock.featured.exists");
	TranslatableComponent.Builder MSG_FEATURED_FULL = Component.translatable().key("message.hellblock.featured.full");
	TranslatableComponent.Builder MSG_FEATURED_FAILED_PURCHASE = Component.translatable()
			.key("message.hellblock.featured.fail");
	TranslatableComponent.Builder MSG_FEATURED_SUCCESS = Component.translatable()
			.key("message.hellblock.featured.success");
	// Block protection
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_BREAK_DENY = Component.translatable()
			.key("message.hellblock.protection.break.deny");
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_PLACE_DENY = Component.translatable()
			.key("message.hellblock.protection.place.deny");
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_BUILD_DENY = Component.translatable()
			.key("message.hellblock.protection.build.deny");
	// Animal damage protection
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_ANIMAL_DAMAGE_DENY = Component.translatable()
			.key("message.hellblock.protection.animal.damage.deny");
	// Mob damage protection
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_MOB_DAMAGE_DENY = Component.translatable()
			.key("message.hellblock.protection.mob.damage.deny");
	// Firework damage protection
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_FIREWORK_DAMAGE_DENY = Component.translatable()
			.key("message.hellblock.protection.firework.damage.deny");
	// Interaction protection
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_USE_DENY = Component.translatable()
			.key("message.hellblock.protection.use.deny");
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_INTERACT_DENY = Component.translatable()
			.key("message.hellblock.protection.interact.deny");
	// Bucket protection
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_BUCKET_FILL_DENY = Component.translatable()
			.key("message.hellblock.protection.bucket.fill.deny");
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_BUCKET_EMPTY_DENY = Component.translatable()
			.key("message.hellblock.protection.bucket.empty.deny");
	// Projectile protection
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_PROJECTILE_DENY = Component.translatable()
			.key("message.hellblock.protection.projectile.deny");
	// Explosion protection
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_EXPLOSION_DENY = Component.translatable()
			.key("message.hellblock.protection.explosion.deny");
	// Wither spawn protection
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_WITHER_SPAWN_DENY = Component.translatable()
			.key("message.hellblock.protection.wither.spawn.deny");
	// PVP protection
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_PVP_DENY = Component.translatable()
			.key("message.hellblock.protection.pvp.deny");
	// Chest & container access
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_CHEST_ACCESS_DENY = Component.translatable()
			.key("message.hellblock.protection.chest.access.deny");
	// Anvil usage
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_USE_ANVIL_DENY = Component.translatable()
			.key("message.hellblock.protection.use.anvil.deny");
	// Dripleaf usage
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_USE_DRIPLEAF_DENY = Component.translatable()
			.key("message.hellblock.protection.use.dripleaf.deny");
	// Vehicle place & destroy
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_VEHICLE_PLACE_DENY = Component.translatable()
			.key("message.hellblock.protection.vehicle.place.deny");
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_VEHICLE_DESTROY_DENY = Component.translatable()
			.key("message.hellblock.protection.vehicle.destroy.deny");
	// Ride
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_RIDE_DENY = Component.translatable()
			.key("message.hellblock.protection.ride.deny");
	// Enderpearl usage
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_ENDERPEARL_DENY = Component.translatable()
			.key("message.hellblock.protection.enderpearl.deny");
	// Chorus fruit teleport
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_CHORUS_TELEPORT_DENY = Component.translatable()
			.key("message.hellblock.protection.chorus.teleport.deny");
	// Block trampling
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_TRAMPLE_DENY = Component.translatable()
			.key("message.hellblock.protection.trample.deny");
	// Item frame rotation
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_ITEM_FRAME_ROTATE_DENY = Component.translatable()
			.key("message.hellblock.protection.itemframe.rotate.deny");
	// Mob spawn
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_MOB_SPAWN_DENY = Component.translatable()
			.key("message.hellblock.protection.mob.spawn.deny");
	// Lighter (flint & steel)
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_LIGHTER_DENY = Component.translatable()
			.key("message.hellblock.protection.lighter.deny");
	// Respawn anchors
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_RESPAWN_ANCHOR_DENY = Component.translatable()
			.key("message.hellblock.protection.respawn.anchor.deny");
	// Wind charge burst
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_WIND_CHARGE_DENY = Component.translatable()
			.key("message.hellblock.protection.wind.charge.deny");
	// Potion splash
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_POTION_SPLASH_DENY = Component.translatable()
			.key("message.hellblock.protection.potion.splash.deny");
	// Snowman trails
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_SNOWMAN_TRAILS_DENY = Component.translatable()
			.key("message.hellblock.protection.snowman.trails.deny");
	// Enderman grief
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_ENDERMAN_GRIEF_DENY = Component.translatable()
			.key("message.hellblock.protection.enderman.grief.deny");
	// Ghast fireball
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_GHAST_FIREBALL_DENY = Component.translatable()
			.key("message.hellblock.protection.ghast.fireball.deny");
	// Fall damage
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_FALL_DAMAGE_DENY = Component.translatable()
			.key("message.hellblock.protection.fall.damage.deny");
	// Health regen
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_HEALTH_REGEN_DENY = Component.translatable()
			.key("message.hellblock.protection.health.regen.deny");
	// Hunger drain
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_HUNGER_DRAIN_DENY = Component.translatable()
			.key("message.hellblock.protection.hunger.drain.deny");
	// Entry
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_ENTRY_DENY = Component.translatable()
			.key("message.hellblock.protection.entry.deny");
	// Invincibility
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_INVINCIBLE_DENY = Component.translatable()
			.key("message.hellblock.protection.invincible.deny");
	// Sleep
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_SLEEP_DENY = Component.translatable()
			.key("message.hellblock.protection.sleep.deny");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNSAFE_ENVIRONMENT = Component.translatable()
			.key("message.hellblock.login.unsafe.environment");
	TranslatableComponent.Builder MSG_HELLBLOCK_CLEARED_INVENTORY = Component.translatable()
			.key("message.hellblock.login.clear.inventory");
	TranslatableComponent.Builder MSG_HELLBLOCK_CLEARED_ENDERCHEST = Component.translatable()
			.key("message.hellblock.login.clear.enderchest");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVITE_REMINDER = Component.translatable()
			.key("message.hellblock.invitation.reminder");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_JOINED_SERVER = Component.translatable()
			.key("message.hellblock.coop.joined.server");
	TranslatableComponent.Builder MSG_HELLBLOCK_LOGIN_ABANDONED = Component.translatable()
			.key("message.hellblock.login.abandoned");
	TranslatableComponent.Builder MSG_HELLBLOCK_GROWING_GLOWSTONE_TREE = Component.translatable()
			.key("message.hellblock.grow.glowstone.tree");
	TranslatableComponent.Builder MSG_HELLBLOCK_LAVARAIN_WARNING = Component.translatable()
			.key("message.hellblock.lavarain.warning");
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
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_OWNER_FORCED_NOTIFICATION = Component.translatable()
			.key("message.hellblock.reset.forceful.owner.notification");
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
			.key("command.hellblock.admin.wrong_world");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_DELETE_CONFIRM = Component.translatable()
			.key("command.hellblock.admin.confirm_delete");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ISLAND_DELETED = Component.translatable()
			.key("command.hellblock.admin.island_deleted");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_PURGE_UNAVAILABLE = Component.translatable()
			.key("command.hellblock.admin.purge.unavailable");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_PURGE_SUCCESS = Component.translatable()
			.key("command.hellblock.admin.purge.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_PURGE_FAILURE = Component.translatable()
			.key("command.hellblock.admin.purge.failure");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_PURGE_CONFIRM = Component.translatable()
			.key("command.hellblock.admin.purge.confirm");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_FORCE_TELEPORT = Component.translatable()
			.key("command.hellblock.admin.force.teleport");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_FORCE_BIOME = Component.translatable()
			.key("command.hellblock.admin.force.biome");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_RESTORE_SUCCESS = Component.translatable()
			.key("command.hellblock.admin.restore.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_RESTORE_NOT_ABANDONED = Component.translatable()
			.key("command.hellblock.admin.restore.not_abandoned");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_TRANSFER_SUCCESS = Component.translatable()
			.key("command.hellblock.admin.transfer.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_TRANSFER_LOST = Component.translatable()
			.key("command.hellblock.admin.transfer.lost");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_TRANSFER_GAINED = Component.translatable()
			.key("command.hellblock.admin.transfer.gained");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_FIXOWNER_SUCCESS = Component.translatable()
			.key("command.hellblock.admin.fixowner.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_FIXOWNER_FAILURE = Component.translatable()
			.key("command.hellblock.admin.fixowner.failure");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_INSPECT_HEADER = Component.translatable()
			.key("command.hellblock.admin.inspect.header");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_INSPECT_ENTRY = Component.translatable()
			.key("command.hellblock.admin.inspect.entry");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ACTIVITY = Component.translatable()
			.key("command.hellblock.admin.activity");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_CLEANUP_CORRUPT = Component.translatable()
			.key("command.hellblock.admin.cleanup.corrupt");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_CLEANUP_ORPHAN = Component.translatable()
			.key("command.hellblock.admin.cleanup.orphan");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_CLEANUP_STARTED = Component.translatable()
			.key("command.hellblock.admin.cleanup.started");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_UNLOCKED = Component.translatable()
			.key("command.hellblock.admin.unlocked");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_LOCKED = Component.translatable()
			.key("command.hellblock.admin.locked");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_SETHOME = Component.translatable()
			.key("command.hellblock.admin.sethome");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_SET_LEVEL = Component.translatable()
			.key("command.hellblock.admin.setlevel");
	// Header for snapshot list
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ROLLBACK_LIST_HEADER = Component.translatable()
			.key("command.hellblock.admin.rollback.list.header");
	// List truncated footer
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ROLLBACK_LIST_MORE = Component.translatable()
			.key("command.hellblock.admin.rollback.list.more");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ROLLBACK_LIST_HOVER = Component.translatable()
			.key("command.hellblock.admin.rollback.list.hover");
	// Each snapshot entry
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ROLLBACK_LIST_ENTRY = Component.translatable()
			.key("command.hellblock.admin.rollback.list.entry");
	// No snapshots to list
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ROLLBACK_LIST_EMPTY = Component.translatable()
			.key("command.hellblock.admin.rollback.list.empty");
	// Rollback succeeded to latest snapshot
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ROLLBACK_LATEST = Component.translatable()
			.key("command.hellblock.admin.rollback.latest");
	// Rollback succeeded to minutes snapshot
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ROLLBACK_MINUTES = Component.translatable()
			.key("command.hellblock.admin.rollback.minutes");
	// Rollback succeeded to timestamp snapshot
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ROLLBACK_TIMESTAMP = Component.translatable()
			.key("command.hellblock.admin.rollback.timestamp");
	// No snapshots found
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ROLLBACK_NONE = Component.translatable()
			.key("command.hellblock.admin.rollback.none");
	// Invalid snapshot timestamp
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ROLLBACK_INVALID = Component.translatable()
			.key("command.hellblock.admin.rollback.invalid");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ABANDONED_LIST = Component.translatable()
			.key("command.hellblock.admin.abandoned.list");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_INVALID_PAGE = Component.translatable()
			.key("command.hellblock.admin.invalid.page");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_NO_ABANDONED_FOUND = Component.translatable()
			.key("command.hellblock.admin.no.abandoned.found");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ABANDONED = Component.translatable()
			.key("command.hellblock.admin.abandoned");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_RESET_COOLDOWN = Component.translatable()
			.key("command.hellblock.admin.reset.cooldowns");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_INFO = Component.translatable()
			.key("command.hellblock.admin.info");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_HOME_TO_BEDROCK = Component.translatable()
			.key("message.hellblock.home.set.to.bedrock");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NOT_TRUSTED = Component.translatable()
			.key("message.hellblock.coop.not_trusted");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ALREADY_TRUSTED = Component.translatable()
			.key("message.hellblock.coop.already_trusted");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_GIVEN = Component.translatable()
			.key("message.hellblock.coop.trust_given");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_GAINED = Component.translatable()
			.key("message.hellblock.coop.trust_gained");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_REVOKED = Component.translatable()
			.key("message.hellblock.coop.trust_revoked");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_LOST = Component.translatable()
			.key("message.hellblock.coop.trust_lost");
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
			.key("message.hellblock.coop.invite.none");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_EXPIRED = Component.translatable()
			.key("message.hellblock.coop.invite_expired");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITATION_HEADER = Component.translatable()
			.key("message.hellblock.coop.invite.header");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITATION_LIST = Component.translatable()
			.key("message.hellblock.coop.invite.entry");
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
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_MEMBER_LEFT_WHILE_OFFLINE = Component.translatable()
			.key("message.hellblock.coop.member.left.offline");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_KICKED_WHILE_OFFLINE = Component.translatable()
			.key("message.hellblock.coop.kicked.offline");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ADDED_WHILE_OFFLINE = Component.translatable()
			.key("message.hellblock.coop.added.offline");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_OFFLINE = Component.translatable()
			.key("message.hellblock.coop.invite.offline");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_OWNER_TRANSFER_NOTIFY_PARTY = Component.translatable()
			.key("message.hellblock.coop.owner.transfer.party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_OWNER_TRANSFERRED_WHILE_OFFLINE = Component.translatable()
			.key("message.hellblock.coop.owner.transfer.offline");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_DELETED_WHILE_OFFLINE = Component.translatable()
			.key("message.hellblock.coop.deleted.offline");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_RESET_WHILE_OFFLINE = Component.translatable()
			.key("message.hellblock.coop.reset.offline");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUSTED_WHILE_OFFLINE = Component.translatable()
			.key("message.hellblock.coop.trusted.offline");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_UNTRUSTED_WHILE_OFFLINE = Component.translatable()
			.key("message.hellblock.coop.untrusted.offline");
	TranslatableComponent.Builder MSG_HELLBLOCK_CHALLENGE_COMPLETED = Component.translatable()
			.key("message.hellblock.challenge.completed");
	TranslatableComponent.Builder MSG_HELLBLOCK_CHALLENGE_PROGRESS_BAR = Component.translatable()
			.key("message.hellblock.challenge.progress.bar");
	TranslatableComponent.Builder MSG_HELLBLOCK_SCHEMATIC_PROGRESS_BAR = Component.translatable()
			.key("message.hellblock.schematic.progress.bar");
	TranslatableComponent.Builder MSG_HELLBLOCK_WITHER_SPAWNED = Component.translatable()
			.key("message.hellblock.wither.spawned");
	TranslatableComponent.Builder MSG_HELLBLOCK_WITHER_DEFEATED = Component.translatable()
			.key("message.hellblock.wither.defeated");
	TranslatableComponent.Builder MSG_HELLBLOCK_WITHER_DESPAWNED = Component.translatable()
			.key("message.hellblock.wither.despawned");
	// Error & data states
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_NOT_FOUND = Component.translatable()
			.key("command.hellblock.top.not_found");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_INVALID_PAGE = Component.translatable()
			.key("command.hellblock.top.invalid.page");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_NOT_RANKED = Component.translatable()
			.key("command.hellblock.top.not_ranked");
	// Header & row format
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_HEADER = Component.translatable()
			.key("command.hellblock.top.header");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_FORMAT = Component.translatable()
			.key("command.hellblock.top.format");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_RANK_1 = Component.translatable()
			.key("command.hellblock.top.rank.1");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_RANK_2 = Component.translatable()
			.key("command.hellblock.top.rank.2");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_RANK_3 = Component.translatable()
			.key("command.hellblock.top.rank.3");
	// Self summary & "view your page"
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_SELF_SUMMARY = Component.translatable()
			.key("command.hellblock.top.self.summary");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_VIEW_YOUR_PAGE = Component.translatable()
			.key("command.hellblock.top.view.your.page");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_VIEW_YOUR_PAGE_HOVER = Component.translatable()
			.key("command.hellblock.top.view.your.page.hover");
	// Navigation (previous / next / separator)
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_PREVIOUS = Component.translatable()
			.key("command.hellblock.top.previous");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_PREVIOUS_HOVER = Component.translatable()
			.key("command.hellblock.top.previous.hover");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_NEXT = Component.translatable().key("command.hellblock.top.next");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_NEXT_HOVER = Component.translatable()
			.key("command.hellblock.top.next.hover");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_NAV_SEPARATOR = Component.translatable()
			.key("command.hellblock.top.nav.separator");
	// Page info & hover
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_PAGE_INFO = Component.translatable()
			.key("command.hellblock.top.page.info");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_PAGE_INFO_HOVER = Component.translatable()
			.key("command.hellblock.top.page.info.hover");
	// Wrappers around nav bar
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_NAV_WRAPPER_START = Component.translatable()
			.key("command.hellblock.top.nav.wrapper.start");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_NAV_WRAPPER_END = Component.translatable()
			.key("command.hellblock.top.nav.wrapper.end");

	TranslatableComponent.Builder FORMAT_OPEN = Component.translatable().key("format.open");
	TranslatableComponent.Builder FORMAT_CLOSED = Component.translatable().key("format.closed");
	TranslatableComponent.Builder FORMAT_LOCKED = Component.translatable().key("format.locked");
	TranslatableComponent.Builder FORMAT_UNLOCKED = Component.translatable().key("format.unlocked");
	TranslatableComponent.Builder FORMAT_YES = Component.translatable().key("format.yes");
	TranslatableComponent.Builder FORMAT_NO = Component.translatable().key("format.no");
	TranslatableComponent.Builder FORMAT_NEVER = Component.translatable().key("format.never");
	TranslatableComponent.Builder FORMAT_NONE = Component.translatable().key("format.none");
	TranslatableComponent.Builder FORMAT_UNRANKED = Component.translatable().key("format.unranked");
	TranslatableComponent.Builder FORMAT_UNKNOWN = Component.translatable().key("format.unknown");
	TranslatableComponent.Builder FORMAT_MAXED = Component.translatable().key("format.maxed");

	TranslatableComponent.Builder FORMAT_SECOND = Component.translatable().key("format.second");
	TranslatableComponent.Builder FORMAT_MINUTE = Component.translatable().key("format.minute");
	TranslatableComponent.Builder FORMAT_HOUR = Component.translatable().key("format.hour");
	TranslatableComponent.Builder FORMAT_DAY = Component.translatable().key("format.day");

	TranslatableComponent.Builder COMMAND_RELOAD_SUCCESS = Component.translatable().key("command.reload.success");
	TranslatableComponent.Builder COMMAND_RELOADING = Component.translatable().key("command.reloading");
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
	TranslatableComponent.Builder COMMAND_DEBUG_WORLDS_FAILURE = Component.translatable()
			.key("command.debug.worlds.failure");
	TranslatableComponent.Builder COMMAND_DEBUG_WORLDS_SUCCESS = Component.translatable()
			.key("command.debug.worlds.success");
}