package com.swiftlicious.hellblock.listeners.fishing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import com.saicone.rtag.item.ItemTagStream;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.creation.item.damage.CustomDurabilityItem;
import com.swiftlicious.hellblock.creation.item.damage.DurabilityItem;
import com.swiftlicious.hellblock.creation.item.damage.VanillaDurabilityItem;
import com.swiftlicious.hellblock.effects.EffectModifier;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.mechanics.hook.HookConfig;
import com.swiftlicious.hellblock.utils.PlayerUtils;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ScoreComponent;

public class HookManager implements Listener, HookManagerInterface {

	protected final HellblockPlugin instance;
	private final Map<String, HookConfig> hooks = new HashMap<>();
	private final LZ4Factory factory;

	public HookManager(HellblockPlugin plugin) {
		instance = plugin;
		this.factory = LZ4Factory.fastestInstance();
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		hooks.clear();
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		instance.debug(hooks.size() > 0 ? "Loaded " + hooks.size() + " hook" + (hooks.size() == 1 ? "" : "s")
				: "No hooks found to load");
	}

	@Override
	public boolean registerHook(HookConfig hook) {
		if (hooks.containsKey(hook.id())) {
			return false;
		}
		hooks.put(hook.id(), hook);
		return true;
	}

	@NotNull
	@Override
	public Optional<HookConfig> getHook(String id) {
		return Optional.ofNullable(hooks.get(id));
	}

	@Override
	public Optional<String> getHookID(ItemStack rod) {
		if (rod == null || rod.getType() != Material.FISHING_ROD || rod.getAmount() == 0) {
			return Optional.empty();
		}

		final Item<ItemStack> wrapped = instance.getItemManager().wrap(rod);
		return wrapped.getTag("HellblockItem", "hook_id").map(o -> (String) o);
	}

