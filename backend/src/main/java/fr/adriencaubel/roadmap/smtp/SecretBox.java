package fr.adriencaubel.roadmap.smtp;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts secrets at rest (the Owner's SMTP password) with AES-256-GCM.
 * A sealed value is a random 12-byte nonce followed by the ciphertext and its tag;
 * any change to it, or another key, makes decryption fail.
 */
public class SecretBox {

	private static final int NONCE_BYTES = 12;
	private static final int TAG_BITS = 128;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final SecretKeySpec key;

	/** @param base64Key 32 bytes, base64; blank means no key (secrets cannot be stored). */
	public SecretBox(String base64Key) {
		if (base64Key == null || base64Key.isBlank()) {
			this.key = null;
			return;
		}
		byte[] bytes = Base64.getDecoder().decode(base64Key.strip());
		if (bytes.length != 32) {
			throw new IllegalArgumentException("SMTP_SECRET_KEY must be 32 bytes, base64-encoded");
		}
		this.key = new SecretKeySpec(bytes, "AES");
	}

	public boolean isAvailable() {
		return key != null;
	}

	public byte[] encrypt(String plainText) {
		requireKey();
		byte[] nonce = new byte[NONCE_BYTES];
		RANDOM.nextBytes(nonce);
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
			byte[] sealed = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			return ByteBuffer.allocate(NONCE_BYTES + sealed.length).put(nonce).put(sealed).array();
		} catch (GeneralSecurityException e) {
			throw new SecretException("Could not encrypt", e);
		}
	}

	public String decrypt(byte[] value) {
		requireKey();
		if (value == null || value.length <= NONCE_BYTES) {
			throw new SecretException("Not a sealed value", null);
		}
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, value, 0, NONCE_BYTES));
			byte[] plain = cipher.doFinal(value, NONCE_BYTES, value.length - NONCE_BYTES);
			return new String(plain, StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new SecretException("Could not decrypt: wrong key or tampered value", e);
		}
	}

	private void requireKey() {
		if (key == null) {
			throw new SecretUnavailableException();
		}
	}

	public static class SecretException extends RuntimeException {
		SecretException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/** No SMTP_SECRET_KEY on this server. */
	public static class SecretUnavailableException extends RuntimeException {
		SecretUnavailableException() {
			super("Email to Readers is not available on this server");
		}
	}
}
