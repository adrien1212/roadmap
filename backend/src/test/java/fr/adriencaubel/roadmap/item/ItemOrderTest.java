package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.common.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemOrderTest {

	private record Row(Long id) {
	}

	private final List<Row> rows = List.of(new Row(1L), new Row(2L), new Row(3L));

	@Test
	void arrangesItemsInTheRequestedOrder() {
		assertThat(ItemOrder.arrange(rows, Row::id, List.of(3L, 1L, 2L)))
				.containsExactly(new Row(3L), new Row(1L), new Row(2L));
	}

	@Test
	void rejectsAListMissingAnItem() {
		assertThatThrownBy(() -> ItemOrder.arrange(rows, Row::id, List.of(3L, 1L)))
				.isInstanceOf(ValidationException.class);
	}

	@Test
	void rejectsAnIdThatIsNotOnTheRoadmap() {
		assertThatThrownBy(() -> ItemOrder.arrange(rows, Row::id, List.of(3L, 1L, 2L, 99L)))
				.isInstanceOf(ValidationException.class);
		assertThatThrownBy(() -> ItemOrder.arrange(rows, Row::id, List.of(3L, 1L, 99L)))
				.isInstanceOf(ValidationException.class);
	}

	@Test
	void rejectsADuplicateId() {
		assertThatThrownBy(() -> ItemOrder.arrange(rows, Row::id, List.of(1L, 1L, 2L)))
				.isInstanceOf(ValidationException.class);
	}

	@Test
	void rejectsAMissingList() {
		assertThatThrownBy(() -> ItemOrder.arrange(rows, Row::id, null)).isInstanceOf(ValidationException.class);
	}
}
