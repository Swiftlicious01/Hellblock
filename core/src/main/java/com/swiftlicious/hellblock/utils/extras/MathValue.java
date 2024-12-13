package com.swiftlicious.hellblock.utils.extras;

import com.swiftlicious.hellblock.context.Context;

/**
 * The MathValue interface represents a mathematical value that can be evaluated
 * within a specific context. This interface allows for the evaluation of
 * mathematical expressions or plain numerical values in the context of custom
 * fishing mechanics.
 *
 * @param <T> the type of the holder object for the context
 */
public interface MathValue<T> {

	/**
	 * Evaluates the mathematical value within the given context.
	 *
	 * @param context the context in which the value is evaluated
	 * @return the evaluated value as a double
	 */
	double evaluate(Context<T> context);

	/**
	 * Evaluates the mathematical value within the given context.
	 *
	 * @param context              the context in which the value is evaluated
	 * @param parseRawPlaceholders whether to parse raw placeholders for instance
	 *                             %xxx%
	 * @return the evaluated value as a double
	 */
	default double evaluate(Context<T> context, boolean parseRawPlaceholders) {
		return evaluate(context);
	}

	/**
	 * Creates a MathValue based on a mathematical expression.
	 *
	 * @param expression the mathematical expression to evaluate
	 * @param <T>        the type of the holder object for the context
	 * @return a MathValue instance representing the given expression
	 */
	static <T> MathValue<T> expression(String expression) {
		return new ExpressionMathValue<>(expression);
	}

	/**
	 * Creates a MathValue based on a plain numerical value.
	 *
	 * @param value the numerical value to represent
	 * @param <T>   the type of the holder object for the context
	 * @return a MathValue instance representing the given plain value
	 */
	static <T> MathValue<T> plain(double value) {
		return new PlainMathValue<>(value);
	}

	/**
	 * Creates a MathValue based on a range of values.
	 *
	 * @param value the ranged value to represent
	 * @param <T>   the type of the holder object for the context
	 * @return a MathValue instance representing the given ranged value
	 */
	static <T> MathValue<T> rangedDouble(String value) {
		return new RangedDoubleValue<>(value);
	}

	/**
	 * Creates a MathValue based on a range of values.
	 *
	 * @param value the ranged value to represent
	 * @param <T>   the type of the holder object for the context
	 * @return a MathValue instance representing the given ranged value
	 */
	static <T> MathValue<T> rangedInt(String value) {
		return new RangedIntValue<>(value);
	}

	/**
	 * Automatically creates a MathValue based on the given object. If the object is
	 * a String, it is treated as a mathematical expression. If the object is a
	 * numerical type (Double, Integer, Long, Float), it is treated as a plain
	 * value.
	 *
	 * @param o   the object to evaluate and create a MathValue from
	 * @param <T> the type of the holder object for the context
	 * @return a MathValue instance representing the given object, either as an
	 *         expression or a plain value
	 * @throws IllegalArgumentException if the object type is not supported
	 */
	static <T> MathValue<T> auto(Object o) {
		return auto(o, false);
	}

	static <T> MathValue<T> auto(Object o, boolean intFirst) {
		if (o instanceof String s) {
			if (s.contains("~")) {
				return intFirst ? (s.contains(".") ? rangedDouble(s) : rangedInt(s)) : rangedDouble(s);
			}
			try {
				return plain(Double.parseDouble(s));
			} catch (NumberFormatException e) {
				return expression(s);
			}
		} else if (o instanceof Number n) {
			return plain(n.doubleValue());
		}
		throw new IllegalArgumentException("Unsupported type: " + o.getClass());
	}
}