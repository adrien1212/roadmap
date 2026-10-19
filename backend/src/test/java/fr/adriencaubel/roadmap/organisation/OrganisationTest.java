package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.common.ConflictException;
import fr.adriencaubel.roadmap.common.ForbiddenException;
import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.owner.Owner;
import org.junit.jupiter.api.Test;

import java.time.Instant;

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

	private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");

	private static Owner in(Organisation organisation, Role role) {
		Owner owner = new Owner("google", role.key(), role.key() + "@acme.com", role.key());
		owner.joinAs(organisation, role, NOW);
		return owner;
	}

	@Test
	void isClosedByItsRoleOwnerTypingItsName() {
		Organisation organisation = new Organisation("Acme product team");
		Owner owner = in(organisation, Role.OWNER);

		organisation.close(owner, " Acme product team ", NOW);

		assertThat(organisation.getClosedAt()).isEqualTo(NOW);
		assertThat(organisation.getClosedBy()).isSameAs(owner);
	}

	@Test
	void isClosedByNobodyElse() {
		Organisation organisation = new Organisation("Acme product team");

		assertThatThrownBy(() -> organisation.close(in(organisation, Role.ADMIN), "Acme product team", NOW))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> organisation.close(in(new Organisation("Other"), Role.OWNER), "Acme product team", NOW))
				.isInstanceOf(ForbiddenException.class);
		assertThat(organisation.getClosedAt()).isNull();
	}

	@Test
	void needsItsExactNameToClose() {
		Organisation organisation = new Organisation("Acme product team");

		assertThatThrownBy(() -> organisation.close(in(organisation, Role.OWNER), "acme product team", NOW))
				.isInstanceOf(ValidationException.class);
		assertThat(organisation.getClosedAt()).isNull();
	}

	@Test
	void closesOnce() {
		Organisation organisation = new Organisation("Acme product team");
		Owner owner = in(organisation, Role.OWNER);
		organisation.close(owner, "Acme product team", NOW);

		assertThatThrownBy(() -> organisation.close(owner, "Acme product team", NOW))
				.isInstanceOf(ConflictException.class);
	}
}
