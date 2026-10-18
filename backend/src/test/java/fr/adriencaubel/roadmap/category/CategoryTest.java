package fr.adriencaubel.roadmap.category;

import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.organisation.Organisation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

	private final Organisation organisation = new Organisation("Acme");

	@Test
	void belongsToItsOrganisation() {
		assertThat(new Category(organisation, "Security", CategoryColour.SKY).getOrganisation()).isSameAs(organisation);
	}

	@Test
	void trimsTheNameOnCreate() {
		Category category = new Category(organisation, "  Security  ", CategoryColour.SKY);

		assertThat(category.getName()).isEqualTo("Security");
		assertThat(category.getColour()).isEqualTo(CategoryColour.SKY);
	}

	@Test
	void trimsTheNameOnRename() {
		Category category = new Category(organisation, "Security", CategoryColour.SKY);

		category.rename("\tPayments \n");

		assertThat(category.getName()).isEqualTo("Payments");
	}

	@Test
	void requiresAName() {
		assertThatThrownBy(() -> new Category(organisation, null, CategoryColour.SKY)).isInstanceOf(ValidationException.class);
		assertThatThrownBy(() -> new Category(organisation, "   ", CategoryColour.SKY)).isInstanceOf(ValidationException.class);
	}

	@Test
	void limitsTheNameTo60Characters() {
		assertThat(new Category(organisation, "a".repeat(60), CategoryColour.SKY).getName()).hasSize(60);
		assertThatThrownBy(() -> new Category(organisation, "a".repeat(61), CategoryColour.SKY))
				.isInstanceOf(ValidationException.class);
	}

	@Test
	void requiresAColour() {
		assertThatThrownBy(() -> new Category(organisation, "Security", null)).isInstanceOf(ValidationException.class);
	}
}
