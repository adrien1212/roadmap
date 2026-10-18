package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.common.EmailAddress;
import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.owner.Owner;
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

import java.time.Duration;
import java.time.Instant;

/**
 * A single-use link for one email address, with a Role. Pending until accepted, revoked, replaced
 * (revoked by a newer one for the same email) or expired, 7 days after creation.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation {

	static final Duration LIFETIME = Duration.ofDays(7);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Organisation organisation;

	@Column(nullable = false)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Owner invitedBy;

	/** SHA-256 of the secret in the link; the secret itself is never stored. */
	@Column(nullable = false, updatable = false)
	private String secretHash;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant usedAt;

	private Instant revokedAt;

	public static Invitation create(Organisation organisation, Owner invitedBy, String email, Role role, String secret,
			Instant now) {
		if (role == null || !role.invitable()) {
			throw new ValidationException("An Invitation is for an Admin or an Editor");
		}
		Invitation invitation = new Invitation();
		invitation.organisation = organisation;
		invitation.invitedBy = invitedBy;
		invitation.email = EmailAddress.normalise(email);
		invitation.role = role;
		invitation.secretHash = InvitationSecret.hash(secret);
		invitation.createdAt = now;
		invitation.expiresAt = now.plus(LIFETIME);
		return invitation;
	}

	public boolean isPending(Instant now) {
		return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
	}

	/** Only the Google account whose verified email is the Invitation's, ignoring case. */
	public boolean isFor(Owner owner) {
		return owner.isEmailVerified() && owner.getEmail() != null && email.equalsIgnoreCase(owner.getEmail());
	}

	/** "j***@acme.com": enough for the invited person to recognise it. */
	public String maskedEmail() {
		int at = email.indexOf('@');
		return email.charAt(0) + "***" + email.substring(at);
	}

	/** Revoked by an Owner, or replaced by a newer Invitation for the same email. */
	public void revoke(Instant now) {
		if (usedAt == null && revokedAt == null) {
			revokedAt = now;
		}
	}
}
