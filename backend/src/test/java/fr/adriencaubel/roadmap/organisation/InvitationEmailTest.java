package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.smtp.OutgoingEmail;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationEmailTest {

	private static final String URL = "https://example.com/join/abc";
	private static final Instant EXPIRES = Instant.parse("2026-10-25T10:00:00Z");

	@Test
	void saysWhoInvitesToWhatWithWhichRoleUntilWhen() {
		OutgoingEmail email = InvitationEmail.of("jane@acme.com", "Ada Lovelace", "Acme product team", Role.EDITOR, URL,
				EXPIRES);

		assertThat(email.to()).isEqualTo("jane@acme.com");
		assertThat(email.subject()).isEqualTo("Ada Lovelace invited you to Acme product team");
		assertThat(email.text()).contains("Editor").contains(URL).contains("25 Oct 2026");
		assertThat(email.html()).contains("Editor").contains("href=\"" + URL + "\"").contains("25 Oct 2026");
	}

	@Test
	void escapesNamesInHtmlAndKeepsTheSubjectOnOneLine() {
		OutgoingEmail email = InvitationEmail.of("jane@acme.com", "Ada\r\nBcc: x@evil.example", "<b>Acme</b>", Role.ADMIN,
				URL, EXPIRES);

		assertThat(email.subject()).doesNotContain("\n").doesNotContain("\r");
		assertThat(email.html()).contains("&lt;b&gt;Acme&lt;/b&gt;").doesNotContain("<b>Acme</b>");
	}
}
