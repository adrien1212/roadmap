package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.common.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** The publisher's people who maintain the same Roadmaps; owns Roadmaps, Categories, the Brand and SMTP settings. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organisation {

	static final int NAME_MAX_LENGTH = 80;
	static final int BRAND_MAX_LENGTH = 60;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Seen only by its people, never by Readers. */
	@Column(nullable = false)
	private String name;

	/** The product name Readers see on Public pages; optional, never taken from a Google profile. */
	private String brandName;

	@Column(nullable = false)
	private Instant createdAt;

	public Organisation(String name) {
		rename(name);
		this.createdAt = Instant.now();
	}

	/** Trimmed, required, at most {@value NAME_MAX_LENGTH} characters. */
	public void rename(String name) {
		String trimmed = name == null ? "" : name.strip();
		if (trimmed.isEmpty()) {
			throw new ValidationException("Organisation name is required");
		}
		if (trimmed.length() > NAME_MAX_LENGTH) {
			throw new ValidationException("Organisation name must be at most " + NAME_MAX_LENGTH + " characters");
		}
		this.name = trimmed;
	}

	/** Trimmed, at most {@value BRAND_MAX_LENGTH} characters; blank or null clears the Brand. */
	public void changeBrand(String name) {
		String trimmed = name == null ? "" : name.strip();
		if (trimmed.length() > BRAND_MAX_LENGTH) {
			throw new ValidationException("Product name must be at most " + BRAND_MAX_LENGTH + " characters");
		}
		this.brandName = trimmed.isEmpty() ? null : trimmed;
	}
}
