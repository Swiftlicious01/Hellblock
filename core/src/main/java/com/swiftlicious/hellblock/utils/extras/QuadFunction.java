package com.swiftlicious.hellblock.utils.extras;

import java.util.Objects;
import java.util.function.Function;

public interface QuadFunction<T, U, V, Z, R> {
	R apply(T var1, U var2, V var3, Z var4);

	default <W> QuadFunction<T, U, V, Z, W> andThen(Function<? super R, ? extends W> after) {
		Objects.requireNonNull(after);
		return (t, u, v, z) -> after.apply(this.apply(t, u, v, z));
	}
}