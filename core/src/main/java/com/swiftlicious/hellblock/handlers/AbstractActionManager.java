package com.swiftlicious.hellblock.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.builtin.ActionActionbarNearby;
import com.swiftlicious.hellblock.handlers.builtin.ActionBroadcast;
import com.swiftlicious.hellblock.handlers.builtin.ActionChain;
import com.swiftlicious.hellblock.handlers.builtin.ActionCommand;
import com.swiftlicious.hellblock.handlers.builtin.ActionCommandNearby;
import com.swiftlicious.hellblock.handlers.builtin.ActionConditional;
import com.swiftlicious.hellblock.handlers.builtin.ActionDelay;
import com.swiftlicious.hellblock.handlers.builtin.ActionDropItem;
import com.swiftlicious.hellblock.handlers.builtin.ActionFakeItem;
import com.swiftlicious.hellblock.handlers.builtin.ActionHologram;
import com.swiftlicious.hellblock.handlers.builtin.ActionMessageNearby;
import com.swiftlicious.hellblock.handlers.builtin.ActionParticle;
import com.swiftlicious.hellblock.handlers.builtin.ActionPriority;
import com.swiftlicious.hellblock.handlers.builtin.ActionRandomCommand;
import com.swiftlicious.hellblock.handlers.builtin.ActionSpawnEntity;
import com.swiftlicious.hellblock.handlers.builtin.ActionTimer;
import com.swiftlicious.hellblock.handlers.builtin.ActionTitleNearby;
import com.swiftlicious.hellblock.utils.ClassUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.factory.ActionExpansion;
import com.swiftlicious.hellblock.utils.factory.ActionFactory;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public abstract class AbstractActionManager<T> implements ActionManager<T> {

	protected final HellblockPlugin instance;
	private final Map<String, ActionFactory<T>> actionFactoryMap = new HashMap<>();
	private static final String EXPANSION_FOLDER = "expansions/action";

	public AbstractActionManager(HellblockPlugin plugin) {
		this.instance = plugin;
		this.registerBuiltInActions();
	}

	protected void registerBuiltInActions() {
		this.registerCommandAction();
		this.registerBroadcastAction();
		this.registerNearbyMessage();
		this.registerNearbyActionBar();
		this.registerNearbyTitle();
		this.registerParticleAction();
		this.registerDropItemsAction();
		this.registerFakeItemAction();
		this.registerHologramAction();
		this.registerSpawnEntityAction();
	}

	@Override
	public boolean registerAction(ActionFactory<T> actionFactory, String... types) {
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

	@Override
	public boolean hasAction(@NotNull String type) {
		return actionFactoryMap.containsKey(type);
	}

	@Nullable
	@Override
	public ActionFactory<T> getActionFactory(@NotNull String type) {
		return actionFactoryMap.get(type);
	}

	@Override
	public Action<T> parseAction(Section section) {
		if (section == null)
			return Action.empty();
		ActionFactory<T> factory = getActionFactory(section.getString("type"));
		if (factory == null) {
			instance.getPluginLogger().warn("Action type: " + section.getString("type") + " doesn't exist.");
			return Action.empty();
		}
		return factory.process(section.get("value"),
				section.contains("chance") ? MathValue.auto(section.get("chance")) : MathValue.plain(1d));
	}

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public Action<T>[] parseActions(Section section) {
		ArrayList<Action<T>> actionList = new ArrayList<>();
		if (section != null)
			for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					Action<T> action = parseAction(innerSection);
					if (action != null)
						actionList.add(action);
				}
			}
		return actionList.toArray(new Action[0]);
	}

	@Override
	public Action<T> parseAction(@NotNull String type, @NotNull Object args) {
		ActionFactory<T> factory = getActionFactory(type);
		if (factory == null) {
			instance.getPluginLogger().warn("Action type: " + type + " doesn't exist.");
			return Action.empty();
		}
		return factory.process(args, MathValue.plain(1));
	}

	@SuppressWarnings({ "unchecked" })
	protected void loadExpansions(Class<T> tClass) {
		File expansionFolder = new File(instance.getDataFolder(), EXPANSION_FOLDER);
		if (!expansionFolder.exists())
			expansionFolder.mkdirs();

		List<Class<? extends ActionExpansion<T>>> classes = new ArrayList<>();
		File[] expansionJars = expansionFolder.listFiles();
		if (expansionJars == null)
			return;
		for (File expansionJar : expansionJars) {
			if (expansionJar.getName().endsWith(".jar")) {
				try {
					Class<? extends ActionExpansion<T>> expansionClass = (Class<? extends ActionExpansion<T>>) ClassUtils
							.findClass(expansionJar, ActionExpansion.class, tClass);
					classes.add(expansionClass);
				} catch (IOException | ClassNotFoundException e) {
					instance.getPluginLogger().warn("Failed to load expansion: " + expansionJar.getName(), e);
				}
			}
		}
		try {
			for (Class<? extends ActionExpansion<T>> expansionClass : classes) {
				ActionExpansion<T> expansion = expansionClass.getDeclaredConstructor().newInstance();
				unregisterAction(expansion.getActionType());
				registerAction(expansion.getActionFactory(), expansion.getActionType());
				instance.getPluginLogger().info("Loaded action expansion: " + expansion.getActionType() + "["
						+ expansion.getVersion() + "]" + " by " + expansion.getAuthor());
			}
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException
				| NoSuchMethodException e) {
			instance.getPluginLogger().warn("Error occurred when creating expansion instance.", e);
		}
	}

	protected void registerBroadcastAction() {
		registerAction((args, chance) -> new ActionBroadcast<>(instance, args, chance), "broadcast");
	}

	protected void registerNearbyMessage() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				return new ActionMessageNearby<>(instance, section, chance);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at message-nearby action which should be Section");
				return Action.empty();
			}
		}, "message-nearby");
	}

	protected void registerNearbyActionBar() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				return new ActionActionbarNearby<>(instance, section, chance);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at actionbar-nearby action which should be Section");
				return Action.empty();
			}
		}, "actionbar-nearby");
	}

	protected void registerCommandAction() {
		registerAction((args, chance) -> new ActionCommand<>(instance, args, chance), "command");
		registerAction((args, chance) -> new ActionRandomCommand<>(instance, args, chance), "random-command");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				return new ActionCommandNearby<>(instance, section, chance);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at command-nearby action which should be Section");
				return Action.empty();
			}
		}, "command-nearby");
	}

	protected void registerBundleAction(Class<T> tClass) {
		registerAction((args, chance) -> new ActionChain<>(instance, this, args, chance), "chain");
		registerAction((args, chance) -> new ActionDelay<>(instance, this, args, chance), "delay");
		registerAction((args, chance) -> new ActionTimer<>(instance, this, args, chance), "timer");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				return new ActionConditional<>(instance, this, tClass, section, chance);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at conditional action which is expected to be `Section`");
				return Action.empty();
			}
		}, "conditional");
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				return new ActionPriority<>(instance, this, tClass, section, chance);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at priority action which is expected to be `Section`");
				return Action.empty();
			}
		}, "priority");
	}

	protected void registerNearbyTitle() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				return new ActionTitleNearby<>(instance, section, chance);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at title-nearby action which is expected to be `Section`");
				return Action.empty();
			}
		}, "title-nearby");
	}

	protected void registerParticleAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				return new ActionParticle<>(instance, section, chance);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at particle action which is expected to be `Section`");
				return Action.empty();
			}
		}, "particle");
	}

	protected void registerDropItemsAction() {
		registerAction((args, chance) -> {
			if (args instanceof Section section) {
				return new ActionDropItem<>(instance, section, chance);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at drop-item action which is expected to be `Section`");
				return Action.empty();
			}
		}, "drop-item");
	}

	protected void registerHologramAction() {
		registerAction(((args, chance) -> {
			if (args instanceof Section section) {
				return new ActionHologram<>(instance, section, chance);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at hologram action which is expected to be `Section`");
				return Action.empty();
			}
		}), "hologram");
	}

	protected void registerFakeItemAction() {
		registerAction(((args, chance) -> {
			if (args instanceof Section section) {
				return new ActionFakeItem<>(instance, section, chance);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at fake-item action which is expected to be `Section`");
				return Action.empty();
			}
		}), "fake-item");
	}

	protected void registerSpawnEntityAction() {
		this.registerAction((args, chance) -> {
			if (args instanceof Section section) {
				return new ActionSpawnEntity<>(instance, section, chance);
			} else {
				instance.getPluginLogger().warn("Invalid value type: " + args.getClass().getSimpleName()
						+ " found at spawn-entity action which is expected to be `Section`");
				return Action.empty();
			}
		}, "spawn-entity", "spawn-mob");
	}
}