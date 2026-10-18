package fr.adriencaubel.roadmap.roadmap;

import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.organisation.Organisation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A named, publishable timeline of Items; Published when publishedAt is set, Private otherwise. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Roadmap {

	static final String DEFAULT_NAME = "Untitled Roadmap";
	static final int NAME_MAX_LENGTH = 120;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Organisation organisation;

	@Column(nullable = false)
	private String name;

	/** Set once at creation, never changes. */
	@Column(nullable = false, updatable = false)
	private String publicId;

	private Instant publishedAt;

	private Roadmap(Organisation organisation, String name) {
		this.organisation = organisation;
		this.publicId = PublicId.generate();
		rename(name);
	}

	/** A new, Private Roadmap; a null name gives "Untitled Roadmap". */
	public static Roadmap create(Organisation organisation, String name) {
		return new Roadmap(organisation, name == null ? DEFAULT_NAME : name);
	}

	public void rename(String name) {
		String trimmed = name == null ? "" : name.strip();
		if (trimmed.isEmpty()) {
			throw new ValidationException("Name is required");
		}
		if (trimmed.length() > NAME_MAX_LENGTH) {
			throw new ValidationException("Name must be at most " + NAME_MAX_LENGTH + " characters");
		}
		this.name = trimmed;
	}

	/** Makes the Roadmap Published; on an already Published one, publishedAt only moves forward. */
	public void publish(Instant now) {
		if (publishedAt == null || now.isAfter(publishedAt)) {
			publishedAt = now;
		}
	}

	/** Makes the Roadmap Private; its publicId is kept for a later republish. */
	public void unpublish() {
		publishedAt = null;
	}
}
