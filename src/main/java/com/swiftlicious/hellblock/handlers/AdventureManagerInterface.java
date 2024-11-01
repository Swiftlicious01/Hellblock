package com.swiftlicious.hellblock.handlers;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

public interface AdventureManagerInterface {

	/**
	 * Get component from text
	 * 
	 * @param text text
	 * @return component
	 */
	Component getComponentFromMiniMessage(String text);

	/**
	 * Send a message to a command sender
	 * 
	 * @param sender sender
	 * @param msg    message
	 */
	void sendMessage(CommandSender sender, String msg);

	/**
	 * Send a message with prefix
	 *
	 * @param sender command sender
	 * @param s      message
	 */
	void sendMessageWithPrefix(CommandSender sender, String msg);

	/**
	 * Send a message to console
	 * 
	 * @param msg message
	 */
	void sendConsoleMessage(String msg);

	/**
	 * Send a message to all players
	 * 
	 * @param msg message
	 */
	void sendGlobalMessage(String msg);

	/**
	 * Send a message to a player
	 * 
	 * @param player player
	 * @param msg    message
	 */
	void sendPlayerMessage(Player player, String msg);

	/**
	 * Send a centered message to a player
	 * 
	 * @param player player
	 * @param msg    message
	 */
	void sendCenteredMessage(Player player, String msg);

	/**
	 * Send a title to a player
	 * 
	 * @param player   player
	 * @param title    title
	 * @param subtitle subtitle
	 * @param in       in (ticks)
	 * @param duration duration (ticks)
	 * @param out      out (ticks)
	 */
	void sendTitle(Player player, String title, String subtitle, int in, int duration, int out);

	/**
	 * Send a title to a player
	 * 
	 * @param player   player
	 * @param title    title
	 * @param subtitle subtitle
	 * @param in       in (ticks)
	 * @param duration duration (ticks)
	 * @param out      out (ticks)
	 */
	void sendTitle(Player player, Component title, Component subtitle, int in, int duration, int out);

	/**
	 * Send actionbar
	 * 
	 * @param player player
	 * @param msg    msg
	 */
	void sendActionbar(Player player, String msg);

	/**
	 * Play a sound to a player
	 * 
	 * @param player player
	 * @param source sound source
	 * @param key    sound key
	 * @param volume volume
	 * @param pitch  pitch
	 */
	void sendSound(Player player, Sound.Source source, net.kyori.adventure.key.Key key, float volume, float pitch);

	/**
	 * Play a sound to the location
	 * 
	 * @param location location
	 * @param source   sound source
	 * @param key      sound key
	 * @param volume   volume
	 * @param pitch    pitch
	 */
	void sendSound(Location location, Sound.Source source, net.kyori.adventure.key.Key key, float volume, float pitch);

	void sendSound(Player player, Sound sound);

	void sendSound(Location location, Sound sound);

	/**
	 * Replace legacy color codes to MiniMessage format
	 * 
	 * @param legacy legacy text
	 * @return MiniMessage format text
	 */
	String legacyToMiniMessage(String legacy);

	/**
	 * if a char is legacy color code
	 * 
	 * @param c char
	 * @return is legacy color
	 */
	boolean isColorCode(char c);

	/**
	 * Get legacy format text
	 * 
	 * @param component component
	 * @return legacy format text
	 */
	String componentToLegacy(Component component);

	/**
	 * Get json
	 * 
	 * @param component component
	 * @return json
	 */
	String componentToJson(Component component);

	/**
	 * Get component
	 * 
	 * @param json json
	 * @return component
	 */
	Component jsonToComponent(String json);

	/**
	 * Get paper component
	 * 
	 * @param component shaded component
	 * @return paper component
	 */
	Object shadedComponentToOriginalComponent(Component component);

}