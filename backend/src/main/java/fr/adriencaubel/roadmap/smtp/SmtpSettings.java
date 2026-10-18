package fr.adriencaubel.roadmap.smtp;

import fr.adriencaubel.roadmap.common.EmailAddress;
import fr.adriencaubel.roadmap.common.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * The Organisation's SMTP server, for emails to subscribed Readers and Invitations. One per Organisation.
 * The password is stored sealed.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmtpSettings {

	static final int HOST_MAX_LENGTH = 253;
	static final int NAME_MAX_LENGTH = 100;
	static final int ERROR_MAX_LENGTH = 500;

	@Id
	private Long organisationId;

	@Column(nullable = false)
	private String host;

	@Column(nullable = false)
	private int port;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SmtpSecurity security;

	private String username;

	@Getter(AccessLevel.NONE)
	private byte[] passwordEncrypted;

	private String fromName;

	@Column(nullable = false)
	private String fromEmail;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SmtpState state;

	private String lastError;

	private Instant testedAt;

	public static SmtpSettings forOrganisation(Long organisationId) {
		SmtpSettings settings = new SmtpSettings();
		settings.organisationId = organisationId;
		settings.state = SmtpState.UNTESTED;
		return settings;
	}

	/** Any change needs a new test before Readers can subscribe again. */
	public void change(String host, int port, SmtpSecurity security, String username, String fromName, String fromEmail) {
		String trimmedHost = host == null ? "" : host.strip();
		if (trimmedHost.isEmpty() || trimmedHost.length() > HOST_MAX_LENGTH) {
			throw new ValidationException("SMTP host is required");
		}
		if (!SmtpServerGuard.ALLOWED_PORTS.contains(port)) {
			throw new ValidationException("Port must be 25, 465, 587 or 2525");
		}
		if (security == null) {
			throw new ValidationException("Security is required");
		}
		String name = blankToNull(fromName);
		if (name != null && name.length() > NAME_MAX_LENGTH) {
			throw new ValidationException("Sender name must be at most " + NAME_MAX_LENGTH + " characters");
		}
		this.host = trimmedHost;
		this.port = port;
		this.security = security;
		this.username = blankToNull(username);
		this.fromName = name;
		this.fromEmail = EmailAddress.normalise(fromEmail);
		needsTest();
	}

	public void replacePassword(byte[] sealed) {
		this.passwordEncrypted = sealed;
		needsTest();
	}

	public boolean hasPassword() {
		return passwordEncrypted != null;
	}

	/** The sealed password, for SecretBox only. */
	byte[] sealedPassword() {
		return passwordEncrypted;
	}

	public void markConnected(Instant now) {
		this.state = SmtpState.CONNECTED;
		this.testedAt = now;
		this.lastError = null;
	}

	public void markNotWorking(String error) {
		this.state = SmtpState.NOT_WORKING;
		String message = error == null || error.isBlank() ? "The SMTP server refused the email." : error.strip();
		this.lastError = message.length() > ERROR_MAX_LENGTH ? message.substring(0, ERROR_MAX_LENGTH) : message;
	}

	private void needsTest() {
		this.state = SmtpState.UNTESTED;
		this.testedAt = null;
		this.lastError = null;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
