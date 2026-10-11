package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.owner.Owner;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

	private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");

	private final Roadmap roadmap = Roadmap.create(new Owner("dev", "dev", null, null), "Q3");

	private Subscription pending() {
		return Subscription.pending(roadmap, "  Jane@Example.com ", NOW);
	}

	@Test
	void startsPendingWithANormalisedEmail() {
		Subscription subscription = pending();

		assertThat(subscription.getEmail()).isEqualTo("jane@example.com");
		assertThat(subscription.getState()).isEqualTo(SubscriptionState.PENDING);
		assertThat(subscription.isConfirmed()).isFalse();
		assertThat(subscription.getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void rejectsAnImplausibleEmail() {
		assertThatThrownBy(() -> Subscription.pending(roadmap, "jane", NOW)).isInstanceOf(ValidationException.class);
	}

	@Test
	void getsARandomUrlSafeToken() {
		Set<String> tokens = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			tokens.add(pending().getToken());
		}
		assertThat(tokens).hasSize(1000).allMatch(t -> t.matches("[A-Za-z0-9_-]{22}"));
	}

	@Test
	void becomesConfirmed() {
		Subscription subscription = pending();

		subscription.confirm(NOW.plusSeconds(60));

		assertThat(subscription.isConfirmed()).isTrue();
		assertThat(subscription.getConfirmedAt()).isEqualTo(NOW.plusSeconds(60));
	}

	@Test
	void keepsItsFirstConfirmationDate() {
		Subscription subscription = pending();
		subscription.confirm(NOW.plusSeconds(60));

		subscription.confirm(NOW.plusSeconds(600));

		assertThat(subscription.getConfirmedAt()).isEqualTo(NOW.plusSeconds(60));
	}

	@Test
	void expiresWhenPendingForMoreThanSevenDays() {
		Subscription subscription = pending();

		assertThat(subscription.isExpired(NOW.plus(Duration.ofDays(7)))).isFalse();
		assertThat(subscription.isExpired(NOW.plus(Duration.ofDays(7)).plusSeconds(1))).isTrue();
	}

	@Test
	void neverExpiresOnceConfirmed() {
		Subscription subscription = pending();
		subscription.confirm(NOW);

		assertThat(subscription.isExpired(NOW.plus(Duration.ofDays(365)))).isFalse();
	}

	@Test
	void sendsAtMostOneConfirmationEmailPerTenMinutes() {
		Subscription subscription = pending();
		assertThat(subscription.mayResendConfirmation(NOW)).isTrue();

		subscription.confirmationSent(NOW);

		assertThat(subscription.mayResendConfirmation(NOW.plus(Duration.ofMinutes(9)))).isFalse();
		assertThat(subscription.mayResendConfirmation(NOW.plus(Duration.ofMinutes(10)))).isTrue();
	}

	@Test
	void sendsNoConfirmationEmailOnceConfirmed() {
		Subscription subscription = pending();
		subscription.confirm(NOW);

		assertThat(subscription.mayResendConfirmation(NOW.plus(Duration.ofDays(1)))).isFalse();
	}

	@Test
	void restartsAnExpiredPendingSubscriptionWithANewToken() {
		Subscription subscription = pending();
		subscription.confirmationSent(NOW);
		String oldToken = subscription.getToken();
		Instant later = NOW.plus(Duration.ofDays(8));

		subscription.restart(later);

		assertThat(subscription.getToken()).isNotEqualTo(oldToken);
		assertThat(subscription.getCreatedAt()).isEqualTo(later);
		assertThat(subscription.isExpired(later)).isFalse();
		assertThat(subscription.mayResendConfirmation(later)).isTrue();
	}
}
