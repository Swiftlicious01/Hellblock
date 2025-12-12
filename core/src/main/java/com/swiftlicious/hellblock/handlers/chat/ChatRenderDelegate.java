package com.swiftlicious.hellblock.handlers.chat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import com.swiftlicious.hellblock.HellblockPlugin;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

public class ChatRenderDelegate {

	private final Object prefix; // Paper-native Component instance
	private final Object previousRenderer;
	private Method renderMethod;
	private Method appendMethod;
	private Method spaceMethod;
	private boolean initialized = false;

	public ChatRenderDelegate(Object prefix, Object previousRenderer) {
		this.prefix = prefix;
		this.previousRenderer = previousRenderer;
	}

	private void ensureInitialized(Class<?> baseComponentClass) throws Exception {
		if (initialized)
			return;

		// Find the Component interface in the hierarchy (Paper’s native one)
		Class<?> componentInterface = null;
		for (Class<?> iface : baseComponentClass.getInterfaces()) {
			if (iface.getSimpleName().equals("Component")) {
				componentInterface = iface;
				break;
			}
		}

		// If not found directly, traverse up the hierarchy
		Class<?> current = baseComponentClass;
		while (componentInterface == null && current != null) {
			for (Class<?> iface : current.getInterfaces()) {
				if (iface.getSimpleName().equals("Component")) {
					componentInterface = iface;
					break;
				}
			}
			current = current.getSuperclass();
		}

		if (componentInterface == null) {
			throw new IllegalStateException("Could not find Component interface for " + baseComponentClass);
		}

		// Locate append(Component)
		for (Method m : baseComponentClass.getMethods()) {
			if (!m.getName().equals("append") || m.getParameterCount() != 1)
				continue;

			Class<?> param = m.getParameterTypes()[0];
			if (param.getSimpleName().equals("Component")) { // exact match
				appendMethod = m;
				break;
			}
		}

		if (appendMethod == null) {
			// Log available ones for sanity check
			String all = Arrays.stream(baseComponentClass.getMethods()).filter(m -> m.getName().equals("append"))
					.map(Method::toString).collect(Collectors.joining(", "));
			throw new NoSuchMethodException(
					"Could not find append(Component) on " + baseComponentClass + ". Available options: " + all);
		}

		// Locate static Component.space() on the interface
		for (Method m : componentInterface.getMethods()) {
			if (Modifier.isStatic(m.getModifiers()) && m.getName().equals("space")) {
				spaceMethod = m;
				break;
			}
		}

		if (spaceMethod == null)
			throw new NoSuchMethodException("Could not find static Component.space() on " + componentInterface);

		initialized = true;
	}

	/**
	 * Matches Paper's ChatRenderer.render signature reflectively: Component
	 * render(Player source, Component displayName, Component message, Audience
	 * viewer)
	 */
	@RuntimeType
	public Object render(@Argument(0) Object source, @Argument(1) Object displayName, @Argument(2) Object message,
			@Argument(3) Object viewer) throws Throwable {
		try {
			// Invoke previous renderer first
			if (renderMethod == null && previousRenderer != null) {
				for (Method m : previousRenderer.getClass().getMethods()) {
					if (!m.getName().equals("render") || m.getParameterCount() != 4)
						continue;
					Class<?>[] params = m.getParameterTypes();
					if (params[0].isInstance(source) && params[1].isInstance(displayName)
							&& params[2].isInstance(message) && params[3].isInstance(viewer)) {
						m.setAccessible(true);
						renderMethod = m;
						break;
					}
				}
			}

			Object baseRendered = message;
			if (previousRenderer != null && renderMethod != null) {
				renderMethod.setAccessible(true);
				baseRendered = renderMethod.invoke(previousRenderer, source, displayName, message, viewer);
			}

			// Ensure component methods resolved from the correct loader
			ensureInitialized(baseRendered.getClass());

			ClassLoader nativeLoader = prefix.getClass().getClassLoader();
			Class<?> compInterface = spaceMethod.getDeclaringClass();
			if (!spaceMethod.getDeclaringClass().getClassLoader().equals(nativeLoader)) {
				Class<?> nativeComponentClass = nativeLoader.loadClass(compInterface.getName());
				spaceMethod = nativeComponentClass.getMethod("space");
			}

			// Combine prefix + space + rendered
			if (prefix != null && !isComponentEmpty(prefix)) {
				Object space = spaceMethod.invoke(null);
				Object combined = appendMethod.invoke(prefix, space);
				return appendMethod.invoke(combined, baseRendered);
			} else {
				// No prefix, skip adding space
				return baseRendered;
			}

		} catch (Throwable t) {
			HellblockPlugin.getInstance().getPluginLogger().severe("Failed to render chat message " + message, t);
			return message;
		}
	}

	private boolean isComponentEmpty(Object component) {
		if (component == null)
			return true;
		try {
			// Try to find a "content()" method
			Method contentMethod = null;
			try {
				contentMethod = component.getClass().getMethod("content");
			} catch (NoSuchMethodException ignored) {
				// not all component types have content()
			}

			String content = "";
			if (contentMethod != null) {
				Object c = contentMethod.invoke(component);
				if (c != null)
					content = c.toString();
			}

			// Try to find "children()" method to check for subcomponents
			Method childrenMethod = null;
			try {
				childrenMethod = component.getClass().getMethod("children");
			} catch (NoSuchMethodException ignored) {
				// not all components have children()
			}

			boolean hasChildren = false;
			if (childrenMethod != null) {
				Object children = childrenMethod.invoke(component);
				if (children instanceof Collection<?>) {
					hasChildren = !((Collection<?>) children).isEmpty();
				}
			}

			// Consider empty if no text and no children
			return (content.isEmpty() && !hasChildren);
		} catch (Throwable ignored) {
			// If we can't reflectively determine content, assume not empty (safe fallback)
			return false;
		}
	}
}