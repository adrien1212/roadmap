package fr.adriencaubel.roadmap.category;

import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.organisation.Organisation;
import fr.adriencaubel.roadmap.organisation.Role;
import fr.adriencaubel.roadmap.owner.Owner;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

	private final Owner owner = new Owner("dev", "dev", null, null);

	@Test
	void belongsToItsCreatorsOrganisation() {
		Organisation organisation = new Organisation("Acme");
		owner.joinAs(organisation, Role.EDITOR, Instant.now());

		assertThat(new Category(owner, "Security", CategoryColour.SKY).getOrganisation()).isSameAs(organisation);
	}

	@Test
	void trimsTheNameOnCreate() {
		Category category = new Category(owner, "  Security  ", CategoryColour.SKY);

		assertThat(category.getName()).isEqualTo("Security");
		assertThat(category.getColour()).isEqualTo(CategoryColour.SKY);
	}

	@Test
	void trimsTheNameOnRename() {
		Category category = new Category(owner, "Security", CategoryColour.SKY);

		category.rename("\tPayments \n");

		assertThat(category.getName()).isEqualTo("Payments");
	}

	@Test
	void requiresAName() {
		assertThatThrownBy(() -> new Category(owner, null, CategoryColour.SKY)).isInstanceOf(ValidationException.class);
		assertThatThrownBy(() -> new Category(owner, "   ", CategoryColour.SKY)).isInstanceOf(ValidationException.class);
	}

	@Test
	void limitsTheNameTo60Characters() {
		assertThat(new Category(owner, "a".repeat(60), CategoryColour.SKY).getName()).hasSize(60);
		assertThatThrownBy(() -> new Category(owner, "a".repeat(61), CategoryColour.SKY))
				.isInstanceOf(ValidationException.class);
	}

	@Test
	void requiresAColour() {
		assertThatThrownBy(() -> new Category(owner, "Security", null)).isInstanceOf(ValidationException.class);
	}
}