	@EventHandler(ignoreCancelled = true)
	public void onDragDrop(InventoryClickEvent event) {
		final Player player = (Player) event.getWhoClicked();
		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}
		if (event.getClickedInventory() != player.getInventory()) {
			return;
		}
		if (player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}
		final ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getType() != Material.FISHING_ROD) {
			return;
		}
		if (instance.getFishingManager().getFishHook(player).isPresent()) {
			return;
		}
		final ItemStack cursor = event.getCursor();
		if (cursor.getType() == Material.AIR) {
			if (event.getClick() != ClickType.RIGHT) {
				return;
			}
			final Item<ItemStack> wrapped = instance.getItemManager().wrap(clicked);
			if (!wrapped.hasTag("HellblockItem", "hook_id")) {
				return;
			}
			event.setCancelled(true);
			final String id = (String) wrapped.getTag("HellblockItem", "hook_id").orElseThrow();
			final byte[] hookItemBase64 = (byte[]) wrapped.getTag("HellblockItem", "hook_stack").orElse(null);
			final int damage = (int) wrapped.getTag("HellblockItem", "hook_damage").orElse(0);
			final ItemStack itemStack;
			if (hookItemBase64 != null) {
				itemStack = bytesToHook(hookItemBase64);
			} else {
				itemStack = instance.getItemManager().buildInternal(Context.player(player).arg(ContextKeys.ID, id), id);
			}
			instance.getItemManager().setDamage(player, itemStack, damage);

			wrapped.removeTag("HellblockItem", "hook_id");
			wrapped.removeTag("HellblockItem", "hook_stack");
			wrapped.removeTag("HellblockItem", "hook_damage");
			wrapped.removeTag("HellblockItem", "hook_max_damage");

			// unsafe but have to use this
			player.setItemOnCursor(itemStack);

			final List<String> previousLore = wrapped.lore().orElse(new ArrayList<>());
			final List<String> newLore = new ArrayList<>();
			for (String previous : previousLore) {
				final Component component = AdventureHelper.jsonToComponent(previous);
				if (component instanceof ScoreComponent scoreComponent && "hb".equals(scoreComponent.name())
						&& "hook".equals(scoreComponent.objective())) {
					continue;
				}
				newLore.add(previous);
			}
			wrapped.lore(newLore);
			wrapped.load();
			return;
		}

		final String hookID = instance.getItemManager().getItemID(cursor);
		final Optional<HookConfig> setting = getHook(hookID);
		if (setting.isEmpty()) {
			return;
		}

		final Context<Player> context = Context.player(player);
		final HookConfig hookConfig = setting.get();
		final Optional<EffectModifier> modifier = instance.getEffectManager().getEffectModifier(hookID,
				MechanicType.HOOK);
		final boolean modifierCondition = modifier.isPresent()
				&& !RequirementManager.isSatisfied(context, modifier.get().requirements());
		if (modifierCondition) {
			return;
		}
		event.setCancelled(true);

		final ItemStack clonedHook = cursor.clone();
		clonedHook.setAmount(1);
		cursor.setAmount(cursor.getAmount() - 1);

		final Item<ItemStack> wrapped = instance.getItemManager().wrap(clicked);
		final String previousHookID = (String) wrapped.getTag("HellblockItem", "hook_id").orElse(null);
		if (previousHookID != null) {
			final int previousHookDamage = (int) wrapped.getTag("HellblockItem", "hook_damage").orElse(0);
			final ItemStack previousItemStack;
			final byte[] stackBytes = (byte[]) wrapped.getTag("HellblockItem", "hook_stack").orElse(null);
			if (stackBytes != null) {
				previousItemStack = bytesToHook(stackBytes);
			} else {
				previousItemStack = instance.getItemManager().buildInternal(Context.player(player), previousHookID);
			}
			if (previousItemStack != null) {
				instance.getItemManager().setDamage(player, previousItemStack, previousHookDamage);
				if (cursor.getAmount() == 0) {
					player.setItemOnCursor(previousItemStack);
				} else {
					PlayerUtils.giveItem(player, previousItemStack, 1);
				}
			}
		}

		final Item<ItemStack> wrappedHook = instance.getItemManager().wrap(clonedHook);
		final DurabilityItem durabilityItem;
		if (wrappedHook.hasTag("HellblockItem", "max_dur")) {
			durabilityItem = new CustomDurabilityItem(wrappedHook);
		} else if (hookConfig.maxUsages() > 0) {
			wrappedHook.setTag(hookConfig.maxUsages(), "HellblockItem", "max_dur");
			durabilityItem = new CustomDurabilityItem(wrappedHook);
		} else {
			durabilityItem = new VanillaDurabilityItem(wrappedHook);
		}

		wrapped.setTag(hookID, "HellblockItem", "hook_id");
		wrapped.setTag(hookToBytes(clonedHook), "HellblockItem", "hook_stack");
		wrapped.setTag(durabilityItem.damage(), "HellblockItem", "hook_damage");
		wrapped.setTag(durabilityItem.maxDamage(), "HellblockItem", "hook_max_damage");

		final List<String> previousLore = wrapped.lore().orElse(new ArrayList<>());
		final List<String> newLore = new ArrayList<>();
		final List<String> durabilityLore = new ArrayList<>();
		for (String previous : previousLore) {
			final Component component = AdventureHelper.jsonToComponent(previous);
			if (component instanceof ScoreComponent scoreComponent && "hb".equals(scoreComponent.name())) {
				if ("hook".equals(scoreComponent.objective())) {
					continue;
				} else if ("durability".equals(scoreComponent.objective())) {
					durabilityLore.add(previous);
					continue;
				}
			}
			newLore.add(previous);
		}
		hookConfig.lore().forEach(lore -> {
			final ScoreComponent.Builder builder = Component.score().name("hb").objective("hook");
			builder.append(AdventureHelper.miniMessageToComponent(
					lore.replace("{dur}", String.valueOf(durabilityItem.maxDamage() - durabilityItem.damage()))
							.replace("{max}", String.valueOf(durabilityItem.maxDamage()))));
			newLore.add(AdventureHelper.componentToJson(builder.build()));
		});
		newLore.addAll(durabilityLore);
		wrapped.lore(newLore);
		wrapped.load();
	}

	private byte[] hookToBytes(ItemStack hook) {
		try {
			final byte[] data = ItemTagStream.INSTANCE.toBytes(hook);
			final int decompressedLength = data.length;
			final LZ4Compressor compressor = factory.fastCompressor();
			final int maxCompressedLength = compressor.maxCompressedLength(decompressedLength);
			final byte[] compressed = new byte[maxCompressedLength];
			final int compressedLength = compressor.compress(data, 0, decompressedLength, compressed, 0,
					maxCompressedLength);

			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			final DataOutputStream outputStream = new DataOutputStream(byteArrayOutputStream);
			outputStream.writeInt(decompressedLength);
			outputStream.write(compressed, 0, compressedLength);
			outputStream.close();

			return byteArrayOutputStream.toByteArray();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private ItemStack bytesToHook(byte[] bytes) {
		try {
			final DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(bytes));
			final int decompressedLength = inputStream.readInt();
			final byte[] compressed = new byte[inputStream.available()];
			inputStream.readFully(compressed);

			final LZ4FastDecompressor decompressor = factory.fastDecompressor();
			final byte[] restored = new byte[decompressedLength];
			decompressor.decompress(compressed, 0, restored, 0, decompressedLength);

			return ItemTagStream.INSTANCE.fromBytes(restored);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}