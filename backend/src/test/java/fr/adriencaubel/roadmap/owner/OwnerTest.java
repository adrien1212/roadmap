package fr.adriencaubel.roadmap.owner;

import fr.adriencaubel.roadmap.common.ConflictException;
import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.organisation.Organisation;
import fr.adriencaubel.roadmap.organisation.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnerTest {

	private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

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

	@Test
	void takesTheLatestEmailAndNameFromItsProvider() {
		owner.refreshProfile("jane.doe@example.com", "Jane D.", true);

		assertThat(owner.getEmail()).isEqualTo("jane.doe@example.com");
		assertThat(owner.getName()).isEqualTo("Jane D.");
		assertThat(owner.isEmailVerified()).isTrue();
	}

	@Test
	void keepsItsBrandWhenTheProfileChanges() {
		owner.changeBrand("Acme");

		owner.refreshProfile("jane.doe@example.com", "Jane D.", true);

		assertThat(owner.getBrandName()).isEqualTo("Acme");
	}

	@Test
	void hasNoOrganisationAtFirstSignIn() {
		assertThat(owner.getOrganisation()).isNull();
		assertThat(owner.getRole()).isNull();
		assertThat(owner.isEmailVerified()).isFalse();
	}

	@Test
	void joinsAnOrganisationWithARole() {
		Organisation organisation = new Organisation("Acme");

		owner.joinAs(organisation, Role.EDITOR, NOW);

		assertThat(owner.getOrganisation()).isSameAs(organisation);
		assertThat(owner.getRole()).isEqualTo(Role.EDITOR);
		assertThat(owner.getJoinedAt()).isEqualTo(NOW);
	}

	@Test
	void cannotJoinASecondOrganisation() {
		owner.joinAs(new Organisation("Acme"), Role.EDITOR, NOW);

		assertThatThrownBy(() -> owner.joinAs(new Organisation("Other"), Role.ADMIN, NOW))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void changesBetweenAdminAndEditor() {
		owner.joinAs(new Organisation("Acme"), Role.EDITOR, NOW);

		owner.changeRole(Role.ADMIN);

		assertThat(owner.getRole()).isEqualTo(Role.ADMIN);
	}

	@Test
	void nobodyBecomesOwnerByARoleChange() {
		owner.joinAs(new Organisation("Acme"), Role.ADMIN, NOW);

		assertThatThrownBy(() -> owner.changeRole(Role.OWNER)).isInstanceOf(ConflictException.class);
		assertThat(owner.getRole()).isEqualTo(Role.ADMIN);
	}

	@Test
	void theRoleOwnerNeverChanges() {
		owner.joinAs(new Organisation("Acme"), Role.OWNER, NOW);

		assertThatThrownBy(() -> owner.changeRole(Role.ADMIN)).isInstanceOf(ConflictException.class);
		assertThat(owner.getRole()).isEqualTo(Role.OWNER);
	}

	@Test
	void leavingClearsTheOrganisationAndRole() {
		owner.joinAs(new Organisation("Acme"), Role.EDITOR, NOW);

		owner.leave();

		assertThat(owner.getOrganisation()).isNull();
		assertThat(owner.getRole()).isNull();
		assertThat(owner.getJoinedAt()).isNull();
	}

	@Test
	void theRoleOwnerCannotLeave() {
		Organisation organisation = new Organisation("Acme");
		owner.joinAs(organisation, Role.OWNER, NOW);

		assertThatThrownBy(owner::leave).isInstanceOf(ConflictException.class);
		assertThat(owner.getOrganisation()).isSameAs(organisation);
	}
}
