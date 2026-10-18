package fr.adriencaubel.roadmap.organisation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

	@Test
	void anInvitationCarriesAdminOrEditorNeverOwner() {
		assertThat(Role.ADMIN.invitable()).isTrue();
		assertThat(Role.EDITOR.invitable()).isTrue();
		assertThat(Role.OWNER.invitable()).isFalse();
	}
}
