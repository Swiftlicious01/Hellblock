package com.swiftlicious.hellblock.config.locale;

import com.swiftlicious.hellblock.listeners.weather.WeatherType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public interface MessageConstants {

	// Island entry and farewell messages
	TranslatableComponent.Builder HELLBLOCK_ENTRY_MESSAGE = Component.translatable().key("island.entry.message");
	TranslatableComponent.Builder HELLBLOCK_FAREWELL_MESSAGE = Component.translatable().key("island.farewell.message");
	TranslatableComponent.Builder HELLBLOCK_ABANDONED_ENTRY_MESSAGE = Component.translatable()
			.key("island.entry.abandoned.message");
	TranslatableComponent.Builder HELLBLOCK_ABANDONED_FAREWELL_MESSAGE = Component.translatable()
			.key("island.farewell.abandoned.message");
	TranslatableComponent.Builder MSG_HELLBLOCK_NO_LEAVING_BORDER = Component.translatable()
			.key("message.hellblock.no.exit.border");

	// Generic command argument messages
	TranslatableComponent.Builder COMMAND_INVALID_PAGE_ARGUMENT = Component.translatable()
			.key("command.invalid.page.argument");

	// Display messages
	TranslatableComponent.Builder COMMAND_HELLBLOCK_DISPLAY_TOGGLED = Component.translatable()
			.key("command.hellblock.display.toggled");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_DISPLAY_UNCHANGED = Component.translatable()
			.key("command.hellblock.display.unchanged");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_DISPLAY_SET = Component.translatable()
			.key("command.hellblock.display.set");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_DISPLAY_INVALID = Component.translatable()
			.key("command.hellblock.display.invalid");

	// Name messages
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_EMPTY = Component.translatable()
			.key("command.hellblock.name.empty");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_SHOW = Component.translatable()
			.key("command.hellblock.name.show");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_TOO_LONG = Component.translatable()
			.key("command.hellblock.name.too.long");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_BANNED_WORDS = Component.translatable()
			.key("command.hellblock.name.banned.words");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_UNCHANGED = Component.translatable()
			.key("command.hellblock.name.unchanged");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_RESET_DEFAULT = Component.translatable()
			.key("command.hellblock.name.reset.default");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_NAME_SET_SUCCESS = Component.translatable()
			.key("command.hellblock.name.set.success");

	// Bio messages
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_TOO_LONG = Component.translatable()
			.key("command.hellblock.bio.too.long");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_BANNED_WORDS = Component.translatable()
			.key("command.hellblock.bio.banned.words");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_RESET_DEFAULT = Component.translatable()
			.key("command.hellblock.bio.reset.default");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_TOO_MANY_LINES = Component.translatable()
			.key("command.hellblock.bio.too.many.lines");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_UNCHANGED = Component.translatable()
			.key("command.hellblock.bio.unchanged");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_EMPTY = Component.translatable()
			.key("command.hellblock.bio.empty");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_SHOW = Component.translatable()
			.key("command.hellblock.bio.show");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_BIO_SET_SUCCESS = Component.translatable()
			.key("command.hellblock.bio.set.success");

	// Help messages
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_HELP = Component.translatable()
			.key("command.hellblock.help.coop.list");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_HELP = Component.translatable()
			.key("command.hellblock.help.admin.list");
	TranslatableComponent.Builder MSG_HELLBLOCK_USER_HELP = Component.translatable()
			.key("command.hellblock.help.user.list");

	// Help headers
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_HELP_HEADER = Component.translatable()
			.key("command.hellblock.help.coop.header");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_HELP_HEADER = Component.translatable()
			.key("command.hellblock.help.admin.header");
	TranslatableComponent.Builder MSG_HELLBLOCK_USER_HELP_HEADER = Component.translatable()
			.key("command.hellblock.help.user.header");

	// Cooldown messages
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_ON_COOLDOWN = Component.translatable()
			.key("command.hellblock.reset.on.cooldown");
	TranslatableComponent.Builder MSG_HELLBLOCK_BIOME_ON_COOLDOWN = Component.translatable()
			.key("command.hellblock.biome.on.cooldown");
	TranslatableComponent.Builder MSG_HELLBLOCK_TRANSFER_ON_COOLDOWN = Component.translatable()
			.key("command.hellblock.transfer.on.cooldown");

	// Biome change messages
	TranslatableComponent.Builder MSG_HELLBLOCK_BIOME_SAME_BIOME = Component.translatable()
			.key("command.hellblock.biome.same.choice");
	TranslatableComponent.Builder MSG_HELLBLOCK_BIOME_CHANGED = Component.translatable()
			.key("command.hellblock.biome.changed");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVALID_BIOME = Component.translatable()
			.key("command.hellblock.biome.invalid");
	TranslatableComponent.Builder MSG_HELLBLOCK_MUST_BE_ON_ISLAND = Component.translatable()
			.key("command.hellblock.must.be.on.island");

	// Lock/unlock messages
	TranslatableComponent.Builder MSG_HELLBLOCK_LOCK_SUCCESS = Component.translatable()
			.key("command.hellblock.locked.island");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNLOCK_SUCCESS = Component.translatable()
			.key("command.hellblock.unlocked.island");

	// Ban/unban messages
	TranslatableComponent.Builder MSG_HELLBLOCK_NOT_BANNED = Component.translatable()
			.key("command.hellblock.not.banned");
	TranslatableComponent.Builder MSG_HELLBLOCK_ALREADY_BANNED = Component.translatable()
			.key("command.hellblock.already.banned");
	TranslatableComponent.Builder MSG_HELLBLOCK_BANNED_PLAYER = Component.translatable()
			.key("command.hellblock.banned.from.island");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNBANNED_PLAYER = Component.translatable()
			.key("command.hellblock.unbanned.from.island");

	// Reset messages
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_IN_PROCESS = Component.translatable()
			.key("command.hellblock.reset.in.process");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_NOT_IN_PROCESS = Component.translatable()
			.key("command.hellblock.reset.not.in.process");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_CONFIRMATION = Component.translatable()
			.key("command.hellblock.reset.confirmation");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_CONFIRMATION_BUTTON = Component.translatable()
			.key("command.hellblock.reset.confirmation.button");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_CONFIRMATION_HOVER = Component.translatable()
			.key("command.hellblock.reset.confirmation.button.hover");

	// Home messages
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_TELEPORT = Component.translatable()
			.key("command.hellblock.home.teleport");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_NOT_BROKEN = Component.translatable()
			.key("command.hellblock.home.not.broken");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_NEW_LOCATION = Component.translatable()
			.key("command.hellblock.home.new.location");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_SAME_LOCATION = Component.translatable()
			.key("command.hellblock.home.same.location");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_UNSAFE_STANDING_LOCATION = Component.translatable()
			.key("command.hellblock.home.standing.unsafe");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOME_ERROR_LOCATION = Component.translatable()
			.key("command.hellblock.home.standing.change.error");

	// Warp messages
	TranslatableComponent.Builder MSG_HELLBLOCK_WARP_NEW_LOCATION = Component.translatable()
			.key("command.hellblock.warp.new.location");
	TranslatableComponent.Builder MSG_HELLBLOCK_WARP_SAME_LOCATION = Component.translatable()
			.key("command.hellblock.warp.same.location");
	TranslatableComponent.Builder MSG_HELLBLOCK_WARP_UNSAFE_STANDING_LOCATION = Component.translatable()
			.key("command.hellblock.warp.standing.unsafe");

	// Creation messages
	TranslatableComponent.Builder MSG_HELLBLOCK_CREATION_FAILURE_ALREADY_EXISTS = Component.translatable()
			.key("command.hellblock.creation.failure.already.exists");

	// Information messages
	TranslatableComponent.Builder MSG_HELLBLOCK_INFORMATION = Component.translatable()
			.key("command.hellblock.information.format");
	TranslatableComponent.Builder MSG_HELLBLOCK_INFO_LIST_STYLE = Component.translatable()
			.key("command.hellblock.information.list.style");
	TranslatableComponent.Builder MSG_HELLBLOCK_INFO_LIST_SEPARATOR = Component.translatable()
			.key("command.hellblock.information.list.separator");
	TranslatableComponent.Builder MSG_HELLBLOCK_INFO_LIST_CUTOFF = Component.translatable()
			.key("command.hellblock.information.list.cutoff");

	// Visit messages
	TranslatableComponent.Builder MSG_HELLBLOCK_VISIT_ABANDONED = Component.translatable()
			.key("message.hellblock.visit.abandoned");
	TranslatableComponent.Builder MSG_HELLBLOCK_VISIT_ENTRY = Component.translatable()
			.key("message.hellblock.visit.entry");
	TranslatableComponent.Builder MSG_HELLBLOCK_LOCKED_FROM_VISITORS = Component.translatable()
			.key("message.hellblock.locked.from.visitors");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNSAFE_TO_VISIT = Component.translatable()
			.key("message.hellblock.unsafe.to.visit");

	// Open hellblock messages
	TranslatableComponent.Builder MSG_HELLBLOCK_OPEN_SUCCESS = Component.translatable()
			.key("command.hellblock.open.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_OPEN_FAILURE_NOT_LOADED = Component.translatable()
			.key("command.hellblock.open.failure.not.loaded");

	// Visitor list messages
	TranslatableComponent.Builder MSG_HELLBLOCK_NO_VISITORS = Component.translatable()
			.key("command.hellblock.visitors.none");
	TranslatableComponent.Builder MSG_HELLBLOCK_VISITOR_LIST = Component.translatable()
			.key("command.hellblock.visitors.list");

	// Visit log messages
	TranslatableComponent.Builder MSG_HELLBLOCK_VISIT_LOG_HEADER = Component.translatable()
			.key("message.hellblock.visit.log.header");
	TranslatableComponent.Builder MSG_HELLBLOCK_VISIT_LOG_ENTRY = Component.translatable()
			.key("message.hellblock.visit.log.entry");
	TranslatableComponent.Builder MSG_HELLBLOCK_VISIT_LOG_EMPTY = Component.translatable()
			.key("message.hellblock.visit.log.empty");

	// Visit GUI messages
	TranslatableComponent.Builder COMMAND_INVALID_SORT_TYPE = Component.translatable()
			.key("command.hellblock.visit.menu.invalid.sort");
	TranslatableComponent.Builder COMMAND_VISIT_GUI_FAILED = Component.translatable()
			.key("command.hellblock.visit.menu.failed");

	// Ban/trust GUI messages
	TranslatableComponent.Builder MSG_HELLBLOCK_NO_BANNED_FOUND = Component.translatable()
			.key("message.hellblock.no.banned.found");
	TranslatableComponent.Builder MSG_HELLBLOCK_NO_TRUSTED_FOUND = Component.translatable()
			.key("message.hellblock.no.trusted.found");

	// Upgrade notifications
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
	// Upgrade notifications for party members
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
	// Upgrade failure/success messages
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_MAX_TIER = Component.translatable()
			.key("command.hellblock.upgrade.max.tier");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVALID_UPGRADE = Component.translatable()
			.key("command.hellblock.upgrade.invalid");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_NO_COST = Component.translatable()
			.key("command.hellblock.upgrade.no.cost");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_CANNOT_AFFORD = Component.translatable()
			.key("command.hellblock.upgrade.cannot.afford");
	TranslatableComponent.Builder MSG_HELLBLOCK_UPGRADE_SUCCESS = Component.translatable()
			.key("command.hellblock.upgrade.success");

	// Hopper limiting
	TranslatableComponent.Builder MSG_HELLBLOCK_HOPPER_INFO = Component.translatable()
			.key("message.hellblock.hopper.info");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOPPER_LIMIT_REACHED = Component.translatable()
			.key("message.hellblock.hopper.limit.reached");
	TranslatableComponent.Builder MSG_HELLBLOCK_HOPPER_EXCESS_WARNING = Component.translatable()
			.key("message.hellblock.hopper.excess.warning");

	// Featured slots
	TranslatableComponent.Builder MSG_FEATURED_EXISTS = Component.translatable()
			.key("message.hellblock.featured.exists");
	TranslatableComponent.Builder MSG_FEATURED_FULL = Component.translatable().key("message.hellblock.featured.full");
	TranslatableComponent.Builder MSG_FEATURED_FAILED_PURCHASE = Component.translatable()
			.key("message.hellblock.featured.fail");
	TranslatableComponent.Builder MSG_FEATURED_SUCCESS = Component.translatable()
			.key("message.hellblock.featured.success");

	// Flag messages
	TranslatableComponent.Builder MSG_HELLBLOCK_INVALID_FLAG = Component.translatable()
			.key("command.hellblock.flag.invalid");
	TranslatableComponent.Builder MSG_HELLBLOCK_CHANGED_FLAG = Component.translatable()
			.key("command.hellblock.flag.changed");

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
	// Item drop
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_ITEM_DROP_DENY = Component.translatable()
			.key("message.hellblock.protection.item.drop.deny");
	// Item pickup
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_ITEM_PICKUP_DENY = Component.translatable()
			.key("message.hellblock.protection.item.pickup.deny");
	TranslatableComponent.Builder MSG_ITEM_RECENTLY_DROPPED_PICKUP_DENY = Component.translatable()
			.key("message.hellblock.protection.item.pickup.recently.dropped.deny");
	// Exp drops
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_EXP_DROP_DENY = Component.translatable()
			.key("message.hellblock.protection.exp.drop.deny");
	// Painting destroy
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_PAINTING_DESTROY_DENY = Component.translatable()
			.key("message.hellblock.protection.painting.destroy.deny");
	// Item frame destroy
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_ITEM_FRAME_DESTROY_DENY = Component.translatable()
			.key("message.hellblock.protection.itemframe.destroy.deny");
	// Wind charge burst
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_WIND_CHARGE_DENY = Component.translatable()
			.key("message.hellblock.protection.wind.charge.deny");
	// Potion splash
	TranslatableComponent.Builder MSG_HELLBLOCK_PROTECTION_POTION_SPLASH_DENY = Component.translatable()
			.key("message.hellblock.protection.potion.splash.deny");
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

	// Login messages
	TranslatableComponent.Builder MSG_HELLBLOCK_UNSAFE_ENVIRONMENT = Component.translatable()
			.key("message.hellblock.login.unsafe.environment");
	TranslatableComponent.Builder MSG_HELLBLOCK_CLEARED_INVENTORY = Component.translatable()
			.key("message.hellblock.login.clear.inventory");
	TranslatableComponent.Builder MSG_HELLBLOCK_CLEARED_ENDERCHEST = Component.translatable()
			.key("message.hellblock.login.clear.enderchest");
	TranslatableComponent.Builder MSG_HELLBLOCK_LOGIN_ABANDONED = Component.translatable()
			.key("message.hellblock.login.abandoned");

	// Invitation messages
	TranslatableComponent.Builder MSG_HELLBLOCK_INVITE_REMINDER = Component.translatable()
			.key("message.hellblock.invitation.reminder");
	TranslatableComponent.Builder BTN_HELLBLOCK_INVITE_REMINDER_HERE = Component.translatable()
			.key("message.hellblock.invitation.reminder.button");
	TranslatableComponent.Builder BTN_HELLBLOCK_INVITE_REMINDER_HOVER = Component.translatable()
			.key("message.hellblock.invitation.reminder.button.hover");

	// Join messages
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_JOINED_SERVER = Component.translatable()
			.key("message.hellblock.coop.joined.server");

	// Glowstone tree messages
	TranslatableComponent.Builder MSG_HELLBLOCK_GROWING_GLOWSTONE_TREE = Component.translatable()
			.key("message.hellblock.grow.glowstone.tree");

	// Weather warnings
	TranslatableComponent.Builder MSG_HELLBLOCK_LAVARAIN_WARNING = Component.translatable()
			.key("message.hellblock.lavarain.warning");
	TranslatableComponent.Builder MSG_HELLBLOCK_ASHSTORM_WARNING = Component.translatable()
			.key("message.hellblock.ashstorm.warning");
	TranslatableComponent.Builder MSG_HELLBLOCK_EMBERFOG_WARNING = Component.translatable()
			.key("message.hellblock.emberfog.warning");
	TranslatableComponent.Builder MSG_HELLBLOCK_MAGMAWIND_WARNING = Component.translatable()
			.key("message.hellblock.magmawind.warning");

	// Island entry denial messages
	TranslatableComponent.Builder MSG_HELLBLOCK_BANNED_ENTRY = Component.translatable()
			.key("message.hellblock.banned.from.entry");
	TranslatableComponent.Builder MSG_HELLBLOCK_LOCKED_ENTRY = Component.translatable()
			.key("message.hellblock.locked.no.entry");

	// Island creation messages
	TranslatableComponent.Builder MSG_HELLBLOCK_CREATE_PROCESS = Component.translatable()
			.key("message.hellblock.create.process");

	// Island reset messages
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

	// Unsafe teleportation messages
	TranslatableComponent.Builder MSG_HELLBLOCK_UNSAFE_CONDITIONS = Component.translatable()
			.key("message.hellblock.spawn.unsafe.conditions");
	TranslatableComponent.Builder MSG_HELLBLOCK_ERROR_HOME_LOCATION = Component.translatable()
			.key("message.hellblock.error.teleport.to.home");
	TranslatableComponent.Builder MSG_HELLBLOCK_UNSAFE_TELEPORT_TO_ISLAND = Component.translatable()
			.key("message.hellblock.unsafe.teleport.to.island");
	TranslatableComponent.Builder MSG_HELLBLOCK_RESET_HOME_TO_BEDROCK = Component.translatable()
			.key("message.hellblock.home.set.to.bedrock");

	// Island linking messages
	TranslatableComponent.Builder MSG_HELLBLOCK_UNSAFE_LINKING = Component.translatable()
			.key("message.hellblock.link.unsafe");
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

	// Player target messages
	TranslatableComponent.Builder MSG_HELLBLOCK_PLAYER_OFFLINE = Component.translatable()
			.key("command.hellblock.player.offline");
	TranslatableComponent.Builder MSG_HELLBLOCK_NO_ISLAND_FOUND = Component.translatable()
			.key("command.hellblock.player.no.island.found");
	TranslatableComponent.Builder MSG_HELLBLOCK_NOT_TO_SELF = Component.translatable()
			.key("command.hellblock.player.not.to.self");
	TranslatableComponent.Builder MSG_HELLBLOCK_NOT_TO_PARTY = Component.translatable()
			.key("command.hellblock.not.to.party");
	TranslatableComponent.Builder MSG_HELLBLOCK_NOT_FOUND = Component.translatable().key("command.hellblock.not.found");
	TranslatableComponent.Builder MSG_NOT_OWNER_OF_HELLBLOCK = Component.translatable()
			.key("command.hellblock.not.owner");
	TranslatableComponent.Builder MSG_HELLBLOCK_IS_ABANDONED = Component.translatable()
			.key("command.hellblock.is.abandoned");
	TranslatableComponent.Builder MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD = Component.translatable()
			.key("command.hellblock.player.data.failure.load");
	TranslatableComponent.Builder MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED = Component.translatable()
			.key("command.hellblock.owner.data.not.loaded");

	// Admin delete messages
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_DELETE_CONFIRM = Component.translatable()
			.key("command.hellblock.admin.delete.confirm");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ISLAND_DELETED = Component.translatable()
			.key("command.hellblock.admin.delete.island");

	// Purge messages
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_PURGE_UNAVAILABLE = Component.translatable()
			.key("command.hellblock.admin.purge.unavailable");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_PURGE_SUCCESS = Component.translatable()
			.key("command.hellblock.admin.purge.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_PURGE_FAILURE = Component.translatable()
			.key("command.hellblock.admin.purge.failure");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_PURGE_CONFIRM = Component.translatable()
			.key("command.hellblock.admin.purge.confirm");

	// Force messages
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_FORCE_TELEPORT = Component.translatable()
			.key("command.hellblock.admin.force.teleport");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_FORCE_BIOME = Component.translatable()
			.key("command.hellblock.admin.force.biome");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_FORCE_FLAG = Component.translatable()
			.key("command.hellblock.admin.force.flag");

	// Restore messages
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_RESTORE_SUCCESS = Component.translatable()
			.key("command.hellblock.admin.restore.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_RESTORE_NOT_ABANDONED = Component.translatable()
			.key("command.hellblock.admin.restore.not.abandoned");

	// Transfer messages
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_TRANSFER_SUCCESS = Component.translatable()
			.key("command.hellblock.admin.transfer.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_TRANSFER_LOST = Component.translatable()
			.key("command.hellblock.admin.transfer.lost");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_TRANSFER_GAINED = Component.translatable()
			.key("command.hellblock.admin.transfer.gained");

	// Fix owner messages
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_FIXOWNER_SUCCESS = Component.translatable()
			.key("command.hellblock.admin.fixowner.success");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_FIXOWNER_FAILURE = Component.translatable()
			.key("command.hellblock.admin.fixowner.failure");

	// Activity log message
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ACTIVITY_ONLINE = Component.translatable()
			.key("command.hellblock.admin.activity.online");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ACTIVITY_LAST_SEEN = Component.translatable()
			.key("command.hellblock.admin.activity.last.seen");

	// Cleanup messages
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_CLEANUP_CORRUPT = Component.translatable()
			.key("command.hellblock.admin.cleanup.corrupt");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_CLEANUP_ORPHAN = Component.translatable()
			.key("command.hellblock.admin.cleanup.orphan");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_CLEANUP_COMPLETED = Component.translatable()
			.key("command.hellblock.admin.cleanup.completed");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_CLEANUP_NONE_FOUND = Component.translatable()
			.key("command.hellblock.admin.cleanup.none.found");

	// Lock/unlock messages
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_UNLOCKED = Component.translatable()
			.key("command.hellblock.admin.unlocked");
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_LOCKED = Component.translatable()
			.key("command.hellblock.admin.locked");

	// Set home message
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_SETHOME = Component.translatable()
			.key("command.hellblock.admin.home.set");

	// Set island level message
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_SET_LEVEL = Component.translatable()
			.key("command.hellblock.admin.level.set");
	// Recalculate island level message
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_RECALCULATED_LEVEL = Component.translatable()
			.key("command.hellblock.admin.level.recalculated");

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

	// Abandoned island listing
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ABANDONED_LIST = Component.translatable()
			.key("command.hellblock.admin.abandoned.list");
	// No abandoned islands found
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_NO_ABANDONED_FOUND = Component.translatable()
			.key("command.hellblock.admin.abandoned.none.found");
	// Abandoned island cleanup confirmation
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_ABANDONED = Component.translatable()
			.key("command.hellblock.admin.abandoned.marked");
	// Cooldown resets
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_RESET_COOLDOWN = Component.translatable()
			.key("command.hellblock.admin.reset.cooldowns");
	// Admin info
	TranslatableComponent.Builder MSG_HELLBLOCK_ADMIN_INFO = Component.translatable()
			.key("command.hellblock.admin.info");

	// Trust management
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NOT_TRUSTED = Component.translatable()
			.key("command.hellblock.coop.not.trusted");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ALREADY_TRUSTED = Component.translatable()
			.key("command.hellblock.coop.already.trusted");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_GIVEN = Component.translatable()
			.key("command.hellblock.coop.trust.given");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_GAINED = Component.translatable()
			.key("command.hellblock.coop.trust.gained");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_REVOKED = Component.translatable()
			.key("command.hellblock.coop.trust.revoked");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRUST_LOST = Component.translatable()
			.key("command.hellblock.coop.trust.lost");

	// Party management
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TUTORIAL = Component.translatable()
			.key("command.hellblock.coop.tutorial");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS = Component.translatable()
			.key("command.hellblock.coop.hellblock.exists");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ALREADY_HAS_HELLBLOCK = Component.translatable()
			.key("command.hellblock.coop.already.has.hellblock");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ALREADY_IN_PARTY = Component.translatable()
			.key("command.hellblock.coop.already.in.party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY = Component.translatable()
			.key("command.hellblock.coop.not.found.in.party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_BANNED_FROM_INVITE = Component.translatable()
			.key("command.hellblock.coop.banned.from.island");

	// Toggling party settings
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

	// Sending invites
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NO_INVITE_SELF = Component.translatable()
			.key("command.hellblock.coop.no.invite.self");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_CANCELLED = Component.translatable()
			.key("command.hellblock.coop.invite.cancelled");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_REVOKED = Component.translatable()
			.key("command.hellblock.coop.invite.revoked");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_EXISTS = Component.translatable()
			.key("command.hellblock.coop.already.has.invite");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_SENT = Component.translatable()
			.key("command.hellblock.coop.invite.sent");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_RECEIVED = Component.translatable()
			.key("command.hellblock.coop.invite.received.prefix");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_EXPIRED = Component.translatable()
			.key("command.hellblock.coop.invite.expired");

	// Invite buttons
	TranslatableComponent.Builder BTN_HELLBLOCK_COOP_INVITE_ACCEPT = Component.translatable()
			.key("command.hellblock.coop.button.accept");
	TranslatableComponent.Builder BTN_HELLBLOCK_COOP_INVITE_DECLINE = Component.translatable()
			.key("command.hellblock.coop.button.decline");
	TranslatableComponent.Builder BTN_HELLBLOCK_COOP_INVITE_ACCEPT_HOVER = Component.translatable()
			.key("command.hellblock.coop.button.accept.hover");
	TranslatableComponent.Builder BTN_HELLBLOCK_COOP_INVITE_DECLINE_HOVER = Component.translatable()
			.key("command.hellblock.coop.button.decline.hover");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_EXPIRES = Component.translatable()
			.key("command.hellblock.coop.invite.expires.suffix");

	// Viewing invites
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NO_INVITE_FOUND = Component.translatable()
			.key("command.hellblock.coop.no.invite.found");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NO_INVITES = Component.translatable()
			.key("command.hellblock.coop.invite.none");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITATION_HEADER = Component.translatable()
			.key("command.hellblock.coop.invite.header");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_ENTRY = Component.translatable()
			.key("command.hellblock.coop.invite.entry.prefix");

	// Joining party
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_ABANDONED = Component.translatable()
			.key("command.hellblock.coop.abandoned.invite");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ALREADY_JOINED_PARTY = Component.translatable()
			.key("command.hellblock.coop.already.joined.party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NOT_IN_PARTY = Component.translatable()
			.key("command.hellblock.coop.not.in.party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ADDED_TO_PARTY = Component.translatable()
			.key("command.hellblock.coop.added.to.party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_JOINED_PARTY = Component.translatable()
			.key("command.hellblock.coop.joined.party");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_PARTY_FULL = Component.translatable()
			.key("command.hellblock.coop.party.full");

	// Rejecting invite
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_REJECTED_TO_OWNER = Component.translatable()
			.key("command.hellblock.coop.invite.rejected.to.owner");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_INVITE_REJECTED = Component.translatable()
			.key("command.hellblock.coop.invite.rejected");

	// Leaving party
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_OWNER_NO_LEAVE = Component.translatable()
			.key("command.hellblock.coop.owner.no.leave");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_PARTY_LEFT = Component.translatable()
			.key("command.hellblock.coop.party.left");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_LEFT_PARTY = Component.translatable()
			.key("command.hellblock.coop.left.party");

	// Kicking members
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NO_KICK_SELF = Component.translatable()
			.key("command.hellblock.coop.no.kick.self");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_PARTY_KICKED = Component.translatable()
			.key("command.hellblock.coop.party.kicked");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_REMOVED_FROM_PARTY = Component.translatable()
			.key("command.hellblock.coop.removed.from.party");

	// Transfer ownership
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_TRANSFER_OWNERSHIP_DISABLED = Component.translatable()
			.key("command.hellblock.coop.transfer.ownership.disabled");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NO_TRANSFER_SELF = Component.translatable()
			.key("command.hellblock.coop.no.transfer.self");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_ALREADY_OWNER_OF_ISLAND = Component.translatable()
			.key("command.hellblock.coop.already.own.island");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_NEW_OWNER_SET = Component.translatable()
			.key("command.hellblock.coop.new.owner.set");
	TranslatableComponent.Builder MSG_HELLBLOCK_COOP_OWNER_TRANSFER_SUCCESS = Component.translatable()
			.key("command.hellblock.coop.owner.transfer.success");

	// Offline notifications
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

	// Chat settings
	TranslatableComponent.Builder COMMAND_HELLBLOCK_CHAT_TOGGLED = Component.translatable()
			.key("command.hellblock.chat.toggled");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_CHAT_UNCHANGED = Component.translatable()
			.key("command.hellblock.chat.unchanged");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_CHAT_NOT_IN_COOP = Component.translatable()
			.key("command.hellblock.chat.not.in.coop");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_CHAT_SET = Component.translatable()
			.key("command.hellblock.chat.set");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_CHAT_INVALID = Component.translatable()
			.key("command.hellblock.chat.invalid");

	// Chat listening modes
	TranslatableComponent.Builder COMMAND_HELLBLOCK_CHAT_LISTEN_GLOBAL = Component.translatable()
			.key("command.hellblock.chat.listening.global");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_CHAT_LISTEN_LOCAL = Component.translatable()
			.key("command.hellblock.chat.listening.local");
	TranslatableComponent.Builder COMMAND_HELLBLOCK_CHAT_LISTEN_COOP = Component.translatable()
			.key("command.hellblock.chat.listening.coop");

	// Challenge messages
	TranslatableComponent.Builder MSG_HELLBLOCK_CHALLENGE_COMPLETED = Component.translatable()
			.key("message.hellblock.challenge.completed");

	// Challenge & schematic progress bars
	TranslatableComponent.Builder MSG_HELLBLOCK_CHALLENGE_PROGRESS_BAR = Component.translatable()
			.key("message.hellblock.progressbar.challenge");
	TranslatableComponent.Builder MSG_HELLBLOCK_SCHEMATIC_PROGRESS_BAR = Component.translatable()
			.key("message.hellblock.progressbar.schematic");

	// Wither event messages
	TranslatableComponent.Builder MSG_HELLBLOCK_WITHER_SPAWNED = Component.translatable()
			.key("message.hellblock.wither.spawned");
	TranslatableComponent.Builder MSG_HELLBLOCK_WITHER_DEFEATED = Component.translatable()
			.key("message.hellblock.wither.defeated");
	TranslatableComponent.Builder MSG_HELLBLOCK_WITHER_DESPAWNED = Component.translatable()
			.key("message.hellblock.wither.despawned");
	TranslatableComponent.Builder MSG_HELLBLOCK_WITHER_NAME = Component.translatable()
			.key("message.hellblock.wither.name");

	// Sky Siege event messages
	TranslatableComponent.Builder MSG_HELLBLOCK_SKYSIEGE_STARTED = Component.translatable()
			.key("message.hellblock.skysiege.start");
	TranslatableComponent.Builder MSG_HELLBLOCK_SKYSIEGE_WAVE = Component.translatable()
			.key("message.hellblock.skysiege.wave");
	TranslatableComponent.Builder MSG_HELLBLOCK_SKYSIEGE_BOSS_SPAWNED = Component.translatable()
			.key("message.hellblock.skysiege.boss.spawned");
	TranslatableComponent.Builder MSG_HELLBLOCK_SKYSIEGE_BOSS_BAR = Component.translatable()
			.key("message.hellblock.skysiege.bossbar.remaining");
	TranslatableComponent.Builder MSG_HELLBLOCK_SKYSIEGE_BOSS_HEALTH_BAR = Component.translatable()
			.key("message.hellblock.skysiege.bossbar.health");
	TranslatableComponent.Builder MSG_HELLBLOCK_SKYSIEGE_BOSS_NAME = Component.translatable()
			.key("message.hellblock.skysiege.boss.name");
	TranslatableComponent.Builder MSG_HELLBLOCK_SKYSIEGE_VICTORY = Component.translatable()
			.key("message.hellblock.skysiege.victory");
	TranslatableComponent.Builder MSG_HELLBLOCK_SKYSIEGE_FAILURE = Component.translatable()
			.key("message.hellblock.skysiege.failure");
	TranslatableComponent.Builder MSG_HELLBLOCK_SKYSIEGE_SUMMARY = Component.translatable()
			.key("message.hellblock.skysiege.end.summary");

	// Hellblock Invasion event messages
	TranslatableComponent.Builder MSG_HELLBLOCK_INVASION_START = Component.translatable()
			.key("message.hellblock.invasion.start");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVASION_WAVE = Component.translatable()
			.key("message.hellblock.invasion.wave");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVASION_SUMMARY = Component.translatable()
			.key("message.hellblock.invasion.end.summary");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVASION_BOSS_BAR = Component.translatable()
			.key("message.hellblock.invasion.bossbar.remaining");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVASION_BOSS_HEALTH_BAR = Component.translatable()
			.key("message.hellblock.invasion.bossbar.health");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVASION_BANNER_ITEM_NAME = Component.translatable()
			.key("message.hellblock.invasion.banner.item.name");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVASION_BOSS_NAME = Component.translatable()
			.key("message.hellblock.invasion.boss.name");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVASION_SHAMAN_NAME = Component.translatable()
			.key("message.hellblock.invasion.shaman.name");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVASION_CORRUPTED_NAME = Component.translatable()
			.key("message.hellblock.invasion.corrupted.name");
	TranslatableComponent.Builder MSG_HELLBLOCK_INVASION_BERSERKER_NAME = Component.translatable()
			.key("message.hellblock.invasion.berserker.name");

	// Errors related to hellblock world & bedrock
	TranslatableComponent.Builder MSG_HELLBLOCK_WORLD_ERROR = Component.translatable()
			.key("command.hellblock.error.world");
	TranslatableComponent.Builder MSG_HELLBLOCK_BEDROCK_ERROR = Component.translatable()
			.key("command.hellblock.error.bedrock");

	// Error & data states
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_NOT_FOUND = Component.translatable()
			.key("command.hellblock.top.not.found");
	TranslatableComponent.Builder MSG_HELLBLOCK_TOP_NOT_RANKED = Component.translatable()
			.key("command.hellblock.top.not.ranked");
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

	// Auto-generated name & bio defaults
	TranslatableComponent.Builder MSG_HELLBLOCK_BIO_DEFAULT = Component.translatable()
			.key("message.hellblock.bio.auto.default");
	TranslatableComponent.Builder MSG_HELLBLOCK_NAME_DEFAULT = Component.translatable()
			.key("message.hellblock.name.auto.default");

	// Schematic permission denied
	TranslatableComponent.Builder MSG_HELLBLOCK_NO_SCHEMATIC_PERMISSION = Component.translatable()
			.key("message.hellblock.permission.schematic.denied");

	// General formats
	TranslatableComponent.Builder FORMAT_OPEN = Component.translatable().key("format.open");
	TranslatableComponent.Builder FORMAT_CLOSED = Component.translatable().key("format.closed");
	TranslatableComponent.Builder FORMAT_LOCKED = Component.translatable().key("format.locked");
	TranslatableComponent.Builder FORMAT_UNLOCKED = Component.translatable().key("format.unlocked");
	TranslatableComponent.Builder FORMAT_YES = Component.translatable().key("format.yes");
	TranslatableComponent.Builder FORMAT_NO = Component.translatable().key("format.no");
	TranslatableComponent.Builder FORMAT_ON = Component.translatable().key("format.on");
	TranslatableComponent.Builder FORMAT_OFF = Component.translatable().key("format.off");
	TranslatableComponent.Builder FORMAT_NEVER = Component.translatable().key("format.never");
	TranslatableComponent.Builder FORMAT_NONE = Component.translatable().key("format.none");
	TranslatableComponent.Builder FORMAT_UNRANKED = Component.translatable().key("format.unranked");
	TranslatableComponent.Builder FORMAT_UNKNOWN = Component.translatable().key("format.unknown");
	TranslatableComponent.Builder FORMAT_MAXED = Component.translatable().key("format.maxed");

	// Chat channels
	TranslatableComponent.Builder CHAT_GLOBAL = Component.translatable().key("chat.global");
	TranslatableComponent.Builder CHAT_LOCAL = Component.translatable().key("chat.local");
	TranslatableComponent.Builder CHAT_PARTY = Component.translatable().key("chat.party");

	// Time formats
	TranslatableComponent.Builder FORMAT_SECOND = Component.translatable().key("format.second");
	TranslatableComponent.Builder FORMAT_MINUTE = Component.translatable().key("format.minute");
	TranslatableComponent.Builder FORMAT_HOUR = Component.translatable().key("format.hour");
	TranslatableComponent.Builder FORMAT_DAY = Component.translatable().key("format.day");

	// Reload command messages
	TranslatableComponent.Builder COMMAND_RELOAD_SUCCESS = Component.translatable().key("command.reload.success");
	TranslatableComponent.Builder COMMAND_RELOADING = Component.translatable().key("command.reloading");

	// Item command messages
	TranslatableComponent.Builder COMMAND_ITEM_FAILURE_NOT_EXIST = Component.translatable()
			.key("command.item.failure.not.exist");
	TranslatableComponent.Builder COMMAND_ITEM_GIVE_SUCCESS = Component.translatable().key("command.item.give.success");
	TranslatableComponent.Builder COMMAND_ITEM_GET_SUCCESS = Component.translatable().key("command.item.get.success");

	// Item import
	TranslatableComponent.Builder COMMAND_ITEM_IMPORT_FAILURE_NO_ITEM = Component.translatable()
			.key("command.item.import.failure.no.item");
	TranslatableComponent.Builder COMMAND_ITEM_IMPORT_SUCCESS = Component.translatable()
			.key("command.item.import.success");

	// Fish finder
	TranslatableComponent.Builder COMMAND_FISH_FINDER_POSSIBLE_LOOTS = Component.translatable()
			.key("command.fishfinder.possible.loots");
	TranslatableComponent.Builder COMMAND_FISH_FINDER_NO_LOOT = Component.translatable()
			.key("command.fishfinder.no.loot");
	TranslatableComponent.Builder COMMAND_FISH_FINDER_SPLIT_CHAR = Component.translatable()
			.key("command.fishfinder.split.char");

	// Data command messages
	TranslatableComponent.Builder COMMAND_DATA_FAILURE_NOT_LOADED = Component.translatable()
			.key("command.data.failure.not.loaded");

	// Market command messages
	TranslatableComponent.Builder COMMAND_MARKET_OPEN_SUCCESS = Component.translatable()
			.key("command.market.open.success");
	TranslatableComponent.Builder COMMAND_MARKET_OPEN_FAILURE_NOT_LOADED = Component.translatable()
			.key("command.market.open.failure.not.loaded");

	// Data unlock
	TranslatableComponent.Builder COMMAND_DATA_UNLOCK_SUCCESS = Component.translatable()
			.key("command.data.unlock.success");

	// Data import
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_FAILURE_NOT_EXISTS = Component.translatable()
			.key("command.data.import.failure.not.exists");
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_FAILURE_PLAYER_ONLINE = Component.translatable()
			.key("command.data.import.failure.player.online");
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_FAILURE_INVALID_FILE = Component.translatable()
			.key("command.data.import.failure.invalid.file");
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_START = Component.translatable().key("command.data.import.start");
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_PROGRESS = Component.translatable()
			.key("command.data.import.progress");
	TranslatableComponent.Builder COMMAND_DATA_IMPORT_SUCCESS = Component.translatable()
			.key("command.data.import.success");

	// Data export
	TranslatableComponent.Builder COMMAND_DATA_EXPORT_FAILURE_PLAYER_ONLINE = Component.translatable()
			.key("command.data.export.failure.player.online");
	TranslatableComponent.Builder COMMAND_DATA_EXPORT_START = Component.translatable().key("command.data.export.start");
	TranslatableComponent.Builder COMMAND_DATA_EXPORT_PROGRESS = Component.translatable()
			.key("command.data.export.progress");
	TranslatableComponent.Builder COMMAND_DATA_EXPORT_SUCCESS = Component.translatable()
			.key("command.data.export.success");

	// Statistics command messages
	TranslatableComponent.Builder COMMAND_STATISTICS_FAILURE_NOT_LOADED = Component.translatable()
			.key("command.statistics.failure.not.loaded");
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

	// Loot debug messages
	TranslatableComponent.Builder COMMAND_DEBUG_LOOT_FAILURE_ROD = Component.translatable()
			.key("command.debug.loot.failure.rod");
	TranslatableComponent.Builder COMMAND_DEBUG_LOOT_SUCCESS = Component.translatable()
			.key("command.debug.loot.success");
	TranslatableComponent.Builder COMMAND_DEBUG_LOOT_FAILURE_NO_LOOT = Component.translatable()
			.key("command.debug.loot.failure.no.loot");

	// NBT debug messages
	TranslatableComponent.Builder COMMAND_DEBUG_NBT_NO_ITEM_IN_HAND = Component.translatable()
			.key("command.debug.nbt.no.item.in.hand");

	// Biome debug messages
	TranslatableComponent.Builder COMMAND_DEBUG_BIOME_RESULT = Component.translatable()
			.key("command.debug.biome.result");

	// World debug messages
	TranslatableComponent.Builder COMMAND_DEBUG_WORLDS_LIST_SUMMARY = Component.translatable()
			.key("command.debug.worlds.list.summary");
	TranslatableComponent.Builder COMMAND_DEBUG_WORLDS_NOT_FOUND = Component.translatable()
			.key("command.debug.worlds.not.found");
	TranslatableComponent.Builder COMMAND_DEBUG_WORLDS_FAILURE = Component.translatable()
			.key("command.debug.worlds.failure");
	TranslatableComponent.Builder COMMAND_DEBUG_WORLDS_SUCCESS = Component.translatable()
			.key("command.debug.worlds.success");

	public static TranslatableComponent forWeatherWarning(WeatherType type) {
		switch (type) {
		case LAVA_RAIN:
			return MSG_HELLBLOCK_LAVARAIN_WARNING.build();
		case ASH_STORM:
			return MSG_HELLBLOCK_ASHSTORM_WARNING.build();
		case EMBER_FOG:
			return MSG_HELLBLOCK_EMBERFOG_WARNING.build();
		case MAGMA_WIND:
			return MSG_HELLBLOCK_MAGMAWIND_WARNING.build();
		default:
			return MSG_HELLBLOCK_LAVARAIN_WARNING.build();
		}
	}
}