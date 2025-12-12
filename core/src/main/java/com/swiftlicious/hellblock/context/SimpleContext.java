package com.swiftlicious.hellblock.context;

import org.jetbrains.annotations.Nullable;

public class SimpleContext<T> extends AbstractContext<T> {
	public SimpleContext(@Nullable T holder, boolean sync) {
		super(holder, sync);
	}
}
