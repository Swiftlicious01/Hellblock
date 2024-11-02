package com.swiftlicious.hellblock.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.VaultHook;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.loot.LootManager;
import com.swiftlicious.hellblock.scheduler.CancellableTask;
import com.swiftlicious.hellblock.utils.ArmorStandUtils;
import com.swiftlicious.hellblock.utils.ItemUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.EmptyAction;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.factory.ActionFactory;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class ActionManager implements ActionManagerInterface {

	private final HellblockPlugin instance;
	private final HashMap<String, ActionFactory> actionFactoryMap;

	public ActionManager(HellblockPlugin plugin) {
		instance = plugin;
		this.actionFactoryMap = new HashMap<>();
		this.registerInbuiltActions();
	}

	// Method to register various built-in actions during initialization.
	private void registerInbuiltActions() {
		this.registerMessageAction();
		this.registerCommandAction();
		this.registerMendingAction();
		this.registerExpAction();
		this.registerChainAction();
		this.registerPotionAction();
		this.registerSoundAction();
		this.registerPluginExpAction();
		this.registerTitleAction();
		this.registerActionBarAction();
		this.registerCloseInvAction();
		this.registerDelayedAction();
		this.registerConditionalAction();
		this.registerPriorityAction();
		this.registerLevelAction();
		this.registerHologramAction();
		this.registerFakeItemAction();
		this.registerFishFindAction();
		this.registerFoodAction();
		this.registerItemAmountAction();
		this.registerItemDurabilityAction();
		this.registerGiveItemAction();
		this.registerMoneyAction();
		this.registerTimerAction();
	}

	public void load() {
		loadGlobalEventActions();
	}

	public void unload() {
		if (instance.getGlobalSettings() != null)
			instance.getGlobalSettings().unload();
	}

	public void disable() {
		unload();
		this.actionFactoryMap.clear();
	}

	// Method to load global event actions from the plugin's configuration file.
	private void loadGlobalEventActions() {
		YamlConfiguration config = instance.getConfig("config.yml");
		instance.getGlobalSettings().loadEvents(config.getConfigurationSection("lava-fishing-options.global-events"));
	}

	/**
	 * Registers an ActionFactory for a specific action type. This method allows you
	 * to associate an ActionFactory with a custom action type.
	 *
	 * @param type          The custom action type to register.
	 * @param actionFactory The ActionFactory responsible for creating actions of
	 *                      the specified type.
	 * @return True if the registration was successful (the action type was not
	 *         already registered), false otherwise.
	 */
	@Override
	public boolean registerAction(String type, ActionFactory actionFactory) {
		if (this.actionFactoryMap.containsKey(type))
			return false;
		this.actionFactoryMap.put(type, actionFactory);
		return true;
	}

	/**
	 * Unregisters an ActionFactory for a specific action type. This method allows
	 * you to remove the association between an action type and its ActionFactory.
	 *
	 * @param type The custom action type to unregister.
	 * @return True if the action type was successfully unregistered, false if it
	 *         was not found.
	 */
	@Override
	public boolean unregisterAction(String type) {
		return this.actionFactoryMap.remove(type) != null;
	}

	/**
	 * Retrieves an Action object based on the configuration provided in a
	 * ConfigurationSection. This method reads the type of action from the section,
	 * obtains the corresponding ActionFactory, and builds an Action object using
	 * the specified values and chance.
	 *
	 * @param section The ConfigurationSection containing the action configuration.
	 * @return An Action object created based on the configuration, or an
	 *         EmptyAction instance if the action type is invalid.
	 */
	@Override
	public Action getAction(ConfigurationSection section) {
		ActionFactory factory = getActionFactory(section.getString("type"));
		if (factory == null) {
			LogUtils.warn(String.format("Action type: %s doesn't exist.", section.getString("type")));
			// to prevent NPE
			return EmptyAction.EMPTY;
		}
		return factory.build(section.get("value"), section.getDouble("chance", 1d));
	}

	/**
	 * Retrieves a mapping of ActionTriggers to arrays of Actions from a
	 * ConfigurationSection. This method iterates through the provided
	 * ConfigurationSection to extract action triggers and their associated arrays
	 * of Actions.
	 *
	 * @param section The ConfigurationSection containing action mappings.
	 * @return A HashMap where keys are ActionTriggers and values are arrays of
	 *         Action objects.
	 */
	@Override
	@NotNull
	public HashMap<ActionTrigger, Action[]> getActionMap(ConfigurationSection section) {
		// Create an empty HashMap to store the action mappings
		HashMap<ActionTrigger, Action[]> actionMap = new HashMap<>();

		// If the provided ConfigurationSection is null, return the empty actionMap
		if (section == null)
			return actionMap;

		// Iterate through all key-value pairs in the ConfigurationSection
		for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection innerSection) {
				// Convert the key to an ActionTrigger enum (assuming it's in uppercase English)
				// and map it to an array of Actions obtained from the inner section
				try {
					actionMap.put(ActionTrigger.valueOf(entry.getKey().toUpperCase(Locale.ENGLISH)),
							getActions(innerSection));
				} catch (IllegalArgumentException e) {
					LogUtils.warn(String.format("Event: %s doesn't exist!", entry.getKey()));
				}
			}
		}
		return actionMap;
	}

	/**
	 * Retrieves an array of Action objects from a ConfigurationSection. This method
	 * iterates through the provided ConfigurationSection to extract Action
	 * configurations and build an array of Action objects.
	 *
	 * @param section The ConfigurationSection containing action configurations.
	 * @return An array of Action objects created based on the configurations in the
	 *         section.
	 */
	@NotNull
	@Override
	public Action[] getActions(ConfigurationSection section) {
		// Create an ArrayList to store the Actions
		ArrayList<Action> actionList = new ArrayList<>();
		if (section == null)
			return actionList.toArray(new Action[0]);

		// Iterate through all key-value pairs in the ConfigurationSection
		for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection innerSection) {
				Action action = getAction(innerSection);
				if (action != null)
					actionList.add(action);
			}
		}
		return actionList.toArray(new Action[0]);
	}

	/**
	 * Retrieves an ActionFactory associated with a specific action type.
	 *
	 * @param type The action type for which to retrieve the ActionFactory.
	 * @return The ActionFactory associated with the specified action type, or null
	 *         if not found.
	 */
	@Nullable
	@Override
	public ActionFactory getActionFactory(String type) {
		return actionFactoryMap.get(type);
	}

	/**
	 * Retrieves a mapping of success times to corresponding arrays of actions from
	 * a ConfigurationSection.
	 *
	 * @param section The ConfigurationSection containing success times actions.
	 * @return A HashMap where success times associated with actions.
	 */
	@Override
	public HashMap<Integer, Action[]> getTimesActionMap(ConfigurationSection section) {
		HashMap<Integer, Action[]> actionMap = new HashMap<>();
		if (section == null)
			return actionMap;
		for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection innerSection) {
				actionMap.put(Integer.parseInt(entry.getKey()), instance.getActionManager().getActions(innerSection));
			}
		}
		return actionMap;
	}

	private void registerMessageAction() {
		registerAction("message", (args, chance) -> {
			List<String> msg = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				List<String> replaced = instance.getPlaceholderManager().parse(condition.getPlayer(), msg,
						condition.getArgs());
				for (String text : replaced) {
					instance.getAdventureManager().sendPlayerMessage(condition.getPlayer(), text);
				}
			};
		});
		registerAction("broadcast", (args, chance) -> {
			List<String> msg = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				List<String> replaced = instance.getPlaceholderManager().parse(condition.getPlayer(), msg,
						condition.getArgs());
				for (Player player : Bukkit.getOnlinePlayers()) {
					for (String text : replaced) {
						instance.getAdventureManager().sendPlayerMessage(player, text);
					}
				}
			};
		});
		registerAction("message-nearby", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				List<String> msg = section.getStringList("message");
				int range = section.getInt("range");
				return condition -> {
					if (Math.random() > chance)
						return;
					Player owner = condition.getPlayer();
					instance.getScheduler().runTaskSync(() -> {
						for (Player player : condition.getLocation().getWorld()
								.getNearbyPlayers(condition.getLocation(), range, range, range)) {
							double distance = LocationUtils.getDistance(player.getLocation(), condition.getLocation());
							if (distance <= range) {
								condition.insertArg("{near}", player.getName());
								List<String> replaced = instance.getPlaceholderManager().parse(owner, msg,
										condition.getArgs());
								for (String text : replaced) {
									instance.getAdventureManager().sendPlayerMessage((Player) player, text);
								}
								condition.delArg("{near}");
							}
						}
					}, condition.getLocation());
				};
			} else {
				LogUtils.warn("Illegal value format found at action: message-nearby.");
				return EmptyAction.EMPTY;
			}
		});
		registerAction("random-message", (args, chance) -> {
			List<String> msg = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				String random = msg.get(ThreadLocalRandom.current().nextInt(msg.size()));
				random = instance.getPlaceholderManager().parse(condition.getPlayer(), random, condition.getArgs());
				instance.getAdventureManager().sendPlayerMessage(condition.getPlayer(), random);
			};
		});
	}

	private void registerCommandAction() {
		registerAction("command", (args, chance) -> {
			List<String> cmd = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				List<String> replaced = instance.getPlaceholderManager().parse(condition.getPlayer(), cmd,
						condition.getArgs());
				instance.getScheduler().runTaskSync(() -> {
					for (String text : replaced) {
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), text);
					}
				}, condition.getLocation());
			};
		});
		registerAction("random-command", (args, chance) -> {
			List<String> cmd = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				String random = cmd.get(ThreadLocalRandom.current().nextInt(cmd.size()));
				random = instance.getPlaceholderManager().parse(condition.getPlayer(), random, condition.getArgs());
				String finalRandom = random;
				instance.getScheduler().runTaskSync(() -> {
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalRandom);
				}, condition.getLocation());
			};
		});
		registerAction("command-nearby", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				List<String> cmd = section.getStringList("command");
				int range = section.getInt("range");
				return condition -> {
					if (Math.random() > chance)
						return;
					Player owner = condition.getPlayer();
					instance.getScheduler().runTaskSync(() -> {
						for (Player player : condition.getLocation().getWorld()
								.getNearbyPlayers(condition.getLocation(), range, range, range)) {
							double distance = LocationUtils.getDistance(player.getLocation(), condition.getLocation());
							if (distance <= range) {
								condition.insertArg("{near}", player.getName());
								List<String> replaced = instance.getPlaceholderManager().parse(owner, cmd,
										condition.getArgs());
								for (String text : replaced) {
									Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), text);
								}
								condition.delArg("{near}");
							}
						}
					}, condition.getLocation());
				};
			} else {
				LogUtils.warn("Illegal value format found at action: command-nearby.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerCloseInvAction() {
		registerAction("close-inv", (args, chance) -> condition -> {
			if (Math.random() > chance)
				return;
			condition.getPlayer().closeInventory();
		});
	}

	private void registerActionBarAction() {
		registerAction("actionbar", (args, chance) -> {
			String text = (String) args;
			return condition -> {
				if (Math.random() > chance)
					return;
				String parsed = instance.getPlaceholderManager().parse(condition.getPlayer(), text,
						condition.getArgs());
				instance.getAdventureManager().sendActionbar(condition.getPlayer(), parsed);
			};
		});
		registerAction("random-actionbar", (args, chance) -> {
			List<String> texts = instance.getConfigUtils().stringListArgs(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				String random = texts.get(ThreadLocalRandom.current().nextInt(texts.size()));
				random = instance.getPlaceholderManager().parse(condition.getPlayer(), random, condition.getArgs());
				instance.getAdventureManager().sendActionbar(condition.getPlayer(), random);
			};
		});
		registerAction("actionbar-nearby", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				String actionbar = section.getString("actionbar");
				int range = section.getInt("range");
				return condition -> {
					if (Math.random() > chance)
						return;
					Player owner = condition.getPlayer();
					instance.getScheduler().runTaskSync(() -> {
						for (Player player : condition.getLocation().getWorld()
								.getNearbyPlayers(condition.getLocation(), range, range, range)) {
							double distance = LocationUtils.getDistance(player.getLocation(), condition.getLocation());
							if (distance <= range) {
								condition.insertArg("{near}", player.getName());
								String replaced = instance.getPlaceholderManager().parse(owner, actionbar,
										condition.getArgs());
								instance.getAdventureManager().sendActionbar((Player) player, replaced);
								condition.delArg("{near}");
							}
						}
					}, condition.getLocation());
				};
			} else {
				LogUtils.warn("Illegal value format found at action: command-nearby.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerMendingAction() {
		registerAction("mending", (args, chance) -> {
			var value = instance.getConfigUtils().getValue(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				if (instance.getVersionManager().isSpigot()) {
					condition.getPlayer().getLocation().getWorld().spawn(condition.getPlayer().getLocation(),
							ExperienceOrb.class,
							e -> e.setExperience((int) value.get(condition.getPlayer(), condition.getArgs())));
				} else {
					condition.getPlayer().giveExp((int) value.get(condition.getPlayer(), condition.getArgs()));
					instance.getAdventureManager().sendSound(condition.getPlayer(), Sound.Source.PLAYER,
							Key.key("minecraft:entity.experience_orb.pickup"), 1, 1);
				}
			};
		});
	}

	private void registerFoodAction() {
		registerAction("food", (args, chance) -> {
			var value = instance.getConfigUtils().getValue(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				Player player = condition.getPlayer();
				player.setFoodLevel((int) (player.getFoodLevel() + value.get(player, condition.getArgs())));
			};
		});
		registerAction("saturation", (args, chance) -> {
			var value = instance.getConfigUtils().getValue(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				Player player = condition.getPlayer();
				player.setSaturation((float) (player.getSaturation() + value.get(player, condition.getArgs())));
			};
		});
	}

	private void registerExpAction() {
		registerAction("exp", (args, chance) -> {
			var value = instance.getConfigUtils().getValue(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				condition.getPlayer().giveExp((int) value.get(condition.getPlayer(), condition.getArgs()));
				instance.getAdventureManager().sendSound(condition.getPlayer(), Sound.Source.PLAYER,
						Key.key("minecraft:entity.experience_orb.pickup"), 1, 1);
			};
		});
	}

	private void registerHologramAction() {
		registerAction("hologram", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				String text = section.getString("text", "");
				int duration = section.getInt("duration", 20);
				boolean position = section.getString("position", "other").equals("other");
				double x = section.getDouble("x");
				double y = section.getDouble("y");
				double z = section.getDouble("z");
				int range = section.getInt("range", 16);
				return condition -> {
					if (Math.random() > chance)
						return;
					Player owner = condition.getPlayer();
					Location location = position ? condition.getLocation() : owner.getLocation();
					if (range > 0) {
						instance.getScheduler().runTaskSync(() -> {
							for (Player player : location.getWorld().getNearbyPlayers(location, range, range, range)) {
								double distance = LocationUtils.getDistance(player.getLocation(), location);
								if (distance <= range) {
									ArmorStandUtils.sendHologram((Player) player, location.clone().add(x, y, z),
											instance.getAdventureManager().getComponentFromMiniMessage(instance
													.getPlaceholderManager().parse(owner, text, condition.getArgs())),
											duration);
								}
							}
						}, location);
					} else {
						ArmorStandUtils.sendHologram(owner, location.clone().add(x, y, z),
								instance.getAdventureManager().getComponentFromMiniMessage(
										instance.getPlaceholderManager().parse(owner, text, condition.getArgs())),
								duration);
					}
				};
			} else {
				LogUtils.warn("Illegal value format found at action: hologram.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerItemAmountAction() {
		registerAction("item-amount", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				boolean mainOrOff = section.getString("hand", "main").equalsIgnoreCase("main");
				int amount = section.getInt("amount", 1);
				return condition -> {
					if (Math.random() > chance)
						return;
					Player player = condition.getPlayer();
					ItemStack itemStack = mainOrOff ? player.getInventory().getItemInMainHand()
							: player.getInventory().getItemInOffHand();
					itemStack.setAmount(Math.max(0, itemStack.getAmount() + amount));
				};
			} else {
				LogUtils.warn("Illegal value format found at action: item-amount.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerItemDurabilityAction() {
		registerAction("durability", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				EquipmentSlot slot = EquipmentSlot
						.valueOf(section.getString("slot", "hand").toUpperCase(Locale.ENGLISH));
				int amount = section.getInt("amount", 1);
				return condition -> {
					if (Math.random() > chance)
						return;
					Player player = condition.getPlayer();
					ItemStack itemStack = player.getInventory().getItem(slot);
					if (amount > 0) {
						ItemUtils.increaseDurability(itemStack, amount, true);
					} else {
						ItemUtils.decreaseDurability(condition.getPlayer(), itemStack, -amount, true);
					}
				};
			} else {
				LogUtils.warn("Illegal value format found at action: durability.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerGiveItemAction() {
		registerAction("give-item", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				String id = section.getString("item");
				int amount = section.getInt("amount", 1);
				return condition -> {
					if (Math.random() > chance)
						return;
					Player player = condition.getPlayer();
					ItemUtils.giveItem(player,
							Objects.requireNonNull(instance.getItemManager().buildAnyPluginItemByID(player, id)),
							amount);
				};
			} else {
				LogUtils.warn("Illegal value format found at action: give-item.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerFakeItemAction() {
		registerAction("fake-item", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				String[] itemSplit = section.getString("item", "").split(":", 2);
				int duration = section.getInt("duration", 20);
				boolean position = !section.getString("position", "player").equals("player");
				String x = instance.getConfigUtils().getString(section.get("x", "0"));
				String y = instance.getConfigUtils().getString(section.get("y", "0"));
				String z = instance.getConfigUtils().getString(section.get("z", "0"));
				String yaw = instance.getConfigUtils().getString(section.get("yaw", "0"));
				int range = section.getInt("range", 0);
				boolean opposite = section.getBoolean("opposite-yaw", false);
				return condition -> {
					if (Math.random() > chance)
						return;
					Player owner = condition.getPlayer();
					Location location = position ? condition.getLocation() : owner.getLocation();
					location = location.clone().add(
							instance.getPlaceholderManager().getExpressionValue(owner, x, condition.getArgs()),
							instance.getPlaceholderManager().getExpressionValue(owner, y, condition.getArgs()),
							instance.getPlaceholderManager().getExpressionValue(owner, z, condition.getArgs()));
					Location finalLocation = location;
					ItemStack itemStack = instance.getItemManager().build(owner, itemSplit[0],
							instance.getPlaceholderManager().parse(owner, itemSplit[1], condition.getArgs()),
							condition.getArgs());
					if (range > 0) {
						instance.getScheduler().runTaskSync(() -> {
							for (Player player : finalLocation.getWorld().getNearbyPlayers(finalLocation, range, range,
									range)) {
								double distance = LocationUtils.getDistance(player.getLocation(), finalLocation);
								if (distance <= range) {
									Location locationTemp = finalLocation.clone();
									if (opposite)
										locationTemp.setYaw(-player.getLocation().getYaw());
									else
										locationTemp.setYaw((float) instance.getPlaceholderManager()
												.getExpressionValue((Player) player, yaw, condition.getArgs()));
									ArmorStandUtils.sendFakeItem(condition.getPlayer(), locationTemp, itemStack,
											duration);
								}
							}
						}, condition.getLocation());
					} else {
						if (opposite)
							finalLocation.setYaw(-owner.getLocation().getYaw());
						else
							finalLocation.setYaw((float) instance.getPlaceholderManager().getExpressionValue(owner, yaw,
									condition.getArgs()));
						ArmorStandUtils.sendFakeItem(condition.getPlayer(), finalLocation, itemStack, duration);
					}
				};
			} else {
				LogUtils.warn("Illegal value format found at action: fake-item.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerChainAction() {
		registerAction("chain", (args, chance) -> {
			List<Action> actions = new ArrayList<>();
			if (args instanceof ConfigurationSection section) {
				for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
					if (entry.getValue() instanceof ConfigurationSection innerSection) {
						actions.add(getAction(innerSection));
					}
				}
			}
			return condition -> {
				if (Math.random() > chance)
					return;
				for (Action action : actions) {
					action.trigger(condition);
				}
			};
		});
	}

	private void registerMoneyAction() {
		registerAction("give-money", (args, chance) -> {
			var value = instance.getConfigUtils().getValue(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				VaultHook.getEconomy().depositPlayer(condition.getPlayer(),
						value.get(condition.getPlayer(), condition.getArgs()));
			};
		});
		registerAction("take-money", (args, chance) -> {
			var value = instance.getConfigUtils().getValue(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				VaultHook.getEconomy().withdrawPlayer(condition.getPlayer(),
						value.get(condition.getPlayer(), condition.getArgs()));
			};
		});
	}

	private void registerDelayedAction() {
		registerAction("delay", (args, chance) -> {
			List<Action> actions = new ArrayList<>();
			int delay;
			boolean async;
			if (args instanceof ConfigurationSection section) {
				delay = section.getInt("delay", 1);
				async = section.getBoolean("async", false);
				ConfigurationSection actionSection = section.getConfigurationSection("actions");
				if (actionSection != null) {
					for (Map.Entry<String, Object> entry : actionSection.getValues(false).entrySet()) {
						if (entry.getValue() instanceof ConfigurationSection innerSection) {
							actions.add(getAction(innerSection));
						}
					}
				}
			} else {
				delay = 1;
				async = false;
			}
			return condition -> {
				if (Math.random() > chance)
					return;
				if (async) {
					instance.getScheduler().runTaskSyncLater(() -> {
						for (Action action : actions) {
							action.trigger(condition);
						}
					}, condition.getLocation(), delay * 50L, TimeUnit.MILLISECONDS);
				} else {
					instance.getScheduler().runTaskSyncLater(() -> {
						for (Action action : actions) {
							action.trigger(condition);
						}
					}, condition.getLocation(), delay * 50L, TimeUnit.MILLISECONDS);
				}
			};
		});
	}

	private void registerTimerAction() {
		registerAction("timer", (args, chance) -> {
			List<Action> actions = new ArrayList<>();
			int delay;
			int duration;
			int period;
			boolean async;
			if (args instanceof ConfigurationSection section) {
				delay = section.getInt("delay", 2);
				duration = section.getInt("duration", 20);
				period = section.getInt("period", 2);
				async = section.getBoolean("async", false);
				ConfigurationSection actionSection = section.getConfigurationSection("actions");
				if (actionSection != null) {
					for (Map.Entry<String, Object> entry : actionSection.getValues(false).entrySet()) {
						if (entry.getValue() instanceof ConfigurationSection innerSection) {
							actions.add(getAction(innerSection));
						}
					}
				}
			} else {
				delay = 1;
				async = false;
				duration = 20;
				period = 1;
			}
			return condition -> {
				if (Math.random() > chance)
					return;
				CancellableTask cancellableTask;
				if (async) {
					cancellableTask = instance.getScheduler().runTaskAsyncTimer(() -> {
						for (Action action : actions) {
							action.trigger(condition);
						}
					}, delay * 50L, period * 50L, TimeUnit.MILLISECONDS);
				} else {
					cancellableTask = instance.getScheduler().runTaskSyncTimer(() -> {
						for (Action action : actions) {
							action.trigger(condition);
						}
					}, condition.getLocation(), delay, period);
				}
				instance.getScheduler().runTaskSyncLater(cancellableTask::cancel, condition.getLocation(), duration);
			};
		});
	}

	private void registerTitleAction() {
		registerAction("title", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				String title = section.getString("title");
				String subtitle = section.getString("subtitle");
				int fadeIn = section.getInt("fade-in", 20);
				int stay = section.getInt("stay", 30);
				int fadeOut = section.getInt("fade-out", 10);
				return condition -> {
					if (Math.random() > chance)
						return;
					instance.getAdventureManager().sendTitle(condition.getPlayer(),
							instance.getPlaceholderManager().parse(condition.getPlayer(), title, condition.getArgs()),
							instance.getPlaceholderManager().parse(condition.getPlayer(), subtitle,
									condition.getArgs()),
							fadeIn, stay, fadeOut);
				};
			} else {
				LogUtils.warn("Illegal value format found at action: title.");
				return EmptyAction.EMPTY;
			}
		});
		registerAction("title-nearby", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				String title = section.getString("title");
				String subtitle = section.getString("subtitle");
				int fadeIn = section.getInt("fade-in", 20);
				int stay = section.getInt("stay", 30);
				int fadeOut = section.getInt("fade-out", 10);
				int range = section.getInt("range", 0);
				return condition -> {
					if (Math.random() > chance)
						return;
					instance.getScheduler().runTaskSync(() -> {
						for (Player player : condition.getLocation().getWorld()
								.getNearbyPlayers(condition.getLocation(), range, range, range)) {
							double distance = LocationUtils.getDistance(player.getLocation(), condition.getLocation());
							if (distance <= range) {
								condition.insertArg("{near}", player.getName());
								instance.getAdventureManager().sendTitle(condition.getPlayer(),
										instance.getPlaceholderManager().parse(condition.getPlayer(), title,
												condition.getArgs()),
										instance.getPlaceholderManager().parse(condition.getPlayer(), subtitle,
												condition.getArgs()),
										fadeIn, stay, fadeOut);
								condition.delArg("{near}");
							}
						}
					}, condition.getLocation());
				};
			} else {
				LogUtils.warn("Illegal value format found at action: title-nearby.");
				return EmptyAction.EMPTY;
			}
		});
		registerAction("random-title", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				List<String> titles = section.getStringList("titles");
				if (titles.size() == 0)
					titles.add("");
				List<String> subtitles = section.getStringList("subtitles");
				if (subtitles.size() == 0)
					subtitles.add("");
				int fadeIn = section.getInt("fade-in", 20);
				int stay = section.getInt("stay", 30);
				int fadeOut = section.getInt("fade-out", 10);
				return condition -> {
					if (Math.random() > chance)
						return;
					instance.getAdventureManager().sendTitle(condition.getPlayer(),
							instance.getPlaceholderManager().parse(condition.getPlayer(),
									titles.get(ThreadLocalRandom.current().nextInt(titles.size())),
									condition.getArgs()),
							instance.getPlaceholderManager().parse(condition.getPlayer(),
									subtitles.get(ThreadLocalRandom.current().nextInt(subtitles.size())),
									condition.getArgs()),
							fadeIn, stay, fadeOut);
				};
			} else {
				LogUtils.warn("Illegal value format found at action: random-title.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerPotionAction() {
		registerAction("potion-effect", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				// Fetch the potion effect registry from the registry access
				final Registry<PotionType> potionRegistry = RegistryAccess.registryAccess()
						.getRegistry(RegistryKey.POTION);
				@NotNull
				List<PotionEffect> effectTypes = Objects
						.requireNonNull(potionRegistry.getOrThrow(NamespacedKey
								.fromString(section.getString("type", "BLINDNESS").toUpperCase(Locale.ENGLISH))))
						.getPotionEffects();
				for (PotionEffect effect : effectTypes) {
					PotionEffect potionEffect = new PotionEffect(effect.getType(), section.getInt("duration", 20),
							section.getInt("amplifier", 0));
					return condition -> {
						if (Math.random() > chance)
							return;
						condition.getPlayer().addPotionEffect(potionEffect);
					};
				}
			} else {
				LogUtils.warn("Illegal value format found at action: potion-effect.");
				return EmptyAction.EMPTY;
			}
			return EmptyAction.EMPTY;
		});
	}

	private void registerLevelAction() {
		registerAction("level", (args, chance) -> {
			var value = instance.getConfigUtils().getValue(args);
			return condition -> {
				if (Math.random() > chance)
					return;
				Player player = condition.getPlayer();
				player.setLevel(
						(int) Math.max(0, player.getLevel() + value.get(condition.getPlayer(), condition.getArgs())));
			};
		});
	}

	private void registerSoundAction() {
		registerAction("sound", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				Sound sound = Sound.sound(Key.key(section.getString("key")),
						Sound.Source.valueOf(section.getString("source", "PLAYER").toUpperCase(Locale.ENGLISH)),
						(float) section.getDouble("volume", 1), (float) section.getDouble("pitch", 1));
				return condition -> {
					if (Math.random() > chance)
						return;
					instance.getAdventureManager().sendSound(condition.getPlayer(), sound);
				};
			} else {
				LogUtils.warn("Illegal value format found at action: sound.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerConditionalAction() {
		registerAction("conditional", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				Action[] actions = getActions(section.getConfigurationSection("actions"));
				Requirement[] requirements = instance.getRequirementManager()
						.getRequirements(section.getConfigurationSection("conditions"), true);
				return condition -> {
					if (Math.random() > chance)
						return;
					if (requirements != null)
						for (Requirement requirement : requirements) {
							if (!requirement.isConditionMet(condition)) {
								return;
							}
						}
					for (Action action : actions) {
						action.trigger(condition);
					}
				};
			} else {
				LogUtils.warn("Illegal value format found at action: conditional.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerPriorityAction() {
		registerAction("priority", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				List<Pair<Requirement[], Action[]>> conditionActionPairList = new ArrayList<>();
				for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
					if (entry.getValue() instanceof ConfigurationSection inner) {
						Action[] actions = getActions(inner.getConfigurationSection("actions"));
						Requirement[] requirements = instance.getRequirementManager()
								.getRequirements(inner.getConfigurationSection("conditions"), false);
						conditionActionPairList.add(Pair.of(requirements, actions));
					}
				}
				return condition -> {
					if (Math.random() > chance)
						return;
					outer: for (Pair<Requirement[], Action[]> pair : conditionActionPairList) {
						if (pair.left() != null)
							for (Requirement requirement : pair.left()) {
								if (!requirement.isConditionMet(condition)) {
									continue outer;
								}
							}
						if (pair.right() != null)
							for (Action action : pair.right()) {
								action.trigger(condition);
							}
						return;
					}
				};
			} else {
				LogUtils.warn("Illegal value format found at action: priority.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerPluginExpAction() {
		registerAction("plugin-exp", (args, chance) -> {
			if (args instanceof ConfigurationSection section) {
				String pluginName = section.getString("plugin");
				var value = instance.getConfigUtils().getValue(section.get("exp"));
				String target = section.getString("target");
				return condition -> {
					if (Math.random() > chance)
						return;
					Optional.ofNullable(instance.getIntegrationManager().getLevelPlugin(pluginName))
							.ifPresentOrElse(it -> {
								it.addXp(condition.getPlayer(), target,
										value.get(condition.getPlayer(), condition.getArgs()));
							}, () -> LogUtils.warn(String.format(
									"Plugin (%s's) level is not compatible. Please double check if it's a problem caused by pronunciation.",
									pluginName)));
				};
			} else {
				LogUtils.warn("Illegal value format found at action: plugin-exp.");
				return EmptyAction.EMPTY;
			}
		});
	}

	private void registerFishFindAction() {
		registerAction("fish-finder", (args, chance) -> {
			boolean arg = (boolean) args;
			return condition -> {
				if (Math.random() > chance)
					return;
				condition.insertArg("{lava}", String.valueOf(arg));
				LootManager lootManager = instance.getLootManager();
				List<String> loots = instance.getLootManager().getPossibleLootKeys(condition).stream()
						.map(lootManager::getLoot).filter(Objects::nonNull).filter(Loot::showInFinder)
						.map(Loot::getNick).toList();
				StringJoiner stringJoiner = new StringJoiner(HBLocale.MSG_Split_Char);
				for (String loot : loots) {
					stringJoiner.add(loot);
				}
				condition.delArg("{lava}");
				instance.getAdventureManager().sendMessageWithPrefix(condition.getPlayer(),
						HBLocale.MSG_Possible_Loots + stringJoiner);
			};
		});
	}
}
