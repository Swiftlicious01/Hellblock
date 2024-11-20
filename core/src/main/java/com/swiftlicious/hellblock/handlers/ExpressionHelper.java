package com.swiftlicious.hellblock.handlers;

import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Helper class for evaluating mathematical expressions.
 */
public class ExpressionHelper {

	/**
	 * Evaluates a mathematical expression provided as a string.
	 *
	 * @param expression the mathematical expression to evaluate
	 * @return the result of the evaluation as a double
	 */
	public static double evaluate(String expression) {
		return new ExpressionBuilder(expression).build().evaluate();
	}
}