package com.swiftlicious.hellblock.database.dependency.classpath;

import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.nio.file.Path;

import com.swiftlicious.hellblock.HellblockPlugin;

public class ReflectionClassPathAppender implements ClassPathAppender {
	private final URLClassLoaderAccess classLoaderAccess;

	public ReflectionClassPathAppender(ClassLoader classLoader) {
		if (classLoader instanceof URLClassLoader) {
			this.classLoaderAccess = URLClassLoaderAccess.create((URLClassLoader) classLoader);
		} else {
			throw new IllegalStateException("ClassLoader is not instance of URLClassLoader");
		}
	}

	public ReflectionClassPathAppender(HellblockPlugin plugin) {
		this(plugin.getClass().getClassLoader());
	}

	@Override
	public void addJarToClasspath(Path file) {
		try {
			this.classLoaderAccess.addURL(file.toUri().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}