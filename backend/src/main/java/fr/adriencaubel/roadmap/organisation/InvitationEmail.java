package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.smtp.EmailText;
import fr.adriencaubel.roadmap.smtp.OutgoingEmail;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** What an invited person receives. Names are HTML-escaped; the subject is kept on one line. */
final class InvitationEmail {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
			.withZone(ZoneOffset.UTC);

	private InvitationEmail() {
	}

	static OutgoingEmail of(String to, String inviterName, String organisationName, Role role, String url,
			Instant expiresAt) {
		String roleName = role == Role.ADMIN ? "Admin" : "Editor";
		String expires = DAY.format(expiresAt);
		String subject = EmailText.oneLine(inviterName + " invited you to " + organisationName);
		String text = """
				%s invited you to join %s on Product name, as %s.

				Accept the invitation (it works only for %s, until %s):

				%s
				""".formatted(inviterName, organisationName, roleName, to, expires, url);
		String html = """
				<p>%s invited you to join <strong>%s</strong> on Product name, as %s.</p>
				<p><a href="%s">Accept the invitation</a></p>
				<p style="color:#6b7280">It works only for %s, until %s.</p>
				""".formatted(EmailText.escape(inviterName), EmailText.escape(organisationName), roleName,
				EmailText.escape(url), EmailText.escape(to), expires);
		return new OutgoingEmail(to, subject, text, html);
	}
}
