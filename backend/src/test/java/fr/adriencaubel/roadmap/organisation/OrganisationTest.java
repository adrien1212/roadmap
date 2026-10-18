package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.common.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganisationTest {

	@Test
	void trimsTheName() {
		Organisation organisation = new Organisation("  Acme product team  ");

		assertThat(organisation.getName()).isEqualTo("Acme product team");
	}

	@Test
	void requiresAName() {
		assertThatThrownBy(() -> new Organisation("   ")).isInstanceOf(ValidationException.class);
		assertThatThrownBy(() -> new Organisation(null)).isInstanceOf(ValidationException.class);
	}

	@Test
	void limitsTheNameTo80Characters() {
		Organisation organisation = new Organisation("a".repeat(80));
		assertThat(organisation.getName()).hasSize(80);

		assertThatThrownBy(() -> organisation.rename("b".repeat(81))).isInstanceOf(ValidationException.class);
		assertThat(organisation.getName()).isEqualTo("a".repeat(80));
	}

	@Test
	void hasNoBrandAtCreation() {
		assertThat(new Organisation("Acme").getBrandName()).isNull();
	}
}
