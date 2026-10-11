package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.owner.Owner;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ItemTest {

	private final Roadmap roadmap = Roadmap.create(new Owner("dev", "dev", null, null), "Q3");

	private Item item() {
		return Item.append(roadmap, "Dark mode", null);
	}

	@Test
	void defaultsToPlannedWithoutCategoryNorDatesAndNotInternal() {
		Item item = item();

		assertThat(item.getStatus()).isEqualTo(ItemStatus.PLANNED);
		assertThat(item.getCategory()).isNull();
		assertThat(item.isInternal()).isFalse();
		assertThat(item.getStartDate()).isNull();
		assertThat(item.getEndDate()).isNull();
		assertThat(item.getDescription()).isNull();
	}

	@Test
	void isFirstOnAnEmptyRoadmap() {
		assertThat(Item.append(roadmap, "Dark mode", null).getPosition()).isZero();
	}

	@Test
	void isAppendedAfterTheLastItem() {
		assertThat(Item.append(roadmap, "Dark mode", 7).getPosition()).isEqualTo(8);
	}

	@Test
	void trimsAndRequiresATitle() {
		assertThat(Item.append(roadmap, "  Dark mode ", null).getTitle()).isEqualTo("Dark mode");
		assertThatThrownBy(() -> Item.append(roadmap, " ", null)).isInstanceOf(ValidationException.class);
		assertThatThrownBy(() -> item().retitle(null)).isInstanceOf(ValidationException.class);
	}

	@Test
	void limitsTheTitleTo140Characters() {
		assertThat(Item.append(roadmap, "a".repeat(140), null).getTitle()).hasSize(140);
		assertThatThrownBy(() -> Item.append(roadmap, "a".repeat(141), null)).isInstanceOf(ValidationException.class);
	}

	@Test
	void keepsLineBreaksInTheDescription() {
		Item item = item();

		item.describe("First line\nSecond line\n");

		assertThat(item.getDescription()).isEqualTo("First line\nSecond line\n");
	}

	@Test
	void limitsTheDescriptionTo10000Characters() {
		assertThatNoException().isThrownBy(() -> item().describe("a".repeat(10_000)));
		assertThatThrownBy(() -> item().describe("a".repeat(10_001))).isInstanceOf(ValidationException.class);
	}

	@Test
	void acceptsNoDatesAStartOnlyOrAnEndOnly() {
		Item item = item();

		item.schedule(LocalDate.of(2026, 3, 1), null);
		assertThat(item.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
		assertThat(item.getEndDate()).isNull();

		item.schedule(null, LocalDate.of(2026, 5, 1));
		assertThat(item.getStartDate()).isNull();
		assertThat(item.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 1));

		item.schedule(null, null);
		assertThat(item.getStartDate()).isNull();
		assertThat(item.getEndDate()).isNull();
	}

	@Test
	void acceptsAnEndOnOrAfterTheStart() {
		Item item = item();

		item.schedule(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1));
		assertThat(item.getEndDate()).isEqualTo(LocalDate.of(2026, 3, 1));

		item.schedule(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));
		assertThat(item.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 1));
	}

	@Test
	void rejectsAnEndBeforeTheStartAndKeepsThePreviousDates() {
		Item item = item();
		item.schedule(LocalDate.of(2026, 3, 1), null);

		assertThatThrownBy(() -> item.schedule(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1)))
				.isInstanceOf(ValidationException.class);
		assertThat(item.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
	}

	@Test
	void requiresAStatus() {
		assertThatThrownBy(() -> item().changeStatus(null)).isInstanceOf(ValidationException.class);
	}
}
