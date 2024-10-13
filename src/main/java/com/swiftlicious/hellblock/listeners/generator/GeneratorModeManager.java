package com.swiftlicious.hellblock.listeners.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import com.cryptomorin.xseries.particles.XParticle;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.LogUtils;

public class GeneratorModeManager {

	private final HellblockPlugin instance;

	private List<GenMode> generatorModes;
	private final GenMode defaultGenMode;
	private final GenMode universalGenMode;

	public GeneratorModeManager(HellblockPlugin plugin) {
		instance = plugin;
		this.generatorModes = new ArrayList<>();
		List<Material> defaultBlocks = new ArrayList<>();
		defaultBlocks.add(Material.LAVA);
		defaultBlocks.add(Material.LAVA);
		// THE ID IS 0 SINCE IT WILL ONLY BE USED IF NO OTHER GENMODES ARE LOADED
		this.defaultGenMode = new GenMode(0, defaultBlocks, "Netherrack generator", Material.NETHERRACK);
		this.universalGenMode = new GenMode(-1, defaultBlocks, "Universal generator", null);
	}

	public void loadFromConfig() {
		this.generatorModes = new ArrayList<>();
		if (instance.getConfig("config.yml").contains("generator-options.generationModes")) {

			ConfigurationSection section = instance.getConfig("config.yml")
					.getConfigurationSection("generator-options.generationModes");
			if (section == null) {
				LogUtils.severe("No generation mode section found");
				return;
			}
			for (String s : section.getKeys(false)) {
				List<String> blockNames = section.getStringList(s + ".blocks");
				List<Material> blockMaterials;
				blockMaterials = new ArrayList<>();
				for (String name : blockNames) {
					Material m = Material.valueOf(name.toUpperCase());
					blockMaterials.add(m);
				}
				Map<BlockFace, Material> fixedBlockMaterials = null;
				if (section.isConfigurationSection(s + ".fixedBlocks")) {
					fixedBlockMaterials = new HashMap<>();
					for (String fixedBlockFace : Objects
							.requireNonNull(section.getConfigurationSection(s + ".fixedBlocks")).getKeys(false)) {
						BlockFace blockFace = BlockFace.valueOf(fixedBlockFace.toUpperCase());
						if (!this.isSupportedBlockFace(blockFace)) {
							LogUtils.severe(fixedBlockFace.toUpperCase()
									+ " is not a valid block face. Use UP, DOWN, EAST, NORTH, WEST or SOUTH");
							continue;
						}
						String materialName = section.getString(s + ".fixedBlocks." + fixedBlockFace);
						if (materialName == null) {
							LogUtils.severe("Syntax error under block face &e" + fixedBlockFace.toUpperCase()
									+ " - No material name");
							continue;
						}
						Material m = Material.getMaterial(materialName.toUpperCase());
						if (m == null) {
							LogUtils.severe(
									materialName.toUpperCase() + " is not a valid material under block face &e"
											+ fixedBlockFace.toUpperCase());
							continue;
						}
						fixedBlockMaterials.put(blockFace, m);
					}
				}
				int id;
				try {
					id = Integer.parseInt(s);
				} catch (NumberFormatException e) {
					LogUtils.severe(s + " is not a valid generation mode id. MOST BE A NUMBER");
					return;
				}
				if (id < 0) {
					LogUtils.severe(id + " is not a valid generation mode id. MOST BE A POSITIVE NUMBER");
					return;
				}
				String name = null;
				if (section.contains(s + ".displayName")) {
					name = section.getString(s + ".displayName");
				}
				Material fallbackMaterial = null;
				if (section.contains(s + ".fallback")) {
					fallbackMaterial = Material
							.getMaterial(Objects.requireNonNull(section.getString(s + ".fallback")).toUpperCase());
					if (fallbackMaterial == null) {
						LogUtils.severe(section.getString(s + ".fallback") + " is not a valid fallback material");
					}
				}
				GenMode mode = new GenMode(id, blockMaterials, fixedBlockMaterials, name, fallbackMaterial);
				if (section.contains(s + ".searchForPlayersNearby")) {
					mode.setSearchForPlayersNearby(section.getBoolean(s + ".searchForPlayersNearby", false));
				}
				if (section.contains(s + ".generationSound")) {
					String soundString = section.getString(s + ".generationSound");
					if (soundString != null && !soundString.equalsIgnoreCase("none")) {
						Arrays.stream(Sound.values()).filter(sound -> sound.name().equalsIgnoreCase(soundString))
								.findFirst().ifPresentOrElse(mode::setGenSound,
										() -> LogUtils.severe("The sound " + soundString + " does not exist"));
					}
				}
				if (section.contains(s + ".particleEffect")) {
					String particle = section.getString(s + ".particleEffect");
					if (particle != null) {
						XParticle[] effects = XParticle.values();
						Arrays.stream(effects)
								.filter(particleEffect -> particleEffect.name().equalsIgnoreCase(particle)).findFirst()
								.ifPresentOrElse(mode::setParticleEffect, () -> LogUtils.severe("The particle " + particle + " does not exist"));
					}
				}

				if (section.contains(s + ".canGenerateWhileLavaRaining")) {
					boolean canGenWhileLavaRaining = section.getBoolean(s + ".canGenerateWhileLavaRaining");
					mode.setCanGenWhileLavaRaining(canGenWhileLavaRaining);
				}
				if (mode.isValid()) {
					this.generatorModes.add(mode);
				}
			}
		}

		if (this.generatorModes.isEmpty()) {
			LogUtils.severe("COULD NOT FIND ANY GENERATION MODES IN CONFIG. USING DEFAULT INSTEAD!");
			this.generatorModes.add(this.defaultGenMode);
		}

	}

	public GenMode getModeById(int id) {
		for (GenMode mode : this.getModes()) {
			if (mode.getId() == id)
				return mode;
		}
		return null;
	}

	public boolean isSupportedBlockFace(BlockFace blockFace) {
		if (blockFace == null)
			return false;
		return blockFace.equals(BlockFace.DOWN) || blockFace.equals(BlockFace.UP) || blockFace.equals(BlockFace.WEST)
				|| blockFace.equals(BlockFace.NORTH) || blockFace.equals(BlockFace.EAST)
				|| blockFace.equals(BlockFace.SOUTH);
	}

	public List<GenMode> getModesContainingMaterial(Material m) {
		List<GenMode> modesContainingMaterial = new ArrayList<>();
		for (GenMode mode : this.getModes()) {
			if (mode.containsBlock(m))
				modesContainingMaterial.add(mode);
		}
		return modesContainingMaterial;
	}

	public List<GenMode> getModes() {
		return this.generatorModes;
	}

	public GenMode getUniversalGenMode() {
		return universalGenMode;
	}

}