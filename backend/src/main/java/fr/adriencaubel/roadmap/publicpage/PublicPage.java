package fr.adriencaubel.roadmap.publicpage;

import fr.adriencaubel.roadmap.category.Category;
import fr.adriencaubel.roadmap.item.Item;
import fr.adriencaubel.roadmap.roadmap.Roadmap;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * What a Reader receives for a Published Roadmap, and nothing more: no Owner data but its Brand,
 * no internal flag, no Internal Items.
 */
public record PublicPage(String name, Instant publishedAt, String brandName, boolean subscriptionsOpen,
		List<PublicCategory> categories, List<PublicItem> items) {

	/** For a Roadmap whose Readers can't subscribe (no working SMTP server). */
	static PublicPage of(Roadmap roadmap, List<Item> itemsInManualOrder) {
		return of(roadmap, itemsInManualOrder, false);
	}

	/**
	 * Internal Items are dropped here, on the backend; Categories are those of the remaining Items.
	 * subscriptionsOpen only says whether to offer the subscribe box, nothing else about the Owner's mail setup.
	 */
	static PublicPage of(Roadmap roadmap, List<Item> itemsInManualOrder, boolean subscriptionsOpen) {
		List<Item> visible = itemsInManualOrder.stream().filter(item -> !item.isInternal()).toList();
		List<PublicCategory> categories = visible.stream()
				.map(Item::getCategory)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new))
				.stream()
				.map(PublicCategory::of)
				.toList();
		// The Brand is the only Owner data a Reader receives.
		return new PublicPage(roadmap.getName(), roadmap.getPublishedAt(), roadmap.getOwner().getBrandName(),
				subscriptionsOpen, categories,
				visible.stream().map(PublicItem::of).toList());
	}

	public record PublicCategory(Long id, String name, String colour) {

		static PublicCategory of(Category category) {
			return new PublicCategory(category.getId(), category.getName(), category.getColour().key());
		}
	}

	public record PublicItem(Long id, String title, String description, LocalDate startDate, LocalDate endDate,
			String status, Long categoryId, Instant shippedAt) {

		static PublicItem of(Item item) {
			return new PublicItem(item.getId(), item.getTitle(), item.getDescription(), item.getStartDate(),
					item.getEndDate(), item.getStatus().key(),
					item.getCategory() == null ? null : item.getCategory().getId(), item.getShippedAt());
		}
	}
}
