package fr.adriencaubel.roadmap.owner;

import fr.adriencaubel.roadmap.common.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnerTest {

	private final Owner owner = new Owner("google", "123", "jane@example.com", "Jane Doe");

	@Test
	void hasNoBrandUntilTheOwnerSetsOne() {
		assertThat(owner.getBrandName()).isNull();
	}

	@Test
	void trimsTheBrand() {
		owner.changeBrand("  Acme  ");

		assertThat(owner.getBrandName()).isEqualTo("Acme");
	}

	@Test
	void clearsTheBrandWhenBlankOrNull() {
		owner.changeBrand("Acme");
		owner.changeBrand("   ");
		assertThat(owner.getBrandName()).isNull();

		owner.changeBrand("Acme");
		owner.changeBrand(null);
		assertThat(owner.getBrandName()).isNull();
	}

	@Test
	void limitsTheBrandTo60Characters() {
		owner.changeBrand("a".repeat(60));
		assertThat(owner.getBrandName()).hasSize(60);

		assertThatThrownBy(() -> owner.changeBrand("b".repeat(61))).isInstanceOf(ValidationException.class);
		assertThat(owner.getBrandName()).isEqualTo("a".repeat(60));
	}

	@Test
	void isNeverTakenFromTheGoogleProfile() {
		assertThat(owner.getBrandName()).isNotEqualTo(owner.getName());
	}
}
