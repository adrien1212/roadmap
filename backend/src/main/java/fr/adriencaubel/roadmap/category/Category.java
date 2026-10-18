package fr.adriencaubel.roadmap.category;

import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.organisation.Organisation;
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

/** A name + colour label, owned by an Organisation and shared by all its Roadmaps. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

	static final int NAME_MAX_LENGTH = 60;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Organisation organisation;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CategoryColour colour;

	public Category(Organisation organisation, String name, CategoryColour colour) {
		this.organisation = organisation;
		rename(name);
		recolour(colour);
	}

	public void rename(String name) {
		this.name = normalizeName(name);
	}

	public void recolour(CategoryColour colour) {
		if (colour == null) {
			throw new ValidationException("Colour is required");
		}
		this.colour = colour;
	}

	/** Trimmed, non-blank, at most {@value NAME_MAX_LENGTH} characters. */
	static String normalizeName(String name) {
		String trimmed = name == null ? "" : name.strip();
		if (trimmed.isEmpty()) {
			throw new ValidationException("Name is required");
		}
		if (trimmed.length() > NAME_MAX_LENGTH) {
			throw new ValidationException("Name must be at most " + NAME_MAX_LENGTH + " characters");
		}
		return trimmed;
	}
}
