package com.swiftlicious.hellblock.v1_19_r2;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftFishHook;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.nms.NMSHandler;
import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.nms.entity.display.FakeItemDisplay;
import com.swiftlicious.hellblock.nms.entity.display.FakeTextDisplay;
import com.swiftlicious.hellblock.nms.exception.UnsupportedVersionException;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class NMSUtils1_19_R2 implements NMSHandler {

	private final EntityDataAccessor<Boolean> dataBiting;

	@SuppressWarnings("unchecked")
	public NMSUtils1_19_R2() {
		try {
			Field dataBitingField = FishingHook.class.getDeclaredField("ap");
			dataBitingField.setAccessible(true);
			dataBiting = (EntityDataAccessor<Boolean>) dataBitingField.get(null);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to get hook biting state", e);
		}
	}

	@Override
	public boolean isFishingHookBit(FishHook hook) {
		FishingHook fishingHook = ((CraftFishHook) hook).getHandle();
		return fishingHook.getEntityData().get(dataBiting);
	}

	@Override
	public List<ItemStack> getFishingLoot(Player player, FishHook hook, ItemStack rod) {
		Location location = hook.getLocation();
		ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
		RandomSource source = RandomSource.create();
		source.setSeed(hook.getUniqueId().getLeastSignificantBits() ^ level.getGameTime());
		LootContext.Builder loottableinfo_builder = (new LootContext.Builder(level))
				.withParameter(LootContextParams.ORIGIN, new Vec3(location.getX(), location.getY(), location.getZ()))
				.withParameter(LootContextParams.TOOL, CraftItemStack.asNMSCopy(rod))
				.withParameter(LootContextParams.THIS_ENTITY, ((CraftFishHook) hook).getHandle()).withRandom(source)
				.withLuck((float) (rod.getEnchantmentLevel(Enchantment.LUCK)
						+ Optional.ofNullable(player.getAttribute(Attribute.GENERIC_LUCK))
								.map(AttributeInstance::getValue).orElse(0d)));
		LootTable loottable = level.getServer().getLootTables().get(BuiltInLootTables.FISHING);
		List<net.minecraft.world.item.ItemStack> list = loottable
				.getRandomItems(loottableinfo_builder.create(LootContextParamSets.FISHING));
		return list.stream().filter(itemStack -> itemStack != null && !itemStack.isEmpty())
				.map(net.minecraft.world.item.ItemStack::getBukkitStack).toList();
	}

	@Override
	public void useItem(Player player, HandSlot handSlot, @Nullable ItemStack itemStack) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.gameMode.useItem(serverPlayer, ((CraftWorld) player.getWorld()).getHandle(),
				Optional.ofNullable(itemStack).map(stack -> ((CraftItemStack) itemStack).handle)
						.orElse(serverPlayer.getItemBySlot(EquipmentSlot.valueOf(handSlot.name() + "HAND"))),
				InteractionHand.valueOf(handSlot.name() + "_HAND"));
	}

    @Override
    public FakeArmorStand createFakeArmorStand(Location location) {
        return new ArmorStandInstance(location);
    }

    @Override
    public FakeItemDisplay createFakeItemDisplay(Location location) {
        throw new UnsupportedVersionException("Version 1.19.2 does not support this action");
    }

    @Override
    public FakeTextDisplay createFakeTextDisplay(Location location) {
        throw new UnsupportedVersionException("Version 1.19.2 does not support this action");
    }
}