package fr.adriencaubel.roadmap.roadmap;

/** A Roadmap as shown on the Roadmaps screen. */
public record RoadmapSummary(Roadmap roadmap, long itemCount, long internalItemCount) {
}
