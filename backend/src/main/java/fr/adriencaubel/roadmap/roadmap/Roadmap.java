package fr.adriencaubel.roadmap.roadmap;

import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.owner.Owner;
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
	private Owner owner;

	@Column(nullable = false)
	private String name;

	/** Set once at creation, never changes. */
	@Column(nullable = false, updatable = false)
	private String publicId;

	private Instant publishedAt;

	/** Readers who unsubscribed or were removed; their addresses are deleted, only the count is kept. */
	@Column(nullable = false)
	private int unsubscribedCount;

	private Roadmap(Owner owner, String name) {
		this.owner = owner;
		this.publicId = PublicId.generate();
		rename(name);
	}

	/** A new, Private Roadmap; a null name gives "Untitled Roadmap". */
	public static Roadmap create(Owner owner, String name) {
		return new Roadmap(owner, name == null ? DEFAULT_NAME : name);
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

	public void countUnsubscribe() {
		unsubscribedCount++;
	}
}
