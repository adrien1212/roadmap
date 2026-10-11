package fr.adriencaubel.roadmap.smtp;

import fr.adriencaubel.roadmap.common.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmtpSettingsTest {

	private SmtpSettings configured() {
		SmtpSettings settings = SmtpSettings.forOwner(1L);
		settings.change(" smtp.acme.com ", 587, SmtpSecurity.STARTTLS, " updates@acme.com ", " Acme ", " Updates@Acme.com ");
		return settings;
	}

	@Test
	void normalisesWhatTheOwnerTyped() {
		SmtpSettings settings = configured();

		assertThat(settings.getHost()).isEqualTo("smtp.acme.com");
		assertThat(settings.getUsername()).isEqualTo("updates@acme.com");
		assertThat(settings.getFromName()).isEqualTo("Acme");
		assertThat(settings.getFromEmail()).isEqualTo("updates@acme.com");
	}

	@Test
	void needsATestAfterEveryChange() {
		SmtpSettings settings = configured();
		settings.markConnected(Instant.parse("2026-10-11T10:00:00Z"));

		settings.change("smtp.acme.com", 465, SmtpSecurity.SSL, "updates@acme.com", "Acme", "updates@acme.com");

		assertThat(settings.getState()).isEqualTo(SmtpState.UNTESTED);
		assertThat(settings.getTestedAt()).isNull();
	}

	@Test
	void needsATestAfterANewPassword() {
		SmtpSettings settings = configured();
		settings.markConnected(Instant.parse("2026-10-11T10:00:00Z"));

		settings.replacePassword(new byte[] {1, 2, 3});

		assertThat(settings.getState()).isEqualTo(SmtpState.UNTESTED);
		assertThat(settings.hasPassword()).isTrue();
	}

	@Test
	void recordsASuccessfulTest() {
		SmtpSettings settings = configured();

		settings.markConnected(Instant.parse("2026-10-11T10:00:00Z"));

		assertThat(settings.getState()).isEqualTo(SmtpState.CONNECTED);
		assertThat(settings.getTestedAt()).isEqualTo(Instant.parse("2026-10-11T10:00:00Z"));
		assertThat(settings.getLastError()).isNull();
	}

	@Test
	void recordsAFailureWithAShortError() {
		SmtpSettings settings = configured();

		settings.markNotWorking("x".repeat(900));

		assertThat(settings.getState()).isEqualTo(SmtpState.NOT_WORKING);
		assertThat(settings.getLastError()).hasSize(500);
	}

	@Test
	void allowsOnlySmtpPorts() {
		assertThatThrownBy(() -> SmtpSettings.forOwner(1L)
				.change("smtp.acme.com", 22, SmtpSecurity.STARTTLS, null, null, "updates@acme.com"))
				.isInstanceOf(ValidationException.class);
	}

	@Test
	void requiresAHostAndAValidSenderEmail() {
		assertThatThrownBy(() -> SmtpSettings.forOwner(1L)
				.change(" ", 587, SmtpSecurity.STARTTLS, null, null, "updates@acme.com"))
				.isInstanceOf(ValidationException.class);
		assertThatThrownBy(() -> SmtpSettings.forOwner(1L)
				.change("smtp.acme.com", 587, SmtpSecurity.STARTTLS, null, null, "not-an-email"))
				.isInstanceOf(ValidationException.class);
	}

	@Test
	void requiresASecurityMode() {
		assertThatThrownBy(() -> SmtpSettings.forOwner(1L)
				.change("smtp.acme.com", 587, null, null, null, "updates@acme.com"))
				.isInstanceOf(ValidationException.class);
	}

	@Test
	void treatsBlankOptionalFieldsAsNone() {
		SmtpSettings settings = SmtpSettings.forOwner(1L);

		settings.change("smtp.acme.com", 587, SmtpSecurity.STARTTLS, "  ", "", "updates@acme.com");

		assertThat(settings.getUsername()).isNull();
		assertThat(settings.getFromName()).isNull();
	}
}
