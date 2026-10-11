package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.smtp.OutgoingEmail;

/** What Readers receive. Owner text is HTML-escaped; subjects are kept on one line. */
final class EmailContent {

	private EmailContent() {
	}

	static OutgoingEmail confirmation(String to, String roadmapName, String brandName, String confirmUrl) {
		String who = brandName != null ? brandName : roadmapName;
		String subject = oneLine("Confirm your email for " + roadmapName);
		String text = """
				Confirm that you want an email from %s each time an Item of %s ships:

				%s

				If you didn't ask for this, ignore this email: you won't be subscribed.
				""".formatted(who, roadmapName, confirmUrl);
		String html = """
				<p>Confirm that you want an email from %s each time an Item of <strong>%s</strong> ships.</p>
				<p><a href="%s">Confirm my email</a></p>
				<p style="color:#6b7280">If you didn't ask for this, ignore this email: you won't be subscribed.</p>
				""".formatted(escape(who), escape(roadmapName), escape(confirmUrl));
		return new OutgoingEmail(to, subject, text, html);
	}

	static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}

	/** No line breaks in headers: a title can't add headers (e.g. Bcc) to an email. */
	static String oneLine(String text) {
		return text.replaceAll("[\\r\\n]+", " ").strip();
	}
}
