package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.owner.Owner;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvitationTest {

	private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

	private final Organisation organisation = new Organisation("Acme product team");
	private final Owner ada = new Owner("google", "1", "ada@acme.com", "Ada Lovelace");
	private final String secret = InvitationSecret.generate();

	private Invitation invitation() {
		return Invitation.create(organisation, ada, " Jane@Acme.com ", Role.EDITOR, secret, NOW);
	}

	@Test
	void isForOneNormalisedEmailWithARole() {
		Invitation invitation = invitation();

		assertThat(invitation.getEmail()).isEqualTo("jane@acme.com");
		assertThat(invitation.getRole()).isEqualTo(Role.EDITOR);
		assertThat(invitation.getInvitedBy()).isSameAs(ada);
		assertThat(invitation.getOrganisation()).isSameAs(organisation);
	}

	@Test
	void neverMakesSomeoneOwner() {
		assertThatThrownBy(() -> Invitation.create(organisation, ada, "jane@acme.com", Role.OWNER, secret, NOW))
				.isInstanceOf(ValidationException.class);
	}

	@Test
	void rejectsAnImplausibleEmail() {
		assertThatThrownBy(() -> Invitation.create(organisation, ada, "jane", Role.EDITOR, secret, NOW))
				.isInstanceOf(ValidationException.class);
	}

	@Test
	void expiresSevenDaysAfterCreation() {
		Invitation invitation = invitation();

		assertThat(invitation.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
		assertThat(invitation.isPending(NOW.plus(Duration.ofDays(7)).minusMillis(1))).isTrue();
		assertThat(invitation.isPending(NOW.plus(Duration.ofDays(7)))).isFalse();
	}

	@Test
	void isNoLongerPendingOnceRevoked() {
		Invitation invitation = invitation();

		invitation.revoke(NOW);

		assertThat(invitation.isPending(NOW)).isFalse();
		assertThat(invitation.getRevokedAt()).isEqualTo(NOW);
	}

	@Test
	void storesOnlyAHashOfTheSecret() {
		Invitation invitation = invitation();

		assertThat(invitation.getSecretHash()).isEqualTo(InvitationSecret.hash(secret)).doesNotContain(secret);
		assertThat(invitation.getSecretHash()).matches("[0-9a-f]{64}");
	}

	@Test
	void secretsAreLongRandomAndUrlSafe() {
		String other = InvitationSecret.generate();

		assertThat(secret).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(other);
		assertThat(InvitationSecret.hash(secret)).isNotEqualTo(InvitationSecret.hash(other));
	}

	@Test
	void isForTheGoogleAccountWithTheSameVerifiedEmail() {
		Invitation invitation = invitation();
		Owner jane = new Owner("google", "2", "JANE@acme.com", "Jane");

		assertThat(invitation.isFor(jane)).isFalse();

		jane.refreshProfile("JANE@acme.com", "Jane", true);
		assertThat(invitation.isFor(jane)).isTrue();

		jane.refreshProfile("jane@other.com", "Jane", true);
		assertThat(invitation.isFor(jane)).isFalse();
	}

	@Test
	void masksItsEmail() {
		assertThat(invitation().maskedEmail()).isEqualTo("j***@acme.com");
	}
}
