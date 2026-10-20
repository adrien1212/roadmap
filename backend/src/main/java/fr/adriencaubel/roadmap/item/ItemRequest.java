package fr.adriencaubel.roadmap.item;

import java.time.LocalDate;

/** Body of POST (missing fields take the defaults) and PUT (replaces every field). */
public record ItemRequest(
		String title,
		String description,
		LocalDate startDate,
		String startPrecision,
		LocalDate endDate,
		String endPrecision,
		String status,
		Long categoryId,
		Boolean internal) {
}
