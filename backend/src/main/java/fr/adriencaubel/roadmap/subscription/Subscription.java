package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.common.EmailAddress;
import fr.adriencaubel.roadmap.item.Item;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
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

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * A Reader's email address on one Published Roadmap (a Roadmap Subscription), or on one Item of it
 * (an Item Subscription, when `item` is set): pending until confirmed from the email.
 * Unsubscribing deletes it (the address is not kept). The token is the Reader's only key to it.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

	static final Duration PENDING_LIFETIME = Duration.ofDays(7);
	static final Duration CONFIRMATION_RESEND_DELAY = Duration.ofMinutes(10);
	private static final SecureRandom RANDOM = new SecureRandom();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Roadmap roadmap;

	/** Null for a Roadmap Subscription; for an Item Subscription, the one Item it covers (of `roadmap`). */
	@ManyToOne(fetch = FetchType.LAZY)
	private Item item;

	@Column(nullable = false)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SubscriptionState state;

	/** 128 random bits, base64url: confirms and unsubscribes. */
	@Column(nullable = false, updatable = false)
	private String token;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant confirmedAt;

	/** The last email sent in answer to a subscribe request: the confirmation, or "already subscribed". */
	private Instant confirmationSentAt;

	public static Subscription pending(Roadmap roadmap, String email, Instant now) {
		Subscription subscription = new Subscription();
		subscription.roadmap = roadmap;
		subscription.email = EmailAddress.normalise(email);
		subscription.state = SubscriptionState.PENDING;
		subscription.token = newToken();
		subscription.createdAt = now;
		return subscription;
	}

	/** "Notify me when this ships": emailed only when this Item becomes Shipped. */
	public static Subscription pendingForItem(Item item, String email, Instant now) {
		Subscription subscription = pending(item.getRoadmap(), email, now);
		subscription.item = item;
		return subscription;
	}

	public boolean isForItem() {
		return item != null;
	}

	public void confirm(Instant now) {
		if (state != SubscriptionState.CONFIRMED) {
			state = SubscriptionState.CONFIRMED;
			confirmedAt = now;
		}
	}

	public boolean isConfirmed() {
		return state == SubscriptionState.CONFIRMED;
	}

	/** Pending for more than 7 days: deleted, and its token no longer works. */
	public boolean isExpired(Instant now) {
		return state == SubscriptionState.PENDING && now.isAfter(createdAt.plus(PENDING_LIFETIME));
	}

	/** At most one confirmation email per 10 minutes, and none once confirmed. */
	public boolean mayResendConfirmation(Instant now) {
		return state == SubscriptionState.PENDING
				&& (confirmationSentAt == null || !now.isBefore(confirmationSentAt.plus(CONFIRMATION_RESEND_DELAY)));
	}

	/** An expired pending Subscription asked for again: a fresh start, with a new token. */
	public void restart(Instant now) {
		token = newToken();
		createdAt = now;
		confirmationSentAt = null;
	}

	public void confirmationSent(Instant now) {
		confirmationSentAt = now;
	}

	/** A confirmed Reader asked again: at most one "already subscribed" email per 10 minutes, to their inbox only. */
	public boolean mayRemindSubscribed(Instant now) {
		return state == SubscriptionState.CONFIRMED;
				// && (confirmationSentAt == null || !now.isBefore(confirmationSentAt.plus(CONFIRMATION_RESEND_DELAY)));
	}

	public void remindedSubscribed(Instant now) {
		confirmationSentAt = now;
	}

	private static String newToken() {
		byte[] bytes = new byte[16];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
