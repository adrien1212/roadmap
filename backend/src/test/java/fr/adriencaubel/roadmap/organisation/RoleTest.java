package fr.adriencaubel.roadmap.organisation;

import fr.adriencaubel.roadmap.common.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleTest {

	@Test
	void anInvitationCarriesAdminOrEditorNeverOwner() {
		assertThat(Role.ADMIN.invitable()).isTrue();
		assertThat(Role.EDITOR.invitable()).isTrue();
		assertThat(Role.OWNER.invitable()).isFalse();
	}

	@Test
	void isWrittenInLowerCaseInJson() {
		assertThat(Role.OWNER.key()).isEqualTo("owner");
		assertThat(Role.fromKey("editor")).isEqualTo(Role.EDITOR);
		assertThatThrownBy(() -> Role.fromKey("viewer")).isInstanceOf(ValidationException.class);
		assertThatThrownBy(() -> Role.fromKey(null)).isInstanceOf(ValidationException.class);
	}
}
