package fr.adriencaubel.roadmap.category;

import fr.adriencaubel.roadmap.common.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryColourTest {

	@Test
	void parsesEveryPaletteKey() {
		assertThat(CategoryColour.fromKey("violet")).isEqualTo(CategoryColour.VIOLET);
		assertThat(CategoryColour.fromKey("slate")).isEqualTo(CategoryColour.SLATE);
		assertThat(CategoryColour.values()).extracting(CategoryColour::key)
				.containsExactly("violet", "emerald", "orange", "pink", "sky", "amber", "lime", "slate");
	}

	@Test
	void rejectsAKeyOutsideThePalette() {
		assertThatThrownBy(() -> CategoryColour.fromKey("red")).isInstanceOf(ValidationException.class);
	}

	@Test
	void rejectsUppercaseAndMissingKeys() {
		assertThatThrownBy(() -> CategoryColour.fromKey("VIOLET")).isInstanceOf(ValidationException.class);
		assertThatThrownBy(() -> CategoryColour.fromKey(null)).isInstanceOf(ValidationException.class);
	}
}
