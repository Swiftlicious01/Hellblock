package com.swiftlicious.hellblock.handlers;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.addons.IntegrationManager;
import com.swiftlicious.hellblock.creation.addons.VaultHook;
import com.swiftlicious.hellblock.creation.addons.bedrock.FloodGateUtils;
import com.swiftlicious.hellblock.creation.addons.bedrock.GeyserUtils;
import com.swiftlicious.hellblock.creation.addons.level.LevelerProvider;
import com.swiftlicious.hellblock.effects.EffectProperties;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.utils.ListUtils;
import com.swiftlicious.hellblock.utils.MiscUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class PlayerRequirementManager extends AbstractRequirementManager<Player> {

	public PlayerRequirementManager(HellblockPlugin plugin) {
		super(plugin, Player.class);
	}

	@Override
	protected void registerBuiltInRequirements() {
		super.registerBuiltInRequirements();
		this.registerEquipmentRequirement();
		this.registerRodRequirement();
		this.registerHookRequirement();
		this.registerBaitRequirement();
		this.registerGroupRequirement();
		this.registerLootRequirement();
		this.registerLootTypeRequirement();
		this.registerSizeRequirement();
		this.registerHasStatsRequirement();
		this.registerInLavaRequirement();
		this.registerItemInHandRequirement();
		this.registerPermissionRequirement();
		this.registerPluginLevelRequirement();
		this.registerCoolDownRequirement();
		this.registerLevelRequirement();
		this.registerMoneyRequirement();
		this.registerPotionEffectRequirement();
		this.registerSneakRequirement();
		this.registerGameModeRequirement();
		this.registerIsFirstLootRequirement();
		this.registerHasPlayerLootRequirement();
		this.registerLootOrderRequirement();
		this.registerIsBedrockPlayerRequirement();
		this.registerIsNewSizeRecordRequirement();
	}

	@Override
	public void load() {
		loadExpansions(Player.class);
	}

	private void registerEquipmentRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				EquipmentSlot slot = EquipmentSlot
						.valueOf(section.getString("slot", "HEAD").toUpperCase(Locale.ENGLISH));
				List<String> items = ListUtils.toList(section.get("item"));
				return context -> {
					ItemStack itemStack = context.holder().getInventory().getItem(slot);
					String id = instance.getItemManager().getItemID(itemStack);
					if (items.contains(id))
						return true;
					if (runActions)
						ActionManager.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at equipment requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "equipment");
	}

	private void registerItemInHandRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				boolean regex = section.getBoolean("regex", false);
				String hand = section.getString("hand", "main");
				int mode;
				if (hand.equalsIgnoreCase("main")) {
					mode = 1;
				} else if (hand.equalsIgnoreCase("off")) {
					mode = 2;
				} else if (hand.equalsIgnoreCase("other")) {
					mode = 3;
				} else {
					mode = 0;
					instance.getPluginLogger()
							.warn("Invalid hand argument: " + hand + " which is expected to be main/off/other");
					return Requirement.empty();
				}
				int amount = section.getInt("amount", 0);
				List<String> items = ListUtils.toList(section.get("item"));
				boolean any = items.contains("any") || items.contains("*");
				return context -> {
					Player player = context.holder();
					if (player == null)
						return true;
					EquipmentSlot slot = context.arg(ContextKeys.SLOT);
					ItemStack itemStack;
					if (slot == null) {
						itemStack = mode == 1 ? player.getInventory().getItemInMainHand()
								: player.getInventory().getItemInOffHand();
					} else {
						if (mode == 3) {
							itemStack = player.getInventory()
									.getItem(slot == EquipmentSlot.HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
						} else {
							itemStack = player.getInventory().getItem(slot);
						}
					}
					String id = instance.getItemManager().getItemID(itemStack);
					if (!regex) {
						if ((any || items.contains(id)) && itemStack.getAmount() >= amount)
							return true;
					} else {
						for (String itemRegex : items) {
							if (id.matches(itemRegex) && itemStack.getAmount() >= amount) {
								return true;
							}
						}
					}
					if (runActions)
						ActionManager.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at item-in-hand requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "item-in-hand");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				boolean mainOrOff = section.getString("hand", "main").equalsIgnoreCase("main");
				int amount = section.getInt("amount", 1);
				List<String> items = ListUtils.toList(section.get("item"));
				return context -> {
					ItemStack itemStack = mainOrOff ? context.holder().getInventory().getItemInMainHand()
							: context.holder().getInventory().getItemInOffHand();
					String id = instance.getItemManager().getItemID(itemStack);
					if (!items.contains(id) || itemStack.getAmount() < amount) {
						return true;
					}
					if (runActions)
						ActionManager.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at !item-in-hand requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "!item-in-hand");
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				boolean regex = section.getBoolean("regex", false);
				String hand = section.getString("hand", "main");
				int mode;
				if (hand.equalsIgnoreCase("main")) {
					mode = 1;
				} else if (hand.equalsIgnoreCase("off")) {
					mode = 2;
				} else if (hand.equalsIgnoreCase("other")) {
					mode = 3;
				} else {
					mode = 0;
					instance.getPluginLogger()
							.warn("Invalid hand argument: " + hand + " which is expected to be main/off/other");
					return Requirement.empty();
				}
				int amount = section.getInt("amount", 0);
				List<String> items = ListUtils.toList(section.get("material"));
				boolean any = items.contains("any") || items.contains("*");
				return context -> {
					Player player = context.holder();
					if (player == null)
						return true;
					EquipmentSlot slot = context.arg(ContextKeys.SLOT);
					ItemStack itemStack;
					if (slot == null) {
						itemStack = mode == 1 ? player.getInventory().getItemInMainHand()
								: player.getInventory().getItemInOffHand();
					} else {
						if (mode == 3) {
							itemStack = player.getInventory()
									.getItem(slot == EquipmentSlot.HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
						} else {
							itemStack = player.getInventory().getItem(slot);
						}
					}
					String id = itemStack.getType().name();
					if (!regex) {
						if ((any || items.contains(id)) && itemStack.getAmount() >= amount)
							return true;
					} else {
						for (String itemRegex : items) {
							if (id.matches(itemRegex) && itemStack.getAmount() >= amount) {
								return true;
							}
						}
					}
					if (runActions)
						ActionManager.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at material-in-hand requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "material-in-hand");
	}

	private void registerPluginLevelRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				String pluginName = section.getString("plugin");
				int level = section.getInt("level");
				String target = section.getString("target");
				return context -> {
					LevelerProvider levelerProvider = instance.getIntegrationManager().getLevelerProvider(pluginName);
					if (levelerProvider == null) {
						instance.getPluginLogger().warn("Plugin (" + pluginName
								+ "'s) level is not compatible. Please double check if it's a problem caused by pronunciation.");
						return true;
					}
					if (levelerProvider.getLevel(context.holder(), target) >= level)
						return true;
					if (runActions)
						ActionManager.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at plugin-level requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "plugin-level");
	}

	private void registerIsBedrockPlayerRequirement() {
		registerRequirement(((args, actions, runActions) -> context -> {
			boolean arg = (boolean) args;
			if (IntegrationManager.getInstance().hasGeyser()) {
				boolean is = GeyserUtils.isBedrockPlayer(context.holder().getUniqueId());
				if (is && arg) {
					return true;
				}
				if (!is && !arg) {
					return true;
				}
			}
			if (IntegrationManager.getInstance().hasFloodGate()) {
				boolean is = FloodGateUtils.isBedrockPlayer(context.holder().getUniqueId());
				if (is && arg) {
					return true;
				}
				if (!is && !arg) {
					return true;
				}
			}
			if (!IntegrationManager.getInstance().hasFloodGate() && !IntegrationManager.getInstance().hasGeyser()
					&& !arg) {
				return true;
			}
			if (runActions)
				ActionManager.trigger(context, actions);
			return false;
		}), "is-bedrock-player");
	}

	private void registerIsNewSizeRecordRequirement() {
		registerRequirement((args, actions, runActions) -> {
			boolean is = (boolean) args;
			return context -> {
				boolean current = Optional.ofNullable(context.arg(ContextKeys.IS_NEW_SIZE_RECORD)).orElse(false);
				if (is == current)
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "new-size-record");
	}

	private void registerInLavaRequirement() {
		registerRequirement((args, actions, runActions) -> {
			boolean inLava = (boolean) args;
			return context -> {
				boolean in_lava = Optional.ofNullable(context.arg(ContextKeys.SURROUNDING)).orElse("")
						.equals(EffectProperties.LAVA_FISHING.key());
				if (in_lava == inLava)
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "in-lava");
	}

	private void registerRodRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> rods = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.ROD);
				if (rods.contains(id))
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "rod");
		registerRequirement((args, actions, runActions) -> {
			Set<String> rods = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.ROD);
				if (!rods.contains(id))
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "!rod");
	}

	private void registerGroupRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> groups = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String lootID = context.arg(ContextKeys.ID);
				Optional<Loot> loot = instance.getLootManager().getLoot(lootID);
				if (loot.isEmpty())
					return false;
				String[] group = loot.get().lootGroup();
				if (group != null)
					for (String x : group)
						if (groups.contains(x))
							return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "group");
		registerRequirement((args, actions, runActions) -> {
			Set<String> groups = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String lootID = context.arg(ContextKeys.ID);
				Optional<Loot> loot = instance.getLootManager().getLoot(lootID);
				if (loot.isEmpty())
					return false;
				String[] group = loot.get().lootGroup();
				if (group == null)
					return true;
				outer: {
					for (String x : group)
						if (groups.contains(x))
							break outer;
					return true;
				}
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "!group");
	}

	private void registerLootRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> arg = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String lootID = context.arg(ContextKeys.ID);
				if (arg.contains(lootID))
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "loot");
		registerRequirement((args, actions, runActions) -> {
			Set<String> arg = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String lootID = context.arg(ContextKeys.ID);
				if (!arg.contains(lootID))
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "!loot");
	}

	private void registerHookRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> hooks = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.HOOK);
				if (hooks.contains(id))
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "hook");
		registerRequirement((args, actions, runActions) -> {
			Set<String> hooks = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.HOOK);
				if (!hooks.contains(id))
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "!hook");
		registerRequirement((args, actions, runActions) -> {
			boolean has = (boolean) args;
			return context -> {
				String id = context.arg(ContextKeys.HOOK);
				if (id != null && has)
					return true;
				if (id == null && !has)
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "has-hook");
	}

	private void registerBaitRequirement() {
		registerRequirement((args, actions, runActions) -> {
			Set<String> arg = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.BAIT);
				if (arg.contains(id))
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "bait");
		registerRequirement((args, actions, runActions) -> {
			Set<String> arg = new HashSet<>(ListUtils.toList(args));
			return context -> {
				String id = context.arg(ContextKeys.BAIT);
				if (!arg.contains(id))
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "!bait");
		registerRequirement((args, actions, runActions) -> {
			boolean has = (boolean) args;
			return context -> {
				String id = context.arg(ContextKeys.BAIT);
				if (id != null && has)
					return true;
				if (id == null && !has)
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "has-bait");
	}

	private void registerSizeRequirement() {
		registerRequirement((args, actions, runActions) -> {
			boolean has = (boolean) args;
			return context -> {
				float size = Optional.ofNullable(context.arg(ContextKeys.SIZE)).orElse(-1f);
				if (size != -1 && has)
					return true;
				if (size == -1 && !has)
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "has-size");
	}

	private void registerHasStatsRequirement() {
		registerRequirement((args, actions, runActions) -> {
			boolean has = (boolean) args;
			return context -> {
				String loot = context.arg(ContextKeys.ID);
				Optional<Loot> lootInstance = instance.getLootManager().getLoot(loot);
				if (lootInstance.isPresent()) {
					if (!lootInstance.get().disableStats() && has)
						return true;
					if (lootInstance.get().disableStats() && !has)
						return true;
				}
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "has-stats");
	}

	private void registerLootTypeRequirement() {
		registerRequirement((args, actions, runActions) -> {
			List<String> types = ListUtils.toList(args);
			return context -> {
				String loot = context.arg(ContextKeys.ID);
				Optional<Loot> lootInstance = instance.getLootManager().getLoot(loot);
				if (lootInstance.isPresent()) {
					if (types.contains(lootInstance.get().type().name().toLowerCase(Locale.ENGLISH)))
						return true;
				}
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "loot-type");
		registerRequirement((args, actions, runActions) -> {
			List<String> types = ListUtils.toList(args);
			return context -> {
				String loot = context.arg(ContextKeys.ID);
				Optional<Loot> lootInstance = instance.getLootManager().getLoot(loot);
				if (lootInstance.isPresent()) {
					if (!types.contains(lootInstance.get().type().name().toLowerCase(Locale.ENGLISH)))
						return true;
				}
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "!loot-type");
	}

	private void registerLevelRequirement() {
		registerRequirement((args, actions, runActions) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				int current = context.holder().getLevel();
				if (current >= value.evaluate(context, true))
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "level");
	}

	private void registerMoneyRequirement() {
		registerRequirement((args, actions, runActions) -> {
			MathValue<Player> value = MathValue.auto(args);
			return context -> {
				double current = VaultHook.getBalance(context.holder());
				if (current >= value.evaluate(context, true))
					return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "money");
	}

	private void registerCoolDownRequirement() {
		registerRequirement((args, actions, runActions) -> {
			if (args instanceof Section section) {
				String key = section.getString("key");
				int time = section.getInt("time");
				return context -> {
					if (!instance.getCooldownManager().isCoolDown(context.holder().getUniqueId(), key, time))
						return true;
					if (runActions)
						ActionManager.trigger(context, actions);
					return false;
				};
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at cooldown requirement which is expected be `Section`");
				return Requirement.empty();
			}
		}, "cooldown");
	}

	private void registerPermissionRequirement() {
		registerRequirement((args, actions, runActions) -> {
			List<String> perms = ListUtils.toList(args);
			return context -> {
				for (String perm : perms)
					if (context.holder().hasPermission(perm))
						return true;
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "permission");
		registerRequirement((args, actions, runActions) -> {
			List<String> perms = ListUtils.toList(args);
			return context -> {
				for (String perm : perms)
					if (context.holder().hasPermission(perm)) {
						if (runActions)
							ActionManager.trigger(context, actions);
						return false;
					}
				return true;
			};
		}, "!permission");
	}

	@SuppressWarnings("deprecation")
	private void registerPotionEffectRequirement() {
		registerRequirement((args, actions, runActions) -> {
			String potions = (String) args;
			String[] split = potions.split("(<=|>=|<|>|==)", 2);
			PotionEffectType type = PotionEffectType.getByName(split[0]);
			if (type == null) {
				instance.getPluginLogger().warn("Potion effect doesn't exist: " + split[0]);
				return Requirement.empty();
			}
			int required = Integer.parseInt(split[1]);
			String operator = potions.substring(split[0].length(), potions.length() - split[1].length());
			return context -> {
				int level = -1;
				PotionEffect potionEffect = context.holder().getPotionEffect(type);
				if (potionEffect != null) {
					level = potionEffect.getAmplifier();
				}
				boolean result = false;
				switch (operator) {
				case ">=" -> {
					if (level >= required)
						result = true;
				}
				case ">" -> {
					if (level > required)
						result = true;
				}
				case "=", "==" -> {
					if (level == required)
						result = true;
				}
				case "!=" -> {
					if (level != required)
						result = true;
				}
				case "<=" -> {
					if (level <= required)
						result = true;
				}
				case "<" -> {
					if (level < required)
						result = true;
				}
				}
				if (result) {
					return true;
				}
				if (runActions)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "potion-effect");
	}

	private void registerSneakRequirement() {
		registerRequirement((args, actions, advanced) -> {
			boolean sneak = (boolean) args;
			return context -> {
				if (context.holder() == null)
					return false;
				if (sneak) {
					if (context.holder().isSneaking())
						return true;
				} else {
					if (!context.holder().isSneaking())
						return true;
				}
				if (advanced)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "sneak");
	}

	protected void registerGameModeRequirement() {
		registerRequirement((args, actions, advanced) -> {
			List<String> modes = ListUtils.toList(args);
			return context -> {
				if (context.holder() == null)
					return false;
				var name = context.holder().getGameMode().name().toLowerCase(Locale.ENGLISH);
				if (modes.contains(name)) {
					return true;
				}
				if (advanced)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "gamemode");
	}

	protected void registerIsFirstLootRequirement() {
		registerRequirement((args, actions, advanced) -> {
			boolean is = (boolean) args;
			return context -> {
				int order = Optional.ofNullable(context.arg(ContextKeys.LOOT_ORDER)).orElse(1);
				if (is && order == 1)
					return true;
				if (!is && order != 1)
					return true;
				if (advanced)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "is-first-loot");
	}

	protected void registerLootOrderRequirement() {
		registerRequirement((args, actions, advanced) -> {
			int order = MiscUtils.getAsInt(args);
			return context -> {
				int actualOrder = Optional.ofNullable(context.arg(ContextKeys.LOOT_ORDER)).orElse(1);
				if (order == actualOrder)
					return true;
				if (advanced)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "loot-order");
	}

	protected void registerHasPlayerLootRequirement() {
		registerRequirement((args, actions, advanced) -> {
			boolean has = (boolean) args;
			return context -> {
				if (has && context.holder() != null)
					return true;
				if (!has && context.holder() == null)
					return true;
				if (advanced)
					ActionManager.trigger(context, actions);
				return false;
			};
		}, "has-player");
	}
}