package com.swiftlicious.hellblock.challenges.requirement;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.AsyncChallengeRequirement;
import com.swiftlicious.hellblock.listeners.FarmingHandler;
import com.swiftlicious.hellblock.listeners.FarmingHandler.PositionedBlock;
import com.swiftlicious.hellblock.world.CustomBlock;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;
import com.swiftlicious.hellblock.world.block.Growable;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Represents a challenge requirement where a player must harvest or grow a
 * specific crop type within a Hellblock island environment.
 * <p>
 * This requirement supports all standard overworld and Nether crops, including
 * special behavior for lava-grown plants (e.g., mushrooms, sugar cane, nether
 * wart). It also ensures that crops are fully grown or meet structural
 * conditions (like height for bamboo or cactus).
 * </p>
 *
 * <p>
 * <b>Example configuration (in challenges.yml):</b>
 * </p>
 * 
 * <pre>
 *   HARVEST_NETHER_WART:
 *     needed-amount: 50
 *     action: FARM
 *     data:
 *       crop: NETHER_WART
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: WARPED_FUNGUS
 * </pre>
 *
 * <p>
 * In this example, the player must harvest 50 fully-grown Nether Wart plants.
 * The challenge only progresses if crops meet the proper growth and
 * environmental conditions.
 * </p>
 */
public class FarmRequirement implements AsyncChallengeRequirement {

	/** List of all crops that can be tracked by this requirement. */
	public static final Set<Material> SUPPORTED_CROPS = EnumSet.of(Material.WHEAT, Material.POTATOES,
			Material.BEETROOTS, Material.CARROTS, Material.MELON, Material.PUMPKIN, Material.SUGAR_CANE,
			Material.NETHER_WART, Material.RED_MUSHROOM, Material.BROWN_MUSHROOM, Material.CACTUS,
			Material.SWEET_BERRY_BUSH, Material.BAMBOO);

	private final Material cropType;

	public FarmRequirement(@NotNull Section data) {
		String crop = data.getString("crop");
		if (crop == null) {
			throw new IllegalArgumentException("FARM requires 'crop' in data");
		}

		Material m = Material.matchMaterial(crop.toUpperCase(Locale.ROOT));
		if (m == null || !SUPPORTED_CROPS.contains(m)) {
			throw new IllegalArgumentException("Invalid or unsupported crop material: " + crop);
		}

		this.cropType = m;
	}

	@Override
	public boolean matches(@NotNull Object context) {
		// Fallback or "unsupported in sync mode"
		throw new UnsupportedOperationException("FarmRequirement requires async matching");
	}

