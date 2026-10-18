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

	@Test
	void trimsTheBrand() {
		Organisation organisation = new Organisation("Acme product team");

		organisation.changeBrand("  Acme  ");

		assertThat(organisation.getBrandName()).isEqualTo("Acme");
	}

	@Test
	void clearsTheBrandWhenBlankOrNull() {
		Organisation organisation = new Organisation("Acme product team");
		organisation.changeBrand("Acme");
		organisation.changeBrand("   ");
		assertThat(organisation.getBrandName()).isNull();

		organisation.changeBrand("Acme");
		organisation.changeBrand(null);
		assertThat(organisation.getBrandName()).isNull();
	}

	@Test
	void limitsTheBrandTo60Characters() {
		Organisation organisation = new Organisation("Acme product team");
		organisation.changeBrand("a".repeat(60));
		assertThat(organisation.getBrandName()).hasSize(60);

		assertThatThrownBy(() -> organisation.changeBrand("b".repeat(61))).isInstanceOf(ValidationException.class);
		assertThat(organisation.getBrandName()).isEqualTo("a".repeat(60));
	}

	@Test
	void keepsItsBrandWhenRenamed() {
		Organisation organisation = new Organisation("Acme product team");
		organisation.changeBrand("Acme");

		organisation.rename("Acme");

		assertThat(organisation.getBrandName()).isEqualTo("Acme");
	}
}
