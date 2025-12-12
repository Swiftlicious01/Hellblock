package com.swiftlicious.hellblock.player;

import java.time.LocalDate;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory.EmptyCheck;

/**
 * The {@code EarningData} class represents daily earnings data tied to a
 * specific date. It stores the total earnings for a particular day and resets
 * if accessed on a new day.
 * <p>
 * This class implements {@link EmptyCheck} to determine if any earnings are
 * recorded.
 */
public final class EarningData implements EmptyCheck {

	@Expose
	@SerializedName("earnings")
	protected double earnings;

	@Expose
	@SerializedName("date")
	protected LocalDate date;

	/**
	 * Constructs a new {@code EarningData} instance with the specified earnings and
	 * date. Automatically resets the earnings if the provided date is not today.
	 *
	 * @param earnings the amount earned
	 * @param date     the date the earnings are associated with
	 */
	public EarningData(double earnings, @NotNull LocalDate date) {
		this.earnings = earnings;
		this.date = date;
		refresh();
	}

	/**
	 * Gets the total earnings for the current date.
	 *
	 * @return the amount of earnings
	 */
	public double getEarnings() {
		return this.earnings;
	}

	/**
	 * Gets the date associated with the current earnings.
	 *
	 * @return the date of the earnings
	 */
	@NotNull
	public LocalDate getDate() {
		return this.date;
	}

	/**
	 * Sets the earnings amount.
	 *
	 * @param earnings the new earnings value
	 */
	public void setEarnings(double earnings) {
		this.earnings = earnings;
	}

	/**
	 * Sets the date associated with the earnings.
	 *
	 * @param date the new date value
	 */
	public void setDate(@NotNull LocalDate date) {
		this.date = date;
	}

	/**
	 * Creates an instance of {@code EarningData} with default values: zero earnings
	 * and today's date.
	 *
	 * @return a new instance of {@code EarningData} with default values
	 */
	@NotNull
	public static EarningData empty() {
		return new EarningData(0.0D, LocalDate.now());
	}

	/**
	 * Creates a copy of this {@code EarningData} instance.
	 *
	 * @return a new {@code EarningData} instance with the same earnings and date
	 */
	@NotNull
	public final EarningData copy() {
		return new EarningData(earnings, date);
	}

	/**
	 * Resets the earnings to 0 if the stored date does not match today's date. Also
	 * updates the stored date to today.
	 */
	public void refresh() {
		LocalDate today = LocalDate.now();
		if (!today.equals(date)) {
			this.date = today;
			this.earnings = 0.0D;
		}
	}

	/**
	 * Checks whether the earnings value is zero.
	 *
	 * @return true if no earnings are recorded, false otherwise
	 */
	@Override
	public boolean isEmpty() {
		return this.earnings == 0.0D;
	}
}