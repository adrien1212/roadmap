package fr.adriencaubel.roadmap.item;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** One date of an Item: a day, or the whole week (Monday to Sunday), month or quarter starting on `first`. */
public record PlannedDate(LocalDate first, DatePrecision precision) {

	/** Normalised to the first day of its period; null without a date. A missing Precision is a Day. */
	public static PlannedDate of(LocalDate date, DatePrecision precision) {
		if (date == null) {
			return null;
		}
		DatePrecision p = precision == null ? DatePrecision.DAY : precision;
		LocalDate first = switch (p) {
			case DAY -> date;
			case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			case MONTH -> date.withDayOfMonth(1);
			case QUARTER -> LocalDate.of(date.getYear(), (date.getMonthValue() - 1) / 3 * 3 + 1, 1);
		};
		return new PlannedDate(first, p);
	}

	/** The last day of the period. */
	public LocalDate last() {
		return switch (precision) {
			case DAY -> first;
			case WEEK -> first.plusDays(6);
			case MONTH -> first.plusMonths(1).minusDays(1);
			case QUARTER -> first.plusMonths(3).minusDays(1);
		};
	}
}
