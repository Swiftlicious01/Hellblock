package com.swiftlicious.hellblock.handlers;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

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
	 * Converts a MiniMessage string to a JSON string.
	 *
	 * @param miniMessage the MiniMessage string
	 * @return the JSON string representation
	 */
	String miniMessageToJson(String miniMessage);

	/**
	 * Converts a JSON string to a MiniMessage string.
	 *
	 * @param json the JSON string
	 * @return the MiniMessage string representation
	 */
	String jsonToMiniMessage(String json);

	/**
	 * Send a message to a command sender
	 * 
	 * @param sender sender
	 * @param msg    message
	 */
	void sendMessage(CommandSender sender, String msg);

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
	void sendPlayerMessage(CommandSender player, String msg);

	/**
	 * Send a centered message to a player
	 * 
	 * @param player player
	 * @param msg    message
	 */
	void sendCenteredMessage(CommandSender player, String msg);

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
	void sendTitle(CommandSender player, String title, String subtitle, int in, int duration, int out);

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
	void sendTitle(CommandSender player, Component title, Component subtitle, int in, int duration, int out);

	/**
	 * Send actionbar
	 * 
	 * @param player player
	 * @param msg    msg
	 */
	void sendActionbar(CommandSender player, String msg);

	/**
	 * Play a sound to a player
	 * 
	 * @param player player
	 * @param source sound source
	 * @param key    sound key
	 * @param volume volume
	 * @param pitch  pitch
	 */
	void playSound(CommandSender player, Sound.Source source, net.kyori.adventure.key.Key key, float volume, float pitch);

	/**
	 * Play a sound to the location
	 * 
	 * @param location location
	 * @param source   sound source
	 * @param key      sound key
	 * @param volume   volume
	 * @param pitch    pitch
	 */
	void playSound(Location location, Sound.Source source, net.kyori.adventure.key.Key key, float volume, float pitch);

	void playSound(CommandSender player, Sound sound);

	void playSound(Location location, Sound sound);

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