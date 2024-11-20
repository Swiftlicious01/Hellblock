package com.swiftlicious.hellblock.config.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.node.Node;
import com.swiftlicious.hellblock.config.parser.function.ConfigParserFunction;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.utils.extras.TriConsumer;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Configuration types for various mechanics.
 */
public class ConfigType {

	public static final ConfigType ITEM = of("item", () -> {
		Map<String, Node<ConfigParserFunction>> parsers = new HashMap<>();
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getLootFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getItemFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEventFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getBaseEffectFormatFunctions());
		return parsers;
	}, (id, section, functions) -> {
		MechanicType.register(id, MechanicType.LOOT);
		ItemConfigParser config = new ItemConfigParser(id, section, functions);
		HellblockPlugin.getInstance().getItemManager().registerItem(config.getItem());
		HellblockPlugin.getInstance().getLootManager().registerLoot(config.getLoot());
		HellblockPlugin.getInstance().getEventManager().registerEventCarrier(config.getEventCarrier());
	});

	public static final ConfigType ENTITY = of("entity", () -> {
		Map<String, Node<ConfigParserFunction>> parsers = new HashMap<>();
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getLootFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEntityFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEventFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getBaseEffectFormatFunctions());
		return parsers;
	}, (id, section, functions) -> {
		MechanicType.register(id, MechanicType.LOOT);
		EntityConfigParser config = new EntityConfigParser(id, section, functions);
		HellblockPlugin.getInstance().getEntityManager().registerEntity(config.getEntity());
		HellblockPlugin.getInstance().getLootManager().registerLoot(config.getLoot());
		HellblockPlugin.getInstance().getEventManager().registerEventCarrier(config.getEventCarrier());
	});

	public static final ConfigType BLOCK = of("block", () -> {
		Map<String, Node<ConfigParserFunction>> parsers = new HashMap<>();
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getLootFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getBlockFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEventFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getBaseEffectFormatFunctions());
		return parsers;
	}, (id, section, functions) -> {
		MechanicType.register(id, MechanicType.LOOT);
		BlockConfigParser config = new BlockConfigParser(id, section, functions);
		HellblockPlugin.getInstance().getBlockManager().registerBlock(config.getBlock());
		HellblockPlugin.getInstance().getLootManager().registerLoot(config.getLoot());
		HellblockPlugin.getInstance().getEventManager().registerEventCarrier(config.getEventCarrier());
	});

	public static final ConfigType ROD = of("rod", () -> {
		Map<String, Node<ConfigParserFunction>> parsers = new HashMap<>();
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getItemFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEffectModifierFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEventFormatFunctions());
		return parsers;
	}, (id, section, functions) -> {
		MechanicType.register(id, MechanicType.ROD);
		RodConfigParser config = new RodConfigParser(id, section, functions);
		HellblockPlugin.getInstance().getItemManager().registerItem(config.getItem());
		// HellblockPlugin.getInstance().getLootManager().registerLoot(config.getLoot());
		HellblockPlugin.getInstance().getEffectManager().registerEffectModifier(config.getEffectModifier(),
				MechanicType.ROD);
		HellblockPlugin.getInstance().getEventManager().registerEventCarrier(config.getEventCarrier());
	});

	public static final ConfigType BAIT = of("bait", () -> {
		Map<String, Node<ConfigParserFunction>> parsers = new HashMap<>();
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getItemFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEffectModifierFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEventFormatFunctions());
		return parsers;
	}, (id, section, functions) -> {
		MechanicType.register(id, MechanicType.BAIT);
		BaitConfigParser config = new BaitConfigParser(id, section, functions);
		HellblockPlugin.getInstance().getItemManager().registerItem(config.getItem());
		// HellblockPlugin.getInstance().getLootManager().registerLoot(config.getLoot());
		HellblockPlugin.getInstance().getEffectManager().registerEffectModifier(config.getEffectModifier(),
				MechanicType.BAIT);
		HellblockPlugin.getInstance().getEventManager().registerEventCarrier(config.getEventCarrier());
	});

	public static final ConfigType HOOK = of("hook", () -> {
		Map<String, Node<ConfigParserFunction>> parsers = new HashMap<>();
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getItemFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEffectModifierFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEventFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getHookFormatFunctions());
		return parsers;
	}, (id, section, functions) -> {
		MechanicType.register(id, MechanicType.HOOK);
		HookConfigParser config = new HookConfigParser(id, section, functions);
		HellblockPlugin.getInstance().getItemManager().registerItem(config.getItem());
		// HellblockPlugin.getInstance().getLootManager().registerLoot(config.getLoot());
		HellblockPlugin.getInstance().getEffectManager().registerEffectModifier(config.getEffectModifier(),
				MechanicType.HOOK);
		HellblockPlugin.getInstance().getEventManager().registerEventCarrier(config.getEventCarrier());
		HellblockPlugin.getInstance().getHookManager().registerHook(config.getHook());
	});

	public static final ConfigType UTIL = of("util", () -> {
		Map<String, Node<ConfigParserFunction>> parsers = new HashMap<>();
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getItemFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEffectModifierFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEventFormatFunctions());
		return parsers;
	}, (id, section, functions) -> {
		MechanicType.register(id, MechanicType.UTIL);
		UtilConfigParser config = new UtilConfigParser(id, section, functions);
		HellblockPlugin.getInstance().getItemManager().registerItem(config.getItem());
		// HellblockPlugin.getInstance().getLootManager().registerLoot(config.getLoot());
		HellblockPlugin.getInstance().getEffectManager().registerEffectModifier(config.getEffectModifier(),
				MechanicType.UTIL);
		HellblockPlugin.getInstance().getEventManager().registerEventCarrier(config.getEventCarrier());
	});

	public static final ConfigType ENCHANT = of("enchant", () -> {
		Map<String, Node<ConfigParserFunction>> parsers = new HashMap<>();
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEventFormatFunctions());
		parsers.putAll(HellblockPlugin.getInstance().getConfigManager().getEffectModifierFormatFunctions());
		return parsers;
	}, (id, section, functions) -> {
		EnchantConfigParser config = new EnchantConfigParser(id, section, functions);
		HellblockPlugin.getInstance().getEffectManager().registerEffectModifier(config.getEffectModifier(),
				MechanicType.ENCHANT);
		HellblockPlugin.getInstance().getEventManager().registerEventCarrier(config.getEventCarrier());
	});

	private static final ConfigType[] values = new ConfigType[] { ITEM, ENTITY, BLOCK, HOOK, ROD, BAIT, UTIL, ENCHANT };

	/**
	 * Gets an array of all configuration types.
	 *
	 * @return An array of all configuration types.
	 */
	public static ConfigType[] values() {
		return values;
	}

	private final String path;
	private TriConsumer<String, Section, Map<String, Node<ConfigParserFunction>>> argumentConsumer;
	private Supplier<Map<String, Node<ConfigParserFunction>>> parserSupplier;

	/**
	 * Creates a new ConfigType with the specified path and argument consumer.
	 *
	 * @param path             the configuration path.
	 * @param argumentConsumer the argument consumer.
	 */
	public ConfigType(String path, Supplier<Map<String, Node<ConfigParserFunction>>> parserSupplier,
			TriConsumer<String, Section, Map<String, Node<ConfigParserFunction>>> argumentConsumer) {
		this.path = path;
		this.argumentConsumer = argumentConsumer;
		this.parserSupplier = parserSupplier;
	}

	/**
	 * Set the argument consumer.
	 *
	 * @param argumentConsumer the argument consumer
	 */
	public void argumentConsumer(
			TriConsumer<String, Section, Map<String, Node<ConfigParserFunction>>> argumentConsumer) {
		this.argumentConsumer = argumentConsumer;
	}

	/**
	 * Creates a new ConfigType with the specified path and argument consumer.
	 *
	 * @param path             the configuration path.
	 * @param argumentConsumer the argument consumer.
	 * @return A new ConfigType instance.
	 */
	public static ConfigType of(String path, Supplier<Map<String, Node<ConfigParserFunction>>> parserSupplier,
			TriConsumer<String, Section, Map<String, Node<ConfigParserFunction>>> argumentConsumer) {
		return new ConfigType(path, parserSupplier, argumentConsumer);
	}

	/**
	 * Parses the configuration for this type.
	 *
	 * @param id        the identifier.
	 * @param section   the configuration section.
	 * @param functions the configuration functions.
	 */
	public void parse(String id, Section section, Map<String, Node<ConfigParserFunction>> functions) {
		argumentConsumer.accept(id, section, functions);
	}

	/**
	 * Gets the configuration path.
	 *
	 * @return The configuration path.
	 */
	public String path() {
		return path;
	}

	/**
	 * Get the config parsers
	 *
	 * @return config parsers
	 */
	public Map<String, Node<ConfigParserFunction>> parser() {
		return parserSupplier.get();
	}
}