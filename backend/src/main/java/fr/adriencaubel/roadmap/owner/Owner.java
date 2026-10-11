package fr.adriencaubel.roadmap.owner;

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

	/** The product name Readers see on Public pages; optional, never taken from the Google profile. */
	private String brandName;

	public Owner(String provider, String providerSubject, String email, String name) {
		this.provider = provider;
		this.providerSubject = providerSubject;
		this.email = email;
		this.name = name;
		this.createdAt = Instant.now();
	}

	/** The email and name the provider currently has for this person, taken at each sign-in. Never the Brand. */
	public void refreshProfile(String email, String name) {
		this.email = email;
		this.name = name;
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
