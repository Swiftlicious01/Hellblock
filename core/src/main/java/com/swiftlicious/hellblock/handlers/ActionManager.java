package com.swiftlicious.hellblock.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.creation.addons.VaultHook;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.effects.EffectInterface;
import com.swiftlicious.hellblock.nms.entity.FakeEntity;
import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.nms.entity.display.FakeItemDisplay;
import com.swiftlicious.hellblock.nms.toast.AdvancementType;
import com.swiftlicious.hellblock.player.ContextKeys;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.ListUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.factory.ActionFactory;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

import static java.util.Objects.requireNonNull;

public class ActionManager implements ActionManagerInterface<Player> {

	protected final HellblockPlugin instance;
	private final Map<String, ActionFactory<Player>> actionFactoryMap = new HashMap<>();

	public ActionManager(HellblockPlugin plugin) {
		this.instance = plugin;
		this.registerBuiltInActions();
	}

	@Override
	public void disable() {
		this.actionFactoryMap.clear();
	}

	@Override
	public boolean registerAction(ActionFactory<Player> actionFactory, String... types) {
		for (String type : types) {
			if (this.actionFactoryMap.containsKey(type))
				return false;
		}
		for (String type : types) {
			this.actionFactoryMap.put(type, actionFactory);
		}
		return true;
	}

	@Override
	public boolean unregisterAction(String type) {
		return this.actionFactoryMap.remove(type) != null;
	}

	@Nullable
	@Override
	public ActionFactory<Player> getActionFactory(@NotNull String type) {
		return actionFactoryMap.get(type);
	}

	@Override
	public boolean hasAction(@NotNull String type) {
		return actionFactoryMap.containsKey(type);
	}

