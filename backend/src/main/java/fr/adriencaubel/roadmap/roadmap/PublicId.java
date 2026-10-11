package fr.adriencaubel.roadmap.roadmap;

import java.security.SecureRandom;
import java.util.Base64;

/** The unguessable id in a Public page URL: 128 random bits, base64url without padding (22 chars). */
final class PublicId {

	private static final SecureRandom RANDOM = new SecureRandom();

	private PublicId() {
	}

	static String generate() {
		byte[] bytes = new byte[16];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
