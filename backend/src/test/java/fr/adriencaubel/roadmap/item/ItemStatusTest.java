package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.common.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemStatusTest {

	@Test
	void usesLowercaseJsonKeys() {
		assertThat(ItemStatus.values()).extracting(ItemStatus::key)
				.containsExactly("planned", "in_progress", "shipped");
	}

	@Test
	void parsesAJsonKey() {
		assertThat(ItemStatus.fromKey("in_progress")).isEqualTo(ItemStatus.IN_PROGRESS);
	}

	@Test
	void rejectsAnUnknownKey() {
		assertThatThrownBy(() -> ItemStatus.fromKey("IN_PROGRESS")).isInstanceOf(ValidationException.class);
		assertThatThrownBy(() -> ItemStatus.fromKey("done")).isInstanceOf(ValidationException.class);
	}
}
