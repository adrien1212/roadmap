package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.common.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlannedDateTest {

	@Test
	void keepsADayAsItIs() {
		PlannedDate date = PlannedDate.of(LocalDate.of(2026, 3, 14), DatePrecision.DAY);

		assertThat(date.first()).isEqualTo(LocalDate.of(2026, 3, 14));
		assertThat(date.last()).isEqualTo(LocalDate.of(2026, 3, 14));
	}

	@Test
	void startsAWeekOnItsMonday() {
		// Saturday Mar 14, 2026 and Sunday Mar 15 are in the week of Monday Mar 9.
		assertThat(PlannedDate.of(LocalDate.of(2026, 3, 14), DatePrecision.WEEK).first()).isEqualTo(LocalDate.of(2026, 3, 9));
		assertThat(PlannedDate.of(LocalDate.of(2026, 3, 15), DatePrecision.WEEK).first()).isEqualTo(LocalDate.of(2026, 3, 9));
		assertThat(PlannedDate.of(LocalDate.of(2026, 3, 9), DatePrecision.WEEK).first()).isEqualTo(LocalDate.of(2026, 3, 9));
	}

	@Test
	void endsAWeekOnItsSundayEvenInTheNextYear() {
		PlannedDate week = PlannedDate.of(LocalDate.of(2026, 12, 30), DatePrecision.WEEK);

		assertThat(week.first()).isEqualTo(LocalDate.of(2026, 12, 28));
		assertThat(week.last()).isEqualTo(LocalDate.of(2027, 1, 3));
	}

	@Test
	void spansAWholeMonth() {
		PlannedDate march = PlannedDate.of(LocalDate.of(2026, 3, 14), DatePrecision.MONTH);
		PlannedDate leapFebruary = PlannedDate.of(LocalDate.of(2028, 2, 10), DatePrecision.MONTH);

		assertThat(march.first()).isEqualTo(LocalDate.of(2026, 3, 1));
		assertThat(march.last()).isEqualTo(LocalDate.of(2026, 3, 31));
		assertThat(leapFebruary.last()).isEqualTo(LocalDate.of(2028, 2, 29));
	}

	@Test
	void spansAWholeQuarter() {
		assertThat(PlannedDate.of(LocalDate.of(2026, 2, 14), DatePrecision.QUARTER))
				.isEqualTo(new PlannedDate(LocalDate.of(2026, 1, 1), DatePrecision.QUARTER));
		assertThat(PlannedDate.of(LocalDate.of(2026, 3, 31), DatePrecision.QUARTER).last()).isEqualTo(LocalDate.of(2026, 3, 31));
		assertThat(PlannedDate.of(LocalDate.of(2026, 5, 1), DatePrecision.QUARTER).last()).isEqualTo(LocalDate.of(2026, 6, 30));
		assertThat(PlannedDate.of(LocalDate.of(2026, 8, 20), DatePrecision.QUARTER).first()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(PlannedDate.of(LocalDate.of(2026, 8, 20), DatePrecision.QUARTER).last()).isEqualTo(LocalDate.of(2026, 9, 30));
		assertThat(PlannedDate.of(LocalDate.of(2026, 11, 2), DatePrecision.QUARTER).last()).isEqualTo(LocalDate.of(2026, 12, 31));
	}

	@Test
	void isNoDateWithoutADate() {
		assertThat(PlannedDate.of(null, DatePrecision.QUARTER)).isNull();
	}

	@Test
	void takesDayWhenThePrecisionIsMissing() {
		assertThat(PlannedDate.of(LocalDate.of(2026, 3, 14), null).precision()).isEqualTo(DatePrecision.DAY);
	}

	@Test
	void readsLowerCaseKeys() {
		assertThat(DatePrecision.fromKey("week")).isEqualTo(DatePrecision.WEEK);
		assertThat(DatePrecision.fromKey(null)).isEqualTo(DatePrecision.DAY);
		assertThat(DatePrecision.QUARTER.key()).isEqualTo("quarter");
		assertThatThrownBy(() -> DatePrecision.fromKey("year")).isInstanceOf(ValidationException.class);
	}
}
