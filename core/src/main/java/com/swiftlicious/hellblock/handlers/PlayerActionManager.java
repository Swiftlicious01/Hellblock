package com.swiftlicious.hellblock.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.addons.VaultHook;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.effects.EffectInterface;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.loot.LootType;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.nms.toast.AdvancementType;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.ListUtils;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

public class PlayerActionManager extends AbstractActionManager<Player> {

	public PlayerActionManager(HellblockPlugin plugin) {
		super(plugin);
	}

	@Override
	public void load() {
		loadExpansions(Player.class);
	}

	@Override
	public void registerBuiltInActions() {
		super.registerBuiltInActions();
		super.registerBundleAction(Player.class);
		this.registerMessageAction();
		this.registerToastAction();
		this.registerPlayerCommandAction();
		this.registerActionBarAction();
		this.registerCloseInvAction();
		this.registerExpAction();
		this.registerFoodAction();
		this.registerMoneyAction();
		this.registerItemAction();
		this.registerPotionAction();
		this.registerFishFindAction();
		this.registerPluginExpAction();
		this.registerSoundAction();
		this.registerTitleAction();
		this.registerSwingHandAction();
		this.registerInsertArgumentAction();
		this.registerDropRandomLootsAction();
	}

	private void registerMessageAction() {
		registerAction((args, chance) -> {
			final List<String> messages = ListUtils.toList(args);
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final Player player = context.holder();
				final List<String> replaced = instance.getPlaceholderManager().parse(player, messages,
						context.placeholderMap());
				final Sender audience = instance.getSenderFactory().wrap(player);
				replaced.forEach(text -> audience.sendMessage(AdventureHelper.miniMessageToComponent(text)));
			};
		}, "message");
		registerAction((args, chance) -> {
			final List<String> messages = ListUtils.toList(args);
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				String random = messages.get(RandomUtils.generateRandomInt(0, messages.size() - 1));
				final Player player = context.holder();
				random = instance.getPlaceholderManager().parse(player, random, context.placeholderMap());
				final Sender audience = instance.getSenderFactory().wrap(player);
				audience.sendMessage(AdventureHelper.miniMessageToComponent(random));
			};
		}, "random-message");
	}

	private void registerPlayerCommandAction() {
		registerAction((args, chance) -> {
			final List<String> commands = ListUtils.toList(args);
			return context -> {
				if (context.holder() == null) {
					return;
				}
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final List<String> replaced = instance.getPlaceholderManager().parse(context.holder(), commands,
						context.placeholderMap());
				instance.getScheduler().sync().run(
						() -> replaced.forEach(text -> context.holder().performCommand(text)),
						context.holder().getLocation());
			};
		}, "player-command");
	}

	private void registerCloseInvAction() {
		registerAction((args, chance) -> context -> {
			if (Math.random() > chance.evaluate(context)) {
				return;
			}
			context.holder().closeInventory();
		}, "close-inv");
	}

	private void registerToastAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				final String title = section.getString("title");
				final AdvancementType advancementType = AdvancementType
						.valueOf(Objects.requireNonNull(section.getString("advancement-type")));
				final String material = section.getString("icon");
				return context -> {
					if (Math.random() > chance.evaluate(context)) {
						return;
					}
					if (Material.getMaterial(material) == null || Material.getMaterial(material) == Material.AIR) {
						return;
					}
					final Player player = context.holder();
					final ItemStack itemStack = new ItemStack(Material.getMaterial(material));
					if (itemStack != null) {
						final String temp;
						temp = instance.getPlaceholderManager().parse(context.holder(), title,
								context.placeholderMap());
						VersionHelper.getNMSManager().sendToast(player, itemStack,
								AdventureHelper.componentToJson(AdventureHelper.miniMessageToComponent(temp)),
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

	private void registerInsertArgumentAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				final List<Pair<String, TextValue<Player>>> argList = new ArrayList<>();
				section.getStringRouteMappedValues(false).entrySet().forEach(
						entry -> argList.add(Pair.of(entry.getKey(), TextValue.auto(entry.getValue().toString()))));
				return context -> {
					if (Math.random() > chance.evaluate(context)) {
						return;
					}
					argList.forEach(pair -> context.arg(ContextKeys.of(pair.left(), String.class),
							pair.right().render(context)));
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at context-arg action which is expected to be `Section`");
				return Action.empty();
			}
		}, "context-arg");
	}

	private void registerDropRandomLootsAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				final boolean toInv = section.getBoolean("to-inventory");
				final MathValue<Player> count = MathValue.auto(section.get("amount"));
				final int extraAttempts = section.getInt("extra-attempts", 5);
				return context -> {
					if (Math.random() > chance.evaluate(context)) {
						return;
					}
					Effect effect = context.arg(ContextKeys.EFFECT);
					if (effect == null) {
						effect = EffectInterface.newInstance();
					}
					int triesTimes = 0;
					int successTimes = 0;
					final int requiredTimes = (int) count.evaluate(context);
					final Player player = context.holder();
					ItemStack rod = player.getInventory().getItemInMainHand();
					if (rod.getType() != Material.FISHING_ROD) {
						rod = player.getInventory().getItemInOffHand();
					}
					if (rod.getType() != Material.FISHING_ROD) {
						rod = new ItemStack(Material.FISHING_ROD);
					}
					final FishHook fishHook = context.arg(ContextKeys.HOOK_ENTITY);
					if (fishHook == null) {
						return;
					}

					while (successTimes < requiredTimes && triesTimes < requiredTimes + extraAttempts) {
						final Loot loot = instance.getLootManager().getNextLoot(effect, context);
						final Context<Player> newContext = Context.player(player).combine(context);
						if (loot != null && loot.type() == LootType.ITEM) {
							newContext.arg(ContextKeys.ID, loot.id());
							if (!toInv) {
								instance.getItemManager().dropItemLoot(newContext, rod, fishHook);
							} else {
								final ItemStack itemLoot = instance.getItemManager().getItemLoot(newContext, rod,
										fishHook);
								PlayerUtils.giveItem(player, itemLoot, itemLoot.getAmount());
							}
							successTimes++;
						}
						triesTimes++;
					}
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at drop-random-loots action which should be Section");
				return Action.empty();
			}
		}, "drop-random-loots");
	}

	private void registerActionBarAction() {
		registerAction((args, chance) -> {
			final String text = (String) args;
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final Player player = context.holder();
				final Component component = AdventureHelper.miniMessageToComponent(
						instance.getPlaceholderManager().parse(player, text, context.placeholderMap()));
				VersionHelper.getNMSManager().sendActionBar(player, AdventureHelper.componentToJson(component));
			};
		}, "actionbar");
		registerAction((args, chance) -> {
			final List<String> texts = ListUtils.toList(args);
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				String random = texts.get(RandomUtils.generateRandomInt(0, texts.size() - 1));
				final Player player = context.holder();
				random = instance.getPlaceholderManager().parse(player, random, context.placeholderMap());
				VersionHelper.getNMSManager().sendActionBar(player,
						AdventureHelper.componentToJson(AdventureHelper.miniMessageToComponent(random)));
			};
		}, "random-actionbar");
	}

	private void registerExpAction() {
		registerAction((args, chance) -> {
			final MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final Player player = context.holder();
				final ExperienceOrb entity = player.getLocation().getWorld()
						.spawn(player.getLocation().clone().add(0, 0.5, 0), ExperienceOrb.class);
				entity.setExperience((int) value.evaluate(context));
			};
		}, "mending");
		registerAction((args, chance) -> {
			final MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final Player player = context.holder();
				player.giveExp((int) Math.round(value.evaluate(context)));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.PLAYER, 1, 1));
			};
		}, "exp");
		registerAction((args, chance) -> {
			final MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final Player player = context.holder();
				player.setLevel((int) Math.max(0, player.getLevel() + value.evaluate(context)));
			};
		}, "level");
	}

	private void registerFoodAction() {
		registerAction((args, chance) -> {
			final MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final Player player = context.holder();
				player.setFoodLevel((int) (player.getFoodLevel() + value.evaluate(context)));
			};
		}, "food");
		registerAction((args, chance) -> {
			final MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final Player player = context.holder();
				player.setSaturation((float) (player.getSaturation() + value.evaluate(context)));
			};
		}, "saturation");
	}

	private void registerItemAction() {
		registerAction((args, chance) -> {
			final Boolean mainOrOff;
			final int amount;
			if (args instanceof Integer integer) {
				mainOrOff = null;
				amount = integer;
			} else if (args instanceof Section section) {
				final String hand = section.getString("hand");
				mainOrOff = hand == null ? null : "main".equalsIgnoreCase(hand);
				amount = section.getInt("amount", 1);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at item-amount action which is expected to be `Section`");
				return Action.empty();
			}
			return context -> {
				if (context.holder() == null) {
					return;
				}
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final Player player = context.holder();
				final EquipmentSlot hand = context.arg(ContextKeys.SLOT);
				if (mainOrOff == null && hand == null) {
					return;
				}
				final boolean tempHand = Objects.requireNonNullElseGet(mainOrOff, () -> hand == EquipmentSlot.HAND);
				final ItemStack itemStack = tempHand ? player.getInventory().getItemInMainHand()
						: player.getInventory().getItemInOffHand();
				if (amount < 0) {
					itemStack.setAmount(Math.max(0, itemStack.getAmount() + amount));
				} else if (amount > 0) {
					PlayerUtils.giveItem(player, itemStack, amount);
				}
			};
		}, "item-amount");
		registerAction((args, chance) -> {
			final int amount;
			final EquipmentSlot slot;
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
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final Player player = context.holder();
				if (player == null) {
					return;
				}
				EquipmentSlot tempSlot = slot;
				final EquipmentSlot equipmentSlot = context.arg(ContextKeys.SLOT);
				if (tempSlot == null && equipmentSlot != null) {
					tempSlot = equipmentSlot;
				}
				if (tempSlot == null) {
					return;
				}
				final ItemStack itemStack = player.getInventory().getItem(tempSlot);
				if (itemStack.getType() == Material.AIR || itemStack.getAmount() == 0) {
					return;
				}
				if (itemStack.getItemMeta() == null) {
					return;
				}
				if (amount > 0) {
					instance.getItemManager().decreaseDamage(context.holder(), itemStack, amount);
				} else {
					instance.getItemManager().increaseDamage(context.holder(), itemStack, -amount, true);
				}
			};
		}, "durability");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				final String material = section.getString("material");
				final String displayName = section.getString("display.name");
				final List<String> displayLore = section.getStringList("display.lore");
				final Map<com.swiftlicious.hellblock.utils.extras.Key, Short> enchantments;
				if (section.contains("enchantments")) {
					enchantments = instance.getConfigManager().getEnchantments(section.getSection("enchantments"));
				} else {
					enchantments = Collections.emptyMap();
				}
				final boolean unbreakable = section.getBoolean("unbreakable", false);
				final int damage = section.getInt("damage");
				final int amount = section.getInt("amount", 1);
				final boolean toInventory = section.getBoolean("to-inventory", false);
				return context -> {
					if (Math.random() > chance.evaluate(context)) {
						return;
					}
					if (Material.getMaterial(material) == null || Material.getMaterial(material) == Material.AIR) {
						return;
					}
					final Player player = context.holder();
					final ItemStack itemStack = new ItemStack(Material.getMaterial(material), amount);
					if (itemStack != null) {
						final Item<ItemStack> item = instance.getItemManager().wrap(itemStack);
						if (displayName != null && !displayName.isEmpty()) {
							final TextValue<Player> name = TextValue.auto("<!i><white>" + displayName);
							item.displayName(AdventureHelper.miniMessageToJson(name.render(context)));
						}
						if (displayLore != null && !displayLore.isEmpty()) {
							final List<TextValue<Player>> lore = new ArrayList<>();
							displayLore.forEach(text -> lore.add(TextValue.auto("<!i><white>" + text)));
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
						final int maxStack = itemStack.getMaxStackSize();
						int amountToGive = amount;
						while (amountToGive > 0) {
							final int perStackSize = Math.min(maxStack, amountToGive);
							amountToGive -= perStackSize;
							final ItemStack more = item.load().clone();
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
				final TextValue<Player> id = TextValue.auto(section.getString("item"));
				final MathValue<Player> amount = MathValue.auto(section.get("amount", 1));
				final boolean toInventory = section.getBoolean("to-inventory", false);
				return context -> {
					if (Math.random() > chance.evaluate(context)) {
						return;
					}
					final Player player = context.holder();
					final ItemStack itemStack = instance.getItemManager().buildAny(context, id.render(context));
					if (itemStack != null) {
						final int maxStack = itemStack.getMaxStackSize();
						int amountToGive = (int) amount.evaluate(context);
						while (amountToGive > 0) {
							final int perStackSize = Math.min(maxStack, amountToGive);
							amountToGive -= perStackSize;
							final ItemStack more = itemStack.clone();
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

	private void registerMoneyAction() {
		registerAction((args, chance) -> {
			final MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				if (!VaultHook.isHooked()) {
					return;
				}
				VaultHook.deposit(context.holder(), value.evaluate(context));
			};
		}, "give-money");
		registerAction((args, chance) -> {
			final MathValue<Player> value = MathValue.auto(args);
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				if (!VaultHook.isHooked()) {
					return;
				}
				VaultHook.withdraw(context.holder(), value.evaluate(context));
			};
		}, "take-money");
	}

	private void registerPotionAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				PotionEffectType potionEffectType = PotionEffectResolver
						.resolve(section.getString("type", "BLINDNESS"));
				final PotionEffect potionEffect = new PotionEffect(potionEffectType, section.getInt("duration", 20),
						section.getInt("amplifier", 0));
				return context -> {
					if (Math.random() > chance.evaluate(context)) {
						return;
					}
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
				final MathValue<Player> volume = MathValue.auto(section.get("volume", 1));
				final MathValue<Player> pitch = MathValue.auto(section.get("pitch", 1));
				final Key key = Key.key(section.getString("key"));
				final Sound.Source source = Sound.Source
						.valueOf(section.getString("source", "PLAYER").toUpperCase(Locale.ENGLISH));
				return context -> {
					if (Math.random() > chance.evaluate(context)) {
						return;
					}
					final Player player = context.holder();
					final Audience audience = instance.getSenderFactory().getAudience(player);
					final Sound sound = Sound.sound(key, source, (float) volume.evaluate(context),
							(float) pitch.evaluate(context));
					AdventureHelper.playSound(audience, sound);
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at sound action which is expected to be `Section`");
				return Action.empty();
			}
		}, "sound");
	}

	private void registerPluginExpAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				final String pluginName = section.getString("plugin");
				final MathValue<Player> value = MathValue.auto(section.get("exp"));
				final String target = section.getString("target");
				return context -> {
					if (Math.random() > chance.evaluate(context)) {
						return;
					}
					Optional.ofNullable(instance.getIntegrationManager().getLevelerProvider(pluginName))
							.ifPresentOrElse(it -> it.addXp(context.holder(), target, value.evaluate(context)),
									() -> instance.getPluginLogger().warn("Plugin (" + pluginName
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
				final TextValue<Player> title = TextValue.auto(section.getString("title", ""));
				final TextValue<Player> subtitle = TextValue.auto(section.getString("subtitle", ""));
				final int fadeIn = section.getInt("fade-in", 20);
				final int stay = section.getInt("stay", 30);
				final int fadeOut = section.getInt("fade-out", 10);
				return context -> {
					if (Math.random() > chance.evaluate(context)) {
						return;
					}
					final Player player = context.holder();
					VersionHelper.getNMSManager().sendTitle(player,
							AdventureHelper
									.componentToJson(AdventureHelper.miniMessageToComponent(title.render(context))),
							AdventureHelper
									.componentToJson(AdventureHelper.miniMessageToComponent(subtitle.render(context))),
							fadeIn, stay, fadeOut);
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at title action which is expected to be `Section`");
				return Action.empty();
			}
		}, "title");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				final List<String> titles = section.getStringList("titles");
				if (titles.isEmpty()) {
					titles.add("");
				}
				final List<String> subtitles = section.getStringList("subtitles");
				if (subtitles.isEmpty()) {
					subtitles.add("");
				}
				final int fadeIn = section.getInt("fade-in", 20);
				final int stay = section.getInt("stay", 30);
				final int fadeOut = section.getInt("fade-out", 10);
				return context -> {
					if (Math.random() > chance.evaluate(context)) {
						return;
					}
					final TextValue<Player> title = TextValue
							.auto(titles.get(RandomUtils.generateRandomInt(0, titles.size() - 1)));
					final TextValue<Player> subtitle = TextValue
							.auto(subtitles.get(RandomUtils.generateRandomInt(0, subtitles.size() - 1)));
					final Player player = context.holder();
					VersionHelper.getNMSManager().sendTitle(player,
							AdventureHelper
									.componentToJson(AdventureHelper.miniMessageToComponent(title.render(context))),
							AdventureHelper
									.componentToJson(AdventureHelper.miniMessageToComponent(subtitle.render(context))),
							fadeIn, stay, fadeOut);
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at random-title action which is expected to be `Section`");
				return Action.empty();
			}
		}, "random-title");
	}

	private void registerSwingHandAction() {
		registerAction((args, chance) -> {
			final boolean arg = (boolean) args;
			return context -> {
				if (context.holder() == null) {
					return;
				}
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final EquipmentSlot slot = context.arg(ContextKeys.SLOT);
				if (slot == null) {
					VersionHelper.getNMSManager().swingHand(context.holder(), arg ? HandSlot.MAIN : HandSlot.OFF);
				} else {
					if (slot == EquipmentSlot.HAND) {
						VersionHelper.getNMSManager().swingHand(context.holder(), HandSlot.MAIN);
					}
					if (slot == EquipmentSlot.OFF_HAND) {
						VersionHelper.getNMSManager().swingHand(context.holder(), HandSlot.OFF);
					}
				}
			};
		}, "swing-hand");
	}

	private void registerFishFindAction() {
		registerAction((args, chance) -> {
			final String surrounding = (String) args;
			return context -> {
				if (Math.random() > chance.evaluate(context)) {
					return;
				}
				final String previous = context.arg(ContextKeys.SURROUNDING);
				context.arg(ContextKeys.SURROUNDING, surrounding);
				final Collection<String> loots = instance.getLootManager()
						.getWeightedLoots(EffectInterface.newInstance(), context).keySet();
				final StringJoiner stringJoiner = new StringJoiner(instance.getTranslationManager()
						.miniMessageTranslation(MessageConstants.COMMAND_FISH_FINDER_SPLIT_CHAR.build().key()));
				loots.forEach(loot -> instance.getLootManager().getLoot(loot).ifPresent(lootIns -> {
					if (lootIns.showInFinder()) {
						if (!"UNDEFINED".equals(lootIns.nick())) {
							stringJoiner.add(lootIns.nick());
						}
					}
				}));
				if (previous == null) {
					context.remove(ContextKeys.SURROUNDING);
				} else {
					context.arg(ContextKeys.SURROUNDING, previous);
				}
				final Sender audience = instance.getSenderFactory().wrap(context.holder());
				if (loots.isEmpty()) {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.COMMAND_FISH_FINDER_NO_LOOT.build()));
				} else {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.COMMAND_FISH_FINDER_POSSIBLE_LOOTS
									.arguments(AdventureHelper.miniMessageToComponent(stringJoiner.toString()))
									.build()));
				}
			};
		}, "fish-finder");
	}
}