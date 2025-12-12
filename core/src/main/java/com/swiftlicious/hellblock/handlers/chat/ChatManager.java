package com.swiftlicious.hellblock.handlers.chat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.EventExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.locale.TranslationManager;
import com.swiftlicious.hellblock.database.StorageManager;
import com.swiftlicious.hellblock.handlers.AdventureDependencyHelper;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.CoopChatSetting;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

public class ChatManager implements Reloadable {

	protected final HellblockPlugin instance;

	protected String globalFormat;
	protected String localFormat;
	protected String partyFormat;

	private Listener activeListener;

	public ChatManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		TranslationManager tm = instance.getTranslationManager();
		this.globalFormat = tm.miniMessageTranslation(MessageConstants.CHAT_GLOBAL.build().key());
		this.localFormat = tm.miniMessageTranslation(MessageConstants.CHAT_LOCAL.build().key());
		this.partyFormat = tm.miniMessageTranslation(MessageConstants.CHAT_PARTY.build().key());

		try {
			registerComponentChatListener();
		} catch (ClassNotFoundException e) {
			activeListener = new LegacyChatListener(instance);
			Bukkit.getPluginManager().registerEvents(activeListener, instance);
			instance.getPluginLogger().info("Legacy chat system detected — using AsyncPlayerChatEvent.");
		}
	}

	@Override
	public void unload() {
		if (activeListener != null) {
			HandlerList.unregisterAll(activeListener);
			activeListener = null;
		}

		RENDERER_CACHE.clear();
	}

	@SuppressWarnings("unchecked")
	private void registerComponentChatListener() throws ClassNotFoundException {
		try {
			Class<? extends Event> asyncChatClass = (Class<? extends Event>) Class
					.forName("io.papermc.paper.event.player.AsyncChatEvent");
			Class<?> chatRendererClass = Class.forName("io.papermc.paper.chat.ChatRenderer");

			// Reflective method handles
			Method getPlayerMethod;
			Method rendererGetter;
			Method rendererSetter;
			Method viewersGetter;
			Method messageComponent;

			try {
				getPlayerMethod = asyncChatClass.getMethod("getPlayer");
				messageComponent = asyncChatClass.getMethod("message");
				rendererGetter = asyncChatClass.getMethod("renderer");
				rendererSetter = asyncChatClass.getMethod("renderer", chatRendererClass);
				viewersGetter = asyncChatClass.getMethod("viewers"); // Set<Audience>
			} catch (NoSuchMethodException | SecurityException ex) {
				throw new ClassNotFoundException("AsyncChatEvent API changed or missing methods", ex);
			}

			activeListener = new Listener() {
			}; // dummy listener for unregister

			EventExecutor executor = (listener, event) -> {
				if (!asyncChatClass.isInstance(event))
					return;
				if (!(event instanceof Cancellable cancellable))
					return;

				try {
					Player player = (Player) getPlayerMethod.invoke(event);

					StorageManager storage = instance.getStorageManager();
					Optional<UserData> userDataOpt = storage.getCachedUserData(player.getUniqueId());
					if (userDataOpt.isEmpty())
						return;

					UserData userData = userDataOpt.get();
					HellblockData hellblockData = userData.getHellblockData();
					CoopChatSetting chatSetting = hellblockData.getChatSetting();

					String formattedMessage;
					Set<UUID> recipients = new HashSet<>();

					switch (chatSetting) {
					case GLOBAL -> {
						formattedMessage = globalFormat;
						recipients.addAll(storage.getOnlineUsers().stream().filter(Objects::nonNull)
								.filter(UserData::isOnline).map(UserData::getUUID).toList());
						finishComponentChatEvent(cancellable, CoopChatSetting.GLOBAL, formattedMessage, recipients,
								rendererGetter, rendererSetter, viewersGetter, chatRendererClass, messageComponent);
					}
					case LOCAL, PARTY -> {
						formattedMessage = (chatSetting == CoopChatSetting.LOCAL) ? localFormat : partyFormat;

						if (chatSetting == CoopChatSetting.LOCAL) {
							double radiusSquared = 100 * 100;
							Location senderLoc = player.getLocation();

							recipients.add(player.getUniqueId());
							recipients.addAll(storage.getOnlineUsers().stream().filter(Objects::nonNull)
									.filter(UserData::isOnline).map(UserData::getPlayer).filter(Objects::nonNull)
									.filter(p -> p.getWorld().getUID().equals(player.getWorld().getUID()))
									.filter(p -> p.getLocation().distanceSquared(senderLoc) <= radiusSquared)
									.map(Player::getUniqueId).toList());
						}

						if (hellblockData.hasHellblock()) {
							UUID ownerId = hellblockData.getOwnerUUID();
							if (ownerId != null) {
								storage.getCachedUserDataWithFallback(ownerId, false)
										.thenAccept(ownerOpt -> instance.getScheduler().executeSync(() -> {
											ownerOpt.ifPresent(ownerData -> recipients
													.addAll(ownerData.getHellblockData().getPartyPlusOwner()));
											finishComponentChatEvent(cancellable, CoopChatSetting.PARTY,
													formattedMessage, recipients, rendererGetter, rendererSetter,
													viewersGetter, chatRendererClass, messageComponent);
										}));
								return;
							}
						}

						finishComponentChatEvent(cancellable, CoopChatSetting.LOCAL, formattedMessage, recipients,
								rendererGetter, rendererSetter, viewersGetter, chatRendererClass, messageComponent);
					}
					default -> {
						formattedMessage = globalFormat;
						recipients.addAll(storage.getOnlineUsers().stream().filter(Objects::nonNull)
								.filter(UserData::isOnline).map(UserData::getUUID).toList());
						finishComponentChatEvent(cancellable, CoopChatSetting.GLOBAL, formattedMessage, recipients,
								rendererGetter, rendererSetter, viewersGetter, chatRendererClass, messageComponent);
					}
					}

				} catch (Throwable t) {
					t.printStackTrace();
				}
			};

			Bukkit.getPluginManager().registerEvent(asyncChatClass, activeListener, EventPriority.MONITOR, executor,
					instance, true);
			instance.getPluginLogger().info("Component chat system detected — using Adventure renderer.");

		} catch (ClassNotFoundException ignored) {
			// Server is not running Paper or does not expose the modern async chat event
		} catch (Throwable t) {
			throw new RuntimeException("Failed to register Paper AsyncChatEvent listener", t);
		}
	}

	@SuppressWarnings("unchecked")
	private void finishComponentChatEvent(@NotNull Cancellable event, @NotNull CoopChatSetting chatSetting,
			@NotNull String prefix, @NotNull Set<UUID> recipientIds, @NotNull Method rendererGetter,
			@NotNull Method rendererSetter, @NotNull Method viewersGetter, @NotNull Class<?> chatRendererClass,
			@NotNull Method messageComponent) {
		if (event.isCancelled()) {
			return; // Respect prior cancellations
		}

		try {
			// Restrict viewers to recipients
			Set<net.kyori.adventure.audience.Audience> viewers = (Set<net.kyori.adventure.audience.Audience>) viewersGetter
					.invoke(event);
			viewers.clear();
			viewers.addAll(recipientIds.stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
					.filter(Player::isOnline).collect(Collectors.toSet()));

			RendererCache cache = RENDERER_CACHE.get(chatSetting);
			Class<?> bridgeClass = (cache != null) ? cache.bridgeClass() : null;
			Class<?> rendererClass = (cache != null) ? cache.rendererClass() : null;

			// Grab any native paper component to establish loader context
			Object nativeDisplay = messageComponent.invoke(event);
			// Cache it for reflection access
			NativeComponentCache.setNativeReference(nativeDisplay);

			if (cache == null || !Objects.equals(cache.prefix(), prefix)) {
				// Ensure Gson bridge exists
				if (bridgeClass == null) {
					bridgeClass = new ByteBuddy().subclass(Object.class)
							.name("com.swiftlicious.hellblock.generated.GsonBridge_" + chatSetting.name())
							.defineMethod("fromJson", Object.class, Modifier.PUBLIC | Modifier.STATIC)
							.withParameter(String.class).intercept(MethodDelegation.to(GsonBridgeDelegate.class)).make()
							.load(ChatManager.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
							.getLoaded();
				}

				// Build renderer class if needed
				String rendererName = "com.swiftlicious.hellblock.generated.HellblockChatRenderer_"
						+ chatSetting.name();
				if (rendererClass == null) {
					try {
						rendererClass = Class.forName(rendererName);
					} catch (ClassNotFoundException e) {
						rendererClass = new ByteBuddy().subclass(Object.class).implement(chatRendererClass)
								.name(rendererName)
								.defineField("delegate", ChatRenderDelegate.class, Modifier.PRIVATE | Modifier.FINAL)
								.defineConstructor(Modifier.PUBLIC).withParameters(ChatRenderDelegate.class)
								.intercept(MethodCall.invoke(Object.class.getConstructor())
										.andThen(FieldAccessor.ofField("delegate").setsArgumentAt(0)))
								.method(ElementMatchers.named("render")).intercept(MethodDelegation.toField("delegate"))
								.make().load(ChatManager.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
								.getLoaded();
					}
				}

				instance.debug("Generated ByteBuddy ChatRenderer implementation for " + chatSetting.name());

				cache = new RendererCache(prefix, rendererClass, bridgeClass);
				RENDERER_CACHE.put(chatSetting, cache);
			}

			// Deserialize prefix inside Paper runtime (native kyori)
			Method fromJson = cache.bridgeClass().getMethod("fromJson", String.class);
			Object prefixComponent = fromJson.invoke(null,
					instance.getDependencyManager().runWithLoader(AdventureDependencyHelper.ADVENTURE_DEPENDENCIES,
							() -> AdventureHelper.componentToJson(AdventureHelper.miniMessageToComponent(prefix))));

			// Grab whatever renderer is currently active
			Object previousRenderer = rendererGetter.invoke(event);
			ChatRenderDelegate delegate = new ChatRenderDelegate(prefixComponent, previousRenderer);
			Object rendererInstance = cache.rendererClass().getConstructor(ChatRenderDelegate.class)
					.newInstance(delegate);

			rendererSetter.invoke(event, rendererInstance);
		} catch (Throwable t) {
			instance.getPluginLogger().warn("Failed to finish async chat event", t);
		} finally {
			// Clean up to avoid holding refs between events
			NativeComponentCache.clearNativeReference();
		}
	}

	private static final Map<CoopChatSetting, RendererCache> RENDERER_CACHE = new EnumMap<>(CoopChatSetting.class);

	public record RendererCache(String prefix, @Nullable Class<?> rendererClass, @Nullable Class<?> bridgeClass) {
	}

	public final class NativeComponentCache {
		private static volatile Object nativeComponentRef;

		private NativeComponentCache() {
			throw new UnsupportedOperationException("This class cannot be instantiated");
		}

		public static void setNativeReference(@NotNull Object component) {
			if (component != null) {
				nativeComponentRef = component;
			}
		}

		@NotNull
		public static Object getNativeReference() {
			return nativeComponentRef;
		}

		public static void clearNativeReference() {
			nativeComponentRef = null;
		}
	}

	public class LegacyChatListener implements Listener {

		protected final HellblockPlugin instance;

		public LegacyChatListener(HellblockPlugin plugin) {
			this.instance = plugin;
		}

		@SuppressWarnings("deprecation")
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onLegacyChat(AsyncPlayerChatEvent event) {
			final Player player = event.getPlayer();
			final UUID playerId = player.getUniqueId();
			final StorageManager storageManager = instance.getStorageManager();

			Optional<UserData> userDataOpt = storageManager.getCachedUserData(playerId);
			if (userDataOpt.isEmpty())
				return;

			UserData userData = userDataOpt.get();
			HellblockData hellblockData = userData.getHellblockData();
			CoopChatSetting chatSetting = hellblockData.getChatSetting();

			String existingFormat = event.getFormat();
			Set<UUID> recipients = new HashSet<>();
			String formattedMessage;

			switch (chatSetting) {
			case GLOBAL -> {
				formattedMessage = globalFormat;
				recipients.addAll(storageManager.getOnlineUsers().stream().filter(Objects::nonNull)
						.filter(UserData::isOnline).map(UserData::getUUID).toList());
				finishLegacyChatEvent(event, formattedMessage, existingFormat, recipients);
			}

			case LOCAL, PARTY -> {
				formattedMessage = (chatSetting == CoopChatSetting.LOCAL) ? localFormat : partyFormat;

				if (chatSetting == CoopChatSetting.LOCAL) {
					double radiusSquared = 100 * 100;
					Location senderLoc = player.getLocation();

					recipients.add(player.getUniqueId());
					recipients.addAll(storageManager.getOnlineUsers().stream().filter(Objects::nonNull)
							.filter(UserData::isOnline).map(UserData::getPlayer).filter(Objects::nonNull)
							.filter(p -> p.getWorld().getUID().equals(player.getWorld().getUID()))
							.filter(p -> p.getLocation().distanceSquared(senderLoc) <= radiusSquared)
							.map(Player::getUniqueId).toList());
				}

				if (hellblockData.hasHellblock()) {
					UUID ownerId = hellblockData.getOwnerUUID();
					if (ownerId != null) {
						instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false)
								.thenAccept(ownerOpt -> {
									instance.getScheduler().executeSync(() -> {
										ownerOpt.ifPresent(ownerData -> recipients
												.addAll(ownerData.getHellblockData().getPartyPlusOwner()));
										finishLegacyChatEvent(event, formattedMessage, existingFormat, recipients);
									});
								});
						return; // Stop here; event will be finished in async callback
					}
				}

				finishLegacyChatEvent(event, formattedMessage, existingFormat, recipients);
			}

			default -> {
				formattedMessage = globalFormat;
				recipients.addAll(storageManager.getOnlineUsers().stream().filter(Objects::nonNull)
						.filter(UserData::isOnline).map(UserData::getUUID).toList());
				finishLegacyChatEvent(event, formattedMessage, existingFormat, recipients);
			}
			}
		}

		@SuppressWarnings("deprecation")
		private void finishLegacyChatEvent(@NotNull AsyncPlayerChatEvent event, @NotNull String prefix,
				@NotNull String existingFormat, @NotNull Set<UUID> recipientIds) {
			event.getRecipients().clear();
			event.getRecipients()
					.addAll(recipientIds.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList());

			if (!(prefix.isEmpty() && prefix.isBlank())) {
				event.setFormat(prefix + " " + existingFormat);
			}
		}
	}
}