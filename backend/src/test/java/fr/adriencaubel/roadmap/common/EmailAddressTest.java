package fr.adriencaubel.roadmap.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressTest {

	@Test
	void trimsAndLowerCases() {
		assertThat(EmailAddress.normalise("  Jane.Doe@Example.COM ")).isEqualTo("jane.doe@example.com");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "jane", "jane@", "@example.com", "jane@example", "ja ne@example.com",
			"jane@exa mple.com", "jane@@example.com", "jane@example.com\r\nBcc: x@y.com"})
	void rejectsImplausibleAddresses(String input) {
		assertThatThrownBy(() -> EmailAddress.normalise(input)).isInstanceOf(ValidationException.class);
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> EmailAddress.normalise(null)).isInstanceOf(ValidationException.class);
	}

	@Test
	void limitsTheLengthTo254() {
		String local = "a".repeat(64);
		String domain = "b".repeat(185) + ".com"; // 64 + 1 + 189 = 254
		assertThat(EmailAddress.normalise(local + "@" + domain)).hasSize(254);
		assertThatThrownBy(() -> EmailAddress.normalise(local + "@x" + domain)).isInstanceOf(ValidationException.class);
	}
}
