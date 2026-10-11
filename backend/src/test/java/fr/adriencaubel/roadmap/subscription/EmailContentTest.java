package fr.adriencaubel.roadmap.subscription;

import fr.adriencaubel.roadmap.smtp.OutgoingEmail;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailContentTest {

	private static final String CONFIRM = "https://example.com/r/abc/confirm?token=t0k3n";

	@Test
	void asksTheReaderToConfirm() {
		OutgoingEmail email = EmailContent.confirmation("jane@example.com", "Product roadmap 2026", "Acme", CONFIRM);

		assertThat(email.to()).isEqualTo("jane@example.com");
		assertThat(email.subject()).isEqualTo("Confirm your email for Product roadmap 2026");
		assertThat(email.html()).contains("href=\"" + CONFIRM + "\"").contains("Acme");
		assertThat(email.text()).contains(CONFIRM).contains("Acme");
	}

	@Test
	void namesTheRoadmapWhenThereIsNoBrand() {
		OutgoingEmail email = EmailContent.confirmation("jane@example.com", "Product roadmap 2026", null, CONFIRM);

		assertThat(email.text()).doesNotContain("null");
		assertThat(email.html()).doesNotContain("null");
	}

	@Test
	void escapesOwnerTextInHtml() {
		OutgoingEmail email = EmailContent.confirmation("jane@example.com", "<script>alert(1)</script>", "A & B", CONFIRM);

		assertThat(email.html()).doesNotContain("<script>").contains("&lt;script&gt;").contains("A &amp; B");
	}

	@Test
	void keepsSubjectsOnOneLine() {
		OutgoingEmail email = EmailContent.confirmation("jane@example.com", "Q3\r\nBcc: x@y.com", null, CONFIRM);

		assertThat(email.subject()).doesNotContain("\r").doesNotContain("\n");
	}
}