	@Override
	public Action<Player> parseAction(Section section) {
		if (section == null)
			return Action.empty();
		ActionFactory<Player> factory = getActionFactory(section.getString("type"));
		if (factory == null) {
			instance.getPluginLogger().warn("Action type: " + section.getString("type") + " doesn't exist.");
			return Action.empty();
		}
		return factory.process(section.get("value"),
				section.contains("chance") ? MathValue.auto(section.get("chance")) : MathValue.plain(1));
	}

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public Action<Player>[] parseActions(Section section) {
		List<Action<Player>> actionList = new ArrayList<>();
		if (section != null)
			for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					Action<Player> action = parseAction(innerSection);
					if (action != null)
						actionList.add(action);
				}
			}
		return actionList.toArray(new Action[0]);
	}

	@Override
	public Action<Player> parseAction(@NotNull String type, @NotNull Object args) {
		ActionFactory<Player> factory = getActionFactory(type);
		if (factory == null) {
			instance.getPluginLogger().warn("Action type: " + type + " doesn't exist.");
			return Action.empty();
		}
		return factory.process(args, MathValue.plain(1));
	}

	private void registerBuiltInActions() {
		this.registerMessageAction();
		this.registerToastAction();
		this.registerCommandAction();
		this.registerActionBarAction();
		this.registerCloseInvAction();
		this.registerExpAction();
		this.registerFoodAction();
		this.registerBuildAction();
		this.registerMoneyAction();
		this.registerItemAction();
		this.registerPotionAction();
		this.registerFishFindAction();
		this.registerPluginExpAction();
		this.registerSoundAction();
		this.registerParticleAction();
		this.registerHologramAction();
		this.registerFakeItemAction();
		this.registerTitleAction();
	}

	private void registerMessageAction() {
		registerAction((args, chance) -> {
			List<String> messages = ListUtils.toList(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				final Player player = context.holder();
				List<String> replaced = instance.getPlaceholderManager().parse(player, messages,
						context.placeholderMap());
				Audience audience = instance.getSenderFactory().getAudience(player);
				for (String text : replaced) {
					audience.sendMessage(AdventureHelper.miniMessage(text));
				}
			};
		}, "message");
		registerAction((args, chance) -> {
			List<String> messages = ListUtils.toList(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				String random = messages.get(RandomUtils.generateRandomInt(0, messages.size() - 1));
				final Player player = context.holder();
				random = instance.getPlaceholderManager().parse(player, random, context.placeholderMap());
				Audience audience = instance.getSenderFactory().getAudience(player);
				audience.sendMessage(AdventureHelper.miniMessage(random));
			};
		}, "random-message");
		registerAction((args, chance) -> {
			List<String> messages = ListUtils.toList(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				List<String> replaced = instance.getPlaceholderManager().parse(context.holder(), messages,
						context.placeholderMap());
				for (Player player : Bukkit.getOnlinePlayers()) {
					Audience audience = instance.getSenderFactory().getAudience(player);
					for (String text : replaced) {
						audience.sendMessage(AdventureHelper.miniMessage(text));
					}
				}
			};
		}, "broadcast");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				List<String> messages = ListUtils.toList(section.get("message"));
				MathValue<Player> range = MathValue.auto(section.get("range"));
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					double realRange = range.evaluate(context);
					final Player owner = context.holder();
					Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
					for (Player player : location.getWorld().getPlayers()) {
						if (LocationUtils.getDistance(player.getLocation(), location) <= realRange) {
							context.arg(ContextKeys.TEMP_NEAR_PLAYER, player.getName());
							List<String> replaced = instance.getPlaceholderManager().parse(owner, messages,
									context.placeholderMap());
							Audience audience = instance.getSenderFactory().getAudience(player);
							for (String text : replaced) {
								audience.sendMessage(AdventureHelper.miniMessage(text));
							}
						}
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at message-nearby action which should be Section");
				return Action.empty();
			}
		}, "message-nearby");
	}

	private void registerCommandAction() {
		registerAction((args, chance) -> {
			List<String> commands = ListUtils.toList(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				List<String> replaced = instance.getPlaceholderManager().parse(context.holder(), commands,
						context.placeholderMap());
				instance.getScheduler().sync().run(() -> {
					for (String text : replaced) {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), text);
					}
				}, null);
			};
		}, "command");
		registerAction((args, chance) -> {
			List<String> commands = ListUtils.toList(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				List<String> replaced = instance.getPlaceholderManager().parse(context.holder(), commands,
						context.placeholderMap());
				instance.getScheduler().sync().run(() -> {
					for (String text : replaced) {
						context.holder().performCommand(text);
					}
				}, context.holder().getLocation());
			};
		}, "player-command");
		registerAction((args, chance) -> {
			List<String> commands = ListUtils.toList(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				String random = commands.get(ThreadLocalRandom.current().nextInt(commands.size()));
				random = instance.getPlaceholderManager().parse(context.holder(), random, context.placeholderMap());
				String finalRandom = random;
				instance.getScheduler().sync().run(() -> {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalRandom);
				}, null);
			};
		}, "random-command");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				List<String> cmd = ListUtils.toList(section.get("command"));
				MathValue<Player> range = MathValue.auto(section.get("range"));
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					Player owner = context.holder();
					double realRange = range.evaluate(context);
					Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
					for (Player player : location.getWorld().getPlayers()) {
						if (LocationUtils.getDistance(player.getLocation(), location) <= realRange) {
							context.arg(ContextKeys.TEMP_NEAR_PLAYER, player.getName());
							List<String> replaced = instance.getPlaceholderManager().parse(owner, cmd,
									context.placeholderMap());
							for (String text : replaced) {
								Bukkit.dispatchCommand(Bukkit.getConsoleSender(), text);
							}
						}
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at command-nearby action which should be Section");
				return Action.empty();
			}
		}, "command-nearby");
	}

	private void registerCloseInvAction() {
		registerAction((args, chance) -> context -> {
			if (Math.random() > chance.evaluate(context))
				return;
			context.holder().closeInventory();
		}, "close-inv");
	}

	private void registerToastAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				String title = section.getString("title");
				AdvancementType advancementType = AdvancementType
						.valueOf(Objects.requireNonNull(section.getString("advancement-type")));
				String material = section.getString("icon");
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					if (Material.getMaterial(material) == null || Material.getMaterial(material) == Material.AIR)
						return;
					Player player = context.holder();
					ItemStack itemStack = new ItemStack(Material.getMaterial(material));
					if (itemStack != null) {
						String temp;
						temp = instance.getPlaceholderManager().parse(context.holder(), title,
								context.placeholderMap());
						instance.getVersionManager().getNMSManager().sendToast(player, itemStack,
								AdventureHelper.componentToJson(AdventureHelper.getMiniMessage().deserialize(temp)),
								advancementType.name());
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at toast action which is expected to be `Section`");
				return Action.empty();
			}
		}, "toast");
	}

	private void registerActionBarAction() {
		registerAction((args, chance) -> {
			String text = (String) args;
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				final Player player = context.holder();
				Audience audience = instance.getSenderFactory().getAudience(context.holder());
				Component component = AdventureHelper
						.miniMessage(instance.getPlaceholderManager().parse(player, text, context.placeholderMap()));
				audience.sendActionBar(component);
			};
		}, "actionbar");
		registerAction((args, chance) -> {
			List<String> texts = ListUtils.toList(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				String random = texts.get(RandomUtils.generateRandomInt(0, texts.size() - 1));
				final Player player = context.holder();
				random = instance.getPlaceholderManager().parse(player, random, context.placeholderMap());
				Audience audience = instance.getSenderFactory().getAudience(player);
				audience.sendActionBar(AdventureHelper.miniMessage(random));
			};
		}, "random-actionbar");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				String actionbar = section.getString("actionbar");
				MathValue<Player> range = MathValue.auto(section.get("range"));
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					Player owner = context.holder();
					Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
					double realRange = range.evaluate(context);
					for (Player player : location.getWorld().getPlayers()) {
						if (LocationUtils.getDistance(player.getLocation(), location) <= realRange) {
							context.arg(ContextKeys.TEMP_NEAR_PLAYER, player.getName());
							String replaced = instance.getPlaceholderManager().parse(owner, actionbar,
									context.placeholderMap());
							Audience audience = instance.getSenderFactory().getAudience(player);
							audience.sendActionBar(AdventureHelper.miniMessage(replaced));
						}
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at actionbar-nearby action which should be Section");
				return Action.empty();
			}
		}, "actionbar-nearby");
	}

	private void registerExpAction() {
		registerAction((args, chance) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				final Player player = context.holder();
				ExperienceOrb entity = player.getLocation().getWorld()
						.spawn(player.getLocation().clone().add(0, 0.5, 0), ExperienceOrb.class);
				entity.setExperience((int) value.evaluate(context));
			};
		}, "mending");
		registerAction((args, chance) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				final Player player = context.holder();
				player.giveExp((int) Math.round(value.evaluate(context)));
				instance.getSenderFactory().getAudience(player).playSound(
						Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.PLAYER, 1, 1));
			};
		}, "exp");
		registerAction((args, chance) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				final Player player = context.holder();
				player.setLevel((int) Math.max(0, player.getLevel() + value.evaluate(context)));
			};
		}, "level");
	}

	private void registerFoodAction() {
		registerAction((args, chance) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				Player player = context.holder();
				player.setFoodLevel((int) (player.getFoodLevel() + value.evaluate(context)));
			};
		}, "food");
		registerAction((args, chance) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				Player player = context.holder();
				player.setSaturation((float) (player.getSaturation() + value.evaluate(context)));
			};
		}, "saturation");
	}

	private void registerItemAction() {
		registerAction((args, chance) -> {
			Boolean mainOrOff;
			int amount;
			if (args instanceof Integer integer) {
				mainOrOff = null;
				amount = integer;
			} else if (args instanceof Section section) {
				String hand = section.getString("hand");
				mainOrOff = hand == null ? null : hand.equalsIgnoreCase("main");
				amount = section.getInt("amount", 1);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at item-amount action which is expected to be `Section`");
				return Action.empty();
			}
			return context -> {
				if (context.holder() == null)
					return;
				if (Math.random() > chance.evaluate(context))
					return;
				Player player = context.holder();
				EquipmentSlot hand = context.arg(ContextKeys.SLOT);
				if (mainOrOff == null && hand == null) {
					return;
				}
				boolean tempHand = Objects.requireNonNullElseGet(mainOrOff, () -> hand == EquipmentSlot.HAND);
				ItemStack itemStack = tempHand ? player.getInventory().getItemInMainHand()
						: player.getInventory().getItemInOffHand();
				if (amount < 0) {
					itemStack.setAmount(Math.max(0, itemStack.getAmount() + amount));
				} else if (amount > 0) {
					PlayerUtils.giveItem(player, itemStack, amount);
				}
			};
		}, "item-amount");
		registerAction((args, chance) -> {
			int amount;
			EquipmentSlot slot;
			if (args instanceof Integer integer) {
				slot = null;
				amount = integer;
			} else if (args instanceof Section section) {
				slot = Optional.ofNullable(section.getString("slot"))
						.map(hand -> EquipmentSlot.valueOf(hand.toUpperCase(Locale.ENGLISH))).orElse(null);
				amount = section.getInt("amount", 1);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at durability action which is expected to be `Section`");
				return Action.empty();
			}
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				Player player = context.holder();
				if (player == null)
					return;
				EquipmentSlot tempSlot = slot;
				EquipmentSlot equipmentSlot = context.arg(ContextKeys.SLOT);
				if (tempSlot == null && equipmentSlot != null) {
					tempSlot = equipmentSlot;
				}
				if (tempSlot == null) {
					return;
				}
				ItemStack itemStack = player.getInventory().getItem(tempSlot);
				if (itemStack.getType() == Material.AIR || itemStack.getAmount() == 0)
					return;
				if (itemStack.getItemMeta() == null)
					return;
				if (amount > 0) {
					instance.getItemManager().decreaseDamage(context.holder(), itemStack, amount);
				} else {
					instance.getItemManager().increaseDamage(context.holder(), itemStack, -amount, true);
				}
			};
		}, "durability");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				String material = section.getString("material");
				String displayName = section.getString("display.name");
				List<String> displayLore = section.getStringList("display.lore");
				Map<com.swiftlicious.hellblock.utils.extras.Key, Short> enchantments = instance.getConfigManager()
						.getEnchantments(section);
				boolean unbreakable = section.getBoolean("unbreakable", false);
				int damage = section.getInt("damage");
				int amount = section.getInt("amount", 1);
				boolean toInventory = section.getBoolean("to-inventory", false);
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					if (Material.getMaterial(material) == null || Material.getMaterial(material) == Material.AIR)
						return;
					Player player = context.holder();
					ItemStack itemStack = new ItemStack(Material.getMaterial(material), amount);
					if (itemStack != null) {
						Item<ItemStack> item = instance.getItemManager().wrap(itemStack);
						if (displayName != null && !displayName.isEmpty()) {
							TextValue<Player> name = TextValue.auto("<!i><white>" + displayName);
							item.displayName(AdventureHelper.miniMessageToJson(name.render(context)));
						}
						if (displayLore != null && !displayLore.isEmpty()) {
							List<TextValue<Player>> lore = new ArrayList<>();
							for (String text : displayLore) {
								lore.add(TextValue.auto("<!i><white>" + text));
							}
							item.lore(lore.stream().map(it -> AdventureHelper.miniMessageToJson(it.render(context)))
									.toList());
						}
						if (enchantments != null && !enchantments.isEmpty()) {
							item.enchantments(enchantments);
						}
						if (unbreakable) {
							item.unbreakable(true);
						}
						if (damage > 0) {
							item.damage(damage);
						}
						int maxStack = itemStack.getMaxStackSize();
						int amountToGive = amount;
						while (amountToGive > 0) {
							int perStackSize = Math.min(maxStack, amountToGive);
							amountToGive -= perStackSize;
							ItemStack more = item.load().clone();
							more.setAmount(perStackSize);
							if (toInventory) {
								PlayerUtils.giveItem(player, more, more.getAmount());
							} else {
								PlayerUtils.dropItem(player, more, false, true, false);
							}
						}
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at give-vanilla-item action which is expected to be `Section`");
				return Action.empty();
			}
		}, "give-vanilla-item");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				String id = section.getString("item");
				int amount = section.getInt("amount", 1);
				boolean toInventory = section.getBoolean("to-inventory", false);
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					Player player = context.holder();
					ItemStack itemStack = instance.getItemManager().buildAny(context, id);
					if (itemStack != null) {
						int maxStack = itemStack.getMaxStackSize();
						int amountToGive = amount;
						while (amountToGive > 0) {
							int perStackSize = Math.min(maxStack, amountToGive);
							amountToGive -= perStackSize;
							ItemStack more = itemStack.clone();
							more.setAmount(perStackSize);
							if (toInventory) {
								PlayerUtils.giveItem(player, more, more.getAmount());
							} else {
								PlayerUtils.dropItem(player, more, false, true, false);
							}
						}
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at give-custom-item action which is expected to be `Section`");
				return Action.empty();
			}
		}, "give-custom-item");
	}

	private void registerBuildAction() {
		registerAction((args, chance) -> {
			List<Action<Player>> actions = new ArrayList<>();
			if (args instanceof Section section) {
				for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
					if (entry.getValue() instanceof Section innerSection) {
						actions.add(parseAction(innerSection));
					}
				}
			}
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				for (Action<Player> action : actions) {
					action.trigger(context);
				}
			};
		}, "chain");
		registerAction((args, chance) -> {
			List<Action<Player>> actions = new ArrayList<>();
			int delay;
			boolean async;
			if (args instanceof Section section) {
				delay = section.getInt("delay", 1);
				async = section.getBoolean("async", false);
				Section actionSection = section.getSection("actions");
				if (actionSection != null)
					for (Map.Entry<String, Object> entry : actionSection.getStringRouteMappedValues(false).entrySet())
						if (entry.getValue() instanceof Section innerSection)
							actions.add(parseAction(innerSection));
			} else {
				delay = 1;
				async = false;
			}
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				Location location = context.arg(ContextKeys.LOCATION);
				if (async) {
					instance.getScheduler().asyncLater(() -> {
						for (Action<Player> action : actions)
							action.trigger(context);
					}, delay * 50L, TimeUnit.MILLISECONDS);
				} else {
					instance.getScheduler().sync().runLater(() -> {
						for (Action<Player> action : actions)
							action.trigger(context);
					}, delay, location);
				}
			};
		}, "delay");
		registerAction((args, chance) -> {
			List<Action<Player>> actions = new ArrayList<>();
			int delay, duration, period;
			boolean async;
			if (args instanceof Section section) {
				delay = section.getInt("delay", 2);
				duration = section.getInt("duration", 20);
				period = section.getInt("period", 2);
				async = section.getBoolean("async", false);
				Section actionSection = section.getSection("actions");
				if (actionSection != null)
					for (Map.Entry<String, Object> entry : actionSection.getStringRouteMappedValues(false).entrySet())
						if (entry.getValue() instanceof Section innerSection)
							actions.add(parseAction(innerSection));
			} else {
				delay = 1;
				period = 1;
				async = false;
				duration = 20;
			}
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				Location location = context.arg(ContextKeys.LOCATION);
				SchedulerTask task;
				if (async) {
					task = instance.getScheduler().asyncRepeating(() -> {
						for (Action<Player> action : actions) {
							action.trigger(context);
						}
					}, delay * 50L, period * 50L, TimeUnit.MILLISECONDS);
				} else {
					task = instance.getScheduler().sync().runRepeating(() -> {
						for (Action<Player> action : actions) {
							action.trigger(context);
						}
					}, delay, period, location);
				}
				instance.getScheduler().asyncLater(task::cancel, duration * 50L, TimeUnit.MILLISECONDS);
			};
		}, "timer");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				Action<Player>[] actions = parseActions(section.getSection("actions"));
				Requirement<Player>[] requirements = instance.getRequirementManager()
						.parseRequirements(section.getSection("conditions"), true);
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					for (Requirement<Player> requirement : requirements) {
						if (!requirement.isSatisfied(context)) {
							return;
						}
					}
					for (Action<Player> action : actions) {
						action.trigger(context);
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at conditional action which is expected to be `Section`");
				return Action.empty();
			}
		}, "conditional");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				List<Pair<Requirement<Player>[], Action<Player>[]>> conditionActionPairList = new ArrayList<>();
				for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
					if (entry.getValue() instanceof Section inner) {
						Action<Player>[] actions = parseActions(inner.getSection("actions"));
						Requirement<Player>[] requirements = instance.getRequirementManager()
								.parseRequirements(inner.getSection("conditions"), false);
						conditionActionPairList.add(Pair.of(requirements, actions));
					}
				}
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					outer: for (Pair<Requirement<Player>[], Action<Player>[]> pair : conditionActionPairList) {
						if (pair.left() != null)
							for (Requirement<Player> requirement : pair.left()) {
								if (!requirement.isSatisfied(context)) {
									continue outer;
								}
							}
						if (pair.right() != null)
							for (Action<Player> action : pair.right()) {
								action.trigger(context);
							}
						return;
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at priority action which is expected to be `Section`");
				return Action.empty();
			}
		}, "priority");
	}

	private void registerMoneyAction() {
		registerAction((args, chance) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				if (!VaultHook.isHooked())
					return;
				VaultHook.deposit(context.holder(), value.evaluate(context));
			};
		}, "give-money");
		registerAction((args, chance) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				if (!VaultHook.isHooked())
					return;
				VaultHook.withdraw(context.holder(), value.evaluate(context));
			};
		}, "take-money");
	}

	// The registry name changes a lot
	@SuppressWarnings("deprecation")
	private void registerPotionAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				PotionEffect potionEffect = new PotionEffect(
						Objects.requireNonNull(PotionEffectType
								.getByName(section.getString("type", "BLINDNESS").toUpperCase(Locale.ENGLISH))),
						section.getInt("duration", 20), section.getInt("amplifier", 0));
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					context.holder().addPotionEffect(potionEffect);
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at potion-effect action which is expected to be `Section`");
				return Action.empty();
			}
		}, "potion-effect");
	}

	private void registerSoundAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				Sound sound = Sound.sound(Key.key(section.getString("key")),
						Sound.Source.valueOf(section.getString("source", "PLAYER").toUpperCase(Locale.ENGLISH)),
						section.getFloat("volume", 1.0F).floatValue(), section.getFloat("pitch", 1.0F).floatValue());
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					final Player player = context.holder();
					Audience audience = instance.getSenderFactory().getAudience(player);
					audience.playSound(sound);
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at sound action which is expected to be `Section`");
				return Action.empty();
			}
		}, "sound");
	}

	private void registerParticleAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				Effect effect = Effect.valueOf(section.getString("key"));
				int count = section.getInt("count");
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					final Player player = context.holder();
					player.getWorld().playEffect(player.getLocation(), effect, count);
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at particle action which is expected to be `Section`");
				return Action.empty();
			}
		}, "particle");
	}

	private void registerPluginExpAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				String pluginName = section.getString("plugin");
				MathValue<Player> value = MathValue.auto(section.get("exp"));
				String target = section.getString("target");
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					Optional.ofNullable(instance.getIntegrationManager().getLevelerProvider(pluginName))
							.ifPresentOrElse(it -> {
								it.addXp(context.holder(), target, value.evaluate(context));
							}, () -> instance.getPluginLogger().warn("Plugin (" + pluginName
									+ "'s) level is not compatible. Please double check if it's a problem caused by pronunciation."));
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at plugin-exp action which is expected to be `Section`");
				return Action.empty();
			}
		}, "plugin-exp");
	}

	private void registerTitleAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				TextValue<Player> title = TextValue.auto(section.getString("title", ""));
				TextValue<Player> subtitle = TextValue.auto(section.getString("subtitle", ""));
				int fadeIn = section.getInt("fade-in", 20);
				int stay = section.getInt("stay", 30);
				int fadeOut = section.getInt("fade-out", 10);
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					final Player player = context.holder();
					Audience audience = instance.getSenderFactory().getAudience(player);
					AdventureHelper.sendTitle(audience, AdventureHelper.miniMessage(title.render(context)),
							AdventureHelper.miniMessage(subtitle.render(context)), fadeIn, stay, fadeOut);
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at title action which is expected to be `Section`");
				return Action.empty();
			}
		}, "title");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				List<String> titles = section.getStringList("titles");
				if (titles.isEmpty())
					titles.add("");
				List<String> subtitles = section.getStringList("subtitles");
				if (subtitles.isEmpty())
					subtitles.add("");
				int fadeIn = section.getInt("fade-in", 20);
				int stay = section.getInt("stay", 30);
				int fadeOut = section.getInt("fade-out", 10);
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					TextValue<Player> title = TextValue
							.auto(titles.get(RandomUtils.generateRandomInt(0, titles.size() - 1)));
					TextValue<Player> subtitle = TextValue
							.auto(subtitles.get(RandomUtils.generateRandomInt(0, subtitles.size() - 1)));
					final Player player = context.holder();
					Audience audience = instance.getSenderFactory().getAudience(player);
					AdventureHelper.sendTitle(audience, AdventureHelper.miniMessage(title.render(context)),
							AdventureHelper.miniMessage(subtitle.render(context)), fadeIn, stay, fadeOut);
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at random-title action which is expected to be `Section`");
				return Action.empty();
			}
		}, "random-title");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				TextValue<Player> title = TextValue.auto(section.getString("title"));
				TextValue<Player> subtitle = TextValue.auto(section.getString("subtitle"));
				int fadeIn = section.getInt("fade-in", 20);
				int stay = section.getInt("stay", 30);
				int fadeOut = section.getInt("fade-out", 10);
				int range = section.getInt("range", 0);
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					Location location = requireNonNull(context.arg(ContextKeys.LOCATION));
					for (Player player : location.getWorld().getPlayers()) {
						if (LocationUtils.getDistance(player.getLocation(), location) <= range) {
							context.arg(ContextKeys.TEMP_NEAR_PLAYER, player.getName());
							Audience audience = instance.getSenderFactory().getAudience(player);
							AdventureHelper.sendTitle(audience, AdventureHelper.miniMessage(title.render(context)),
									AdventureHelper.miniMessage(subtitle.render(context)), fadeIn, stay, fadeOut);
						}
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at title-nearby action which is expected to be `Section`");
				return Action.empty();
			}
		}, "title-nearby");
	}

	private void registerFakeItemAction() {
		registerAction(((args, chance) -> {
			if (args instanceof Section section) {
				String itemID = section.getString("item", "");
				String[] split = itemID.split(":");
				if (split.length >= 2)
					itemID = split[split.length - 1];
				MathValue<Player> duration = MathValue.auto(section.get("duration", 20));
				boolean position = !section.getString("position", "player").equals("player");
				MathValue<Player> x = MathValue.auto(section.get("x", 0));
				MathValue<Player> y = MathValue.auto(section.get("y", 0));
				MathValue<Player> z = MathValue.auto(section.get("z", 0));
				MathValue<Player> yaw = MathValue.auto(section.get("yaw", 0));
				int range = section.getInt("range", 0);
				boolean opposite = section.getBoolean("opposite-yaw", false);
				boolean useItemDisplay = section.getBoolean("use-item-display", false);
				String finalItemID = itemID;
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					Player owner = context.holder();
					Location location = position ? requireNonNull(context.arg(ContextKeys.OTHER_LOCATION)).clone()
							: owner.getLocation().clone();
					location.add(x.evaluate(context), y.evaluate(context) - 1, z.evaluate(context));
					location.setPitch(0);
					if (opposite)
						location.setYaw(-owner.getLocation().getYaw());
					else
						location.setYaw((float) yaw.evaluate(context));
					FakeEntity fakeEntity;
					if (useItemDisplay && instance.getVersionManager().isVersionNewerThan1_19_4()) {
						location.add(0, 1.5, 0);
						FakeItemDisplay itemDisplay = instance.getVersionManager().getNMSManager()
								.createFakeItemDisplay(location);
						itemDisplay.item(instance.getItemManager().buildInternal(context, finalItemID));
						fakeEntity = itemDisplay;
					} else {
						FakeArmorStand armorStand = instance.getVersionManager().getNMSManager()
								.createFakeArmorStand(location);
						armorStand.invisible(true);
						armorStand.equipment(EquipmentSlot.HEAD,
								instance.getItemManager().buildInternal(context, finalItemID));
						fakeEntity = armorStand;
					}
					List<Player> viewers = new ArrayList<>();
					if (range > 0) {
						for (Player player : location.getWorld().getPlayers()) {
							if (LocationUtils.getDistance(player.getLocation(), location) <= range) {
								viewers.add(player);
							}
						}
					} else {
						viewers.add(owner);
					}
					for (Player player : viewers) {
						fakeEntity.spawn(player);
					}
					instance.getScheduler().asyncLater(() -> {
						for (Player player : viewers) {
							if (player.isOnline() && player.isValid()) {
								fakeEntity.destroy(player);
							}
						}
					}, (long) (duration.evaluate(context) * 50), TimeUnit.MILLISECONDS);
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at fake-item action which is expected to be `Section`");
				return Action.empty();
			}
		}), "fake-item");
	}

	private void registerHologramAction() {
		registerAction(((args, chance) -> {
			if (args instanceof Section section) {
				TextValue<Player> text = TextValue.auto(section.getString("text", ""));
				MathValue<Player> duration = MathValue.auto(section.get("duration", 20));
				boolean position = section.getString("position", "other").equals("other");
				MathValue<Player> x = MathValue.auto(section.get("x", 0));
				MathValue<Player> y = MathValue.auto(section.get("y", 0));
				MathValue<Player> z = MathValue.auto(section.get("z", 0));
				String rgbaStr = section.getString("rgba", "0,0,0,0");
				int[] rgba = new int[4];
				String[] split = rgbaStr.split(",");
				for (int i = 0; i < split.length; i++) {
					rgba[i] = Integer.parseInt(split[i]);
				}
				int range = section.getInt("range", 16);
				boolean useTextDisplay = section.getBoolean("use-text-display", false);
				return context -> {
					if (Math.random() > chance.evaluate(context))
						return;
					Player owner = context.holder();
					Location location = position ? requireNonNull(context.arg(ContextKeys.OTHER_LOCATION)).clone()
							: owner.getLocation().clone();
					location.add(x.evaluate(context), y.evaluate(context), z.evaluate(context));
					Set<Player> viewers = new HashSet<>();
					if (range > 0) {
						for (Player player : location.getWorld().getPlayers()) {
							if (LocationUtils.getDistance(player.getLocation(), location) <= range) {
								viewers.add(player);
							}
						}
					} else {
						viewers.add(owner);
					}
					instance.getHologramManager().createHologram(location,
							AdventureHelper.miniMessageToJson(text.render(context)), (int) duration.evaluate(context),
							useTextDisplay, rgba, viewers);
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at hologram action which is expected to be `Section`");
				return Action.empty();
			}
		}), "hologram");
	}

	private void registerFishFindAction() {
		registerAction((args, chance) -> {
			String surrounding;
			if (args instanceof Boolean b) {
				surrounding = b ? "lava" : "water";
			} else {
				surrounding = (String) args;
			}
			return context -> {
				if (Math.random() > chance.evaluate(context))
					return;
				String previous = context.arg(ContextKeys.SURROUNDING);
				context.arg(ContextKeys.SURROUNDING, surrounding);
				Collection<String> loots = instance.getLootManager()
						.getWeightedLoots(EffectInterface.newInstance(), context).keySet();
				StringJoiner stringJoiner = new StringJoiner(instance.getTranslationManager()
						.miniMessageTranslation(MessageConstants.COMMAND_FISH_FINDER_SPLIT_CHAR.build().key()));
				for (String loot : loots) {
					instance.getLootManager().getLoot(loot).ifPresent(lootIns -> {
						if (lootIns.showInFinder()) {
							if (!lootIns.nick().equals("UNDEFINED")) {
								stringJoiner.add(lootIns.nick());
							}
						}
					});
				}
				if (previous == null) {
					context.remove(ContextKeys.SURROUNDING);
				} else {
					context.arg(ContextKeys.SURROUNDING, previous);
				}
				if (loots.isEmpty()) {
					instance.getSenderFactory().wrap(context.holder()).sendMessage(instance.getTranslationManager()
							.render(MessageConstants.COMMAND_FISH_FINDER_NO_LOOT.build()));
				} else {
					instance.getSenderFactory().wrap(context.holder()).sendMessage(
							instance.getTranslationManager().render(MessageConstants.COMMAND_FISH_FINDER_POSSIBLE_LOOTS
									.arguments(AdventureHelper.miniMessage(stringJoiner.toString())).build()));
				}
			};
		}, "fish-finder");
	}
}