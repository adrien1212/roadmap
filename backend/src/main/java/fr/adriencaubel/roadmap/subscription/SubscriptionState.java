package fr.adriencaubel.roadmap.subscription;

import java.util.Locale;

public enum SubscriptionState {
	/** Waiting for the Reader to confirm from the email; never emailed about Items. */
	PENDING,
	/** A subscribed Reader. */
	CONFIRMED;

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}
}
