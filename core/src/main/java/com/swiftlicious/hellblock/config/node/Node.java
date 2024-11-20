package com.swiftlicious.hellblock.config.node;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Node<T> {

	private final T value;
	private final Map<String, Node<T>> childTree = new HashMap<>();

	public Node(T value) {
		this.value = value;
	}

	public Node() {
		this(null);
	}

	@Nullable
	public T nodeValue() {
		return value;
	}

	@NotNull
	public Map<String, Node<T>> getChildTree() {
		return childTree;
	}

	@Nullable
	public Node<T> getChild(String node) {
		return childTree.get(node);
	}

	@Nullable
	public Node<T> removeChild(String node) {
		return childTree.remove(node);
	}
}