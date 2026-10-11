package fr.adriencaubel.roadmap.roadmap;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PublicIdTest {

	@Test
	void isUrlSafeWithoutPadding() {
		assertThat(PublicId.generate()).matches("[A-Za-z0-9_-]{22}");
	}

	@Test
	void carries128RandomBits() {
		assertThat(Base64.getUrlDecoder().decode(PublicId.generate())).hasSize(16);
	}

	@Test
	void differsOnEveryCall() {
		Set<String> ids = new HashSet<>();
		for (int i = 0; i < 10_000; i++) {
			ids.add(PublicId.generate());
		}
		assertThat(ids).hasSize(10_000);
	}
}
