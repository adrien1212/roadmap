package fr.adriencaubel.roadmap.common;

import java.util.Locale;
import java.util.regex.Pattern;

/** A plausible email address, trimmed and lower-cased. Not proof it exists: confirmation emails are. */
public final class EmailAddress {

	static final int MAX_LENGTH = 254;
	private static final Pattern PLAUSIBLE = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

	private EmailAddress() {
	}

	public static String normalise(String input) {
		String email = input == null ? "" : input.strip().toLowerCase(Locale.ROOT);
		if (email.length() > MAX_LENGTH || !PLAUSIBLE.matcher(email).matches()) {
			throw new ValidationException("Enter an email address, like you@example.com.");
		}
		return email;
	}
}
