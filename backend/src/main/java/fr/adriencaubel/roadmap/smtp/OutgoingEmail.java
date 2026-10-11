package fr.adriencaubel.roadmap.smtp;

import java.util.Map;

/** One email to one recipient, with a plain-text and an HTML part. */
public record OutgoingEmail(String to, String subject, String text, String html, Map<String, String> headers) {

	public OutgoingEmail(String to, String subject, String text, String html) {
		this(to, subject, text, html, Map.of());
	}
}
