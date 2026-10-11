package fr.adriencaubel.roadmap.roadmap;

import java.util.List;

/** A Roadmap as shown on the Roadmaps screen. */
public record RoadmapSummary(Roadmap roadmap, long itemCount, long internalItemCount, List<DatedItem> preview) {
}
