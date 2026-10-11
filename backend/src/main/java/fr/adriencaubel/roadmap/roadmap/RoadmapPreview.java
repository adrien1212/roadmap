package fr.adriencaubel.roadmap.roadmap;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

/** Picks the Items drawn in a Roadmap's thumbnail on the Roadmaps screen. */
final class RoadmapPreview {

	static final int MAX_ITEMS = 4;

	private RoadmapPreview() {
	}

	/** The first {@value MAX_ITEMS} Items, in manual order, whose dates overlap the year. */
	static List<DatedItem> select(List<DatedItem> inManualOrder, Year year) {
		LocalDate first = year.atDay(1);
		LocalDate last = year.atMonth(12).atEndOfMonth();
		return inManualOrder.stream()
				.filter(item -> !item.startDate().isAfter(last) && !item.endDate().isBefore(first))
				.limit(MAX_ITEMS)
				.toList();
	}
}
