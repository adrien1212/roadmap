package fr.adriencaubel.roadmap.roadmap;

import fr.adriencaubel.roadmap.item.ItemStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoadmapPreviewTest {

	private static final Year YEAR = Year.of(2026);

	private static DatedItem dated(String start, String end) {
		return new DatedItem(1L, LocalDate.parse(start), LocalDate.parse(end), ItemStatus.PLANNED);
	}

	@Test
	void keepsItemsOverlappingTheYearInManualOrder() {
		DatedItem inside = dated("2026-03-01", "2026-04-01");
		DatedItem fromLastYear = dated("2025-11-01", "2026-01-15");
		DatedItem intoNextYear = dated("2026-12-01", "2027-02-01");
		DatedItem lastYear = dated("2025-01-01", "2025-12-31");
		DatedItem nextYear = dated("2027-01-01", "2027-03-01");

		assertThat(RoadmapPreview.select(List.of(intoNextYear, lastYear, inside, nextYear, fromLastYear), YEAR))
				.containsExactly(intoNextYear, inside, fromLastYear);
	}

	@Test
	void keepsAtMostFourItems() {
		List<DatedItem> items = List.of(
				dated("2026-01-01", "2026-01-02"), dated("2026-02-01", "2026-02-02"),
				dated("2026-03-01", "2026-03-02"), dated("2026-04-01", "2026-04-02"),
				dated("2026-05-01", "2026-05-02"));

		assertThat(RoadmapPreview.select(items, YEAR)).containsExactlyElementsOf(items.subList(0, 4));
	}
}
