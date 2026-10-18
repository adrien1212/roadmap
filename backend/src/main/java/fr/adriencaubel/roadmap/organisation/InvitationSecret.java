package fr.adriencaubel.roadmap.organisation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * The secret in an Invitation link: 256 random bits, base64url without padding (43 chars).
 * Only its SHA-256 is stored; a high-entropy secret needs no slow hash.
 */
final class InvitationSecret {

	private static final SecureRandom RANDOM = new SecureRandom();

	private InvitationSecret() {
	}

	static String generate() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hash(String secret) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is always available", e);
		}
	}
}
