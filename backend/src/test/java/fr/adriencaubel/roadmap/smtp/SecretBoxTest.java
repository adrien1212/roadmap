package fr.adriencaubel.roadmap.smtp;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretBoxTest {

	private static String key(int fill) {
		byte[] bytes = new byte[32];
		java.util.Arrays.fill(bytes, (byte) fill);
		return Base64.getEncoder().encodeToString(bytes);
	}

	private final SecretBox box = new SecretBox(key(7));

	@Test
	void decryptsWhatItEncrypted() {
		byte[] sealed = box.encrypt("s3cret-pässword");

		assertThat(box.decrypt(sealed)).isEqualTo("s3cret-pässword");
	}

	@Test
	void neverStoresThePlainText() {
		assertThat(new String(box.encrypt("s3cret"))).doesNotContain("s3cret");
	}

	@Test
	void sealsTheSameTextDifferentlyEachTime() {
		assertThat(box.encrypt("s3cret")).isNotEqualTo(box.encrypt("s3cret"));
	}

	@Test
	void rejectsATamperedValue() {
		byte[] sealed = box.encrypt("s3cret");
		sealed[sealed.length - 1] ^= 1;

		assertThatThrownBy(() -> box.decrypt(sealed)).isInstanceOf(SecretBox.SecretException.class);
	}

	@Test
	void rejectsAValueSealedWithAnotherKey() {
		byte[] sealed = new SecretBox(key(9)).encrypt("s3cret");

		assertThatThrownBy(() -> box.decrypt(sealed)).isInstanceOf(SecretBox.SecretException.class);
	}

	@Test
	void isUnavailableWithoutAKey() {
		SecretBox none = new SecretBox("");

		assertThat(none.isAvailable()).isFalse();
		assertThatThrownBy(() -> none.encrypt("s3cret")).isInstanceOf(SecretBox.SecretUnavailableException.class);
	}

	@Test
	void refusesAKeyThatIsNot256Bits() {
		String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

		assertThatThrownBy(() -> new SecretBox(shortKey)).isInstanceOf(IllegalArgumentException.class);
	}
}
