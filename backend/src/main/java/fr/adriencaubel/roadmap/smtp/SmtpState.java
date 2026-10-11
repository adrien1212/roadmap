package fr.adriencaubel.roadmap.smtp;

import java.util.Locale;

/** Whether emails to Readers can be sent. Saved settings without a row are "none". */
public enum SmtpState {
	/** Saved but not tested since the last change: subscriptions paused. */
	UNTESTED,
	/** The last test email or send succeeded. */
	CONNECTED,
	/** The last test or send failed: subscriptions paused until a test succeeds. */
	NOT_WORKING;

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}
}
