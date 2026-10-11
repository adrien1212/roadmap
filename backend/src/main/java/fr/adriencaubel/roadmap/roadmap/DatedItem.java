package fr.adriencaubel.roadmap.roadmap;

import fr.adriencaubel.roadmap.item.ItemStatus;

import java.time.LocalDate;

/** An Item with both dates, as needed by the Roadmaps screen preview. */
public record DatedItem(Long roadmapId, LocalDate startDate, LocalDate endDate, ItemStatus status) {
}
