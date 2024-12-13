package com.swiftlicious.hellblock.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.addons.VaultHook;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.effects.EffectInterface;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.nms.toast.AdvancementType;
import com.swiftlicious.hellblock.utils.ListUtils;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.world.HellblockBlock;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

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

	protected void registerBuiltInActions() {
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
		this.registerTickAction();
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
	}

	private void registerPlayerCommandAction() {
		registerAction((args, chance) -> {
			List<String> commands = ListUtils.toList(args);
			return context -> {
				if (context.holder() == null)
					return;
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
						VersionHelper.getNMSManager().sendToast(player, itemStack,
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
	}

	private void registerSwingHandAction() {
		registerAction((args, chance) -> {
			boolean arg = (boolean) args;
			return context -> {
				if (context.holder() == null)
					return;
				if (Math.random() > chance.evaluate(context))
					return;
				EquipmentSlot slot = context.arg(ContextKeys.SLOT);
				if (slot == null) {
					VersionHelper.getNMSManager().swingHand(context.holder(), arg ? HandSlot.MAIN : HandSlot.OFF);
				} else {
					if (slot == EquipmentSlot.HAND)
						VersionHelper.getNMSManager().swingHand(context.holder(), HandSlot.MAIN);
					if (slot == EquipmentSlot.OFF_HAND)
						VersionHelper.getNMSManager().swingHand(context.holder(), HandSlot.OFF);
				}
			};
		}, "swing-hand");
	}

	private void registerFishFindAction() {
		registerAction((args, chance) -> {
			String surrounding = (String) args;
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

	private void registerTickAction() {
		registerAction((args, chance) -> context -> {
			if (context.holder() == null)
				return;
			if (Math.random() > chance.evaluate(context))
				return;
			Location location = Objects.requireNonNull(context.arg(ContextKeys.LOCATION));
			Pos3 pos3 = Pos3.from(location);
			Optional<HellblockWorld<?>> optionalWorld = instance.getWorldManager().getWorld(location.getWorld());
			optionalWorld.ifPresent(world -> world.getChunk(pos3.toChunkPos())
					.flatMap(chunk -> chunk.getBlockState(pos3)).ifPresent(state -> {
						HellblockBlock customBlock = state.type();
						customBlock.randomTick(state, world, pos3, false);
						customBlock.scheduledTick(state, world, pos3, false);
					}));
		}, "tick");
	}
}