	/**
	 * Checks whether the provided {@link Block} context satisfies the configured
	 * crop conditions. Each crop type has specific logic:
	 * <ul>
	 * <li>Ageable crops (e.g., wheat, carrots) must be fully grown.</li>
	 * <li>Tall crops (e.g., bamboo, cactus, sugar cane) must reach ≥3 blocks high
	 * and be broken at base or 2nd block.</li>
	 * <li>Mushrooms, sugar cane, and other lava-grown crops are validated using the
	 * {@link FarmingHandler} lava checks.</li>
	 * <li>Melons and pumpkins must have adjacent, fully-grown stems above hydrated
	 * farmland.</li>
	 * </ul>
	 *
	 * @param context The event context (expected to be a {@link Block}).
	 * @return {@code true} if the block meets all defined growth and environmental
	 *         conditions.
	 */
	@Override
	public CompletableFuture<Boolean> matchesAsync(@NotNull Object context) {
		if (!(context instanceof Block block))
			return CompletableFuture.completedFuture(false);

		Optional<HellblockWorld<?>> worldOpt = HellblockPlugin.getInstance().getWorldManager()
				.getWorld(block.getWorld());
		if (worldOpt.isEmpty())
			return CompletableFuture.completedFuture(false);

		HellblockWorld<?> world = worldOpt.get();
		Pos3 pos = Pos3.from(block.getLocation());

		return world.getBlockState(pos).thenCompose(stateOpt -> {
			if (stateOpt.isEmpty())
				return CompletableFuture.completedFuture(false);

			FarmingHandler farmManager = HellblockPlugin.getInstance().getFarmingManager();
			CustomBlockState state = stateOpt.get();
			CustomBlock type = state.type();

			// Normalize crop key
			String key = type.type().value().toLowerCase();
			String matchKey = cropType.name().toLowerCase();
			if (!key.equals(matchKey) && !key.equals("minecraft:" + matchKey) && !key.equals("hellblock:" + matchKey))
				return CompletableFuture.completedFuture(false);

			// === Crop-specific handling ===

			if (cropType == Material.NETHER_WART && type instanceof Growable wart) {
				return CompletableFuture.completedFuture(wart.getAge(state) == wart.getMaxAge(state));
			}

			if (cropType == Material.SUGAR_CANE) {
				return farmManager.getCropBase(pos, FarmingHandler.SUGAR_CANE_KEYS, world)
						.thenCompose(base -> farmManager.getCropHeight(base, FarmingHandler.SUGAR_CANE_KEYS, world)
								.thenCompose(height -> {
									if (height < 3 || !(pos.equals(base) || pos.equals(base.up())))
										return CompletableFuture.completedFuture(false);

									return world.getBlockState(base)
											.thenCompose(baseStateOpt -> farmManager.checkForLavaAroundSugarCane(
													new PositionedBlock(base, baseStateOpt.orElse(null)), world));
								}));
			}

			if (cropType == Material.MELON || cropType == Material.PUMPKIN) {
				List<CompletableFuture<Boolean>> checks = new ArrayList<>();

				for (BlockFace face : FarmingHandler.FACES) {
					Pos3 adjacent = pos.offset(face);
					CompletableFuture<Boolean> check = world.getBlockState(adjacent).thenCompose(stemOpt -> {
						if (stemOpt.isPresent() && stemOpt.get().type() instanceof Growable growable
								&& growable.getAge(stemOpt.get()) == growable.getMaxAge(stemOpt.get())) {

							Pos3 below = adjacent.down();
							return world.getBlockState(below).thenCompose(belowStateOpt -> {
								if (belowStateOpt.isEmpty())
									return CompletableFuture.completedFuture(false);

								return farmManager
										.checkForLavaAroundFarm(new PositionedBlock(below, belowStateOpt.get()), world);
							});
						}
						return CompletableFuture.completedFuture(false);
					});
					checks.add(check);
				}

				return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new))
						.thenApply(v -> checks.stream().anyMatch(f -> f.join()));
			}

			if (cropType == Material.SWEET_BERRY_BUSH && type instanceof Growable bush) {
				if (bush.getAge(state) == bush.getMaxAge(state)) {
					return farmManager.checkForLavaAroundBerryBush(new PositionedBlock(pos, state), world);
				}
				return CompletableFuture.completedFuture(false);
			}

			if (cropType == Material.BAMBOO || cropType == Material.CACTUS) {
				Set<String> keys = cropType == Material.BAMBOO ? FarmingHandler.BAMBOO_KEYS
						: FarmingHandler.CACTUS_KEYS;

				return farmManager.getCropBase(pos, keys, world)
						.thenCompose(base -> farmManager.getCropHeight(base, keys, world).thenCompose(height -> {
							if (height < 3 || !(pos.equals(base) || pos.equals(base.up())))
								return CompletableFuture.completedFuture(false);

							return world.getBlockState(base).thenCompose(baseStateOpt -> {
								PositionedBlock baseBlock = new PositionedBlock(base, baseStateOpt.orElse(null));
								if (cropType == Material.BAMBOO) {
									return farmManager.checkForLavaAroundBamboo(baseBlock, world);
								} else {
									return farmManager.checkForLavaAroundCactus(baseBlock, world);
								}
							});
						}));
			}

			if (cropType == Material.RED_MUSHROOM || cropType == Material.BROWN_MUSHROOM) {
				return CompletableFuture
						.completedFuture(farmManager.isLavaGrownMushroom(new PositionedBlock(pos, state)));
			}

			if (type instanceof Growable growable) {
				if (growable.getAge(state) == growable.getMaxAge(state)) {
					Pos3 below = pos.down();
					return world.getBlockState(below).thenCompose(belowStateOpt -> {
						if (belowStateOpt.isEmpty())
							return CompletableFuture.completedFuture(false);

						return farmManager.checkForLavaAroundFarm(new PositionedBlock(below, belowStateOpt.get()),
								world);
					});
				}
			}

			return CompletableFuture.completedFuture(false);
		});
	}
}