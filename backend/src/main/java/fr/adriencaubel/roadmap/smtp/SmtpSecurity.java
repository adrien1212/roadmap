package fr.adriencaubel.roadmap.smtp;

import fr.adriencaubel.roadmap.common.ValidationException;

import java.util.Locale;

/** How the connection to the SMTP server is secured. JSON value is the lowercase key. */
public enum SmtpSecurity {
	/** Plain connection upgraded with STARTTLS (usually port 587). */
	STARTTLS,
	/** TLS from the first byte (usually port 465). */
	SSL;

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static SmtpSecurity fromKey(String key) {
		for (SmtpSecurity security : values()) {
			if (security.key().equals(key)) {
				return security;
			}
		}
		throw new ValidationException("Security must be starttls or ssl");
	}
}
