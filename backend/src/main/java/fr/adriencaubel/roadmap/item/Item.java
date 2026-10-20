package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.category.Category;
import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/** One row of a Roadmap. Dates are optional and independent; position is the manual order. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

	static final int TITLE_MAX_LENGTH = 140;
	static final int DESCRIPTION_MAX_LENGTH = 10_000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Roadmap roadmap;

	@ManyToOne(fetch = FetchType.LAZY)
	private Category category;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	/** The first day of its period (see startPrecision). */
	private LocalDate startDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DatePrecision startPrecision = DatePrecision.DAY;

	/** The first day of its period too: the period's last day is computed (PlannedDate.last), never stored. */
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DatePrecision endPrecision = DatePrecision.DAY;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ItemStatus status;

	@Column(nullable = false)
	private boolean internal;

	@Column(nullable = false)
	private int position;

	/** When the Status last became Shipped; set and cleared by changeStatus, never inferred from dates. */
	private Instant shippedAt;

	/** A new Planned, uncategorised, non-Internal Item placed after lastPosition (null when the Roadmap is empty). */
	public static Item append(Roadmap roadmap, String title, Integer lastPosition) {
		Item item = new Item();
		item.roadmap = roadmap;
		item.retitle(title);
		item.status = ItemStatus.PLANNED;
		item.position = lastPosition == null ? 0 : lastPosition + 1;
		return item;
	}

	public void retitle(String title) {
		String trimmed = title == null ? "" : title.strip();
		if (trimmed.isEmpty()) {
			throw new ValidationException("Title is required");
		}
		if (trimmed.length() > TITLE_MAX_LENGTH) {
			throw new ValidationException("Title must be at most " + TITLE_MAX_LENGTH + " characters");
		}
		this.title = trimmed;
	}

	/** Plain text, kept as written (line breaks included). */
	public void describe(String description) {
		if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
			throw new ValidationException("Description must be at most " + DESCRIPTION_MAX_LENGTH + " characters");
		}
		this.description = description;
	}

	/** Two Day dates; either may be null. */
	public void schedule(LocalDate startDate, LocalDate endDate) {
		schedule(PlannedDate.of(startDate, DatePrecision.DAY), PlannedDate.of(endDate, DatePrecision.DAY));
	}

	/**
	 * Either date may be null (no date; its Precision is then Day). When both are set, the end period must not
	 * finish before the start period begins: start Q3 / end July is fine, start Q3 / end June is not.
	 */
	public void schedule(PlannedDate start, PlannedDate end) {
		if (start != null && end != null && end.last().isBefore(start.first())) {
			throw new ValidationException("End date must be on or after the start date");
		}
		this.startDate = start == null ? null : start.first();
		this.startPrecision = start == null ? DatePrecision.DAY : start.precision();
		this.endDate = end == null ? null : end.first();
		this.endPrecision = end == null ? DatePrecision.DAY : end.precision();
	}

	/** Becoming Shipped records `now` as the Shipped date; staying Shipped keeps it; leaving Shipped clears it. */
	public void changeStatus(ItemStatus status, Instant now) {
		if (status == null) {
			throw new ValidationException("Status is required");
		}
		if (status == ItemStatus.SHIPPED && this.status != ItemStatus.SHIPPED) {
			shippedAt = now;
		}
		if (status != ItemStatus.SHIPPED) {
			shippedAt = null;
		}
		this.status = status;
	}

	/** Null removes the Category. */
	public void categorize(Category category) {
		this.category = category;
	}

	public void markInternal(boolean internal) {
		this.internal = internal;
	}

	void moveTo(int position) {
		this.position = position;
	}
}
