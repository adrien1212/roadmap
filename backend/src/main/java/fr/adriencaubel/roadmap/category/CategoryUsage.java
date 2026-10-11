package fr.adriencaubel.roadmap.category;

/** A Category with the number of Items using it across all Roadmaps. */
public record CategoryUsage(Category category, long itemCount) {
}
