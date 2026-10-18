package fr.adriencaubel.roadmap.owner;

import fr.adriencaubel.roadmap.common.ConflictException;
import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.organisation.Organisation;
import fr.adriencaubel.roadmap.organisation.Role;
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

/** A person signed in with a provider; identified by (provider, providerSubject). */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Owner {

	static final int BRAND_MAX_LENGTH = 60;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String provider;

	@Column(nullable = false)
	private String providerSubject;

	private String email;

	private String name;

	@Column(nullable = false)
	private Instant createdAt;

	/** Whether the provider vouches for the email; an Invitation is accepted only with a verified one. */
	private boolean emailVerified;

	/** The product name Readers see on Public pages; optional, never taken from the Google profile. */
	private String brandName;

	/** At most one; null until the Owner creates or joins one, or after they leave. */
	@ManyToOne(fetch = FetchType.LAZY)
	private Organisation organisation;

	/** The Role in {@link #organisation}; null without one. */
	@Enumerated(EnumType.STRING)
	private Role role;

	private Instant joinedAt;

	public Owner(String provider, String providerSubject, String email, String name) {
		this.provider = provider;
		this.providerSubject = providerSubject;
		this.email = email;
		this.name = name;
		this.createdAt = Instant.now();
	}

	/** The email and name the provider currently has for this person, taken at each sign-in. Never the Brand. */
	public void refreshProfile(String email, String name, boolean emailVerified) {
		this.email = email;
		this.name = name;
		this.emailVerified = emailVerified;
	}

	/** Creating an Organisation joins it as {@link Role#OWNER}; an Invitation, with its Role. */
	public void joinAs(Organisation organisation, Role role, Instant now) {
		if (this.organisation != null) {
			throw new ConflictException("You already belong to " + this.organisation.getName());
		}
		this.organisation = organisation;
		this.role = role;
		this.joinedAt = now;
	}

	/** Between Admin and Editor only: the Role Owner never changes, and nobody becomes Owner. */
	public void changeRole(Role role) {
		if (this.role == Role.OWNER) {
			throw new ConflictException("The Owner's Role cannot change");
		}
		if (role == Role.OWNER) {
			throw new ConflictException("Nobody becomes Owner by a Role change");
		}
		this.role = role;
	}

	/** Removed or leaving: what they created stays with the Organisation. The Role Owner cannot leave. */
	public void leave() {
		if (this.role == Role.OWNER) {
			throw new ConflictException("The Owner cannot leave the Organisation");
		}
		this.organisation = null;
		this.role = null;
		this.joinedAt = null;
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
