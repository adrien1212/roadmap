package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.common.ConflictException;
import fr.adriencaubel.roadmap.common.ForbiddenException;
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

	/** Set when its Role Owner closes it; its data is kept so it can be restored later. */
	private Instant closedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	private Owner closedBy;

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

	/**
	 * Only its Role Owner, typing its exact name. The caller then makes its Roadmaps Private, revokes its
	 * Invitations and takes everyone out of it; nothing is deleted.
	 */
	public void close(Owner by, String typedName, Instant now) {
		Organisation theirs = by.getOrganisation();
		boolean member = theirs == this || (id != null && theirs != null && id.equals(theirs.getId()));
		if (!member || by.getRole() != Role.OWNER) {
			throw new ForbiddenException("Only the Owner can close the Organisation");
		}
		if (closedAt != null) {
			throw new ConflictException(name + " is already closed");
		}
		if (typedName == null || !typedName.strip().equals(name)) {
			throw new ValidationException("Type " + name + " to close it");
		}
		this.closedAt = now;
		this.closedBy = by;
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
